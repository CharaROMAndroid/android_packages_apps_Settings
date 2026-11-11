/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.security;

import android.content.Context;
import android.os.SystemProperties;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class NativeDebugPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnResume, Preference.OnPreferenceChangeListener {

    private static final String SYS_KEY_NATIVE_DEBUG = "persist.native_debug";
    private static final String PREF_KEY_NATIVE_DEBUG = "native_debug";

    private PreferenceScreen mScreen;
    private SwitchPreference mNativeDebug;
    private final UserManager mUm;

    public NativeDebugPreferenceController(Context context) {
        super(context);
        mUm = UserManager.get(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        updatePreferenceState();
    }

    @Override
    public boolean isAvailable() {
        return mUm.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_NATIVE_DEBUG;
    }

    private void updatePreferenceState() {
        if (mScreen == null || !isAvailable()) {
            return;
        }

        mNativeDebug = mScreen.findPreference(PREF_KEY_NATIVE_DEBUG);
        if (mNativeDebug == null) {
            return;
        }
        mNativeDebug.setOnPreferenceChangeListener(this);
        // Match init's canonical "true" compatibility trigger. An absent or invalid
        // property leaves the kernel at the restricted scope-1 boot default.
        mNativeDebug.setChecked(
                "true".equals(SystemProperties.get(SYS_KEY_NATIVE_DEBUG, "false")));
    }

    @Override
    public void onResume() {
        updatePreferenceState();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (!PREF_KEY_NATIVE_DEBUG.equals(preference.getKey())
                || !(value instanceof Boolean) || !isAvailable()) {
            return false;
        }
        // Only an explicit user action changes the persisted boot/runtime policy.
        // ON is compatibility (scope 0); OFF is restricted (scope 1), not scope 2.
        SystemProperties.set(SYS_KEY_NATIVE_DEBUG, Boolean.toString((Boolean) value));
        return true;
    }
}
