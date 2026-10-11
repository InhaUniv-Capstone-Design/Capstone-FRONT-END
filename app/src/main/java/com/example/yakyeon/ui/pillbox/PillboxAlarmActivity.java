package com.example.yakyeon.ui.pillbox;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.yakyeon.R;
import com.example.yakyeon.data.PillboxRepository;
import com.example.yakyeon.data.mock.MockPillboxRepository;
import com.example.yakyeon.data.model.PillboxMedicine;
import com.example.yakyeon.data.model.PillboxSession;
import com.example.yakyeon.data.model.PillboxSlot;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.NotificationEntry;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PillboxAlarmActivity extends AppCompatActivity {

    private static final String STATE_SLOT = "alarm_slot";
    private static final String STATE_IDS = "alarm_ids";

    private final PillboxRepository pillbox = MockPillboxRepository.getInstance();
    private final Map<String, List<String>> drafts = new HashMap<>();
    private final List<TextView> tabs = new ArrayList<>();

    private int selectedSlot = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pillbox_alarm);
        WindowSpacing.applySystemBars(findViewById(R.id.pillbox_alarm_root));
        BottomNav.bindPillbox(this);
        NotificationEntry.bind(this);
        tabs.add(findViewById(R.id.pillbox_alarm_tab_1));
        tabs.add(findViewById(R.id.pillbox_alarm_tab_2));
        tabs.add(findViewById(R.id.pillbox_alarm_tab_3));
        for (int index = 0; index < tabs.size(); index++) {
            int slotNumber = index + 1;
            tabs.get(index).setText(getString(R.string.pillbox_slot_tab, slotNumber));
            tabs.get(index).setOnClickListener(view -> selectSlot(slotNumber));
        }
        findViewById(R.id.pillbox_alarm_back).setOnClickListener(view -> finish());
        findViewById(R.id.pillbox_alarm_save).setOnClickListener(view -> pillbox.saveAlarmTimes(drafts));
        if (!restore(savedInstanceState)) {
            loadDrafts();
        }
        bindSlot();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!pillbox.hasSimulatedLink()) {
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SLOT, selectedSlot);
        ArrayList<String> encoded = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : drafts.entrySet()) {
            StringBuilder builder = new StringBuilder(entry.getKey());
            for (String time : entry.getValue()) {
                builder.append('\n').append(time == null ? "" : time);
            }
            encoded.add(builder.toString());
        }
        outState.putStringArrayList(STATE_IDS, encoded);
    }

    private boolean restore(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return false;
        }
        selectedSlot = savedInstanceState.getInt(STATE_SLOT, 1);
        ArrayList<String> encoded = savedInstanceState.getStringArrayList(STATE_IDS);
        if (encoded == null) {
            return false;
        }
        drafts.clear();
        for (String row : encoded) {
            String[] parts = row.split("\n", -1);
            if (parts.length < 2) {
                continue;
            }
            List<String> times = new ArrayList<>();
            for (int index = 1; index < parts.length; index++) {
                times.add(parts[index]);
            }
            drafts.put(parts[0], times);
        }
        return !drafts.isEmpty();
    }

    private void loadDrafts() {
        PillboxSession session = pillbox.session();
        if (session == null) {
            finish();
            return;
        }
        drafts.clear();
        for (PillboxSlot slot : session.getSlots()) {
            for (PillboxMedicine medicine : slot.getMedicines()) {
                drafts.put(medicine.getId(), new ArrayList<>(medicine.getAlarmTimes()));
            }
        }
    }

    private void selectSlot(int slotNumber) {
        selectedSlot = slotNumber;
        bindSlot();
    }

    private void bindSlot() {
        PillboxSession session = pillbox.session();
        if (session == null) {
            finish();
            return;
        }
        styleTabs();
        PillboxSlot slot = findSlot(session);
        if (slot == null) {
            return;
        }
        ((TextView) findViewById(R.id.pillbox_alarm_slot_number)).setText(String.valueOf(slot.getNumber()));
        ((TextView) findViewById(R.id.pillbox_alarm_prescription)).setText(slot.getPrescriptionName());
        ((TextView) findViewById(R.id.pillbox_alarm_pharmacy)).setText(slot.getPharmacyLabel());
        ((TextView) findViewById(R.id.pillbox_alarm_count)).setText(
                getString(R.string.pillbox_medicine_count, slot.getMedicines().size())
        );
        LinearLayout list = findViewById(R.id.pillbox_alarm_medicines);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        boolean unset = false;
        for (PillboxMedicine medicine : slot.getMedicines()) {
            list.addView(bindMedicine(inflater, list, medicine));
            if (hasUnset(medicine.getId())) {
                unset = true;
            }
        }
        findViewById(R.id.pillbox_alarm_warning).setVisibility(unset ? View.VISIBLE : View.GONE);
    }

    private View bindMedicine(LayoutInflater inflater, LinearLayout parent, PillboxMedicine medicine) {
        View card = inflater.inflate(R.layout.item_alarm_medicine, parent, false);
        ((TextView) card.findViewById(R.id.alarm_medicine_name)).setText(medicine.getName());
        ((TextView) card.findViewById(R.id.alarm_medicine_frequency)).setText(medicine.getFrequency());
        card.findViewById(R.id.alarm_as_needed).setVisibility(
                medicine.isAsNeeded() ? View.VISIBLE : View.GONE
        );
        LinearLayout times = card.findViewById(R.id.alarm_time_row);
        List<String> values = drafts.get(medicine.getId());
        if (values == null) {
            return card;
        }
        for (int index = 0; index < values.size(); index++) {
            View field = inflater.inflate(R.layout.item_alarm_time, times, false);
            if (index > 0) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) field.getLayoutParams();
                params.setMarginStart(dp(8));
                field.setLayoutParams(params);
            }
            String stored = values.get(index);
            boolean empty = stored == null || stored.length() == 0;
            String label = empty ? getString(R.string.pillbox_alarm_unset_word) : stored;
            TextView caption = field.findViewById(R.id.alarm_caption);
            caption.setText(medicine.isAsNeeded()
                    ? getString(R.string.pillbox_alarm_optional, label)
                    : getString(R.string.pillbox_alarm_round, index + 1, label));
            TextView value = field.findViewById(R.id.alarm_time_value);
            value.setText(empty ? getString(R.string.pillbox_alarm_unset) : stored);
            value.setTextColor(ContextCompat.getColor(this, empty ? R.color.muted : R.color.ink));
            int alarmIndex = index;
            field.findViewById(R.id.alarm_time_field).setOnClickListener(
                    view -> pickTime(medicine.getId(), alarmIndex)
            );
            field.findViewById(R.id.alarm_time_field).setContentDescription(
                    getString(R.string.pillbox_pick_time)
            );
            times.addView(field);
        }
        return card;
    }

    private void pickTime(String medicineId, int index) {
        List<String> values = drafts.get(medicineId);
        if (values == null || index >= values.size()) {
            return;
        }
        int hour = 8;
        int minute = 0;
        String current = values.get(index);
        if (current != null && current.contains(":")) {
            String[] parts = current.split(":");
            if (parts.length == 2) {
                try {
                    hour = Integer.parseInt(parts[0]);
                    minute = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    hour = 8;
                    minute = 0;
                }
            }
        }
        new TimePickerDialog(this, (picker, pickedHour, pickedMinute) -> {
            values.set(index, String.format(Locale.KOREA, "%02d:%02d", pickedHour, pickedMinute));
            bindSlot();
        }, hour, minute, true).show();
    }

    private void styleTabs() {
        for (int index = 0; index < tabs.size(); index++) {
            boolean selected = index + 1 == selectedSlot;
            TextView tab = tabs.get(index);
            tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : 0);
            tab.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.muted));
        }
    }

    private PillboxSlot findSlot(PillboxSession session) {
        for (PillboxSlot slot : session.getSlots()) {
            if (slot.getNumber() == selectedSlot) {
                return slot;
            }
        }
        return session.getSlots().isEmpty() ? null : session.getSlots().get(0);
    }

    private boolean hasUnset(String medicineId) {
        List<String> values = drafts.get(medicineId);
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (value == null || value.length() == 0) {
                return true;
            }
        }
        return false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
