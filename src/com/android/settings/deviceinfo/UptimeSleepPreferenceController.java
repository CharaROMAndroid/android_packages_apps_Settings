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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/** Single "Uptime" row that also shows how much of it was spent in deep sleep. */
public class UptimeSleepPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {

    private static final String KEY_UPTIME = "up_time";
    private static final long UPDATE_INTERVAL_MS = DateUtils.SECOND_IN_MILLIS;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            updateTimes();
            mHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };
    private Preference mPreference;

    public UptimeSleepPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_UPTIME;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_UPTIME);
        updateTimes();
    }

    @Override
    public void onStart() {
        mHandler.post(mUpdater);
    }

    @Override
    public void onStop() {
        mHandler.removeCallbacks(mUpdater);
    }

    private void updateTimes() {
        if (mPreference == null) {
            return;
        }
        final long elapsed = SystemClock.elapsedRealtime();
        final long awake = SystemClock.uptimeMillis();
        final long sleep = Math.max(0L, elapsed - awake);
        final int percent = elapsed > 0 ? (int) (sleep * 100L / elapsed) : 0;
        mPreference.setSummary(mContext.getString(R.string.status_up_time_with_sleep,
                DateUtils.formatElapsedTime(elapsed / 1000L),
                DateUtils.formatElapsedTime(sleep / 1000L),
                percent));
    }
}
