package com.android.settings.security;

import android.content.Context;
import com.android.settings.core.BasePreferenceController;

public class DuressPasswordPreferenceController extends BasePreferenceController {

    private static final String KEY = "duress_password_settings";

    public DuressPasswordPreferenceController(Context context) {
        super(context, KEY);
    }

    public DuressPasswordPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
