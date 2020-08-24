/* Copyright (c) 2020 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef BRAVE_COMPONENTS_IPFS_COMMON_IPFS_CONSTANTS_H_
#define BRAVE_COMPONENTS_IPFS_COMMON_IPFS_CONSTANTS_H_

namespace ipfs {
extern const char kHttpAPIServerEndpoint[];
extern const char kSwarmPeersAPIURL[];

enum class IPFSResolveMethodTypes {
  IPFS_GATEWAY,
  IPFS_LOCAL,
  IPFS_DISABLED,
};

}  // namespace ipfs

#endif  // BRAVE_COMPONENTS_IPFS_COMMON_IPFS_CONSTANTS_H_
