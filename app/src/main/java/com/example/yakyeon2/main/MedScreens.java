package com.example.yakyeon2.main;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon2.R;
import com.example.yakyeon2.api.HomeContracts;
import com.example.yakyeon2.api.HomeRegistration;
import com.example.yakyeon2.data.ClockTime;
import com.example.yakyeon2.data.DoseAmount;
import com.example.yakyeon2.data.DoseRound;
import com.example.yakyeon2.data.DoseTiming;
import com.example.yakyeon2.data.EnvelopeDraft;
import com.example.yakyeon2.data.MedicationRules;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.MedicineDraft;
import com.example.yakyeon2.data.SavedEnvelope;
import com.example.yakyeon2.data.SavedMedicine;
import com.example.yakyeon2.data.MedicineNameCatalog;
import com.example.yakyeon2.data.YakyeonRepositoryProvider;
import com.example.yakyeon2.data.YakyeonSession;
import com.example.yakyeon2.navigation.MedScreen;
import com.example.yakyeon2.ui.ConfirmDialog;
import com.example.yakyeon2.ui.ShortMessage;
import com.example.yakyeon2.ui.YakyeonTextField;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
public final class MedScreens {

    public interface Host {
        void showMessage(CharSequence message);

        void openScreen(String key);

        void back();

        void popTo(String key);

        void showRegisteredMedications();

        void refresh();

        boolean usesServerRecords();

        void confirmMedicationConsent();

        void submitServerRegistration();

        void capturePrescription();

        void searchMedicineNames(String query, MedicineNamePicker.ResultListener listener);

        void endServerPrescription(long prescriptionId);

        void endServerMedication(long medicationId);

        void deleteServerMedication(long medicationId);

        void openServerSchedule(long prescriptionId);

        void recordServerDose(long doseId, long takenAtMillis);

        void revertServerDose(long doseId);

        void selectServerScheduleDate(java.time.LocalDate date);

        void selectServerHistoryStatus(String statusOrNull);

        void selectServerHistoryRange(java.time.LocalDate from, java.time.LocalDate to);

        void requireRegisteredPillbox(Runnable onReady);
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private MedScreens() {
    }

    public static String titleFor(AppCompatActivity activity, String key) {
        if (MedScreen.LIST.equals(key) || MedScreen.CONSENT.equals(key)) {
            return activity.getString(R.string.med_list_title);
        }
        if (MedScreen.ENVELOPE.equals(key) || MedScreen.EDIT.equals(key)) {
            return activity.getString(R.string.med_envelope_title);
        }
        if (MedScreen.SLOT.equals(key)) {
            return activity.getString(R.string.med_envelope_title);
        }
        if (MedScreen.ALARM.equals(key)) {
            return activity.getString(R.string.med_alarm_title);
        }
        if (MedScreen.SCHEDULE.equals(key)) {
            return activity.getString(R.string.med_schedule_title);
        }
        if (MedScreen.HISTORY.equals(key)) {
            return activity.getString(R.string.med_history_title);
        }
        return null;
    }

    public static View list(AppCompatActivity activity, MedicationStore store, Host host, boolean debuggable) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_list, null);
        TextView summary = root.findViewById(R.id.text_med_summary);
        TextView empty = root.findViewById(R.id.text_med_empty);
        int envelopes = store.activeEnvelopeCount();
        int medicines = store.activeMedicineCount();
        summary.setText(activity.getString(R.string.med_list_summary, envelopes, medicines));
        empty.setVisibility(store.envelopes().isEmpty() ? View.VISIBLE : View.GONE);
        TextView banner = root.findViewById(R.id.text_dev_banner);
        banner.setVisibility(store.hasDevListSample() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.button_dev_list).setVisibility(View.GONE);
        root.findViewById(R.id.button_register_med).setOnClickListener(v -> {
            store.beginDraft();
            host.openScreen(MedScreen.CONSENT);
        });
        LinearLayout active = root.findViewById(R.id.med_active);
        LinearLayout stopped = root.findViewById(R.id.med_stopped);
        boolean anyActive = false;
        boolean anyStopped = false;
        for (SavedEnvelope envelope : store.envelopes()) {
            LinearLayout target = envelope.isStopped() ? stopped : active;
            if (envelope.isStopped()) {
                anyStopped = true;
            } else {
                anyActive = true;
            }
            addEnvelope(activity, store, host, target, envelope);
        }
        root.findViewById(R.id.text_med_active).setVisibility(anyActive ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.text_med_stopped).setVisibility(anyStopped ? View.VISIBLE : View.GONE);
        return root;
    }

    public static AlertDialog consentDialog(AppCompatActivity activity, MedicationStore store, Host host) {
        if (store.draft() == null) {
            store.beginDraft();
        }
        EnvelopeDraft draft = store.draft();
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_consent, null);
        CheckBox collection = root.findViewById(R.id.check_med_collection);
        CheckBox notice = root.findViewById(R.id.check_med_notice);
        collection.setChecked(draft.isCollectionChecked());
        notice.setChecked(draft.isNoticeChecked());
        collection.setOnCheckedChangeListener((button, checked) -> draft.setCollectionChecked(checked));
        notice.setOnCheckedChangeListener((button, checked) -> draft.setNoticeChecked(checked));
        root.findViewById(R.id.button_med_consent_cancel).setOnClickListener(v -> host.back());
        root.findViewById(R.id.button_med_consent_next).setOnClickListener(v -> {
            if (host.usesServerRecords()) {
                if (!draft.isCollectionChecked() || !draft.isNoticeChecked()) {
                    host.showMessage(activity.getString(R.string.med_required));
                    return;
                }
                host.confirmMedicationConsent();
                return;
            }
            host.openScreen(MedScreen.ENVELOPE);
        });
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_ConsentCard)
                .setView(root)
                .setCancelable(true)
                .create();
        dialog.setOnShowListener(shown -> fitConsentCard(activity, dialog, root));
        return dialog;
    }

    private static void fitConsentCard(AppCompatActivity activity, AlertDialog dialog, View root) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = Math.min(Math.round(304f * metrics.density), metrics.widthPixels - Math.round(24f * metrics.density));
        int maxHeight = metrics.heightPixels - Math.round(32f * metrics.density);
        View scroll = root.findViewById(R.id.consent_scroll);
        root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int extra = root.getMeasuredHeight() - scroll.getMeasuredHeight();
        int scrollHeight = Math.max(
                Math.round(160f * metrics.density),
                Math.min(Math.round(635f * metrics.density), maxHeight - extra)
        );
        ViewGroup.LayoutParams scrollParams = scroll.getLayoutParams();
        scrollParams.height = scrollHeight;
        scroll.setLayoutParams(scrollParams);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setDimAmount(0f);
        window.setLayout(width, Math.min(maxHeight, extra + scrollHeight));
    }

    public static View envelope(
            AppCompatActivity activity,
            MedicationStore store,
            YakyeonSession session,
            Host host,
            boolean debuggable
    ) {
        if (store.draft() == null) {
            store.beginDraft();
        }
        EnvelopeDraft draft = store.draft();
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_envelope, null);
        root.findViewById(R.id.text_dev_banner).setVisibility(
                draft.isDevSample() || draft.isFixturePractice() ? View.VISIBLE : View.GONE
        );
        View.OnClickListener capture = v -> {
            if (host.usesServerRecords()) {
                host.capturePrescription();
                return;
            }
            host.showMessage(activity.getString(R.string.med_ocr_unavailable));
        };
        root.findViewById(R.id.button_capture).setOnClickListener(capture);
        root.findViewById(R.id.text_capture).setOnClickListener(capture);
        YakyeonTextField hospital = root.findViewById(R.id.field_hospital);
        hospital.setHintEnabled(false);
        hospital.setText(draft.getHospitalName());
        if (hospital.getEditText() != null) {
            hospital.getEditText().setHint(R.string.med_hospital_hint);
            hospital.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    draft.setHospitalName(s == null ? "" : s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
        LinearLayout items = root.findViewById(R.id.envelope_items);
        for (int index = 0; index < draft.medicines().size(); index++) {
            items.addView(draftRow(activity, store, host, draft.medicines().get(index), index));
        }
        root.findViewById(R.id.button_add_medicine).setOnClickListener(v -> {
            store.addMedicine(LocalDate.now(zone()));
            host.refresh();
        });
        root.findViewById(R.id.button_dev_envelope).setVisibility(View.GONE);
        root.findViewById(R.id.button_without_pillbox).setOnClickListener(v -> openWithoutPillbox(store, host));
        root.findViewById(R.id.button_into_pillbox).setOnClickListener(v -> openSlot(activity, store, session, host));
        return root;
    }

    public static View edit(AppCompatActivity activity, MedicationStore store, YakyeonSession session, Host host) {
        MedicineDraft medicine = store.editingDraft();
        if (medicine == null) {
            store.addMedicine(LocalDate.now(zone()));
            medicine = store.editingDraft();
        }
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_form, null);
        bindForm(activity, host, root, medicine, false, () -> {
            if (store.deleteEditingDraft()) {
                host.back();
            }
        }, null);
        root.findViewById(R.id.button_without_pillbox).setOnClickListener(v -> openWithoutPillbox(store, host));
        root.findViewById(R.id.button_into_pillbox).setOnClickListener(v -> openSlot(activity, store, session, host));
        return root;
    }

    public static void showSavedCard(AppCompatActivity activity, MedicationStore store, Host host) {
        SavedMedicine saved = store.editingSaved();
        SavedEnvelope envelope = store.editingEnvelope();
        if (saved == null || envelope == null) {
            return;
        }
        MedicineDraft edited = copy(saved);
        View form = LayoutInflater.from(activity).inflate(R.layout.dialog_alarm_card, null);
        Dialog dialog = new Dialog(activity, R.style.ThemeOverlay_Yakyeon_Popup);
        dialog.setContentView(form);
        dialog.setCancelable(true);
        bindAlarmCard(activity, form, edited, () -> ConfirmDialog.show(
                activity,
                activity.getString(R.string.med_delete_title),
                activity.getString(R.string.med_delete_message),
                activity.getString(R.string.med_delete),
                activity.getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        store.deleteEditingSaved();
                        dialog.dismiss();
                        host.refresh();
                    }

                    @Override
                    public void onCancel() {
                    }
                },
                ConfirmDialog.Mark.TRASH,
                ConfirmDialog.Tone.DANGER
        ), () -> askToApply(activity, store, edited, dialog, host));
        dialog.setOnShowListener(shown -> fitPopup(activity, dialog, Math.round(324f)));
        dialog.show();
    }

    public static View slot(AppCompatActivity activity, MedicationStore store, YakyeonSession session, Host host) {
        if (store.slotAssignEnvelopeId() != null) {
            return savedEnvelopeSlot(activity, store, session, host);
        }
        EnvelopeDraft draft = store.draft();
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_slot, null);
        String hospital = draft == null || draft.getHospitalName().trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : draft.getHospitalName();
        List<String> names = new ArrayList<>();
        List<Integer> rounds = new ArrayList<>();
        if (draft != null) {
            for (MedicineDraft medicine : draft.medicines()) {
                names.add(displayName(activity, medicine.getName()));
                rounds.add(medicine.rounds().size());
            }
        }
        bindSlotBundle(activity, root, hospital, names, rounds);
        TextView note = root.findViewById(R.id.text_slot_note);
        note.setText(activity.getString(R.string.med_slot_no_duplicate)
                + "\n"
                + activity.getString(R.string.med_slot_device_separate));
        LinearLayout list = root.findViewById(R.id.slot_list);
        boolean serverDraft = host.usesServerRecords();
        for (int index = 0; index < (serverDraft ? 3 : session.slotCount()); index++) {
            boolean occupied = !serverDraft && session.isSlotOccupied(index);
            boolean selected = draft != null && draft.getSlotIndex() != null && draft.getSlotIndex() == index;
            int slotIndex = index;
            View row = slotChoice(activity, store, index, occupied, selected);
            row.setOnClickListener(v -> {
                if (occupied || draft == null) {
                    host.showMessage(activity.getString(R.string.pillbox_slot_full));
                    return;
                }
                draft.setSlotIndex(slotIndex);
                host.refresh();
            });
            list.addView(row);
        }
        root.findViewById(R.id.button_slot_without).setVisibility(View.GONE);
        root.findViewById(R.id.button_slot_done).setOnClickListener(v -> {
            if (draft == null || draft.getSlotIndex() == null) {
                host.showMessage(activity.getString(R.string.med_slot_need));
                return;
            }
            if (!host.usesServerRecords() && session.isSlotOccupied(draft.getSlotIndex())) {
                host.showMessage(activity.getString(R.string.pillbox_slot_full));
                return;
            }
            host.openScreen(MedScreen.ALARM);
        });
        return root;
    }

    public static Dialog alarmDialog(AppCompatActivity activity, MedicationStore store, Host host) {
        boolean editing = store.isAlarmEdit();
        SavedEnvelope saved = editing ? store.alarmEditEnvelope() : null;
        EnvelopeDraft draft = editing ? null : store.draft();
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_alarm, null);
        TextView title = root.findViewById(R.id.text_alarm_title);
        String hospital = editing
                ? (saved == null ? "" : saved.getHospitalName())
                : (draft == null ? "" : draft.getHospitalName());
        if (hospital == null || hospital.trim().isEmpty()) {
            title.setText(activity.getString(R.string.med_alarm_heading_plain));
        } else {
            title.setText(activity.getString(R.string.med_alarm_heading, hospital.trim()));
        }
        if (editing) {
            StringBuilder body = new StringBuilder(activity.getString(R.string.med_alarm_edit_subtitle));
            body.append('\n').append(activity.getString(R.string.med_bundle_name_help));
            body.append('\n').append(activity.getString(R.string.med_alarm_unused_rounds));
            if (saved != null) {
                for (SavedMedicine medicine : saved.medicines()) {
                    body.append('\n')
                            .append(displayName(activity, medicine.getName()))
                            .append(" · ")
                            .append(timingsText(activity, medicine.timings()));
                }
            }
            ((TextView) root.findViewById(R.id.text_alarm_subtitle)).setText(body);
            ((TextView) root.findViewById(R.id.text_alarm_summary))
                    .setText(activity.getString(R.string.med_alarm_edit_summary));
        }
        Set<DoseRound> selected = editing
                ? MedicationRules.selectedRounds(saved)
                : MedicationRules.selectedRounds(draft);
        Map<DoseRound, Integer> minutes = editing
                ? store.alarmEditMinutes()
                : (draft == null ? null : draft.alarmMinutes());
        LinearLayout rounds = root.findViewById(R.id.alarm_rounds);
        TextView empty = root.findViewById(R.id.text_alarm_empty);
        boolean any = false;
        if (minutes != null) {
            for (DoseRound round : DoseRound.values()) {
                if (!editing && !selected.contains(round)) {
                    continue;
                }
                any = true;
                rounds.addView(alarmRound(activity, minutes, selected, round, root));
            }
        }
        empty.setVisibility(any ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.alarm_scroll).setVisibility(any ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.button_alarm_save).setOnClickListener(v -> {
            if (editing) {
                finishAlarmEdit(activity, store, host, root);
            } else {
                finishRegistration(activity, store, host, root);
            }
        });
        Dialog dialog = new Dialog(activity, R.style.ThemeOverlay_Yakyeon_AlarmCard);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(shown -> fitAlarmCard(activity, dialog, root));
        return dialog;
    }

    private static void fitAlarmCard(AppCompatActivity activity, Dialog dialog, View root) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int side = Math.round(24f * metrics.density);
        int width = Math.min(Math.round(304f * metrics.density), metrics.widthPixels - side);
        int maxHeight = Math.min(
                Math.round(648f * metrics.density),
                metrics.heightPixels - Math.round(32f * metrics.density)
        );
        View scroll = root.findViewById(R.id.alarm_scroll);
        View rounds = root.findViewById(R.id.alarm_rounds);
        int contentWidth = Math.max(0, width - Math.round(40f * metrics.density));
        rounds.measure(
                View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int extra = root.getMeasuredHeight() - scroll.getMeasuredHeight();
        int scrollHeight = scroll.getVisibility() == View.GONE
                ? 0
                : Math.max(0, Math.min(rounds.getMeasuredHeight(), maxHeight - extra));
        ViewGroup.LayoutParams scrollParams = scroll.getLayoutParams();
        scrollParams.height = scrollHeight;
        scroll.setLayoutParams(scrollParams);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setDimAmount(0f);
        window.getDecorView().setPadding(0, 0, 0, 0);
        window.setLayout(width, Math.min(maxHeight, extra + scrollHeight));
    }

    private static View alarmRound(
            AppCompatActivity activity,
            Map<DoseRound, Integer> minutes,
            Set<DoseRound> selected,
            DoseRound round,
            View dialogRoot
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_alarm_round, null);
        TextView name = row.findViewById(R.id.text_round_name);
        name.setText(roundLabel(activity, round));
        paintWheel(row, MedicationRules.displayedAlarmMinute(
                minutes,
                round,
                selected.contains(round)
        ));
        row.findViewById(R.id.text_hour_prev).setOnClickListener(v ->
                stepAlarm(activity, minutes, selected, round, row, dialogRoot, true, -1));
        row.findViewById(R.id.text_hour_next).setOnClickListener(v ->
                stepAlarm(activity, minutes, selected, round, row, dialogRoot, true, 1));
        row.findViewById(R.id.text_minute_prev).setOnClickListener(v ->
                stepAlarm(activity, minutes, selected, round, row, dialogRoot, false, -1));
        row.findViewById(R.id.text_minute_next).setOnClickListener(v ->
                stepAlarm(activity, minutes, selected, round, row, dialogRoot, false, 1));
        return row;
    }

    private static void stepAlarm(
            AppCompatActivity activity,
            Map<DoseRound, Integer> minutes,
            Set<DoseRound> selected,
            DoseRound round,
            View row,
            View dialogRoot,
            boolean hour,
            int delta
    ) {
        int current = MedicationRules.displayedAlarmMinute(minutes, round, true);
        int next = hour ? ClockTime.shiftHour(current, delta) : ClockTime.shiftMinute(current, delta);
        minutes.put(round, next);
        paintWheel(row, next);
        if (MedicationRules.checkAlarmTimes(selected, minutes) == MedicationRules.TimeCheck.TOO_CLOSE) {
            ShortMessage.show(dialogRoot, activity.getString(R.string.med_time_rule));
        }
    }

    private static void paintWheel(View row, int minutes) {
        TextView clock = row.findViewById(R.id.text_round_clock);
        clock.setText(ClockTime.format12(minutes));
        ((TextView) row.findViewById(R.id.text_hour_prev)).setText(
                ClockTime.formatHour24(ClockTime.shiftHour(minutes, -1))
        );
        ((TextView) row.findViewById(R.id.text_hour_value)).setText(ClockTime.formatHour24(minutes));
        ((TextView) row.findViewById(R.id.text_hour_next)).setText(
                ClockTime.formatHour24(ClockTime.shiftHour(minutes, 1))
        );
        ((TextView) row.findViewById(R.id.text_minute_prev)).setText(
                ClockTime.formatMinute(ClockTime.shiftMinute(minutes, -1))
        );
        ((TextView) row.findViewById(R.id.text_minute_value)).setText(ClockTime.formatMinute(minutes));
        ((TextView) row.findViewById(R.id.text_minute_next)).setText(
                ClockTime.formatMinute(ClockTime.shiftMinute(minutes, 1))
        );
    }

    private static void finishRegistration(
            AppCompatActivity activity,
            MedicationStore store,
            Host host,
            View anchor
    ) {
        if (store.draft() != null && store.draft().isDevSample()) {
            ShortMessage.show(anchor, activity.getString(R.string.med_dev_not_saved));
            return;
        }
        if (host.usesServerRecords()) {
            HomeRegistration.Prepared prepared = HomeRegistration.prepare(store.draft());
            switch (MedicationRules.validate(store.draft())) {
                case NOT_CONFIRMED:
                    showNeedConfirm(activity);
                    return;
                case MISSING_TIME:
                case TOO_CLOSE:
                    showTimeError(activity);
                    return;
                case OK:
                case MISSING_REQUIRED:
                default:
                    break;
            }
            if (prepared.block == HomeRegistration.Block.NONE) {
                host.showRegisteredMedications();
                host.submitServerRegistration();
                return;
            }
            ShortMessage.show(anchor, activity.getString(R.string.med_required));
            return;
        }
        switch (store.commit(zone())) {
            case OK:
                host.showRegisteredMedications();
                break;
            case NOT_CONFIRMED:
                showNeedConfirm(activity);
                break;
            case MISSING_TIME:
            case TOO_CLOSE:
                showTimeError(activity);
                break;
            case MISSING_REQUIRED:
            default:
                ShortMessage.show(anchor, activity.getString(R.string.med_required));
                break;
        }
    }

    private static void finishAlarmEdit(
            AppCompatActivity activity,
            MedicationStore store,
            Host host,
            View anchor
    ) {
        if (store.saveAlarmEdit(zone(), System.currentTimeMillis())) {
            host.popTo(MedScreen.LIST);
            host.showMessage(activity.getString(R.string.med_local_saved));
            return;
        }
        showTimeError(activity);
    }

    private static void showNeedConfirm(AppCompatActivity activity) {
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.med_need_confirm_body),
                "",
                activity.getString(R.string.action_confirm),
                null,
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    private static void showTimeError(AppCompatActivity activity) {
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.med_time_error_title),
                activity.getString(R.string.med_time_error_body),
                activity.getString(R.string.med_yes),
                activity.getString(R.string.med_no),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    private static void fitPopup(AppCompatActivity activity, Dialog dialog, int widthDp) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = Math.min(
                Math.round(widthDp * metrics.density) + Math.round(16f * metrics.density),
                metrics.widthPixels - Math.round(12f * metrics.density)
        );
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static void openWithoutPillbox(MedicationStore store, Host host) {
        if (store.draft() != null) {
            store.draft().setSlotIndex(null);
        }
        host.openScreen(MedScreen.ALARM);
    }

    private static void openSlot(
            AppCompatActivity activity,
            MedicationStore store,
            YakyeonSession session,
            Host host
    ) {
        host.requireRegisteredPillbox(() -> {
            if (!host.usesServerRecords() && session.occupiedSlotCount() >= session.slotCount()) {
                host.showMessage(activity.getString(R.string.med_slot_none_available));
                return;
            }
            host.openScreen(MedScreen.SLOT);
        });
    }

    private static void addEnvelope(
            AppCompatActivity activity,
            MedicationStore store,
            Host host,
            LinearLayout target,
            SavedEnvelope envelope
    ) {
        View card = LayoutInflater.from(activity).inflate(R.layout.item_med_envelope, target, false);
        String name = envelope.getHospitalName().trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : envelope.getHospitalName();
        ((TextView) card.findViewById(R.id.text_envelope_name)).setText(name);
        ((TextView) card.findViewById(R.id.text_envelope_count)).setText(
                activity.getString(R.string.med_bundle_count, envelope.medicines().size())
        );
        TextView badge = card.findViewById(R.id.text_envelope_badge);
        View actions = card.findViewById(R.id.row_envelope_actions);
        View stop = card.findViewById(R.id.button_envelope_stop);
        if (envelope.isStopped()) {
            badge.setText(R.string.med_stopped);
            badge.setTextColor(activity.getColor(R.color.yakyeon_muted));
            badge.setBackgroundResource(R.drawable.bg_med_ended_badge);
            actions.setVisibility(View.GONE);
            stop.setVisibility(View.GONE);
        } else {
            Long days = envelopeDaysLeft(envelope);
            if (days == null) {
                badge.setVisibility(View.GONE);
            } else {
                badge.setText(activity.getString(R.string.med_days_left, days));
            }
            card.findViewById(R.id.button_edit_time).setOnClickListener(v -> {
                if (store.beginAlarmEdit(envelope.getId())) {
                    host.openScreen(MedScreen.ALARM);
                }
            });
            card.findViewById(R.id.button_change_slot).setOnClickListener(v -> {
                if (store.beginSlotAssign(envelope.getId())) {
                    host.openScreen(MedScreen.SLOT);
                }
            });
            stop.setOnClickListener(v -> ConfirmDialog.show(
                    activity,
                    activity.getString(R.string.med_stop_title),
                    activity.getString(R.string.med_stop_message),
                    activity.getString(R.string.med_stop_confirm),
                    activity.getString(R.string.action_cancel),
                    new ConfirmDialog.Callback() {
                        @Override
                        public void onConfirm() {
                            store.stopEnvelope(envelope.getId(), System.currentTimeMillis());
                            host.refresh();
                        }

                        @Override
                        public void onCancel() {
                        }
                    },
                    ConfirmDialog.Mark.CHECK,
                    ConfirmDialog.Tone.PRIMARY
            ));
        }
        LinearLayout medicines = card.findViewById(R.id.envelope_medicines);
        for (SavedMedicine medicine : envelope.medicines()) {
            View row = LayoutInflater.from(activity).inflate(R.layout.item_med_list_medicine, medicines, false);
            ((TextView) row.findViewById(R.id.text_medicine_name)).setText(displayName(activity, medicine.getName()));
            ((TextView) row.findViewById(R.id.text_medicine_frequency)).setText(
                    activity.getString(R.string.med_frequency, medicine.rounds().size())
            );
            if (!envelope.isStopped()) {
                row.setOnLongClickListener(v -> {
                    if (store.beginAlarmEdit(envelope.getId())) {
                        host.openScreen(MedScreen.ALARM);
                    }
                    return true;
                });
            }
            medicines.addView(row);
        }
        target.addView(card);
    }

    private static Long envelopeDaysLeft(SavedEnvelope envelope) {
        LocalDate today = LocalDate.now(zone());
        Long soonest = null;
        for (SavedMedicine medicine : envelope.medicines()) {
            if (medicine.getEnd() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, medicine.getEnd());
            if (days < 0) {
                continue;
            }
            if (soonest == null || days < soonest) {
                soonest = days;
            }
        }
        return soonest;
    }

    private static View draftRow(
            AppCompatActivity activity,
            MedicationStore store,
            Host host,
            MedicineDraft medicine,
            int index
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_med_draft_row, null);
        boolean expanded = store.editingIndex() == index;
        TextView name = row.findViewById(R.id.text_row_name);
        TextView frequency = row.findViewById(R.id.text_row_frequency);
        name.setText(displayName(activity, medicine.getName()));
        frequency.setText(activity.getString(R.string.med_frequency, medicine.rounds().size()));
        row.findViewById(R.id.text_row_chevron).setVisibility(expanded ? View.GONE : View.VISIBLE);
        View panel = row.findViewById(R.id.panel_expand);
        panel.setVisibility(expanded ? View.VISIBLE : View.GONE);
        row.findViewById(R.id.row_header).setOnClickListener(v -> {
            if (expanded) {
                store.collapseEditing();
            } else {
                store.editDraftAt(index);
            }
            host.refresh();
        });
        if (!expanded) {
            return row;
        }
        TextView nameValue = row.findViewById(R.id.text_name_value);
        String currentName = medicine.getName();
        nameValue.setText(currentName == null || currentName.trim().isEmpty()
                ? activity.getString(R.string.med_name_missing)
                : currentName);
        showNameKind(activity, row.findViewById(R.id.text_name_kind), currentName);
        nameValue.setOnClickListener(v -> pickMedicineName(activity, medicine, host, host::refresh));
        bindAmount(activity, row, medicine);
        CheckBox timingBed = row.findViewById(R.id.check_timing_bed);
        bindTiming(row.findViewById(R.id.check_timing_before), medicine, DoseTiming.BEFORE_MEAL);
        bindTiming(row.findViewById(R.id.check_timing_after), medicine, DoseTiming.AFTER_MEAL_30);
        bindRound(row.findViewById(R.id.check_morning), medicine, DoseRound.MORNING, row, activity);
        bindRound(row.findViewById(R.id.check_lunch), medicine, DoseRound.LUNCH, row, activity);
        bindRound(row.findViewById(R.id.check_evening), medicine, DoseRound.EVENING, row, activity);
        bindRound(row.findViewById(R.id.check_bedtime), medicine, DoseRound.BEDTIME, row, activity,
                () -> syncBedtimeTiming(medicine, timingBed));
        syncBedtimeTiming(medicine, timingBed);
        TextView periodStart = row.findViewById(R.id.text_period_start);
        TextView periodEnd = row.findViewById(R.id.text_period_end);
        paintDateField(activity, periodStart, medicine.getStart(), R.string.med_period_start);
        paintDateField(activity, periodEnd, medicine.getEnd(), R.string.med_period_end);
        periodStart.setOnClickListener(v -> pickDate(activity, medicine.getStart(), start -> {
            medicine.setStart(start);
            paintDateField(activity, periodStart, start, R.string.med_period_start);
        }));
        periodEnd.setOnClickListener(v -> pickDate(activity, medicine.getEnd(), end -> {
            medicine.setEnd(end);
            paintDateField(activity, periodEnd, end, R.string.med_period_end);
        }));
        CheckBox confirm = row.findViewById(R.id.check_confirmed);
        confirm.setChecked(medicine.isConfirmed());
        confirm.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !canConfirm(medicine)) {
                button.setChecked(false);
                ShortMessage.show(row, activity.getString(R.string.med_required));
                return;
            }
            medicine.setConfirmed(checked);
        });
        row.findViewById(R.id.button_delete_med).setOnClickListener(v -> ConfirmDialog.show(
                activity,
                activity.getString(R.string.med_delete_title),
                activity.getString(R.string.med_delete_message),
                activity.getString(R.string.med_delete),
                activity.getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        store.editDraftAt(index);
                        store.deleteEditingDraft();
                        host.refresh();
                    }

                    @Override
                    public void onCancel() {
                    }
                },
                ConfirmDialog.Mark.TRASH,
                ConfirmDialog.Tone.DANGER
        ));
        return row;
    }

    private static void pickMedicineName(
            AppCompatActivity activity,
            MedicineDraft medicine,
            Host host,
            Runnable after
    ) {
        if (host != null && host.usesServerRecords()) {
            MedicineNamePicker.showRemote(activity, host::searchMedicineNames, (name, drugId) -> {
                medicine.setName(name);
                medicine.setDrugId(drugId);
                after.run();
            });
            return;
        }
        MedicineNamePicker.show(activity, YakyeonRepositoryProvider.get().medicineNames(), name -> {
            medicine.setName(name);
            after.run();
        });
    }

    private static void showNameKind(AppCompatActivity activity, TextView kind, String name) {
        String label = nameKind(activity, name);
        if (label == null) {
            kind.setVisibility(View.GONE);
            return;
        }
        kind.setVisibility(View.VISIBLE);
        kind.setText(label);
    }

    private static String nameLine(AppCompatActivity activity, String name) {
        String label = nameKind(activity, name);
        String shown = displayName(activity, name);
        if (label == null) {
            return shown;
        }
        return shown + "\n" + label;
    }

    private static String nameKind(AppCompatActivity activity, String name) {
        MedicineNameCatalog.Source source = YakyeonRepositoryProvider.get().medicineNames().sourceOf(name);
        if (source == MedicineNameCatalog.Source.EXAMPLE) {
            return activity.getString(R.string.med_name_example_selected);
        }
        if (source == MedicineNameCatalog.Source.CUSTOM) {
            return activity.getString(R.string.med_name_custom_selected);
        }
        return null;
    }

    private static String periodLabel(AppCompatActivity activity, MedicineDraft medicine) {
        if (medicine.getStart() == null || medicine.getEnd() == null) {
            return activity.getString(R.string.med_date_unset);
        }
        return activity.getString(
                R.string.med_period_value,
                DATE.format(medicine.getStart()),
                DATE.format(medicine.getEnd())
        );
    }

    private static void bindForm(
            AppCompatActivity activity,
            Host host,
            View root,
            MedicineDraft medicine,
            boolean savedEdit,
            Runnable onDelete,
            Runnable onSave
    ) {
        TextView searchState = root.findViewById(R.id.text_search_state);
        searchState.setText(nameLine(activity, medicine.getName()));
        root.findViewById(R.id.button_search_med).setOnClickListener(v ->
                pickMedicineName(activity, medicine, host, () -> searchState.setText(
                        medicine.getDrugId() == null
                                ? nameLine(activity, medicine.getName())
                                : displayName(activity, medicine.getName())
                )));
        View save = root.findViewById(R.id.button_save_med);
        View confirmed = root.findViewById(R.id.check_confirmed);
        View without = root.findViewById(R.id.button_without_pillbox);
        View into = root.findViewById(R.id.button_into_pillbox);
        if (savedEdit) {
            save.setVisibility(View.VISIBLE);
            confirmed.setVisibility(View.GONE);
            without.setVisibility(View.GONE);
            into.setVisibility(View.GONE);
            save.setOnClickListener(v -> onSave.run());
        }
        android.widget.Button amount = root.findViewById(R.id.button_amount);
        amount.setText(draftAmountLabel(activity, medicine));
        amount.setOnClickListener(v -> showAmountPicker(activity, medicine, () ->
                amount.setText(draftAmountLabel(activity, medicine))));
        bindRound(root.findViewById(R.id.check_morning), medicine, DoseRound.MORNING, root, activity);
        bindRound(root.findViewById(R.id.check_lunch), medicine, DoseRound.LUNCH, root, activity);
        bindRound(root.findViewById(R.id.check_evening), medicine, DoseRound.EVENING, root, activity);
        bindRound(root.findViewById(R.id.check_bedtime), medicine, DoseRound.BEDTIME, root, activity);
        updateFrequency(activity, root, medicine);
        android.widget.Button timing = root.findViewById(R.id.button_timing);
        timing.setText(timingsText(activity, medicine.timings()));
        timing.setOnClickListener(v -> pickTimings(activity, medicine, () ->
                timing.setText(timingsText(activity, medicine.timings()))));
        android.widget.Button start = root.findViewById(R.id.button_start);
        android.widget.Button end = root.findViewById(R.id.button_end);
        start.setText(dateLabel(activity, activity.getString(R.string.med_period_start), medicine.getStart()));
        end.setText(dateLabel(activity, activity.getString(R.string.med_period_end), medicine.getEnd()));
        start.setOnClickListener(v -> pickDate(activity, medicine.getStart(), picked -> {
            medicine.setStart(picked);
            start.setText(dateLabel(activity, activity.getString(R.string.med_period_start), picked));
        }));
        end.setOnClickListener(v -> pickDate(activity, medicine.getEnd(), picked -> {
            medicine.setEnd(picked);
            end.setText(dateLabel(activity, activity.getString(R.string.med_period_end), picked));
        }));
        CheckBox confirm = root.findViewById(R.id.check_confirmed);
        confirm.setChecked(medicine.isConfirmed());
        confirm.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !canConfirm(medicine)) {
                button.setChecked(false);
                ShortMessage.show(root, activity.getString(R.string.med_required));
                return;
            }
            medicine.setConfirmed(checked);
        });
        root.findViewById(R.id.button_delete_med).setOnClickListener(v -> {
            if (savedEdit) {
                onDelete.run();
                return;
            }
            ConfirmDialog.show(
                    activity,
                    activity.getString(R.string.med_delete_title),
                    activity.getString(R.string.med_delete_message),
                    activity.getString(R.string.med_delete),
                    activity.getString(R.string.action_cancel),
                    new ConfirmDialog.Callback() {
                        @Override
                        public void onConfirm() {
                            onDelete.run();
                        }

                        @Override
                        public void onCancel() {
                        }
                    },
                    ConfirmDialog.Mark.TRASH,
                    ConfirmDialog.Tone.DANGER
            );
        });
    }

    private static void bindAlarmCard(
            AppCompatActivity activity,
            View root,
            MedicineDraft medicine,
            Runnable onDelete,
            Runnable onSave
    ) {
        TextView name = root.findViewById(R.id.text_alarm_name);
        TextView amount = root.findViewById(R.id.text_alarm_amount);
        TextView timing = root.findViewById(R.id.text_alarm_timing);
        TextView period = root.findViewById(R.id.text_alarm_period);
        Runnable paint = () -> {
            name.setText(displayName(activity, medicine.getName()));
            amount.setText(draftAmountLabel(activity, medicine));
            timing.setText(timingsText(activity, medicine.timings()));
            period.setText(periodLabel(activity, medicine));
            updateFrequency(activity, root, medicine);
        };
        paint.run();
        name.setOnClickListener(v -> pickMedicineName(activity, medicine, null, paint));
        amount.setOnClickListener(v -> showAmountPicker(activity, medicine, paint));
        timing.setOnClickListener(v -> pickTimings(activity, medicine, paint));
        period.setOnClickListener(v -> pickDate(activity, medicine.getStart(), start -> {
            medicine.setStart(start);
            pickDate(activity, medicine.getEnd(), end -> {
                medicine.setEnd(end);
                paint.run();
            });
        }));
        bindRound(root.findViewById(R.id.check_morning), medicine, DoseRound.MORNING, root, activity);
        bindRound(root.findViewById(R.id.check_lunch), medicine, DoseRound.LUNCH, root, activity);
        bindRound(root.findViewById(R.id.check_evening), medicine, DoseRound.EVENING, root, activity);
        bindRound(root.findViewById(R.id.check_bedtime), medicine, DoseRound.BEDTIME, root, activity);
        updateFrequency(activity, root, medicine);
        root.findViewById(R.id.button_alarm_delete).setOnClickListener(v -> onDelete.run());
        root.findViewById(R.id.button_alarm_save).setOnClickListener(v -> onSave.run());
    }

    private static void askToApply(
            AppCompatActivity activity,
            MedicationStore store,
            MedicineDraft edited,
            Dialog dialog,
            Host host
    ) {
        if (!MedicationRules.hasRequiredFields(edited)) {
            showTimeError(activity);
            return;
        }
        SavedEnvelope envelope = store.editingEnvelope();
        MedicationRules.TimeCheck times = envelope == null
                ? MedicationRules.TimeCheck.MISSING
                : MedicationRules.checkAlarmTimes(edited.rounds(), envelope.alarmMinutes());
        if (times != MedicationRules.TimeCheck.OK) {
            showTimeError(activity);
            return;
        }
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.med_edit_confirm_title),
                "",
                activity.getString(R.string.med_yes),
                activity.getString(R.string.med_no),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        if (store.applySavedEdit(edited, zone())) {
                            dialog.dismiss();
                            host.refresh();
                            host.showMessage(activity.getString(R.string.med_local_saved));
                        } else {
                            host.showMessage(activity.getString(R.string.med_required));
                        }
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    private static void bindRound(
            CheckBox box,
            MedicineDraft medicine,
            DoseRound round,
            View root,
            AppCompatActivity activity
    ) {
        bindRound(box, medicine, round, root, activity, null);
    }

    private static void bindRound(
            CheckBox box,
            MedicineDraft medicine,
            DoseRound round,
            View root,
            AppCompatActivity activity,
            Runnable afterChange
    ) {
        box.setOnCheckedChangeListener(null);
        box.setChecked(medicine.rounds().contains(round));
        box.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                medicine.setPrn(false);
                medicine.rounds().add(round);
            } else {
                medicine.rounds().remove(round);
            }
            updateFrequency(activity, root, medicine);
            if (afterChange != null) {
                afterChange.run();
            }
        });
    }

    private static void bindTiming(CheckBox box, MedicineDraft medicine, DoseTiming timing) {
        box.setOnCheckedChangeListener(null);
        box.setChecked(medicine.timings().contains(timing));
        box.setOnCheckedChangeListener((button, checked) -> {
            medicine.clearMealRelation();
            if (checked) {
                medicine.timings().add(timing);
            } else {
                medicine.timings().remove(timing);
            }
        });
    }

    private static void syncBedtimeTiming(MedicineDraft medicine, CheckBox timingBed) {
        boolean allowed = medicine.rounds().contains(DoseRound.BEDTIME);
        if (!allowed) {
            medicine.timings().remove(DoseTiming.BEDTIME);
        }
        timingBed.setOnCheckedChangeListener(null);
        timingBed.setChecked(medicine.timings().contains(DoseTiming.BEDTIME));
        timingBed.setEnabled(allowed);
        timingBed.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !medicine.rounds().contains(DoseRound.BEDTIME)) {
                button.setChecked(false);
                return;
            }
            if (checked) {
                medicine.timings().add(DoseTiming.BEDTIME);
            } else {
                medicine.timings().remove(DoseTiming.BEDTIME);
            }
        });
    }

    private static void paintDateField(
            AppCompatActivity activity,
            TextView field,
            LocalDate date,
            int emptyLabel
    ) {
        if (date == null) {
            field.setText(activity.getString(emptyLabel));
            field.setTextColor(activity.getColor(R.color.yakyeon_muted));
            return;
        }
        field.setText(DATE.format(date));
        field.setTextColor(activity.getColor(R.color.yakyeon_text));
    }

    private static void updateFrequency(AppCompatActivity activity, View root, MedicineDraft medicine) {
        TextView frequency = root.findViewById(R.id.text_frequency);
        if (frequency == null) {
            frequency = root.findViewById(R.id.text_row_frequency);
        }
        if (frequency == null) {
            return;
        }
        frequency.setText(activity.getString(R.string.med_frequency, medicine.rounds().size()));
    }

    private static void pickDate(AppCompatActivity activity, LocalDate current, java.util.function.Consumer<LocalDate> onPick) {
        LocalDate initial = current == null ? LocalDate.now(zone()) : current;
        new DatePickerDialog(activity, (view, year, month, day) ->
                onPick.accept(LocalDate.of(year, month + 1, day)),
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        ).show();
    }

    static String roundLabel(AppCompatActivity activity, DoseRound round) {
        switch (round) {
            case LUNCH:
                return activity.getString(R.string.round_lunch);
            case EVENING:
                return activity.getString(R.string.round_evening);
            case BEDTIME:
                return activity.getString(R.string.round_bedtime);
            case MORNING:
            default:
                return activity.getString(R.string.round_morning);
        }
    }

    static String statusLabel(AppCompatActivity activity, com.example.yakyeon2.data.DoseStatus status) {
        switch (status) {
            case COMPLETED:
                return activity.getString(R.string.med_status_completed);
            case DELAYED:
                return activity.getString(R.string.med_status_delayed);
            case MISSED:
                return activity.getString(R.string.med_status_missed);
            case SCHEDULED:
            default:
                return activity.getString(R.string.med_status_scheduled);
        }
    }

    private static MedicineDraft copy(SavedMedicine saved) {
        MedicineDraft edited = new MedicineDraft();
        edited.setName(saved.getName());
        edited.setAmount(saved.getAmount());
        edited.rounds().addAll(saved.rounds());
        edited.timings().clear();
        edited.timings().addAll(saved.timings());
        edited.setStart(saved.getStart());
        edited.setEnd(saved.getEnd());
        edited.setConfirmed(true);
        return edited;
    }

    private static String displayName(AppCompatActivity activity, String name) {
        if (name == null || name.trim().isEmpty()) {
            return activity.getString(R.string.med_name_missing);
        }
        return name;
    }

    private static String medicineDetail(AppCompatActivity activity, MedicineDraft medicine) {
        return draftAmountLabel(activity, medicine) + " · " + timingsText(activity, medicine.timings());
    }

    private static String draftAmountLabel(AppCompatActivity activity, MedicineDraft medicine) {
        String quantity = medicine.getDoseText();
        String unit = medicine.getDoseUnit();
        if (quantity != null && !quantity.isEmpty() && unit != null) {
            return quantity + unit;
        }
        if (medicine.getAmount() == null) {
            return activity.getString(R.string.med_amount_unset);
        }
        return amountLabel(activity, medicine.getAmount());
    }

    private static String dateLabel(AppCompatActivity activity, String prefix, LocalDate date) {
        if (date == null) {
            return prefix + " · " + activity.getString(R.string.med_date_unset);
        }
        return prefix + " · " + DATE.format(date);
    }

    private static String amountLabel(AppCompatActivity activity, DoseAmount amount) {
        return amount.quantityText() + amount.unitText();
    }

    private static void bindAmount(AppCompatActivity activity, View row, MedicineDraft medicine) {
        EditText number = row.findViewById(R.id.field_amount_number);
        TextView unitView = row.findViewById(R.id.text_amount_unit);
        DoseAmount current = medicine.getAmount();
        String unit = medicine.getDoseUnit();
        if (unit == null && current != null) {
            unit = current.unitText();
        }
        if (unit == null) {
            unit = DoseAmount.UNIT_TABLET;
        }
        String[] chosenUnit = new String[] {unit};
        String quantity = medicine.getDoseText();
        if ((quantity == null || quantity.isEmpty()) && current != null) {
            quantity = current.quantityText();
        }
        number.setText(quantity == null ? "" : quantity);
        paintUnit(activity, unitView, chosenUnit[0]);
        number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyTypedAmount(medicine, s == null ? "" : s.toString(), chosenUnit[0]);
            }
        });
        number.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                warnInvalidAmount(activity, row, number.getText().toString(), chosenUnit[0]);
            }
        });
        unitView.setOnClickListener(v -> {
            String[] units = DoseAmount.units();
            new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                    .setTitle(activity.getString(R.string.med_amount_unit))
                    .setItems(units, (dialog, which) -> {
                        chosenUnit[0] = units[which];
                        paintUnit(activity, unitView, chosenUnit[0]);
                        applyTypedAmount(medicine, number.getText().toString(), chosenUnit[0]);
                        warnInvalidAmount(activity, row, number.getText().toString(), chosenUnit[0]);
                    })
                    .show();
        });
    }

    private static void paintUnit(AppCompatActivity activity, TextView unitView, String unit) {
        if (unit == null) {
            unitView.setText(activity.getString(R.string.med_amount_unit));
            unitView.setTextColor(activity.getColor(R.color.yakyeon_muted));
            return;
        }
        unitView.setText(unit);
        unitView.setTextColor(activity.getColor(R.color.yakyeon_text));
    }

    private static void applyTypedAmount(MedicineDraft medicine, String raw, String unit) {
        String quantity = DoseAmount.canonicalQuantity(raw);
        String resolved = unit == null ? DoseAmount.UNIT_TABLET : unit;
        if (quantity == null || !DoseAmount.knownUnit(resolved)) {
            medicine.setAmount(null);
            return;
        }
        DoseAmount picked = DoseAmount.from(resolved, quantity);
        if (picked != null) {
            medicine.setAmount(picked);
            return;
        }
        medicine.setFreeDose(quantity, resolved);
    }

    private static void warnInvalidAmount(
            AppCompatActivity activity,
            View row,
            String raw,
            String unit
    ) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return;
        }
        String quantity = DoseAmount.canonicalQuantity(text);
        if (quantity == null) {
            ShortMessage.show(row, activity.getString(R.string.med_amount_rejected));
        }
    }

    private static void showAmountPicker(AppCompatActivity activity, MedicineDraft medicine, Runnable after) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.space_m);
        column.setPadding(pad, pad, pad, 0);
        TextView numberLabel = new TextView(activity);
        numberLabel.setText(R.string.med_amount_number);
        numberLabel.setTextColor(activity.getColor(R.color.yakyeon_muted));
        numberLabel.setTextSize(12);
        TextView unitLabel = new TextView(activity);
        unitLabel.setText(R.string.med_amount_unit);
        unitLabel.setTextColor(activity.getColor(R.color.yakyeon_muted));
        unitLabel.setTextSize(12);
        EditText number = new EditText(activity);
        number.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        number.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789."));
        number.setHint(R.string.med_amount_number);
        number.setSingleLine(true);
        number.setTextColor(activity.getColor(R.color.yakyeon_text));
        number.setHintTextColor(activity.getColor(R.color.yakyeon_muted));
        android.widget.Spinner unit = new android.widget.Spinner(activity);
        String[] units = DoseAmount.units();
        unit.setAdapter(new android.widget.ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                units
        ));
        DoseAmount current = medicine.getAmount();
        String shownQuantity = medicine.getDoseText();
        String shownUnit = medicine.getDoseUnit();
        if ((shownQuantity == null || shownQuantity.isEmpty()) && current != null) {
            shownQuantity = current.quantityText();
        }
        if (shownUnit == null && current != null) {
            shownUnit = current.unitText();
        }
        if (shownUnit == null) {
            shownUnit = DoseAmount.UNIT_TABLET;
        }
        int unitIndex = 0;
        if (shownQuantity != null && !shownQuantity.isEmpty()) {
            number.setText(shownQuantity);
        }
        for (int index = 0; index < units.length; index++) {
            if (units[index].equals(shownUnit)) {
                unitIndex = index;
            }
        }
        unit.setSelection(unitIndex);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        unitParams.setMarginStart(activity.getResources().getDimensionPixelSize(R.dimen.space_s));
        LinearLayout numberColumn = new LinearLayout(activity);
        numberColumn.setOrientation(LinearLayout.VERTICAL);
        numberColumn.addView(numberLabel);
        numberColumn.addView(number);
        LinearLayout unitColumn = new LinearLayout(activity);
        unitColumn.setOrientation(LinearLayout.VERTICAL);
        unitColumn.addView(unitLabel);
        unitColumn.addView(unit);
        row.addView(numberColumn, numberParams);
        row.addView(unitColumn, unitParams);
        column.addView(row);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setTitle(activity.getString(R.string.med_amount))
                .setView(column)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(shown -> {
            number.requestFocus();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int unitPosition = unit.getSelectedItemPosition();
                if (unitPosition < 0) {
                    unitPosition = 0;
                }
                String raw = number.getText() == null ? "" : number.getText().toString();
                String quantity = DoseAmount.canonicalQuantity(raw);
                String chosen = units[unitPosition];
                if (quantity == null || !DoseAmount.knownUnit(chosen)) {
                    ShortMessage.show(column, activity.getString(R.string.med_amount_rejected));
                    return;
                }
                DoseAmount picked = DoseAmount.from(chosen, quantity);
                if (picked != null) {
                    medicine.setAmount(picked);
                } else {
                    medicine.setFreeDose(quantity, chosen);
                }
                after.run();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private static CharSequence slotNote(AppCompatActivity activity) {
        return activity.getString(R.string.med_slot_no_duplicate)
                + "\n"
                + activity.getString(R.string.med_slot_device_separate)
                + "\n"
                + activity.getString(R.string.med_slot_local_note);
    }

    private static void bindSlotBundle(
            AppCompatActivity activity,
            View root,
            String hospital,
            List<String> names,
            List<Integer> rounds
    ) {
        ((TextView) root.findViewById(R.id.text_slot_hospital)).setText(hospital);
        ((TextView) root.findViewById(R.id.text_slot_count)).setText(
                activity.getString(R.string.med_slot_medicine_count, names.size())
        );
        LinearLayout medicines = root.findViewById(R.id.slot_medicines);
        medicines.removeAllViews();
        for (int index = 0; index < names.size(); index++) {
            View row = LayoutInflater.from(activity).inflate(R.layout.item_med_list_medicine, medicines, false);
            row.setBackground(null);
            row.setClickable(false);
            ((TextView) row.findViewById(R.id.text_medicine_name)).setText(names.get(index));
            ((TextView) row.findViewById(R.id.text_medicine_frequency)).setText(
                    activity.getString(R.string.med_frequency, rounds.get(index))
            );
            medicines.addView(row);
        }
    }

    private static View slotChoice(
            AppCompatActivity activity,
            MedicationStore store,
            int index,
            boolean occupied,
            boolean selected
    ) {
        View card = LayoutInflater.from(activity).inflate(R.layout.item_slot_choice, null);
        if (occupied) {
            card.setBackgroundResource(R.drawable.bg_slot_locked);
        } else if (selected) {
            card.setBackgroundResource(R.drawable.bg_slot_selected);
        } else {
            card.setBackgroundResource(R.drawable.bg_slot_idle);
        }
        TextView title = card.findViewById(R.id.text_choice_title);
        title.setText(activity.getString(R.string.pillbox_slot_name, index + 1));
        TextView detail = card.findViewById(R.id.text_choice_detail);
        TextView badge = card.findViewById(R.id.text_choice_badge);
        android.widget.ImageView state = card.findViewById(R.id.icon_choice_state);
        if (occupied) {
            String hospital = hospitalInSlot(store, index);
            detail.setText(hospital == null
                    ? activity.getString(R.string.pillbox_slot_full)
                    : activity.getString(R.string.pillbox_slot_locked, hospital));
            badge.setVisibility(View.VISIBLE);
            state.setVisibility(View.VISIBLE);
            state.setImageResource(R.drawable.ic_slot_lock);
            state.setContentDescription(activity.getString(R.string.pillbox_slot_in_use));
        } else if (selected) {
            detail.setText(keepsCurrentSlot(store, index)
                    ? R.string.med_slot_selected
                    : R.string.pillbox_slot_chosen);
            detail.setTextColor(activity.getColor(R.color.yakyeon_brand));
            state.setVisibility(View.VISIBLE);
            state.setImageResource(R.drawable.ic_slot_check);
            state.setContentDescription(activity.getString(R.string.med_slot_selected));
        } else {
            detail.setText(R.string.pillbox_slot_open);
        }
        int gap = activity.getResources().getDimensionPixelSize(R.dimen.space_s);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (index > 0) {
            params.topMargin = gap + Math.round(2f * activity.getResources().getDisplayMetrics().density);
        }
        card.setLayoutParams(params);
        return card;
    }

    private static boolean keepsCurrentSlot(MedicationStore store, int index) {
        String assignId = store.slotAssignEnvelopeId();
        if (assignId == null) {
            return false;
        }
        for (SavedEnvelope envelope : store.envelopes()) {
            if (!assignId.equals(envelope.getId())) {
                continue;
            }
            Integer current = envelope.getSlotIndex();
            return current != null && current == index;
        }
        return false;
    }

    private static String hospitalInSlot(MedicationStore store, int index) {
        for (SavedEnvelope envelope : store.envelopes()) {
            if (envelope.isStopped() || envelope.getSlotIndex() == null || envelope.getSlotIndex() != index) {
                continue;
            }
            String name = envelope.getHospitalName();
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return name.trim();
        }
        return null;
    }

    private static View savedEnvelopeSlot(
            AppCompatActivity activity,
            MedicationStore store,
            YakyeonSession session,
            Host host
    ) {
        SavedEnvelope envelope = null;
        String assignId = store.slotAssignEnvelopeId();
        for (SavedEnvelope candidate : store.envelopes()) {
            if (candidate.getId().equals(assignId)) {
                envelope = candidate;
                break;
            }
        }
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_slot, null);
        String hospital = envelope == null || envelope.getHospitalName().trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : envelope.getHospitalName();
        List<String> names = new ArrayList<>();
        List<Integer> rounds = new ArrayList<>();
        if (envelope != null) {
            for (SavedMedicine medicine : envelope.medicines()) {
                names.add(displayName(activity, medicine.getName()));
                rounds.add(medicine.rounds().size());
            }
        }
        bindSlotBundle(activity, root, hospital, names, rounds);
        TextView note = root.findViewById(R.id.text_slot_note);
        note.setText(slotNote(activity));
        LinearLayout list = root.findViewById(R.id.slot_list);
        for (int index = 0; index < session.slotCount(); index++) {
            boolean taken = store.slotTakenByOther(assignId, index);
            Integer pending = store.pendingSlotIndex();
            boolean selected = pending != null && pending == index && !taken;
            int slotIndex = index;
            View row = slotChoice(activity, store, index, taken, selected);
            row.setOnClickListener(v -> {
                if (taken) {
                    host.showMessage(activity.getString(R.string.pillbox_slot_full));
                    return;
                }
                store.choosePendingSlot(slotIndex);
                host.refresh();
            });
            list.addView(row);
        }
        View without = root.findViewById(R.id.button_slot_without);
        without.setVisibility(View.VISIBLE);
        without.setOnClickListener(v -> {
            if (assignId == null || !store.assignSlot(assignId, null)) {
                host.showMessage(activity.getString(R.string.med_readonly));
                return;
            }
            store.cancelSlotAssign();
            host.popTo(MedScreen.LIST);
        });
        com.google.android.material.button.MaterialButton done = root.findViewById(R.id.button_slot_done);
        done.setText(R.string.med_slot_assign_done);
        done.setOnClickListener(v -> {
            if (store.pendingSlotIndex() == null) {
                host.showMessage(activity.getString(R.string.med_slot_assign_need));
                return;
            }
            if (!store.confirmSlotAssign()) {
                host.showMessage(activity.getString(R.string.pillbox_slot_full));
                return;
            }
            host.popTo(MedScreen.LIST);
            host.showMessage(activity.getString(R.string.med_slot_assigned_local));
        });
        return root;
    }

    private static void pickTimings(AppCompatActivity activity, MedicineDraft medicine, Runnable after) {
        DoseTiming[] values = DoseTiming.values();
        CharSequence[] labels = new CharSequence[values.length];
        boolean[] checked = new boolean[values.length];
        for (int index = 0; index < values.length; index++) {
            labels[index] = timingLabel(activity, values[index]);
            checked[index] = medicine.timings().contains(values[index]);
        }
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setTitle(activity.getString(R.string.med_timing))
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    medicine.timings().clear();
                    for (int index = 0; index < values.length; index++) {
                        if (checked[index]) {
                            medicine.timings().add(values[index]);
                        }
                    }
                    after.run();
                })
                .show();
    }

    private static String timingsText(AppCompatActivity activity, Set<DoseTiming> timings) {
        if (timings == null || timings.isEmpty()) {
            return activity.getString(R.string.med_timing_unset);
        }
        StringBuilder text = new StringBuilder();
        for (DoseTiming timing : DoseTiming.values()) {
            if (!timings.contains(timing)) {
                continue;
            }
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(timingLabel(activity, timing));
        }
        return text.toString();
    }

    private static String timingLabel(AppCompatActivity activity, DoseTiming timing) {
        switch (timing) {
            case BEFORE_MEAL:
                return activity.getString(R.string.med_timing_before);
            case BEDTIME:
                return activity.getString(R.string.med_timing_bed);
            case AFTER_MEAL_30:
            default:
                return activity.getString(R.string.med_timing_after);
        }
    }

    public static View serverList(
            AppCompatActivity activity,
            MainScreens.CareView view,
            String message,
            java.util.List<HomeContracts.Prescription> prescriptions,
            boolean writes,
            Host host
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_list, null);
        TextView summary = root.findViewById(R.id.text_med_summary);
        TextView empty = root.findViewById(R.id.text_med_empty);
        root.findViewById(R.id.text_dev_banner).setVisibility(View.GONE);
        root.findViewById(R.id.button_dev_list).setVisibility(View.GONE);
        root.findViewById(R.id.button_register_med).setOnClickListener(v -> {
            if (!writes) {
                host.showMessage(activity.getString(R.string.med_readonly));
                return;
            }
            YakyeonRepositoryProvider.get().medications().beginDraft();
            host.openScreen(MedScreen.CONSENT);
        });
        if (view != MainScreens.CareView.READY) {
            String text = view == MainScreens.CareView.LOADING
                    ? activity.getString(R.string.home_loading)
                    : (message == null ? activity.getString(R.string.api_response_error) : message);
            summary.setText(text);
            empty.setText(text);
            return root;
        }
        java.util.List<HomeContracts.Prescription> rows = prescriptions == null
                ? java.util.Collections.emptyList()
                : prescriptions;
        int medicines = 0;
        int activeCount = 0;
        for (HomeContracts.Prescription prescription : rows) {
            medicines += prescription.medications.size();
            if (!prescriptionEnded(prescription)) {
                activeCount++;
            }
        }
        summary.setText(activity.getString(R.string.med_list_summary, activeCount, medicines));
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        LinearLayout active = root.findViewById(R.id.med_active);
        LinearLayout stopped = root.findViewById(R.id.med_stopped);
        boolean anyActive = false;
        boolean anyStopped = false;
        for (HomeContracts.Prescription prescription : rows) {
            boolean ended = prescriptionEnded(prescription);
            if (ended) {
                anyStopped = true;
                stopped.addView(serverEnvelope(activity, prescription, true, writes, host));
            } else {
                anyActive = true;
                active.addView(serverEnvelope(activity, prescription, false, writes, host));
            }
        }
        root.findViewById(R.id.text_med_active).setVisibility(anyActive ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.text_med_stopped).setVisibility(anyStopped ? View.VISIBLE : View.GONE);
        return root;
    }

    public static void fillOcr(MedicationStore store, HomeContracts.OcrDraft recognized) {
        if (store == null || store.draft() == null || recognized == null) {
            return;
        }
        EnvelopeDraft draft = store.draft();
        if (recognized.hospitalName != null && !recognized.hospitalName.isEmpty()) {
            draft.setHospitalName(recognized.hospitalName);
        }
        if (recognized.pharmacyName != null) {
            draft.setPharmacyName(recognized.pharmacyName);
        }
        if (recognized.prescribedDate != null) {
            draft.setPrescribedDate(recognized.prescribedDate);
        }
        if (recognized.recognized.isEmpty()) {
            return;
        }
        draft.setFromOcr(true);
        LocalDate start = LocalDate.now(zone());
        if (recognized.prescribedDate != null) {
            try {
                start = LocalDate.parse(recognized.prescribedDate);
            } catch (java.time.format.DateTimeParseException ignored) {
                start = LocalDate.now(zone());
            }
        }
        for (HomeContracts.RecognizedMedication item : recognized.recognized) {
            MedicineDraft medicine = store.addMedicine(start);
            medicine.setName(item.drugName);
            medicine.setDrugId(item.matchedDrugId);
            medicine.setPrn(item.prn);
            DoseAmount amount = DoseAmount.from(DoseAmount.UNIT_TABLET, item.dosePerIntake);
            if (amount != null) {
                medicine.setAmount(amount);
            }
            medicine.setDoseText(item.dosePerIntake);
            if (item.mealRelation != null) {
                medicine.setMealRelation(item.mealRelation, item.mealOffsetMin == null ? null : item.mealOffsetMin.intValue());
            }
            if (item.totalDays >= 1L && item.totalDays <= 365L) {
                medicine.setEnd(start.plusDays(item.totalDays - 1L));
            }
        }
    }

    public static Dialog serverAlarmDialog(
            AppCompatActivity activity,
            String hospital,
            java.util.Map<DoseRound, Integer> minutes,
            java.util.Set<DoseRound> selected,
            Runnable onSave
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_med_alarm, null);
        TextView title = root.findViewById(R.id.text_alarm_title);
        if (hospital == null || hospital.trim().isEmpty()) {
            title.setText(activity.getString(R.string.med_alarm_heading_plain));
        } else {
            title.setText(activity.getString(R.string.med_alarm_heading, hospital.trim()));
        }
        LinearLayout rounds = root.findViewById(R.id.alarm_rounds);
        TextView empty = root.findViewById(R.id.text_alarm_empty);
        int roundCount = 0;
        for (DoseRound round : DoseRound.values()) {
            if (!selected.contains(round)) {
                continue;
            }
            roundCount++;
            rounds.addView(alarmRound(activity, minutes, selected, round, root));
        }
        boolean any = roundCount > 0;
        empty.setVisibility(any ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.alarm_scroll).setVisibility(any ? View.VISIBLE : View.GONE);
        Dialog dialog = new Dialog(activity, R.style.ThemeOverlay_Yakyeon_AlarmCard);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.setCancelable(true);
        root.findViewById(R.id.button_alarm_save).setOnClickListener(v -> {
            if (!any) {
                return;
            }
            if (MedicationRules.checkAlarmTimes(selected, minutes) != MedicationRules.TimeCheck.OK) {
                ShortMessage.show(root, activity.getString(R.string.med_time_rule));
                return;
            }
            dialog.dismiss();
            onSave.run();
        });
        dialog.setOnShowListener(shown -> fitAlarmCard(activity, dialog, root));
        return dialog;
    }

    private static boolean canConfirm(MedicineDraft medicine) {
        if (medicine != null && medicine.isPrn()) {
            String name = medicine.getName();
            return name != null
                    && !name.trim().isEmpty()
                    && medicine.getStart() != null
                    && medicine.getEnd() != null
                    && !medicine.getEnd().isBefore(medicine.getStart());
        }
        return MedicationRules.hasRequiredFields(medicine);
    }

    private static boolean prescriptionEnded(HomeContracts.Prescription prescription) {
        if (prescription.medications.isEmpty()) {
            return false;
        }
        for (HomeContracts.Medication medication : prescription.medications) {
            if (!HomeContracts.STATUS_ENDED.equals(medication.status)) {
                return false;
            }
        }
        return true;
    }

    private static View serverEnvelope(
            AppCompatActivity activity,
            HomeContracts.Prescription prescription,
            boolean ended,
            boolean writes,
            Host host
    ) {
        View card = LayoutInflater.from(activity).inflate(R.layout.item_med_envelope, null);
        String name = prescription.hospitalName == null || prescription.hospitalName.trim().isEmpty()
                ? activity.getString(R.string.med_slot_empty_name)
                : prescription.hospitalName;
        ((TextView) card.findViewById(R.id.text_envelope_name)).setText(name);
        ((TextView) card.findViewById(R.id.text_envelope_count)).setText(
                activity.getString(R.string.med_bundle_count, prescription.medications.size())
        );
        TextView badge = card.findViewById(R.id.text_envelope_badge);
        View actions = card.findViewById(R.id.row_envelope_actions);
        View stop = card.findViewById(R.id.button_envelope_stop);
        if (ended || !writes) {
            if (ended) {
                badge.setText(R.string.med_stopped);
                badge.setTextColor(activity.getColor(R.color.yakyeon_muted));
                badge.setBackgroundResource(R.drawable.bg_med_ended_badge);
            } else {
                badge.setVisibility(View.GONE);
            }
            actions.setVisibility(View.GONE);
            stop.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.GONE);
            card.findViewById(R.id.button_edit_time).setOnClickListener(v ->
                    host.openServerSchedule(prescription.prescriptionId));
            card.findViewById(R.id.button_change_slot).setOnClickListener(v ->
                    host.showMessage(activity.getString(R.string.med_slot_after_register)));
            stop.setOnClickListener(v -> ConfirmDialog.show(
                    activity,
                    activity.getString(R.string.med_stop_title),
                    activity.getString(R.string.med_stop_message),
                    activity.getString(R.string.med_stop_confirm),
                    activity.getString(R.string.action_cancel),
                    new ConfirmDialog.Callback() {
                        @Override
                        public void onConfirm() {
                            host.endServerPrescription(prescription.prescriptionId);
                        }

                        @Override
                        public void onCancel() {
                        }
                    },
                    ConfirmDialog.Mark.CHECK,
                    ConfirmDialog.Tone.PRIMARY
            ));
        }
        LinearLayout medicines = card.findViewById(R.id.envelope_medicines);
        for (HomeContracts.Medication medication : prescription.medications) {
            View row = LayoutInflater.from(activity).inflate(R.layout.item_med_list_medicine, medicines, false);
            ((TextView) row.findViewById(R.id.text_medicine_name)).setText(displayName(activity, medication.drugName));
            int times = medication.prn
                    ? medication.timingSlots.size()
                    : (int) medication.timesPerDay;
            ((TextView) row.findViewById(R.id.text_medicine_frequency)).setText(
                    activity.getString(R.string.med_frequency, times)
            );
            if (!ended && writes && !HomeContracts.STATUS_ENDED.equals(medication.status)) {
                row.setOnClickListener(v -> ConfirmDialog.show(
                        activity,
                        activity.getString(R.string.med_end_one_title),
                        activity.getString(R.string.med_end_one_message),
                        activity.getString(R.string.med_stop_confirm),
                        activity.getString(R.string.action_cancel),
                        new ConfirmDialog.Callback() {
                            @Override
                            public void onConfirm() {
                                host.endServerMedication(medication.medicationId);
                            }

                            @Override
                            public void onCancel() {
                            }
                        },
                        ConfirmDialog.Mark.CHECK,
                        ConfirmDialog.Tone.PRIMARY
                ));
                row.setOnLongClickListener(v -> {
                    ConfirmDialog.show(
                            activity,
                            activity.getString(R.string.med_delete_title),
                            activity.getString(R.string.med_delete_server_message),
                            activity.getString(R.string.med_delete),
                            activity.getString(R.string.action_cancel),
                            new ConfirmDialog.Callback() {
                                @Override
                                public void onConfirm() {
                                    host.deleteServerMedication(medication.medicationId);
                                }

                                @Override
                                public void onCancel() {
                                }
                            },
                            ConfirmDialog.Mark.TRASH,
                            ConfirmDialog.Tone.DANGER
                    );
                    return true;
                });
            }
            medicines.addView(row);
        }
        return card;
    }

    private static ZoneId zone() {
        return ZoneId.systemDefault();
    }
}
