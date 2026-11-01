package com.example.yakyeon2.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MedicationStateCodec {

    public static final class Decoded {
        public final List<SavedEnvelope> envelopes = new ArrayList<>();
        public final List<DoseRecord> records = new ArrayList<>();
        public final List<String> devSampleEnvelopeIds = new ArrayList<>();
        public int sequence;
    }

    private static final char SEP = '\u001f';

    private MedicationStateCodec() {
    }

    public static String encode(
            List<SavedEnvelope> envelopes,
            List<DoseRecord> records,
            List<String> devSampleEnvelopeIds,
            int sequence
    ) {
        StringBuilder out = new StringBuilder();
        out.append('S').append(SEP).append(sequence).append('\n');
        for (String devId : devSampleEnvelopeIds) {
            out.append('D').append(SEP).append(escape(devId)).append('\n');
        }
        for (SavedEnvelope envelope : envelopes) {
            out.append('E').append(SEP).append(escape(envelope.getId()))
                    .append(SEP).append(escape(envelope.getHospitalName()))
                    .append(SEP).append(envelope.getSlotIndex() == null ? "" : envelope.getSlotIndex())
                    .append(SEP).append(envelope.isStopped() ? "1" : "0")
                    .append(SEP).append(envelope.getRegisteredAtMillis());
            for (DoseRound round : DoseRound.values()) {
                Integer minute = envelope.alarmMinutes().get(round);
                out.append(SEP).append(minute == null ? "" : minute);
            }
            out.append('\n');
            for (SavedMedicine medicine : envelope.medicines()) {
                out.append('M').append(SEP).append(escape(envelope.getId()))
                        .append(SEP).append(escape(medicine.getId()))
                        .append(SEP).append(escape(medicine.getName()))
                        .append(SEP).append(medicine.getAmount() == null ? "" : medicine.getAmount().name())
                        .append(SEP).append(rounds(medicine))
                        .append(SEP).append(timings(medicine))
                        .append(SEP).append(medicine.getStart() == null ? "" : medicine.getStart())
                        .append(SEP).append(medicine.getEnd() == null ? "" : medicine.getEnd())
                        .append('\n');
            }
        }
        for (DoseRecord record : records) {
            out.append('R').append(SEP).append(escape(record.getId()))
                    .append(SEP).append(escape(record.getEnvelopeId()))
                    .append(SEP).append(escape(record.getMedicineId()))
                    .append(SEP).append(escape(record.getEnvelopeName()))
                    .append(SEP).append(escape(record.getMedicineName()))
                    .append(SEP).append(record.getRound().name())
                    .append(SEP).append(record.getScheduledAtMillis())
                    .append(SEP).append(record.getManualStatus() == null ? "" : record.getManualStatus().name())
                    .append(SEP).append(record.getTakenAtMillis() == null ? "" : record.getTakenAtMillis())
                    .append('\n');
        }
        return out.toString();
    }

    public static Decoded decode(String encoded) {
        Decoded decoded = new Decoded();
        if (encoded == null || encoded.isEmpty()) {
            return decoded;
        }
        Map<String, SavedEnvelope> byId = new java.util.LinkedHashMap<>();
        for (String line : encoded.split("\n", -1)) {
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = split(line);
            try {
                if (parts[0].equals("S") && parts.length >= 2) {
                    decoded.sequence = Integer.parseInt(parts[1]);
                } else if (parts[0].equals("D") && parts.length >= 2) {
                    decoded.devSampleEnvelopeIds.add(unescape(parts[1]));
                } else if (parts[0].equals("E") && parts.length >= 10) {
                    Integer slot = parts[3].isEmpty() ? null : Integer.valueOf(parts[3]);
                    Map<DoseRound, Integer> alarms = new EnumMap<>(DoseRound.class);
                    DoseRound[] rounds = DoseRound.values();
                    for (int index = 0; index < rounds.length && 6 + index < parts.length; index++) {
                        if (!parts[6 + index].isEmpty()) {
                            alarms.put(rounds[index], Integer.valueOf(parts[6 + index]));
                        }
                    }
                    SavedEnvelope envelope = new SavedEnvelope(
                            unescape(parts[1]),
                            unescape(parts[2]),
                            slot,
                            alarms
                    );
                    envelope.setStopped("1".equals(parts[4]));
                    envelope.setRegisteredAtMillis(Long.parseLong(parts[5]));
                    byId.put(envelope.getId(), envelope);
                    decoded.envelopes.add(envelope);
                } else if (parts[0].equals("M") && parts.length >= 9) {
                    SavedEnvelope envelope = byId.get(unescape(parts[1]));
                    if (envelope == null) {
                        continue;
                    }
                    MedicineDraft draft = new MedicineDraft();
                    draft.setName(unescape(parts[3]));
                    if (!parts[4].isEmpty()) {
                        draft.setAmount(DoseAmount.valueOf(parts[4]));
                    }
                    if (!parts[5].isEmpty()) {
                        for (String name : parts[5].split(",")) {
                            if (!name.isEmpty()) {
                                draft.rounds().add(DoseRound.valueOf(name));
                            }
                        }
                    }
                    if (!parts[6].isEmpty()) {
                        for (String name : parts[6].split(",")) {
                            if (!name.isEmpty()) {
                                draft.timings().add(DoseTiming.valueOf(name));
                            }
                        }
                    }
                    if (!parts[7].isEmpty()) {
                        draft.setStart(LocalDate.parse(parts[7]));
                    }
                    if (!parts[8].isEmpty()) {
                        draft.setEnd(LocalDate.parse(parts[8]));
                    }
                    envelope.medicines().add(new SavedMedicine(unescape(parts[2]), draft));
                } else if (parts[0].equals("R") && parts.length >= 10) {
                    DoseRecord record = new DoseRecord(
                            unescape(parts[1]),
                            unescape(parts[2]),
                            unescape(parts[3]),
                            unescape(parts[4]),
                            unescape(parts[5]),
                            DoseRound.valueOf(parts[6]),
                            Long.parseLong(parts[7])
                    );
                    if (!parts[8].isEmpty()) {
                        Long taken = parts[9].isEmpty() ? null : Long.valueOf(parts[9]);
                        record.restoreManual(DoseStatus.valueOf(parts[8]), taken);
                    }
                    decoded.records.add(record);
                }
            } catch (RuntimeException ignored) {
                // Skip a damaged line and keep the rest of the local record.
            }
        }
        return decoded;
    }

    private static String timings(SavedMedicine medicine) {
        StringBuilder names = new StringBuilder();
        for (DoseTiming timing : DoseTiming.values()) {
            if (!medicine.timings().contains(timing)) {
                continue;
            }
            if (names.length() > 0) {
                names.append(',');
            }
            names.append(timing.name());
        }
        return names.toString();
    }

    private static String rounds(SavedMedicine medicine) {
        StringBuilder names = new StringBuilder();
        for (DoseRound round : DoseRound.values()) {
            if (!medicine.rounds().contains(round)) {
                continue;
            }
            if (names.length() > 0) {
                names.append(',');
            }
            names.append(round.name());
        }
        return names.toString();
    }

    private static String[] split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '\\' && index + 1 < line.length()) {
                current.append(ch);
                current.append(line.charAt(++index));
            } else if (ch == SEP) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace(String.valueOf(SEP), "\\s");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (escape) {
                if (ch == 'n') {
                    out.append('\n');
                } else if (ch == 's') {
                    out.append(SEP);
                } else {
                    out.append(ch);
                }
                escape = false;
            } else if (ch == '\\') {
                escape = true;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
