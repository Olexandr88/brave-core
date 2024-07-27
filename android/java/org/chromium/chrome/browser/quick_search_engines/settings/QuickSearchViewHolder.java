/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.quick_search_engines.settings;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.chromium.chrome.R;

public class QuickSearchViewHolder extends RecyclerView.ViewHolder {
    ImageView mSearchEngineLogo;
    TextView mSearchEngineText;
    MaterialSwitch mSearchEngineSwitch;
    View mView;

    QuickSearchViewHolder(View itemView) {
        super(itemView);
        this.mView = itemView;
        this.mSearchEngineLogo = (ImageView) itemView.findViewById(R.id.search_engine_logo);
        this.mSearchEngineText = (TextView) itemView.findViewById(R.id.search_engine_text);
        this.mSearchEngineSwitch =
                (MaterialSwitch) itemView.findViewById(R.id.search_engine_switch);
    }
}
