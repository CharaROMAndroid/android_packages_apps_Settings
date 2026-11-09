/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import static android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Blur effects on/off for the whole phone (notification shade, recents, dialogs, apps that
 * ask for window blur). It drives Settings.Global.DISABLE_WINDOW_BLURS, the same setting
 * as "Allow window-level blurs" in Developer options, which WindowManager applies to every
 * window; the default (unset or 0) is blur on.
 */
public class BlurEffectsPreferenceController extends TogglePreferenceController {

    public BlurEffectsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        // Only offered where SurfaceFlinger can blur (ro.surface_flinger.supports_background_blur).
        return CROSS_WINDOW_BLUR_SUPPORTED ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DISABLE_WINDOW_BLURS, 0) == 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DISABLE_WINDOW_BLURS, isChecked ? 0 : 1);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
