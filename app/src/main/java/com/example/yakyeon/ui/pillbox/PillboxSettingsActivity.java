package com.example.yakyeon.ui.pillbox;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.PillboxRepository;
import com.example.yakyeon.data.mock.MockPillboxRepository;
import com.example.yakyeon.data.model.PillboxSession;
import com.example.yakyeon.ui.common.AppNavigator;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.Calendar;

public class PillboxSettingsActivity extends AppCompatActivity implements PillboxRenameDialog.Listener {

    private final PillboxRepository pillbox = MockPillboxRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pillbox_settings);
        WindowSpacing.applySystemBars(findViewById(R.id.pillbox_settings_root));
        findViewById(R.id.pillbox_settings_back).setOnClickListener(view -> finish());
        findViewById(R.id.pillbox_rename_row).setOnClickListener(view -> openRename());
        findViewById(R.id.pillbox_notification_row).setOnClickListener(view -> toggleNotifications());
        findViewById(R.id.pillbox_alarm_row).setOnClickListener(
                view -> AppNavigator.openPillboxAlarm(this)
        );
        findViewById(R.id.pillbox_sync_row).setOnClickListener(view -> sync());
        findViewById(R.id.pillbox_disconnect_row).setOnClickListener(view -> disconnect());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!pillbox.hasSimulatedLink()) {
            finish();
            return;
        }
        bindSession();
    }

    @Override
    public void onPillboxRenamed(String displayName) {
        pillbox.rename(displayName);
        bindSession();
    }

    private void openRename() {
        PillboxSession session = pillbox.session();
        if (session == null) {
            return;
        }
        PillboxRenameDialog.open(getSupportFragmentManager(), session.getDisplayName());
    }

    private void toggleNotifications() {
        PillboxSession session = pillbox.session();
        if (session == null) {
            return;
        }
        pillbox.setNotificationsEnabled(!session.isNotificationsEnabled());
        bindSession();
    }

    private void sync() {
        ((TextView) findViewById(R.id.pillbox_sync_detail)).setText(
                pillbox.recordSimulatedSync(Calendar.getInstance())
        );
    }

    private void disconnect() {
        pillbox.unlinkSimulatedDevice();
        finish();
    }

    private void bindSession() {
        PillboxSession session = pillbox.session();
        if (session == null) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.pillbox_device_name)).setText(session.getDisplayName());
        ((TextView) findViewById(R.id.pillbox_connection_detail)).setText(session.getConnectionDetail());
        ((TextView) findViewById(R.id.pillbox_alarm_detail)).setText(
                getString(R.string.pillbox_slots_active, session.getSlots().size())
        );
        ((TextView) findViewById(R.id.pillbox_sync_detail)).setText(session.getSyncLabel());
        ImageView toggle = findViewById(R.id.pillbox_notification_toggle);
        boolean enabled = session.isNotificationsEnabled();
        toggle.setImageResource(enabled ? R.drawable.ic_toggle_on : R.drawable.ic_toggle_off);
        toggle.setContentDescription(getString(enabled ? R.string.selected : R.string.not_selected));
    }
}
