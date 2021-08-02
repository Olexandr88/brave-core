/* Copyright (c) 2020 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "brave/browser/extensions/api/brave_talk_api.h"

#include <memory>
#include <string>

#include "base/environment.h"
#include "base/feature_list.h"
#include "brave/components/brave_talk/browser/regions.h"
#include "brave/components/brave_talk/features.h"
#include "brave/components/ntp_widget_utils/browser/ntp_widget_utils_region.h"
#include "chrome/browser/profiles/profile.h"

namespace extensions {
namespace api {

ExtensionFunction::ResponseAction BraveTalkIsSupportedFunction::Run() {
  Profile* profile = Profile::FromBrowserContext(browser_context());

  if (profile->IsTor()) {
    return RespondNow(Error("Not available in Tor profile"));
  }

  bool is_supported = ntp_widget_utils::IsRegionSupported(
      profile->GetPrefs(), brave_talk::unsupported_regions, false);
  if (is_supported) {
    is_supported = base::FeatureList::IsEnabled(brave_talk::kBraveTalkFeature);
  }

  return RespondNow(OneArgument(base::Value(is_supported)));
}

}  // namespace api
}  // namespace extensions
