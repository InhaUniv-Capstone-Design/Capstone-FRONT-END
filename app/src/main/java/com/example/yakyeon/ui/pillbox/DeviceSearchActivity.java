package com.example.yakyeon.ui.pillbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.PillboxRepository;
import com.example.yakyeon.data.mock.MockPillboxRepository;
import com.example.yakyeon.data.model.DiscoveredPillbox;
import com.example.yakyeon.ui.common.AppNavigator;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.List;

public class DeviceSearchActivity extends AppCompatActivity {

    private final PillboxRepository pillbox = MockPillboxRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_device_search);
        WindowSpacing.applySystemBars(findViewById(R.id.device_search_root));
        BottomNav.bindPillbox(this);
        findViewById(R.id.device_search_back).setOnClickListener(view -> finish());
        findViewById(R.id.device_search_rescan).setOnClickListener(view -> bindDevices());
        bindDevices();
    }

    private void bindDevices() {
        List<DiscoveredPillbox> devices = pillbox.simulatedNearbyDevices();
        ((TextView) findViewById(R.id.device_search_count)).setText(
                getString(R.string.pillbox_found_count, devices.size())
        );
        findViewById(R.id.device_search_found).setVisibility(
                devices.isEmpty() ? View.GONE : View.VISIBLE
        );
        LinearLayout list = findViewById(R.id.device_search_list);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < devices.size(); index++) {
            DiscoveredPillbox device = devices.get(index);
            View row = inflater.inflate(R.layout.item_discovered_device, list, false);
            if (index > 0) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) row.getLayoutParams();
                params.topMargin = dp(12);
                row.setLayoutParams(params);
            }
            ((TextView) row.findViewById(R.id.device_name)).setText(device.getName());
            ((TextView) row.findViewById(R.id.device_serial)).setText(device.getSerialLabel());
            row.setOnClickListener(view -> link(device));
            list.addView(row);
        }
    }

    private void link(DiscoveredPillbox device) {
        if (pillbox.linkSimulatedDevice(device.getId())) {
            AppNavigator.openPillbox(this);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
