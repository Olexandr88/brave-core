/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#include "brave/components/brave_wallet/browser/internal/orchard_block_scanner.h"

namespace brave_wallet {

OrchardBlockScanner::Result::Result() = default;

OrchardBlockScanner::Result::Result(std::vector<OrchardNote> discovered_notes,
                                    std::vector<OrchardNullifier> spent_notes,
                                    std::unique_ptr<orchard::OrchardDecodedBlocksBundle> scanned_blocks)
    : discovered_notes(std::move(discovered_notes)),
      found_nullifiers(std::move(spent_notes)),
      scanned_blocks(std::move(scanned_blocks)) {}

OrchardBlockScanner::Result::Result(OrchardBlockScanner::Result&&) = default;
OrchardBlockScanner::Result& OrchardBlockScanner::Result::operator=(OrchardBlockScanner::Result&&) = default;

// OrchardBlockScanner::Result::Result(const Result&) = default;

// OrchardBlockScanner::Result& OrchardBlockScanner::Result::operator=(
//     const Result&) = default;

OrchardBlockScanner::Result::~Result() = default;

OrchardBlockScanner::OrchardBlockScanner(
    const OrchardFullViewKey& full_view_key)
    : decoder_(orchard::OrchardBlockDecoder::FromFullViewKey(full_view_key)) {}

OrchardBlockScanner::~OrchardBlockScanner() = default;

base::expected<OrchardBlockScanner::Result, OrchardBlockScanner::ErrorCode>
OrchardBlockScanner::ScanBlocks(
    FrontierChainState chain_state,
    std::vector<zcash::mojom::CompactBlockPtr> blocks) {
  std::unique_ptr<orchard::OrchardDecodedBlocksBundle> result =
      decoder_->ScanBlocks(chain_state, blocks);
  if (!result) {
    return base::unexpected(ErrorCode::kInputError);
  }

  if (!result->GetDiscoveredNotes()) {
    return base::unexpected(ErrorCode::kInputError);
  }

  std::vector<OrchardNullifier> found_nullifiers;
  std::vector<OrchardNote> found_notes = result->GetDiscoveredNotes().value();

  for (const auto& block : blocks) {
    for (const auto& tx : block->vtx) {
      // We only scan orchard actions here
      for (const auto& orchard_action : tx->orchard_actions) {
        if (orchard_action->nullifier.size() != kOrchardNullifierSize) {
          return base::unexpected(ErrorCode::kInputError);
        }

        OrchardNullifier nullifier;
        // Nullifier is a public information about some note being spent.
        // -- Here we are trying to find a known spendable notes which nullifier
        // matches nullifier from the processed transaction.
        base::ranges::copy(orchard_action->nullifier,
                           nullifier.nullifier.begin());
        nullifier.block_id = block->height;
        found_nullifiers.push_back(std::move(nullifier));
      }
    }
  }

  return Result({std::move(found_notes), std::move(found_nullifiers),
                 std::move(result)});
}

}  // namespace brave_wallet
