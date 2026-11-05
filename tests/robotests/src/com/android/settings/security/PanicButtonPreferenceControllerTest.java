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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PanicButtonPreferenceControllerTest {

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;

    private PanicButtonPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new PanicButtonPreferenceController(mContext, "panic_button_settings");
    }

    @Test
    public void getAvailabilityStatus_configActivityPresent_available() {
        setResolvedActivity(true);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_missingOrDisabledActivity_unavailable() {
        // PackageManager does not resolve missing/disabled activities without MATCH_DISABLED_COMPONENTS.
        when(mPackageManager.resolveActivity(any(Intent.class), eq(0))).thenReturn(null);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_missingActivityInfo_unavailable() {
        when(mPackageManager.resolveActivity(any(Intent.class), eq(0)))
                .thenReturn(new ResolveInfo());
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_privateActivity_unavailable() {
        setResolvedActivity(false);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_resolvesExplicitConfigurationNotTrigger() {
        mController.getAvailabilityStatus();
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mPackageManager).resolveActivity(captor.capture(), eq(0));
        final Intent intent = captor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getComponent().getPackageName()).isEqualTo("com.android.emergencybutton");
        assertThat(intent.getComponent().getClassName())
                .isEqualTo("com.android.emergencybutton.main.MainActivity");
        assertThat(intent.getExtras()).isNull();
    }

    private void setResolvedActivity(boolean exported) {
        final ResolveInfo resolved = new ResolveInfo();
        resolved.activityInfo = new ActivityInfo();
        resolved.activityInfo.exported = exported;
        when(mPackageManager.resolveActivity(any(Intent.class), eq(0))).thenReturn(resolved);
    }
}
