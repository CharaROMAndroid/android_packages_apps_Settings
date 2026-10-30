/*
 * Copyright (C) 2026 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.os.SystemProperties;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NativeDebugPreferenceControllerTest {
    private static final String PROPERTY = "persist.native_debug";
    private NativeDebugPreferenceController mController;
    private PreferenceScreen mScreen;
    private PreferenceCategory mCategory;
    private SwitchPreference mSwitch;
    private UserManager mUserManager;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        mUserManager = mock(UserManager.class);
        shadowOf((Application) context).setSystemService(Context.USER_SERVICE, mUserManager);
        when(mUserManager.isAdminUser()).thenReturn(true);
        mScreen = mock(PreferenceScreen.class);
        mCategory = mock(PreferenceCategory.class);
        mSwitch = mock(SwitchPreference.class);
        when(mScreen.findPreference("security_category")).thenReturn(mCategory);
        when(mScreen.findPreference("native_debug")).thenReturn(mSwitch);
        when(mCategory.findPreference("native_debug")).thenReturn(mSwitch);
        when(mSwitch.getKey()).thenReturn("native_debug");
        SystemProperties.set(PROPERTY, "");
        mController = new NativeDebugPreferenceController(context);
    }

    @Test
    public void openingAndResuming_withoutSavedChoice_remainsRestrictedAndDoesNotPersist() {
        mController.displayPreference(mScreen);
        mController.onResume();
        mController.onResume();
        verify(mSwitch, org.mockito.Mockito.atLeastOnce()).setChecked(false);
        assertThat(SystemProperties.get(PROPERTY)).isEmpty();
    }

    @Test
    public void opening_existingCompatibility_preservesChoice() {
        SystemProperties.set(PROPERTY, "true");
        mController.displayPreference(mScreen);
        mController.onResume();
        verify(mSwitch, org.mockito.Mockito.atLeastOnce()).setChecked(true);
        assertThat(SystemProperties.get(PROPERTY)).isEqualTo("true");
    }

    @Test
    public void opening_existingRestricted_preservesChoice() {
        SystemProperties.set(PROPERTY, "false");
        mController.displayPreference(mScreen);
        mController.onResume();
        verify(mSwitch, org.mockito.Mockito.atLeastOnce()).setChecked(false);
        assertThat(SystemProperties.get(PROPERTY)).isEqualTo("false");
    }

    @Test
    public void nonCanonicalBoolean_doesNotClaimCompatibilityOrRewriteProperty() {
        SystemProperties.set(PROPERTY, "1");
        mController.displayPreference(mScreen);
        mController.onResume();
        verify(mSwitch, org.mockito.Mockito.atLeastOnce()).setChecked(false);
        assertThat(SystemProperties.get(PROPERTY)).isEqualTo("1");
    }

    @Test
    public void explicitChoice_usesRequestedValue_notInverseOfOldSwitchState() {
        when(mSwitch.isChecked()).thenReturn(true);
        assertThat(mController.onPreferenceChange(mSwitch, true)).isTrue();
        assertThat(SystemProperties.get(PROPERTY)).isEqualTo("true");
        when(mSwitch.isChecked()).thenReturn(false);
        assertThat(mController.onPreferenceChange(mSwitch, false)).isTrue();
        assertThat(SystemProperties.get(PROPERTY)).isEqualTo("false");
    }

    @Test
    public void secondaryUser_cannotChangePolicy_andRepeatedResumeIsSafe() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        mController.displayPreference(mScreen);
        mController.onResume();
        mController.onResume();
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.onPreferenceChange(mSwitch, true)).isFalse();
        assertThat(SystemProperties.get(PROPERTY)).isEmpty();
        verify(mSwitch, never()).setChecked(anyBoolean());
    }

    @Test
    public void unrelatedOrInvalidChanges_areRejected() {
        Preference other = mock(Preference.class);
        when(other.getKey()).thenReturn("other");
        assertThat(mController.onPreferenceChange(other, true)).isFalse();
        assertThat(mController.onPreferenceChange(mSwitch, "true")).isFalse();
        assertThat(mController.onPreferenceChange(mSwitch, null)).isFalse();
        assertThat(SystemProperties.get(PROPERTY)).isEmpty();
    }

    @Test
    public void missingCategoryOrSwitch_doesNotCrash() {
        when(mScreen.findPreference("security_category")).thenReturn(null);
        mController.displayPreference(mScreen);
        mController.onResume();
        when(mScreen.findPreference("security_category")).thenReturn(mCategory);
        when(mCategory.findPreference("native_debug")).thenReturn(null);
        mController.displayPreference(mScreen);
        mController.onResume();
    }
}
