package com.example.yakyeon.ui.pillbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.PillboxRepository;
import com.example.yakyeon.data.mock.MockPillboxRepository;
import com.example.yakyeon.data.model.PillboxMedicine;
import com.example.yakyeon.data.model.PillboxSession;
import com.example.yakyeon.data.model.PillboxSlot;
import com.example.yakyeon.ui.common.AppNavigator;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.NotificationEntry;
import com.example.yakyeon.ui.common.WindowSpacing;

public class PillboxActivity extends AppCompatActivity implements PillboxConsentDialog.Listener {

    private final PillboxRepository pillbox = MockPillboxRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pillbox);
        WindowSpacing.applySystemBars(findViewById(R.id.pillbox_root));
        BottomNav.bindPillbox(this);
        NotificationEntry.bind(this);
        findViewById(R.id.pillbox_connect).setOnClickListener(
                view -> PillboxConsentDialog.open(getSupportFragmentManager())
        );
        findViewById(R.id.pillbox_settings).setOnClickListener(
                view -> AppNavigator.openPillboxSettings(this)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindSession();
    }

    @Override
    public void onPillboxConsentGranted() {
        AppNavigator.openDeviceSearch(this);
    }

    private void bindSession() {
        boolean linked = pillbox.hasSimulatedLink();
        findViewById(R.id.pillbox_empty_caption).setVisibility(linked ? View.GONE : View.VISIBLE);
        findViewById(R.id.pillbox_empty).setVisibility(linked ? View.GONE : View.VISIBLE);
        findViewById(R.id.pillbox_connected).setVisibility(linked ? View.VISIBLE : View.GONE);
        if (!linked) {
            return;
        }
        PillboxSession session = pillbox.session();
        if (session == null) {
            return;
        }
        ((TextView) findViewById(R.id.pillbox_battery)).setText(session.getBatteryLabel());
        ((TextView) findViewById(R.id.pillbox_usage)).setText(
                getString(R.string.pillbox_slots_in_use, session.getSlots().size())
        );
        LinearLayout slots = findViewById(R.id.pillbox_slots);
        slots.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (PillboxSlot slot : session.getSlots()) {
            slots.addView(bindSlot(inflater, slots, slot));
        }
    }

    private View bindSlot(LayoutInflater inflater, LinearLayout parent, PillboxSlot slot) {
        View view = inflater.inflate(R.layout.item_pillbox_slot, parent, false);
        boolean error = slot.getStatus() == PillboxSlot.Status.OPEN_ERROR;
        view.setBackgroundResource(error ? R.drawable.bg_slot_card_error : R.drawable.bg_slot_card);
        TextView number = view.findViewById(R.id.slot_number);
        number.setText(String.valueOf(slot.getNumber()));
        number.setBackgroundResource(error ? R.drawable.bg_circle_danger : R.drawable.bg_circle_white);
        number.setTextColor(getColor(error ? R.color.danger : R.color.brand_dark));
        ((TextView) view.findViewById(R.id.slot_prescription)).setText(slot.getPrescriptionName());
        TextView status = view.findViewById(R.id.slot_status);
        status.setText(error ? R.string.pillbox_status_error : R.string.pillbox_status_ok);
        status.setTextColor(getColor(error ? R.color.danger : R.color.status_ok));
        if (error) {
            status.setBackgroundResource(R.drawable.bg_status_error);
        }
        View errorRow = view.findViewById(R.id.slot_error);
        errorRow.setVisibility(error ? View.VISIBLE : View.GONE);
        if (error) {
            ((TextView) view.findViewById(R.id.slot_error_message)).setText(slot.getErrorMessage());
        }
        LinearLayout medicines = view.findViewById(R.id.slot_medicines);
        int frequencyColor = getColor(error ? R.color.graph_caption : R.color.muted);
        for (PillboxMedicine medicine : slot.getMedicines()) {
            View row = inflater.inflate(R.layout.item_slot_medicine, medicines, false);
            if (medicines.getChildCount() == 0) {
                ((LinearLayout.LayoutParams) row.getLayoutParams()).topMargin = 0;
            }
            ((TextView) row.findViewById(R.id.medicine_name)).setText(medicine.getName());
            TextView frequency = row.findViewById(R.id.medicine_frequency);
            frequency.setText(medicine.getFrequency());
            frequency.setTextColor(frequencyColor);
            medicines.addView(row);
        }
        return view;
    }
}
