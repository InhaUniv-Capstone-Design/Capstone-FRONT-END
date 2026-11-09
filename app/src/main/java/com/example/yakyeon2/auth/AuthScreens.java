package com.example.yakyeon2.auth;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.yakyeon2.R;
import com.example.yakyeon2.api.AuthContracts;
import com.example.yakyeon2.data.AccountType;
import com.example.yakyeon2.dev.ManualCheckGate;
import com.example.yakyeon2.ui.YakyeonTextField;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class AuthScreens {

    public interface Host {
        void showMessage(View anchor, CharSequence message);

        void showNotice(CharSequence title, CharSequence message, Runnable onConfirm);

        void onLogin(String id, String password);

        void onManualCheck();

        void onRequestSignup();

        void onConsentAccepted();

        void onTypeNext(AccountType type);

        void onContinueCaregiver();

        void onOpenRecovery(AuthFlow.RecoveryTab tab);

        void onRecoveryTab(AuthFlow.RecoveryTab tab);

        void onFindLoginId(String name, String phone);

        void onResetIdentityNext(String loginId, String phone);

        void onRecoveryPhoneTyped(String phone);

        void onRecoveryVerifiedNext();

        void onResetPassword(String loginId, String phone, String password, String passwordConfirm);

        void onOpenLoginFromRecovery();

        void onStartPasswordReset();

        void onRequestPhoneCode(String phone);

        void onPhoneDraftChanged();

        void onPhoneCodeDraft(String phone, String code);

        void onCheckLoginId(String loginId);

        void onSignupSubmit(
                String name,
                String birthdate,
                String phone,
                String code,
                String id,
                String password,
                String passwordConfirm
        );
    }

    private AuthScreens() {
    }

    public static View login(AppCompatActivity activity, Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_login, null);
        YakyeonTextField id = root.findViewById(R.id.field_login_id);
        YakyeonTextField password = root.findViewById(R.id.field_login_password);
        prepareField(id, activity.getString(R.string.auth_id_hint), false);
        prepareField(password, activity.getString(R.string.auth_password_hint), true);
        id.useDirectTextInput();
        id.setCredentialLimit(AuthValidation.LOGIN_ID_MAX);
        password.setCredentialLimit(AuthValidation.LOGIN_PASSWORD_MAX);
        roundField(id);
        roundField(password);
        root.findViewById(R.id.button_login).setOnClickListener(v ->
                host.onLogin(id.getText(), password.getText()));
        ManualCheckGate.attachLoginButton(activity, root, host::onManualCheck);
        root.findViewById(R.id.button_open_signup).setOnClickListener(v -> host.onRequestSignup());
        root.findViewById(R.id.tab_find_id).setOnClickListener(v ->
                host.onOpenRecovery(AuthFlow.RecoveryTab.ID));
        root.findViewById(R.id.tab_find_password).setOnClickListener(v ->
                host.onOpenRecovery(AuthFlow.RecoveryTab.PASSWORD));
        return root;
    }

    public static View accountRecovery(
            AppCompatActivity activity,
            Host host,
            AuthFlow.RecoveryTab selected,
            RecoveryForm form
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_account_recovery, null);
        boolean[] findId = {selected != AuthFlow.RecoveryTab.PASSWORD};
        View idPanel = root.findViewById(R.id.panel_find_id);
        View passwordPanel = root.findViewById(R.id.panel_find_password);
        TextView idLabel = root.findViewById(R.id.label_find_id);
        TextView passwordLabel = root.findViewById(R.id.label_find_password);
        View idMark = root.findViewById(R.id.indicator_find_id);
        View passwordMark = root.findViewById(R.id.indicator_find_password);
        int brand = activity.getColor(R.color.yakyeon_brand);
        int muted = activity.getColor(R.color.yakyeon_muted);
        Runnable paint = () -> {
            idPanel.setVisibility(findId[0] ? View.VISIBLE : View.GONE);
            passwordPanel.setVisibility(findId[0] ? View.GONE : View.VISIBLE);
            idMark.setVisibility(findId[0] ? View.VISIBLE : View.INVISIBLE);
            passwordMark.setVisibility(findId[0] ? View.INVISIBLE : View.VISIBLE);
            idLabel.setTextColor(findId[0] ? brand : muted);
            passwordLabel.setTextColor(findId[0] ? muted : brand);
            idLabel.setTypeface(null, findId[0] ? Typeface.BOLD : Typeface.NORMAL);
            passwordLabel.setTypeface(null, findId[0] ? Typeface.NORMAL : Typeface.BOLD);
        };
        root.findViewById(R.id.tab_find_id).setOnClickListener(v -> {
            if (findId[0]) {
                return;
            }
            findId[0] = true;
            host.onRecoveryTab(AuthFlow.RecoveryTab.ID);
            paint.run();
        });
        root.findViewById(R.id.tab_find_password).setOnClickListener(v -> {
            if (!findId[0]) {
                return;
            }
            findId[0] = false;
            host.onRecoveryTab(AuthFlow.RecoveryTab.PASSWORD);
            paint.run();
        });
        paint.run();

        YakyeonTextField name = root.findViewById(R.id.field_find_name);
        YakyeonTextField phone = root.findViewById(R.id.field_find_phone);
        YakyeonTextField id = root.findViewById(R.id.field_reset_id);
        YakyeonTextField resetPhone = root.findViewById(R.id.field_reset_phone);
        prepareField(name, activity.getString(R.string.auth_name_hint), false);
        prepareField(phone, activity.getString(R.string.auth_phone_hint), false);
        prepareField(id, activity.getString(R.string.auth_id_rule_hint), false);
        id.useDirectTextInput();
        prepareField(resetPhone, activity.getString(R.string.auth_phone_hint), false);
        phone.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        resetPhone.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        name.setText(form.findName());
        phone.setText(form.findPhone());
        id.setText(form.resetLoginId());
        resetPhone.setText(form.resetPhone());
        watchIdentity(name, () -> form.setFindName(name.getText()));
        watchIdentity(phone, () -> {
            form.setFindPhone(phone.getText());
            host.onRecoveryPhoneTyped(phone.getText());
        });
        watchIdentity(id, () -> form.setResetLoginId(id.getText()));
        watchIdentity(resetPhone, () -> {
            form.setResetPhone(resetPhone.getText());
            host.onRecoveryPhoneTyped(resetPhone.getText());
        });
        root.findViewById(R.id.button_find_id).setOnClickListener(v ->
                host.onFindLoginId(name.getText(), phone.getText()));
        root.findViewById(R.id.button_reset_next).setOnClickListener(v ->
                host.onResetIdentityNext(id.getText(), resetPhone.getText()));
        return root;
    }

    public static View recoveryVerify(
            AppCompatActivity activity,
            Host host,
            RecoveryForm form,
            AuthFlow.RecoveryTab tab,
            AuthContracts.PhoneVerification phoneVerification,
            String verifiedPhone,
            boolean phoneVerified,
            long verificationExpiresAt
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_recovery_verify, null);
        YakyeonTextField phone = root.findViewById(R.id.field_phone);
        YakyeonTextField code = root.findViewById(R.id.field_verification_code);
        prepareField(phone, activity.getString(R.string.auth_phone_hint), false);
        prepareField(code, activity.getString(R.string.auth_code_hint), false);
        phone.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        code.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        String stored = tab == AuthFlow.RecoveryTab.PASSWORD ? form.resetPhone() : form.findPhone();
        phone.setText(stored);
        root.findViewById(R.id.button_request_code).setOnClickListener(v ->
                host.onRequestPhoneCode(phone.getText()));
        phone.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (tab == AuthFlow.RecoveryTab.PASSWORD) {
                    form.setResetPhone(phone.getText());
                } else {
                    form.setFindPhone(phone.getText());
                }
                host.onPhoneDraftChanged();
            }
        });
        code.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                host.onPhoneCodeDraft(phone.getText(), code.getText());
            }
        });
        root.findViewById(R.id.button_recovery_next).setOnClickListener(v -> host.onRecoveryVerifiedNext());
        applyPhoneVerification(activity, root, phoneVerification, host);
        paintVerification(root, verifiedPhone, phoneVerified, verificationExpiresAt);
        return root;
    }

    public static View recoveryPassword(AppCompatActivity activity, Host host, RecoveryForm form) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_recovery_password, null);
        YakyeonTextField id = root.findViewById(R.id.field_reset_id);
        YakyeonTextField password = root.findViewById(R.id.field_reset_password);
        YakyeonTextField confirm = root.findViewById(R.id.field_reset_confirm);
        TextView error = root.findViewById(R.id.text_reset_password_error);
        View submit = root.findViewById(R.id.button_reset_password);
        prepareField(id, activity.getString(R.string.auth_id_rule_hint), false);
        id.useDirectTextInput();
        prepareField(password, activity.getString(R.string.auth_password_rule_hint), true);
        prepareField(confirm, activity.getString(R.string.auth_password_confirm_hint), true);
        id.setText(form.resetLoginId());
        Runnable refresh = () -> refreshResetPassword(activity, error, submit, password.getText(), confirm.getText());
        watchIdentity(id, () -> form.setResetLoginId(id.getText()));
        watchIdentity(password, refresh);
        watchIdentity(confirm, refresh);
        refresh.run();
        submit.setOnClickListener(v -> {
            if (!submit.isEnabled()) {
                return;
            }
            host.onResetPassword(id.getText(), form.resetPhone(), password.getText(), confirm.getText());
        });
        return root;
    }

    public static void refreshResetPassword(
            AppCompatActivity activity,
            TextView error,
            View submit,
            String password,
            String confirm
    ) {
        AuthValidation.PasswordIssue issue = AuthValidation.passwordIssue(password);
        boolean mismatch = issue == AuthValidation.PasswordIssue.OK
                && confirm != null
                && !confirm.isEmpty()
                && !password.equals(confirm);
        int color = activity.getColor(mismatch || isPasswordError(issue)
                ? R.color.yakyeon_danger
                : R.color.yakyeon_muted);
        error.setTextColor(color);
        if (mismatch) {
            error.setText(activity.getString(R.string.auth_reset_mismatch));
        } else {
            error.setText(passwordIssueText(activity, issue));
        }
        submit.setEnabled(AuthValidation.canSubmitReset(password, confirm));
    }

    public static View recoveryResult(
            AppCompatActivity activity,
            Host host,
            List<AuthContracts.FoundAccount> accounts
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_recovery_result, null);
        LinearLayout list = root.findViewById(R.id.list_found_accounts);
        TextView empty = root.findViewById(R.id.text_found_empty);
        boolean none = accounts == null || accounts.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        list.setVisibility(none ? View.GONE : View.VISIBLE);
        if (!none) {
            for (AuthContracts.FoundAccount account : accounts) {
                View row = LayoutInflater.from(activity).inflate(R.layout.item_found_account, list, false);
                TextView type = row.findViewById(R.id.text_account_type);
                TextView loginId = row.findViewById(R.id.text_account_login_id);
                type.setText(accountLabel(activity, account.accountType));
                loginId.setText(account.loginId);
                list.addView(row);
            }
        }
        root.findViewById(R.id.button_recovery_login).setOnClickListener(v -> host.onOpenLoginFromRecovery());
        root.findViewById(R.id.button_recovery_to_reset).setOnClickListener(v -> host.onStartPasswordReset());
        return root;
    }

    public static View recoveryDone(AppCompatActivity activity, Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_recovery_done, null);
        root.findViewById(R.id.button_recovery_login).setOnClickListener(v -> host.onOpenLoginFromRecovery());
        return root;
    }

    private static void watchIdentity(YakyeonTextField field, Runnable after) {
        field.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                after.run();
            }
        });
    }

    private static boolean isPasswordError(AuthValidation.PasswordIssue issue) {
        return issue != AuthValidation.PasswordIssue.EMPTY && issue != AuthValidation.PasswordIssue.OK;
    }

    private static String passwordIssueText(AppCompatActivity activity, AuthValidation.PasswordIssue issue) {
        switch (issue) {
            case TOO_SHORT:
                return activity.getString(R.string.auth_password_too_short);
            case TOO_LONG:
                return activity.getString(R.string.auth_password_too_long);
            case NEED_LETTER:
                return activity.getString(R.string.auth_password_need_letter);
            case NEED_DIGIT:
                return activity.getString(R.string.auth_password_need_digit);
            case NEED_SPECIAL:
                return activity.getString(R.string.auth_password_need_special);
            case EMPTY:
            case OK:
            default:
                return activity.getString(R.string.auth_password_rule_caption);
        }
    }

    private static String accountLabel(AppCompatActivity activity, String accountType) {
        if (com.example.yakyeon2.data.AccountRoles.PATIENT.equals(accountType)) {
            return activity.getString(R.string.auth_account_patient);
        }
        if (com.example.yakyeon2.data.AccountRoles.GUARDIAN.equals(accountType)) {
            return activity.getString(R.string.auth_account_guardian);
        }
        return activity.getString(R.string.auth_account_other);
    }

    public static View signupType(AppCompatActivity activity, AccountType selected, Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_signup_type, null);
        RadioButton self = root.findViewById(R.id.radio_self);
        RadioButton caregiver = root.findViewById(R.id.radio_caregiver);
        self.setButtonTintList(null);
        caregiver.setButtonTintList(null);
        MaterialCardView selfCard = root.findViewById(R.id.card_self);
        MaterialCardView caregiverCard = root.findViewById(R.id.card_caregiver);
        int idleStroke = activity.getColor(R.color.yakyeon_card_line);
        int selectedStroke = activity.getColor(R.color.yakyeon_primary);
        int strokeWidth = (int) (1 * activity.getResources().getDisplayMetrics().density);
        Runnable paint = () -> {
            selfCard.setStrokeWidth(strokeWidth);
            caregiverCard.setStrokeWidth(strokeWidth);
            selfCard.setStrokeColor(self.isChecked() ? selectedStroke : idleStroke);
            caregiverCard.setStrokeColor(caregiver.isChecked() ? selectedStroke : idleStroke);
        };
        Runnable selectSelf = () -> {
            self.setChecked(true);
            caregiver.setChecked(false);
            paint.run();
        };
        Runnable selectCaregiver = () -> {
            self.setChecked(false);
            caregiver.setChecked(true);
            paint.run();
        };
        selfCard.setOnClickListener(v -> selectSelf.run());
        caregiverCard.setOnClickListener(v -> selectCaregiver.run());
        self.setOnClickListener(v -> selectSelf.run());
        caregiver.setOnClickListener(v -> selectCaregiver.run());
        if (selected == AccountType.SELF) {
            selectSelf.run();
        } else if (selected == AccountType.CAREGIVER) {
            selectCaregiver.run();
        } else {
            paint.run();
        }
        root.findViewById(R.id.button_next).setOnClickListener(v -> {
            if (self.isChecked()) {
                host.onTypeNext(AccountType.SELF);
            } else if (caregiver.isChecked()) {
                host.onTypeNext(AccountType.CAREGIVER);
            } else {
                host.onTypeNext(null);
            }
        });
        return root;
    }

    public static View signupForm(
            AppCompatActivity activity,
            Host host,
            AuthContracts.PhoneVerification phoneVerification,
            String verifiedPhone,
            boolean phoneVerified,
            long verificationExpiresAt
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_signup_form, null);
        YakyeonTextField name = root.findViewById(R.id.field_name);
        YakyeonTextField birth = root.findViewById(R.id.field_birth);
        YakyeonTextField phone = root.findViewById(R.id.field_phone);
        YakyeonTextField code = root.findViewById(R.id.field_verification_code);
        YakyeonTextField id = root.findViewById(R.id.field_signup_id);
        YakyeonTextField password = root.findViewById(R.id.field_signup_password);
        YakyeonTextField confirm = root.findViewById(R.id.field_password_confirm);
        prepareField(name, activity.getString(R.string.auth_name_hint), false);
        prepareField(birth, activity.getString(R.string.auth_birth_hint), false);
        prepareField(phone, activity.getString(R.string.auth_phone_hint), false);
        prepareField(code, activity.getString(R.string.auth_code_hint), false);
        prepareField(id, activity.getString(R.string.auth_id_rule_hint), false);
        id.useDirectTextInput();
        prepareField(password, activity.getString(R.string.auth_password_rule_hint), true);
        prepareField(confirm, activity.getString(R.string.auth_password_confirm_hint), true);
        phone.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        code.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        birth.getEditText().setFocusable(false);
        birth.getEditText().setClickable(true);
        View.OnClickListener openDate = v -> showBirthdatePicker(activity, birth);
        birth.getEditText().setOnClickListener(openDate);
        birth.setOnClickListener(openDate);
        root.findViewById(R.id.button_request_code).setOnClickListener(v ->
                host.onRequestPhoneCode(phone.getText()));
        phone.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                host.onPhoneDraftChanged();
            }
        });
        code.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                host.onPhoneCodeDraft(phone.getText(), code.getText());
            }
        });
        root.findViewById(R.id.button_check_id).setOnClickListener(v -> host.onCheckLoginId(id.getText()));
        root.findViewById(R.id.button_submit_signup).setOnClickListener(v ->
                host.onSignupSubmit(
                        name.getText(),
                        birth.getText(),
                        phone.getText(),
                        code.getText(),
                        id.getText(),
                        password.getText(),
                        confirm.getText()
                ));
        applyPhoneVerification(activity, root, phoneVerification, host);
        paintVerification(root, verifiedPhone, phoneVerified, verificationExpiresAt);
        return root;
    }

    public static void paintVerification(
            View root,
            String verifiedPhone,
            boolean phoneVerified,
            long verificationExpiresAt
    ) {
        if (root == null) {
            return;
        }
        TextView timer = root.findViewById(R.id.text_code_timer);
        YakyeonTextField phone = root.findViewById(R.id.field_phone);
        YakyeonTextField code = root.findViewById(R.id.field_verification_code);
        if (timer == null) {
            return;
        }
        String digits = phone == null ? null : AuthContracts.phoneDigits(phone.getText());
        boolean showDone = phoneVerified && digits != null && digits.equals(verifiedPhone);
        String doneLabel = root.getContext().getString(R.string.auth_phone_verified);
        TextView done = root.findViewById(R.id.text_code_verified);
        if (done != null) {
            done.setText(doneLabel);
            done.setVisibility(showDone ? View.VISIBLE : View.GONE);
        }
        if (code != null) {
            if (showDone) {
                code.clearFocus();
                InputMethodManager input = (InputMethodManager) root.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (input != null) {
                    input.hideSoftInputFromWindow(code.getWindowToken(), 0);
                }
            }
            code.setVisibility(showDone ? View.GONE : View.VISIBLE);
        }
        TextView smsCode = root.findViewById(R.id.text_sms_code);
        TextView smsGuide = root.findViewById(R.id.text_sms_guide);
        TextView smsDigits = root.findViewById(R.id.text_sms_digits);
        View smsButton = root.findViewById(R.id.button_open_sms);
        if (smsCode != null) {
            if (showDone) {
                smsCode.setText(doneLabel);
                smsCode.setLetterSpacing(0f);
                smsCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            } else if (smsCode.getTag() instanceof String) {
                smsCode.setText((String) smsCode.getTag());
                smsCode.setLetterSpacing(0.2f);
                smsCode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
            }
        }
        int extra = showDone ? View.GONE : View.VISIBLE;
        if (smsGuide != null) {
            smsGuide.setVisibility(extra);
        }
        if (smsDigits != null) {
            smsDigits.setVisibility(extra);
        }
        if (smsButton != null) {
            smsButton.setVisibility(extra);
        }
        if (verificationExpiresAt <= 0L || showDone) {
            timer.setVisibility(View.GONE);
            return;
        }
        long seconds = AuthContracts.remainingCountdownSeconds(
                verificationExpiresAt,
                System.currentTimeMillis()
        );
        timer.setVisibility(View.VISIBLE);
        timer.setText(AuthContracts.countdownText(seconds));
    }

    public static void applyPhoneVerification(
            AppCompatActivity activity,
            View root,
            AuthContracts.PhoneVerification verification,
            Host host
    ) {
        View codePanel = root.findViewById(R.id.panel_verification_code);
        View smsPanel = root.findViewById(R.id.panel_outbound_sms);
        if (codePanel == null || smsPanel == null) {
            return;
        }
        boolean outbound = verification != null && verification.outboundSms();
        codePanel.setVisibility(outbound ? View.GONE : View.VISIBLE);
        smsPanel.setVisibility(outbound ? View.VISIBLE : View.GONE);
        if (!outbound) {
            return;
        }
        TextView guide = root.findViewById(R.id.text_sms_guide);
        TextView code = root.findViewById(R.id.text_sms_code);
        guide.setText(activity.getString(
                R.string.auth_sms_guide,
                AuthContracts.displayMessageTo(verification.messageTo)
        ));
        code.setTag(verification.messageText);
        code.setText(verification.messageText);
        code.setLetterSpacing(0.2f);
        code.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        root.findViewById(R.id.button_open_sms).setOnClickListener(v -> {
            if (!openSms(activity, verification.messageTo, verification.messageText)) {
                host.showMessage(root, activity.getString(R.string.auth_sms_unavailable));
            }
        });
    }

    private static boolean openSms(AppCompatActivity activity, String messageTo, String messageText) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + messageTo));
        intent.putExtra("sms_body", messageText);
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException exception) {
            return false;
        }
    }

    public static void showConsent(
            AppCompatActivity activity,
            Host host,
            String termsBody,
            String otherBody
    ) {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_signup_consent, null);
        TextView terms = content.findViewById(R.id.text_terms_body);
        TextView privacy = content.findViewById(R.id.text_privacy_body);
        terms.setText(termsBody);
        privacy.setText(otherBody);
        CheckBox termsBox = content.findViewById(R.id.check_terms);
        CheckBox privacyBox = content.findViewById(R.id.check_privacy);
        CheckBox noticeBox = content.findViewById(R.id.check_notice);
        CheckBox sensitiveBox = content.findViewById(R.id.check_sensitive);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setView(content)
                .create();
        content.findViewById(R.id.button_consent_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.button_consent_proceed).setOnClickListener(v -> {
            SignupConsent consent = new SignupConsent(
                    termsBox.isChecked(),
                    privacyBox.isChecked(),
                    noticeBox.isChecked(),
                    sensitiveBox.isChecked()
            );
            if (!consent.allowsSignup()) {
                host.showMessage(content, activity.getString(R.string.auth_consent_incomplete));
                return;
            }
            dialog.dismiss();
            host.onConsentAccepted();
        });
        dialog.setOnShowListener(shown -> {
            applyConsentWindow(activity, dialog, content);
            hideUnusedDialogSlots(dialog);
            keepConsentScrollWeights(content);
            content.requestLayout();
        });
        dialog.show();
    }

    public static void showCaregiver(AppCompatActivity activity, Host host) {
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_caregiver_notice, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_CaregiverDialog)
                .setView(content)
                .create();
        content.findViewById(R.id.button_caregiver_close).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.button_caregiver_continue).setOnClickListener(v -> {
            dialog.dismiss();
            host.onContinueCaregiver();
        });
        dialog.setOnShowListener(shown -> tintCaregiverSurface(activity, dialog));
        dialog.show();
    }

    private static void prepareField(YakyeonTextField field, String hint, boolean password) {
        field.setHintEnabled(false);
        field.getEditText().setHint(hint);
        if (password) {
            field.setPasswordToggleEnabled(true);
        }
    }

    private static void applyConsentWindow(AppCompatActivity activity, AlertDialog dialog, View content) {
        float density = activity.getResources().getDisplayMetrics().density;
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int bars = (int) (48 * density);
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(
                activity.findViewById(android.R.id.content)
        );
        if (insets != null) {
            Insets system = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            bars = system.top + system.bottom;
        }
        int height = screenHeight - bars - (int) (24 * density);
        int min = (int) (320 * density);
        if (height < min) {
            height = Math.max((int) (280 * density), screenHeight - bars);
        }
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (screenWidth * 0.92f), height);
        }
        content.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        stretchDialogParent(content);
        content.requestLayout();
    }

    private static void keepConsentScrollWeights(View content) {
        weightScroll(content.findViewById(R.id.consent_scroll_terms));
        weightScroll(content.findViewById(R.id.consent_scroll_other));
    }

    private static void weightScroll(View scroll) {
        if (!(scroll.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return;
        }
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scroll.getLayoutParams();
        params.height = 0;
        params.weight = 1f;
    }

    private static void hideUnusedDialogSlots(AlertDialog dialog) {
        setGone(dialog.findViewById(com.google.android.material.R.id.contentPanel));
        setGone(dialog.findViewById(com.google.android.material.R.id.buttonPanel));
    }

    private static void setGone(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private static void stretchDialogParent(View content) {
        View parent = content.getParent() instanceof View ? (View) content.getParent() : null;
        while (parent != null && parent.getLayoutParams() != null) {
            parent.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (!(parent.getParent() instanceof View) || parent.getId() == com.google.android.material.R.id.parentPanel) {
                break;
            }
            parent = (View) parent.getParent();
        }
    }

    private static void tintCaregiverSurface(AppCompatActivity activity, AlertDialog dialog) {
        int white = activity.getColor(R.color.white);
        View panel = dialog.findViewById(com.google.android.material.R.id.parentPanel);
        if (panel != null) {
            panel.setBackgroundTintList(ColorStateList.valueOf(white));
        }
    }

    private static void roundField(YakyeonTextField field) {
        float radius = 100f * field.getResources().getDisplayMetrics().density;
        field.setBoxCornerRadii(radius, radius, radius, radius);
    }

    private static void showBirthdatePicker(AppCompatActivity activity, YakyeonTextField birth) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(activity, (view, year, month, day) -> birth.setText(
                String.format(Locale.KOREA, "%04d.%02d.%02d", year, month + 1, day)
        ), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @NonNull
    private static String readConsent(AppCompatActivity activity) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                activity.getResources().openRawResource(R.raw.signup_consent),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        } catch (IOException exception) {
            return "";
        }
        return builder.toString();
    }
}
