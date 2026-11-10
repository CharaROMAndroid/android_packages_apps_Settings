/*
 * Copyright (C) 2026 CharaROM
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.development.qstile;

import static com.android.settingslib.development.AbstractEnableAdbPreferenceController.ADB_SETTING_OFF;
import static com.android.settingslib.development.AbstractEnableAdbPreferenceController.ADB_SETTING_ON;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;

import com.android.settingslib.development.DevelopmentSettingsEnabler;

/**
 * USB debugging tile. Turning off is immediate. Turning on needs the same conditions as the
 * Developer options switch: admin user, no debugging restriction, developer options on,
 * unlocked device, and the same ADB warning confirmation.
 */
public class AdbUsbDebuggingDevelopmentTile extends DevelopmentTiles {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ContentObserver mObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            refresh();
        }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, mObserver);
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED), false,
                mObserver);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    protected boolean isEnabled() {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED,
                ADB_SETTING_OFF) != ADB_SETTING_OFF;
    }

    private boolean canEnable() {
        final UserManager um = getSystemService(UserManager.class);
        return um.isAdminUser()
                && !um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)
                && DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this);
    }

    @Override
    public void refresh() {
        super.refresh();
        // Developer options off is handled by super (tile removed). Restricted or non-admin
        // users get an unavailable tile; user restrictions are re-read on every listen.
        if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this) && !canEnable()
                && getQsTile() != null) {
            getQsTile().setState(Tile.STATE_UNAVAILABLE);
            getQsTile().updateTile();
        }
    }

    @Override
    public void onClick() {
        final Tile tile = getQsTile();
        if (tile.getState() == Tile.STATE_UNAVAILABLE) {
            return;
        }
        if (isEnabled()) {
            setIsEnabled(false);
            refresh();
            return;
        }
        if (!canEnable()) {
            refresh();
            return;
        }
        // Never enable from the lock screen; re-check everything after unlock and confirm.
        unlockAndRun(() -> {
            if (!canEnable()) {
                refresh();
                return;
            }
            showDialog(new AlertDialog.Builder(this)
                    .setTitle(com.android.settingslib.R.string.adb_warning_title)
                    .setMessage(com.android.settingslib.R.string.adb_warning_message)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        if (canEnable() && !getSystemService(KeyguardManager.class)
                                .isKeyguardLocked()) {
                            setIsEnabled(true);
                        }
                        refresh();
                    })
                    .setNegativeButton(android.R.string.cancel, (d, w) -> refresh())
                    .create());
        });
    }

    @Override
    public void setIsEnabled(boolean enabled) {
        if (enabled && !canEnable()) {
            return;
        }
        Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED,
                enabled ? ADB_SETTING_ON : ADB_SETTING_OFF);
        sendBroadcast(new Intent(
                com.android.settingslib.development.AbstractEnableAdbPreferenceController
                        .ACTION_ENABLE_ADB_STATE_CHANGED).setPackage(getPackageName()));
    }
}
