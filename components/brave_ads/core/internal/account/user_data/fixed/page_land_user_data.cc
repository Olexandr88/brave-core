/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#include "brave/components/brave_ads/core/internal/account/user_data/fixed/page_land_user_data.h"

#include "brave/components/brave_ads/core/internal/common/net/http/http_status_code_util.h"
#include "brave/components/brave_ads/core/internal/settings/settings.h"
#include "brave/components/brave_ads/core/internal/tabs/tab_info.h"

namespace brave_ads {

namespace {

constexpr char kHttpResponseStatusKey[] = "httpResponseStatus";

}  // namespace

base::Value::Dict BuildPageLandUserData(const TabInfo& tab) {
  if (!UserHasJoinedBraveRewards()) {
    return {};
  }

  base::Value::Dict user_data;

  user_data.Set(kHttpResponseStatusKey,
                HttpStatusCodeToString(tab.http_status_code));

  return user_data;
}

}  // namespace brave_ads
