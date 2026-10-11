package com.example.yakyeon.ui.medication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.MedicationReminderRepository;
import com.example.yakyeon.data.mock.MockMedicationReminderRepository;
import com.example.yakyeon.data.model.ReminderDay;
import com.example.yakyeon.data.model.ReminderRecord;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.NotificationEntry;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.List;

public class MedicationReminderActivity extends AppCompatActivity {

    private final MedicationReminderRepository reminders = MockMedicationReminderRepository.getInstance();

    private LinearLayout days;
    private String filter = ReminderRecord.FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medication_reminder);
        WindowSpacing.applySystemBars(findViewById(R.id.reminder_root));
        BottomNav.bindNeutral(this);
        NotificationEntry.bind(this);

        days = findViewById(R.id.reminder_days);
        ((TextView) findViewById(R.id.reminder_period_range)).setText(reminders.periodRange());
        bindFilter(R.id.reminder_filter_all, ReminderRecord.FILTER_ALL);
        bindFilter(R.id.reminder_filter_on_time, ReminderRecord.STATUS_ON_TIME);
        bindFilter(R.id.reminder_filter_delayed, ReminderRecord.STATUS_DELAYED);
        bindFilter(R.id.reminder_filter_missed, ReminderRecord.STATUS_MISSED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecords();
    }

    public void refreshRecords() {
        if (days != null) {
            showRecords();
        }
    }

    private void bindFilter(int viewId, String value) {
        findViewById(viewId).setOnClickListener(view -> {
            filter = value;
            showRecords();
        });
    }

    private void showRecords() {
        styleChip(findViewById(R.id.reminder_filter_all), ReminderRecord.FILTER_ALL);
        styleChip(findViewById(R.id.reminder_filter_on_time), ReminderRecord.STATUS_ON_TIME);
        styleChip(findViewById(R.id.reminder_filter_delayed), ReminderRecord.STATUS_DELAYED);
        styleChip(findViewById(R.id.reminder_filter_missed), ReminderRecord.STATUS_MISSED);

        days.removeAllViews();
        List<ReminderDay> groups = reminders.days(filter);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            if (groupIndex > 0) {
                View band = new View(this);
                band.setBackgroundColor(getColor(R.color.reminder_band));
                LinearLayout.LayoutParams bandParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(8)
                );
                bandParams.topMargin = dp(20);
                bandParams.bottomMargin = dp(20);
                days.addView(band, bandParams);
            }
            ReminderDay day = groups.get(groupIndex);
            TextView header = (TextView) inflater.inflate(R.layout.item_reminder_date, days, false);
            header.setText(day.getDateLabel());
            days.addView(header);
            List<ReminderRecord> records = day.getRecords();
            for (int index = 0; index < records.size(); index++) {
                if (index > 0) {
                    View divider = new View(this);
                    divider.setBackgroundColor(getColor(R.color.reminder_band));
                    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(1)
                    );
                    dividerParams.leftMargin = dp(12);
                    dividerParams.rightMargin = dp(12);
                    days.addView(divider, dividerParams);
                }
                days.addView(bindRecord(inflater, records.get(index)));
            }
        }
    }

    private View bindRecord(LayoutInflater inflater, ReminderRecord record) {
        View item = inflater.inflate(R.layout.item_reminder_record, days, false);
        TextView clock = item.findViewById(R.id.reminder_clock);
        TextView name = item.findViewById(R.id.reminder_name);
        TextView status = item.findViewById(R.id.reminder_status);
        clock.setText(record.getClock());
        name.setText(record.getMedicationName());
        status.setText(getString(R.string.reminder_status_badge, record.getStatus()));
        if (ReminderRecord.STATUS_DELAYED.equals(record.getStatus())) {
            item.setBackgroundResource(R.drawable.bg_reminder_row_delayed);
            clock.setTextColor(getColor(R.color.reminder_delayed));
            status.setBackgroundResource(R.drawable.bg_reminder_badge_delayed);
            status.setTextColor(getColor(R.color.reminder_delayed));
        } else if (ReminderRecord.STATUS_MISSED.equals(record.getStatus())) {
            item.setBackgroundResource(R.drawable.bg_reminder_row_missed);
            clock.setTextColor(getColor(R.color.reminder_missed));
            status.setBackgroundResource(R.drawable.bg_reminder_badge_missed);
            status.setTextColor(getColor(R.color.reminder_missed));
        } else {
            clock.setTextColor(getColor(R.color.ink));
            status.setBackgroundResource(R.drawable.bg_reminder_badge_on_time);
            status.setTextColor(getColor(R.color.reminder_on_time));
        }
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(view ->
                MedicationReminderPopup.open(getSupportFragmentManager(), record.getId())
        );
        return item;
    }

    private void styleChip(TextView chip, String value) {
        boolean selected = value.equals(filter);
        chip.setBackgroundResource(selected
                ? R.drawable.bg_reminder_filter_selected
                : R.drawable.bg_chip);
        if (selected) {
            chip.setTextColor(getColor(R.color.white));
        } else if (ReminderRecord.STATUS_ON_TIME.equals(value)) {
            chip.setTextColor(getColor(R.color.brand_dark));
        } else {
            chip.setTextColor(getColor(R.color.muted));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
