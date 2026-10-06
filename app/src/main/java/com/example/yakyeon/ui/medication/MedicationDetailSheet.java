package com.example.yakyeon.ui.medication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.yakyeon.R;
import com.example.yakyeon.data.mock.MockMedicationRepository;
import com.example.yakyeon.data.model.Medication;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MedicationDetailSheet extends BottomSheetDialogFragment {

    public static final String TAG = "medication_detail";
    private static final String ARG_ID = "medication_id";

    public static void open(FragmentManager manager, String medicationId) {
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }
        MedicationDetailSheet sheet = new MedicationDetailSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ID, medicationId);
        sheet.setArguments(args);
        sheet.show(manager, TAG);
    }

    @Override
    public int getTheme() {
        return R.style.Theme_Yakyeon_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_medication_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String medicationId = requireArguments().getString(ARG_ID);
        Medication medication = MockMedicationRepository.getInstance().findById(medicationId);
        if (medication == null) {
            dismiss();
            return;
        }
        TextView category = view.findViewById(R.id.detail_category);
        bindOptional(category, medication.getCategory());
        TextView name = view.findViewById(R.id.detail_name);
        name.setText(medication.getName());
        bindRow(view.findViewById(R.id.detail_usage_row), view.findViewById(R.id.detail_usage), medication.getUsage());
        bindRow(view.findViewById(R.id.detail_class_row), view.findViewById(R.id.detail_class), medication.getDrugClass());
        bindRow(
                view.findViewById(R.id.detail_side_effects_row),
                view.findViewById(R.id.detail_side_effects),
                medication.getSideEffects()
        );
        bindRow(
                view.findViewById(R.id.detail_precautions_row),
                view.findViewById(R.id.detail_precautions),
                medication.getPrecautions()
        );
        view.findViewById(R.id.detail_close).setOnClickListener(button -> dismiss());
        view.findViewById(R.id.detail_confirm).setOnClickListener(button -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheets.expand(this);
    }

    private static void bindOptional(TextView view, String value) {
        if (value == null || value.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(value);
    }

    private static void bindRow(View row, TextView valueView, String value) {
        if (value == null || value.trim().isEmpty()) {
            row.setVisibility(View.GONE);
            return;
        }
        row.setVisibility(View.VISIBLE);
        valueView.setText(value);
    }
}
