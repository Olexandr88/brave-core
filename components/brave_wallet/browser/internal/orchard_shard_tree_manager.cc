/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#include "brave/components/brave_wallet/browser/internal/orchard_shard_tree_manager.h"

#include "brave/components/brave_wallet/common/zcash_utils.h"

namespace brave_wallet {

// static
std::unique_ptr<OrchardShardTreeManager> OrchardShardTreeManager::Create(
    std::unique_ptr<OrchardShardTreeDelegate> delegate) {
  auto shard_tree = orchard::OrchardShardTree::Create(std::move(delegate));
  if (!shard_tree) {
    return nullptr;
  }
  return std::make_unique<OrchardShardTreeManager>(std::move(shard_tree));
}

OrchardShardTreeManager::OrchardShardTreeManager(
    std::unique_ptr<::brave_wallet::orchard::OrchardShardTree> shard_tree) {
  orchard_shard_tree_ = std::move(shard_tree);
}

OrchardShardTreeManager::~OrchardShardTreeManager() {}

bool OrchardShardTreeManager::InsertCommitments(
    OrchardBlockScanner::Result result) {
  return orchard_shard_tree_->ApplyScanResults(
      std::move(result.scanned_blocks));
}

}  // namespace brave_wallet
