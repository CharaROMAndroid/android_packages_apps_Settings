/*
 * Copyright (C) 2026 CharaROM
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.vpn2;

import android.app.settings.SettingsEnums;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.MultiSelectListPreference;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Per-user presentation control. This is not a VPN routing/exclusion setting. */
public class VpnStateHidingSettings extends RestrictedDashboardFragment {
    private static final String KEY = "vpn_state_hidden_packages";

    public VpnStateHidingSettings() {
        super(UserManager.DISALLOW_CONFIG_VPN);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.VPN;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.vpn_state_hiding_settings;
    }

    @Override
    protected String getLogTag() {
        return "VpnStateHiding";
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        final MultiSelectListPreference picker = findPreference(KEY);
        final PackageManager pm = requireContext().getPackageManager();
        final List<ApplicationInfo> apps = new ArrayList<>();
        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            if (UserHandle.getAppId(app.uid) >= Process.FIRST_APPLICATION_UID) apps.add(app);
        }
        apps.sort(new ApplicationInfo.DisplayNameComparator(pm));
        final CharSequence[] labels = new CharSequence[apps.size()];
        final CharSequence[] packages = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            labels[i] = apps.get(i).loadLabel(pm) + " (" + apps.get(i).packageName + ")";
            packages[i] = apps.get(i).packageName;
        }
        picker.setEntries(labels);
        picker.setEntryValues(packages);
        final String stored = Settings.Secure.getString(requireContext().getContentResolver(), KEY);
        picker.setValues(TextUtils.isEmpty(stored) ? Collections.emptySet()
                : new HashSet<>(Arrays.asList(stored.split(","))));
        picker.setEnabled(!isUiRestricted());
        picker.setOnPreferenceChangeListener((preference, value) -> {
            if (isUiRestricted()) return false;
            // MultiSelectListPreference supplies a Set<String>. Sort for deterministic storage.
            @SuppressWarnings("unchecked")
            final Set<String> selected = (Set<String>) value;
            final List<String> ordered = new ArrayList<>(selected);
            Collections.sort(ordered);
            return Settings.Secure.putString(requireContext().getContentResolver(), KEY,
                    TextUtils.join(",", ordered));
        });
    }
}
