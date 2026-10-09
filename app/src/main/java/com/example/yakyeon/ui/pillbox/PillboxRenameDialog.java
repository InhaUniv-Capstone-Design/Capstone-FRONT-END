package com.example.yakyeon.ui.pillbox;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.yakyeon.R;

public class PillboxRenameDialog extends DialogFragment {

    public interface Listener {
        void onPillboxRenamed(String displayName);
    }

    public static final String TAG = "pillbox_rename";
    private static final String ARG_NAME = "display_name";

    public static void open(FragmentManager manager, String displayName) {
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }
        PillboxRenameDialog dialog = new PillboxRenameDialog();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, displayName);
        dialog.setArguments(args);
        dialog.show(manager, TAG);
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
        return inflater.inflate(R.layout.dialog_pillbox_rename, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText input = view.findViewById(R.id.pillbox_rename_input);
        if (savedInstanceState == null && getArguments() != null) {
            input.setText(getArguments().getString(ARG_NAME, ""));
            input.setSelection(input.getText().length());
        }
        view.findViewById(R.id.pillbox_rename_cancel).setOnClickListener(button -> dismiss());
        view.findViewById(R.id.pillbox_rename_save).setOnClickListener(button -> save(input));
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

    private void save(EditText input) {
        String name = input.getText().toString().trim();
        if (name.length() == 0) {
            return;
        }
        Listener listener = getActivity() instanceof Listener ? (Listener) getActivity() : null;
        dismiss();
        if (listener != null) {
            listener.onPillboxRenamed(name);
        }
    }
}
