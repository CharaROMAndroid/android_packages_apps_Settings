/*
 * Copyright (C) 2024 GrapheneOS
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

// CharaROM: Duress password base activity
// Reference: https://github.com/GrapheneOS/platform_packages_apps_Settings/commit/0a49b2cb8931c200d0234ccc17cecbedca02ce3c

package com.android.settings.security;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SetupWizardUtils;
import com.android.settings.overlay.FeatureFactory;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.template.DescriptionMixin;
import com.google.android.setupdesign.util.ThemeHelper;

public class DuressPasswordActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);

        setResult(RESULT_OK);
    }

    protected boolean allowNextOnStop;

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && hasUserCredential()) {
            if (allowNextOnStop) {
                allowNextOnStop = false;
            } else {
                // require user credential to be re-entered after activity is backgrounded
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    protected boolean hasUserCredential() {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            /** @see com.android.settings.password.ConfirmDeviceCredentialBaseActivity#onDestroy */
            getMainThreadHandler().postDelayed(() -> {
                System.gc();
                System.runFinalization();
                System.gc();
            }, 5000);
        }
    }

    static void adjustDescriptionStyle(GlifLayout l) {
        l.getMixin(DescriptionMixin.class).getTextView().setTextSize(16f);
    }

    LockPatternUtils getLockPatternUtils() {
        return FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(this);
    }
}
