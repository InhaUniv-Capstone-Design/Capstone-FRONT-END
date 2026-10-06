package com.example.yakyeon.ui.medication;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.yakyeon.R;

public class MedicationDeleteDialog extends DialogFragment {

    public static final String TAG = "medication_delete";
    private static final String ARG_ID = "medication_id";

    public static void open(FragmentManager manager, String medicationId) {
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }
        MedicationDeleteDialog dialog = new MedicationDeleteDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ID, medicationId);
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
        return inflater.inflate(R.layout.dialog_medication_delete, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String medicationId = requireArguments().getString(ARG_ID);
        view.findViewById(R.id.delete_cancel).setOnClickListener(button -> dismiss());
        view.findViewById(R.id.delete_confirm).setOnClickListener(button -> {
            if (getActivity() instanceof MedicationListActivity) {
                ((MedicationListActivity) getActivity()).confirmMedicationDelete(medicationId);
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
