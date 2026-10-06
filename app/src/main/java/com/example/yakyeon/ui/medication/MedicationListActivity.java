package com.example.yakyeon.ui.medication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yakyeon.R;
import com.example.yakyeon.data.MedicationRepository;
import com.example.yakyeon.data.mock.MockMedicationRepository;
import com.example.yakyeon.data.model.Medication;
import com.example.yakyeon.ui.common.AppNavigator;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.List;

public class MedicationListActivity extends AppCompatActivity {

    private final MedicationRepository medications = MockMedicationRepository.getInstance();
    private MedicationAdapter activeAdapter;
    private MedicationAdapter endedAdapter;
    private TextView countView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medication_list);
        WindowSpacing.applySystemBars(findViewById(R.id.medication_list_root));
        BottomNav.bind(this);

        countView = findViewById(R.id.medication_list_count);
        activeAdapter = prepareList(R.id.medication_active_list);
        endedAdapter = prepareList(R.id.medication_ended_list);
        reloadMedications();

        findViewById(R.id.medication_register_button).setOnClickListener(
                view -> AppNavigator.openPrescriptionOcr(this)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activeAdapter != null) {
            reloadMedications();
        }
    }

    public void reloadMedications() {
        medications.updateExpiredMedications();
        submitSection(R.id.medication_active_section, activeAdapter, medications.getActiveMedications());
        submitSection(R.id.medication_ended_section, endedAdapter, medications.getEndedMedications());
        countView.setText(getString(R.string.medication_list_count, medications.countActive()));
    }

    public void applyMedicationEdit(
            String id,
            String name,
            double doseAmount,
            String doseUnit,
            String timesPerDay,
            String doseTimes,
            String doseTiming,
            String duration
    ) {
        medications.updateMedication(
                id,
                name,
                doseAmount,
                doseUnit,
                timesPerDay,
                doseTimes,
                doseTiming,
                duration
        );
        reloadMedications();
    }

    public void confirmMedicationDelete(String id) {
        medications.deleteMedication(id);
        dismissEditSheet();
        reloadMedications();
    }

    public void confirmMedicationEnd(String id) {
        medications.endMedication(id);
        dismissEditSheet();
        reloadMedications();
    }

    private MedicationAdapter prepareList(int listId) {
        RecyclerView list = findViewById(listId);
        list.setLayoutManager(new LinearLayoutManager(this));
        MedicationAdapter adapter = new MedicationAdapter(this::showDetail);
        adapter.setOnMedicationLongClickListener(this::showEdit);
        list.setAdapter(adapter);
        return adapter;
    }

    private void submitSection(int sectionId, MedicationAdapter adapter, List<Medication> items) {
        View section = findViewById(sectionId);
        section.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.submit(items);
    }

    private void showDetail(Medication medication) {
        MedicationDetailSheet.open(getSupportFragmentManager(), medication.getId());
    }

    private void showEdit(Medication medication) {
        MedicationEditSheet.open(getSupportFragmentManager(), medication.getId());
    }

    private void dismissEditSheet() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(MedicationEditSheet.TAG);
        if (fragment instanceof MedicationEditSheet) {
            ((MedicationEditSheet) fragment).dismiss();
        }
    }
}
