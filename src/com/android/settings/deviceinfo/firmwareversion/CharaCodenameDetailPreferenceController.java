/*
 * Copyright (C) 2019-2021 The BlissRoms Project
 * Copyright (C) 2026 CharaROM Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

// LINT.IfChange
public class CharaCodenameDetailPreferenceController extends BasePreferenceController {

    private static final String TAG = "CharaCodename";

    private static final String KEY_CHARA_CODENAME = "ro.chara.build.codename";

    public CharaCodenameDetailPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        return SystemProperties.get(KEY_CHARA_CODENAME,
                mContext.getString(R.string.chara_build_default));
    }
}
// LINT.ThenChange(LineageVersionDetailPreference.kt)