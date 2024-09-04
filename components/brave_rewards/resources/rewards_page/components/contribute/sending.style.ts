/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

import { css, scopedCSS } from '../../lib/scoped_css'

export const style = scopedCSS('contribution-sending', css`
  & {
    --leo-progressring-size: 72px;

    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
  }

  h3 {
    margin-bottom: 72px;
  }
`)

