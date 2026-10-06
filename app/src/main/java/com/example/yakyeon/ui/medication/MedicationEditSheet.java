package com.example.yakyeon.ui.medication;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.yakyeon.R;
import com.example.yakyeon.data.MedicationSearchRepository;
import com.example.yakyeon.data.mock.MockMedicationRepository;
import com.example.yakyeon.data.mock.MockMedicationSearchRepository;
import com.example.yakyeon.data.model.Medication;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;

public class MedicationEditSheet extends BottomSheetDialogFragment {

    public static final String TAG = "medication_edit";
    private static final String ARG_ID = "medication_id";

    private final MedicationSearchRepository searchRepository = MockMedicationSearchRepository.getInstance();

    private Calendar startDate;
    private Calendar endDate;

    public static void open(FragmentManager manager, String medicationId) {
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }
        MedicationEditSheet sheet = new MedicationEditSheet();
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
        return inflater.inflate(R.layout.bottom_sheet_medication_edit, container, false);
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

        AutoCompleteTextView name = view.findViewById(R.id.edit_name);
        AutoCompleteTextView doseAmount = view.findViewById(R.id.edit_dose_amount);
        AutoCompleteTextView doseUnit = view.findViewById(R.id.edit_dose_unit);
        AutoCompleteTextView timesPerDay = view.findViewById(R.id.edit_times_per_day);
        TextView doseTimes = view.findViewById(R.id.edit_dose_times);
        AutoCompleteTextView doseTiming = view.findViewById(R.id.edit_dose_timing);
        TextView duration = view.findViewById(R.id.edit_duration);

        MedicationInputControls.bindName(name, searchRepository, medication.getName());
        MedicationInputControls.bindChoice(
                doseAmount,
                getResources().getStringArray(R.array.medication_dose_quantities),
                Medication.formatAmount(medication.getDoseAmount())
        );
        MedicationInputControls.bindChoice(
                doseUnit,
                getResources().getStringArray(R.array.medication_dose_units),
                medication.getDoseUnit()
        );
        MedicationInputControls.bindChoice(
                timesPerDay,
                getResources().getStringArray(R.array.medication_times_per_day),
                medication.getTimesPerDay()
        );
        MedicationInputControls.bindChoice(
                doseTiming,
                getResources().getStringArray(R.array.medication_dose_timings),
                medication.getDoseTiming()
        );
        MedicationInputControls.bindTime(doseTimes, medication.getDoseTimes());
        bindPeriod(duration, medication.getDuration());

        boolean active = medication.getClassification() == Medication.Classification.ACTIVE;
        View endButton = view.findViewById(R.id.edit_end);
        endButton.setVisibility(active ? View.VISIBLE : View.GONE);
        endButton.setOnClickListener(button ->
                MedicationEndDialog.open(requireActivity().getSupportFragmentManager(), medicationId)
        );
        view.findViewById(R.id.edit_delete).setOnClickListener(button ->
                MedicationDeleteDialog.open(requireActivity().getSupportFragmentManager(), medicationId)
        );
        view.findViewById(R.id.edit_close).setOnClickListener(button -> dismiss());
        view.findViewById(R.id.edit_save).setOnClickListener(button -> save(
                medicationId,
                name,
                doseAmount,
                doseUnit,
                timesPerDay,
                doseTimes,
                doseTiming
        ));
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheets.expand(this);
    }

    private void save(
            String medicationId,
            AutoCompleteTextView name,
            AutoCompleteTextView doseAmount,
            AutoCompleteTextView doseUnit,
            AutoCompleteTextView timesPerDay,
            TextView doseTimes,
            AutoCompleteTextView doseTiming
    ) {
        String selectedName = MedicationInputControls.textOf(name);
        String amountText = MedicationInputControls.textOf(doseAmount);
        String unit = MedicationInputControls.textOf(doseUnit);
        String times = MedicationInputControls.textOf(timesPerDay);
        String time = MedicationInputControls.textOf(doseTimes);
        String timing = MedicationInputControls.textOf(doseTiming);
        Integer error = MedicationInputControls.validationError(
                requireContext(),
                searchRepository,
                selectedName,
                amountText,
                unit,
                times,
                time,
                timing,
                startDate,
                endDate
        );
        if (error != null) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!(getActivity() instanceof MedicationListActivity)) {
            dismiss();
            return;
        }
        ((MedicationListActivity) getActivity()).applyMedicationEdit(
                medicationId,
                selectedName,
                Double.parseDouble(amountText),
                unit,
                times,
                time,
                timing,
                MedicationInputControls.formatPeriod(startDate, endDate)
        );
        dismiss();
    }

    private void bindPeriod(TextView field, String current) {
        Calendar[] period = MedicationInputControls.parsePeriod(current);
        if (period != null) {
            startDate = period[0];
            endDate = period[1];
            field.setText(MedicationInputControls.formatPeriod(startDate, endDate));
        }
        field.setOnClickListener(view -> showStartDatePicker(field));
    }

    private void showStartDatePicker(TextView field) {
        Calendar initial = startDate == null ? Calendar.getInstance() : startDate;
        new DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> showEndDatePicker(
                        field,
                        MedicationInputControls.dateOnly(year, month, day)
                ),
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showEndDatePicker(TextView field, Calendar start) {
        Calendar initial = endDate == null || endDate.before(start) ? start : endDate;
        new DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> {
                    Calendar end = MedicationInputControls.dateOnly(year, month, day);
                    if (end.before(start)) {
                        Toast.makeText(
                                requireContext(),
                                R.string.medication_edit_period_order,
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }
                    startDate = start;
                    endDate = end;
                    field.setText(MedicationInputControls.formatPeriod(start, end));
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
}
