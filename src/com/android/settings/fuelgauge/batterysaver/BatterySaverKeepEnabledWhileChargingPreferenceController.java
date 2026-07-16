/*
 * Copyright (C) 2026 Project CiRCLE
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

package com.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.Global;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controls whether Battery Saver stays enabled while the device is charging, instead of being
 * automatically turned off as soon as a charger is plugged in.
 */
public class BatterySaverKeepEnabledWhileChargingPreferenceController
        extends TogglePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private Context mContext;

    public BatterySaverKeepEnabledWhileChargingPreferenceController(
            Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Global.LOW_POWER_MODE_KEEP_ENABLED_WHILE_CHARGING,
                        0)
                == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Global.LOW_POWER_MODE_KEEP_ENABLED_WHILE_CHARGING,
                isChecked ? 1 : 0);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }
}
