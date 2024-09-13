// Copyright (c) 2024 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this file,
// you can obtain one at http://mozilla.org/MPL/2.0/.

#include <algorithm>

#include "brave/components/brave_wallet/browser/zcash/zcash_scan_blocks_task.h"

namespace brave_wallet {

namespace {
constexpr uint32_t kBatchSize = 1024;
}  // namespace

ZCashScanBlocksTask::ZCashScanBlocksTask(
    ZCashShieldSyncService* sync_service,
    ZCashScanBlocksTaskObserver observer) : sync_service_(sync_service), observer_(std::move(observer)) {
}

ZCashScanBlocksTask::~ZCashScanBlocksTask() {
}

void ZCashScanBlocksTask::Start() {
  ScheduleWorkOnTask();
}

void ZCashScanBlocksTask::ScheduleWorkOnTask() {
  base::SequencedTaskRunner::GetCurrentDefault()->PostTask(
      FROM_HERE, base::BindOnce(&ZCashScanBlocksTask::WorkOnTask,
                                weak_ptr_factory_.GetWeakPtr()));
}

void ZCashScanBlocksTask::WorkOnTask() {
  if (error_) {
    observer_.Run(base::unexpected(*error_));
    return;
  }

  if (!latest_scanned_block_) {
    GetLatestScannedBlock();
    return;
  }

  if (!chain_tip_block_) {
    GetChainTip();
    return;
  }

  if (!scan_ranges_) {
    PrepareScanRanges();
    return;
  }

  ScanRanges();
}

void ZCashScanBlocksTask::PrepareScanRanges() {
  uint32_t from = latest_scanned_block_.value() + 1;
  uint32_t to = chain_tip_block_.value();
  initial_ranges_count_ = std::ceil((double)(to - from) / kBatchSize);
  scan_ranges_ = std::deque<ScanRange>();
  for (size_t i = 0; i < initial_ranges_count_.value(); i++) {
    scan_ranges_->push_back(ScanRange{ static_cast<uint32_t>(from + i * kBatchSize), std::min (to, static_cast<uint32_t>(from + (i + 1)* kBatchSize)) });
  }
}

void ZCashScanBlocksTask::GetLatestScannedBlock() {
  sync_service_->sync_state_.AsyncCall(&ZCashOrchardSyncState::GetAccountMeta)
      .WithArgs(sync_service_->account_id_.Clone())
      .Then(base::BindOnce(&ZCashScanBlocksTask::OnGetAccountMeta,
                           weak_ptr_factory_.GetWeakPtr()));
}

void ZCashScanBlocksTask::OnGetAccountMeta(    base::expected<ZCashOrchardStorage::AccountMeta, ZCashOrchardStorage::Error>
                                               result) {
  if (!result.has_value()) {
    error_ = ZCashShieldSyncService::Error{ZCashShieldSyncService::ErrorCode::kFailedToRetrieveAccount, ""};
    ScheduleWorkOnTask();
    return;
  }

  latest_scanned_block_ = result.value().latest_scanned_block_id;
  ScheduleWorkOnTask();
}

void ZCashScanBlocksTask::GetChainTip() {
  sync_service_->zcash_rpc_->GetLatestBlock(
      sync_service_->chain_id_, base::BindOnce(&ZCashScanBlocksTask::OnGetChainTip,
                                weak_ptr_factory_.GetWeakPtr()));
}

void ZCashScanBlocksTask::OnGetChainTip(
    base::expected<zcash::mojom::BlockIDPtr, std::string> result) {
  if (!result.has_value()) {
    error_ = ZCashShieldSyncService::Error{ ZCashShieldSyncService::ErrorCode::kFailedToUpdateChainTip, result.error() };
    ScheduleWorkOnTask();
    return;
  }

  chain_tip_block_ = (*result)->height;
  ScheduleWorkOnTask();
}


void ZCashScanBlocksTask::ScanRanges() {
  CHECK(scan_ranges_);
  if (scan_ranges_->empty()) {
    return;
  }
  auto scan_range = scan_ranges_->front();
  scan_ranges_->pop_front();
  current_block_range_ = std::make_unique<ZCashBlocksBatchScanTask>(
      sync_service_, scan_range.from, scan_range.to,
      base::BindOnce(&ZCashScanBlocksTask::OnScanningRangeComplete,
                     weak_ptr_factory_.GetWeakPtr()));
  current_block_range_->Start();
}

void ZCashScanBlocksTask::OnScanningRangeComplete(base::expected<bool, ZCashShieldSyncService::Error> result) {
  if (!result.has_value()) {
    error_ = result.error();
    ScheduleWorkOnTask();
    return;
  }

  ScanRanges();
}

}  // namespace brave_wallet
