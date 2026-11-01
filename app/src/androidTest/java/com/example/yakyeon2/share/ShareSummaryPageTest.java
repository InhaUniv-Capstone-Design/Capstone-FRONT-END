package com.example.yakyeon2.share;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.yakyeon2.data.ClinicalShareOptions;
import com.example.yakyeon2.data.MedicineDraft;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.SavedMedicine;
import com.example.yakyeon2.data.ScenarioPrescriptions;
import com.example.yakyeon2.data.ShareSummary;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ShareSummaryPageTest {

    @Test
    public void rendersClinicianSummary() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MedicationStore store = ScenarioPrescriptions.knownStore();
        store.setDevGraphRelationScenario(true);
        SavedMedicine finished = ScenarioPrescriptions.medicine(store, "약6");
        MedicineDraft draft = new MedicineDraft();
        draft.setName(finished.getName());
        draft.setAmount(finished.getAmount());
        draft.rounds().addAll(finished.rounds());
        draft.setStart(LocalDate.of(2026, 9, 20));
        draft.setEnd(LocalDate.of(2026, 10, 1));
        finished.apply(draft);

        ShareSummary summary = ShareSummary.build(
                store,
                new ClinicalShareOptions(),
                "김약연",
                ScenarioPrescriptions.REFERENCE_DAY
        );
        Bitmap bitmap = ShareSummaryPage.render(context, summary);
        assertTrue(bitmap.getWidth() == 1080);
        assertTrue(bitmap.getHeight() > 800);
        bitmap.recycle();
    }
}
