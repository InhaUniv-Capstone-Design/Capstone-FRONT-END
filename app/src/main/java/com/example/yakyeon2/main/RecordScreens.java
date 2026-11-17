package com.example.yakyeon2.main;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon2.R;
import com.example.yakyeon2.api.HomeContracts;
import com.example.yakyeon2.ui.SavedToast;
import com.example.yakyeon2.data.ClockTime;
import com.example.yakyeon2.data.DoseRecord;
import com.example.yakyeon2.data.DoseRound;
import com.example.yakyeon2.data.DoseStatus;
import com.example.yakyeon2.data.HistoryFilter;
import com.example.yakyeon2.data.MedicationRules;
import com.example.yakyeon2.data.MedicationStore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class RecordScreens {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter RANGE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter SECTION = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
    private static final String[] WEEKDAYS = {"월", "화", "수", "목", "금", "토", "일"};

    private RecordScreens() {
    }

    public static View schedule(AppCompatActivity activity, MedicationStore store, MedScreens.Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_schedule, null);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate selected = store.scheduleDate(today);
        LinearLayout week = root.findViewById(R.id.week_row);
        LocalDate start = selected.minusDays(selected.getDayOfWeek().getValue() - 1L);
        float density = activity.getResources().getDisplayMetrics().density;
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = start.plusDays(offset);
            View day = LayoutInflater.from(activity).inflate(R.layout.item_schedule_day, week, false);
            boolean chosen = date.equals(selected);
            boolean sunday = date.getDayOfWeek().getValue() == 7;
            TextView weekday = day.findViewById(R.id.text_weekday);
            TextView number = day.findViewById(R.id.text_day);
            View dot = day.findViewById(R.id.view_day_dot);
            weekday.setText(WEEKDAYS[date.getDayOfWeek().getValue() - 1]);
            number.setText(String.valueOf(date.getDayOfMonth()));
            int idle = activity.getColor(sunday ? R.color.yakyeon_sunday : R.color.yakyeon_muted);
            int idleNumber = activity.getColor(sunday ? R.color.yakyeon_sunday : R.color.yakyeon_text);
            weekday.setTextColor(chosen ? 0xCCFFFFFF : idle);
            number.setTextColor(chosen ? activity.getColor(R.color.white) : idleNumber);
            day.setBackgroundResource(chosen ? R.drawable.yakyeon_day_selected : 0);
            dot.setVisibility(chosen ? View.VISIBLE : View.INVISIBLE);
            day.setOnClickListener(v -> {
                store.setScheduleDate(date);
                host.refresh();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    Math.round(64f * density),
                    1f
            );
            if (offset < 6) {
                params.setMarginEnd(Math.round(4f * density));
            }
            week.addView(day, params);
        }
        String heading = DAY.format(selected);
        ((TextView) root.findViewById(R.id.text_schedule_date)).setText(heading);
        ((TextView) root.findViewById(R.id.text_schedule_heading)).setText(heading);
        List<DoseRecord> rows = new ArrayList<>(MedicationRules.onDate(store.records(), selected, zone));
        rows.sort(Comparator.comparingLong(DoseRecord::getScheduledAtMillis));
        long now = System.currentTimeMillis();
        rows = MedicationRules.onePerEnvelopeRound(rows, now);
        TextView empty = root.findViewById(R.id.text_schedule_empty);
        LinearLayout list = root.findViewById(R.id.schedule_rows);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        for (int index = 0; index < rows.size(); index++) {
            if (index > 0) {
                View line = new View(activity);
                line.setBackgroundColor(activity.getColor(R.color.yakyeon_divider));
                list.addView(line, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        Math.max(1, Math.round(density))
                ));
            }
            list.addView(scheduleRow(activity, store, rows.get(index), now, zone, host));
        }
        return root;
    }

    private static View scheduleRow(
            AppCompatActivity activity,
            MedicationStore store,
            DoseRecord record,
            long now,
            ZoneId zone,
            MedScreens.Host host
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_schedule_row, null);
        DoseStatus status = MedicationRules.displayStatus(record, now);
        java.time.ZonedDateTime scheduled = Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(zone);
        TextView time = row.findViewById(R.id.text_schedule_time);
        time.setText(ClockTime.format12(scheduled.getHour() * 60 + scheduled.getMinute()));
        TextView name = row.findViewById(R.id.text_schedule_name);
        name.setText(envelopeLabel(activity, record));
        ImageView icon = row.findViewById(R.id.icon_round);
        boolean night = record.getRound() == DoseRound.EVENING || record.getRound() == DoseRound.BEDTIME;
        icon.setImageResource(night ? R.drawable.ic_yakyeon_moon : R.drawable.ic_yakyeon_sun);
        TextView badge = row.findViewById(R.id.text_schedule_status);
        badge.setText(MedScreens.statusLabel(activity, status));
        int iconBg;
        int iconTint;
        int badgeBg;
        int badgeText;
        if (status == DoseStatus.COMPLETED) {
            iconBg = activity.getColor(R.color.yakyeon_primary);
            iconTint = activity.getColor(R.color.white);
            badgeBg = iconBg;
            badgeText = iconTint;
        } else if (status == DoseStatus.DELAYED) {
            iconBg = activity.getColor(R.color.yakyeon_delayed_badge);
            iconTint = activity.getColor(R.color.yakyeon_delayed_text);
            badgeBg = iconBg;
            badgeText = iconTint;
        } else if (status == DoseStatus.MISSED) {
            iconBg = activity.getColor(R.color.yakyeon_missed_badge);
            iconTint = activity.getColor(R.color.yakyeon_missed_text);
            badgeBg = iconBg;
            badgeText = iconTint;
        } else {
            iconBg = activity.getColor(R.color.yakyeon_mint);
            iconTint = activity.getColor(R.color.yakyeon_primary);
            badgeBg = activity.getColor(R.color.yakyeon_sheet);
            badgeText = iconTint;
        }
        paintRound(icon, iconBg, iconTint);
        paintPill(badge, badgeBg, badgeText);
        TextView revise = row.findViewById(R.id.text_schedule_revise);
        revise.setTextColor(activity.getColor(R.color.yakyeon_missed_text));
        revise.setPaintFlags(revise.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        if (MedicationRules.canRevise(record, now)) {
            revise.setVisibility(View.VISIBLE);
            revise.setOnClickListener(v -> openTaken(activity, store, record, host));
        }
        TextView manual = row.findViewById(R.id.text_schedule_manual);
        if (record.getTakenAtMillis() != null
                && (status == DoseStatus.COMPLETED || status == DoseStatus.DELAYED)) {
            String taken = CLOCK.format(Instant.ofEpochMilli(record.getTakenAtMillis()).atZone(zone));
            manual.setVisibility(View.VISIBLE);
            manual.setTextColor(activity.getColor(R.color.yakyeon_primary));
            manual.setTextSize(10);
            if (status == DoseStatus.DELAYED) {
                manual.setText(activity.getString(R.string.med_manual_mark, taken));
                manual.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_modal_edit, 0, 0, 0);
                manual.setCompoundDrawablePadding(Math.round(4f * activity.getResources().getDisplayMetrics().density));
            } else {
                manual.setText(activity.getString(R.string.med_taken_auto, taken));
                manual.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
        return row;
    }

    public static View history(AppCompatActivity activity, MedicationStore store, MedScreens.Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_history, null);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        bindFilter(root.findViewById(R.id.filter_all), HistoryFilter.ALL, store, host);
        bindFilter(root.findViewById(R.id.filter_completed), HistoryFilter.COMPLETED, store, host);
        bindFilter(root.findViewById(R.id.filter_delayed), HistoryFilter.DELAYED, store, host);
        bindFilter(root.findViewById(R.id.filter_missed), HistoryFilter.MISSED, store, host);
        LocalDate start = store.historyStart(today);
        LocalDate end = store.historyEnd(today);
        TextView range = root.findViewById(R.id.text_history_range);
        TextView label = root.findViewById(R.id.text_history_label);
        boolean defaultRange = start.equals(MedicationRules.defaultHistoryStart(today)) && end.equals(today);
        label.setVisibility(defaultRange ? View.VISIBLE : View.GONE);
        if (defaultRange) {
            label.setText(activity.getString(R.string.med_range_default));
        }
        range.setText(RANGE.format(start) + " - " + RANGE.format(end));
        root.findViewById(R.id.button_history_range).setOnClickListener(v -> pickRange(activity, store, start, end, host));
        long now = System.currentTimeMillis();
        long from = start.atStartOfDay(zone).toInstant().toEpochMilli();
        long to = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        MedicationRules.HistoryPage page = MedicationRules.history(
                store.records(),
                store.historyFilter(),
                from,
                to,
                now
        );
        TextView empty = root.findViewById(R.id.text_history_empty);
        if (!page.hasRecords) {
            empty.setText(activity.getString(R.string.med_history_empty));
        } else if (page.visible.isEmpty()) {
            empty.setText(activity.getString(R.string.med_history_filter_empty));
        } else {
            empty.setVisibility(View.GONE);
        }
        LinearLayout list = root.findViewById(R.id.history_rows);
        LocalDate previous = null;
        List<DoseRecord> visible = new ArrayList<>(page.visible);
        visible.sort((left, right) -> {
            LocalDate leftDate = Instant.ofEpochMilli(left.getScheduledAtMillis()).atZone(zone).toLocalDate();
            LocalDate rightDate = Instant.ofEpochMilli(right.getScheduledAtMillis()).atZone(zone).toLocalDate();
            int byDate = rightDate.compareTo(leftDate);
            if (byDate != 0) {
                return byDate;
            }
            return Long.compare(left.getScheduledAtMillis(), right.getScheduledAtMillis());
        });
        float density = activity.getResources().getDisplayMetrics().density;
        int pad = Math.round(18f * density);
        for (DoseRecord record : visible) {
            LocalDate date = Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(zone).toLocalDate();
            if (!date.equals(previous)) {
                if (previous != null) {
                    View gap = new View(activity);
                    gap.setBackgroundColor(activity.getColor(R.color.yakyeon_history_gap));
                    list.addView(gap, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(8f * density)
                    ));
                }
                TextView section = new TextView(activity);
                section.setText(sectionLabel(activity, today, date));
                section.setTypeface(null, Typeface.BOLD);
                section.setTextColor(activity.getColor(R.color.yakyeon_text));
                section.setTextSize(13f);
                section.setPadding(pad, Math.round(12f * density), pad, Math.round(6f * density));
                list.addView(section);
                previous = date;
            }
            list.addView(historyRow(activity, record, now, zone, density));
        }
        return root;
    }

    private static View historyRow(
            AppCompatActivity activity,
            DoseRecord record,
            long now,
            ZoneId zone,
            float density
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_history_row, null);
        DoseStatus status = MedicationRules.displayStatus(record, now);
        long shown = record.getTakenAtMillis() != null
                && (status == DoseStatus.COMPLETED || status == DoseStatus.DELAYED)
                ? record.getTakenAtMillis()
                : record.getScheduledAtMillis();
        TextView time = row.findViewById(R.id.text_history_time);
        time.setText(CLOCK.format(Instant.ofEpochMilli(shown).atZone(zone)));
        TextView name = row.findViewById(R.id.text_history_name);
        name.setText(recordLabel(activity, record));
        TextView badge = row.findViewById(R.id.text_history_status);
        badge.setText("· " + MedScreens.statusLabel(activity, status));
        int timeColor = activity.getColor(R.color.yakyeon_text);
        int rowColor = activity.getColor(R.color.white);
        int badgeBg = activity.getColor(R.color.yakyeon_history_done_bg);
        int badgeText = activity.getColor(R.color.yakyeon_history_done_text);
        if (status == DoseStatus.DELAYED) {
            timeColor = activity.getColor(R.color.yakyeon_delayed_text);
            rowColor = activity.getColor(R.color.yakyeon_delayed_row);
            badgeBg = activity.getColor(R.color.yakyeon_delayed_badge);
            badgeText = timeColor;
        } else if (status == DoseStatus.MISSED) {
            timeColor = activity.getColor(R.color.yakyeon_missed_text);
            rowColor = activity.getColor(R.color.yakyeon_missed_row);
            badgeBg = activity.getColor(R.color.yakyeon_missed_badge);
            badgeText = timeColor;
        }
        time.setTextColor(timeColor);
        GradientDrawable background = new GradientDrawable();
        background.setColor(rowColor);
        background.setCornerRadius(10f * density);
        row.setBackground(background);
        paintPill(badge, badgeBg, badgeText);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.round(48f * density)
        );
        params.setMarginStart(Math.round(12f * density));
        params.setMarginEnd(Math.round(12f * density));
        row.setLayoutParams(params);
        return row;
    }

    private static String envelopeLabel(AppCompatActivity activity, DoseRecord record) {
        if (record.getEnvelopeName() != null && !record.getEnvelopeName().trim().isEmpty()) {
            return record.getEnvelopeName().trim();
        }
        return activity.getString(R.string.med_slot_empty_name);
    }

    private static String recordLabel(AppCompatActivity activity, DoseRecord record) {
        if (record.getEnvelopeName() != null && !record.getEnvelopeName().trim().isEmpty()) {
            return record.getEnvelopeName();
        }
        if (record.getMedicineName() != null && !record.getMedicineName().trim().isEmpty()) {
            return record.getMedicineName();
        }
        return activity.getString(R.string.med_name_missing);
    }

    private static void paintRound(ImageView icon, int background, int tint) {
        float radius = 12f * icon.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radius);
        drawable.setColor(background);
        icon.setBackground(drawable);
        icon.setImageTintList(ColorStateList.valueOf(tint));
    }

    private static void paintPill(TextView badge, int background, int text) {
        float radius = 999f * badge.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radius);
        drawable.setColor(background);
        badge.setBackground(drawable);
        badge.setTextColor(text);
    }

    private static void openTaken(
            AppCompatActivity activity,
            MedicationStore store,
            DoseRecord record,
            MedScreens.Host host
    ) {
        View sheet = LayoutInflater.from(activity).inflate(R.layout.dialog_dose_sheet, null);
        String hospital = record.getEnvelopeName();
        if (hospital == null || hospital.trim().isEmpty()) {
            hospital = activity.getString(R.string.med_slot_empty_name);
        }
        ((TextView) sheet.findViewById(R.id.text_sheet_title)).setText(activity.getString(
                R.string.med_taken_title_named,
                hospital.trim(),
                MedScreens.roundLabel(activity, record.getRound())
        ));
        TextView message = sheet.findViewById(R.id.text_sheet_message);
        ZoneId zone = ZoneId.systemDefault();
        String clock = ClockTime.format12(Instant.ofEpochMilli(record.getScheduledAtMillis())
                .atZone(zone).getHour() * 60
                + Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(zone).getMinute());
        message.setVisibility(View.VISIBLE);
        message.setText(activity.getString(R.string.med_taken_missed, clock));
        LinearLayout options = sheet.findViewById(R.id.sheet_options);
        Dialog dialog = presentSheet(activity, sheet);
        options.addView(doseOption(activity, activity.getString(R.string.med_ate), null, true, false, () -> {
            dialog.dismiss();
            openRound(activity, store, record, host);
        }));
        options.addView(doseOption(activity, activity.getString(R.string.med_not_ate), null, false, true, dialog::dismiss));
        sheet.findViewById(R.id.button_sheet_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static void openRound(
            AppCompatActivity activity,
            MedicationStore store,
            DoseRecord origin,
            MedScreens.Host host
    ) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate date = Instant.ofEpochMilli(origin.getScheduledAtMillis()).atZone(zone).toLocalDate();
        long now = System.currentTimeMillis();
        java.util.LinkedHashMap<String, List<DoseRecord>> groups = new java.util.LinkedHashMap<>();
        for (DoseRecord candidate : MedicationRules.onDate(store.records(), date, zone)) {
            if (!origin.getEnvelopeId().equals(candidate.getEnvelopeId())) {
                continue;
            }
            String key = candidate.getRound().name() + "\u001f" + candidate.getScheduledAtMillis();
            List<DoseRecord> group = groups.get(key);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(key, group);
            }
            group.add(candidate);
        }
        long currentMillis = -1L;
        for (List<DoseRecord> group : groups.values()) {
            long scheduled = group.get(0).getScheduledAtMillis();
            if (scheduled <= now && scheduled > currentMillis) {
                currentMillis = scheduled;
            }
        }
        View sheet = LayoutInflater.from(activity).inflate(R.layout.dialog_dose_sheet, null);
        ((TextView) sheet.findViewById(R.id.text_sheet_title)).setText(R.string.med_round_title);
        LinearLayout options = sheet.findViewById(R.id.sheet_options);
        Dialog dialog = presentSheet(activity, sheet);
        boolean firstOption = true;
        for (List<DoseRecord> group : groups.values()) {
            DoseRecord sample = group.get(0);
            List<DoseRecord> revisable = new ArrayList<>();
            for (DoseRecord candidate : group) {
                if (MedicationRules.canRevise(candidate, now)) {
                    revisable.add(candidate);
                }
            }
            boolean allowed = !revisable.isEmpty();
            java.time.ZonedDateTime when = Instant.ofEpochMilli(sample.getScheduledAtMillis()).atZone(zone);
            String clock = ClockTime.format12(when.getHour() * 60 + when.getMinute());
            String slot;
            if (sample.getScheduledAtMillis() == currentMillis) {
                slot = activity.getString(R.string.med_round_current);
            } else if (currentMillis > 0 && sample.getScheduledAtMillis() < currentMillis) {
                slot = activity.getString(R.string.med_round_previous);
            } else {
                slot = MedScreens.roundLabel(activity, sample.getRound());
            }
            String title = activity.getString(R.string.med_round_slot, clock, slot);
            String detail;
            if (!allowed) {
                int reason = MedicationRules.reviseBlock(sample, now) == MedicationRules.ReviseBlock.NOT_YET
                        ? R.string.med_round_not_yet
                        : R.string.med_round_blocked;
                detail = activity.getString(reason);
            } else {
                detail = activity.getString(
                        R.string.med_round_ate,
                        MedScreens.roundLabel(activity, sample.getRound())
                );
            }
            boolean selected = allowed && sample.getScheduledAtMillis() == origin.getScheduledAtMillis();
            View option = doseOption(activity, title, detail, selected, !firstOption, allowed ? () -> {
                dialog.dismiss();
                openTime(activity, store, revisable, host, null);
            } : null);
            firstOption = false;
            options.addView(option);
        }
        sheet.findViewById(R.id.button_sheet_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static void openTime(
            AppCompatActivity activity,
            MedicationStore store,
            List<DoseRecord> records,
            MedScreens.Host host,
            Runnable reopenList
    ) {
        ZoneId zone = ZoneId.systemDefault();
        DoseRecord sample = records.get(0);
        long[] takenAt = new long[] {System.currentTimeMillis()};
        int[] hour = new int[] {Instant.ofEpochMilli(takenAt[0]).atZone(zone).getHour()};
        int[] minute = new int[] {Instant.ofEpochMilli(takenAt[0]).atZone(zone).getMinute()};
        View sheet = LayoutInflater.from(activity).inflate(R.layout.dialog_taken_time, null);
        bindTimeIdentity(sheet, sample.getEnvelopeName(), medicineNames(records));
        java.time.ZonedDateTime scheduled = Instant.ofEpochMilli(sample.getScheduledAtMillis()).atZone(zone);
        ((TextView) sheet.findViewById(R.id.text_time_target)).setText(activity.getString(
                R.string.med_time_target,
                ClockTime.format12(scheduled.getHour() * 60 + scheduled.getMinute())
        ));
        Runnable paint = () -> {
            ((TextView) sheet.findViewById(R.id.text_hour_prev)).setText(clockPart(hour[0] - 1, 24));
            ((TextView) sheet.findViewById(R.id.text_hour_value)).setText(clockPart(hour[0], 24));
            ((TextView) sheet.findViewById(R.id.text_hour_next)).setText(clockPart(hour[0] + 1, 24));
            ((TextView) sheet.findViewById(R.id.text_minute_prev)).setText(clockPart(minute[0] - 1, 60));
            ((TextView) sheet.findViewById(R.id.text_minute_value)).setText(clockPart(minute[0], 60));
            ((TextView) sheet.findViewById(R.id.text_minute_next)).setText(clockPart(minute[0] + 1, 60));
        };
        paint.run();
        sheet.findViewById(R.id.text_hour_prev).setOnClickListener(v -> shiftClock(zone, takenAt, hour, minute, -1, 0, paint));
        sheet.findViewById(R.id.text_hour_next).setOnClickListener(v -> shiftClock(zone, takenAt, hour, minute, 1, 0, paint));
        sheet.findViewById(R.id.text_minute_prev).setOnClickListener(v -> shiftClock(zone, takenAt, hour, minute, 0, -1, paint));
        sheet.findViewById(R.id.text_minute_next).setOnClickListener(v -> shiftClock(zone, takenAt, hour, minute, 0, 1, paint));
        Dialog dialog = presentSheet(activity, sheet);
        sheet.findViewById(R.id.button_time_cancel).setOnClickListener(v -> {
            dialog.dismiss();
            if (reopenList != null) {
                reopenList.run();
            }
        });
        sheet.findViewById(R.id.button_time_save).setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            boolean saved = false;
            for (DoseRecord record : records) {
                if (store.recordManual(record, now, takenAt[0])) {
                    saved = true;
                }
            }
            dialog.dismiss();
            if (saved) {
                host.refresh();
                String scheduledClock = String.format(Locale.KOREAN, "%02d:%02d", scheduled.getHour(), scheduled.getMinute());
                String actualClock = String.format(Locale.KOREAN, "%02d:%02d", hour[0], minute[0]);
                SavedToast.show(
                        activity,
                        activity.getString(R.string.med_time_saved),
                        activity.getString(
                                R.string.med_time_saved_detail,
                                MedScreens.roundLabel(activity, sample.getRound()),
                                scheduledClock,
                                actualClock
                        )
                );
            } else {
                host.showMessage(activity.getString(R.string.med_readonly));
            }
            if (reopenList != null) {
                reopenList.run();
            }
        });
        dialog.show();
    }

    private static void shiftClock(
            ZoneId zone,
            long[] takenAt,
            int[] hour,
            int[] minute,
            int hourDelta,
            int minuteDelta,
            Runnable paint
    ) {
        java.time.ZonedDateTime current = Instant.ofEpochMilli(takenAt[0]).atZone(zone);
        java.time.ZonedDateTime picked = current.plusHours(hourDelta).plusMinutes(minuteDelta);
        if (picked.toInstant().toEpochMilli() > System.currentTimeMillis()) {
            return;
        }
        hour[0] = picked.getHour();
        minute[0] = picked.getMinute();
        takenAt[0] = picked.toInstant().toEpochMilli();
        paint.run();
    }

    private static String clockPart(int value, int modulo) {
        return String.format(Locale.KOREAN, "%02d", Math.floorMod(value, modulo));
    }

    private static View doseOption(
            AppCompatActivity activity,
            String title,
            String detail,
            boolean selected,
            boolean gap,
            Runnable action
    ) {
        View option = LayoutInflater.from(activity).inflate(R.layout.item_dose_option, null);
        option.setBackgroundResource(selected ? R.drawable.bg_sheet_option_on : R.drawable.bg_sheet_option_off);
        option.findViewById(R.id.option_radio).setBackgroundResource(
                selected ? R.drawable.bg_sheet_radio_on : R.drawable.bg_sheet_radio_off
        );
        option.findViewById(R.id.option_check).setVisibility(selected ? View.VISIBLE : View.GONE);
        ((TextView) option.findViewById(R.id.text_option_title)).setText(title);
        TextView body = option.findViewById(R.id.text_option_detail);
        if (detail == null || detail.isEmpty()) {
            body.setVisibility(View.GONE);
        } else {
            body.setVisibility(View.VISIBLE);
            body.setText(detail);
        }
        if (action != null) {
            option.setOnClickListener(v -> action.run());
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (gap) {
            params.topMargin = Math.round(8f * activity.getResources().getDisplayMetrics().density);
        }
        option.setLayoutParams(params);
        return option;
    }

    private static Dialog presentSheet(AppCompatActivity activity, View content) {
        Dialog dialog = new Dialog(activity, R.style.ThemeOverlay_Yakyeon_Popup);
        dialog.setContentView(content);
        dialog.setCancelable(true);
        dialog.setOnShowListener(shown -> {
            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(
                    metrics.widthPixels - Math.round(16f * metrics.density),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        });
        return dialog;
    }

    private static void bindFilter(TextView chip, HistoryFilter filter, MedicationStore store, MedScreens.Host host) {
        boolean selected = store.historyFilter() == filter;
        chip.setBackgroundResource(selected ? R.drawable.yakyeon_chip_on : R.drawable.yakyeon_chip_off);
        int idle = filter == HistoryFilter.COMPLETED
                ? chip.getContext().getColor(R.color.yakyeon_brand)
                : chip.getContext().getColor(R.color.yakyeon_muted);
        chip.setTextColor(selected ? chip.getContext().getColor(R.color.white) : idle);
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setOnClickListener(v -> {
            store.setHistoryFilter(filter);
            host.refresh();
        });
    }

    private static void pickRange(
            AppCompatActivity activity,
            MedicationStore store,
            LocalDate start,
            LocalDate end,
            MedScreens.Host host
    ) {
        new DatePickerDialog(activity, (view, year, month, day) -> {
            LocalDate pickedStart = LocalDate.of(year, month + 1, day);
            new DatePickerDialog(activity, (endView, endYear, endMonth, endDay) -> {
                LocalDate pickedEnd = LocalDate.of(endYear, endMonth + 1, endDay);
                if (!store.setHistoryRange(pickedStart, pickedEnd)) {
                    host.showMessage(activity.getString(R.string.med_range_max));
                    return;
                }
                host.refresh();
            }, end.getYear(), end.getMonthValue() - 1, end.getDayOfMonth()).show();
        }, start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth()).show();
    }

    public static View serverSchedule(
            AppCompatActivity activity,
            LocalDate selected,
            MainScreens.CareView view,
            String message,
            List<HomeContracts.Dose> doses,
            boolean writes,
            MedScreens.Host host
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_schedule, null);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate shown = selected == null ? today : selected;
        LinearLayout week = root.findViewById(R.id.week_row);
        LocalDate start = shown.minusDays(shown.getDayOfWeek().getValue() - 1L);
        float density = activity.getResources().getDisplayMetrics().density;
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = start.plusDays(offset);
            View day = LayoutInflater.from(activity).inflate(R.layout.item_schedule_day, week, false);
            boolean chosen = date.equals(shown);
            boolean sunday = date.getDayOfWeek().getValue() == 7;
            TextView weekday = day.findViewById(R.id.text_weekday);
            TextView number = day.findViewById(R.id.text_day);
            View dot = day.findViewById(R.id.view_day_dot);
            weekday.setText(WEEKDAYS[date.getDayOfWeek().getValue() - 1]);
            number.setText(String.valueOf(date.getDayOfMonth()));
            int idle = activity.getColor(sunday ? R.color.yakyeon_sunday : R.color.yakyeon_muted);
            int idleNumber = activity.getColor(sunday ? R.color.yakyeon_sunday : R.color.yakyeon_text);
            weekday.setTextColor(chosen ? 0xCCFFFFFF : idle);
            number.setTextColor(chosen ? activity.getColor(R.color.white) : idleNumber);
            day.setBackgroundResource(chosen ? R.drawable.yakyeon_day_selected : 0);
            dot.setVisibility(chosen ? View.VISIBLE : View.INVISIBLE);
            day.setOnClickListener(v -> host.selectServerScheduleDate(date));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Math.round(64f * density), 1f);
            if (offset < 6) {
                params.setMarginEnd(Math.round(4f * density));
            }
            week.addView(day, params);
        }
        String heading = DAY.format(shown);
        ((TextView) root.findViewById(R.id.text_schedule_date)).setText(heading);
        ((TextView) root.findViewById(R.id.text_schedule_heading)).setText(heading);
        TextView empty = root.findViewById(R.id.text_schedule_empty);
        LinearLayout list = root.findViewById(R.id.schedule_rows);
        if (view != MainScreens.CareView.READY) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(view == MainScreens.CareView.LOADING
                    ? activity.getString(R.string.home_loading)
                    : (message == null ? activity.getString(R.string.api_response_error) : message));
            list.setVisibility(View.GONE);
            return root;
        }
        List<HomeContracts.Dose> rows = doses == null ? new ArrayList<>() : new ArrayList<>(doses);
        rows.sort(Comparator.comparing(dose -> dose.scheduledAt));
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);
        for (int index = 0; index < rows.size(); index++) {
            if (index > 0) {
                View line = new View(activity);
                line.setBackgroundColor(activity.getColor(R.color.yakyeon_divider));
                list.addView(line, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        Math.max(1, Math.round(density))
                ));
            }
            list.addView(serverScheduleRow(activity, rows.get(index), writes, host));
        }
        return root;
    }

    public static View serverHistory(
            AppCompatActivity activity,
            MainScreens.CareView view,
            String message,
            String status,
            LocalDate from,
            LocalDate to,
            List<HomeContracts.Dose> doses,
            MedScreens.Host host
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_history, null);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        paintHistoryChip(root.findViewById(R.id.filter_all), status == null, () -> host.selectServerHistoryStatus(null));
        paintHistoryChip(root.findViewById(R.id.filter_completed), HomeContracts.STATUS_NORMAL.equals(status),
                () -> host.selectServerHistoryStatus(HomeContracts.STATUS_NORMAL));
        paintHistoryChip(root.findViewById(R.id.filter_delayed), HomeContracts.STATUS_DELAYED.equals(status),
                () -> host.selectServerHistoryStatus(HomeContracts.STATUS_DELAYED));
        paintHistoryChip(root.findViewById(R.id.filter_missed), HomeContracts.STATUS_MISSED.equals(status),
                () -> host.selectServerHistoryStatus(HomeContracts.STATUS_MISSED));
        TextView range = root.findViewById(R.id.text_history_range);
        TextView label = root.findViewById(R.id.text_history_label);
        boolean defaultRange = from == null || to == null;
        label.setVisibility(defaultRange ? View.VISIBLE : View.GONE);
        if (defaultRange) {
            label.setText(activity.getString(R.string.med_range_default));
            range.setText(activity.getString(R.string.med_range_default));
        } else {
            range.setText(RANGE.format(from) + " - " + RANGE.format(to));
        }
        LocalDate rangeStart = from == null ? today.minusDays(13) : from;
        LocalDate rangeEnd = to == null ? today : to;
        root.findViewById(R.id.button_history_range).setOnClickListener(v -> pickServerRange(
                activity,
                rangeStart,
                rangeEnd,
                host
        ));
        TextView empty = root.findViewById(R.id.text_history_empty);
        LinearLayout list = root.findViewById(R.id.history_rows);
        if (view != MainScreens.CareView.READY) {
            empty.setText(view == MainScreens.CareView.LOADING
                    ? activity.getString(R.string.home_loading)
                    : (message == null ? activity.getString(R.string.api_response_error) : message));
            return root;
        }
        if (doses == null || doses.isEmpty()) {
            empty.setText(activity.getString(R.string.med_history_empty));
            return root;
        }
        empty.setVisibility(View.GONE);
        float density = activity.getResources().getDisplayMetrics().density;
        int pad = Math.round(18f * density);
        LocalDate previous = null;
        for (HomeContracts.Dose dose : doses) {
            LocalDate date = HomeContracts.seoulDate(dose.scheduledAt);
            if (date == null) {
                date = today;
            }
            if (!date.equals(previous)) {
                TextView section = new TextView(activity);
                section.setText(sectionLabel(activity, today, date));
                section.setTypeface(null, Typeface.BOLD);
                section.setTextColor(activity.getColor(R.color.yakyeon_text));
                section.setTextSize(13f);
                section.setPadding(pad, Math.round(12f * density), pad, Math.round(6f * density));
                list.addView(section);
                previous = date;
            }
            list.addView(serverHistoryRow(activity, dose, density));
        }
        return root;
    }

    private static void paintHistoryChip(TextView chip, boolean selected, Runnable action) {
        chip.setBackgroundResource(selected ? R.drawable.yakyeon_chip_on : R.drawable.yakyeon_chip_off);
        chip.setOnClickListener(v -> action.run());
    }

    private static View serverScheduleRow(
            AppCompatActivity activity,
            HomeContracts.Dose dose,
            boolean writes,
            MedScreens.Host host
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_schedule_row, null);
        TextView time = row.findViewById(R.id.text_schedule_time);
        time.setText(HomeContracts.clock(dose.scheduledAt));
        TextView name = row.findViewById(R.id.text_schedule_name);
        String hospital = dose.hospitalName == null || dose.hospitalName.trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : dose.hospitalName.trim();
        StringBuilder drugs = new StringBuilder();
        for (HomeContracts.DoseDrug drug : dose.medications) {
            if (drugs.length() > 0) {
                drugs.append(", ");
            }
            drugs.append(drug.drugName);
        }
        name.setText(hospital + " · " + drugs);
        ImageView icon = row.findViewById(R.id.icon_round);
        boolean night = HomeContracts.SLOT_DINNER.equals(dose.timingSlot)
                || HomeContracts.SLOT_BEDTIME.equals(dose.timingSlot);
        icon.setImageResource(night ? R.drawable.ic_yakyeon_moon : R.drawable.ic_yakyeon_sun);
        Instant now = Instant.now();
        String shown = HomeContracts.scheduleStatus(dose);
        TextView badge = row.findViewById(R.id.text_schedule_status);
        badge.setText(serverStatusLabel(activity, shown));
        int iconBg = activity.getColor(R.color.yakyeon_mint);
        int iconTint = activity.getColor(R.color.yakyeon_primary);
        if (HomeContracts.STATUS_NORMAL.equals(shown)) {
            iconBg = activity.getColor(R.color.yakyeon_primary);
            iconTint = activity.getColor(R.color.white);
        } else if (HomeContracts.STATUS_DELAYED.equals(shown)) {
            iconBg = activity.getColor(R.color.yakyeon_delayed_badge);
            iconTint = activity.getColor(R.color.yakyeon_delayed_text);
        } else if (HomeContracts.STATUS_MISSED.equals(shown)) {
            iconBg = activity.getColor(R.color.yakyeon_missed_badge);
            iconTint = activity.getColor(R.color.yakyeon_missed_text);
        }
        paintRound(icon, iconBg, iconTint);
        paintPill(badge, iconBg, iconTint);
        TextView revise = row.findViewById(R.id.text_schedule_revise);
        TextView manual = row.findViewById(R.id.text_schedule_manual);
        revise.setVisibility(View.GONE);
        manual.setVisibility(View.GONE);
        HomeContracts.DoseFollowUp follow = HomeContracts.followUp(dose, now);
        if (follow == HomeContracts.DoseFollowUp.ASK && writes) {
            revise.setVisibility(View.VISIBLE);
            revise.setText(R.string.med_dose_ask);
            revise.setTextColor(activity.getColor(R.color.yakyeon_delayed_text));
            revise.setOnClickListener(v -> askTakenTime(activity, dose, host, null));
        } else if (follow == HomeContracts.DoseFollowUp.RECORDED && dose.takenAt != null) {
            String clock = HomeContracts.clock(dose.takenAt);
            if (!clock.isEmpty()) {
                manual.setVisibility(View.VISIBLE);
                manual.setText(clock);
                manual.setTextColor(activity.getColor(R.color.yakyeon_text));
                manual.setOnClickListener(null);
            }
        }
        return row;
    }

    private static View serverHistoryRow(AppCompatActivity activity, HomeContracts.Dose dose, float density) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_history_row, null);
        TextView time = row.findViewById(R.id.text_history_time);
        time.setText(dose.takenAt != null ? HomeContracts.clock(dose.takenAt) : HomeContracts.clock(dose.scheduledAt));
        TextView name = row.findViewById(R.id.text_history_name);
        String label = dose.hospitalName == null || dose.hospitalName.trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : dose.hospitalName.trim();
        if (dose.corrected) {
            label = label + " · " + activity.getString(R.string.med_corrected);
        }
        name.setText(label);
        TextView badge = row.findViewById(R.id.text_history_status);
        badge.setText("· " + serverStatusLabel(activity, dose.status));
        int timeColor = activity.getColor(R.color.yakyeon_text);
        int rowColor = activity.getColor(R.color.white);
        int badgeBg = activity.getColor(R.color.yakyeon_history_done_bg);
        int badgeText = activity.getColor(R.color.yakyeon_history_done_text);
        if (HomeContracts.STATUS_DELAYED.equals(dose.status)) {
            timeColor = activity.getColor(R.color.yakyeon_delayed_text);
            rowColor = activity.getColor(R.color.yakyeon_delayed_row);
            badgeBg = activity.getColor(R.color.yakyeon_delayed_badge);
            badgeText = timeColor;
        } else if (HomeContracts.STATUS_MISSED.equals(dose.status)) {
            timeColor = activity.getColor(R.color.yakyeon_missed_text);
            rowColor = activity.getColor(R.color.yakyeon_missed_row);
            badgeBg = activity.getColor(R.color.yakyeon_missed_badge);
            badgeText = timeColor;
        }
        time.setTextColor(timeColor);
        GradientDrawable background = new GradientDrawable();
        background.setColor(rowColor);
        background.setCornerRadius(10f * density);
        row.setBackground(background);
        paintPill(badge, badgeBg, badgeText);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.round(48f * density)
        );
        params.setMarginStart(Math.round(12f * density));
        params.setMarginEnd(Math.round(12f * density));
        row.setLayoutParams(params);
        return row;
    }

    public static void checkServerDoses(
            AppCompatActivity activity,
            List<HomeContracts.Dose> doses,
            MedScreens.Host host
    ) {
        checkServerDoses(activity, doses, host, true);
    }

    public static void checkServerDoses(
            AppCompatActivity activity,
            List<HomeContracts.Dose> doses,
            MedScreens.Host host,
            boolean announceIfEmpty
    ) {
        Instant now = Instant.now();
        List<HomeContracts.Dose> pending = new ArrayList<>();
        if (doses != null) {
            for (HomeContracts.Dose dose : doses) {
                if (HomeContracts.followUp(dose, now) == HomeContracts.DoseFollowUp.ASK) {
                    pending.add(dose);
                }
            }
        }
        pending.sort(Comparator.comparing(dose -> dose.scheduledAt == null ? "" : dose.scheduledAt));
        if (pending.isEmpty()) {
            if (announceIfEmpty) {
                host.showMessage(activity.getString(R.string.home_remaining_empty));
            }
            return;
        }
        showServerUncheckedList(activity, doses, pending, host);
    }

    private static void showServerUncheckedList(
            AppCompatActivity activity,
            List<HomeContracts.Dose> doses,
            List<HomeContracts.Dose> pending,
            MedScreens.Host host
    ) {
        View sheet = LayoutInflater.from(activity).inflate(R.layout.dialog_dose_sheet, null);
        ((TextView) sheet.findViewById(R.id.text_sheet_title)).setText(R.string.home_overdue_title);
        TextView message = sheet.findViewById(R.id.text_sheet_message);
        message.setVisibility(View.VISIBLE);
        message.setText(R.string.med_dose_ask);
        LinearLayout options = sheet.findViewById(R.id.sheet_options);
        Dialog dialog = presentSheet(activity, sheet);
        boolean first = true;
        for (HomeContracts.Dose dose : pending) {
            String title = MainScreens.slotLabel(activity, dose.timingSlot)
                    + " "
                    + HomeContracts.clock(dose.scheduledAt);
            View option = doseOption(activity, title, serverDoseDetail(activity, dose), false, !first, () -> {
                dialog.dismiss();
                askTakenTime(activity, dose, host, null, () -> checkServerDoses(activity, doses, host));
            });
            first = false;
            options.addView(option);
        }
        sheet.findViewById(R.id.button_sheet_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        host.onUncheckedDoseListShown(dialog);
    }

    public static void checkLocalDoses(
            AppCompatActivity activity,
            MedicationStore store,
            MedScreens.Host host
    ) {
        checkLocalDoses(activity, store, host, true);
    }

    private static void checkLocalDoses(
            AppCompatActivity activity,
            MedicationStore store,
            MedScreens.Host host,
            boolean announceIfEmpty
    ) {
        ZoneId zone = ZoneId.systemDefault();
        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.now(zone);
        List<DoseRecord> pending = new ArrayList<>();
        for (DoseRecord record : MedicationRules.onDate(store.records(), today, zone)) {
            if (MedicationRules.canRevise(record, now)) {
                pending.add(record);
            }
        }
        pending.sort(Comparator.comparingLong(DoseRecord::getScheduledAtMillis));
        LinkedHashMap<String, List<DoseRecord>> groups = new LinkedHashMap<>();
        for (DoseRecord record : pending) {
            String key = record.getEnvelopeId()
                    + "\u001f"
                    + record.getRound().name()
                    + "\u001f"
                    + record.getScheduledAtMillis();
            List<DoseRecord> group = groups.get(key);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(key, group);
            }
            group.add(record);
        }
        if (groups.isEmpty()) {
            if (announceIfEmpty) {
                host.showMessage(activity.getString(R.string.home_remaining_empty));
            }
            return;
        }
        showLocalUncheckedList(activity, store, host, groups);
    }

    private static void showLocalUncheckedList(
            AppCompatActivity activity,
            MedicationStore store,
            MedScreens.Host host,
            LinkedHashMap<String, List<DoseRecord>> groups
    ) {
        ZoneId zone = ZoneId.systemDefault();
        View sheet = LayoutInflater.from(activity).inflate(R.layout.dialog_dose_sheet, null);
        ((TextView) sheet.findViewById(R.id.text_sheet_title)).setText(R.string.home_overdue_title);
        TextView message = sheet.findViewById(R.id.text_sheet_message);
        message.setVisibility(View.VISIBLE);
        message.setText(R.string.med_dose_ask);
        LinearLayout options = sheet.findViewById(R.id.sheet_options);
        Dialog dialog = presentSheet(activity, sheet);
        boolean first = true;
        for (List<DoseRecord> group : groups.values()) {
            DoseRecord sample = group.get(0);
            java.time.ZonedDateTime when = Instant.ofEpochMilli(sample.getScheduledAtMillis()).atZone(zone);
            String title = MedScreens.roundLabel(activity, sample.getRound())
                    + " "
                    + String.format(Locale.KOREAN, "%02d:%02d", when.getHour(), when.getMinute());
            String detail = sample.getEnvelopeName() == null ? "" : sample.getEnvelopeName().trim();
            List<DoseRecord> chosen = group;
            View option = doseOption(activity, title, detail, false, !first, () -> {
                dialog.dismiss();
                openTime(activity, store, chosen, host, () -> checkLocalDoses(activity, store, host, false));
            });
            first = false;
            options.addView(option);
        }
        sheet.findViewById(R.id.button_sheet_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static void bindTimeIdentity(View sheet, String hospital, String medicines) {
        TextView hospitalView = sheet.findViewById(R.id.text_time_hospital);
        TextView medicineView = sheet.findViewById(R.id.text_time_medicines);
        View block = sheet.findViewById(R.id.time_dose_identity);
        String hospitalText = hospital == null ? "" : hospital.trim();
        String medicineText = medicines == null ? "" : medicines.trim();
        hospitalView.setText(hospitalText);
        medicineView.setText(medicineText);
        hospitalView.setVisibility(hospitalText.isEmpty() ? View.GONE : View.VISIBLE);
        medicineView.setVisibility(medicineText.isEmpty() ? View.GONE : View.VISIBLE);
        block.setVisibility(hospitalText.isEmpty() && medicineText.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static String medicineNames(List<DoseRecord> records) {
        StringBuilder names = new StringBuilder();
        if (records == null) {
            return "";
        }
        for (DoseRecord record : records) {
            if (record == null || record.getMedicineName() == null) {
                continue;
            }
            String name = record.getMedicineName().trim();
            if (name.isEmpty()) {
                continue;
            }
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(name);
        }
        return names.toString();
    }

    private static String serverMedicineNames(HomeContracts.Dose dose) {
        StringBuilder names = new StringBuilder();
        if (dose == null || dose.medications == null) {
            return "";
        }
        for (HomeContracts.DoseDrug drug : dose.medications) {
            if (drug == null || drug.drugName == null) {
                continue;
            }
            String name = drug.drugName.trim();
            if (name.isEmpty()) {
                continue;
            }
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(name);
        }
        return names.toString();
    }

    private static String serverDoseDetail(AppCompatActivity activity, HomeContracts.Dose dose) {
        String hospital = dose.hospitalName == null || dose.hospitalName.trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : dose.hospitalName.trim();
        StringBuilder drugs = new StringBuilder();
        if (dose.medications != null) {
            for (HomeContracts.DoseDrug drug : dose.medications) {
                if (drug == null || drug.drugName == null || drug.drugName.trim().isEmpty()) {
                    continue;
                }
                if (drugs.length() > 0) {
                    drugs.append(", ");
                }
                drugs.append(drug.drugName.trim());
            }
        }
        if (drugs.length() == 0) {
            return hospital;
        }
        return hospital + " · " + drugs;
    }

    public static Dialog askTakenTime(
            AppCompatActivity activity,
            HomeContracts.Dose dose,
            MedScreens.Host host,
            String title
    ) {
        return askTakenTime(activity, dose, host, title, null);
    }

    public static Dialog askTakenTime(
            AppCompatActivity activity,
            HomeContracts.Dose dose,
            MedScreens.Host host,
            String title,
            Runnable reopenList
    ) {
        ZoneId zone = ZoneId.systemDefault();
        long[] takenAt = new long[] {System.currentTimeMillis()};
        int[] hour = new int[] {Instant.ofEpochMilli(takenAt[0]).atZone(zone).getHour()};
        int[] minute = new int[] {Instant.ofEpochMilli(takenAt[0]).atZone(zone).getMinute()};
        View sheet = LayoutInflater.from(activity).inflate(R.layout.dialog_taken_time, null);
        bindTimeIdentity(sheet, dose.hospitalName, serverMedicineNames(dose));
        if (title != null && !title.isEmpty()) {
            ((TextView) sheet.findViewById(R.id.text_time_title)).setText(title);
        }
        ((TextView) sheet.findViewById(R.id.text_time_target)).setText(
                MainScreens.slotLabel(activity, dose.timingSlot) + " " + HomeContracts.clock(dose.scheduledAt)
        );
        Runnable paint = () -> {
            ((TextView) sheet.findViewById(R.id.text_hour_prev)).setText(clockPart(hour[0] - 1, 24));
            ((TextView) sheet.findViewById(R.id.text_hour_value)).setText(clockPart(hour[0], 24));
            ((TextView) sheet.findViewById(R.id.text_hour_next)).setText(clockPart(hour[0] + 1, 24));
            ((TextView) sheet.findViewById(R.id.text_minute_prev)).setText(clockPart(minute[0] - 1, 60));
            ((TextView) sheet.findViewById(R.id.text_minute_value)).setText(clockPart(minute[0], 60));
            ((TextView) sheet.findViewById(R.id.text_minute_next)).setText(clockPart(minute[0] + 1, 60));
        };
        paint.run();
        sheet.findViewById(R.id.text_hour_prev).setOnClickListener(v -> shiftClock(zone, takenAt, hour, minute, -1, 0, paint));
        sheet.findViewById(R.id.text_hour_next).setOnClickListener(v -> shiftClock(zone, takenAt, hour, minute, 1, 0, paint));
        sheet.findViewById(R.id.text_minute_prev).setOnClickListener(v -> shiftClock(zone, takenAt, hour, minute, 0, -1, paint));
        sheet.findViewById(R.id.text_minute_next).setOnClickListener(v -> shiftClock(zone, takenAt, hour, minute, 0, 1, paint));
        Dialog dialog = presentSheet(activity, sheet);
        sheet.findViewById(R.id.button_time_cancel).setOnClickListener(v -> {
            dialog.dismiss();
            if (reopenList != null) {
                reopenList.run();
            }
        });
        sheet.findViewById(R.id.button_time_save).setOnClickListener(v -> {
            dialog.dismiss();
            host.recordServerDose(dose.scheduledDoseId, takenAt[0], reopenList != null);
        });
        dialog.show();
        return dialog;
    }

    private static void pickServerRange(
            AppCompatActivity activity,
            LocalDate start,
            LocalDate end,
            MedScreens.Host host
    ) {
        new DatePickerDialog(activity, (view, year, month, day) -> {
            LocalDate pickedStart = LocalDate.of(year, month + 1, day);
            new DatePickerDialog(activity, (endView, endYear, endMonth, endDay) -> {
                LocalDate pickedEnd = LocalDate.of(endYear, endMonth + 1, endDay);
                if (HomeContracts.historySpanDays(pickedStart, pickedEnd) == null) {
                    host.showMessage(activity.getString(R.string.med_history_server_max));
                    return;
                }
                host.selectServerHistoryRange(pickedStart, pickedEnd);
            }, end.getYear(), end.getMonthValue() - 1, end.getDayOfMonth()).show();
        }, start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth()).show();
    }

    private static String serverStatusLabel(AppCompatActivity activity, String status) {
        if (HomeContracts.STATUS_NORMAL.equals(status)) {
            return activity.getString(R.string.med_status_completed);
        }
        if (HomeContracts.STATUS_DELAYED.equals(status)) {
            return activity.getString(R.string.med_status_delayed);
        }
        if (HomeContracts.STATUS_MISSED.equals(status)) {
            return activity.getString(R.string.med_status_missed);
        }
        return activity.getString(R.string.med_status_scheduled);
    }

    private static String sectionLabel(AppCompatActivity activity, LocalDate today, LocalDate date) {
        if (date.equals(today)) {
            return activity.getString(R.string.med_today);
        }
        if (date.equals(today.minusDays(1))) {
            return activity.getString(R.string.med_yesterday);
        }
        return SECTION.format(date);
    }
}
