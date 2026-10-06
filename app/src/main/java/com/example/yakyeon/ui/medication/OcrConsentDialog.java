package com.example.yakyeon.ui.medication;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.yakyeon.R;

public class OcrConsentDialog extends DialogFragment {

    public interface Listener {
        void onOcrConsentGranted();
    }

    public static final String TAG = "ocr_consent";
    private static final String STATE_CHECK = "ocr_check";
    private static final String STATE_PRIVACY = "ocr_privacy";

    private boolean checkConfirmed;
    private boolean privacyConfirmed;
    private ImageView checkIcon;
    private ImageView privacyIcon;

    public static void open(FragmentManager manager) {
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }
        new OcrConsentDialog().show(manager, TAG);
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
        return inflater.inflate(R.layout.dialog_ocr_consent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkIcon = view.findViewById(R.id.ocr_consent_check_icon);
        privacyIcon = view.findViewById(R.id.ocr_consent_privacy_icon);
        if (savedInstanceState != null) {
            checkConfirmed = savedInstanceState.getBoolean(STATE_CHECK);
            privacyConfirmed = savedInstanceState.getBoolean(STATE_PRIVACY);
        }
        renderChecks();
        view.findViewById(R.id.ocr_consent_check_row).setOnClickListener(row -> {
            checkConfirmed = !checkConfirmed;
            renderChecks();
        });
        view.findViewById(R.id.ocr_consent_privacy_row).setOnClickListener(row -> {
            privacyConfirmed = !privacyConfirmed;
            renderChecks();
        });
        view.findViewById(R.id.ocr_consent_cancel).setOnClickListener(button -> dismiss());
        view.findViewById(R.id.ocr_consent_agree).setOnClickListener(button -> agree());
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
        outState.putBoolean(STATE_CHECK, checkConfirmed);
        outState.putBoolean(STATE_PRIVACY, privacyConfirmed);
    }

    private void agree() {
        if (!checkConfirmed || !privacyConfirmed) {
            Toast.makeText(requireContext(), R.string.ocr_consent_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Listener listener = getActivity() instanceof Listener ? (Listener) getActivity() : null;
        dismiss();
        if (listener != null) {
            listener.onOcrConsentGranted();
        }
    }

    private void renderChecks() {
        checkIcon.setImageResource(checkConfirmed
                ? R.drawable.ic_checkbox_checked
                : R.drawable.ic_checkbox);
        privacyIcon.setImageResource(privacyConfirmed
                ? R.drawable.ic_checkbox_checked
                : R.drawable.ic_checkbox);
        checkIcon.setContentDescription(getString(
                checkConfirmed ? R.string.selected : R.string.not_selected
        ));
        privacyIcon.setContentDescription(getString(
                privacyConfirmed ? R.string.selected : R.string.not_selected
        ));
    }
}
