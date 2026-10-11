package com.example.yakyeon.data;

import com.example.yakyeon.data.model.ReminderDay;
import com.example.yakyeon.data.model.ReminderRecord;

import java.util.List;

public interface MedicationReminderRepository {

    String periodRange();

    String alertTimeLabel(ReminderRecord record);

    String checklistTitle(ReminderRecord record);

    List<ReminderDay> days(String filter);

    ReminderRecord find(String id);

    boolean markUserCompleted(String id);
}
