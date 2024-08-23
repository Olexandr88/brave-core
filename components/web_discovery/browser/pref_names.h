/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#ifndef BRAVE_COMPONENTS_WEB_DISCOVERY_BROWSER_PREF_NAMES_H_
#define BRAVE_COMPONENTS_WEB_DISCOVERY_BROWSER_PREF_NAMES_H_

namespace web_discovery {

// Profile prefs
inline constexpr char kWebDiscoveryEnabled[] = "brave.web_discovery_enabled";

// The following pref values are used for generating
// anonymous signatures for user submissions.
// Since they are not used for encrypting sensitive data,
// they do not require secure storage.
inline constexpr char kCredentialRSAPrivateKey[] =
    "brave.web_discovery.rsa_priv_key";
inline constexpr char kAnonymousCredentialsDict[] =
    "brave.web_discovery.anon_creds";

// Local state
inline constexpr char kPatternsRetrievalTime[] =
    "brave.web_discovery.patterns_retrieval_time";

}  // namespace web_discovery

#endif  // BRAVE_COMPONENTS_WEB_DISCOVERY_BROWSER_PREF_NAMES_H_
