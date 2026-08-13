/*
 * Copyright (C) 2026 CharaROM
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

package com.android.settings.datetime;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.EditTextPreference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class NtpServerPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener, LifecycleObserver {

    private static final String DEFAULT_NTP_SERVER = "pool.ntp.org";

    private EditTextPreference mPreference;
    private ContentObserver mAutoTimeObserver;

    public NtpServerPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = (EditTextPreference) preference;
        String currentServer = getNtpServer();
        mPreference.setText(currentServer);
        mPreference.setSummary(currentServer);

        // Disable when auto time is off
        boolean autoTimeEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME, 0) != 0;
        mPreference.setEnabled(autoTimeEnabled);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (mAutoTimeObserver == null) {
            mAutoTimeObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    if (mPreference != null) {
                        boolean autoTimeEnabled = Settings.Global.getInt(
                                mContext.getContentResolver(),
                                Settings.Global.AUTO_TIME, 0) != 0;
                        mPreference.setEnabled(autoTimeEnabled);
                    }
                }
            };
        }
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AUTO_TIME),
                false, mAutoTimeObserver);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        if (mAutoTimeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mAutoTimeObserver);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String server = (String) newValue;
        if (TextUtils.isEmpty(server)) {
            Settings.Global.putString(mContext.getContentResolver(),
                    Settings.Global.NTP_SERVER, null);
        } else {
            Settings.Global.putString(mContext.getContentResolver(),
                    Settings.Global.NTP_SERVER, server);
        }
        updateState(preference);
        return true;
    }

    private String getNtpServer() {
        String server = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.NTP_SERVER);
        if (TextUtils.isEmpty(server)) {
            return DEFAULT_NTP_SERVER;
        }
        return server;
    }
}
