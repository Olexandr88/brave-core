/* Copyright (c) 2023 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#include "brave/components/brave_rewards/core/endpoints/post_create_transaction/post_create_transaction.h"

#include <utility>

namespace brave_rewards::internal::endpoints {
PostCreateTransaction::PostCreateTransaction(
    RewardsEngineImpl& engine,
    std::string&& token,
    std::string&& address,
    mojom::ExternalTransactionPtr transaction)
    : RequestBuilder(engine),
      token_(std::move(token)),
      address_(std::move(address)),
      transaction_(std::move(transaction)) {
  DCHECK(transaction_);
  DCHECK(transaction_->transaction_id.empty());
  DCHECK(!transaction_->contribution_id.empty());
  DCHECK(!transaction_->destination.empty());
  DCHECK(!transaction_->amount.empty());
}

PostCreateTransaction::~PostCreateTransaction() = default;

std::string PostCreateTransaction::ContentType() const {
  return kApplicationJson;
}

}  // namespace brave_rewards::internal::endpoints
