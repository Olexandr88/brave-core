/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#include "brave/components/brave_wallet/browser/zcash/zcash_shield_sync_service.h"

#include <algorithm>
#include <utility>

#include "base/task/thread_pool.h"
#include "brave/components/brave_wallet/browser/zcash/zcash_scan_blocks_task.h"
#include "brave/components/brave_wallet/browser/zcash/zcash_update_subtree_roots_task.h"
#include "brave/components/brave_wallet/browser/zcash/zcash_verify_chain_state_task.h"
#include "brave/components/brave_wallet/common/common_utils.h"
#include "brave/components/brave_wallet/common/hex_utils.h"
#include "brave/components/brave_wallet/common/zcash_utils.h"

namespace brave_wallet {

namespace {

size_t GetCode(ZCashShieldSyncService::ErrorCode error) {
  switch (error) {
    case ZCashShieldSyncService::ErrorCode::kFailedToDownloadBlocks:
      return 0;
    case ZCashShieldSyncService::ErrorCode::kFailedToUpdateDatabase:
      return 1;
    case ZCashShieldSyncService::ErrorCode::kFailedToUpdateChainTip:
      return 2;
    case ZCashShieldSyncService::ErrorCode::kFailedToRetrieveSpendableNotes:
      return 3;
    case ZCashShieldSyncService::ErrorCode::kFailedToReceiveTreeState:
      return 4;
    case ZCashShieldSyncService::ErrorCode::kFailedToInitAccount:
      return 5;
    case ZCashShieldSyncService::ErrorCode::kFailedToRetrieveAccount:
      return 6;
    case ZCashShieldSyncService::ErrorCode::kFailedToVerifyChainState:
      return 7;
    case ZCashShieldSyncService::ErrorCode::kFailedToUpdateSubtreeRoots:
      return 8;
    case ZCashShieldSyncService::ErrorCode::kDatabaseError:
      return 9;
    case ZCashShieldSyncService::ErrorCode::kScannerError:
      return 10;
  }
}

}  // namespace

ZCashShieldSyncService::OrchardBlockScannerProxy::OrchardBlockScannerProxy(
    OrchardFullViewKey full_view_key) {
  background_block_scanner_.emplace(
      base::ThreadPool::CreateSequencedTaskRunner({base::MayBlock()}),
      full_view_key);
}

ZCashShieldSyncService::OrchardBlockScannerProxy::~OrchardBlockScannerProxy() =
    default;

void ZCashShieldSyncService::OrchardBlockScannerProxy::ScanBlocks(
    FrontierChainState frontier_chain_state,
    std::vector<zcash::mojom::CompactBlockPtr> blocks,
    base::OnceCallback<void(base::expected<OrchardBlockScanner::Result,
                                           OrchardBlockScanner::ErrorCode>)>
        callback) {
  background_block_scanner_.AsyncCall(&OrchardBlockScanner::ScanBlocks)
      .WithArgs(std::move(frontier_chain_state), std::move(blocks))
      .Then(std::move(callback));
}

ZCashShieldSyncService::ZCashShieldSyncService(
    ZCashRpc* zcash_rpc,
    const mojom::AccountIdPtr& account_id,
    const mojom::ZCashAccountShieldBirthdayPtr& account_birthday,
    const OrchardFullViewKey& fvk,
    base::FilePath db_dir_path,
    base::WeakPtr<Observer> observer)
    : zcash_rpc_(zcash_rpc),
      account_id_(account_id.Clone()),
      account_birthday_(account_birthday.Clone()),
      db_dir_path_(db_dir_path),
      observer_(std::move(observer)) {
  chain_id_ = GetNetworkForZCashKeyring(account_id->keyring_id);
  block_scanner_ = std::make_unique<OrchardBlockScannerProxy>(fvk);
  sync_state_.emplace(
      base::ThreadPool::CreateSequencedTaskRunner({base::MayBlock()}),
      db_dir_path_);
}

ZCashShieldSyncService::~ZCashShieldSyncService() = default;

void ZCashShieldSyncService::SetOrchardBlockScannerProxyForTesting(
    std::unique_ptr<OrchardBlockScannerProxy> block_scanner) {
  block_scanner_ = std::move(block_scanner);
}

void ZCashShieldSyncService::StartSyncing() {
  ScheduleWorkOnTask();
  if (observer_) {
    observer_->OnSyncStart(account_id_);
  }
}

mojom::ZCashShieldSyncStatusPtr ZCashShieldSyncService::GetSyncStatus() {
  return current_sync_status_.Clone();
}

void ZCashShieldSyncService::ScheduleWorkOnTask() {
  base::SequencedTaskRunner::GetCurrentDefault()->PostTask(
      FROM_HERE, base::BindOnce(&ZCashShieldSyncService::WorkOnTask,
                                weak_ptr_factory_.GetWeakPtr()));
}

void ZCashShieldSyncService::WorkOnTask() {
  if (stopped_) {
    return;
  }

  if (error_) {
    if (observer_) {
      observer_->OnSyncError(
          account_id_,
          base::NumberToString(GetCode(error_->code)) + ": " + error_->message);
    }
    return;
  }

  if (!account_meta_) {
    GetOrCreateAccount();
    return;
  }

  if (!chain_state_verified_) {
    VerifyChainState();
    return;
  }

  if (!subtree_roots_updated_) {
    UpdateSubtreeRoots();
    return;
  }

  if (!spendable_notes_) {
    UpdateSpendableNotes();
    return;
  }

  if (!block_scanner_) {
    StartBlockScanning();
    return;
  }

  if (latest_scanned_block_result_ &&
      latest_scanned_block_result_->IsFinished()) {
    if (observer_) {
      observer_->OnSyncStop(account_id_);
      stopped_ = true;
    }
  }
}

void ZCashShieldSyncService::GetOrCreateAccount() {
  sync_state_.AsyncCall(&ZCashOrchardSyncState::GetAccountMeta)
      .WithArgs(account_id_.Clone())
      .Then(base::BindOnce(&ZCashShieldSyncService::OnGetAccountMeta,
                           weak_ptr_factory_.GetWeakPtr()));
}

void ZCashShieldSyncService::OnGetAccountMeta(
    base::expected<ZCashOrchardStorage::AccountMeta, ZCashOrchardStorage::Error>
        result) {
  if (result.has_value()) {
    account_meta_ = *result;
    if (account_meta_->latest_scanned_block_id <
        account_meta_->account_birthday) {
      error_ = Error{ErrorCode::kFailedToRetrieveAccount, ""};
    }
    ScheduleWorkOnTask();
    return;
  }

  if (result.error().error_code ==
      ZCashOrchardStorage::ErrorCode::kAccountNotFound) {
    InitAccount();
    return;
  } else {
    error_ = Error{ErrorCode::kFailedToRetrieveAccount, result.error().message};
  }
  ScheduleWorkOnTask();
}

void ZCashShieldSyncService::InitAccount() {
  sync_state_.AsyncCall(&ZCashOrchardSyncState::RegisterAccount)
      .WithArgs(account_id_.Clone(), account_birthday_->value,
                account_birthday_->hash)
      .Then(base::BindOnce(&ZCashShieldSyncService::OnAccountInit,
                           weak_ptr_factory_.GetWeakPtr()));
}

void ZCashShieldSyncService::OnAccountInit(
    base::expected<ZCashOrchardStorage::AccountMeta, ZCashOrchardStorage::Error>
        result) {
  if (!result.has_value()) {
    error_ = Error{ErrorCode::kFailedToInitAccount, result.error().message};
  } else {
    account_meta_ = *result;
  }
  ScheduleWorkOnTask();
}

void ZCashShieldSyncService::UpdateSpendableNotes() {
  spendable_notes_ = std::nullopt;
  sync_state_.AsyncCall(&ZCashOrchardSyncState::GetSpendableNotes)
      .WithArgs(account_id_.Clone())
      .Then(base::BindOnce(&ZCashShieldSyncService::OnGetSpendableNotes,
                           weak_ptr_factory_.GetWeakPtr()));
}

void ZCashShieldSyncService::OnGetSpendableNotes(
    base::expected<std::vector<OrchardNote>, ZCashOrchardStorage::Error>
        result) {
  if (!result.has_value()) {
    error_ = Error{ErrorCode::kFailedToRetrieveSpendableNotes,
                   result.error().message};
    ScheduleWorkOnTask();
    return;
  }

  spendable_notes_ = result.value();

  if (latest_scanned_block_result_) {
    current_sync_status_ = mojom::ZCashShieldSyncStatus::New(
        latest_scanned_block_result_->start_block,
        latest_scanned_block_result_->end_block,
        latest_scanned_block_result_->total_ranges,
        latest_scanned_block_result_->ready_ranges, spendable_notes_->size(),
        GetSpendableBalance());
  } else {
    current_sync_status_ = mojom::ZCashShieldSyncStatus::New(
        latest_scanned_block_.value(), latest_scanned_block_.value(), 0, 0,
        spendable_notes_->size(), GetSpendableBalance());
  }

  if (observer_) {
    observer_->OnSyncStatusUpdate(account_id_, current_sync_status_.Clone());
  }

  ScheduleWorkOnTask();
}

void ZCashShieldSyncService::VerifyChainState() {
  CHECK(!verify_chain_state_task_.get());
  verify_chain_state_task_ = std::make_unique<ZCashVerifyChainStateTask>(
      this, base::BindOnce(&ZCashShieldSyncService::OnChainStateVerified,
                           weak_ptr_factory_.GetWeakPtr()));
  verify_chain_state_task_->Start();
}

void ZCashShieldSyncService::OnChainStateVerified(
    base::expected<bool, ZCashShieldSyncService::Error> result) {
  if (!result.has_value()) {
    error_ = result.error();
    ScheduleWorkOnTask();
    return;
  }

  if (!result.value()) {
    error_ = Error{ErrorCode::kFailedToVerifyChainState, ""};
    ScheduleWorkOnTask();
    return;
  }

  chain_state_verified_ = true;
  ScheduleWorkOnTask();
}

void ZCashShieldSyncService::UpdateSubtreeRoots() {
  CHECK(!update_subtree_roots_task_);
  update_subtree_roots_task_ = std::make_unique<ZCashUpdateSubtreeRootsTask>(
      this,
      base::BindOnce(&ZCashShieldSyncService::OnSubtreeRootsUpdated, weak_ptr_factory_.GetWeakPtr()));
  update_subtree_roots_task_->Start();
}

void ZCashShieldSyncService::OnSubtreeRootsUpdated(
    bool result) {
  if (!result) {
    error_ = Error{ErrorCode::kFailedToUpdateSubtreeRoots, ""};
    ScheduleWorkOnTask();
    return;
  }

  subtree_roots_updated_ = true;
  ScheduleWorkOnTask();
}

void ZCashShieldSyncService::StartBlockScanning() {
  CHECK(!scan_blocks_task_);
  scan_blocks_task_ = std::make_unique<ZCashScanBlocksTask>(
      this, base::BindRepeating(&ZCashShieldSyncService::OnScanRangeResult,
                                weak_ptr_factory_.GetWeakPtr()));
  scan_blocks_task_->Start();
}

void ZCashShieldSyncService::OnScanRangeResult(
    base::expected<ScanRangeResult, ZCashShieldSyncService::Error> result) {
  if (!result.has_value()) {
    scan_blocks_task_.reset();
    error_ = result.error();
    ScheduleWorkOnTask();
    return;
  }

  latest_scanned_block_result_ = result.value();
  UpdateSpendableNotes();
}

uint32_t ZCashShieldSyncService::GetSpendableBalance() {
  CHECK(spendable_notes_.has_value());
  uint32_t balance = 0;
  for (const auto& note : spendable_notes_.value()) {
    balance += note.amount;
  }
  return balance;
}

void ZCashShieldSyncService::Reset() {
  // sync_state_.AsyncCall(&ZCashOrchardStorage::ResetDatabase);
}

}  // namespace brave_wallet
