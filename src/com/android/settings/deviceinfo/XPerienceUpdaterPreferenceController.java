/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2017 The XPerience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import com.android.settings.development.DevelopmentSettings;

public class XPerienceUpdaterPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {

    private static final String KEY_XPE_UPDATES = "xpe_updates";
    Context mContext;

    public XPerienceUpdaterPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean isAvailable() {
        return DevelopmentSettings.isPackageInstalled(mContext, "mx.xperience.updater");
    }

    @Override
    public String getPreferenceKey() {
        return KEY_XPE_UPDATES;
    }
}