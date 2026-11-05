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

package com.android.settings.security;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import com.android.settings.core.BasePreferenceController;

/** Opens the existing Panic app configuration, not a destructive panic trigger. */
public class PanicButtonPreferenceController extends BasePreferenceController {

    public PanicButtonPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        final Intent intent = new Intent(Intent.ACTION_VIEW).setClassName(
                "com.android.emergencybutton", "com.android.emergencybutton.main.MainActivity");
        final ResolveInfo resolved = mContext.getPackageManager().resolveActivity(intent, 0);
        return resolved != null && resolved.activityInfo != null && resolved.activityInfo.exported
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
