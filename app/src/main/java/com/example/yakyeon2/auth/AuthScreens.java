package com.example.yakyeon2.auth;

import android.app.DatePickerDialog;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.yakyeon2.data.AccountType;
import com.example.yakyeon2.ui.YakyeonTextField;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;

public final class AuthScreens {

    public interface Host {
        void showMessage(View anchor, CharSequence message);

        void showNotice(CharSequence title, CharSequence message, Runnable onConfirm);

        void onLogin(String id, String password);

        void onRequestSignup();

        void onConsentAccepted();

        void onTypeNext(AccountType type);

        void onContinueCaregiver();

        void onDevHome();

        void onDevPillboxScenario();

        void onRequestPhoneCode(String phone);

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
        roundField(id);
        roundField(password);
        root.findViewById(R.id.button_login).setOnClickListener(v ->
                host.onLogin(id.getText(), password.getText()));
        root.findViewById(R.id.button_open_signup).setOnClickListener(v -> host.onRequestSignup());
        boolean debuggable = (activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (debuggable) {
            root.findViewById(R.id.button_dev_home).setVisibility(View.VISIBLE);
            root.findViewById(R.id.button_dev_pillbox_scenario).setVisibility(View.VISIBLE);
            root.findViewById(R.id.text_dev_home_note).setVisibility(View.VISIBLE);
            root.findViewById(R.id.button_dev_home).setOnClickListener(v -> host.onDevHome());
            root.findViewById(R.id.button_dev_pillbox_scenario).setOnClickListener(v ->
                    host.onDevPillboxScenario());
        }
        return root;
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

    public static View signupForm(AppCompatActivity activity, Host host) {
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
        return root;
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
