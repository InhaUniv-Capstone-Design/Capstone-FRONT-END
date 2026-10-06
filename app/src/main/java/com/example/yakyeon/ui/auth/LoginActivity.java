package com.example.yakyeon.ui.auth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.ui.common.AppNavigator;
import com.example.yakyeon.ui.common.WindowSpacing;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        WindowSpacing.applySystemBars(findViewById(R.id.login_root));

        findViewById(R.id.login_button).setOnClickListener(view -> AppNavigator.openHome(this));
        findViewById(R.id.signup_button).setOnClickListener(view -> AppNavigator.openSignupType(this));
    }
}
