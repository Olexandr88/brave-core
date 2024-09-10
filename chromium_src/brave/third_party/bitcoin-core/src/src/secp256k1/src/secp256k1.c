/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

#define SECP256K1_BUILD
#include "brave/third_party/bitcoin-core/src/src/secp256k1/include/secp256k1.h"

// Optimized scalar_impl.h causes build failure on arm64 Release Android and Linux, 
// macOS arm64 has test failures.
// Started to happen after llvm clang update
// Probably caused by https://github.com/chromium/chromium/commit/db80d4cfe61e100db0eda975c5d986edc9140fa1
// This change may be reverted if the issue will be fixed in future revisions.

#pragma clang optimize off
#include "brave/third_party/bitcoin-core/src/src/secp256k1/src/scalar_impl.h"
#pragma clang optimize on

// Avoid redefining, as it defining at the original secp256k1.c
#undef SECP256K1_BUILD

#include "src/brave/third_party/bitcoin-core/src/src/secp256k1/src/secp256k1.c"  // IWYU pragma: export
