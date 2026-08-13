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

// CharaROM: Duress password setup activity
// Reference: https://github.com/GrapheneOS/platform_packages_apps_Settings/commit/0a49b2cb8931c200d0234ccc17cecbedca02ce3c

package com.android.settings.security;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.PasswordValidationError;
import com.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class DuressPasswordSetupActivity extends DuressPasswordActivity
        implements TextView.OnEditorActionListener {
    static final String TAG = DuressPasswordSetupActivity.class.getSimpleName();
    static final String EXTRA_USER_CREDENTIAL = "user_credential";
    static final String EXTRA_TITLE_TEXT = "title";
    private boolean isUpdate;

    enum DuressCredentialType {
        PIN(LockscreenCredential::createPin,
                R.id.pin_input, R.id.pin_input_confirmation,
                R.string.lockpassword_confirm_pins_dont_match),
        PASSWORD(LockscreenCredential::createPassword,
                R.id.password_input, R.id.password_input_confirmation,
                R.string.lockpassword_confirm_passwords_dont_match),
        ;

        final Function<String, LockscreenCredential> credentialCreator;
        final @IdRes int mainInputId;
        final @IdRes int confirmationInputId;
        final @StringRes int confirmationMismatchText;

        DuressCredentialType(Function<String, LockscreenCredential> credentialCreator,
                int mainInputId, int confirmationInputId,
                int confirmationMismatchText) {
            this.credentialCreator = credentialCreator;
            this.mainInputId = mainInputId;
            this.confirmationInputId = confirmationInputId;
            this.confirmationMismatchText = confirmationMismatchText;
        }

        LockscreenCredential createCredential(DuressPasswordSetupActivity activity) {
            EditText ed = activity.requireViewById(mainInputId);
            return credentialCreator.apply(ed.getText().toString());
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.duress_password_setup);

        GlifLayout layout = requireViewById(R.id.glif_layout);
        int headerText = getIntent().getIntExtra(EXTRA_TITLE_TEXT, R.string.duress_pwd_action_add);
        isUpdate = headerText == R.string.duress_pwd_action_update;
        layout.setHeaderText(headerText);
        adjustDescriptionStyle(layout);

        FooterBarMixin footerBar = layout.getMixin(FooterBarMixin.class);
        {
            var b = new FooterButton.Builder(this);
            b.setText(isUpdate ? R.string.duress_pwd_update_button : R.string.duress_pwd_add_button);
            b.setButtonType(FooterButton.ButtonType.DONE);
            b.setListener(v -> save());
            b.setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary);
            footerBar.setPrimaryButton(b.build());
        }
        {
            var b = new FooterButton.Builder(this);
            b.setText(R.string.duress_pwd_cancel_button);
            b.setButtonType(FooterButton.ButtonType.CANCEL);
            b.setListener(v -> finish());
            b.setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary);
            footerBar.setSecondaryButton(b.build());
        }
        for (var ct : DuressCredentialType.values()) {
            for (int id : new int[] { ct.mainInputId, ct.confirmationInputId }) {
                EditText ed = requireViewById(id);
                ed.setTag(ct);
                ed.setOnEditorActionListener(this);
            }
        }
    }

    // TextView.OnEditorActionListener
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if ((actionId != EditorInfo.IME_ACTION_NEXT && actionId != EditorInfo.IME_ACTION_DONE)) {
            return false;
        }

        EditText ed = (EditText) v;
        DuressCredentialType ct = (DuressCredentialType) ed.getTag();
        int id = v.getId();

        if (id == ct.mainInputId) {
            return !checkCredentialInputErrors(ct);
        }
        if (id == ct.confirmationInputId) {
            return !checkCredentialConfirmationError(ct);
        }
        return false;
    }

    private void save() {
        boolean credentialCheckRes = true;
        for (DuressCredentialType ct : DuressCredentialType.values()) {
            credentialCheckRes &= checkCredentialInputErrors(ct);
            credentialCheckRes &= checkCredentialConfirmationError(ct);
        }
        if (!credentialCheckRes) {
            return;
        }

        var userCredential = getIntent().getParcelableExtra(EXTRA_USER_CREDENTIAL, LockscreenCredential.class);
        Objects.requireNonNull(userCredential, EXTRA_USER_CREDENTIAL);
        LockscreenCredential pin = DuressCredentialType.PIN.createCredential(this);
        LockscreenCredential password = DuressCredentialType.PASSWORD.createCredential(this);
        LockPatternUtils lockPatternUtils = getLockPatternUtils();

        Runnable save = () -> {
            try {
                lockPatternUtils.setDuressCredentials(userCredential, pin, password);
            } catch (Exception e) {
                Log.e(TAG, "setDuressCredentials failed", e);

                var d = new AlertDialog.Builder(this);
                d.setMessage(getString(R.string.duress_pwd_save_error, e.toString()));
                d.setNeutralButton(R.string.duress_pwd_error_dialog_dismiss, null);
                d.show();
                return;
            }
            Toast.makeText(this,
                    isUpdate ? R.string.duress_pwd_toast_updated : R.string.duress_pwd_toast_added,
                    Toast.LENGTH_LONG).show();
            finish();
        };

        if (isUpdate) {
            save.run();
        } else {
            var warning = new AlertDialog.Builder(this);
            warning.setTitle(R.string.duress_pwd_save_warning_title);
            warning.setMessage(R.string.duress_pwd_save_warning_text);
            warning.setNegativeButton(R.string.duress_pwd_cancel_button, null);
            warning.setPositiveButton(R.string.duress_pwd_proceed_button, (d, w) -> {
                save.run();
            });
            warning.show();
        }
    }

    private boolean checkCredentialInputErrors(DuressCredentialType ct) {
        EditText ed = requireViewById(ct.mainInputId);
        String text = ed.getText().toString();
        LockscreenCredential c = ct.credentialCreator.apply(text);

        List<PasswordValidationError> errors = LockPatternUtils.validateDuressCredential(c);
        String error = null;
        boolean res = true;
        if (!errors.isEmpty()) {
            res = false;
            StringBuilder sb = new StringBuilder();
            for (PasswordValidationError e : errors) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(e.toString());
            }
            error = sb.toString();
        }
        ed.setError(error);
        return res;
    }

    private boolean checkCredentialConfirmationError(DuressCredentialType ct) {
        EditText main = requireViewById(ct.mainInputId);
        EditText confirmation = requireViewById(ct.confirmationInputId);

        boolean res = main.getText().toString().equals(confirmation.getText().toString());
        confirmation.setError(res ? null : getText(ct.confirmationMismatchText));
        return res;
    }
}
