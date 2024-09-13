// Copyright (c) 2024 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this file,
// You can obtain one at https://mozilla.org/MPL/2.0/.

#include "brave/components/brave_wallet/browser/zcash/rust/orchard_decoded_blocks_bunde_impl.h"

namespace brave_wallet::orchard {

OrchardDecodedBlocksBundleImpl::OrchardDecodedBlocksBundleImpl(rust::Box<BatchOrchardDecodeBundle> v) : batch_decode_result_(std::move(v)) {

}

OrchardDecodedBlocksBundleImpl::~OrchardDecodedBlocksBundleImpl() {

}

std::optional<std::vector<::brave_wallet::OrchardNote>> OrchardDecodedBlocksBundleImpl::GetDiscoveredNotes() {
  std::vector<OrchardNote> result;

  for (size_t i = 0; i < batch_decode_result_->size(); i++) {
    result.emplace_back(OrchardNote(
        {batch_decode_result_->note_block_height(i),
         batch_decode_result_->note_nullifier(i),
         batch_decode_result_->note_value(i)}));
  }

  return result;
}

BatchOrchardDecodeBundle& OrchardDecodedBlocksBundleImpl::GetDecodeBundle() {
  return *batch_decode_result_;
}

}  // namespace brave_wallet::orchard
