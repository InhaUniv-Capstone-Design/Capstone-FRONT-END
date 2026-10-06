package com.example.yakyeon.ui.auth;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.yakyeon.R;

public class GuardianConsentDialog extends DialogFragment {

    public interface Listener {
        void onGuardianConsentConfirmed();
    }

    private static final String TAG = "guardian_consent";
    private static final String STATE_TERMS = "terms_checked";
    private static final String STATE_INFO = "info_checked";

    private boolean termsChecked;
    private boolean infoChecked;
    private ImageView termsCheck;
    private ImageView infoCheck;

    public static void show(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return;
        }
        new GuardianConsentDialog().show(fragmentManager, TAG);
    }

    @Override
    public int getTheme() {
        return R.style.Theme_Yakyeon_ConsentDialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.dialog_guardian_consent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        termsCheck = view.findViewById(R.id.consent_terms_check);
        infoCheck = view.findViewById(R.id.consent_info_check);
        if (savedInstanceState != null) {
            termsChecked = savedInstanceState.getBoolean(STATE_TERMS);
            infoChecked = savedInstanceState.getBoolean(STATE_INFO);
        }
        renderChecks();

        view.findViewById(R.id.consent_terms_row).setOnClickListener(row -> {
            termsChecked = !termsChecked;
            renderChecks();
        });
        view.findViewById(R.id.consent_info_row).setOnClickListener(row -> {
            infoChecked = !infoChecked;
            renderChecks();
        });
        view.findViewById(R.id.close_button).setOnClickListener(button -> dismiss());
        view.findViewById(R.id.confirm_button).setOnClickListener(button -> confirm());
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setDimAmount(0.65f);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_TERMS, termsChecked);
        outState.putBoolean(STATE_INFO, infoChecked);
    }

    private void confirm() {
        Listener listener = getActivity() instanceof Listener ? (Listener) getActivity() : null;
        dismiss();
        if (listener != null) {
            listener.onGuardianConsentConfirmed();
        }
    }

    private void renderChecks() {
        termsCheck.setImageResource(termsChecked
                ? R.drawable.ic_checkbox_checked
                : R.drawable.ic_checkbox);
        infoCheck.setImageResource(infoChecked
                ? R.drawable.ic_checkbox_checked
                : R.drawable.ic_checkbox);
        termsCheck.setContentDescription(getString(termsChecked ? R.string.selected : R.string.not_selected));
        infoCheck.setContentDescription(getString(infoChecked ? R.string.selected : R.string.not_selected));
    }
}
