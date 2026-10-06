package com.example.yakyeon.ui.auth;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.ui.common.AppNavigator;
import com.example.yakyeon.ui.common.WindowSpacing;

public class SignupTypeActivity extends AppCompatActivity implements GuardianConsentDialog.Listener {

    private static final String STATE_TYPE = "signup_type";

    private LinearLayout selfChoice;
    private LinearLayout guardianChoice;
    private ImageView selfRadio;
    private ImageView guardianRadio;
    @Nullable
    private SignupDraft.Type selectedType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_type);
        WindowSpacing.applySystemBars(findViewById(R.id.signup_type_root));

        selfChoice = findViewById(R.id.self_choice);
        guardianChoice = findViewById(R.id.guardian_choice);
        selfRadio = findViewById(R.id.self_radio);
        guardianRadio = findViewById(R.id.guardian_radio);

        if (savedInstanceState != null) {
            String storedType = savedInstanceState.getString(STATE_TYPE);
            if (storedType != null) {
                selectedType = SignupDraft.Type.valueOf(storedType);
            }
        }
        renderSelection();

        findViewById(R.id.back_button).setOnClickListener(view -> finish());
        selfChoice.setOnClickListener(view -> choose(SignupDraft.Type.SELF));
        guardianChoice.setOnClickListener(view -> choose(SignupDraft.Type.GUARDIAN));
        findViewById(R.id.next_button).setOnClickListener(view -> {
            if (selectedType != null) {
                proceed(selectedType);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedType != null) {
            outState.putString(STATE_TYPE, selectedType.name());
        }
    }

    @Override
    public void onGuardianConsentConfirmed() {
        AppNavigator.openSignupForm(this, SignupDraft.Type.GUARDIAN);
    }

    private void choose(SignupDraft.Type type) {
        selectedType = type;
        renderSelection();
        proceed(type);
    }

    private void proceed(SignupDraft.Type type) {
        if (type == SignupDraft.Type.SELF) {
            AppNavigator.openSignupForm(this, type);
            return;
        }
        GuardianConsentDialog.show(getSupportFragmentManager());
    }

    private void renderSelection() {
        boolean selfSelected = selectedType == SignupDraft.Type.SELF;
        boolean guardianSelected = selectedType == SignupDraft.Type.GUARDIAN;
        selfChoice.setBackgroundResource(selfSelected
                ? R.drawable.bg_choice_selected
                : R.drawable.bg_choice);
        guardianChoice.setBackgroundResource(guardianSelected
                ? R.drawable.bg_choice_selected
                : R.drawable.bg_choice);
        selfRadio.setImageResource(selfSelected ? R.drawable.ic_radio_selected : R.drawable.bg_radio);
        guardianRadio.setImageResource(guardianSelected ? R.drawable.ic_radio_selected : R.drawable.bg_radio);
        selfRadio.setContentDescription(getString(selfSelected ? R.string.selected : R.string.not_selected));
        guardianRadio.setContentDescription(getString(guardianSelected ? R.string.selected : R.string.not_selected));
    }
}
