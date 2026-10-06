package com.example.yakyeon.data;

import com.example.yakyeon.data.model.ScheduleDose;

import java.util.Calendar;
import java.util.List;

public interface MedicationScheduleRepository {

    List<ScheduleDose> dosesOn(Calendar date);
}
