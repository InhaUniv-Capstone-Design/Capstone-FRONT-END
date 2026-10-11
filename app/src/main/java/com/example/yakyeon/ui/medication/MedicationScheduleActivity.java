package com.example.yakyeon.ui.medication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.MedicationScheduleRepository;
import com.example.yakyeon.data.mock.MockMedicationScheduleRepository;
import com.example.yakyeon.data.model.ScheduleDose;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.NotificationEntry;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MedicationScheduleActivity extends AppCompatActivity {

    private static final String[] WEEKDAYS = {"월", "화", "수", "목", "금", "토", "일"};

    private final MedicationScheduleRepository schedule = MockMedicationScheduleRepository.getInstance();
    private final SimpleDateFormat fullDate = new SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA);

    private LinearLayout weekRow;
    private LinearLayout doseList;
    private TextView headerDate;
    private TextView selectedDate;
    private TextView countView;
    private Calendar selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medication_schedule);
        WindowSpacing.applySystemBars(findViewById(R.id.schedule_root));
        BottomNav.bind(this);
        NotificationEntry.bind(this);

        weekRow = findViewById(R.id.schedule_week);
        doseList = findViewById(R.id.schedule_doses);
        headerDate = findViewById(R.id.schedule_header_date);
        selectedDate = findViewById(R.id.schedule_selected_date);
        countView = findViewById(R.id.schedule_count);
        selected = dateOnly(Calendar.getInstance());
        showSelectedDay();
    }

    private void showSelectedDay() {
        String label = fullDate.format(selected.getTime());
        headerDate.setText(label);
        selectedDate.setText(label);
        bindWeek();
        List<ScheduleDose> doses = schedule.dosesOn(selected);
        boolean today = sameDay(selected, Calendar.getInstance());
        countView.setText(getString(
                today ? R.string.schedule_count_today : R.string.schedule_count,
                doses.size()
        ));
        doseList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < doses.size(); index++) {
            if (index > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(getColor(R.color.divider));
                doseList.addView(divider, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                ));
            }
            doseList.addView(bindDose(inflater, doses.get(index)));
        }
    }

    private void bindWeek() {
        weekRow.removeAllViews();
        Calendar cursor = weekStart(selected);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < 7; index++) {
            Calendar day = (Calendar) cursor.clone();
            View item = inflater.inflate(R.layout.item_schedule_day, weekRow, false);
            boolean chosen = sameDay(day, selected);
            boolean sunday = index == 6;
            TextView weekday = item.findViewById(R.id.schedule_weekday);
            TextView date = item.findViewById(R.id.schedule_date);
            View dot = item.findViewById(R.id.schedule_dot);
            weekday.setText(WEEKDAYS[index]);
            date.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
            item.setBackgroundResource(chosen ? R.drawable.bg_schedule_day_selected : 0);
            int textColor = chosen
                    ? getColor(R.color.white)
                    : getColor(sunday ? R.color.sunday : R.color.ink);
            int weekdayColor = chosen
                    ? getColor(R.color.white)
                    : getColor(sunday ? R.color.sunday : R.color.muted);
            weekday.setTextColor(weekdayColor);
            date.setTextColor(textColor);
            dot.setVisibility(chosen ? View.VISIBLE : View.INVISIBLE);
            item.setOnClickListener(view -> {
                selected = dateOnly(day);
                showSelectedDay();
            });
            weekRow.addView(item);
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private View bindDose(LayoutInflater inflater, ScheduleDose dose) {
        View item = inflater.inflate(R.layout.item_schedule_dose, doseList, false);
        ((TextView) item.findViewById(R.id.schedule_dose_time)).setText(dose.getTimeLabel());
        ((TextView) item.findViewById(R.id.schedule_dose_detail)).setText(
                getString(R.string.schedule_dose_detail, dose.getMedicationName(), dose.getDoseLabel())
        );
        TextView status = item.findViewById(R.id.schedule_dose_status);
        status.setText(dose.getStatus());
        ImageView icon = item.findViewById(R.id.schedule_dose_icon);
        View tile = item.findViewById(R.id.schedule_dose_icon_tile);
        boolean evening = dose.getClock().compareTo("17:00") >= 0;
        icon.setImageResource(evening ? R.drawable.ic_moon : R.drawable.ic_sun);
        if (ScheduleDose.DONE.equals(dose.getStatus())) {
            status.setBackgroundResource(R.drawable.bg_badge_time);
            status.setTextColor(getColor(R.color.white));
            tile.setBackgroundResource(R.drawable.bg_icon_tile);
        } else if (ScheduleDose.MISSED.equals(dose.getStatus())) {
            status.setBackgroundResource(R.drawable.bg_schedule_missed);
            status.setTextColor(getColor(R.color.review));
            tile.setBackgroundResource(R.drawable.bg_icon_tile_muted);
            item.setAlpha(0.7f);
        } else {
            status.setBackgroundResource(R.drawable.bg_chip);
            status.setTextColor(getColor(R.color.muted));
            tile.setBackgroundResource(R.drawable.bg_icon_tile_muted);
            item.setAlpha(0.7f);
        }
        return item;
    }

    private static Calendar weekStart(Calendar day) {
        Calendar start = (Calendar) day.clone();
        int offset = start.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                ? 6
                : start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
        start.add(Calendar.DAY_OF_MONTH, -offset);
        return dateOnly(start);
    }

    private static boolean sameDay(Calendar left, Calendar right) {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    private static Calendar dateOnly(Calendar source) {
        Calendar calendar = (Calendar) source.clone();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
