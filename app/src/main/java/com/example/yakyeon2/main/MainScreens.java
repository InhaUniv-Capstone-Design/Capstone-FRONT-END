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
import com.example.yakyeon2.data.ClockTime;
import com.example.yakyeon2.data.DevFixtures;
import com.example.yakyeon2.data.ScenarioPrescriptions;
import com.example.yakyeon2.data.DoseRecord;
import com.example.yakyeon2.data.DoseRound;
import com.example.yakyeon2.data.DoseStatus;
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
import com.example.yakyeon2.ui.DrugGraphView;
import com.example.yakyeon2.ui.YakyeonListRow;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

        void refresh();

        void back();

        void showPillboxTab();

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
            String userName
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_home, null);
        paintGreeting(activity, root, userName);
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
        root.findViewById(R.id.button_home_history).setOnClickListener(v -> host.openDetail(MedScreen.HISTORY));
        com.google.android.material.card.MaterialCardView remainingCard =
                root.findViewById(R.id.card_remaining);
        remainingCard.setCardBackgroundColor(activity.getColor(R.color.white));
        remainingCard.setCardElevation(0f);
        remainingCard.setOnClickListener(v -> host.openDetail(MedScreen.SCHEDULE));
        root.findViewById(R.id.button_week).setOnClickListener(v -> host.openDetail(MedScreen.SCHEDULE));
        LinearLayout remaining = root.findViewById(R.id.remaining_rows);
        TextView remainingEmpty = root.findViewById(R.id.text_remaining_empty);
        List<DoseRecord> left = new ArrayList<>(MedicationRules.remainingToday(store.records(), now, zone));
        Collections.sort(left, Comparator.comparingLong(DoseRecord::getScheduledAtMillis));
        left = new ArrayList<>(MedicationRules.onePerEnvelopeRound(left));
        remainingEmpty.setVisibility(left.isEmpty() ? View.VISIBLE : View.GONE);
        for (int index = 0; index < left.size(); index++) {
            if (index > 0) {
                View divider = new View(activity);
                divider.setBackgroundColor(activity.getColor(R.color.yakyeon_divider));
                remaining.addView(divider, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density))
                ));
            }
            remaining.addView(remainingRow(activity, left.get(index), zone, host));
        }
        if (debuggable) {
            root.findViewById(R.id.text_home_greeting).setOnLongClickListener(v -> {
                host.openDetail(DEV_TOOLS);
                return true;
            });
        }
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
            boolean awaitingPatientLink
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_home, null);
        paintGreeting(activity, root, userName);
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
        root.findViewById(R.id.button_home_history).setOnClickListener(v -> host.openDetail(MedScreen.HISTORY));
        View.OnClickListener openSchedule = v -> host.openDetail(MedScreen.SCHEDULE);
        root.findViewById(R.id.card_remaining).setOnClickListener(openSchedule);
        root.findViewById(R.id.button_week).setOnClickListener(openSchedule);
        if (debuggable) {
            root.findViewById(R.id.text_home_greeting).setOnLongClickListener(v -> {
                host.openDetail(DEV_TOOLS);
                return true;
            });
        }
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
            paintServerSlot(root, R.id.slot_morning, R.id.icon_morning, R.drawable.ic_yakyeon_sun, null);
            paintServerSlot(root, R.id.slot_lunch, R.id.icon_lunch, R.drawable.ic_yakyeon_sun, null);
            paintServerSlot(root, R.id.slot_evening, R.id.icon_evening, R.drawable.ic_yakyeon_moon, null);
            paintServerSlot(root, R.id.slot_bedtime, R.id.icon_bedtime, R.drawable.ic_yakyeon_moon, null);
            return root;
        }
        if (summary == null || summary.kind != HomeContracts.Section.Kind.READY || summary.value == null) {
            String message = summary == null || summary.message == null
                    ? activity.getString(R.string.api_response_error)
                    : summary.message;
            progress.setText(message);
            paintServerSlot(root, R.id.slot_morning, R.id.icon_morning, R.drawable.ic_yakyeon_sun, null);
            paintServerSlot(root, R.id.slot_lunch, R.id.icon_lunch, R.drawable.ic_yakyeon_sun, null);
            paintServerSlot(root, R.id.slot_evening, R.id.icon_evening, R.drawable.ic_yakyeon_moon, null);
            paintServerSlot(root, R.id.slot_bedtime, R.id.icon_bedtime, R.drawable.ic_yakyeon_moon, null);
        } else {
            HomeContracts.HomeSummary value = summary.value;
            progress.setText(activity.getString(R.string.home_progress, value.done, value.total));
            track.setWeightSum(value.total <= 0L ? 1f : value.total);
            LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) fill.getLayoutParams();
            fillParams.weight = value.total <= 0L ? 0f : Math.min(value.done, value.total);
            fill.setLayoutParams(fillParams);
            paintServerSlot(root, R.id.slot_morning, R.id.icon_morning, R.drawable.ic_yakyeon_sun,
                    HomeContracts.slotState(value.slots, HomeContracts.SLOT_MORNING));
            paintServerSlot(root, R.id.slot_lunch, R.id.icon_lunch, R.drawable.ic_yakyeon_sun,
                    HomeContracts.slotState(value.slots, HomeContracts.SLOT_LUNCH));
            paintServerSlot(root, R.id.slot_evening, R.id.icon_evening, R.drawable.ic_yakyeon_moon,
                    HomeContracts.slotState(value.slots, HomeContracts.SLOT_DINNER));
            paintServerSlot(root, R.id.slot_bedtime, R.id.icon_bedtime, R.drawable.ic_yakyeon_moon,
                    HomeContracts.slotState(value.slots, HomeContracts.SLOT_BEDTIME));
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
        LinearLayout remaining = root.findViewById(R.id.remaining_rows);
        if (todayDoses == null || todayDoses.kind != HomeContracts.Section.Kind.READY || todayDoses.value == null) {
            remainingEmpty.setVisibility(View.VISIBLE);
            remainingEmpty.setText(todayDoses == null || todayDoses.message == null
                    ? activity.getString(R.string.api_response_error)
                    : todayDoses.message);
            return root;
        }
        java.util.List<HomeContracts.Dose> pending = HomeContracts.pendingSorted(todayDoses.value);
        if (pending.isEmpty() && summary != null && summary.kind == HomeContracts.Section.Kind.READY
                && summary.value != null && summary.value.nextDose != null) {
            remainingEmpty.setVisibility(View.GONE);
            remaining.addView(serverRemaining(activity, summary.value.nextDose, host));
            return root;
        }
        remainingEmpty.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);
        float density = activity.getResources().getDisplayMetrics().density;
        for (int index = 0; index < pending.size(); index++) {
            if (index > 0) {
                View divider = new View(activity);
                divider.setBackgroundColor(activity.getColor(R.color.yakyeon_divider));
                remaining.addView(divider, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        Math.max(1, Math.round(density))
                ));
            }
            remaining.addView(serverRemaining(activity, pending.get(index), host));
        }
        return root;
    }

    private static void paintServerSlot(View root, int slotId, int iconId, int idleIcon, String state) {
        View slot = root.findViewById(slotId);
        ImageView icon = root.findViewById(iconId);
        boolean done = HomeContracts.STATE_DONE.equals(state);
        slot.setBackgroundResource(done ? R.drawable.yakyeon_slot_done : R.drawable.yakyeon_slot_idle);
        icon.setImageResource(done ? R.drawable.ic_yakyeon_check : idleIcon);
        icon.clearColorFilter();
        slot.setAlpha(1f);
        if (HomeContracts.STATE_MISSED.equals(state)) {
            icon.setColorFilter(root.getResources().getColor(R.color.yakyeon_missed_text, null));
        } else if (state == null || HomeContracts.STATE_NONE.equals(state)) {
            slot.setAlpha(0.45f);
        }
    }

    private static View serverRemaining(AppCompatActivity activity, HomeContracts.Dose dose, Host host) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_home_remaining, null);
        TextView title = row.findViewById(R.id.text_remaining_time);
        title.setText(slotLabel(activity, dose.timingSlot) + " " + HomeContracts.clock(dose.scheduledAt));
        String place = dose.hospitalName == null || dose.hospitalName.trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : dose.hospitalName.trim();
        ((TextView) row.findViewById(R.id.text_remaining_place)).setText(place + " · " + doseNames(dose.medications));
        boolean night = HomeContracts.SLOT_DINNER.equals(dose.timingSlot) || HomeContracts.SLOT_BEDTIME.equals(dose.timingSlot);
        ((ImageView) row.findViewById(R.id.icon_round)).setImageResource(
                night ? R.drawable.ic_yakyeon_moon : R.drawable.ic_yakyeon_sun
        );
        row.setOnClickListener(v -> host.openDetail(MedScreen.SCHEDULE));
        return row;
    }

    private static View serverRemaining(AppCompatActivity activity, HomeContracts.NextDose dose, Host host) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_home_remaining, null);
        TextView title = row.findViewById(R.id.text_remaining_time);
        title.setText(slotLabel(activity, dose.timingSlot) + " " + HomeContracts.clock(dose.scheduledAt));
        String place = dose.hospitalName == null || dose.hospitalName.trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : dose.hospitalName.trim();
        StringBuilder names = new StringBuilder();
        for (HomeContracts.NamedDrug drug : dose.medications) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(drug.drugName);
        }
        ((TextView) row.findViewById(R.id.text_remaining_place)).setText(place + " · " + names);
        boolean night = HomeContracts.SLOT_DINNER.equals(dose.timingSlot) || HomeContracts.SLOT_BEDTIME.equals(dose.timingSlot);
        ((ImageView) row.findViewById(R.id.icon_round)).setImageResource(
                night ? R.drawable.ic_yakyeon_moon : R.drawable.ic_yakyeon_sun
        );
        row.setOnClickListener(v -> host.openDetail(MedScreen.SCHEDULE));
        return row;
    }

    private static String doseNames(java.util.List<HomeContracts.DoseDrug> medications) {
        StringBuilder names = new StringBuilder();
        if (medications != null) {
            for (HomeContracts.DoseDrug drug : medications) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(drug.drugName);
            }
        }
        return names.toString();
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

    private static View remainingRow(AppCompatActivity activity, DoseRecord record, ZoneId zone, Host host) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_home_remaining, null);
        ZonedDateTime time = Instant.ofEpochMilli(record.getScheduledAtMillis()).atZone(zone);
        int minutes = time.getHour() * 60 + time.getMinute();
        TextView title = row.findViewById(R.id.text_remaining_time);
        title.setText(MedScreens.roundLabel(activity, record.getRound()) + " " + ClockTime.formatHm(minutes));
        String place = record.getEnvelopeName() == null ? "" : record.getEnvelopeName().trim();
        if (place.isEmpty()) {
            place = activity.getString(R.string.med_slot_empty_name);
        }
        ((TextView) row.findViewById(R.id.text_remaining_place)).setText(place);
        boolean night = record.getRound() == DoseRound.EVENING || record.getRound() == DoseRound.BEDTIME;
        ((ImageView) row.findViewById(R.id.icon_round)).setImageResource(
                night ? R.drawable.ic_yakyeon_moon : R.drawable.ic_yakyeon_sun
        );
        row.setOnClickListener(v -> host.openDetail(MedScreen.SCHEDULE));
        return row;
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
            View row = serverSlot(activity, snapshot.compartments.get(index));
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

    private static View serverSlot(AppCompatActivity activity, CareContracts.Compartment compartment) {
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
            status.setText(R.string.pillbox_pending_load);
            status.setTextColor(activity.getColor(R.color.yakyeon_muted));
            status.setBackground(null);
            status.setPadding(0, 0, 0, 0);
            alert.setVisibility(View.GONE);
        } else if (CareContracts.showsLowStock(compartment)) {
            status.setText(R.string.pillbox_low_stock);
            status.setTextColor(activity.getColor(R.color.yakyeon_required));
            status.setBackground(null);
            status.setPadding(0, 0, 0, 0);
            alert.setVisibility(View.GONE);
        } else {
            status.setText(R.string.pillbox_status_normal);
            status.setTextColor(activity.getColor(R.color.yakyeon_brand));
            status.setBackground(null);
            status.setPadding(0, 0, 0, 0);
            alert.setVisibility(View.GONE);
        }
        status.setVisibility(View.VISIBLE);
        for (CareContracts.PillMedicine medicine : compartment.medications) {
            View line = LayoutInflater.from(activity).inflate(R.layout.item_pillbox_medicine, medicines, false);
            ((TextView) line.findViewById(R.id.text_medicine_name)).setText(medicine.drugName);
            line.findViewById(R.id.text_medicine_count).setVisibility(View.GONE);
            medicines.addView(line);
        }
        if (compartment.loadedCount != null) {
            medicines.addView(slotNote(activity, activity.getString(R.string.pillbox_loaded, compartment.loadedCount)));
        }
        if (CareContracts.showsRemaining(compartment)) {
            medicines.addView(slotNote(
                    activity,
                    activity.getString(R.string.pillbox_remaining, compartment.remainingCount)
            ));
        }
        if (Boolean.FALSE.equals(compartment.pouchPresent)) {
            medicines.addView(slotNote(activity, activity.getString(R.string.pillbox_pouch_absent)));
        }
        if (Boolean.TRUE.equals(compartment.alertActive)) {
            medicines.addView(slotNote(activity, activity.getString(R.string.pillbox_alert_on)));
        }
        if (opened && CareContracts.showsLowStock(compartment)) {
            medicines.addView(slotNote(activity, activity.getString(R.string.pillbox_low_stock)));
        }
        return row;
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

    private static void paintGreeting(AppCompatActivity activity, View root, String name) {
        TextView greeting = root.findViewById(R.id.text_home_greeting);
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
        boolean done = roundFinished(store, round, now, zone);
        View slot = root.findViewById(slotId);
        ImageView icon = root.findViewById(iconId);
        slot.setBackgroundResource(done ? R.drawable.yakyeon_slot_done : R.drawable.yakyeon_slot_idle);
        icon.setImageResource(done ? R.drawable.ic_yakyeon_check : idleIcon);
        return done;
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
