/*
 * Copyright (C) 2026 CharaROM
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.development;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.crdroid.settings.fragments.misc.StateHidingAppPicker;

/** Selects apps whose launch turns off USB and wireless debugging. Never re-enables. */
public class AdbAutoDisableSettings extends RestrictedDashboardFragment {
    public AdbAutoDisableSettings() {
        super(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVELOPMENT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adb_auto_disable_settings;
    }

    @Override
    protected String getLogTag() {
        return "AdbAutoDisable";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        // Keep the dashboard's preference RecyclerView initialized for its lifecycle,
        // observers and restriction handling; the visible content is the shared app picker.
        final View preferences = super.onCreateView(inflater, container, state);
        preferences.setVisibility(View.GONE);
        final FrameLayout root = new FrameLayout(requireContext());
        root.addView(preferences, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(StateHidingAppPicker.createView(requireContext(), getLifecycle(),
                "adb_auto_disable_apps", R.string.adb_auto_disable_title,
                R.string.adb_auto_disable_explanation, this::isUiRestricted),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }
}
