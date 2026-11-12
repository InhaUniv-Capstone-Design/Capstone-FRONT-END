package com.example.yakyeon2.main;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon2.R;
import com.example.yakyeon2.api.CareContracts;
import com.example.yakyeon2.api.HomeContracts;
import com.example.yakyeon2.data.AccountType;
import com.example.yakyeon2.data.DevFixtures;
import com.example.yakyeon2.data.ScenarioPrescriptions;
import com.example.yakyeon2.data.DoseRecord;
import com.example.yakyeon2.data.DoseRound;
import com.example.yakyeon2.data.DoseStatus;
import com.example.yakyeon2.data.HomeDoseFocus;
import com.example.yakyeon2.data.DrugRelations;
import com.example.yakyeon2.data.GraphDocument;
import com.example.yakyeon2.data.MedicationRules;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.SavedEnvelope;
import com.example.yakyeon2.data.SavedMedicine;
import com.example.yakyeon2.data.YakyeonSession;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.yakyeon2.navigation.DevSampleGate;
import com.example.yakyeon2.navigation.MedScreen;
import com.example.yakyeon2.ui.ConfirmDialog;
import com.example.yakyeon2.ui.DrugGraphView;
import com.example.yakyeon2.ui.YakyeonListRow;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MainScreens {

    public static final String PROFILE = "settings-profile";
    public static final String LINK = "settings-link";
    public static final String NOTIFICATIONS = "settings-noti";
    public static final String SHARE = "settings-share";
    public static final String LEGAL = "settings-legal";
    public static final String DEV_TOOLS = "dev-tools";

    public interface Host {
        void showMessage(CharSequence message);

        void showNotice(CharSequence title, CharSequence message);

        void openGraph();

        void openDetail(String key);

        void registerMedication();

        void refresh();

        void checkHomeDoses();

        void markUncheckedDose(long scheduledDoseId);

        void back();

        void showPillboxTab();

        void releaseCompartment(long mappingId);

        void exportClinicalShare();
    }

    public enum CareView {
        LOADING,
        READY,
        ABSENT,
        BLOCKED,
        FAILED
    }

    public interface GraphActions {
        void onNode(String medicationId);

        void onDurSeq(String durSeq);
    }

    private MainScreens() {
    }

    public static View home(
            AppCompatActivity activity,
            YakyeonSession session,
            MedicationStore store,
            Host host,
            boolean debuggable,
            String userName,
            String linkedPatientName
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_home, null);
        paintGreeting(activity, root, userName, linkedPatientName);
        int count = session.medicationIds().size();
        TextView taking = root.findViewById(R.id.text_taking_count);
        taking.setText(activity.getString(R.string.home_taking_count, count));
        ZoneId zone = ZoneId.systemDefault();
        long now = System.currentTimeMillis();
        int done = 0;
        if (paintRound(root, R.id.slot_morning, R.id.icon_morning, R.drawable.ic_yakyeon_sun, store, DoseRound.MORNING, now, zone)) {
            done++;
        }
        if (paintRound(root, R.id.slot_lunch, R.id.icon_lunch, R.drawable.ic_yakyeon_sun, store, DoseRound.LUNCH, now, zone)) {
            done++;
        }
        if (paintRound(root, R.id.slot_evening, R.id.icon_evening, R.drawable.ic_yakyeon_moon, store, DoseRound.EVENING, now, zone)) {
            done++;
        }
        if (paintRound(root, R.id.slot_bedtime, R.id.icon_bedtime, R.drawable.ic_yakyeon_moon, store, DoseRound.BEDTIME, now, zone)) {
            done++;
        }
        View fill = root.findViewById(R.id.progress_fill);
        LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) fill.getLayoutParams();
        fillParams.weight = done;
        fill.setLayoutParams(fillParams);
        TextView progress = root.findViewById(R.id.text_progress);
        progress.setText(activity.getString(R.string.home_progress, done, DoseRound.values().length));
        TextView interaction = root.findViewById(R.id.text_interaction);
        int relations = DrugRelations.confirmedCount(store);
        int relationText = store.isDevGraphRelationScenario()
                ? R.string.home_interaction_dev
                : R.string.home_interaction;
        interaction.setText(activity.getString(relationText, relations));
        root.findViewById(R.id.card_interaction).setOnClickListener(v -> host.openGraph());
        View.OnClickListener openList = v -> host.openDetail(MedScreen.LIST);
        root.findViewById(R.id.text_taking_count).setOnClickListener(openList);
        root.findViewById(R.id.button_add_med).setOnClickListener(openList);
        root.findViewById(R.id.button_register_home).setOnClickListener(v -> host.registerMedication());
        root.findViewById(R.id.button_home_history).setOnClickListener(v -> host.openDetail(MedScreen.SCHEDULE));
        paintTodayDoses(
                activity,
                root,
                localEntries(store.records(), now),
                localCanCheck(store, now, zone),
                now,
                zone,
                host
        );
        return root;
    }

    public static View serverHome(
            AppCompatActivity activity,
            CareView view,
            String blockedMessage,
            HomeContracts.Section<HomeContracts.HomeSummary> summary,
            HomeContracts.Section<java.util.List<HomeContracts.Medication>> activeMeds,
            HomeContracts.Section<CareContracts.Summary> interactions,
            HomeContracts.Section<java.util.List<HomeContracts.Dose>> todayDoses,
            Host host,
            boolean debuggable,
            String userName,
            boolean awaitingPatientLink,
            String linkedPatientName
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_home, null);
        paintGreeting(activity, root, userName, linkedPatientName);
        TextView taking = root.findViewById(R.id.text_taking_count);
        TextView progress = root.findViewById(R.id.text_progress);
        TextView interaction = root.findViewById(R.id.text_interaction);
        TextView remainingEmpty = root.findViewById(R.id.text_remaining_empty);
        LinearLayout track = root.findViewById(R.id.progress_track);
        View fill = root.findViewById(R.id.progress_fill);
        root.findViewById(R.id.card_interaction).setOnClickListener(v -> host.openGraph());
        View.OnClickListener openList = v -> host.openDetail(MedScreen.LIST);
        taking.setOnClickListener(openList);
        root.findViewById(R.id.button_add_med).setOnClickListener(openList);
        root.findViewById(R.id.button_register_home).setOnClickListener(v -> host.registerMedication());
        root.findViewById(R.id.button_home_history).setOnClickListener(v -> host.openDetail(MedScreen.SCHEDULE));
        if (awaitingPatientLink) {
            root.findViewById(R.id.text_home_subtitle).setVisibility(View.GONE);
            root.findViewById(R.id.card_today_entry).setVisibility(View.GONE);
            root.findViewById(R.id.card_interaction).setVisibility(View.GONE);
            root.findViewById(R.id.card_remaining).setVisibility(View.GONE);
            root.findViewById(R.id.panel_patient_wait).setVisibility(View.VISIBLE);
            return root;
        }
        if (view != CareView.READY) {
            String message = view == CareView.LOADING
                    ? activity.getString(R.string.home_loading)
                    : (blockedMessage == null ? activity.getString(R.string.api_response_error) : blockedMessage);
            taking.setText(message);
            progress.setText(message);
            interaction.setText(message);
            remainingEmpty.setText(message);
            remainingEmpty.setVisibility(View.VISIBLE);
            paintHomeSlots(root, null, null, null, null);
            return root;
        }
        LocalDate slotDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (summary == null || summary.kind != HomeContracts.Section.Kind.READY || summary.value == null) {
            String message = summary == null || summary.message == null
                    ? activity.getString(R.string.api_response_error)
                    : summary.message;
            progress.setText(message);
            paintHomeSlots(root, summary, todayDoses, activeMeds, slotDate);
        } else {
            HomeContracts.HomeSummary value = summary.value;
            HomeContracts.SlotProgress slotProgress = HomeContracts.slotProgress(value.slots);
            boolean settled = daySettled(summary, todayDoses);
            int shownDone = settled ? slotProgress.scheduled : slotProgress.done;
            progress.setText(activity.getString(
                    R.string.home_progress,
                    shownDone,
                    slotProgress.scheduled
            ));
            track.setWeightSum(slotProgress.scheduled <= 0 ? 1f : slotProgress.scheduled);
            LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) fill.getLayoutParams();
            fillParams.weight = slotProgress.scheduled <= 0 ? 0f : shownDone;
            fill.setLayoutParams(fillParams);
            paintHomeSlots(root, summary, todayDoses, activeMeds, slotDate);
        }
        if (activeMeds == null || activeMeds.kind != HomeContracts.Section.Kind.READY || activeMeds.value == null) {
            taking.setText(activeMeds == null || activeMeds.message == null
                    ? activity.getString(R.string.api_response_error)
                    : activeMeds.message);
        } else {
            taking.setText(activity.getString(R.string.home_taking_count, activeMeds.value.size()));
        }
        if (interactions == null || interactions.kind != HomeContracts.Section.Kind.READY || interactions.value == null) {
            interaction.setText(interactions == null || interactions.message == null
                    ? activity.getString(R.string.api_response_error)
                    : interactions.message);
        } else {
            String text = activity.getString(R.string.home_interaction, interactions.value.flagged);
            if (interactions.value.unchecked > 0L) {
                text = text + "\n" + activity.getString(
                        R.string.home_interaction_unchecked,
                        interactions.value.unchecked
                );
            }
            interaction.setText(text);
        }
        if (todayDoses == null || todayDoses.kind != HomeContracts.Section.Kind.READY || todayDoses.value == null) {
            remainingEmpty.setVisibility(View.VISIBLE);
            remainingEmpty.setText(todayDoses == null || todayDoses.message == null
                    ? activity.getString(R.string.api_response_error)
                    : todayDoses.message);
            return root;
        }
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        long doseNow = System.currentTimeMillis();
        paintTodayDoses(
                activity,
                root,
                serverEntries(todayDoses.value),
                serverCanCheck(todayDoses.value, doseNow, seoul),
                doseNow,
                seoul,
                host
        );
        return root;
    }

    private static void paintTodayDoses(
            AppCompatActivity activity,
            View root,
            List<HomeDoseFocus.Entry> doses,
            boolean canCheck,
            long now,
            ZoneId zone,
            Host host
    ) {
        View overduePanel = root.findViewById(R.id.panel_overdue);
        View divider = root.findViewById(R.id.divider_home_doses);
        View upcomingPanel = root.findViewById(R.id.panel_upcoming);
        View donePanel = root.findViewById(R.id.panel_done);
        TextView empty = root.findViewById(R.id.text_remaining_empty);
        LinearLayout upcomingRows = root.findViewById(R.id.upcoming_rows);
        LinearLayout overdueRows = root.findViewById(R.id.overdue_rows);
        View check = root.findViewById(R.id.button_home_dose_check);
        upcomingRows.removeAllViews();
        overdueRows.removeAllViews();
        overdueRows.setVisibility(View.GONE);
        check.setOnClickListener(v -> host.checkHomeDoses());
        HomeDoseFocus.Board board = HomeDoseFocus.focus(doses, now, zone);
        if (board.outcome == HomeDoseFocus.Outcome.DONE) {
            overduePanel.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            upcomingPanel.setVisibility(View.GONE);
            donePanel.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
            return;
        }
        if (board.outcome == HomeDoseFocus.Outcome.EMPTY) {
            overduePanel.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            upcomingPanel.setVisibility(View.GONE);
            donePanel.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            empty.setText(R.string.home_remaining_empty);
            return;
        }
        boolean ask = canCheck;
        boolean hasUpcoming = !board.upcoming.isEmpty();
        overduePanel.setVisibility(ask ? View.VISIBLE : View.GONE);
        divider.setVisibility(ask ? View.VISIBLE : View.GONE);
        upcomingPanel.setVisibility(View.VISIBLE);
        donePanel.setVisibility(View.GONE);
        empty.setVisibility(hasUpcoming ? View.GONE : View.VISIBLE);
        if (!hasUpcoming) {
            empty.setText(R.string.home_upcoming_none);
            return;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        int gap = Math.max(1, Math.round(12f * density));
        for (int index = 0; index < board.upcoming.size(); index++) {
            View row = todayRow(activity, board.upcoming.get(index), now, zone);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (index > 0) {
                params.topMargin = gap;
            }
            upcomingRows.addView(row, params);
        }
    }

    private static boolean localCanCheck(MedicationStore store, long now, ZoneId zone) {
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        for (DoseRecord record : MedicationRules.onDate(store.records(), today, zone)) {
            if (MedicationRules.canRevise(record, now)) {
                return true;
            }
        }
        return false;
    }

    private static boolean serverCanCheck(List<HomeContracts.Dose> doses, long now, ZoneId zone) {
        if (doses == null) {
            return false;
        }
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        Instant instant = Instant.ofEpochMilli(now);
        for (HomeContracts.Dose dose : doses) {
            if (dose == null || !today.equals(HomeContracts.seoulDate(dose.scheduledAt))) {
                continue;
            }
            if (HomeContracts.followUp(dose, instant) == HomeContracts.DoseFollowUp.ASK) {
                return true;
            }
        }
        return false;
    }

    private static List<HomeDoseFocus.Entry> localEntries(List<DoseRecord> records, long now) {
        Map<String, List<DoseRecord>> groups = new LinkedHashMap<>();
        if (records != null) {
            for (DoseRecord record : records) {
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
        }
        List<HomeDoseFocus.Entry> entries = new ArrayList<>();
        for (List<DoseRecord> group : groups.values()) {
            DoseRecord first = group.get(0);
            boolean open = false;
            boolean anyMissed = false;
            StringBuilder names = new StringBuilder();
            for (DoseRecord record : group) {
                DoseStatus status = MedicationRules.displayStatus(record, now);
                boolean recorded = status == DoseStatus.COMPLETED || status == DoseStatus.DELAYED;
                boolean missed = !recorded
                        && now > record.getScheduledAtMillis() + MedicationRules.THREE_HOURS_MS;
                if (!recorded && !missed) {
                    open = true;
                } else if (missed) {
                    anyMissed = true;
                }
                String name = record.getMedicineName();
                if (name != null && !name.trim().isEmpty()) {
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(name.trim());
                }
            }
            boolean taken = !open && !anyMissed;
            String place = first.getEnvelopeName() == null ? "" : first.getEnvelopeName().trim();
            if (names.length() > 0) {
                place = place.isEmpty() ? names.toString() : place + " · " + names;
            }
            entries.add(new HomeDoseFocus.Entry(
                    place,
                    first.getRound().name(),
                    first.getScheduledAtMillis(),
                    taken,
                    !open && anyMissed
            ));
        }
        return entries;
    }

    private static List<HomeDoseFocus.Entry> serverEntries(List<HomeContracts.Dose> doses) {
        List<HomeDoseFocus.Entry> entries = new ArrayList<>();
        if (doses == null) {
            return entries;
        }
        for (HomeContracts.Dose dose : doses) {
            Long at = HomeContracts.epochMillis(dose.scheduledAt);
            if (at == null) {
                continue;
            }
            boolean taken = HomeContracts.STATUS_NORMAL.equals(dose.status)
                    || HomeContracts.STATUS_DELAYED.equals(dose.status)
                    || (dose.takenAt != null && !dose.takenAt.isEmpty());
            boolean missed = !taken && HomeContracts.STATUS_MISSED.equals(dose.status);
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
            String place = dose.hospitalName == null ? "" : dose.hospitalName.trim();
            if (drugs.length() > 0) {
                place = place.isEmpty() ? drugs.toString() : place + " · " + drugs;
            }
            entries.add(new HomeDoseFocus.Entry(place, dose.timingSlot, at, taken, missed));
        }
        return entries;
    }

    private static View todayRow(
            AppCompatActivity activity,
            HomeDoseFocus.Entry entry,
            long now,
            ZoneId zone
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_home_upcoming, null);
        ((TextView) row.findViewById(R.id.text_place)).setText(placeName(activity, entry.place));
        ((TextView) row.findViewById(R.id.text_when)).setText(whenLabel(activity, entry, zone));
        TextView badge = row.findViewById(R.id.text_remain);
        if (entry.taken) {
            row.setBackgroundResource(R.drawable.bg_home_upcoming);
            badge.setText(R.string.med_dose_taken);
            badge.setTextColor(activity.getColor(R.color.yakyeon_history_done_text));
        } else if (entry.scheduledAtMillis > now) {
            row.setBackgroundResource(R.drawable.bg_home_upcoming);
            badge.setText(remainLabel(activity, entry.scheduledAtMillis, now));
            badge.setTextColor(activity.getColor(R.color.yakyeon_brand));
        } else {
            boolean missed = now > entry.scheduledAtMillis + MedicationRules.THREE_HOURS_MS;
            if (missed) {
                row.setBackgroundResource(R.drawable.bg_home_missed);
                badge.setBackgroundResource(R.drawable.bg_home_missed_badge);
                badge.setText(R.string.med_status_missed);
                badge.setTextColor(activity.getColor(R.color.yakyeon_missed_text));
            } else {
                row.setBackgroundResource(R.drawable.bg_home_overdue);
                badge.setBackgroundResource(R.drawable.bg_home_remain_badge);
                badge.setText(R.string.med_dose_ask);
                badge.setTextColor(activity.getColor(R.color.yakyeon_delayed_text));
            }
        }
        return row;
    }

    private static String placeName(AppCompatActivity activity, String place) {
        if (place == null || place.trim().isEmpty()) {
            return activity.getString(R.string.med_slot_empty_name);
        }
        return place.trim();
    }

    private static String whenLabel(AppCompatActivity activity, HomeDoseFocus.Entry entry, ZoneId zone) {
        ZonedDateTime time = Instant.ofEpochMilli(entry.scheduledAtMillis).atZone(zone);
        String clock = String.format("%02d:%02d", time.getHour(), time.getMinute());
        return activity.getString(R.string.home_slot_clock, focusSlotLabel(activity, entry.slot), clock);
    }

    private static String remainLabel(AppCompatActivity activity, long scheduledAtMillis, long now) {
        long minutes = Math.max(0L, (scheduledAtMillis - now) / 60000L);
        if (minutes <= 0L) {
            return activity.getString(R.string.home_remain_soon);
        }
        long hours = minutes / 60L;
        long rest = minutes % 60L;
        String amount;
        if (hours > 0L && rest > 0L) {
            amount = activity.getString(R.string.home_remain_hours, hours, rest);
        } else if (hours > 0L) {
            amount = activity.getString(R.string.home_remain_hours_only, hours);
        } else {
            amount = activity.getString(R.string.home_remain_minutes, rest);
        }
        return activity.getString(R.string.home_remain_left, amount);
    }

    private static String focusSlotLabel(AppCompatActivity activity, String slot) {
        if ("MORNING".equals(slot)) {
            return activity.getString(R.string.round_morning);
        }
        if ("LUNCH".equals(slot)) {
            return activity.getString(R.string.round_lunch);
        }
        if ("EVENING".equals(slot) || HomeContracts.SLOT_DINNER.equals(slot)) {
            return activity.getString(R.string.round_evening);
        }
        if ("BEDTIME".equals(slot)) {
            return activity.getString(R.string.round_bedtime);
        }
        return slot == null ? "" : slot;
    }

    private static void paintHomeSlots(
            View root,
            HomeContracts.Section<HomeContracts.HomeSummary> summary,
            HomeContracts.Section<java.util.List<HomeContracts.Dose>> todayDoses,
            HomeContracts.Section<java.util.List<HomeContracts.Medication>> activeMeds,
            LocalDate today
    ) {
        paintServerSlot(root, R.id.slot_morning, R.id.icon_morning, R.drawable.ic_yakyeon_sun,
                homeSlotIconState(summary, todayDoses, activeMeds, HomeContracts.SLOT_MORNING, today));
        paintServerSlot(root, R.id.slot_lunch, R.id.icon_lunch, R.drawable.ic_yakyeon_sun,
                homeSlotIconState(summary, todayDoses, activeMeds, HomeContracts.SLOT_LUNCH, today));
        paintServerSlot(root, R.id.slot_evening, R.id.icon_evening, R.drawable.ic_yakyeon_moon,
                homeSlotIconState(summary, todayDoses, activeMeds, HomeContracts.SLOT_DINNER, today));
        paintServerSlot(root, R.id.slot_bedtime, R.id.icon_bedtime, R.drawable.ic_yakyeon_moon,
                homeSlotIconState(summary, todayDoses, activeMeds, HomeContracts.SLOT_BEDTIME, today));
    }

    private static String homeSlotIconState(
            HomeContracts.Section<HomeContracts.HomeSummary> summary,
            HomeContracts.Section<java.util.List<HomeContracts.Dose>> todayDoses,
            HomeContracts.Section<java.util.List<HomeContracts.Medication>> activeMeds,
            String slot,
            LocalDate today
    ) {
        boolean summaryReady = summary != null
                && summary.kind == HomeContracts.Section.Kind.READY
                && summary.value != null;
        String summaryState = summaryReady
                ? HomeContracts.slotState(summary.value.slots, slot)
                : null;
        boolean dosesReady = todayDoses != null
                && todayDoses.kind == HomeContracts.Section.Kind.READY
                && todayDoses.value != null;
        boolean medicationsReady = activeMeds != null
                && activeMeds.kind == HomeContracts.Section.Kind.READY
                && activeMeds.value != null;
        boolean scheduled = HomeContracts.timingSlotActive(
                slot,
                summaryState,
                summaryReady,
                dosesReady ? todayDoses.value : null,
                dosesReady,
                medicationsReady ? activeMeds.value : null,
                medicationsReady,
                today
        );
        if (!scheduled) {
            return HomeContracts.STATE_NONE;
        }
        if (daySettled(summary, todayDoses) && !slotStillOpen(todayDoses, slot)) {
            return HomeContracts.STATE_DONE;
        }
        if (summaryReady && summaryState != null && !HomeContracts.STATE_NONE.equals(summaryState)) {
            return summaryState;
        }
        return HomeContracts.STATE_PENDING;
    }

    /** Every loaded dose today is taken or missed, and at least one dose exists. */
    private static boolean daySettled(
            HomeContracts.Section<HomeContracts.HomeSummary> summary,
            HomeContracts.Section<java.util.List<HomeContracts.Dose>> todayDoses
    ) {
        boolean dosesReady = todayDoses != null
                && todayDoses.kind == HomeContracts.Section.Kind.READY
                && todayDoses.value != null;
        if (dosesReady) {
            if (todayDoses.value.isEmpty()) {
                return false;
            }
            for (HomeContracts.Dose dose : todayDoses.value) {
                if (!doseSettled(dose)) {
                    return false;
                }
            }
            return true;
        }
        boolean summaryReady = summary != null
                && summary.kind == HomeContracts.Section.Kind.READY
                && summary.value != null
                && summary.value.slots != null;
        if (!summaryReady) {
            return false;
        }
        boolean any = false;
        for (HomeContracts.SlotState slot : summary.value.slots) {
            if (slot == null || HomeContracts.STATE_NONE.equals(slot.state)) {
                continue;
            }
            any = true;
            if (!HomeContracts.STATE_DONE.equals(slot.state)
                    && !HomeContracts.STATE_MISSED.equals(slot.state)) {
                return false;
            }
        }
        return any;
    }

    private static boolean doseSettled(HomeContracts.Dose dose) {
        if (dose == null) {
            return true;
        }
        if (dose.takenAt != null && !dose.takenAt.isEmpty()) {
            return true;
        }
        return HomeContracts.STATUS_NORMAL.equals(dose.status)
                || HomeContracts.STATUS_DELAYED.equals(dose.status)
                || HomeContracts.STATUS_MISSED.equals(dose.status);
    }

    private static boolean slotStillOpen(
            HomeContracts.Section<java.util.List<HomeContracts.Dose>> todayDoses,
            String slot
    ) {
        if (todayDoses == null
                || todayDoses.kind != HomeContracts.Section.Kind.READY
                || todayDoses.value == null) {
            return false;
        }
        for (HomeContracts.Dose dose : todayDoses.value) {
            if (dose != null && slot.equals(dose.timingSlot) && !doseSettled(dose)) {
                return true;
            }
        }
        return false;
    }

    private static void paintServerSlot(View root, int slotId, int iconId, int idleIcon, String state) {
        View slot = root.findViewById(slotId);
        ImageView icon = root.findViewById(iconId);
        boolean done = HomeContracts.STATE_DONE.equals(state);
        boolean missed = HomeContracts.STATE_MISSED.equals(state);
        boolean scheduled = state != null && !HomeContracts.STATE_NONE.equals(state);
        slot.setVisibility(View.VISIBLE);
        slot.setAlpha(1f);
        icon.clearColorFilter();
        if (!scheduled) {
            slot.setBackgroundResource(R.drawable.yakyeon_slot_inactive);
            icon.setImageResource(idleIcon);
            icon.setColorFilter(root.getResources().getColor(R.color.yakyeon_icon_inactive, null));
            return;
        }
        slot.setBackgroundResource(done ? R.drawable.yakyeon_slot_done : R.drawable.yakyeon_slot_idle);
        icon.setImageResource(done
                ? R.drawable.ic_yakyeon_check
                : (missed ? R.drawable.ic_settings_alert : idleIcon));
        if (missed) {
            icon.setColorFilter(root.getResources().getColor(R.color.yakyeon_missed_text, null));
        }
    }

    public static String slotLabel(AppCompatActivity activity, String slot) {
        if (HomeContracts.SLOT_MORNING.equals(slot)) {
            return activity.getString(R.string.round_morning);
        }
        if (HomeContracts.SLOT_LUNCH.equals(slot)) {
            return activity.getString(R.string.round_lunch);
        }
        if (HomeContracts.SLOT_DINNER.equals(slot)) {
            return activity.getString(R.string.round_evening);
        }
        if (HomeContracts.SLOT_BEDTIME.equals(slot)) {
            return activity.getString(R.string.round_bedtime);
        }
        return slot == null ? "" : slot;
    }

    public static View devTools(AppCompatActivity activity, MedicationStore store, Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_dev_tools, null);
        bindFixtures(activity, store, host, root);
        ZoneId zone = ZoneId.systemDefault();
        root.findViewById(R.id.button_dev_list_sample).setOnClickListener(v -> {
            if (!DevSampleGate.toggleListSample(store, true, zone)) {
                host.showMessage(activity.getString(R.string.med_dev_blocked));
            }
            host.refresh();
        });
        root.findViewById(R.id.button_dev_envelope_sample).setOnClickListener(v -> {
            if (store.draft() == null) {
                store.beginDraft();
            }
            if (!DevSampleGate.showEnvelopeSample(store, true, LocalDate.now(zone))) {
                host.showMessage(activity.getString(R.string.med_dev_envelope_blocked));
                return;
            }
            host.openDetail(MedScreen.ENVELOPE);
        });
        return root;
    }

    private static void bindFixtures(
            AppCompatActivity activity,
            MedicationStore store,
            Host host,
            View root
    ) {
        ZoneId zone = ZoneId.systemDefault();
        root.findViewById(R.id.button_dev_reset).setOnClickListener(v -> {
            DevFixtures.reset(store);
            host.refresh();
        });
        root.findViewById(R.id.button_dev_envelopes).setOnClickListener(v ->
                loadFixture(activity, store, host, DevFixtures.Scenario.MULTIPLE_ENVELOPES, zone, false));
        root.findViewById(R.id.button_dev_remaining).setOnClickListener(v ->
                loadFixture(activity, store, host, DevFixtures.Scenario.HOME_REMAINING, zone, false));
        root.findViewById(R.id.button_dev_statuses).setOnClickListener(v ->
                loadFixture(activity, store, host, DevFixtures.Scenario.SCHEDULE_STATUSES, zone, false));
        root.findViewById(R.id.button_dev_alarm).setOnClickListener(v ->
                loadFixture(activity, store, host, DevFixtures.Scenario.ALARM_DRAFT, zone, true));
        root.findViewById(R.id.button_dev_slots_some).setOnClickListener(v ->
                loadFixture(activity, store, host, DevFixtures.Scenario.SLOTS_PARTIAL, zone, false));
        root.findViewById(R.id.button_dev_slots_full).setOnClickListener(v ->
                loadFixture(activity, store, host, DevFixtures.Scenario.SLOTS_FULL, zone, false));
        TextView graphScenario = root.findViewById(R.id.button_dev_graph);
        graphScenario.setText(store.isDevGraphRelationScenario()
                ? R.string.home_dev_graph_off
                : R.string.home_dev_graph_on);
        graphScenario.setOnClickListener(v -> {
            store.setDevGraphRelationScenario(!store.isDevGraphRelationScenario());
            host.openGraph();
        });
        root.findViewById(R.id.button_dev_graph_clear).setOnClickListener(v ->
                installGraphChecks(activity, store, host, zone, 0));
        root.findViewById(R.id.button_dev_graph_one).setOnClickListener(v ->
                installGraphChecks(activity, store, host, zone, 1));
        root.findViewById(R.id.button_dev_graph_three).setOnClickListener(v ->
                installGraphChecks(activity, store, host, zone, 3));
        root.findViewById(R.id.button_dev_scenario_known).setOnClickListener(v ->
                installScenario(activity, store, host, false));
        root.findViewById(R.id.button_dev_scenario_skin).setOnClickListener(v ->
                installScenario(activity, store, host, true));
        root.findViewById(R.id.button_dev_scenario_clear).setOnClickListener(v -> {
            int removed = ScenarioPrescriptions.removeScenario(store);
            host.refresh();
            host.showMessage(activity.getString(
                    removed == 0 ? R.string.home_dev_scenario_kept : R.string.home_dev_scenario_cleared,
                    removed
            ));
        });
        root.findViewById(R.id.button_dev_care_link).setOnClickListener(v ->
                host.openDetail(LINK));
        root.findViewById(R.id.button_dev_hardware_early).setOnClickListener(v -> {
            long nowMillis = System.currentTimeMillis();
            int marked = 0;
            for (int slot = 0; slot < store.sessionSlotCount(); slot++) {
                marked = store.recordEarlyHardwareIntake(slot, nowMillis);
                if (marked > 0) {
                    break;
                }
            }
            if (marked == 0) {
                host.showMessage(activity.getString(R.string.home_dev_hardware_early_none));
                return;
            }
            host.showMessage(activity.getString(R.string.home_dev_hardware_early_done));
            host.openDetail(MedScreen.SCHEDULE);
        });
    }

    private static void installScenario(
            AppCompatActivity activity,
            MedicationStore store,
            Host host,
            boolean dermatology
    ) {
        try {
            boolean added = dermatology
                    ? ScenarioPrescriptions.installDermatology(store)
                    : ScenarioPrescriptions.installKnown(store);
            host.refresh();
            host.showMessage(activity.getString(added
                    ? R.string.home_dev_scenario_added
                    : R.string.home_dev_scenario_kept));
        } catch (IllegalStateException ex) {
            host.showMessage(activity.getString(R.string.home_dev_blocked));
            host.refresh();
        }
    }

    private static void installGraphChecks(
            AppCompatActivity activity,
            MedicationStore store,
            Host host,
            ZoneId zone,
            int count
    ) {
        if (!store.installGraphChecks(count, zone, System.currentTimeMillis())) {
            host.showMessage(activity.getString(R.string.home_dev_blocked));
            host.refresh();
            return;
        }
        host.openGraph();
    }

    private static void loadFixture(
            AppCompatActivity activity,
            MedicationStore store,
            Host host,
            DevFixtures.Scenario scenario,
            ZoneId zone,
            boolean openEnvelope
    ) {
        if (!DevFixtures.apply(store, scenario, zone, System.currentTimeMillis())) {
            host.showMessage(activity.getString(R.string.home_dev_blocked));
            host.refresh();
            return;
        }
        if (openEnvelope) {
            host.openDetail(MedScreen.ENVELOPE);
            return;
        }
        host.refresh();
    }

    public static View graph(AppCompatActivity activity, MedicationStore store) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_graph, null);
        TextView empty = root.findViewById(R.id.text_graph_empty);
        View filled = root.findViewById(R.id.graph_filled);
        List<DrugRelations.Node> nodes = DrugRelations.takingNodes(store);
        TextView sampleNote = root.findViewById(R.id.text_graph_sample_note);
        if (nodes.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(activity.getString(R.string.graph_empty));
            boolean samplesOnly = store != null && (store.hasFixture() || store.hasDevListSample());
            sampleNote.setVisibility(samplesOnly ? View.VISIBLE : View.GONE);
            filled.setVisibility(View.GONE);
            return root;
        }
        sampleNote.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        filled.setVisibility(View.VISIBLE);
        List<DrugRelations.Pair> relations = DrugRelations.confirmedPairs(store);
        TextView devBanner = root.findViewById(R.id.text_graph_dev);
        devBanner.setVisibility(store.isDevGraphRelationScenario() ? View.VISIBLE : View.GONE);
        TextView count = root.findViewById(R.id.text_graph_count);
        int countText = store.isDevGraphRelationScenario()
                ? R.string.graph_relation_count_dev
                : R.string.graph_relation_count;
        count.setText(activity.getString(countText, relations.size()));
        FrameLayout canvas = root.findViewById(R.id.graph_canvas);
        DrugGraphView graphView = new DrugGraphView(activity);
        graphView.setListener(new DrugGraphView.Listener() {
            @Override
            public void onNode(String medicineId) {
                showNode(activity, GraphDocument.findNode(nodes, medicineId));
            }

            @Override
            public void onEdge(String edgeId) {
                showInteractionDetail(activity, GraphDocument.findPair(relations, edgeId));
            }
        });
        canvas.addView(graphView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        graphView.show(GraphDocument.json(nodes, relations));
        LinearLayout pairs = root.findViewById(R.id.graph_pairs);
        pairs.removeAllViews();
        for (DrugRelations.Pair pair : relations) {
            pairs.addView(pairRow(activity, pair));
        }
        return root;
    }

    private static View pairRow(AppCompatActivity activity, DrugRelations.Pair pair) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.yakyeon_graph_pair);
        int padH = dp(activity, 10);
        int padV = dp(activity, 12);
        row.setPadding(padH, padV, padH, padV);
        row.setOnClickListener(v -> showInteractionDetail(activity, pair));

        LinearLayout names = new LinearLayout(activity);
        names.setOrientation(LinearLayout.HORIZONTAL);
        names.setGravity(Gravity.CENTER_VERTICAL);
        names.addView(pairName(activity, pair.getLeftName(), R.color.yakyeon_brand));
        TextView plus = pairName(activity, "+", R.color.yakyeon_deep);
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        plusParams.setMarginStart(dp(activity, 5));
        plusParams.setMarginEnd(dp(activity, 5));
        names.addView(plus, plusParams);
        names.addView(pairName(activity, pair.getRightName(), R.color.yakyeon_brand));

        TextView badge = new TextView(activity);
        badge.setText(pair.isDevScenario() ? R.string.graph_pair_label_dev : R.string.graph_pair_label);
        badge.setTextColor(activity.getColor(R.color.white));
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setBackgroundResource(R.drawable.yakyeon_graph_badge);
        int badgeH = dp(activity, 10);
        int badgeV = dp(activity, 4);
        badge.setPadding(badgeH, badgeV, badgeH, badgeV);

        row.addView(names, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(badge);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(activity, 12);
        row.setLayoutParams(params);
        return row;
    }

    private static TextView pairName(AppCompatActivity activity, String text, int color) {
        TextView name = new TextView(activity);
        name.setText(text);
        name.setTextColor(activity.getColor(color));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        return name;
    }

    private static void showNode(AppCompatActivity activity, DrugRelations.Node node) {
        if (node == null) {
            return;
        }
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setTitle(node.getName())
                .setMessage(activity.getString(R.string.graph_node_selected))
                .setPositiveButton(activity.getString(R.string.action_confirm), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static void showInteractionDetail(AppCompatActivity activity, DrugRelations.Pair pair) {
        if (pair == null) {
            return;
        }
        String names = pair.getLeftName() + " + " + pair.getRightName();
        String message = pair.isDevScenario()
                ? activity.getString(R.string.graph_dev_banner) + "\n" + names
                : names;
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setTitle(activity.getString(R.string.graph_detail_title))
                .setMessage(message)
                .setPositiveButton(activity.getString(R.string.action_confirm), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static int dp(AppCompatActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    public static View pillbox(AppCompatActivity activity, MedicationStore store, Host host) {
        if (!store.pillboxConnected()) {
            return pillboxGate(activity, host);
        }
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_pillbox, null);
        TextView status = root.findViewById(R.id.text_pillbox_status);
        ImageView wifi = root.findViewById(R.id.icon_pillbox_wifi);
        wifi.setVisibility(View.VISIBLE);
        status.setText(R.string.pillbox_connected);
        status.setTextColor(activity.getColor(R.color.yakyeon_badge));
        TextView headline = root.findViewById(R.id.text_pillbox_headline);
        headline.setText(activity.getString(R.string.pillbox_slots_used, occupiedSlots(store)));
        LinearLayout slots = root.findViewById(R.id.pillbox_slots);
        for (int index = 0; index < store.sessionSlotCount(); index++) {
            SavedEnvelope envelope = envelopeInSlot(store, index);
            // Open lid is the only anomaly. Detection is not connected, so slots stay normal.
            View row = slotRow(activity, index, envelope, false);
            int slotIndex = index;
            row.setOnClickListener(v -> {
                if (!store.pillboxConnected()) {
                    host.showMessage(activity.getString(R.string.pillbox_slot_blocked));
                } else if (envelopeInSlot(store, slotIndex) != null) {
                    host.showMessage(activity.getString(R.string.pillbox_slot_full));
                }
            });
            if (index > 0) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = dp(activity, 10);
                slots.addView(row, params);
            } else {
                slots.addView(row);
            }
        }
        root.findViewById(R.id.button_pillbox_settings).setOnClickListener(v ->
                host.openDetail(SettingsScreens.PILLBOX)
        );
        return root;
    }

    public static View serverGraph(
            AppCompatActivity activity,
            CareView view,
            CareContracts.GraphBundle bundle,
            String message,
            GraphActions actions
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_graph, null);
        TextView empty = root.findViewById(R.id.text_graph_empty);
        View filled = root.findViewById(R.id.graph_filled);
        TextView sampleNote = root.findViewById(R.id.text_graph_sample_note);
        if (view != CareView.READY || bundle == null || bundle.summary == null || bundle.graph == null) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(message == null || message.isEmpty()
                    ? activity.getString(view == CareView.LOADING
                    ? R.string.graph_loading
                    : R.string.api_request_failed)
                    : message);
            sampleNote.setVisibility(View.GONE);
            filled.setVisibility(View.GONE);
            return root;
        }
        String notes = graphNotes(activity, bundle);
        if (bundle.graph.nodes.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(bundle.summary.total == 0L
                    ? activity.getString(R.string.graph_empty)
                    : activity.getString(R.string.api_response_error));
            sampleNote.setVisibility(View.VISIBLE);
            sampleNote.setText(notes);
            filled.setVisibility(View.GONE);
            return root;
        }
        sampleNote.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        filled.setVisibility(View.VISIBLE);
        root.findViewById(R.id.text_graph_dev).setVisibility(View.GONE);
        TextView source = root.findViewById(R.id.text_graph_no_source);
        source.setText(notes);
        TextView count = root.findViewById(R.id.text_graph_count);
        count.setText(activity.getString(R.string.graph_relation_count, bundle.interactions.size()));
        FrameLayout canvas = root.findViewById(R.id.graph_canvas);
        DrugGraphView graphView = new DrugGraphView(activity);
        graphView.setListener(new DrugGraphView.Listener() {
            @Override
            public void onNode(String medicineId) {
                actions.onNode(medicineId);
            }

            @Override
            public void onEdge(String edgeId) {
                CareContracts.GraphEdge edge = CareContracts.findEdge(bundle.graph.edges, edgeId);
                if (edge != null) {
                    actions.onDurSeq(edge.durSeq);
                }
            }
        });
        canvas.addView(graphView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        graphView.show(CareContracts.canvasJson(bundle.graph));
        LinearLayout pairs = root.findViewById(R.id.graph_pairs);
        pairs.removeAllViews();
        for (CareContracts.InteractionRow row : bundle.interactions) {
            pairs.addView(serverPairRow(activity, row, actions));
        }
        return root;
    }

    private static String graphNotes(AppCompatActivity activity, CareContracts.GraphBundle bundle) {
        StringBuilder notes = new StringBuilder();
        notes.append(activity.getString(
                R.string.graph_summary,
                bundle.summary.total,
                bundle.summary.flagged,
                bundle.summary.unchecked
        ));
        if (CareContracts.showsUncheckedNotice(bundle.summary)) {
            notes.append('\n').append(activity.getString(R.string.graph_unchecked_notice));
        }
        if (CareContracts.needsSafetyDisclaimer(bundle.summary)) {
            notes.append('\n').append(activity.getString(R.string.graph_not_safety));
        }
        StringBuilder unchecked = new StringBuilder();
        for (CareContracts.GraphNode node : bundle.graph.nodes) {
            if (node.checked) {
                continue;
            }
            if (unchecked.length() > 0) {
                unchecked.append(", ");
            }
            unchecked.append(node.drugName);
        }
        if (unchecked.length() > 0) {
            notes.append('\n').append(activity.getString(R.string.graph_unchecked_list, unchecked.toString()));
        }
        return notes.toString();
    }

    private static View serverPairRow(
            AppCompatActivity activity,
            CareContracts.InteractionRow row,
            GraphActions actions
    ) {
        LinearLayout line = new LinearLayout(activity);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setBackgroundResource(R.drawable.yakyeon_graph_pair);
        int padH = dp(activity, 10);
        int padV = dp(activity, 12);
        line.setPadding(padH, padV, padH, padV);
        line.setOnClickListener(v -> actions.onDurSeq(row.durSeq));
        LinearLayout names = new LinearLayout(activity);
        names.setOrientation(LinearLayout.HORIZONTAL);
        names.setGravity(Gravity.CENTER_VERTICAL);
        names.addView(pairName(activity, row.medicationAName, R.color.yakyeon_brand));
        TextView plus = pairName(activity, "+", R.color.yakyeon_deep);
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        plusParams.setMarginStart(dp(activity, 5));
        plusParams.setMarginEnd(dp(activity, 5));
        names.addView(plus, plusParams);
        names.addView(pairName(activity, row.medicationBName, R.color.yakyeon_brand));
        TextView badge = new TextView(activity);
        badge.setText(R.string.graph_pair_label);
        badge.setTextColor(activity.getColor(R.color.white));
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setBackgroundResource(R.drawable.yakyeon_graph_badge);
        int badgeH = dp(activity, 10);
        int badgeV = dp(activity, 4);
        badge.setPadding(badgeH, badgeV, badgeH, badgeV);
        line.addView(names, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(badge);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(activity, 12);
        line.setLayoutParams(params);
        return line;
    }

    public static View serverPillbox(
            AppCompatActivity activity,
            CareView view,
            CareContracts.Pillbox snapshot,
            String message,
            boolean writesAllowed,
            Host host
    ) {
        if (view == CareView.ABSENT || view == CareView.LOADING) {
            return pillboxGate(activity, host);
        }
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_pillbox, null);
        TextView status = root.findViewById(R.id.text_pillbox_status);
        ImageView wifi = root.findViewById(R.id.icon_pillbox_wifi);
        TextView headline = root.findViewById(R.id.text_pillbox_headline);
        LinearLayout slots = root.findViewById(R.id.pillbox_slots);
        View settings = root.findViewById(R.id.button_pillbox_settings);
        if (view != CareView.READY || snapshot == null || snapshot.device == null) {
            wifi.setVisibility(View.GONE);
            status.setText(message == null || message.isEmpty()
                    ? activity.getString(view == CareView.LOADING
                    ? R.string.pillbox_loading
                    : R.string.api_request_failed)
                    : message);
            status.setTextColor(activity.getColor(R.color.yakyeon_muted));
            if (message == null || message.isEmpty()) {
                return pillboxGate(activity, host);
            }
            headline.setText(message);
            settings.setVisibility(view == CareView.BLOCKED ? View.GONE : View.VISIBLE);
            settings.setOnClickListener(v -> host.openDetail(SettingsScreens.PILLBOX));
            return root;
        }
        if (snapshot.device.online) {
            wifi.setVisibility(View.VISIBLE);
            status.setText(R.string.pillbox_connected);
            status.setTextColor(activity.getColor(R.color.yakyeon_badge));
        } else {
            wifi.setVisibility(View.GONE);
            status.setText(R.string.pillbox_offline);
            status.setTextColor(activity.getColor(R.color.yakyeon_muted));
        }
        headline.setText(activity.getString(
                R.string.pillbox_slots_used,
                CareContracts.occupiedCompartments(snapshot)
        ));
        for (int index = 0; index < snapshot.compartments.size(); index++) {
            View row = serverSlot(
                    activity,
                    snapshot.compartments.get(index),
                    writesAllowed,
                    host
            );
            if (index > 0) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = dp(activity, 10);
                slots.addView(row, params);
            } else {
                slots.addView(row);
            }
        }
        if (!writesAllowed) {
            TextView readOnly = new TextView(activity);
            readOnly.setText(R.string.pillbox_read_only);
            readOnly.setTextColor(activity.getColor(R.color.yakyeon_muted));
            readOnly.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            noteParams.topMargin = dp(activity, 12);
            slots.addView(readOnly, noteParams);
        }
        settings.setOnClickListener(v -> host.openDetail(SettingsScreens.PILLBOX));
        return root;
    }

    private static View serverSlot(
            AppCompatActivity activity,
            CareContracts.Compartment compartment,
            boolean writesAllowed,
            Host host
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_pillbox_slot, null);
        ((TextView) row.findViewById(R.id.text_slot_number)).setText(String.valueOf(compartment.compartmentNo));
        TextView title = row.findViewById(R.id.text_slot_title);
        TextView status = row.findViewById(R.id.text_slot_status);
        View alert = row.findViewById(R.id.slot_open_alert);
        LinearLayout medicines = row.findViewById(R.id.slot_medicines);
        if (CareContracts.EMPTY.equals(compartment.state)) {
            title.setText(R.string.pillbox_empty_label);
            status.setVisibility(View.GONE);
            alert.setVisibility(View.GONE);
            medicines.setVisibility(View.GONE);
            return row;
        }
        String hospital = compartment.hospitalName == null ? "" : compartment.hospitalName.trim();
        title.setText(hospital.isEmpty() ? activity.getString(R.string.pillbox_in_use) : hospital);
        boolean opened = CareContracts.showsOpened(compartment);
        if (opened) {
            row.setBackgroundResource(R.drawable.bg_pillbox_slot_open);
            status.setText(R.string.pillbox_status_check);
            status.setTextColor(activity.getColor(R.color.yakyeon_required));
            status.setBackgroundResource(R.drawable.bg_slot_check);
            int horizontal = dp(activity, 10);
            int vertical = dp(activity, 4);
            status.setPadding(horizontal, vertical, horizontal, vertical);
            alert.setVisibility(View.VISIBLE);
        } else if (CareContracts.PENDING_LOAD.equals(compartment.state)) {
            status.setText(R.string.pillbox_status_pending_load);
            status.setTextColor(activity.getColor(R.color.yakyeon_muted));
            status.setBackground(null);
            status.setPadding(0, 0, 0, 0);
            status.setVisibility(View.VISIBLE);
            alert.setVisibility(View.GONE);
        } else {
            status.setText(R.string.pillbox_status_normal);
            status.setTextColor(activity.getColor(R.color.yakyeon_brand));
            status.setBackground(null);
            status.setPadding(0, 0, 0, 0);
            status.setVisibility(View.VISIBLE);
            alert.setVisibility(View.GONE);
        }
        if (opened) {
            status.setVisibility(View.VISIBLE);
        }
        for (CareContracts.PillMedicine medicine : compartment.medications) {
            View line = LayoutInflater.from(activity).inflate(R.layout.item_pillbox_medicine, medicines, false);
            ((TextView) line.findViewById(R.id.text_medicine_name)).setText(medicine.drugName);
            line.findViewById(R.id.text_medicine_count).setVisibility(View.GONE);
            medicines.addView(line);
        }
        if (Boolean.FALSE.equals(compartment.pouchPresent)) {
            medicines.addView(slotNote(activity, activity.getString(R.string.pillbox_pouch_absent)));
        }
        if (Boolean.TRUE.equals(compartment.alertActive)) {
            medicines.addView(slotNote(activity, activity.getString(R.string.pillbox_alert_on)));
        }
        row.setOnClickListener(v -> {
            if (!writesAllowed) {
                host.showMessage(activity.getString(R.string.pillbox_read_only));
                return;
            }
            if (!CareContracts.canRelease(compartment) || compartment.mappingId == null) {
                host.showMessage(activity.getString(R.string.api_response_error));
                return;
            }
            confirmRelease(activity, compartment.mappingId, host);
        });
        return row;
    }

    private static void confirmRelease(AppCompatActivity activity, long mappingId, Host host) {
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.pillbox_release_title),
                activity.getString(R.string.pillbox_release_message),
                activity.getString(R.string.pillbox_release_action),
                activity.getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        host.releaseCompartment(mappingId);
                    }

                    @Override
                    public void onCancel() {
                    }
                },
                ConfirmDialog.Mark.TRASH,
                ConfirmDialog.Tone.DANGER
        );
    }

    private static TextView slotNote(AppCompatActivity activity, String text) {
        TextView note = new TextView(activity);
        note.setText(text);
        note.setTextColor(activity.getColor(R.color.yakyeon_muted));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setPadding(0, dp(activity, 4), 0, 0);
        return note;
    }

    private static View pillboxGate(AppCompatActivity activity, Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_pillbox_gate, null);
        root.findViewById(R.id.button_pillbox_register).setOnClickListener(v ->
                host.openDetail(SettingsScreens.PILLBOX_SEARCH)
        );
        return root;
    }

    private static View slotRow(
            AppCompatActivity activity,
            int index,
            SavedEnvelope envelope,
            boolean opened
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_pillbox_slot, null);
        ((TextView) row.findViewById(R.id.text_slot_number)).setText(String.valueOf(index + 1));
        TextView title = row.findViewById(R.id.text_slot_title);
        TextView status = row.findViewById(R.id.text_slot_status);
        View alert = row.findViewById(R.id.slot_open_alert);
        LinearLayout medicines = row.findViewById(R.id.slot_medicines);
        if (envelope == null) {
            title.setText(R.string.pillbox_empty_label);
            status.setVisibility(View.GONE);
            alert.setVisibility(View.GONE);
            medicines.setVisibility(View.GONE);
            return row;
        }
        title.setText(prescriptionTitle(activity, envelope));
        if (opened) {
            row.setBackgroundResource(R.drawable.bg_pillbox_slot_open);
            status.setText(R.string.pillbox_status_check);
            status.setTextColor(activity.getColor(R.color.yakyeon_required));
            status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            status.setBackgroundResource(R.drawable.bg_slot_check);
            int horizontal = dp(activity, 10);
            int vertical = dp(activity, 4);
            status.setPadding(horizontal, vertical, horizontal, vertical);
            alert.setVisibility(View.VISIBLE);
        } else {
            status.setText(R.string.pillbox_status_normal);
            status.setTextColor(activity.getColor(R.color.yakyeon_brand));
            status.setBackground(null);
            status.setPadding(0, 0, 0, 0);
            alert.setVisibility(View.GONE);
        }
        status.setVisibility(View.VISIBLE);
        for (SavedMedicine medicine : envelope.medicines()) {
            View line = LayoutInflater.from(activity).inflate(R.layout.item_pillbox_medicine, medicines, false);
            ((TextView) line.findViewById(R.id.text_medicine_name)).setText(medicine.getName());
            TextView count = line.findViewById(R.id.text_medicine_count);
            int rounds = medicine.rounds().size();
            if (rounds == 0) {
                count.setVisibility(View.GONE);
            } else {
                count.setText(activity.getString(R.string.pillbox_daily_count, rounds));
            }
            medicines.addView(line);
        }
        return row;
    }

    private static String prescriptionTitle(AppCompatActivity activity, SavedEnvelope envelope) {
        String hospital = envelope.getHospitalName();
        if (hospital == null || hospital.trim().isEmpty()) {
            return activity.getString(R.string.pillbox_in_use);
        }
        String name = hospital.trim();
        if (name.endsWith("처방")) {
            return name;
        }
        return activity.getString(R.string.pillbox_prescription, name);
    }

    private static int occupiedSlots(MedicationStore store) {
        int count = 0;
        for (int index = 0; index < store.sessionSlotCount(); index++) {
            if (store.sessionSlotOccupied(index)) {
                count++;
            }
        }
        return count;
    }

    private static SavedEnvelope envelopeInSlot(MedicationStore store, int index) {
        for (SavedEnvelope envelope : store.envelopes()) {
            if (!envelope.isStopped()
                    && envelope.getSlotIndex() != null
                    && envelope.getSlotIndex() == index) {
                return envelope;
            }
        }
        return null;
    }

    public static View careLink(
            AppCompatActivity activity,
            SettingsScreens.AccountHost account,
            Host host
    ) {
        return SettingsScreens.careLink(activity, account, host);
    }

    public static View settings(AppCompatActivity activity, AccountType accountType, Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_settings, null);
        LinearLayout list = root.findViewById(R.id.settings_list);
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.yakyeon_med_card);
        int inset = activity.getResources().getDimensionPixelSize(R.dimen.space_m);
        card.setPadding(inset, inset / 2, inset, inset / 2);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int side = activity.getResources().getDimensionPixelSize(R.dimen.space_m) + inset / 8;
        cardParams.setMargins(side, inset / 2, side, inset);
        card.setLayoutParams(cardParams);
        menuRow(activity, card, R.drawable.ic_settings_person, R.string.settings_profile, 0,
                () -> host.openDetail(PROFILE));
        if (accountType != AccountType.SELF) {
            menuRow(activity, card, R.drawable.ic_settings_link, R.string.settings_link, 0,
                    () -> host.openDetail(LINK));
        }
        menuRow(activity, card, R.drawable.ic_settings_bell, R.string.settings_noti, 0,
                () -> host.openDetail(NOTIFICATIONS));
        menuRow(activity, card, R.drawable.ic_settings_share, R.string.settings_share, 0,
                () -> host.openDetail(SHARE));
        menuRow(activity, card, R.drawable.ic_settings_doc, R.string.settings_legal,
                R.string.settings_legal_menu_sub, () -> host.openDetail(LEGAL));
        if (card.getChildCount() > 0) {
            card.removeViewAt(card.getChildCount() - 1);
        }
        list.addView(card);
        return root;
    }

    public static View pending(AppCompatActivity activity, String key) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_pending, null);
        TextView title = root.findViewById(R.id.text_pending_title);
        TextView message = root.findViewById(R.id.text_pending_message);
        title.setText(titleFor(activity, key));
        message.setText(activity.getString(R.string.main_pending_message));
        return root;
    }

    public static String titleFor(AppCompatActivity activity, String key) {
        if (PROFILE.equals(key)) {
            return activity.getString(R.string.settings_profile_heading);
        }
        if (LINK.equals(key)) {
            return activity.getString(R.string.settings_link);
        }
        if (NOTIFICATIONS.equals(key)) {
            return activity.getString(R.string.settings_noti);
        }
        if (SHARE.equals(key)) {
            return activity.getString(R.string.settings_share);
        }
        if (LEGAL.equals(key)) {
            return activity.getString(R.string.settings_legal);
        }
        if (DEV_TOOLS.equals(key)) {
            return activity.getString(R.string.dev_tools_title);
        }
        return key == null ? "" : key;
    }

    private static void paintGreeting(
            AppCompatActivity activity,
            View root,
            String name,
            String linkedPatientName
    ) {
        TextView greeting = root.findViewById(R.id.text_home_greeting);
        TextView subtitle = root.findViewById(R.id.text_home_subtitle);
        String patient = linkedPatientName == null ? "" : linkedPatientName.trim();
        if (!patient.isEmpty()) {
            if (name == null || name.trim().isEmpty()) {
                greeting.setText(R.string.home_greeting);
            } else {
                greeting.setText(activity.getString(R.string.home_greeting_guardian, name.trim()));
            }
            subtitle.setText(activity.getString(R.string.home_subtitle_patient, patient));
            return;
        }
        subtitle.setText(R.string.home_subtitle);
        if (name == null || name.trim().isEmpty()) {
            greeting.setText(R.string.home_greeting);
            return;
        }
        greeting.setText(activity.getString(R.string.home_greeting_named, name.trim()));
    }

    private static boolean paintRound(
            View root,
            int slotId,
            int iconId,
            int idleIcon,
            MedicationStore store,
            DoseRound round,
            long now,
            ZoneId zone
    ) {
        boolean scheduled = roundScheduled(store, round, now, zone);
        boolean done = scheduled && (roundFinished(store, round, now, zone) || todaySettled(store, now, zone));
        View slot = root.findViewById(slotId);
        ImageView icon = root.findViewById(iconId);
        slot.setVisibility(View.VISIBLE);
        slot.setAlpha(1f);
        icon.clearColorFilter();
        if (!scheduled) {
            slot.setBackgroundResource(R.drawable.yakyeon_slot_inactive);
            icon.setImageResource(idleIcon);
            icon.setColorFilter(root.getResources().getColor(R.color.yakyeon_icon_inactive, null));
            return false;
        }
        slot.setBackgroundResource(done ? R.drawable.yakyeon_slot_done : R.drawable.yakyeon_slot_idle);
        icon.setImageResource(done ? R.drawable.ic_yakyeon_check : idleIcon);
        return done;
    }

    /** Today's doses are all taken or past the miss window, and at least one exists. */
    private static boolean todaySettled(MedicationStore store, long now, ZoneId zone) {
        boolean any = false;
        for (DoseRecord record : MedicationRules.onDate(store.records(), LocalDate.now(zone), zone)) {
            any = true;
            DoseStatus status = MedicationRules.displayStatus(record, now);
            if (status == DoseStatus.COMPLETED || status == DoseStatus.DELAYED) {
                continue;
            }
            if (now <= record.getScheduledAtMillis() + MedicationRules.THREE_HOURS_MS) {
                return false;
            }
        }
        return any;
    }

    private static boolean roundScheduled(MedicationStore store, DoseRound round, long now, ZoneId zone) {
        for (DoseRecord record : MedicationRules.onDate(store.records(), LocalDate.now(zone), zone)) {
            if (record.getRound() == round) {
                return true;
            }
        }
        return false;
    }

    private static boolean roundFinished(MedicationStore store, DoseRound round, long now, ZoneId zone) {
        boolean any = false;
        for (DoseRecord record : MedicationRules.onDate(store.records(), LocalDate.now(zone), zone)) {
            if (record.getRound() != round) {
                continue;
            }
            any = true;
            DoseStatus status = MedicationRules.displayStatus(record, now);
            if (status != DoseStatus.COMPLETED && status != DoseStatus.DELAYED) {
                return false;
            }
        }
        return any;
    }

    private static void menuRow(
            AppCompatActivity activity,
            LinearLayout card,
            int icon,
            int title,
            int subtitle,
            Runnable action
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.space_s);
        row.setPadding(0, pad, 0, pad);
        row.setMinimumHeight(activity.getResources().getDimensionPixelSize(R.dimen.min_touch_target) + pad);
        ImageView mark = new ImageView(activity);
        int tile = activity.getResources().getDimensionPixelSize(R.dimen.min_touch_target) - pad;
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(tile, tile);
        mark.setLayoutParams(tileParams);
        mark.setBackgroundResource(R.drawable.bg_settings_icon);
        mark.setImageResource(icon);
        int iconPad = pad / 2;
        mark.setPadding(iconPad, iconPad, iconPad, iconPad);
        mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        row.addView(mark);
        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart(pad);
        text.setLayoutParams(textParams);
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextColor(activity.getColor(R.color.yakyeon_text));
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        text.addView(label);
        if (subtitle != 0) {
            TextView detail = new TextView(activity);
            detail.setText(subtitle);
            detail.setTextColor(activity.getColor(R.color.yakyeon_muted));
            detail.setTextSize(12);
            text.addView(detail);
        }
        row.addView(text);
        ImageView chevron = new ImageView(activity);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        row.addView(chevron);
        row.setOnClickListener(v -> action.run());
        card.addView(row);
        View line = new View(activity);
        line.setBackgroundColor(activity.getColor(R.color.yakyeon_divider));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        );
        line.setLayoutParams(lineParams);
        card.addView(line);
    }

    private static void addRow(AppCompatActivity activity, LinearLayout list, int title, Runnable action) {
        YakyeonListRow row = new YakyeonListRow(activity);
        row.setTitle(activity.getString(title));
        row.setMinimumHeight(activity.getResources().getDimensionPixelSize(R.dimen.min_touch_target));
        row.setOnClickListener(v -> action.run());
        list.addView(row);
    }
}
