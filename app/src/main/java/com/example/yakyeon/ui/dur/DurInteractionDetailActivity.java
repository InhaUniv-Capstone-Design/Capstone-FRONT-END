package com.example.yakyeon.ui.dur;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.mock.MockDurInteractionRepository;
import com.example.yakyeon.data.model.DurInteraction;
import com.example.yakyeon.ui.common.WindowSpacing;

public class DurInteractionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "dur_interaction_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dur_interaction_detail);
        WindowSpacing.applySystemBars(findViewById(R.id.dur_detail_root));

        DurInteraction interaction = MockDurInteractionRepository.getInstance()
                .findById(getIntent().getStringExtra(EXTRA_ID));
        if (interaction == null) {
            finish();
            return;
        }

        findViewById(R.id.dur_back).setOnClickListener(view -> finish());
        ((TextView) findViewById(R.id.dur_drug_a)).setText(interaction.getDrugAName());
        ((TextView) findViewById(R.id.dur_drug_b)).setText(interaction.getDrugBName());
        ((TextView) findViewById(R.id.dur_drug_a_caption)).setText(interaction.getDrugACaption());
        ((TextView) findViewById(R.id.dur_drug_b_caption)).setText(interaction.getDrugBCaption());
        ((TextView) findViewById(R.id.dur_reason)).setText(interaction.getInteractionReason());
        ((TextView) findViewById(R.id.dur_description)).setText(interaction.getInteractionDescription());
        ((TextView) findViewById(R.id.dur_caution)).setText(interaction.getCaution());
    }
}
