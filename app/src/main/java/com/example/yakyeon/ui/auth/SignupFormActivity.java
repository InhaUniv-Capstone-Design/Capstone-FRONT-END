package com.example.yakyeon.ui.auth;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.Calendar;
import java.util.Locale;

public class SignupFormActivity extends AppCompatActivity {

    public static final String EXTRA_SIGNUP_TYPE = "signup_type";

    private SignupDraft.Type signupType = SignupDraft.Type.SELF;
    private SignupDraft submittedDraft;
    private boolean passwordVisible;
    private boolean passwordConfirmVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_form);
        WindowSpacing.applySystemBars(findViewById(R.id.signup_form_root));

        String typeName = getIntent().getStringExtra(EXTRA_SIGNUP_TYPE);
        if (typeName != null) {
            signupType = SignupDraft.Type.valueOf(typeName);
        }

        EditText birthInput = findViewById(R.id.birth_input);
        EditText passwordInput = findViewById(R.id.password_input);
        EditText passwordConfirmInput = findViewById(R.id.password_confirm_input);
        ImageButton passwordVisibilityButton = findViewById(R.id.password_visibility_button);
        ImageButton passwordConfirmVisibilityButton = findViewById(R.id.password_confirm_visibility_button);

        findViewById(R.id.back_button).setOnClickListener(view -> finish());
        birthInput.setOnClickListener(view -> showBirthDatePicker(birthInput));
        findViewById(R.id.birth_picker_button).setOnClickListener(view -> showBirthDatePicker(birthInput));
        passwordVisibilityButton.setOnClickListener(view -> {
            passwordVisible = !passwordVisible;
            applyPasswordVisibility(passwordInput, passwordVisibilityButton, passwordVisible);
        });
        passwordConfirmVisibilityButton.setOnClickListener(view -> {
            passwordConfirmVisible = !passwordConfirmVisible;
            applyPasswordVisibility(passwordConfirmInput, passwordConfirmVisibilityButton, passwordConfirmVisible);
        });
        findViewById(R.id.request_code_button).setOnClickListener(view -> {
            // 인증번호 API는 이후 연결한다. 지금은 입력값만 화면에 남긴다.
        });
        findViewById(R.id.check_id_button).setOnClickListener(view -> {
            // 아이디 중복 확인 API는 이후 연결한다. 지금은 입력값만 화면에 남긴다.
        });
        findViewById(R.id.complete_button).setOnClickListener(view -> submittedDraft = readDraft());
    }

    private SignupDraft readDraft() {
        return new SignupDraft(
                signupType,
                textOf(R.id.name_input),
                textOf(R.id.birth_input),
                textOf(R.id.email_input),
                textOf(R.id.phone_input),
                textOf(R.id.user_id_input),
                textOf(R.id.password_input),
                textOf(R.id.password_confirm_input)
        );
    }

    private String textOf(int viewId) {
        EditText input = findViewById(viewId);
        return input.getText().toString().trim();
    }

    private void showBirthDatePicker(EditText birthInput) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, year, month, dayOfMonth) -> birthInput.setText(
                        String.format(Locale.US, "%04d.%02d.%02d", year, month + 1, dayOfMonth)
                ),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void applyPasswordVisibility(EditText input, ImageButton button, boolean visible) {
        int selection = input.getSelectionEnd();
        input.setInputType(visible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        if (selection >= 0) {
            input.setSelection(selection);
        }
        button.setImageResource(visible ? R.drawable.ic_eye : R.drawable.ic_eye_off);
    }
}
