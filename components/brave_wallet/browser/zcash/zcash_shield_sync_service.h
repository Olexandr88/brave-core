/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#ifndef BRAVE_COMPONENTS_BRAVE_WALLET_BROWSER_ZCASH_ZCASH_SHIELD_SYNC_SERVICE_H_
#define BRAVE_COMPONENTS_BRAVE_WALLET_BROWSER_ZCASH_ZCASH_SHIELD_SYNC_SERVICE_H_

#include <memory>
#include <optional>
#include <string>
#include <vector>

#include "base/memory/scoped_refptr.h"
#include "base/threading/sequence_bound.h"
#include "base/types/expected.h"
#include "brave/components/brave_wallet/browser/internal/orchard_block_scanner.h"
#include "brave/components/brave_wallet/browser/zcash/zcash_orchard_sync_state.h"
#include "brave/components/brave_wallet/browser/zcash/zcash_rpc.h"
#include "brave/components/brave_wallet/common/brave_wallet.mojom.h"
#include "mojo/public/cpp/bindings/remote.h"

namespace brave_wallet {

class ZCashBlocksBatchScanTask;
class ZCashScanBlocksTask;
class ZCashUpdateSubtreeRootsTask;
class ZCashVerifyChainStateTask;

// ZCashScanService downloads and scans blockchain blocks to find
// spendable notes related to the account.
// Provided full view key allows to decode orchard compact actions
// related to the account.
class ZCashShieldSyncService {
 public:
  enum class ErrorCode {
    kFailedToDownloadBlocks,
    kFailedToUpdateDatabase,
    kFailedToUpdateChainTip,
    kFailedToRetrieveSpendableNotes,
    kFailedToReceiveTreeState,
    kFailedToInitAccount,
    kFailedToRetrieveAccount,
    kFailedToVerifyChainState,
    kFailedToUpdateSubtreeRoots,
    kDatabaseError,
    kScannerError,
  };

  struct Error {
    ErrorCode code;
    std::string message;
  };

  struct ScanRangeResult {
    uint32_t start_block = 0;
    uint32_t end_block = 0;
    size_t total_ranges = 0;
    size_t ready_ranges = 0;

    bool IsFinished() { return total_ranges == ready_ranges; }
  };

  class Observer {
   public:
    virtual ~Observer() {}
    virtual void OnSyncStart(const mojom::AccountIdPtr& account_id) = 0;
    virtual void OnSyncStop(const mojom::AccountIdPtr& account_id) = 0;
    virtual void OnSyncError(const mojom::AccountIdPtr& account_id,
                             const std::string& error) = 0;
    virtual void OnSyncStatusUpdate(
        const mojom::AccountIdPtr& account_id,
        const mojom::ZCashShieldSyncStatusPtr& status) = 0;
  };

  class OrchardBlockScannerProxy {
   public:
    explicit OrchardBlockScannerProxy(OrchardFullViewKey full_view_key);
    virtual ~OrchardBlockScannerProxy();
    virtual void ScanBlocks(
        FrontierChainState frontier_chain_state,
        std::vector<zcash::mojom::CompactBlockPtr> blocks,
        base::OnceCallback<void(base::expected<OrchardBlockScanner::Result,
                                               OrchardBlockScanner::ErrorCode>)>
            callback);

   private:
    base::SequenceBound<OrchardBlockScanner> background_block_scanner_;
  };

  ZCashShieldSyncService(
      ZCashRpc* zcash_rpc,
      const mojom::AccountIdPtr& account_id,
      const mojom::ZCashAccountShieldBirthdayPtr& account_birthday,
      const std::array<uint8_t, kOrchardFullViewKeySize>& fvk,
      base::FilePath db_dir_path,
      base::WeakPtr<Observer> observer);
  virtual ~ZCashShieldSyncService();

  bool IsStarted();

  void StartSyncing();

  mojom::ZCashShieldSyncStatusPtr GetSyncStatus();

  void Reset();

 private:
  FRIEND_TEST_ALL_PREFIXES(ZCashShieldSyncServiceTest, ScanBlocks);
  friend class ZCashBlocksBatchScanTask;
  friend class ZCashScanBlocksTask;
  friend class ZCashUpdateSubtreeRootsTask;
  friend class ZCashVerifyChainStateTask;

  void SetOrchardBlockScannerProxyForTesting(
      std::unique_ptr<OrchardBlockScannerProxy> block_scanner);

  void ScheduleWorkOnTask();
  void WorkOnTask();

  // Setup account info
  void GetOrCreateAccount();
  void OnGetAccountMeta(base::expected<ZCashOrchardStorage::AccountMeta,
                                       ZCashOrchardStorage::Error> result);
  void InitAccount();
  void OnAccountInit(base::expected<ZCashOrchardStorage::AccountMeta,
                                    ZCashOrchardStorage::Error> error);

  // Get last known block in the blockchain
  void UpdateChainTip();
  void OnGetLatestBlock(
      base::expected<zcash::mojom::BlockIDPtr, std::string> result);

  // Chain reorg flow
  // Chain reorg happens when latest blocks are removed from the blockchain
  // We assume that there is a limit of reorg depth - kChainReorgBlockDelta
  void VerifyChainState();
  void OnChainStateVerified(
      base::expected<bool, ZCashShieldSyncService::Error> result);

  void UpdateSubtreeRoots();
  void OnSubtreeRootsUpdated(bool result);

  // Update spendable notes state
  void UpdateSpendableNotes();
  void OnGetSpendableNotes(base::expected<std::vector<OrchardNote>,
                                          ZCashOrchardStorage::Error> result);

  void StartBlockScanning();
  void OnScanRangeResult(
      base::expected<ScanRangeResult, ZCashShieldSyncService::Error> result);

  uint32_t GetSpendableBalance();

  std::optional<Error> error() { return error_; }

  // Params
  raw_ptr<ZCashRpc> zcash_rpc_ = nullptr;
  mojom::AccountIdPtr account_id_;
  // Birthday of the account will be used to resolve initial scan range.
  mojom::ZCashAccountShieldBirthdayPtr account_birthday_;
  base::FilePath db_dir_path_;
  base::WeakPtr<Observer> observer_;
  std::string chain_id_;

  base::SequenceBound<ZCashOrchardSyncState> sync_state_;
  std::unique_ptr<OrchardBlockScannerProxy> block_scanner_;

  std::optional<ZCashOrchardStorage::AccountMeta> account_meta_;
  // Latest scanned block
  std::optional<uint32_t> latest_scanned_block_;

  std::unique_ptr<ZCashVerifyChainStateTask> verify_chain_state_task_;
  bool chain_state_verified_ = false;

  std::unique_ptr<ZCashUpdateSubtreeRootsTask> update_subtree_roots_task_;
  bool subtree_roots_updated_ = false;

  std::unique_ptr<ZCashScanBlocksTask> scan_blocks_task_;
  std::optional<ScanRangeResult> latest_scanned_block_result_;

  // Local cache of spendable notes to fast check on discovered nullifiers
  std::optional<std::vector<OrchardNote>> spendable_notes_;
  std::optional<Error> error_;
  bool stopped_ = false;

  mojom::ZCashShieldSyncStatusPtr current_sync_status_;

  base::WeakPtrFactory<ZCashShieldSyncService> weak_ptr_factory_{this};
};

}  // namespace brave_wallet

#endif  // BRAVE_COMPONENTS_BRAVE_WALLET_BROWSER_ZCASH_ZCASH_SHIELD_SYNC_SERVICE_H_
