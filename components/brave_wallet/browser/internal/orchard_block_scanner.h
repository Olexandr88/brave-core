/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#ifndef BRAVE_COMPONENTS_BRAVE_WALLET_BROWSER_INTERNAL_ORCHARD_BLOCK_SCANNER_H_
#define BRAVE_COMPONENTS_BRAVE_WALLET_BROWSER_INTERNAL_ORCHARD_BLOCK_SCANNER_H_

#include <algorithm>
#include <memory>
#include <utility>
#include <vector>

#include "base/types/expected.h"
#include "brave/components/brave_wallet/browser/internal/orchard_block_scanner.h"
#include "brave/components/brave_wallet/browser/zcash/rust/orchard_block_decoder.h"
#include "brave/components/brave_wallet/browser/zcash/rust/orchard_decoded_blocks_bunde.h"
#include "brave/components/brave_wallet/common/zcash_utils.h"

namespace brave_wallet {

// Scans a bunch of blocks with the provided full view key to find
// spendable notes related to the account.
class OrchardBlockScanner {
 public:
  enum class ErrorCode { kInputError, kDecoderError };

  struct Result {
    Result();
    Result(std::vector<OrchardNote> discovered_notes,
           std::vector<OrchardNullifier> spent_notes,
           std::unique_ptr<orchard::OrchardDecodedBlocksBundle> scanned_blocks);
    Result(const Result&) = delete;
    Result& operator=(const Result&) = delete;
    Result(Result&&);
    Result& operator=(Result&&);
    ~Result();

    // New notes have been discovered
    std::vector<OrchardNote> discovered_notes;
    // Nullifiers for the previously discovered notes
    std::vector<OrchardNullifier> found_nullifiers;
    std::unique_ptr<orchard::OrchardDecodedBlocksBundle> scanned_blocks;
  };

  explicit OrchardBlockScanner(const OrchardFullViewKey& full_view_key);
  virtual ~OrchardBlockScanner();

  // Scans blocks to find incoming notes related to fvk
  // Also checks whether existing notes were spent.
  virtual base::expected<Result, OrchardBlockScanner::ErrorCode> ScanBlocks(
      FrontierChainState chain_state,
      std::vector<zcash::mojom::CompactBlockPtr> blocks);

 private:
  std::unique_ptr<orchard::OrchardBlockDecoder> decoder_;
};

}  // namespace brave_wallet

#endif  // BRAVE_COMPONENTS_BRAVE_WALLET_BROWSER_INTERNAL_ORCHARD_BLOCK_SCANNER_H_
