/* Copyright (c) 2022 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

namespace github {
    export function distill() {
        return `This is GitHub. There are ${document.all.length} elements on this page. You're currently on path ${window.location.pathname}.`;
    }
}

// @ts-expect-error
// This file will be wrapped in an IIFE by Webpack
return github.distill();