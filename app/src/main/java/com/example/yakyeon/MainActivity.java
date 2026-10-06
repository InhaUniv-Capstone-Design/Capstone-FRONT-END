package com.example.yakyeon;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.data.MedicationRepository;
import com.example.yakyeon.data.mock.MockMedicationRepository;
import com.example.yakyeon.ui.common.AppNavigator;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.WindowSpacing;

public class MainActivity extends AppCompatActivity {

    private final MedicationRepository medications = MockMedicationRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        WindowSpacing.applySystemBars(findViewById(R.id.home_root));
        BottomNav.bind(this);

        findViewById(R.id.home_status_card).setOnClickListener(
                view -> AppNavigator.openMedicationList(this)
        );
        findViewById(R.id.home_warning_card).setOnClickListener(
                view -> AppNavigator.openContraindications(this)
        );
        View.OnClickListener openSchedule = view -> AppNavigator.openSchedule(this);
        findViewById(R.id.home_slot_morning).setOnClickListener(openSchedule);
        findViewById(R.id.home_slot_lunch).setOnClickListener(openSchedule);
        findViewById(R.id.home_slot_evening).setOnClickListener(openSchedule);
        findViewById(R.id.home_schedule_card).setOnClickListener(openSchedule);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int activeCount = medications.countActive();
        TextView takingCount = findViewById(R.id.home_taking_count);
        takingCount.setText(getString(R.string.home_taking_count, activeCount));
        TextView warningCount = findViewById(R.id.home_warning_count);
        warningCount.setText(getString(R.string.home_warning_medication_count, activeCount));
    }
}
