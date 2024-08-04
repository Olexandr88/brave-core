// Copyright (c) 2022 The Brave Authors. All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this file,
// You can obtain one at https://mozilla.org/MPL/2.0/.

"use strict";

// Ensure this module only gets included once. This is
// required for user scripts injected into all frames.
window.__brave__.includeOnce("PrintScript", function($) {
  window.print = $(function() {
    $.postNativeMessage('printScriptHandler', {"securityToken": SECURITY_TOKEN});
  });
});
