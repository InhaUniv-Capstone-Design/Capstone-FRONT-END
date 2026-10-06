package com.example.yakyeon.ui.dur;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.DurInteractionRepository;
import com.example.yakyeon.data.mock.MockDurInteractionRepository;
import com.example.yakyeon.data.model.DurInteraction;
import com.example.yakyeon.ui.common.AppNavigator;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.List;

public class DurInteractionListActivity extends AppCompatActivity {

    private final DurInteractionRepository interactions = MockDurInteractionRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dur_interaction_list);
        WindowSpacing.applySystemBars(findViewById(R.id.dur_list_root));
        BottomNav.bind(this);

        findViewById(R.id.dur_back).setOnClickListener(view -> finish());
        List<DurInteraction> items = interactions.getInteractions();
        ((TextView) findViewById(R.id.dur_summary_title)).setText(
                getString(R.string.dur_summary_title, items.size())
        );
        ((TextView) findViewById(R.id.dur_count)).setText(getString(R.string.dur_count, items.size()));

        LinearLayout list = findViewById(R.id.dur_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DurInteraction interaction : items) {
            View item = inflater.inflate(R.layout.item_dur_interaction, list, false);
            ((TextView) item.findViewById(R.id.dur_item_pair)).setText(
                    getString(R.string.dur_pair, interaction.getDrugAName(), interaction.getDrugBName())
            );
            ((TextView) item.findViewById(R.id.dur_item_description)).setText(
                    interaction.getInteractionDescription()
            );
            item.setOnClickListener(view ->
                    AppNavigator.openDurInteractionDetail(this, interaction.getId())
            );
            list.addView(item);
        }
    }
}
