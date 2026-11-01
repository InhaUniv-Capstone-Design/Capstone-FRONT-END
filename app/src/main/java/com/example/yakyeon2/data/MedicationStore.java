package com.example.yakyeon2.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class MedicationStore {

    public static final String GRAPH_CHECK_HOSPITAL = "그래프 검증";

    private final YakyeonSession session;
    private final List<SavedEnvelope> envelopes = new ArrayList<>();
    private final List<DoseRecord> records = new ArrayList<>();
    private EnvelopeDraft draft;
    private int editingIndex = -1;
    private String editingEnvelopeId;
    private String editingMedicineId;
    private String alarmEditEnvelopeId;
    private final Map<DoseRound, Integer> alarmEditMinutes = new EnumMap<>(DoseRound.class);
    private String slotAssignEnvelopeId;
    private Integer pendingSlotIndex;
    private HistoryFilter historyFilter = HistoryFilter.ALL;
    private LocalDate historyStart;
    private LocalDate historyEnd;
    private LocalDate scheduleDate;
    private int sequence;
    private final List<String> devSampleEnvelopeIds = new ArrayList<>();
    private boolean fixturePillbox;
    private boolean devGraphRelationScenario;
    private Runnable afterChange;

    public MedicationStore(YakyeonSession session) {
        this.session = session;
    }

    public void setAfterChange(Runnable afterChange) {
        this.afterChange = afterChange;
    }

    public EnvelopeDraft beginDraft() {
        draft = new EnvelopeDraft();
        editingIndex = -1;
        return draft;
    }

    public EnvelopeDraft draft() {
        return draft;
    }

    public List<SavedEnvelope> envelopes() {
        return Collections.unmodifiableList(envelopes);
    }

    public List<DoseRecord> records() {
        return Collections.unmodifiableList(records);
    }

    public int activeMedicineCount() {
        int count = 0;
        for (SavedEnvelope envelope : envelopes) {
            if (!envelope.isStopped()) {
                count += envelope.medicines().size();
            }
        }
        return count;
    }

    public int activeEnvelopeCount() {
        int count = 0;
        for (SavedEnvelope envelope : envelopes) {
            if (!envelope.isStopped()) {
                count++;
            }
        }
        return count;
    }

    public MedicineDraft addMedicine(LocalDate registrationDate) {
        if (draft == null) {
            beginDraft();
        }
        MedicineDraft medicine = new MedicineDraft();
        medicine.setStart(registrationDate);
        draft.medicines().add(medicine);
        editingIndex = draft.medicines().size() - 1;
        editingEnvelopeId = null;
        editingMedicineId = null;
        return medicine;
    }

    public void editDraftAt(int index) {
        editingIndex = index;
        editingEnvelopeId = null;
        editingMedicineId = null;
    }

    public void collapseEditing() {
        editingIndex = -1;
    }

    public int editingIndex() {
        return editingIndex;
    }

    public MedicineDraft editingDraft() {
        if (draft == null || editingIndex < 0 || editingIndex >= draft.medicines().size()) {
            return null;
        }
        return draft.medicines().get(editingIndex);
    }

    public void editSaved(String envelopeId, String medicineId) {
        editingEnvelopeId = envelopeId;
        editingMedicineId = medicineId;
        editingIndex = -1;
    }

    public SavedMedicine editingSaved() {
        SavedEnvelope envelope = envelope(editingEnvelopeId);
        if (envelope == null) {
            return null;
        }
        for (SavedMedicine medicine : envelope.medicines()) {
            if (medicine.getId().equals(editingMedicineId)) {
                return medicine;
            }
        }
        return null;
    }

    public SavedEnvelope editingEnvelope() {
        return envelope(editingEnvelopeId);
    }

    public void abandonDraft() {
        draft = null;
        editingIndex = -1;
    }

    public boolean deleteEditingDraft() {
        if (draft == null || editingIndex < 0 || editingIndex >= draft.medicines().size()) {
            return false;
        }
        draft.medicines().remove(editingIndex);
        editingIndex = -1;
        return true;
    }

    public boolean deleteEditingSaved() {
        SavedEnvelope envelope = editingEnvelope();
        SavedMedicine medicine = editingSaved();
        if (envelope == null || medicine == null) {
            return false;
        }
        envelope.medicines().remove(medicine);
        records.removeIf(record -> medicine.getId().equals(record.getMedicineId()));
        if (envelope.medicines().isEmpty()) {
            envelopes.remove(envelope);
        }
        syncSession();
        return true;
    }

    public boolean stopEnvelope(String envelopeId, long now) {
        SavedEnvelope envelope = envelope(envelopeId);
        if (envelope == null || envelope.isStopped()) {
            return false;
        }
        envelope.setStopped(true);
        records.removeIf(record -> envelopeId.equals(record.getEnvelopeId())
                && record.getManualStatus() == null
                && record.getScheduledAtMillis() > now);
        syncSession();
        return true;
    }

    public MedicationRules.SaveBlock commit(ZoneId zone) {
        return commit(zone, System.currentTimeMillis());
    }

    public MedicationRules.SaveBlock commit(ZoneId zone, long registeredAtMillis) {
        if (draft != null && draft.isDevSample()) {
            return MedicationRules.SaveBlock.MISSING_REQUIRED;
        }
        MedicationRules.SaveBlock block = MedicationRules.validate(draft);
        if (block != MedicationRules.SaveBlock.OK) {
            return block;
        }
        String envelopeId = "env-" + nextId();
        SavedEnvelope saved = new SavedEnvelope(
                envelopeId,
                draft.getHospitalName(),
                draft.getSlotIndex(),
                draft.alarmMinutes()
        );
        saved.setRegisteredAtMillis(registeredAtMillis);
        for (MedicineDraft medicine : draft.medicines()) {
            SavedMedicine stored = new SavedMedicine("med-" + nextId(), medicine);
            saved.medicines().add(stored);
            addExpanded(saved, stored, zone);
        }
        envelopes.add(saved);
        if (draft.getSlotIndex() != null) {
            session.setSlotOccupied(draft.getSlotIndex(), true);
        }
        draft = null;
        editingIndex = -1;
        syncSession();
        return MedicationRules.SaveBlock.OK;
    }

    public void mergeExpanded(ZoneId zone) {
        for (SavedEnvelope envelope : envelopes) {
            for (SavedMedicine medicine : envelope.medicines()) {
                addExpanded(envelope, medicine, zone);
            }
        }
    }

    public boolean recordManual(DoseRecord record, long now, long takenAtMillis) {
        if (!MedicationRules.recordManual(record, now, takenAtMillis)) {
            return false;
        }
        syncSession();
        return true;
    }

    /**
     * Marks the next still-upcoming round in this pillbox slot as taken.
     * Only a dose whose scheduled time is still ahead of {@code takenAtMillis} changes.
     *
     * @return how many medicine records were completed
     */
    public int recordEarlyHardwareIntake(int slotIndex, long takenAtMillis) {
        SavedEnvelope target = null;
        for (SavedEnvelope envelope : envelopes) {
            if (envelope.isStopped()) {
                continue;
            }
            Integer slot = envelope.getSlotIndex();
            if (slot != null && slot == slotIndex) {
                target = envelope;
                break;
            }
        }
        if (target == null) {
            return 0;
        }
        long nextAt = Long.MAX_VALUE;
        DoseRound nextRound = null;
        for (DoseRecord record : records) {
            if (!target.getId().equals(record.getEnvelopeId()) || record.getManualStatus() != null) {
                continue;
            }
            long scheduled = record.getScheduledAtMillis();
            if (scheduled <= takenAtMillis || scheduled >= nextAt) {
                continue;
            }
            nextAt = scheduled;
            nextRound = record.getRound();
        }
        if (nextRound == null) {
            return 0;
        }
        int marked = 0;
        for (DoseRecord record : records) {
            if (!target.getId().equals(record.getEnvelopeId())
                    || record.getRound() != nextRound
                    || record.getScheduledAtMillis() != nextAt) {
                continue;
            }
            if (MedicationRules.recordHardwareEarly(record, takenAtMillis)) {
                marked++;
            }
        }
        if (marked > 0) {
            syncSession();
        }
        return marked;
    }

    public boolean applySavedEdit(MedicineDraft edited, ZoneId zone) {
        SavedMedicine medicine = editingSaved();
        SavedEnvelope envelope = editingEnvelope();
        if (medicine == null || envelope == null || !MedicationRules.hasRequiredFields(edited)) {
            return false;
        }
        if (!edited.isConfirmed()) {
            return false;
        }
        MedicationRules.TimeCheck times = MedicationRules.checkAlarmTimes(
                edited.rounds(),
                envelope.alarmMinutes()
        );
        if (times != MedicationRules.TimeCheck.OK) {
            return false;
        }
        String medicineId = medicine.getId();
        medicine.apply(edited);
        records.removeIf(record -> medicineId.equals(record.getMedicineId()) && record.getManualStatus() == null);
        addExpanded(envelope, medicine, zone);
        syncSession();
        return true;
    }

    public boolean beginAlarmEdit(String envelopeId) {
        SavedEnvelope envelope = envelope(envelopeId);
        if (envelope == null || envelope.isStopped()) {
            return false;
        }
        alarmEditEnvelopeId = envelopeId;
        alarmEditMinutes.clear();
        alarmEditMinutes.putAll(envelope.alarmMinutes());
        return true;
    }

    public boolean isAlarmEdit() {
        return alarmEditEnvelopeId != null;
    }

    public Map<DoseRound, Integer> alarmEditMinutes() {
        return alarmEditMinutes;
    }

    public SavedEnvelope alarmEditEnvelope() {
        return envelope(alarmEditEnvelopeId);
    }

    public void cancelAlarmEdit() {
        alarmEditEnvelopeId = null;
        alarmEditMinutes.clear();
    }

    public boolean saveAlarmEdit(ZoneId zone, long now) {
        if (alarmEditEnvelopeId == null || zone == null) {
            return false;
        }
        if (!updateAlarmMinutes(alarmEditEnvelopeId, alarmEditMinutes, zone, now)) {
            return false;
        }
        cancelAlarmEdit();
        return true;
    }

    public boolean updateAlarmMinutes(
            String envelopeId,
            Map<DoseRound, Integer> minutes,
            ZoneId zone,
            long now
    ) {
        SavedEnvelope envelope = envelope(envelopeId);
        if (envelope == null || minutes == null || zone == null) {
            return false;
        }
        EnumSet<DoseRound> required = EnumSet.noneOf(DoseRound.class);
        for (SavedMedicine medicine : envelope.medicines()) {
            required.addAll(medicine.rounds());
        }
        if (MedicationRules.checkAlarmTimes(required, minutes) != MedicationRules.TimeCheck.OK) {
            return false;
        }
        envelope.replaceAlarmMinutes(minutes);
        records.removeIf(record -> envelopeId.equals(record.getEnvelopeId())
                && record.getManualStatus() == null
                && MedicationRules.displayStatus(record, now) == DoseStatus.SCHEDULED);
        for (SavedMedicine medicine : envelope.medicines()) {
            addExpanded(envelope, medicine, zone);
        }
        syncSession();
        return true;
    }

    public boolean beginSlotAssign(String envelopeId) {
        SavedEnvelope envelope = envelope(envelopeId);
        if (envelope == null || envelope.isStopped()) {
            return false;
        }
        slotAssignEnvelopeId = envelopeId;
        pendingSlotIndex = envelope.getSlotIndex();
        return true;
    }

    public void cancelSlotAssign() {
        slotAssignEnvelopeId = null;
        pendingSlotIndex = null;
    }

    public String slotAssignEnvelopeId() {
        return slotAssignEnvelopeId;
    }

    public Integer pendingSlotIndex() {
        return pendingSlotIndex;
    }

    public void choosePendingSlot(int index) {
        pendingSlotIndex = index;
    }

    public boolean slotTakenByOther(String envelopeId, int index) {
        for (SavedEnvelope other : envelopes) {
            if (other.isStopped() || other.getId().equals(envelopeId)) {
                continue;
            }
            Integer slot = other.getSlotIndex();
            if (slot != null && slot == index) {
                return true;
            }
        }
        return false;
    }

    public boolean confirmSlotAssign() {
        if (slotAssignEnvelopeId == null || pendingSlotIndex == null) {
            return false;
        }
        if (!assignSlot(slotAssignEnvelopeId, pendingSlotIndex)) {
            return false;
        }
        cancelSlotAssign();
        return true;
    }

    public boolean assignSlot(String envelopeId, Integer slotIndex) {
        SavedEnvelope envelope = envelope(envelopeId);
        if (envelope == null || envelope.isStopped()) {
            return false;
        }
        if (slotIndex != null) {
            if (slotIndex < 0 || slotIndex >= session.slotCount()) {
                return false;
            }
            if (slotTakenByOther(envelopeId, slotIndex)) {
                return false;
            }
        }
        envelope.setSlotIndex(slotIndex);
        syncSession();
        return true;
    }

    public boolean removeEnvelope(String envelopeId) {
        SavedEnvelope envelope = envelope(envelopeId);
        if (envelope == null) {
            return false;
        }
        envelopes.remove(envelope);
        records.removeIf(record -> envelopeId.equals(record.getEnvelopeId()));
        if (envelopeId.equals(slotAssignEnvelopeId)) {
            cancelSlotAssign();
        }
        if (envelopeId.equals(alarmEditEnvelopeId)) {
            cancelAlarmEdit();
        }
        syncSession();
        return true;
    }

    public HistoryFilter historyFilter() {
        return historyFilter;
    }

    public void setHistoryFilter(HistoryFilter historyFilter) {
        this.historyFilter = historyFilter == null ? HistoryFilter.ALL : historyFilter;
    }

    public LocalDate historyStart(LocalDate today) {
        return historyStart == null ? MedicationRules.defaultHistoryStart(today) : historyStart;
    }

    public LocalDate historyEnd(LocalDate today) {
        return historyEnd == null ? today : historyEnd;
    }

    public boolean setHistoryRange(LocalDate start, LocalDate end) {
        if (!MedicationRules.rangeAllowed(start, end)) {
            return false;
        }
        historyStart = start;
        historyEnd = end;
        return true;
    }

    public LocalDate scheduleDate(LocalDate today) {
        if (scheduleDate == null || !MedicationRules.sameWeek(scheduleDate, today)) {
            return today;
        }
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate date) {
        scheduleDate = date;
    }

    public boolean hasDevListSample() {
        return !devSampleEnvelopeIds.isEmpty();
    }

    public boolean loadDevListSample(ZoneId zone) {
        if (!envelopes.isEmpty() || !devSampleEnvelopeIds.isEmpty()) {
            return false;
        }
        LocalDate today = LocalDate.now(zone);
        Map<DoseRound, Integer> alarms = new EnumMap<>(DoseRound.class);
        alarms.put(DoseRound.MORNING, 8 * 60);
        alarms.put(DoseRound.LUNCH, 13 * 60);
        alarms.put(DoseRound.EVENING, 18 * 60);
        alarms.put(DoseRound.BEDTIME, 22 * 60);
        String envelopeId = "dev-env-" + nextId();
        SavedEnvelope saved = new SavedEnvelope(envelopeId, "개발용 예시", null, alarms);
        saved.medicines().add(devMedicine("개발용 예시 1", EnumSet.of(
                DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING
        ), today));
        saved.medicines().add(devMedicine("개발용 예시 2", EnumSet.of(DoseRound.MORNING), today));
        envelopes.add(saved);
        for (SavedMedicine medicine : saved.medicines()) {
            records.addAll(MedicationRules.expand(saved, medicine, zone));
        }
        devSampleEnvelopeIds.add(envelopeId);
        syncSession();
        return true;
    }

    public boolean clearDevListSample() {
        if (devSampleEnvelopeIds.isEmpty()) {
            return false;
        }
        envelopes.removeIf(envelope -> devSampleEnvelopeIds.contains(envelope.getId()));
        records.removeIf(record -> devSampleEnvelopeIds.contains(record.getEnvelopeId()));
        devSampleEnvelopeIds.clear();
        syncSession();
        return true;
    }

    public boolean loadDevEnvelopeSample(LocalDate today) {
        if (draft == null || !draft.medicines().isEmpty() || draft.isDevSample()) {
            return false;
        }
        draft.setDevSample(true);
        draft.setHospitalName("개발용 예시");
        draft.medicines().add(devDraft("개발용 예시 1", EnumSet.of(
                DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING
        ), today, false));
        draft.medicines().add(devDraft("개발용 예시 2", EnumSet.of(
                DoseRound.MORNING, DoseRound.LUNCH, DoseRound.EVENING
        ), today, true));
        draft.medicines().add(devDraft("개발용 예시 3", EnumSet.of(DoseRound.BEDTIME), today, false));
        editingIndex = 1;
        return true;
    }

    private SavedMedicine devMedicine(String name, EnumSet<DoseRound> rounds, LocalDate today) {
        return new SavedMedicine("dev-med-" + nextId(), devDraft(name, rounds, today, true));
    }

    private static MedicineDraft devDraft(
            String name,
            EnumSet<DoseRound> rounds,
            LocalDate today,
            boolean filled
    ) {
        MedicineDraft medicine = new MedicineDraft();
        medicine.setName(name);
        medicine.rounds().addAll(rounds);
        medicine.setStart(today);
        medicine.setEnd(today.plusDays(7));
        if (filled) {
            medicine.setAmount(DoseAmount.TABLET_1);
            medicine.setTiming(DoseTiming.AFTER_MEAL_30);
        }
        return medicine;
    }

    public DoseRecord record(String id) {
        for (DoseRecord record : records) {
            if (record.getId().equals(id)) {
                return record;
            }
        }
        return null;
    }

    private SavedEnvelope envelope(String id) {
        if (id == null) {
            return null;
        }
        for (SavedEnvelope envelope : envelopes) {
            if (envelope.getId().equals(id)) {
                return envelope;
            }
        }
        return null;
    }

    private void syncSession() {
        recomputeSlots();
        List<String> ids = new ArrayList<>();
        for (SavedEnvelope envelope : envelopes) {
            if (envelope.isStopped()) {
                continue;
            }
            for (SavedMedicine medicine : envelope.medicines()) {
                ids.add(medicine.getId());
            }
        }
        session.replaceMedicationIds(ids);
        if (afterChange != null) {
            afterChange.run();
        }
    }

    private void recomputeSlots() {
        for (int index = 0; index < session.slotCount(); index++) {
            session.setSlotOccupied(index, false);
        }
        for (SavedEnvelope envelope : envelopes) {
            if (!envelope.isStopped() && envelope.getSlotIndex() != null) {
                session.setSlotOccupied(envelope.getSlotIndex(), true);
            }
        }
    }

    private void addExpanded(SavedEnvelope envelope, SavedMedicine medicine, ZoneId zone) {
        for (DoseRecord candidate : MedicationRules.expand(envelope, medicine, zone)) {
            if (record(candidate.getId()) == null) {
                records.add(candidate);
            }
        }
    }

    public static boolean isSampleId(String id) {
        return id != null && (id.startsWith("fixture-") || id.startsWith("dev-env-"));
    }

    public boolean isSampleEnvelope(String envelopeId) {
        return skipSample(envelopeId);
    }

    public String allocateId(String prefix) {
        return prefix + nextId();
    }

    public void rememberSample(SavedEnvelope envelope, ZoneId zone) {
        envelopes.add(envelope);
        devSampleEnvelopeIds.add(envelope.getId());
        for (SavedMedicine medicine : envelope.medicines()) {
            addExpanded(envelope, medicine, zone);
        }
        syncSession();
    }

    public boolean hasFixture() {
        for (SavedEnvelope envelope : envelopes) {
            if (envelope.getId().startsWith("fixture-")) {
                return true;
            }
        }
        return draft != null && draft.isFixturePractice();
    }

    public void clearFixtures() {
        envelopes.removeIf(envelope -> envelope.getId().startsWith("fixture-"));
        records.removeIf(record -> record.getEnvelopeId().startsWith("fixture-"));
        devSampleEnvelopeIds.removeIf(id -> id.startsWith("fixture-"));
        if (draft != null && draft.isFixturePractice()) {
            draft = null;
            editingIndex = -1;
        }
        clearFixturePillbox();
        syncSession();
    }

    public void markFixturePillbox() {
        fixturePillbox = true;
        session.setPillboxConnected(true);
    }

    public void clearFixturePillbox() {
        if (fixturePillbox) {
            session.setPillboxConnected(false);
            fixturePillbox = false;
        }
    }

    public boolean isDevGraphRelationScenario() {
        return devGraphRelationScenario;
    }

    public void setDevGraphRelationScenario(boolean enabled) {
        devGraphRelationScenario = enabled;
    }

    public boolean isGraphCheck(SavedEnvelope envelope) {
        return envelope != null && GRAPH_CHECK_HOSPITAL.equals(envelope.getHospitalName());
    }

    /**
     * Replaces only envelopes saved for the debug graph check.
     * They go through {@link #commit} so the graph reads them as taking medicines.
     * The in-progress registration draft is put back afterwards.
     * The dev relation flag is turned off and is not written by export.
     */
    public boolean installGraphChecks(int count, ZoneId zone, long registeredAtMillis) {
        if (count < 0 || zone == null) {
            return false;
        }
        EnvelopeDraft keptDraft = draft;
        int keptIndex = editingIndex;
        String keptEnvelopeId = editingEnvelopeId;
        String keptMedicineId = editingMedicineId;
        draft = null;
        editingIndex = -1;
        clearGraphChecks();
        devGraphRelationScenario = false;
        LocalDate day = Instant.ofEpochMilli(registeredAtMillis).atZone(zone).toLocalDate();
        for (int index = 1; index <= count; index++) {
            draft = new EnvelopeDraft();
            editingIndex = -1;
            draft.setHospitalName(GRAPH_CHECK_HOSPITAL);
            draft.alarmMinutes().put(DoseRound.MORNING, 8 * 60);
            draft.alarmMinutes().put(DoseRound.LUNCH, 13 * 60);
            draft.alarmMinutes().put(DoseRound.EVENING, 18 * 60);
            MedicineDraft medicine = addMedicine(day);
            medicine.setName("그래프 검증 " + index);
            medicine.setAmount(DoseAmount.TABLET_1);
            medicine.setTiming(DoseTiming.AFTER_MEAL_30);
            medicine.rounds().add(DoseRound.MORNING);
            medicine.rounds().add(DoseRound.LUNCH);
            medicine.rounds().add(DoseRound.EVENING);
            medicine.setEnd(day);
            medicine.setConfirmed(true);
            if (commit(zone, registeredAtMillis) != MedicationRules.SaveBlock.OK) {
                restoreEditing(keptDraft, keptIndex, keptEnvelopeId, keptMedicineId);
                return false;
            }
        }
        restoreEditing(keptDraft, keptIndex, keptEnvelopeId, keptMedicineId);
        return true;
    }

    public void clearGraphChecks() {
        List<String> ids = new ArrayList<>();
        for (SavedEnvelope envelope : envelopes) {
            if (isGraphCheck(envelope)) {
                ids.add(envelope.getId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        envelopes.removeIf(envelope -> ids.contains(envelope.getId()));
        records.removeIf(record -> ids.contains(record.getEnvelopeId()));
        syncSession();
    }

    private void restoreEditing(
            EnvelopeDraft keptDraft,
            int keptIndex,
            String keptEnvelopeId,
            String keptMedicineId
    ) {
        draft = keptDraft;
        editingIndex = keptIndex;
        editingEnvelopeId = keptEnvelopeId;
        editingMedicineId = keptMedicineId;
    }

    public int freeSlotCount() {
        return session.slotCount() - session.occupiedSlotCount();
    }

    public int sessionSlotCount() {
        return session.slotCount();
    }

    public boolean sessionSlotOccupied(int index) {
        return session.isSlotOccupied(index);
    }

    public boolean pillboxConnected() {
        return session.isPillboxConnected();
    }

    /**
     * Marks the in-app pillbox as connected. This is not a Wi-Fi join and is not
     * cleared when debug fixtures are reset.
     */
    public void connectPillbox() {
        session.setPillboxConnected(true);
    }

    public void disconnectPillbox() {
        session.setPillboxConnected(false);
    }

    /** Drops medicines, records, drafts, and the local pillbox flag for the signed-in account. */
    public void discardAccountData() {
        envelopes.clear();
        records.clear();
        draft = null;
        editingIndex = -1;
        editingEnvelopeId = null;
        editingMedicineId = null;
        alarmEditEnvelopeId = null;
        alarmEditMinutes.clear();
        slotAssignEnvelopeId = null;
        pendingSlotIndex = null;
        historyFilter = HistoryFilter.ALL;
        historyStart = null;
        historyEnd = null;
        scheduleDate = null;
        sequence = 0;
        devSampleEnvelopeIds.clear();
        fixturePillbox = false;
        devGraphRelationScenario = false;
        session.setPillboxConnected(false);
        session.setCareLink(YakyeonSession.CareLink.NONE);
        syncSession();
    }

    public String exportState() {
        List<SavedEnvelope> keptEnvelopes = new ArrayList<>();
        List<DoseRecord> keptRecords = new ArrayList<>();
        for (SavedEnvelope envelope : envelopes) {
            if (skipSample(envelope.getId())) {
                continue;
            }
            keptEnvelopes.add(envelope);
        }
        for (DoseRecord record : records) {
            if (skipSample(record.getEnvelopeId())) {
                continue;
            }
            keptRecords.add(record);
        }
        return MedicationStateCodec.encode(keptEnvelopes, keptRecords, Collections.<String>emptyList(), sequence);
    }

    public void importState(String encoded) {
        MedicationStateCodec.Decoded decoded = MedicationStateCodec.decode(encoded);
        envelopes.clear();
        records.clear();
        devSampleEnvelopeIds.clear();
        for (SavedEnvelope envelope : decoded.envelopes) {
            if (skipSample(envelope.getId()) || decoded.devSampleEnvelopeIds.contains(envelope.getId())) {
                continue;
            }
            envelopes.add(envelope);
        }
        for (DoseRecord record : decoded.records) {
            if (skipSample(record.getEnvelopeId()) || decoded.devSampleEnvelopeIds.contains(record.getEnvelopeId())) {
                continue;
            }
            records.add(record);
        }
        sequence = decoded.sequence;
        recomputeSlots();
        List<String> ids = new ArrayList<>();
        for (SavedEnvelope envelope : envelopes) {
            if (envelope.isStopped()) {
                continue;
            }
            for (SavedMedicine medicine : envelope.medicines()) {
                ids.add(medicine.getId());
            }
        }
        session.replaceMedicationIds(ids);
    }

    private boolean skipSample(String id) {
        return isSampleId(id) || devSampleEnvelopeIds.contains(id);
    }

    private int nextId() {
        sequence++;
        return sequence;
    }
}
