package com.example.yakyeon.data.mock;

import com.example.yakyeon.data.PrescriptionOcrRepository;
import com.example.yakyeon.data.model.PrescriptionOcrResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MockPrescriptionOcrRepository implements PrescriptionOcrRepository {

    private static MockPrescriptionOcrRepository instance;

    public static MockPrescriptionOcrRepository getInstance() {
        if (instance == null) {
            instance = new MockPrescriptionOcrRepository();
        }
        return instance;
    }

    @Override
    public List<PrescriptionOcrResult> recognize() {
        String period = periodFromToday(7);
        List<PrescriptionOcrResult> results = new ArrayList<>();
        results.add(new PrescriptionOcrResult(
                "메트포르민정 500mg",
                "500mg",
                1,
                "정",
                "2회",
                "08:00",
                "아침",
                period,
                false,
                "",
                List.of("메트포르민정 500mg", "메트포르민정 250mg", "메트포르민정 1000mg")
        ));
        results.add(new PrescriptionOcrResult(
                "아토르바스타틴정 10mg",
                "10mg",
                1,
                "정",
                "1회",
                "19:00",
                "저녁",
                period,
                false,
                "",
                Collections.emptyList()
        ));
        results.add(new PrescriptionOcrResult(
                "암로디핀정 5mg",
                "5mg",
                0.5,
                "정",
                "1회",
                "08:00",
                "아침",
                period,
                true,
                "용량을 직접 확인해 주세요",
                Collections.emptyList()
        ));
        return Collections.unmodifiableList(results);
    }

    private static String periodFromToday(int days) {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.DAY_OF_MONTH, days);
        return format(start) + " ~ " + format(end);
    }

    private static String format(Calendar calendar) {
        return String.format(
                Locale.KOREA,
                "%04d.%02d.%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }
}
