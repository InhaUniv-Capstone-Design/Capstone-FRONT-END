package com.example.yakyeon.ui.medication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.yakyeon.R;
import com.example.yakyeon.data.MedicationReminderRepository;
import com.example.yakyeon.data.mock.MockMedicationReminderRepository;
import com.example.yakyeon.data.model.ReminderRecord;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MedicationReminderPopup extends BottomSheetDialogFragment {

    public static final String TAG = "medication_reminder_popup";
    private static final String ARG_ID = "record_id";

    private final MedicationReminderRepository reminders = MockMedicationReminderRepository.getInstance();

    private String recordId;
    private boolean selected;

    public static void open(FragmentManager manager, String recordId) {
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }
        MedicationReminderPopup popup = new MedicationReminderPopup();
        Bundle args = new Bundle();
        args.putString(ARG_ID, recordId);
        popup.setArguments(args);
        popup.show(manager, TAG);
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
        return inflater.inflate(R.layout.bottom_sheet_medication_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recordId = requireArguments().getString(ARG_ID);
        ReminderRecord record = reminders.find(recordId);
        if (record == null) {
            dismiss();
            return;
        }
        selected = record.isCompleted();
        ((TextView) view.findViewById(R.id.reminder_popup_time)).setText(reminders.alertTimeLabel(record));
        ((TextView) view.findViewById(R.id.reminder_popup_list_title)).setText(reminders.checklistTitle(record));
        view.findViewById(R.id.reminder_select_all).setOnClickListener(button -> {
            selected = true;
            bindChecklist(view);
        });
        view.findViewById(R.id.reminder_complete).setOnClickListener(button -> completeSelection());
        bindChecklist(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheets.expand(this);
    }

    private void completeSelection() {
        if (!selected) {
            return;
        }
        if (!reminders.markUserCompleted(recordId)) {
            return;
        }
        if (getActivity() instanceof MedicationReminderActivity) {
            ((MedicationReminderActivity) getActivity()).refreshRecords();
        }
        dismiss();
    }

    private void bindChecklist(View root) {
        ReminderRecord record = reminders.find(recordId);
        if (record == null) {
            return;
        }
        LinearLayout checklist = root.findViewById(R.id.reminder_checklist);
        checklist.removeAllViews();
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_reminder_check, checklist, false);
        ((TextView) row.findViewById(R.id.reminder_check_name)).setText(record.getMedicationName());
        applySelection(row, selected);
        row.setOnClickListener(clicked -> {
            selected = !selected;
            applySelection(row, selected);
        });
        checklist.addView(row);
    }

    private static void applySelection(View row, boolean selected) {
        row.setBackgroundResource(selected
                ? R.drawable.bg_reminder_item_on
                : R.drawable.bg_reminder_item_off);
        row.findViewById(R.id.reminder_check_box).setBackgroundResource(selected
                ? R.drawable.bg_reminder_check_on
                : R.drawable.bg_reminder_check_off);
        ImageView icon = row.findViewById(R.id.reminder_check_icon);
        icon.setVisibility(selected ? View.VISIBLE : View.GONE);
    }
}
