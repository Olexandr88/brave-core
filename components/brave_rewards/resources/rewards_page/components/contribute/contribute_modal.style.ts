/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

import { css, scopedCSS } from '../../lib/scoped_css'

export const style = scopedCSS('contribute-modal', css`
  & {
    overflow: auto;
    max-width: 600px;
  }
`)

