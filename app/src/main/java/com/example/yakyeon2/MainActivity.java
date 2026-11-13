package com.example.yakyeon2;

import android.Manifest;
import android.util.Log;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.yakyeon2.api.ApiClient;
import com.example.yakyeon2.api.ApiCodes;
import com.example.yakyeon2.api.ApiException;
import com.example.yakyeon2.api.ApiFailure;
import com.example.yakyeon2.api.ApiMessages;
import com.example.yakyeon2.api.CareApi;
import com.example.yakyeon2.api.CareContracts;
import com.example.yakyeon2.api.HomeApi;
import com.example.yakyeon2.api.HomeContracts;
import com.example.yakyeon2.api.HomeRegistration;
import com.example.yakyeon2.api.AuthApi;
import com.example.yakyeon2.api.AuthContracts;
import com.example.yakyeon2.api.NotificationInbox;
import com.example.yakyeon2.api.NotificationSync;
import com.example.yakyeon2.api.SettingsApi;
import com.example.yakyeon2.api.SettingsContracts;
import com.example.yakyeon2.api.UserApi;
import com.example.yakyeon2.auth.AuthFlow;
import com.example.yakyeon2.auth.AuthScreens;
import com.example.yakyeon2.auth.AuthValidation;
import com.example.yakyeon2.auth.RecoveryForm;
import com.example.yakyeon2.auth.RecoveryRules;
import com.example.yakyeon2.auth.SignupConsent;
import com.example.yakyeon2.data.AccountRoles;
import com.example.yakyeon2.data.MedicationRules;
import com.example.yakyeon2.data.AccountType;
import com.example.yakyeon2.data.AuthTokenStore;
import com.example.yakyeon2.data.ClinicalShareOptions;
import com.example.yakyeon2.data.LegalMarks;
import com.example.yakyeon2.data.LocalAccountData;
import com.example.yakyeon2.data.LocalAppSettings;
import com.example.yakyeon2.data.NotificationPreferences;
import com.example.yakyeon2.data.ProfileSnapshot;
import com.example.yakyeon2.data.ShareSummary;
import com.example.yakyeon2.share.ShareSummaryExporter;
import com.example.yakyeon2.data.LocalMedicationState;
import com.example.yakyeon2.data.LocalMedicineNames;
import com.example.yakyeon2.data.YakyeonRepositoryProvider;
import com.example.yakyeon2.dev.ManualCheckGate;
import com.example.yakyeon2.data.YakyeonSession;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.EnvelopeDraft;
import com.example.yakyeon2.fcm.BoxTakenRefresh;
import com.example.yakyeon2.fcm.FcmRegistrar;
import com.example.yakyeon2.fcm.YakyeonNotifications;
import com.example.yakyeon2.main.MainScreens;
import com.example.yakyeon2.main.MedicineNamePicker;
import com.example.yakyeon2.main.MedScreens;
import com.example.yakyeon2.main.DoseCatchUpNotice;
import com.example.yakyeon2.main.RecordScreens;
import com.example.yakyeon2.main.SettingsScreens;
import com.example.yakyeon2.navigation.MedScreen;
import com.example.yakyeon2.navigation.AppNavigator;
import com.example.yakyeon2.navigation.AuthScreen;
import com.example.yakyeon2.navigation.DevHomeGate;
import com.example.yakyeon2.navigation.DevPillboxScenarioGate;
import com.example.yakyeon2.navigation.RegistrationChrome;
import com.example.yakyeon2.navigation.ScreenId;
import com.example.yakyeon2.ui.ConfirmDialog;
import com.example.yakyeon2.ui.SavedToast;
import com.example.yakyeon2.ui.ShortMessage;
import com.example.yakyeon2.ui.YakyeonHeader;
import com.example.yakyeon2.ui.YakyeonTextField;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.color.MaterialColors;

public class MainActivity extends AppCompatActivity
        implements AuthScreens.Host, MainScreens.Host, MedScreens.Host, SettingsScreens.AccountHost {

    private static final String STATE_TAB = "state_tab";
    private static final String STATE_NON_MAIN = "state_non_main";
    private static final String STATE_AUTH = "state_auth";
    private static final String STATE_RECOVERY_TAB = "state_recovery_tab";
    private static final String STATE_VERIFICATION_ID = "state_verification_id";
    private static final String STATE_VERIFICATION_TOKEN = "state_verification_token";
    private static final String STATE_VERIFIED_PHONE = "state_verified_phone";
    private static final String STATE_VERIFICATION_EXPIRES = "state_verification_expires";
    private static final String STATE_TOKEN_EXPIRES = "state_token_expires";
    private static final String STATE_PHONE_VERIFIED = "state_phone_verified";
    private static final String STATE_MESSAGE_TEXT = "state_message_text";
    private static final String STATE_MESSAGE_TO = "state_message_to";
    private static final String STATE_ACCEPTED_CODE = "state_accepted_code";
    private static final String STATE_RECOVERY_ANNOUNCED = "state_recovery_announced";
    private static final String STATE_RECOVERY_RETRY = "state_recovery_retry";
    private static final String STATE_PROOF_PURPOSE = "state_proof_purpose";
    private static final String STATE_OPENED_PUSH = "state_opened_push";
    private static final String STATE_DEVICE_SERIAL = "state_device_serial";
    private static final String STATE_DEVICE_SECRET = "state_device_secret";
    private static final String STATE_DEVICE_PATIENT = "state_device_patient";
    private static final String STATE_CAMERA_URI = "state_camera_uri";
    private static final String PROOF_SIGNUP = "signup";
    private static final String PROOF_FIND = "find-id";
    private static final String PROOF_RESET = "reset";

    private final AppNavigator navigator = new AppNavigator();
    private AuthFlow authFlow;
    private YakyeonHeader header;
    private FrameLayout screenContainer;
    private BottomNavigationView bottomNav;
    private View root;
    private int themeBackground;
    private boolean bindingSelection;
    private AlertDialog consentDialog;
    private boolean dismissingConsent;
    private Dialog alarmDialog;
    private boolean dismissingAlarm;
    private boolean loginInFlight;
    private boolean authRequestInFlight;
    private boolean accountRequestInFlight;
    private boolean notificationRequestInFlight;
    private boolean deviceRequestInFlight;
    private boolean compartmentReleaseInFlight;
    private AuthContracts.PreparedTerms signupTerms;
    private long verificationId;
    private String verifiedPhone = "";
    private AuthContracts.PhoneVerification phoneVerification;
    private long verificationExpiresAt;
    private boolean phoneVerified;
    private String verificationToken;
    private long verificationTokenExpiresAt;
    private final RecoveryForm recoveryForm = new RecoveryForm();
    private boolean recoveryVerifiedAnnounced;
    private long recoveryRetryNotBefore;
    private String acceptedCode;
    private String failedCode;
    private boolean phoneConfirmInFlight;
    private long lastOutboundConfirmAt;
    private int outboundAutoConfirms;
    private String proofPurpose = "";
    private Runnable delayedPhoneConfirm;
    private String pendingPushId;
    private String openedPushId;
    private final java.util.Set<Long> doseChecksInFlight =
            java.util.Collections.synchronizedSet(new java.util.HashSet<Long>());
    private final Handler verificationHandler = new Handler(Looper.getMainLooper());
    private final Runnable verificationTick = this::tickVerification;
    private final java.util.List<SettingsScreens.LinkLine> managedLines = new java.util.ArrayList<>();
    private final java.util.List<SettingsScreens.LinkLine> guardianLines = new java.util.ArrayList<>();
    private final java.util.List<SettingsContracts.AgreementRecord> agreementRecords = new java.util.ArrayList<>();
    private LegalMarks agreementMarks = new LegalMarks();
    private boolean showGuardians;
    private boolean careReady;
    private boolean agreementsReady;
    private SettingsScreens.DevicePanel devicePanel;
    private boolean deviceReady;
    private boolean homeNameRequested;
    private boolean deviceWritesAllowed;
    private String deviceStatusMessage;
    private String deviceSerialDraft = "";
    private String deviceSecretDraft = "";
    private Long pendingDevicePatientId;
    private java.time.LocalDate homeShownDate;
    private final Runnable homeDateRollover = () -> {
        if (!isAlive()) {
            return;
        }
        rollHomeToToday();
        armHomeDateWatch();
    };
    private Long settingsPatientId;
    private int settingsEpoch;
    private boolean graphArmed;
    private int graphEpoch;
    private MainScreens.CareView graphView = MainScreens.CareView.LOADING;
    private CareContracts.GraphBundle graphBundle;
    private String graphMessage;
    private boolean pillboxArmed;
    private int pillboxEpoch;
    private MainScreens.CareView pillboxView = MainScreens.CareView.LOADING;
    private CareContracts.Pillbox pillboxSnapshot;
    private String pillboxMessage;
    private boolean listPillboxArmed;
    private int listPillboxEpoch;
    private MainScreens.CareView listPillboxView = MainScreens.CareView.LOADING;
    private CareContracts.Pillbox listPillbox;
    private Long serverSlotPrescriptionId;
    private Integer serverSlotChoice;
    private CareContracts.Pillbox serverSlotPillbox;
    private boolean serverSlotBusy;
    private boolean homeArmed;
    private int homeEpoch;
    private MainScreens.CareView homeView = MainScreens.CareView.LOADING;
    private String homeMessage;
    private HomeContracts.Section<HomeContracts.HomeSummary> homeSummary;
    private HomeContracts.Section<java.util.List<HomeContracts.Medication>> homeActive;
    private HomeContracts.Section<CareContracts.Summary> homeInteractions;
    private HomeContracts.Section<java.util.List<HomeContracts.Dose>> homeToday;
    private HomeContracts.Section<java.util.List<HomeContracts.Prescription>> homePrescriptions;
    private Long homePatientId;
    private String homeLinkedPatientName;
    private boolean homeWrites;
    private boolean homeWriteBusy;
    private boolean scheduleLoaded;
    private int scheduleEpoch;
    private MainScreens.CareView scheduleView = MainScreens.CareView.LOADING;
    private String scheduleMessage;
    private java.util.List<HomeContracts.Dose> scheduleDoses;
    private java.time.LocalDate scheduleDate;
    private boolean historyLoaded;
    private int historyEpoch;
    private MainScreens.CareView historyView = MainScreens.CareView.LOADING;
    private String historyMessage;
    private java.util.List<HomeContracts.Dose> historyDoses;
    private String historyStatus;
    private java.time.LocalDate historyFrom;
    private java.time.LocalDate historyTo;
    private Dialog serverAlarmDialog;
    private Uri pendingCameraUri;
    private final ActivityResultLauncher<String> prescriptionImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::onPrescriptionChosen
    );
    private final ActivityResultLauncher<Uri> prescriptionCamera = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                Uri uri = pendingCameraUri;
                pendingCameraUri = null;
                if (success && uri != null) {
                    onPrescriptionChosen(uri);
                }
            }
    );
    private final java.util.Set<Long> catchUpAsked = new java.util.HashSet<>();
    private HomeContracts.Dose pendingCatchUp;
    private Dialog catchUpDialog;
    private static boolean notificationPermissionRequested;
    private boolean noticePermissionAsked;
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                Log.d(FcmRegistrar.TAG, granted ? "알림 권한 허용" : "알림 권한 거부");
                FcmRegistrar.syncStored(this);
                if (granted) {
                    postCatchUpNotice(pendingCatchUp);
                }
                String screen = navigator.currentScreen();
                if (MainScreens.NOTIFICATIONS.equals(screen) || SettingsScreens.NOTI_DETAIL.equals(screen)) {
                    refresh();
                }
            }
    );
    private boolean notificationReady;
    private Snackbar requestProgress;
    private String progressText;
    private String accountSyncKey;
    private int notificationEpoch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        YakyeonNotifications.ensureChannel(this);
        FcmRegistrar.requestToken(this);
        if (AuthTokenStore.hasUsableAccess(this)) {
            FcmRegistrar.syncStored(this);
        }
        requestNotificationPermission();
        EdgeToEdge.enable(
                this,
                SystemBarStyle.light(Color.WHITE, Color.WHITE),
                SystemBarStyle.light(Color.WHITE, Color.WHITE)
        );
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.main);
        themeBackground = MaterialColors.getColor(root, android.R.attr.colorBackground);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean keyboard = insets.isVisible(WindowInsetsCompat.Type.ime()) && ime.bottom > 0;
            int bottom = keyboard ? Math.max(systemBars.bottom, ime.bottom) : systemBars.bottom;
            boolean opening = keyboard && v.getPaddingBottom() <= systemBars.bottom;
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom);
            if (opening) {
                v.post(this::scrollFocusedFieldIntoView);
            }
            if (!keyboard) {
                return insets;
            }
            return new WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                    .setInsets(
                            WindowInsetsCompat.Type.systemBars(),
                            Insets.of(systemBars.left, systemBars.top, systemBars.right, 0)
                    )
                    .build();
        });

        authFlow = new AuthFlow(navigator, YakyeonRepositoryProvider.get().signupDraft());
        LocalMedicineNames.load(this, YakyeonRepositoryProvider.get().medicineNames());
        AccountType savedType = LocalAppSettings.loadAccountType(this);
        if (savedType != null) {
            YakyeonRepositoryProvider.get().session().setAccountType(savedType);
        }
        LocalAppSettings.loadNotifications(this, YakyeonRepositoryProvider.get().session().notifications());
        MedicationStore medications = YakyeonRepositoryProvider.get().medications();
        LocalMedicationState.load(this, medications);
        medications.setAfterChange(() -> LocalMedicationState.save(this, medications));
        header = findViewById(R.id.header);
        screenContainer = findViewById(R.id.screen_container);
        bottomNav = findViewById(R.id.bottom_nav);
        header.setOnBackClick(v -> {
            if (authFlow.back()) {
                render();
            }
        });
        bottomNav.setOnItemSelectedListener(item -> {
            if (bindingSelection) {
                return true;
            }
            navigator.selectTab(screenFor(item.getItemId()));
            render();
            return true;
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (closeKeyboardInsteadOfLeaving()) {
                    return;
                }
                if (authFlow.back()) {
                    render();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        if (savedInstanceState == null) {
            if (AuthTokenStore.hasUsableAccess(this)) {
                applyStoredAccountType();
                navigator.selectTab(ScreenId.HOME);
            } else {
                authFlow.start();
                if (AuthTokenStore.hasRefreshToken(this)) {
                    restoreSession();
                }
            }
        } else {
            restoreNavigation(savedInstanceState);
            restoreRecovery(savedInstanceState);
        }
        render();
        if (savedInstanceState != null) {
            openedPushId = savedInstanceState.getString(STATE_OPENED_PUSH);
            deviceSerialDraft = savedInstanceState.getString(STATE_DEVICE_SERIAL, "");
            deviceSecretDraft = savedInstanceState.getString(STATE_DEVICE_SECRET, "");
            if (savedInstanceState.containsKey(STATE_DEVICE_PATIENT)) {
                pendingDevicePatientId = savedInstanceState.getLong(STATE_DEVICE_PATIENT);
            }
            String cameraUri = savedInstanceState.getString(STATE_CAMERA_URI);
            if (cameraUri != null) {
                pendingCameraUri = Uri.parse(cameraUri);
            }
        }
        openPushIfPresent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openPushIfPresent(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_AUTH, navigator.isInAuthFlow());
        outState.putString(STATE_TAB, navigator.currentTab().name());
        outState.putStringArray(STATE_NON_MAIN, navigator.nonMainStack().toArray(new String[0]));
        recoveryForm.save(outState);
        outState.putString(STATE_RECOVERY_TAB, authFlow.recoveryTab().name());
        outState.putLong(STATE_VERIFICATION_ID, verificationId);
        outState.putString(STATE_VERIFICATION_TOKEN, verificationToken);
        outState.putString(STATE_VERIFIED_PHONE, verifiedPhone);
        outState.putLong(STATE_VERIFICATION_EXPIRES, verificationExpiresAt);
        outState.putLong(STATE_TOKEN_EXPIRES, verificationTokenExpiresAt);
        outState.putBoolean(STATE_PHONE_VERIFIED, phoneVerified);
        if (phoneVerification != null) {
            outState.putString(STATE_MESSAGE_TEXT, phoneVerification.messageText);
            outState.putString(STATE_MESSAGE_TO, phoneVerification.messageTo);
        }
        outState.putString(STATE_ACCEPTED_CODE, acceptedCode);
        outState.putBoolean(STATE_RECOVERY_ANNOUNCED, recoveryVerifiedAnnounced);
        outState.putLong(STATE_RECOVERY_RETRY, recoveryRetryNotBefore);
        outState.putString(STATE_PROOF_PURPOSE, proofPurpose);
        outState.putString(STATE_OPENED_PUSH, openedPushId);
        outState.putString(STATE_DEVICE_SERIAL, deviceSerialDraft == null ? "" : deviceSerialDraft);
        outState.putString(STATE_DEVICE_SECRET, deviceSecretDraft == null ? "" : deviceSecretDraft);
        if (pendingDevicePatientId != null) {
            outState.putLong(STATE_DEVICE_PATIENT, pendingDevicePatientId);
        }
        if (pendingCameraUri != null) {
            outState.putString(STATE_CAMERA_URI, pendingCameraUri.toString());
        }
    }

    @Override
    public void showMessage(View anchor, CharSequence message) {
        ShortMessage.show(anchor, message);
    }

    @Override
    public void showNotice(CharSequence title, CharSequence message, Runnable onConfirm) {
        ConfirmDialog.show(this, title, message, getString(R.string.action_confirm), null, new ConfirmDialog.Callback() {
            @Override
            public void onConfirm() {
                if (onConfirm != null) {
                    onConfirm.run();
                }
            }

            @Override
            public void onCancel() {
            }
        });
    }

    @Override
    public void onManualCheck() {
        if (!ManualCheckGate.available()) {
            return;
        }
        if (YakyeonRepositoryProvider.get().session().getAccountType() == null) {
            YakyeonRepositoryProvider.get().session().setAccountType(AccountType.SELF);
        }
        navigator.selectTab(ScreenId.HOME);
        render();
        ShortMessage.show(screenContainer, getString(R.string.auth_manual_check_entered));
    }

    @Override
    public void onLogin(String id, String password) {
        switch (AuthValidation.login(id, password)) {
            case EMPTY_ID:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_id));
                break;
            case EMPTY_PASSWORD:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_password));
                break;
            case ID_TOO_LONG:
                ShortMessage.show(screenContainer, getString(R.string.auth_login_id_length));
                break;
            case PASSWORD_TOO_LONG:
                ShortMessage.show(screenContainer, getString(R.string.auth_login_password_length));
                break;
            case NOT_CONNECTED:
                submitLogin(id.trim(), password);
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestSignup() {
        if (authRequestInFlight) {
            return;
        }
        authRequestInFlight = true;
        showProgress(getString(R.string.auth_signup_loading));
        ApiClient.execute(() -> {
            try {
                AuthContracts.PreparedTerms prepared = AuthApi.terms(getApplicationContext());
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    if (prepared.fit == AuthContracts.TermsFit.EXTRA_REQUIRED) {
                        showNotice(getString(R.string.auth_signup), getString(R.string.auth_terms_blocked), null);
                        return;
                    }
                    if (prepared.fit != AuthContracts.TermsFit.READY) {
                        showNotice(getString(R.string.auth_signup), getString(R.string.auth_terms_missing), null);
                        return;
                    }
                    signupTerms = prepared;
                    AuthScreens.showConsent(
                            this,
                            this,
                            prepared.tos,
                            prepared.privacy + "\n\n" + prepared.notice + "\n\n" + prepared.sensitive
                    );
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    showApiFailure(getString(R.string.auth_signup), exception);
                });
            }
        });
    }

    @Override
    public void onRequestPhoneCode(String phone) {
        String digits = AuthContracts.phoneDigits(phone);
        if (digits == null) {
            ShortMessage.show(screenContainer, getString(R.string.auth_phone_invalid));
            return;
        }
        if (onRecoverySurface()
                && RecoveryRules.blocksRetry(recoveryRetryNotBefore, System.currentTimeMillis())) {
            showNotice(recoveryTitle(), getString(R.string.auth_rate_limited), null);
            return;
        }
        if (authRequestInFlight) {
            return;
        }
        authRequestInFlight = true;
        showProgress(getString(R.string.auth_login_loading));
        ApiClient.execute(() -> {
            try {
                AuthContracts.PhoneVerification started = AuthApi.sendPhoneCode(getApplicationContext(), digits);
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    verificationId = started.verificationId;
                    verifiedPhone = digits;
                    phoneVerification = started;
                    lastOutboundConfirmAt = 0L;
                    outboundAutoConfirms = 0;
                    failedCode = null;
                    clearPhoneProof();
                    verificationExpiresAt = AuthContracts.verificationDeadline(
                            System.currentTimeMillis(),
                            started.expiresIn
                    );
                    proofPurpose = purposeForCurrentScreen();
                    YakyeonTextField codeField = screenContainer.findViewById(R.id.field_verification_code);
                    if (codeField != null) {
                        codeField.setText("");
                    }
                    AuthScreens.applyPhoneVerification(this, screenContainer, started, this);
                    tickVerification();
                    if (!started.outboundSms()) {
                        ShortMessage.show(screenContainer, getString(R.string.auth_code_sent));
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    noteRecoveryRateLimit(exception);
                    showApiFailure(getString(R.string.auth_request_code), exception);
                });
            }
        });
    }

    @Override
    public void onPhoneDraftChanged() {
        YakyeonTextField phone = screenContainer.findViewById(R.id.field_phone);
        String digits = phone == null ? null : AuthContracts.phoneDigits(phone.getText());
        if (phoneVerified && (digits == null || !digits.equals(verifiedPhone))) {
            clearPhoneProof();
        }
        tickVerification();
    }

    @Override
    public void onPhoneCodeDraft(String phone, String code) {
        String digits = AuthContracts.phoneDigits(phone);
        String trimmed = code == null ? "" : code.trim();
        if (getString(R.string.auth_phone_verified).equals(trimmed)) {
            return;
        }
        if (phoneVerified && (digits == null || !digits.equals(verifiedPhone) || !trimmed.equals(acceptedCode))) {
            clearPhoneProof();
            if (!trimmed.matches("\\d*")) {
                YakyeonTextField field = screenContainer.findViewById(R.id.field_verification_code);
                if (field != null) {
                    field.setText("");
                }
                tickVerification();
                return;
            }
        }
        tickVerification();
        if (phoneVerification != null && phoneVerification.outboundSms()) {
            return;
        }
        if (!AuthContracts.isVerificationCode(trimmed)) {
            failedCode = null;
            return;
        }
        if (phoneVerified && trimmed.equals(acceptedCode) && digits != null && digits.equals(verifiedPhone)) {
            return;
        }
        if (trimmed.equals(failedCode) || phoneConfirmInFlight || authRequestInFlight) {
            return;
        }
        if (verificationId <= 0L || digits == null || !digits.equals(verifiedPhone)) {
            return;
        }
        if (verificationExpiresAt <= System.currentTimeMillis()) {
            ShortMessage.show(screenContainer, getString(R.string.auth_code_expired));
            return;
        }
        confirmPhoneCode(digits, trimmed, false);
    }

    @Override
    public void onCheckLoginId(String loginId) {
        String id = loginId == null ? "" : loginId.trim();
        if (!id.matches("[A-Za-z0-9]{6,20}")) {
            showNotice(getString(R.string.auth_invalid_id_title), getString(R.string.auth_invalid_id_message), null);
            return;
        }
        if (authRequestInFlight) {
            return;
        }
        authRequestInFlight = true;
        showProgress(getString(R.string.auth_login_loading));
        ApiClient.execute(() -> {
            try {
                boolean available = AuthApi.checkLoginId(getApplicationContext(), id);
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    ShortMessage.show(
                            screenContainer,
                            getString(available ? R.string.auth_id_available : R.string.auth_id_taken)
                    );
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    showApiFailure(getString(R.string.auth_check_duplicate), exception);
                });
            }
        });
    }

    @Override
    public void onConsentAccepted() {
        if (authFlow.openType(new SignupConsent(true, true, true, true))) {
            render();
        }
    }

    @Override
    public void onTypeNext(AccountType type) {
        if (type != null) {
            YakyeonRepositoryProvider.get().session().setAccountType(type);
            LocalAppSettings.saveAccountType(this, type);
        }
        switch (authFlow.next(type)) {
            case NEED_TYPE:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_type));
                break;
            case CAREGIVER_NOTICE:
                AuthScreens.showCaregiver(this, this);
                break;
            case FORM:
                render();
                break;
            default:
                break;
        }
    }

    @Override
    public void onContinueCaregiver() {
        authFlow.continueAfterCaregiverNotice();
        render();
    }

    @Override
    public void onOpenRecovery(AuthFlow.RecoveryTab tab) {
        if (!AuthScreen.LOGIN.equals(navigator.currentScreen())) {
            return;
        }
        recoveryForm.clear();
        resetPhoneVerification();
        recoveryVerifiedAnnounced = false;
        recoveryRetryNotBefore = 0L;
        if (authFlow.openRecovery(tab)) {
            render();
        }
    }

    @Override
    public void onRecoveryTab(AuthFlow.RecoveryTab tab) {
        if (tab == null || tab == authFlow.recoveryTab()) {
            return;
        }
        authFlow.showRecoveryTab(tab);
        resetPhoneVerification();
        recoveryVerifiedAnnounced = false;
        render();
    }

    @Override
    public void onRecoveryPhoneTyped(String phone) {
        String digits = AuthContracts.phoneDigits(phone);
        if (verificationId > 0L && (digits == null || !digits.equals(verifiedPhone))) {
            resetPhoneVerification();
            recoveryVerifiedAnnounced = false;
        }
    }

    @Override
    public void onFindLoginId(String name, String phone) {
        switch (AuthValidation.findId(name, phone)) {
            case MISSING_NAME:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_name));
                break;
            case MISSING_PHONE:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_phone));
                break;
            case INVALID_PHONE:
                ShortMessage.show(screenContainer, getString(R.string.auth_phone_invalid));
                break;
            case READY:
                String digits = AuthContracts.phoneDigits(phone);
                recoveryForm.setFindName(AuthValidation.normalizePersonName(name));
                recoveryForm.setFindPhone(digits);
                continueAfterIdentity(digits, true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onResetIdentityNext(String loginId, String phone) {
        switch (AuthValidation.confirmResetIdentity(loginId, phone)) {
            case MISSING_ID:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_id));
                break;
            case MISSING_PHONE:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_phone));
                break;
            case INVALID_ID:
                showNotice(
                        getString(R.string.auth_invalid_id_title),
                        getString(R.string.auth_invalid_id_message),
                        null
                );
                break;
            case INVALID_PHONE:
                ShortMessage.show(screenContainer, getString(R.string.auth_phone_invalid));
                break;
            case READY:
                String digits = AuthContracts.phoneDigits(phone);
                recoveryForm.setResetLoginId(loginId == null ? "" : loginId.trim());
                recoveryForm.setResetPhone(digits);
                continueAfterIdentity(digits, false);
                break;
            default:
                break;
        }
    }

    @Override
    public void onRecoveryVerifiedNext() {
        if (authRequestInFlight || phoneConfirmInFlight) {
            return;
        }
        YakyeonTextField phone = screenContainer.findViewById(R.id.field_phone);
        String digits = phone == null ? null : AuthContracts.phoneDigits(phone.getText());
        if (!usablePhoneProof(digits)) {
            if (phoneVerified) {
                discardRecoveryProof();
                showNotice(recoveryTitle(), getString(R.string.auth_verification_restart), null);
            } else if (verificationExpiresAt > 0L && verificationExpiresAt <= System.currentTimeMillis()) {
                ShortMessage.show(screenContainer, getString(R.string.auth_code_expired));
            } else {
                ShortMessage.show(screenContainer, getString(R.string.auth_verify_required));
            }
            return;
        }
        if (authFlow.recoveryTab() == AuthFlow.RecoveryTab.PASSWORD) {
            recoveryForm.setResetPhone(digits);
            if (authFlow.openRecoveryPassword()) {
                render();
            }
            return;
        }
        recoveryForm.setFindPhone(digits);
        submitFindId();
    }

    @Override
    public void onResetPassword(String loginId, String phone, String password, String passwordConfirm) {
        if (authRequestInFlight) {
            return;
        }
        switch (AuthValidation.resetPassword(loginId, phone, password, passwordConfirm)) {
            case MISSING_ID:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_id));
                return;
            case MISSING_PHONE:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_phone));
                return;
            case MISSING_PASSWORD:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_password));
                return;
            case MISSING_PASSWORD_CONFIRM:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_password_confirm));
                return;
            case PASSWORD_MISMATCH:
                ShortMessage.show(screenContainer, getString(R.string.auth_reset_mismatch));
                return;
            case INVALID_ID:
                showNotice(
                        getString(R.string.auth_invalid_id_title),
                        getString(R.string.auth_invalid_id_message),
                        null
                );
                return;
            case INVALID_PASSWORD:
                showPasswordIssue(password);
                return;
            case INVALID_PHONE:
                ShortMessage.show(screenContainer, getString(R.string.auth_phone_invalid));
                return;
            case READY:
                break;
            default:
                return;
        }
        String digits = AuthContracts.phoneDigits(phone);
        if (!usablePhoneProof(digits)) {
            discardRecoveryProof();
            showNotice(recoveryTitle(), getString(R.string.auth_verification_restart), null);
            return;
        }
        recoveryForm.setResetLoginId(loginId.trim());
        recoveryForm.setResetPhone(digits);
        submitPasswordReset(loginId.trim(), digits, password, passwordConfirm);
    }

    @Override
    public void onOpenLoginFromRecovery() {
        recoveryForm.clear();
        resetPhoneVerification();
        recoveryVerifiedAnnounced = false;
        authFlow.returnToLogin();
        render();
    }

    @Override
    public void onStartPasswordReset() {
        String phone = recoveryForm.findPhone();
        String loginId = "";
        if (recoveryForm.accounts().size() == 1) {
            loginId = recoveryForm.accounts().get(0).loginId;
        }
        recoveryForm.clear();
        recoveryForm.setResetPhone(phone);
        recoveryForm.setResetLoginId(loginId);
        resetPhoneVerification();
        recoveryVerifiedAnnounced = false;
        if (authFlow.restartRecovery(AuthFlow.RecoveryTab.PASSWORD)) {
            render();
        }
    }

    public void onDevHome() {
        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (DevHomeGate.open(navigator, debuggable)) {
            render();
        }
    }

    public void onDevPillboxScenario() {
        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        MedicationStore store = YakyeonRepositoryProvider.get().medications();
        if (DevPillboxScenarioGate.open(navigator, store, debuggable)) {
            render();
            return;
        }
        if (debuggable) {
            ShortMessage.show(screenContainer, getString(R.string.home_dev_blocked));
        }
    }

    @Override
    public void showMessage(CharSequence message) {
        ShortMessage.show(screenContainer, message);
    }

    @Override
    public void showNotice(CharSequence title, CharSequence message) {
        showNotice(title, message, null);
    }

    @Override
    public void openGraph() {
        navigator.selectTab(ScreenId.GRAPH);
        render();
    }

    public void openDevTools() {
        if (!debuggable()) {
            return;
        }
        navigator.openNonMain(MainScreens.DEV_TOOLS);
        render();
    }

    @Override
    public void openDetail(String key) {
        navigator.openNonMain(key);
        render();
    }

    @Override
    public void openScreen(String key) {
        openDetail(key);
    }

    @Override
    public void back() {
        if (authFlow.back()) {
            render();
        }
    }

    @Override
    public void popTo(String key) {
        navigator.popTo(key);
        render();
    }

    @Override
    public void showRegisteredMedications() {
        navigator.openOrPopTo(MedScreen.LIST);
        render();
        SavedToast.show(this, getString(R.string.med_registered), "");
    }

    @Override
    public void refresh() {
        render();
    }

    @Override
    public void showPillboxTab() {
        navigator.selectTab(ScreenId.PILLBOX);
        render();
    }

    @Override
    public void registerMedication() {
        if (isSignedIn() && !homeWrites) {
            showMessage(getString(R.string.med_readonly));
            return;
        }
        YakyeonRepositoryProvider.get().medications().beginDraft();
        openScreen(MedScreen.CONSENT);
    }

    private void openCalendar() {
        if (showingSchedule()) {
            return;
        }
        openScreen(MedScreen.SCHEDULE);
    }

    private boolean showingSchedule() {
        return MedScreen.SCHEDULE.equals(navigator.currentScreen())
                || (navigator.isMainArea() && navigator.currentTab() == ScreenId.SCHEDULE);
    }

    private String guardianPatientName(SettingsContracts.ManagedPatient patient) {
        if (patient == null || patient.name == null
                || !AccountRoles.GUARDIAN.equals(AuthTokenStore.accountType(this))
                || !SettingsContracts.GUARDIAN.equals(patient.myRole)) {
            return null;
        }
        String name = patient.name.trim();
        return name.isEmpty() ? null : name;
    }

    @Override
    protected void onStart() {
        super.onStart();
        BoxTakenRefresh.attach(boxTakenHost);
        Log.i("BoxTaken", "onStart listener=true");
    }

    @Override
    protected void onStop() {
        BoxTakenRefresh.detach(boxTakenHost);
        Log.i("BoxTaken", "onStop listener=false");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        BoxTakenRefresh.detach(boxTakenHost);
        verificationHandler.removeCallbacks(verificationTick);
        verificationHandler.removeCallbacks(homeDateRollover);
        cancelDelayedPhoneConfirm();
        super.onDestroy();
    }

    @Override
    public void onSignupSubmit(
            String name,
            String birthdate,
            String phone,
            String code,
            String id,
            String password,
            String passwordConfirm
    ) {
        AuthValidation.SignupCheck check = AuthValidation.signup(
                name, birthdate, phone, id, password, passwordConfirm
        );
        switch (check) {
            case PASSWORD_MISMATCH:
                ShortMessage.show(screenContainer, getString(R.string.auth_password_mismatch));
                return;
            case INVALID_ID:
                showNotice(getString(R.string.auth_invalid_id_title), getString(R.string.auth_invalid_id_message), null);
                return;
            case INVALID_PASSWORD:
                showNotice(
                        getString(R.string.auth_invalid_password_title),
                        getString(R.string.auth_invalid_password_message),
                        null
                );
                return;
            case INVALID_PHONE:
                ShortMessage.show(screenContainer, getString(R.string.auth_phone_invalid));
                return;
            case INVALID_BIRTHDATE:
                ShortMessage.show(screenContainer, getString(R.string.auth_birth_invalid));
                return;
            case NOT_CONNECTED:
                break;
            default:
                ShortMessage.show(screenContainer, missingSignupMessage(check));
                return;
        }
        if (signupTerms == null || signupTerms.fit != AuthContracts.TermsFit.READY) {
            showNotice(getString(R.string.auth_signup), getString(R.string.auth_terms_missing), null);
            return;
        }
        String digits = AuthContracts.phoneDigits(phone);
        if (verificationId <= 0L || !PROOF_SIGNUP.equals(proofPurpose)) {
            ShortMessage.show(screenContainer, getString(R.string.auth_verify_first));
            return;
        }
        if (digits == null || !digits.equals(verifiedPhone)) {
            ShortMessage.show(screenContainer, getString(R.string.auth_phone_changed));
            return;
        }
        boolean outboundSms = phoneVerification != null && phoneVerification.outboundSms();
        String verificationCode;
        if (outboundSms) {
            verificationCode = phoneVerification.messageText;
        } else if (usablePhoneProof(digits)) {
            verificationCode = acceptedCode;
        } else {
            if (code == null || code.trim().isEmpty()
                    || getString(R.string.auth_phone_verified).equals(code.trim())) {
                ShortMessage.show(screenContainer, getString(R.string.auth_code_need));
                return;
            }
            if (!AuthContracts.isVerificationCode(code.trim())) {
                ShortMessage.show(screenContainer, getString(R.string.auth_code_format));
                return;
            }
            verificationCode = code.trim();
        }
        String accountType = AccountRoles.toServer(
                YakyeonRepositoryProvider.get().signupDraft().getAccountType()
        );
        if (accountType == null) {
            ShortMessage.show(screenContainer, getString(R.string.auth_need_type));
            return;
        }
        if (authRequestInFlight || phoneConfirmInFlight) {
            return;
        }
        if (!usablePhoneProof(digits) && verificationExpiresAt > 0L
                && verificationExpiresAt <= System.currentTimeMillis()) {
            ShortMessage.show(screenContainer, getString(R.string.auth_code_expired));
            return;
        }
        authRequestInFlight = true;
        showProgress(getString(R.string.auth_signup_loading));
        String loginId = id.trim();
        String birth = AuthContracts.isoDate(birthdate);
        String knownToken = usablePhoneProof(digits) ? verificationToken : null;
        long proofId = verificationId;
        ApiClient.execute(() -> {
            try {
                String token = knownToken;
                if (token == null) {
                    AuthContracts.PhoneConfirm confirm = AuthApi.confirmPhone(
                            getApplicationContext(),
                            proofId,
                            verificationCode,
                            digits
                    );
                    if (!confirm.verified) {
                        runOnUiThread(() -> {
                            authRequestInFlight = false;
                            clearProgress();
                            if (!isAlive()) {
                                return;
                            }
                            failedCode = verificationCode;
                            clearPhoneProof();
                            tickVerification();
                            ShortMessage.show(
                                    screenContainer,
                                    getString(outboundSms ? R.string.auth_sms_pending : R.string.auth_code_wrong)
                            );
                        });
                        return;
                    }
                    token = confirm.token;
                    acceptProof(digits, verificationCode, confirm);
                    runOnUiThread(this::tickVerification);
                }
                AuthApi.signup(
                        getApplicationContext(),
                        loginId,
                        password,
                        passwordConfirm,
                        accountType,
                        name.trim(),
                        birth,
                        digits,
                        proofId,
                        token,
                        signupTerms
                );
                runOnUiThread(() -> finishSignup());
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    tickVerification();
                    showApiFailure(getString(R.string.auth_signup), exception);
                });
            }
        });
    }

    private void confirmPhoneCode(String digits, String code, boolean quiet) {
        long now = System.currentTimeMillis();
        long sinceLast = lastOutboundConfirmAt == 0L
                ? AuthContracts.OUTBOUND_CONFIRM_GAP_MILLIS
                : now - lastOutboundConfirmAt;
        if (sinceLast < AuthContracts.OUTBOUND_CONFIRM_GAP_MILLIS) {
            if (!quiet) {
                schedulePhoneConfirm(digits, code, AuthContracts.OUTBOUND_CONFIRM_GAP_MILLIS - sinceLast);
            }
            return;
        }
        if (phoneConfirmInFlight) {
            if (!quiet) {
                schedulePhoneConfirm(digits, code, AuthContracts.OUTBOUND_CONFIRM_GAP_MILLIS);
            }
            return;
        }
        cancelDelayedPhoneConfirm();
        lastOutboundConfirmAt = now;
        if (quiet) {
            outboundAutoConfirms++;
        }
        phoneConfirmInFlight = true;
        long proofId = verificationId;
        ApiClient.execute(() -> {
            try {
                AuthContracts.PhoneConfirm confirm = AuthApi.confirmPhone(
                        getApplicationContext(),
                        proofId,
                        code,
                        digits
                );
                runOnUiThread(() -> {
                    phoneConfirmInFlight = false;
                    if (!isAlive() || proofId != verificationId) {
                        return;
                    }
                    if (!confirm.verified) {
                        if (quiet) {
                            return;
                        }
                        failedCode = code;
                        clearPhoneProof();
                        tickVerification();
                        ShortMessage.show(
                                screenContainer,
                                getString(phoneVerification != null && phoneVerification.outboundSms()
                                        ? R.string.auth_sms_pending
                                        : R.string.auth_code_wrong)
                        );
                        return;
                    }
                    acceptProof(digits, code, confirm);
                    tickVerification();
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    phoneConfirmInFlight = false;
                    if (exception.hasCode(ApiCodes.VERIFICATION_ATTEMPTS_EXCEEDED)
                            || exception.hasCode(ApiCodes.VERIFICATION_EXPIRED)) {
                        outboundAutoConfirms = AuthContracts.OUTBOUND_AUTO_CONFIRM_LIMIT;
                    }
                    if (!isAlive() || quiet && !exception.hasCode(ApiCodes.VERIFICATION_ATTEMPTS_EXCEEDED)
                            && !exception.hasCode(ApiCodes.VERIFICATION_EXPIRED)) {
                        return;
                    }
                    showApiFailure(getString(R.string.auth_code_label), exception);
                });
            }
        });
    }

    private boolean usablePhoneProof(String digits) {
        return phoneVerified
                && verificationToken != null
                && digits != null
                && digits.equals(verifiedPhone)
                && verificationTokenExpiresAt > System.currentTimeMillis();
    }

    private void acceptProof(String digits, String code, AuthContracts.PhoneConfirm confirm) {
        phoneVerified = true;
        verifiedPhone = digits;
        verificationToken = confirm.token;
        acceptedCode = code;
        failedCode = null;
        long lifetime = confirm.tokenExpiresIn == null ? 1800L : confirm.tokenExpiresIn;
        verificationTokenExpiresAt = System.currentTimeMillis() + lifetime * 1000L;
    }

    private void clearPhoneProof() {
        phoneVerified = false;
        verificationToken = null;
        verificationTokenExpiresAt = 0L;
        acceptedCode = null;
    }

    private void schedulePhoneConfirm(String digits, String code, long delayMillis) {
        cancelDelayedPhoneConfirm();
        delayedPhoneConfirm = () -> {
            delayedPhoneConfirm = null;
            if (!isAlive() || phoneVerified) {
                return;
            }
            confirmPhoneCode(digits, code, false);
        };
        verificationHandler.postDelayed(delayedPhoneConfirm, Math.max(0L, delayMillis));
    }

    private void cancelDelayedPhoneConfirm() {
        if (delayedPhoneConfirm != null) {
            verificationHandler.removeCallbacks(delayedPhoneConfirm);
            delayedPhoneConfirm = null;
        }
    }

    private String purposeForCurrentScreen() {
        String screen = navigator.currentScreen();
        if (AuthScreen.SIGNUP_FORM.equals(screen)) {
            return PROOF_SIGNUP;
        }
        if (AuthScreen.isRecovery(screen)) {
            return authFlow.recoveryTab() == AuthFlow.RecoveryTab.PASSWORD ? PROOF_RESET : PROOF_FIND;
        }
        return "";
    }

    private void resetPhoneVerification() {
        verificationHandler.removeCallbacks(verificationTick);
        cancelDelayedPhoneConfirm();
        verificationId = 0L;
        verifiedPhone = "";
        phoneVerification = null;
        verificationExpiresAt = 0L;
        lastOutboundConfirmAt = 0L;
        outboundAutoConfirms = 0;
        proofPurpose = "";
        failedCode = null;
        clearPhoneProof();
    }

    private boolean keepsPhoneVerification(String screen) {
        return AuthScreen.SIGNUP_FORM.equals(screen) || AuthScreen.isRecovery(screen);
    }

    private boolean onRecoverySurface() {
        return AuthScreen.isRecovery(navigator.currentScreen());
    }

    private String recoveryTitle() {
        return getString(authFlow.recoveryTab() == AuthFlow.RecoveryTab.PASSWORD
                ? R.string.auth_reset_password
                : R.string.auth_find_id);
    }

    private void continueAfterIdentity(String digits, boolean findId) {
        if (usablePhoneProof(digits)) {
            if (findId) {
                submitFindId();
            } else if (authFlow.openRecoveryPassword()) {
                render();
            }
            return;
        }
        boolean samePhone = digits != null && digits.equals(verifiedPhone);
        boolean expiredProof = phoneVerified && samePhone;
        if (!samePhone || phoneVerified) {
            resetPhoneVerification();
            recoveryVerifiedAnnounced = false;
        }
        if (authFlow.openRecoveryVerify()) {
            render();
        }
        if (expiredProof) {
            ShortMessage.show(screenContainer, getString(R.string.auth_verification_restart));
        }
    }

    private void submitFindId() {
        if (!beginRecoveryRequest()) {
            return;
        }
        String name = recoveryForm.findName();
        String phone = recoveryForm.findPhone();
        if (!PROOF_FIND.equals(proofPurpose) || !usablePhoneProof(phone)) {
            releaseRecoveryControls();
            discardRecoveryProof();
            showNotice(recoveryTitle(), getString(R.string.auth_verification_restart), null);
            return;
        }
        long proofId = verificationId;
        String proofToken = verificationToken;
        ApiClient.execute(() -> {
            try {
                java.util.List<AuthContracts.FoundAccount> accounts = AuthApi.findLoginId(
                        getApplicationContext(),
                        name,
                        phone,
                        proofId,
                        proofToken
                );
                runOnUiThread(() -> {
                    if (!isAlive()) {
                        return;
                    }
                    releaseRecoveryControls();
                    resetPhoneVerification();
                    recoveryVerifiedAnnounced = false;
                    recoveryForm.setAccounts(accounts);
                    if (authFlow.openRecoveryResult()) {
                        render();
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> handleRecoveryFailure(exception));
            }
        });
    }

    private void submitPasswordReset(
            String loginId,
            String phone,
            String password,
            String passwordConfirm
    ) {
        if (!beginRecoveryRequest()) {
            return;
        }
        if (!PROOF_RESET.equals(proofPurpose) || !usablePhoneProof(phone)) {
            releaseRecoveryControls();
            discardRecoveryProof();
            showNotice(recoveryTitle(), getString(R.string.auth_verification_restart), null);
            return;
        }
        long proofId = verificationId;
        String proofToken = verificationToken;
        ApiClient.execute(() -> {
            try {
                AuthApi.resetPassword(
                        getApplicationContext(),
                        loginId,
                        phone,
                        proofId,
                        proofToken,
                        password,
                        passwordConfirm
                );
                AuthTokenStore.clear(getApplicationContext());
                FcmRegistrar.noteSessionEnded(getApplicationContext());
                runOnUiThread(() -> finishPasswordReset());
            } catch (ApiException exception) {
                runOnUiThread(() -> handleRecoveryFailure(exception));
            }
        });
    }

    private void finishPasswordReset() {
        if (!isAlive()) {
            return;
        }
        releaseRecoveryControls();
        resetPhoneVerification();
        recoveryVerifiedAnnounced = false;
        AuthTokenStore.clear(this);
        FcmRegistrar.noteSessionEnded(this);
        if (authFlow.openRecoveryDone()) {
            render();
        }
    }

    private boolean beginRecoveryRequest() {
        if (authRequestInFlight || phoneConfirmInFlight) {
            return false;
        }
        if (RecoveryRules.blocksRetry(recoveryRetryNotBefore, System.currentTimeMillis())) {
            showNotice(recoveryTitle(), getString(R.string.auth_rate_limited), null);
            return false;
        }
        authRequestInFlight = true;
        showProgress(getString(R.string.auth_recovery_loading));
        setViewEnabled(R.id.button_find_id, false);
        setViewEnabled(R.id.button_reset_next, false);
        setViewEnabled(R.id.button_recovery_next, false);
        setViewEnabled(R.id.button_reset_password, false);
        setViewEnabled(R.id.button_request_code, false);
        return true;
    }

    private void releaseRecoveryControls() {
        authRequestInFlight = false;
        clearProgress();
        setViewEnabled(R.id.button_find_id, true);
        setViewEnabled(R.id.button_reset_next, true);
        setViewEnabled(R.id.button_request_code, true);
        refreshResetButton();
        tickVerification();
    }

    private void refreshResetButton() {
        View button = screenContainer.findViewById(R.id.button_reset_password);
        YakyeonTextField password = screenContainer.findViewById(R.id.field_reset_password);
        YakyeonTextField confirm = screenContainer.findViewById(R.id.field_reset_confirm);
        TextView error = screenContainer.findViewById(R.id.text_reset_password_error);
        if (button == null || password == null || confirm == null || error == null) {
            return;
        }
        AuthScreens.refreshResetPassword(this, error, button, password.getText(), confirm.getText());
        if (authRequestInFlight) {
            button.setEnabled(false);
        }
    }

    private void handleRecoveryFailure(ApiException exception) {
        if (!isAlive()) {
            return;
        }
        authRequestInFlight = false;
        clearProgress();
        if (exception.endsSession()) {
            showApiFailure(recoveryTitle(), exception);
            return;
        }
        noteRecoveryRateLimit(exception);
        RecoveryRules.Notice notice = RecoveryRules.noticeFor(exception.code());
        if (!RecoveryRules.keepsProof(exception.code())) {
            discardRecoveryProof();
        } else if (notice == RecoveryRules.Notice.ACCOUNT_MISSING
                && AuthScreen.ACCOUNT_RECOVERY_VERIFY.equals(navigator.currentScreen())) {
            if (authFlow.backToRecoveryForm()) {
                render();
            }
        } else {
            setViewEnabled(R.id.button_find_id, true);
            setViewEnabled(R.id.button_reset_next, true);
            setViewEnabled(R.id.button_request_code, true);
            refreshResetButton();
            tickVerification();
        }
        switch (notice) {
            case ACCOUNT_MISSING:
                showNotice(recoveryTitle(), getString(R.string.auth_account_not_found), null);
                break;
            case VERIFICATION_RESTART:
                showNotice(recoveryTitle(), getString(R.string.auth_verification_restart), null);
                break;
            case RATE_LIMIT:
                showNotice(recoveryTitle(), getString(R.string.auth_rate_limited), null);
                break;
            case PASSWORD_MISMATCH:
                showNotice(recoveryTitle(), getString(R.string.auth_reset_mismatch), null);
                break;
            case VALIDATION:
                showNotice(
                        recoveryTitle(),
                        textOr(exception.getMessage(), getString(R.string.auth_recovery_validation)),
                        null
                );
                break;
            default:
                showApiFailure(recoveryTitle(), exception);
                break;
        }
    }

    private void discardRecoveryProof() {
        resetPhoneVerification();
        recoveryVerifiedAnnounced = false;
        if (authFlow.reopenRecoveryVerify()) {
            render();
        }
    }

    private void noteRecoveryRateLimit(ApiException exception) {
        if (!onRecoverySurface()) {
            return;
        }
        RecoveryRules.Notice notice = RecoveryRules.noticeFor(exception.code());
        if (notice == RecoveryRules.Notice.RATE_LIMIT) {
            recoveryRetryNotBefore = System.currentTimeMillis() + RecoveryRules.RATE_LIMIT_COOLDOWN_MS;
        }
    }

    private void showPasswordIssue(String password) {
        int message;
        switch (AuthValidation.passwordIssue(password)) {
            case TOO_SHORT:
                message = R.string.auth_password_too_short;
                break;
            case TOO_LONG:
                message = R.string.auth_password_too_long;
                break;
            case NEED_LETTER:
                message = R.string.auth_password_need_letter;
                break;
            case NEED_DIGIT:
                message = R.string.auth_password_need_digit;
                break;
            case NEED_SPECIAL:
                message = R.string.auth_password_need_special;
                break;
            default:
                message = R.string.auth_invalid_password_message;
                break;
        }
        showNotice(getString(R.string.auth_invalid_password_title), getString(message), null);
    }

    private void expireRecoveryTokenIfNeeded() {
        if (!phoneVerified || verificationTokenExpiresAt <= 0L) {
            return;
        }
        if (verificationTokenExpiresAt > System.currentTimeMillis()) {
            return;
        }
        resetPhoneVerification();
        recoveryVerifiedAnnounced = false;
        ShortMessage.show(screenContainer, getString(R.string.auth_verification_restart));
    }

    private void paintRecoveryVerifyChrome() {
        TextView done = screenContainer.findViewById(R.id.text_code_verified);
        if (done != null) {
            done.setText(getString(R.string.auth_phone_verified_done));
        }
        TextView sms = screenContainer.findViewById(R.id.text_sms_code);
        if (sms != null && phoneVerified) {
            sms.setText(getString(R.string.auth_phone_verified_done));
            sms.setLetterSpacing(0f);
        }
        YakyeonTextField phone = screenContainer.findViewById(R.id.field_phone);
        String digits = phone == null ? null : AuthContracts.phoneDigits(phone.getText());
        View next = screenContainer.findViewById(R.id.button_recovery_next);
        if (next != null) {
            next.setEnabled(!authRequestInFlight && usablePhoneProof(digits));
        }
        if (usablePhoneProof(digits) && !recoveryVerifiedAnnounced) {
            recoveryVerifiedAnnounced = true;
            showNotice(recoveryTitle(), getString(R.string.auth_phone_verified_done), null);
        }
    }

    private void setViewEnabled(int id, boolean enabled) {
        View view = screenContainer.findViewById(id);
        if (view != null) {
            view.setEnabled(enabled);
        }
    }

    private void restoreRecovery(Bundle savedState) {
        recoveryForm.restore(savedState);
        String tab = savedState.getString(STATE_RECOVERY_TAB);
        if (tab != null) {
            try {
                authFlow.restoreRecoveryTab(AuthFlow.RecoveryTab.valueOf(tab));
            } catch (IllegalArgumentException ignored) {
                authFlow.restoreRecoveryTab(AuthFlow.RecoveryTab.ID);
            }
        }
        verificationId = savedState.getLong(STATE_VERIFICATION_ID, 0L);
        verifiedPhone = savedState.getString(STATE_VERIFIED_PHONE, "");
        if (verifiedPhone == null) {
            verifiedPhone = "";
        }
        verificationExpiresAt = savedState.getLong(STATE_VERIFICATION_EXPIRES, 0L);
        verificationTokenExpiresAt = savedState.getLong(STATE_TOKEN_EXPIRES, 0L);
        phoneVerified = savedState.getBoolean(STATE_PHONE_VERIFIED, false);
        verificationToken = phoneVerified ? savedState.getString(STATE_VERIFICATION_TOKEN) : null;
        phoneVerification = AuthContracts.remembered(
                verificationId,
                savedState.getString(STATE_MESSAGE_TEXT),
                savedState.getString(STATE_MESSAGE_TO)
        );
        acceptedCode = savedState.getString(STATE_ACCEPTED_CODE);
        recoveryVerifiedAnnounced = savedState.getBoolean(STATE_RECOVERY_ANNOUNCED, false);
        recoveryRetryNotBefore = savedState.getLong(STATE_RECOVERY_RETRY, 0L);
        proofPurpose = savedState.getString(STATE_PROOF_PURPOSE, "");
        if (proofPurpose == null) {
            proofPurpose = "";
        }
        if (proofPurpose.isEmpty() && verificationId > 0L) {
            proofPurpose = purposeForCurrentScreen();
        }
    }

    private void finishSignup() {
        authRequestInFlight = false;
        clearProgress();
        if (!isAlive()) {
            return;
        }
        verificationId = 0L;
        verifiedPhone = "";
        phoneVerification = null;
        verificationExpiresAt = 0L;
        clearPhoneProof();
        signupTerms = null;
        verificationHandler.removeCallbacks(verificationTick);
        authFlow.returnToLogin();
        render();
        showNotice(getString(R.string.auth_signup), getString(R.string.auth_signup_created), null);
    }

    private void tickVerification() {
        verificationHandler.removeCallbacks(verificationTick);
        if (!isAlive()) {
            return;
        }
        String screen = navigator.currentScreen();
        boolean signup = AuthScreen.SIGNUP_FORM.equals(screen);
        boolean recoveryVerify = AuthScreen.ACCOUNT_RECOVERY_VERIFY.equals(screen);
        if (!signup && !recoveryVerify) {
            return;
        }
        if (recoveryVerify) {
            expireRecoveryTokenIfNeeded();
        }
        AuthScreens.paintVerification(
                screenContainer,
                verifiedPhone,
                phoneVerified,
                verificationExpiresAt
        );
        if (recoveryVerify) {
            paintRecoveryVerifyChrome();
        }
        maybeConfirmOutbound();
        if (verificationId > 0L && verificationExpiresAt > System.currentTimeMillis()) {
            verificationHandler.postDelayed(verificationTick, 1000L);
        }
    }

    private void maybeConfirmOutbound() {
        if (phoneVerified || phoneConfirmInFlight || authRequestInFlight) {
            return;
        }
        if (phoneVerification == null || !phoneVerification.outboundSms()) {
            return;
        }
        if (verificationId <= 0L || verificationExpiresAt <= System.currentTimeMillis()) {
            return;
        }
        if (AuthContracts.phoneDigits(verifiedPhone) == null) {
            return;
        }
        if (outboundAutoConfirms >= AuthContracts.OUTBOUND_AUTO_CONFIRM_LIMIT) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastOutboundConfirmAt != 0L
                && now - lastOutboundConfirmAt < AuthContracts.OUTBOUND_CONFIRM_GAP_MILLIS) {
            return;
        }
        confirmPhoneCode(verifiedPhone, phoneVerification.messageText, true);
    }

    private void restoreNavigation(Bundle savedInstanceState) {
        boolean auth = savedInstanceState.getBoolean(STATE_AUTH);
        if (auth) {
            navigator.showLoginRoot();
        }
        String tabName = savedInstanceState.getString(STATE_TAB);
        if (!auth && tabName != null) {
            navigator.selectTab(ScreenId.valueOf(tabName));
        }
        String[] titles = savedInstanceState.getStringArray(STATE_NON_MAIN);
        if (titles == null) {
            return;
        }
        for (int i = titles.length - 1; i >= 0; i--) {
            navigator.openNonMain(titles[i]);
        }
    }

    private void render() {
        if (MedScreen.HISTORY.equals(navigator.currentScreen())) {
            navigator.pop();
            navigator.openNonMain(MedScreen.SCHEDULE);
        }
        if (navigator.isMainArea() && navigator.currentTab() == ScreenId.SCHEDULE) {
            navigator.selectTab(ScreenId.HOME);
            navigator.openNonMain(MedScreen.SCHEDULE);
        }
        if (!MedScreen.SLOT.equals(navigator.currentScreen())) {
            YakyeonRepositoryProvider.get().medications().cancelSlotAssign();
            clearServerSlot();
        }
        if (SettingsScreens.PILLBOX.equals(navigator.currentScreen())
                && !isSignedIn()
                && !YakyeonRepositoryProvider.get().medications().pillboxConnected()) {
            navigator.selectTab(ScreenId.PILLBOX);
        }
        boolean main = navigator.isMainArea();
        String screen = navigator.currentScreen();
        if (!keepsPhoneVerification(screen)) {
            resetPhoneVerification();
        }
        boolean login = AuthScreen.LOGIN.equals(screen);
        boolean showNav = RegistrationChrome.showsBottomNav(main, screen)
                || SettingsScreens.PILLBOX_SEARCH.equals(screen);
        bottomNav.setVisibility(showNav ? View.VISIBLE : View.GONE);
        boolean homeTab = main && navigator.currentTab() == ScreenId.HOME;
        header.setVisibility(login || homeTab ? View.GONE : View.VISIBLE);
        boolean listSurface = isMedicationListSurface(screen);
        header.setBackVisible(!main && !login && !listSurface);
        header.setBackPlated(!MedScreen.SLOT.equals(screen));
        header.setMinimumHeight(getResources().getDimensionPixelSize(
                listSurface ? R.dimen.header_list_min : R.dimen.header_min_height
        ));
        if (main) {
            header.setTitle(getString(titleFor(navigator.currentTab())));
            header.setForegroundColor(MaterialColors.getColor(header, com.google.android.material.R.attr.colorOnSurface));
            header.setBackgroundColor(Color.TRANSPARENT);
            root.setBackgroundColor(themeBackground);
        } else if (!login) {
            header.setTitle(detailTitle(screen));
            header.setForegroundColor(getColor(R.color.yakyeon_text));
            int surface = getColor(R.color.white);
            header.setBackgroundColor(surface);
            root.setBackgroundColor(surface);
        } else {
            root.setBackgroundColor(getColor(R.color.white));
        }
        if (main || RegistrationChrome.usesCommentAction(screen)) {
            header.setActionIcon(R.drawable.ic_tab_calendar, v -> openCalendar());
        } else if (!login && !navigator.isInAuthFlow() && !showingSchedule()) {
            header.setActionIcon(R.drawable.ic_tab_calendar, v -> openCalendar());
        } else {
            header.setAction(null, null);
        }
        header.setActionIconPlate(listSurface || MedScreen.SLOT.equals(screen));
        bindingSelection = true;
        bottomNav.setSelectedItemId(menuFor(navigator.currentTab()));
        bindingSelection = false;

        if (main) {
            prepareCareTabs();
        } else {
            disarmCareTabs();
        }
        if (needsServerHome(screen)) {
            ensureHomeLoaded();
        }
        if (isSignedIn() && showingSchedule()) {
            ensureScheduleLoaded();
        }
        if (isSignedIn() && MedScreen.HISTORY.equals(screen)) {
            ensureHistoryLoaded();
        }
        syncListPillbox(screen);

        screenContainer.removeAllViews();
        View content;
        if (login) {
            content = AuthScreens.login(this, this);
        } else if (AuthScreen.ACCOUNT_RECOVERY.equals(screen)) {
            content = AuthScreens.accountRecovery(this, this, authFlow.recoveryTab(), recoveryForm);
        } else if (AuthScreen.ACCOUNT_RECOVERY_VERIFY.equals(screen)) {
            content = AuthScreens.recoveryVerify(
                    this,
                    this,
                    recoveryForm,
                    authFlow.recoveryTab(),
                    phoneVerification,
                    verifiedPhone,
                    phoneVerified,
                    verificationExpiresAt
            );
        } else if (AuthScreen.ACCOUNT_RECOVERY_RESULT.equals(screen)) {
            content = AuthScreens.recoveryResult(this, this, recoveryForm.accounts());
        } else if (AuthScreen.ACCOUNT_RECOVERY_PASSWORD.equals(screen)) {
            content = AuthScreens.recoveryPassword(this, this, recoveryForm);
        } else if (AuthScreen.ACCOUNT_RECOVERY_DONE.equals(screen)) {
            content = AuthScreens.recoveryDone(this, this);
        } else if (AuthScreen.SIGNUP_TYPE.equals(screen)) {
            content = AuthScreens.signupType(
                    this,
                    YakyeonRepositoryProvider.get().signupDraft().getAccountType(),
                    this
            );
        } else if (AuthScreen.SIGNUP_FORM.equals(screen)) {
            content = AuthScreens.signupForm(
                    this,
                    this,
                    phoneVerification,
                    verifiedPhone,
                    phoneVerified,
                    verificationExpiresAt
            );
        } else if (main) {
            content = mainTab(navigator.currentTab());
        } else if (MedScreen.isMedication(screen)) {
            content = medicationScreen(screen);
        } else if (MainScreens.DEV_TOOLS.equals(screen)) {
            content = MainScreens.devTools(this, YakyeonRepositoryProvider.get().medications(), this);
        } else if (MainScreens.LINK.equals(screen)) {
            content = MainScreens.careLink(this, this, this);
        } else if (SettingsScreens.isSettings(screen)) {
            content = SettingsScreens.show(
                    this,
                    screen,
                    YakyeonRepositoryProvider.get().session(),
                    YakyeonRepositoryProvider.get().medications(),
                    this,
                    this
            );
        } else {
            content = MainScreens.pending(this, screen);
        }
        screenContainer.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        releaseScrollFocus(content);
        tickVerification();
        maybeSyncAccountScreens(navigator.currentScreen());
        if (AuthScreen.LOGIN.equals(navigator.currentScreen())) {
            setLoginEnabled(!loginInFlight);
        }
        if (progressText != null && (requestProgress == null || !requestProgress.isShown())) {
            showProgress(progressText);
        }
        syncConsentDialog(screen);
        syncAlarmDialog(screen);
        offerCatchUp();
    }

    private void syncConsentDialog(String screen) {
        if (!MedScreen.CONSENT.equals(screen)) {
            if (consentDialog != null) {
                dismissingConsent = true;
                consentDialog.dismiss();
                dismissingConsent = false;
                consentDialog = null;
            }
            return;
        }
        if (consentDialog != null && consentDialog.isShowing()) {
            return;
        }
        MedicationStore store = YakyeonRepositoryProvider.get().medications();
        consentDialog = MedScreens.consentDialog(this, store, this);
        consentDialog.setOnDismissListener(dialog -> {
            consentDialog = null;
            if (!dismissingConsent && MedScreen.CONSENT.equals(navigator.currentScreen())) {
                back();
            }
        });
        consentDialog.show();
    }

    private void syncAlarmDialog(String screen) {
        if (!MedScreen.ALARM.equals(screen)) {
            if (alarmDialog != null) {
                dismissingAlarm = true;
                alarmDialog.dismiss();
                dismissingAlarm = false;
                alarmDialog = null;
            }
            YakyeonRepositoryProvider.get().medications().cancelAlarmEdit();
            return;
        }
        if (alarmDialog != null && alarmDialog.isShowing()) {
            return;
        }
        MedicationStore store = YakyeonRepositoryProvider.get().medications();
        alarmDialog = MedScreens.alarmDialog(this, store, this);
        alarmDialog.setOnDismissListener(dialog -> {
            alarmDialog = null;
            if (!dismissingAlarm && MedScreen.ALARM.equals(navigator.currentScreen())) {
                YakyeonRepositoryProvider.get().medications().cancelAlarmEdit();
                back();
            }
        });
        alarmDialog.show();
    }

    private boolean isMedicationListSurface(String screen) {
        if (MedScreen.LIST.equals(screen) || MedScreen.CONSENT.equals(screen)) {
            return true;
        }
        return MedScreen.ALARM.equals(screen)
                && YakyeonRepositoryProvider.get().medications().isAlarmEdit();
    }

    private boolean debuggable() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private View mainTab(ScreenId tab) {
        YakyeonSession session = YakyeonRepositoryProvider.get().session();
        if (tab == ScreenId.GRAPH) {
            if (isSignedIn()) {
                return MainScreens.serverGraph(this, graphView, graphBundle, graphMessage, new MainScreens.GraphActions() {
                    @Override
                    public void onNode(String medicationId) {
                        showServerNode(medicationId);
                    }

                    @Override
                    public void onDurSeq(String durSeq) {
                        loadEdgeDetail(durSeq);
                    }
                });
            }
            return MainScreens.graph(this, YakyeonRepositoryProvider.get().medications());
        }
        if (tab == ScreenId.PILLBOX) {
            if (isSignedIn()) {
                return MainScreens.serverPillbox(
                        this,
                        pillboxView,
                        pillboxSnapshot,
                        pillboxMessage,
                        deviceWritesAllowed,
                        this
                );
            }
            return MainScreens.pillbox(this, YakyeonRepositoryProvider.get().medications(), this);
        }
        if (tab == ScreenId.SETTINGS) {
            return MainScreens.settings(this, session.getAccountType(), this);
        }
        if (tab == ScreenId.SCHEDULE) {
            return scheduleContent();
        }
        if (isSignedIn()) {
            ensureHomeName();
            return MainScreens.serverHome(
                    this,
                    homeView,
                    homeMessage,
                    homeSummary,
                    homeActive,
                    homeInteractions,
                    homeToday,
                    this,
                    debuggable(),
                    signedInName(),
                    awaitingPatientLink(),
                    homeLinkedPatientName
            );
        }
        MedicationStore store = YakyeonRepositoryProvider.get().medications();
        return MainScreens.home(this, session, store, this, debuggable(), signedInName(), null);
    }

    private View scheduleContent() {
        if (isSignedIn()) {
            return RecordScreens.serverSchedule(
                    this,
                    scheduleDate,
                    scheduleView,
                    scheduleMessage,
                    scheduleDoses,
                    homeWrites,
                    this
            );
        }
        return RecordScreens.schedule(this, YakyeonRepositoryProvider.get().medications(), this);
    }

    private View medicationScreen(String screen) {
        MedicationStore store = YakyeonRepositoryProvider.get().medications();
        YakyeonSession session = YakyeonRepositoryProvider.get().session();
        if (MedScreen.LIST.equals(screen) || MedScreen.CONSENT.equals(screen)) {
            if (isSignedIn()) {
                return MedScreens.serverList(
                        this,
                        prescriptionView(),
                        prescriptionMessage(),
                        prescriptionRows(),
                        listPillboxView,
                        listPillbox,
                        homeWrites,
                        this
                );
            }
            return MedScreens.list(this, store, this, debuggable());
        }
        if (MedScreen.ENVELOPE.equals(screen)) {
            return MedScreens.envelope(this, store, session, this, debuggable());
        }
        if (MedScreen.EDIT.equals(screen)) {
            return MedScreens.edit(this, store, session, this);
        }
        if (MedScreen.SLOT.equals(screen)) {
            if (serverSlotPrescriptionId != null && serverSlotPillbox != null) {
                return MedScreens.serverSlot(
                        this,
                        prescriptionFor(serverSlotPrescriptionId),
                        serverSlotPillbox,
                        serverSlotChoice,
                        serverSlotBusy,
                        this
                );
            }
            return MedScreens.slot(this, store, session, this);
        }
        if (MedScreen.ALARM.equals(screen)) {
            if (store.isAlarmEdit()) {
                return MedScreens.list(this, store, this, debuggable());
            }
            if (store.draft() != null && store.draft().getSlotIndex() != null) {
                return MedScreens.slot(this, store, session, this);
            }
            return MedScreens.envelope(this, store, session, this, debuggable());
        }
        if (MedScreen.SCHEDULE.equals(screen)) {
            return scheduleContent();
        }
        if (isSignedIn()) {
            return RecordScreens.serverHistory(
                    this,
                    historyView,
                    historyMessage,
                    historyStatus,
                    historyFrom,
                    historyTo,
                    historyDoses,
                    this
            );
        }
        return RecordScreens.history(this, store, this);
    }

    private String detailTitle(String screen) {
        if (AuthScreen.SIGNUP_TYPE.equals(screen) || AuthScreen.SIGNUP_FORM.equals(screen)) {
            return getString(R.string.auth_signup);
        }
        if (AuthScreen.ACCOUNT_RECOVERY.equals(screen)) {
            return recoveryTitle();
        }
        if (AuthScreen.ACCOUNT_RECOVERY_VERIFY.equals(screen)) {
            return getString(R.string.auth_verify_title);
        }
        if (AuthScreen.ACCOUNT_RECOVERY_RESULT.equals(screen)) {
            return getString(R.string.auth_find_result_title);
        }
        if (AuthScreen.ACCOUNT_RECOVERY_PASSWORD.equals(screen)
                || AuthScreen.ACCOUNT_RECOVERY_DONE.equals(screen)) {
            return getString(R.string.auth_reset_password);
        }
        if (MedScreen.ALARM.equals(screen)) {
            MedicationStore store = YakyeonRepositoryProvider.get().medications();
            if (store.isAlarmEdit()) {
                return getString(R.string.med_list_title);
            }
            if (store.draft() != null && store.draft().getSlotIndex() != null) {
                return getString(R.string.med_slot_title);
            }
            return getString(R.string.med_envelope_title);
        }
        String medication = MedScreens.titleFor(this, screen);
        if (medication != null) {
            return medication;
        }
        String settings = SettingsScreens.titleFor(this, screen);
        if (settings != null) {
            return settings;
        }
        return MainScreens.titleFor(this, screen);
    }

    private String missingSignupMessage(AuthValidation.SignupCheck check) {
        switch (check) {
            case MISSING_NAME:
                return getString(R.string.auth_need_name);
            case MISSING_BIRTHDATE:
                return getString(R.string.auth_need_birth);
            case MISSING_PHONE:
                return getString(R.string.auth_need_phone);
            case MISSING_ID:
                return getString(R.string.auth_need_id);
            case MISSING_PASSWORD:
                return getString(R.string.auth_need_password);
            case MISSING_PASSWORD_CONFIRM:
                return getString(R.string.auth_need_password_confirm);
            default:
                return getString(R.string.auth_need_type);
        }
    }

    private static int titleFor(ScreenId screen) {
        switch (screen) {
            case HOME:
                return R.string.tab_home;
            case SCHEDULE:
                return R.string.tab_schedule;
            case GRAPH:
                return R.string.tab_graph;
            case PILLBOX:
                return R.string.pillbox_screen_title;
            case SETTINGS:
                return R.string.tab_settings;
            default:
                return R.string.tab_home;
        }
    }

    @Override
    public boolean isSignedIn() {
        return AuthTokenStore.hasSession(this);
    }

    @Override
    public ProfileSnapshot currentProfile() {
        return AuthTokenStore.profile(this);
    }

    private String signedInName() {
        ProfileSnapshot profile = currentProfile();
        if (profile == null || profile.name == null) {
            return null;
        }
        String name = profile.name.trim();
        return name.isEmpty() ? null : name;
    }

    private void ensureHomeName() {
        if (signedInName() != null || homeNameRequested) {
            return;
        }
        homeNameRequested = true;
        ApiClient.execute(() -> {
            try {
                ProfileSnapshot profile = UserApi.profile(getApplicationContext());
                runOnUiThread(() -> {
                    if (!isAlive()) {
                        return;
                    }
                    AuthTokenStore.saveProfile(this, profile);
                    applyStoredAccountType();
                    if (navigator.isMainArea() && navigator.currentTab() == ScreenId.HOME) {
                        refresh();
                    }
                });
            } catch (ApiException exception) {
                homeNameRequested = false;
            }
        });
    }

    @Override
    public void submitName(String name) {
        if (name == null || name.trim().isEmpty()) {
            showNotice(getString(R.string.settings_name_edit), getString(R.string.settings_name_empty));
            return;
        }
        String trimmed = name.trim();
        if (trimmed.length() > 50) {
            showNotice(getString(R.string.settings_name_edit), getString(R.string.settings_name_too_long));
            return;
        }
        if (!beginAccountRequest(getString(R.string.settings_name_saving))) {
            return;
        }
        ApiClient.execute(() -> {
            try {
                ProfileSnapshot profile = UserApi.updateName(getApplicationContext(), trimmed);
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    AuthTokenStore.saveProfile(this, profile);
                    applyStoredAccountType();
                    if (MainScreens.PROFILE.equals(navigator.currentScreen())) {
                        refresh();
                    }
                    showNotice(getString(R.string.settings_name_edit), getString(R.string.settings_name_saved), null);
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    showApiFailure(getString(R.string.settings_name_edit), exception);
                });
            }
        });
    }

    @Override
    public void submitPassword(String currentPassword, String newPassword, String newPasswordConfirm) {
        if (currentPassword == null || currentPassword.isEmpty()
                || newPassword == null || newPassword.isEmpty()
                || newPasswordConfirm == null || newPasswordConfirm.isEmpty()) {
            showNotice(getString(R.string.settings_password), getString(R.string.settings_password_empty));
            return;
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            showNotice(getString(R.string.settings_password), getString(R.string.settings_password_mismatch));
            return;
        }
        if (newPassword.length() < 8 || newPassword.length() > 64) {
            showNotice(getString(R.string.settings_password), getString(R.string.settings_password_length));
            return;
        }
        if (!beginAccountRequest(getString(R.string.settings_password_saving))) {
            return;
        }
        ApiClient.execute(() -> {
            try {
                AuthApi.changePassword(
                        getApplicationContext(),
                        currentPassword,
                        newPassword,
                        newPasswordConfirm
                );
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    clearLocalAccount();
                    showLoginScreen();
                    showNotice(getString(R.string.settings_password), getString(R.string.settings_password_changed), null);
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    showApiFailure(getString(R.string.settings_password), exception);
                });
            }
        });
    }

    @Override
    public void logout() {
        if (!beginAccountRequest(getString(R.string.settings_logout_loading))) {
            return;
        }
        ApiClient.execute(() -> {
            try {
                FcmRegistrar.deleteStored(getApplicationContext());
                boolean serverCalled = AuthApi.logout(getApplicationContext());
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    clearLocalAccount();
                    showLoginScreen();
                    if (!serverCalled) {
                        return;
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    if (exception.endsSession()) {
                        clearLocalAccount();
                        showLoginScreen();
                    }
                    showApiFailure(getString(R.string.settings_logout), exception);
                });
            }
        });
    }

    @Override
    public void submitAccountDelete(String password) {
        if (password == null || password.isEmpty()) {
            showNotice(getString(R.string.settings_withdraw_account), getString(R.string.settings_password_empty));
            return;
        }
        if (!beginAccountRequest(getString(R.string.settings_delete_loading))) {
            return;
        }
        ApiClient.execute(() -> {
            try {
                AuthApi.deleteAccount(getApplicationContext(), password);
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    clearLocalAccount();
                    showLoginScreen();
                    showNotice(
                            getString(R.string.settings_withdraw_account),
                            getString(R.string.settings_account_delete_done),
                            null
                    );
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    showApiFailure(getString(R.string.settings_withdraw_account), exception);
                });
            }
        });
    }

    @Override
    public void commitNotificationEdit(Runnable apply) {
        NotificationPreferences preferences = YakyeonRepositoryProvider.get().session().notifications();
        String before = preferences.exportState();
        boolean[] snapshot = NotificationSync.capture(preferences);
        apply.run();
        if (!isSignedIn()) {
            LocalAppSettings.saveNotifications(this, preferences);
            return;
        }
        java.util.List<NotificationSync.Item> changes = NotificationSync.diff(snapshot, preferences);
        preferences.restoreState(before);
        if (!notificationReady || notificationRequestInFlight) {
            refresh();
            if (!notificationReady) {
                showNotice(getString(R.string.settings_noti), getString(R.string.settings_noti_wait));
            }
            return;
        }
        if (changes.isEmpty()) {
            refresh();
            return;
        }
        refresh();
        notificationRequestInFlight = true;
        notificationEpoch++;
        showProgress(getString(R.string.settings_noti_saving));
        ApiClient.execute(() -> {
            try {
                java.util.List<NotificationSync.Item> saved = UserApi.updateNotificationSettings(
                        getApplicationContext(),
                        changes
                );
                runOnUiThread(() -> {
                    notificationRequestInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    NotificationSync.apply(saved, preferences);
                    LocalAppSettings.saveNotifications(this, preferences);
                    notificationReady = true;
                    String screen = navigator.currentScreen();
                    if (MainScreens.NOTIFICATIONS.equals(screen) || SettingsScreens.NOTI_DETAIL.equals(screen)) {
                        refresh();
                    }
                    ShortMessage.show(screenContainer, getString(R.string.settings_noti_saved));
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    notificationRequestInFlight = false;
                    clearProgress();
                    showApiFailure(getString(R.string.settings_noti), exception);
                });
            }
        });
    }

    private void submitLogin(String loginId, String password) {
        if (loginInFlight) {
            return;
        }
        loginInFlight = true;
        setLoginEnabled(false);
        showProgress(getString(R.string.auth_login_loading));
        ApiClient.execute(() -> {
            try {
                AuthApi.login(getApplicationContext(), loginId, password);
                FcmRegistrar.syncStored(getApplicationContext());
                runOnUiThread(() -> {
                    loginInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    enterHome();
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    loginInFlight = false;
                    clearProgress();
                    setLoginEnabled(true);
                    showApiFailure(getString(R.string.auth_login), exception);
                });
            }
        });
    }

    private void restoreSession() {
        ApiClient.execute(() -> {
            try {
                AuthApi.refresh(getApplicationContext());
                FcmRegistrar.syncStored(getApplicationContext());
                runOnUiThread(() -> {
                    if (!isAlive()) {
                        return;
                    }
                    if (AuthScreen.LOGIN.equals(navigator.currentScreen())) {
                        enterHome();
                    } else {
                        applyStoredAccountType();
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    if (!isAlive()) {
                        return;
                    }
                    if (exception.endsSession()) {
                        clearLocalAccount();
                    }
                    showApiFailure(getString(R.string.auth_login), exception);
                });
            }
        });
    }

    private boolean returnedFromBackground;
    private long doseSurfacesReloadedAt;
    private long ignoreBoxTakenUntil;
    private boolean boxTakenWhileWriting;
    private final BoxTakenRefresh.Host boxTakenHost = this::onBoxTakenRefresh;

    @Override
    protected void onPause() {
        returnedFromBackground = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean pendingBoxTaken = BoxTakenRefresh.consumePending();
        Log.i("BoxTaken", "onResume pendingConsumed=" + pendingBoxTaken
                + " fromBackground=" + returnedFromBackground
                + " signedIn=" + isSignedIn());
        armHomeDateWatch();
        boolean dateChanged = noteHomeDate();
        if (!returnedFromBackground || !isSignedIn() || !isAlive()) {
            if (dateChanged && isAlive()) {
                rollHomeToToday();
            }
            return;
        }
        returnedFromBackground = false;
        FcmRegistrar.syncStored(this);
        String screen = navigator.currentScreen();
        if (MainScreens.NOTIFICATIONS.equals(screen) || SettingsScreens.NOTI_DETAIL.equals(screen)) {
            refresh();
        }
        reloadVisibleShareScope();
    }

    private void reloadVisibleShareScope() {
        String screen = navigator.currentScreen();
        if (isSignedIn() && !doseSurfacesJustReloaded()) {
            invalidateDoseCaches();
            if (needsServerHome(screen)) {
                ensureHomeLoaded();
            }
            if (showingSchedule()) {
                ensureScheduleLoaded();
            }
            if (MedScreen.HISTORY.equals(screen)) {
                ensureHistoryLoaded();
            }
        }
        if (navigator.isMainArea() && navigator.currentTab() == ScreenId.GRAPH) {
            graphArmed = false;
            prepareCareTabs();
        } else if (navigator.isMainArea() && navigator.currentTab() == ScreenId.PILLBOX) {
            pillboxArmed = false;
            prepareCareTabs();
        }
        if (MainScreens.LINK.equals(screen) || SettingsScreens.PILLBOX.equals(screen)) {
            accountSyncKey = null;
            maybeSyncAccountScreens(screen);
        }
    }

    private void maybeSyncAccountScreens(String screen) {
        if (!isSignedIn()) {
            accountSyncKey = null;
            notificationReady = false;
            careReady = false;
            agreementsReady = false;
            deviceReady = false;
            devicePanel = null;
            return;
        }
        if (MainScreens.PROFILE.equals(screen)) {
            if (!"profile".equals(accountSyncKey)) {
                accountSyncKey = "profile";
                loadProfile();
            }
            return;
        }
        if (MainScreens.NOTIFICATIONS.equals(screen) || SettingsScreens.NOTI_DETAIL.equals(screen)) {
            if (!"noti".equals(accountSyncKey)) {
                accountSyncKey = "noti";
                notificationReady = false;
                loadNotificationSettings();
            }
            return;
        }
        if (MainScreens.LEGAL.equals(screen)) {
            if (!"legal".equals(accountSyncKey)) {
                accountSyncKey = "legal";
                agreementsReady = false;
                loadAgreements();
            }
            return;
        }
        if (MainScreens.LINK.equals(screen)) {
            if (!"care".equals(accountSyncKey)) {
                accountSyncKey = "care";
                careReady = false;
                loadCareLinks();
            }
            return;
        }
        if (SettingsScreens.PILLBOX.equals(screen)) {
            if (!"device".equals(accountSyncKey)) {
                accountSyncKey = "device";
                deviceReady = false;
                devicePanel = null;
                loadDevice();
            }
            return;
        }
        accountSyncKey = null;
        notificationReady = false;
    }

    private void loadProfile() {
        showProgress(getString(R.string.settings_profile_loading));
        ApiClient.execute(() -> {
            try {
                ProfileSnapshot profile = UserApi.profile(getApplicationContext());
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive()) {
                        return;
                    }
                    AuthTokenStore.saveProfile(this, profile);
                    applyStoredAccountType();
                    if (MainScreens.PROFILE.equals(navigator.currentScreen())) {
                        refresh();
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive()) {
                        return;
                    }
                    if (MainScreens.PROFILE.equals(navigator.currentScreen())) {
                        showApiFailure(getString(R.string.settings_profile), exception);
                    }
                });
            }
        });
    }

    private void loadNotificationSettings() {
        int epoch = notificationEpoch;
        showProgress(getString(R.string.settings_noti_loading));
        ApiClient.execute(() -> {
            try {
                java.util.List<NotificationSync.Item> items = UserApi.notificationSettings(getApplicationContext());
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive() || epoch != notificationEpoch) {
                        return;
                    }
                    NotificationSync.apply(items, YakyeonRepositoryProvider.get().session().notifications());
                    LocalAppSettings.saveNotifications(
                            this,
                            YakyeonRepositoryProvider.get().session().notifications()
                    );
                    notificationReady = true;
                    String screen = navigator.currentScreen();
                    if (MainScreens.NOTIFICATIONS.equals(screen) || SettingsScreens.NOTI_DETAIL.equals(screen)) {
                        refresh();
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive() || epoch != notificationEpoch) {
                        return;
                    }
                    notificationReady = false;
                    String screen = navigator.currentScreen();
                    if (MainScreens.NOTIFICATIONS.equals(screen) || SettingsScreens.NOTI_DETAIL.equals(screen)) {
                        showApiFailure(getString(R.string.settings_noti), exception);
                    }
                });
            }
        });
    }

    private void clearLoadProgress() {
        if (loginInFlight || authRequestInFlight || accountRequestInFlight
                || notificationRequestInFlight || deviceRequestInFlight) {
            return;
        }
        clearProgress();
    }

    private void enterHome() {
        applyStoredAccountType();
        navigator.selectTab(ScreenId.HOME);
        render();
        if (pendingPushId != null && !pendingPushId.isEmpty()) {
            String id = pendingPushId;
            pendingPushId = null;
            loadPushDetail(id);
        }
    }

    private void showLoginScreen() {
        accountSyncKey = null;
        notificationReady = false;
        authFlow.start();
        render();
    }

    private void applyStoredAccountType() {
        AccountType type = AccountRoles.toLocal(AuthTokenStore.accountType(this));
        if (type == null) {
            return;
        }
        YakyeonRepositoryProvider.get().session().setAccountType(type);
        LocalAppSettings.saveAccountType(this, type);
    }

    private void clearLocalAccount() {
        FcmRegistrar.noteSessionEnded(this);
        LocalAccountData.wipe(this);
        YakyeonRepositoryProvider.get().medications().discardAccountData();
        YakyeonRepositoryProvider.get().medicineNames().restore(java.util.Collections.<String>emptyList());
        YakyeonRepositoryProvider.get().session().notifications().reset();
        YakyeonRepositoryProvider.get().session().setAccountType(null);
        LocalAppSettings.saveAccountType(this, null);
        accountSyncKey = null;
        notificationReady = false;
        careReady = false;
        agreementsReady = false;
        deviceReady = false;
        homeNameRequested = false;
        devicePanel = null;
        deviceWritesAllowed = false;
        compartmentReleaseInFlight = false;
        deviceStatusMessage = null;
        clearDeviceDraft();
        settingsPatientId = null;
        graphArmed = false;
        graphEpoch++;
        graphView = MainScreens.CareView.LOADING;
        graphBundle = null;
        graphMessage = null;
        pillboxArmed = false;
        pillboxEpoch++;
        pillboxView = MainScreens.CareView.LOADING;
        pillboxSnapshot = null;
        pillboxMessage = null;
        clearHomeCache();
        managedLines.clear();
        guardianLines.clear();
        agreementRecords.clear();
        agreementMarks = new LegalMarks();
        settingsEpoch++;
    }

    private boolean beginAccountRequest(String message) {
        if (accountRequestInFlight) {
            return false;
        }
        accountRequestInFlight = true;
        showProgress(message);
        return true;
    }

    private void finishAccountRequest() {
        accountRequestInFlight = false;
        clearProgress();
    }

    private void showProgress(String message) {
        progressText = message;
        hideProgress();
        if (screenContainer == null) {
            return;
        }
        requestProgress = Snackbar.make(screenContainer, message, Snackbar.LENGTH_INDEFINITE);
        requestProgress.getView().setFocusable(false);
        requestProgress.getView().setFocusableInTouchMode(false);
        requestProgress.show();
    }

    private void hideProgress() {
        if (requestProgress != null) {
            requestProgress.dismiss();
            requestProgress = null;
        }
    }

    private void clearProgress() {
        progressText = null;
        hideProgress();
    }

    private void setLoginEnabled(boolean enabled) {
        View button = screenContainer.findViewById(R.id.button_login);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    private void showApiFailure(String title, ApiException exception) {
        if (!isAlive()) {
            return;
        }
        if (exception.endsSession()) {
            clearLocalAccount();
            if (!navigator.isInAuthFlow()) {
                showLoginScreen();
            }
        }
        showNotice(title, messageFor(exception), null);
    }

    private String messageFor(ApiException exception) {
        if (ApiCodes.LOGIN_LOCKED.equals(exception.code())) {
            return getString(R.string.auth_login_locked);
        }
        ApiFailure.Kind kind = exception.failure();
        if (kind == ApiFailure.Kind.NETWORK) {
            return getString(R.string.api_network_error);
        }
        if (kind == ApiFailure.Kind.RESPONSE) {
            if (exception.status() == 404) {
                return getString(R.string.api_not_deployed);
            }
            return getString(R.string.api_response_error);
        }
        if (kind == ApiFailure.Kind.SERVER) {
            return textOr(exception.getMessage(), getString(R.string.api_server_error));
        }
        if (ApiMessages.prefersServerText(exception.code())
                && exception.getMessage() != null
                && !exception.getMessage().isEmpty()) {
            return exception.getMessage();
        }
        String fallback = ApiMessages.fallback(exception.code());
        if (exception.getMessage() != null && !exception.getMessage().isEmpty()) {
            return exception.getMessage();
        }
        if (fallback != null) {
            return fallback;
        }
        if (kind == ApiFailure.Kind.VALIDATION) {
            return getString(R.string.api_validation_error);
        }
        return getString(R.string.api_request_failed);
    }

    private static String textOr(String value, String fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return value;
    }

    private boolean isAlive() {
        return !isFinishing() && !isDestroyed();
    }

    private boolean closeKeyboardInsteadOfLeaving() {
        if (root == null) {
            return false;
        }
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
        if (insets == null || !insets.isVisible(WindowInsetsCompat.Type.ime())) {
            return false;
        }
        View target = getCurrentFocus();
        if (target == null) {
            target = root;
        }
        InputMethodManager input = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (input != null) {
            input.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
        return true;
    }

    private static void releaseScrollFocus(View view) {
        if (view instanceof ScrollView) {
            view.setFocusable(false);
            view.setFocusableInTouchMode(false);
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            releaseScrollFocus(group.getChildAt(i));
        }
    }

    private void scrollFocusedFieldIntoView() {
        View focused = getCurrentFocus();
        if (!(focused instanceof EditText) || focused.getWidth() <= 0 || focused.getHeight() <= 0) {
            return;
        }
        Rect rect = new Rect(0, 0, focused.getWidth(), focused.getHeight());
        int margin = Math.round(16f * getResources().getDisplayMetrics().density);
        rect.inset(0, -margin);
        focused.requestRectangleOnScreen(rect, false);
    }

    @Override
    public boolean agreementsReady() {
        return agreementsReady;
    }

    @Override
    public LegalMarks agreementMarks() {
        return agreementMarks;
    }

    @Override
    public void submitAgreement(String termsType, boolean withdrawn) {
        if (!isSignedIn()) {
            showNotice(getString(R.string.settings_legal), getString(R.string.settings_login_required));
            return;
        }
        if (!agreementsReady) {
            showNotice(getString(R.string.settings_legal), getString(R.string.settings_agreement_wait));
            return;
        }
        if (withdrawn) {
            Long versionId = SettingsContracts.versionId(agreementRecords, termsType);
            if (versionId == null) {
                showNotice(getString(R.string.settings_legal), getString(R.string.auth_terms_missing));
                return;
            }
            sendAgreement(termsType, versionId, true);
            return;
        }
        ConfirmDialog.show(
                this,
                getString(R.string.settings_withdraw_title),
                getString(R.string.settings_withdraw_body),
                getString(R.string.settings_withdraw_confirm),
                getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        sendAgreement(termsType, null, false);
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    @Override
    public boolean careReady() {
        return careReady;
    }

    @Override
    public boolean showGuardianSection() {
        return showGuardians;
    }

    @Override
    public java.util.List<SettingsScreens.LinkLine> managedLinks() {
        return managedLines;
    }

    @Override
    public java.util.List<SettingsScreens.LinkLine> guardianLinks() {
        return guardianLines;
    }

    @Override
    public void unlinkCareLink(long linkId) {
        if (!beginAccountRequest(getString(R.string.settings_logout_loading))) {
            return;
        }
        ApiClient.execute(() -> {
            try {
                SettingsApi.unlink(getApplicationContext(), linkId);
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    careReady = false;
                    accountSyncKey = null;
                    if (MainScreens.LINK.equals(navigator.currentScreen())) {
                        refresh();
                    }
                    ShortMessage.show(screenContainer, getString(R.string.care_link_removed));
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    showApiFailure(getString(R.string.settings_link), exception);
                });
            }
        });
    }

    @Override
    public SettingsScreens.DevicePanel devicePanel() {
        return devicePanel;
    }

    @Override
    public boolean deviceReady() {
        return deviceReady;
    }

    @Override
    public boolean deviceWritesAllowed() {
        return deviceWritesAllowed;
    }

    @Override
    public String deviceStatusMessage() {
        return deviceStatusMessage;
    }

    @Override
    public void confirmDeviceConsent() {
        if (!isSignedIn()) {
            return;
        }
        String serial = deviceSerialDraft == null ? "" : deviceSerialDraft;
        String secretValue = deviceSecretDraft == null ? "" : deviceSecretDraft;
        if (serial.trim().isEmpty() || secretValue.trim().isEmpty()) {
            showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_register_need_both));
            return;
        }
        if (!beginAccountRequest(getString(R.string.pillbox_register_loading))) {
            return;
        }
        long remembered = pendingDevicePatientId == null ? 0L : pendingDevicePatientId;
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                SettingsContracts.ManagedPatient patient;
                if (remembered > 0L) {
                    patient = findPatient(patients, remembered);
                    if (patient == null) {
                        finishDeviceNotice(getString(R.string.settings_patient_none));
                        return;
                    }
                } else {
                    SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                            patients,
                            accountType
                    );
                    if (target.resolution != SettingsContracts.Resolution.ONE) {
                        finishDeviceNotice(getString(target.resolution == SettingsContracts.Resolution.SEVERAL
                                ? R.string.settings_patient_several
                                : R.string.settings_patient_none));
                        return;
                    }
                    patient = findPatient(patients, target.patientId);
                }
                if (patient == null) {
                    finishDeviceNotice(getString(R.string.settings_patient_none));
                    return;
                }
                if (!CareContracts.writesAllowed(patient)) {
                    finishDeviceNotice(getString(R.string.pillbox_read_only));
                    return;
                }
                if (CareContracts.shareWithheld(patient, CareContracts.DEVICE_EVENT)) {
                    finishDeviceNotice(getString(R.string.care_share_withheld));
                    return;
                }
                long patientId = patient.patientId;
                pendingDevicePatientId = patientId;
                java.util.List<SettingsContracts.AgreementRecord> records = SettingsApi.agreements(
                        getApplicationContext()
                );
                SettingsContracts.AgreementRecord deviceTerms = deviceAgreement(records);
                if (deviceTerms == null || !deviceTerms.agreed) {
                    Long versionId;
                    try {
                        versionId = HomeApi.deviceTermsVersion(getApplicationContext());
                    } catch (ApiException termsError) {
                        failDeviceConsent(termsError);
                        return;
                    }
                    if (versionId == null) {
                        finishDeviceNotice(getString(R.string.pillbox_consent_terms_missing));
                        return;
                    }
                    try {
                        SettingsApi.agree(getApplicationContext(), versionId, true);
                    } catch (ApiException agreeError) {
                        failDeviceConsent(agreeError);
                        return;
                    }
                }
                try {
                    CareApi.registerDevice(
                            getApplicationContext(),
                            patientId,
                            serial,
                            secretValue,
                            null
                    );
                } catch (ApiException registerError) {
                    runOnUiThread(() -> {
                        finishDeviceRegisterAttempt();
                        if (!isAlive()) {
                            return;
                        }
                        if (registerError.endsSession()) {
                            showApiFailure(getString(R.string.pillbox_settings), registerError);
                            return;
                        }
                        String specific = ApiMessages.deviceRegister(
                                registerError.status(),
                                registerError.code()
                        );
                        showNotice(
                                getString(R.string.pillbox_settings),
                                specific != null ? specific : careMessage(registerError)
                        );
                    });
                    return;
                }
                runOnUiThread(this::onDeviceRegistered);
            } catch (ApiException exception) {
                failDeviceConsent(exception);
            }
        });
    }

    @Override
    public void setDeviceFlag(String field, boolean enabled) {
        if (!deviceWritesAllowed) {
            showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_read_only));
            if (SettingsScreens.PILLBOX.equals(navigator.currentScreen())) {
                refresh();
            }
            return;
        }
        if (deviceRequestInFlight || settingsPatientId == null) {
            refresh();
            return;
        }
        deviceRequestInFlight = true;
        long patientId = settingsPatientId;
        showProgress(getString(R.string.settings_noti_saving));
        ApiClient.execute(() -> {
            try {
                SettingsContracts.DeviceSnapshot snapshot = SettingsApi.updateDevice(
                        getApplicationContext(),
                        patientId,
                        field,
                        enabled
                );
                runOnUiThread(() -> {
                    deviceRequestInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    devicePanel = panelFrom(snapshot);
                    if (SettingsScreens.PILLBOX.equals(navigator.currentScreen())) {
                        refresh();
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    deviceRequestInFlight = false;
                    clearProgress();
                    if (SettingsScreens.PILLBOX.equals(navigator.currentScreen())) {
                        refresh();
                    }
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.pillbox_settings), exception);
                        return;
                    }
                    showNotice(getString(R.string.pillbox_settings), careMessage(exception));
                });
            }
        });
    }

    private void prepareCareTabs() {
        if (!isSignedIn()) {
            disarmCareTabs();
            return;
        }
        if (navigator.currentTab() == ScreenId.GRAPH) {
            if (pillboxArmed) {
                pillboxArmed = false;
                pillboxEpoch++;
            }
            if (!graphArmed) {
                graphArmed = true;
                graphEpoch++;
                graphView = MainScreens.CareView.LOADING;
                graphBundle = null;
                graphMessage = null;
                loadGraph(graphEpoch);
            }
            return;
        }
        if (navigator.currentTab() == ScreenId.PILLBOX) {
            if (graphArmed) {
                graphArmed = false;
                graphEpoch++;
            }
            if (!pillboxArmed) {
                pillboxArmed = true;
                pillboxEpoch++;
                pillboxView = MainScreens.CareView.LOADING;
                pillboxSnapshot = null;
                pillboxMessage = null;
                loadPillbox(pillboxEpoch);
            }
            return;
        }
        disarmCareTabs();
    }

    private void disarmCareTabs() {
        if (graphArmed) {
            graphArmed = false;
            graphEpoch++;
        }
        if (pillboxArmed) {
            pillboxArmed = false;
            pillboxEpoch++;
        }
    }

    private void loadGraph(int epoch) {
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                        patients,
                        accountType
                );
                if (target.resolution != SettingsContracts.Resolution.ONE) {
                    String text = getString(target.resolution == SettingsContracts.Resolution.SEVERAL
                            ? R.string.settings_patient_several
                            : R.string.settings_patient_none);
                    runOnUiThread(() -> applyGraph(epoch, MainScreens.CareView.BLOCKED, null, text, false, null));
                    return;
                }
                SettingsContracts.ManagedPatient patient = findPatient(patients, target.patientId);
                if (CareContracts.shareWithheld(patient, CareContracts.DUR_INFO)) {
                    runOnUiThread(() -> applyGraph(
                            epoch,
                            MainScreens.CareView.BLOCKED,
                            null,
                            getString(R.string.care_share_withheld),
                            CareContracts.writesAllowed(patient),
                            target.patientId
                    ));
                    return;
                }
                CareContracts.Summary summary = CareApi.summary(getApplicationContext(), target.patientId);
                CareContracts.GraphDocumentModel graph = CareApi.graph(getApplicationContext(), target.patientId);
                java.util.List<CareContracts.InteractionRow> interactions = CareApi.interactions(
                        getApplicationContext(),
                        target.patientId
                );
                CareContracts.requireConsistentCounts(summary, graph, interactions);
                CareContracts.GraphBundle bundle = new CareContracts.GraphBundle(summary, graph, interactions);
                runOnUiThread(() -> applyGraph(
                        epoch,
                        MainScreens.CareView.READY,
                        bundle,
                        null,
                        CareContracts.writesAllowed(patient),
                        target.patientId
                ));
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    if (exception.endsSession()) {
                        applyGraph(epoch, MainScreens.CareView.FAILED, null, careMessage(exception), false, null);
                        showApiFailure(getString(R.string.tab_graph), exception);
                        return;
                    }
                    MainScreens.CareView view = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                            ? MainScreens.CareView.BLOCKED
                            : MainScreens.CareView.FAILED;
                    String text = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                            ? getString(R.string.care_share_withheld)
                            : careMessage(exception);
                    applyGraph(epoch, view, null, text, deviceWritesAllowed, settingsPatientId);
                });
            }
        });
    }

    private void applyGraph(
            int epoch,
            MainScreens.CareView view,
            CareContracts.GraphBundle bundle,
            String message,
            boolean writes,
            Long patientId
    ) {
        if (!isAlive() || epoch != graphEpoch) {
            return;
        }
        graphView = view;
        graphBundle = bundle;
        graphMessage = message;
        deviceWritesAllowed = writes;
        settingsPatientId = patientId;
        if (navigator.isMainArea() && navigator.currentTab() == ScreenId.GRAPH) {
            refresh();
        }
    }

    private void loadPillbox(int epoch) {
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                        patients,
                        accountType
                );
                if (target.resolution != SettingsContracts.Resolution.ONE) {
                    String text = getString(target.resolution == SettingsContracts.Resolution.SEVERAL
                            ? R.string.settings_patient_several
                            : R.string.settings_patient_none);
                    runOnUiThread(() -> applyPillbox(epoch, MainScreens.CareView.BLOCKED, null, text, false, null));
                    return;
                }
                SettingsContracts.ManagedPatient patient = findPatient(patients, target.patientId);
                boolean writes = CareContracts.writesAllowed(patient);
                long patientId = target.patientId;
                if (CareContracts.shareWithheld(patient, CareContracts.DEVICE_EVENT)) {
                    runOnUiThread(() -> applyPillbox(
                            epoch,
                            MainScreens.CareView.BLOCKED,
                            null,
                            getString(R.string.care_share_withheld),
                            writes,
                            patientId
                    ));
                    return;
                }
                try {
                    CareContracts.Pillbox loaded = CareApi.pillbox(getApplicationContext(), patientId);
                    runOnUiThread(() -> applyPillbox(
                            epoch,
                            MainScreens.CareView.READY,
                            loaded,
                            null,
                            writes,
                            patientId
                    ));
                } catch (ApiException exception) {
                    runOnUiThread(() -> {
                        if (exception.endsSession()) {
                            applyPillbox(epoch, MainScreens.CareView.FAILED, null, careMessage(exception), writes, patientId);
                            showApiFailure(getString(R.string.tab_pillbox), exception);
                            return;
                        }
                        if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                            forgetLocalPillbox();
                            applyPillbox(epoch, MainScreens.CareView.ABSENT, null, null, writes, patientId);
                            return;
                        }
                        MainScreens.CareView failedView = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                                ? MainScreens.CareView.BLOCKED
                                : MainScreens.CareView.FAILED;
                        String failedText = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                                ? getString(R.string.care_share_withheld)
                                : careMessage(exception);
                        applyPillbox(epoch, failedView, null, failedText, writes, patientId);
                    });
                }
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    if (exception.endsSession()) {
                        applyPillbox(epoch, MainScreens.CareView.FAILED, null, careMessage(exception), false, null);
                        showApiFailure(getString(R.string.tab_pillbox), exception);
                        return;
                    }
                    if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                        forgetLocalPillbox();
                        applyPillbox(epoch, MainScreens.CareView.ABSENT, null, null, false, null);
                        return;
                    }
                    MainScreens.CareView view = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                            ? MainScreens.CareView.BLOCKED
                            : MainScreens.CareView.FAILED;
                    String text = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                            ? getString(R.string.care_share_withheld)
                            : careMessage(exception);
                    applyPillbox(epoch, view, null, text, deviceWritesAllowed, settingsPatientId);
                });
            }
        });
    }

    private void applyPillbox(
            int epoch,
            MainScreens.CareView view,
            CareContracts.Pillbox snapshot,
            String message,
            boolean writes,
            Long patientId
    ) {
        if (!isAlive() || epoch != pillboxEpoch) {
            return;
        }
        pillboxView = view;
        pillboxSnapshot = snapshot;
        pillboxMessage = message;
        deviceWritesAllowed = writes;
        settingsPatientId = patientId;
        if (navigator.isMainArea() && navigator.currentTab() == ScreenId.PILLBOX) {
            refresh();
        }
    }

    private void showServerNode(String medicationId) {
        if (graphBundle == null || graphBundle.graph == null || medicationId == null) {
            return;
        }
        CareContracts.GraphNode selected = null;
        for (CareContracts.GraphNode node : graphBundle.graph.nodes) {
            if (medicationId.equals(Long.toString(node.medicationId))) {
                selected = node;
                break;
            }
        }
        if (selected == null) {
            return;
        }
        String message = getString(R.string.graph_node_selected);
        if (!selected.checked) {
            message = message + "\n" + getString(R.string.graph_unchecked_node);
        }
        showNotice(selected.drugName, message);
    }

    private void loadEdgeDetail(String durSeq) {
        if (durSeq == null || durSeq.isEmpty() || accountRequestInFlight) {
            return;
        }
        if (!beginAccountRequest(getString(R.string.graph_loading))) {
            return;
        }
        ApiClient.execute(() -> {
            try {
                CareContracts.EdgeDetail detail = CareApi.edge(getApplicationContext(), durSeq);
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    showNotice(
                            getString(R.string.graph_detail_title),
                            getString(
                                    R.string.graph_edge_detail,
                                    detail.reasonText,
                                    detail.sourceName,
                                    detail.sourceBaseDate,
                                    detail.precaution,
                                    detail.disclaimer
                            )
                    );
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.graph_detail_title), exception);
                        return;
                    }
                    showNotice(getString(R.string.graph_detail_title), careMessage(exception));
                });
            }
        });
    }

    private String careMessage(ApiException exception) {
        if (ApiCodes.LOGIN_LOCKED.equals(exception.code())) {
            return getString(R.string.auth_login_locked);
        }
        ApiFailure.Kind kind = exception.failure();
        if (kind == ApiFailure.Kind.NETWORK) {
            return getString(R.string.api_network_error);
        }
        if (kind == ApiFailure.Kind.RESPONSE) {
            if (exception.status() == 404) {
                return getString(R.string.api_not_deployed);
            }
            return getString(R.string.api_response_error);
        }
        if (kind == ApiFailure.Kind.SERVER) {
            return getString(R.string.api_server_error);
        }
        if (ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())) {
            return getString(R.string.care_share_withheld);
        }
        if (ApiMessages.prefersServerText(exception.code())
                && exception.getMessage() != null
                && !exception.getMessage().isEmpty()) {
            return exception.getMessage();
        }
        String fallback = ApiMessages.fallback(exception.code());
        if (fallback != null) {
            return fallback;
        }
        if (kind == ApiFailure.Kind.VALIDATION) {
            return getString(R.string.api_validation_error);
        }
        return getString(R.string.api_request_failed);
    }

    private static SettingsContracts.ManagedPatient findPatient(
            java.util.List<SettingsContracts.ManagedPatient> patients,
            long patientId
    ) {
        if (patients == null) {
            return null;
        }
        for (SettingsContracts.ManagedPatient patient : patients) {
            if (patient.patientId == patientId) {
                return patient;
            }
        }
        return null;
    }

    private static SettingsContracts.AgreementRecord deviceAgreement(
            java.util.List<SettingsContracts.AgreementRecord> records
    ) {
        SettingsContracts.AgreementRecord found = null;
        if (records == null) {
            return null;
        }
        for (SettingsContracts.AgreementRecord record : records) {
            if (!SettingsContracts.DEVICE.equals(record.termsType)) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = record;
        }
        return found;
    }

    @Override
    public void syncDevice() {
        if (!deviceWritesAllowed) {
            showNotice(getString(R.string.pillbox_sync), getString(R.string.pillbox_read_only));
            return;
        }
        if (settingsPatientId == null || deviceRequestInFlight) {
            return;
        }
        deviceRequestInFlight = true;
        long patientId = settingsPatientId;
        showProgress(getString(R.string.pillbox_sync));
        ApiClient.execute(() -> {
            try {
                long version = SettingsApi.syncDevice(getApplicationContext(), patientId);
                runOnUiThread(() -> {
                    deviceRequestInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    showNotice(
                            getString(R.string.pillbox_sync),
                            getString(R.string.pillbox_sync_done, Long.toString(version))
                    );
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    deviceRequestInFlight = false;
                    clearProgress();
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.pillbox_sync), exception);
                        return;
                    }
                    showNotice(getString(R.string.pillbox_sync), careMessage(exception));
                });
            }
        });
    }

    @Override
    public void disconnectDevice() {
        if (!deviceWritesAllowed) {
            showNotice(getString(R.string.pillbox_disconnect), getString(R.string.pillbox_read_only));
            return;
        }
        if (settingsPatientId == null) {
            showNotice(getString(R.string.pillbox_disconnect), getString(R.string.pillbox_unconfirmed));
            return;
        }
        if (!beginAccountRequest(getString(R.string.pillbox_disconnect))) {
            return;
        }
        long patientId = settingsPatientId;
        ApiClient.execute(() -> {
            try {
                SettingsApi.disconnectDevice(getApplicationContext(), patientId);
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    YakyeonRepositoryProvider.get().medications().disconnectPillbox();
                    devicePanel = null;
                    settingsPatientId = null;
                    showPillboxTab();
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.pillbox_disconnect), exception);
                        return;
                    }
                    showNotice(getString(R.string.pillbox_disconnect), careMessage(exception));
                });
            }
        });
    }

    @Override
    public void exportClinicalShare() {
        if (!isSignedIn()) {
            showNotice(getString(R.string.settings_share), getString(R.string.settings_login_required));
            return;
        }
        ClinicalShareOptions options = LocalAppSettings.loadShare(this);
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate from = options.resolveStart(today);
        java.time.LocalDate to = options.resolveEnd(today);
        if (!SettingsContracts.shareRangeAllowed(from, to)) {
            showNotice(getString(R.string.settings_share), getString(R.string.settings_share_too_long));
            return;
        }
        if (!options.isIncludeCurrent()) {
            showNotice(getString(R.string.settings_share), getString(R.string.settings_share_need_current));
            return;
        }
        if (!beginAccountRequest(getString(R.string.settings_share))) {
            return;
        }
        boolean records = options.isIncludeRecords();
        boolean period = options.isIncludeSpan();
        boolean interactions = options.isIncludeDur();
        boolean adherence = options.isIncludeAdherence();
        boolean includeCurrent = options.isIncludeCurrent();
        boolean includeFinished = options.isIncludeFinished();
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                        patients,
                        accountType
                );
                if (target.resolution != SettingsContracts.Resolution.ONE) {
                    runOnUiThread(() -> {
                        finishAccountRequest();
                        if (!isAlive()) {
                            return;
                        }
                        showNotice(
                                getString(R.string.settings_share),
                                getString(target.resolution == SettingsContracts.Resolution.SEVERAL
                                        ? R.string.settings_patient_several
                                        : R.string.settings_patient_none)
                        );
                    });
                    return;
                }
                SettingsContracts.ManagedPatient sharePatient = findPatient(patients, target.patientId);
                if (CareContracts.shareSummaryWithheld(sharePatient)) {
                    runOnUiThread(() -> {
                        finishAccountRequest();
                        if (!isAlive()) {
                            return;
                        }
                        showNotice(getString(R.string.settings_share), getString(R.string.care_share_withheld));
                    });
                    return;
                }
                SettingsContracts.ShareDocument document = SettingsApi.shareSummary(
                        getApplicationContext(),
                        target.patientId,
                        from,
                        to,
                        records,
                        period,
                        interactions,
                        adherence
                );
                java.util.List<SettingsContracts.ShareMedicine> medicines = shareMedicines(
                        target.patientId,
                        document.medications
                );
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    ShareSummaryExporter.open(this, ShareSummary.document(
                            document.patientName,
                            from,
                            to,
                            listedMedicines(medicines),
                            includeCurrent,
                            includeFinished,
                            period,
                            document.interactions,
                            document.records,
                            document.adherence
                    ));
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    showApiFailure(getString(R.string.settings_share), exception);
                });
            }
        });
    }

    private java.util.List<SettingsContracts.ShareMedicine> shareMedicines(
            long patientId,
            java.util.List<SettingsContracts.ShareMedicine> fromSummary
    ) throws ApiException {
        java.util.LinkedHashMap<Long, HomeContracts.Medication> byId = new java.util.LinkedHashMap<>();
        addShareMedications(byId, HomeApi.medications(getApplicationContext(), patientId, null));
        addShareMedications(byId, HomeApi.medications(
                getApplicationContext(),
                patientId,
                HomeContracts.STATUS_ACTIVE
        ));
        addShareMedications(byId, HomeApi.medications(
                getApplicationContext(),
                patientId,
                HomeContracts.STATUS_UPCOMING
        ));
        addShareMedications(byId, HomeApi.medications(
                getApplicationContext(),
                patientId,
                HomeContracts.STATUS_ENDED
        ));
        java.util.List<SettingsContracts.ShareMedicine> catalog = SettingsContracts.shareMedicines(
                new java.util.ArrayList<>(byId.values())
        );
        if (!catalog.isEmpty()) {
            return catalog;
        }
        return fromSummary == null ? java.util.Collections.emptyList() : fromSummary;
    }

    private static void addShareMedications(
            java.util.Map<Long, HomeContracts.Medication> byId,
            java.util.List<HomeContracts.Medication> medications
    ) {
        if (medications == null) {
            return;
        }
        for (HomeContracts.Medication medication : medications) {
            if (medication != null) {
                byId.put(medication.medicationId, medication);
            }
        }
    }

    private static java.util.List<ShareSummary.ListedMedicine> listedMedicines(
            java.util.List<SettingsContracts.ShareMedicine> medications
    ) {
        java.util.List<ShareSummary.ListedMedicine> listed = new java.util.ArrayList<>();
        if (medications == null) {
            return listed;
        }
        for (SettingsContracts.ShareMedicine medicine : medications) {
            listed.add(new ShareSummary.ListedMedicine(
                    medicine.name,
                    medicine.detail,
                    shareDay(medicine.startDate),
                    shareDay(medicine.endDate),
                    medicine.ended,
                    medicine.statusKnown
            ));
        }
        return listed;
    }

    private static java.time.LocalDate shareDay(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(raw.trim());
        } catch (java.time.format.DateTimeParseException exception) {
            return null;
        }
    }

    private void sendAgreement(String termsType, Long versionId, boolean agree) {
        if (!beginAccountRequest(getString(R.string.settings_noti_saving))) {
            return;
        }
        ApiClient.execute(() -> {
            try {
                if (agree) {
                    SettingsApi.agree(getApplicationContext(), versionId, true);
                } else {
                    SettingsApi.withdraw(getApplicationContext(), termsType);
                }
                java.util.List<SettingsContracts.AgreementRecord> records = SettingsApi.agreements(
                        getApplicationContext()
                );
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    applyAgreements(records);
                    if (MainScreens.LEGAL.equals(navigator.currentScreen())) {
                        refresh();
                    }
                    ShortMessage.show(screenContainer, getString(R.string.settings_agreement_saved));
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    showApiFailure(getString(R.string.settings_legal), exception);
                });
            }
        });
    }

    private void loadAgreements() {
        int epoch = ++settingsEpoch;
        showProgress(getString(R.string.settings_profile_loading));
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.AgreementRecord> records = SettingsApi.agreements(
                        getApplicationContext()
                );
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive() || epoch != settingsEpoch) {
                        return;
                    }
                    applyAgreements(records);
                    if (MainScreens.LEGAL.equals(navigator.currentScreen())) {
                        refresh();
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive() || epoch != settingsEpoch) {
                        return;
                    }
                    agreementsReady = false;
                    if (MainScreens.LEGAL.equals(navigator.currentScreen())) {
                        showApiFailure(getString(R.string.settings_legal), exception);
                    }
                });
            }
        });
    }

    private void applyAgreements(java.util.List<SettingsContracts.AgreementRecord> records) {
        agreementRecords.clear();
        agreementRecords.addAll(records);
        agreementMarks = SettingsContracts.marksFrom(records);
        agreementsReady = true;
    }

    private void loadCareLinks() {
        int epoch = ++settingsEpoch;
        showProgress(getString(R.string.settings_profile_loading));
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                java.util.List<SettingsContracts.ManagedPatient> guardianPatients = new java.util.ArrayList<>();
                for (SettingsContracts.ManagedPatient patient : patients) {
                    if (SettingsContracts.GUARDIAN.equals(patient.myRole) && patient.linkId != null) {
                        guardianPatients.add(patient);
                    }
                }
                java.util.List<SettingsContracts.GuardianLink> guardianLinks = new java.util.ArrayList<>();
                boolean patientAccount = AccountRoles.PATIENT.equals(accountType);
                if (patientAccount) {
                    SettingsContracts.ResolvedPatient self = SettingsContracts.resolveSelf(patients);
                    if (self.resolution == SettingsContracts.Resolution.ONE) {
                        guardianLinks.addAll(SettingsApi.guardianLinks(
                                getApplicationContext(),
                                self.patientId
                        ));
                    }
                }
                boolean show = patientAccount;
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive() || epoch != settingsEpoch) {
                        return;
                    }
                    managedLines.clear();
                    for (SettingsContracts.ManagedPatient patient : guardianPatients) {
                        managedLines.add(new SettingsScreens.LinkLine(
                                patient.linkId,
                                patient.name,
                                linkDetail(
                                        null,
                                        patient.permissionScope,
                                        patient.relationship,
                                        patient.phoneMasked,
                                        shareText(patient.sharedItems)
                                ),
                                true
                        ));
                    }
                    guardianLines.clear();
                    for (SettingsContracts.GuardianLink link : guardianLinks) {
                        guardianLines.add(new SettingsScreens.LinkLine(
                                link.linkId,
                                link.guardianName,
                                linkDetail(
                                        link.status,
                                        link.permissionScope,
                                        link.relationship,
                                        null,
                                        shareText(link.sharedItems)
                                ),
                                SettingsContracts.ACTIVE.equals(link.status)
                        ));
                    }
                    showGuardians = show;
                    careReady = true;
                    if (!patientAccount) {
                        reloadHomeAfterLinkSync();
                    }
                    if (MainScreens.LINK.equals(navigator.currentScreen())) {
                        refresh();
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive() || epoch != settingsEpoch) {
                        return;
                    }
                    careReady = true;
                    managedLines.clear();
                    guardianLines.clear();
                    if (MainScreens.LINK.equals(navigator.currentScreen())) {
                        refresh();
                        showApiFailure(getString(R.string.settings_link), exception);
                    }
                });
            }
        });
    }

    private void loadDevice() {
        int epoch = ++settingsEpoch;
        showProgress(getString(R.string.pillbox_loading));
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                        patients,
                        accountType
                );
                if (target.resolution != SettingsContracts.Resolution.ONE) {
                    String text = getString(target.resolution == SettingsContracts.Resolution.SEVERAL
                            ? R.string.settings_patient_several
                            : R.string.settings_patient_none);
                    runOnUiThread(() -> applyDevice(epoch, null, false, null, text, true));
                    return;
                }
                SettingsContracts.ManagedPatient patient = findPatient(patients, target.patientId);
                boolean writes = CareContracts.writesAllowed(patient);
                if (CareContracts.shareWithheld(patient, CareContracts.DEVICE_EVENT)) {
                    runOnUiThread(() -> applyDevice(
                            epoch,
                            null,
                            writes,
                            target.patientId,
                            getString(R.string.care_share_withheld),
                            false
                    ));
                    return;
                }
                try {
                    SettingsContracts.DeviceSnapshot loaded = SettingsApi.device(
                            getApplicationContext(),
                            target.patientId
                    );
                    runOnUiThread(() -> applyDevice(epoch, loaded, writes, target.patientId, null, false));
                } catch (ApiException exception) {
                    runOnUiThread(() -> {
                        if (!isAlive() || epoch != settingsEpoch) {
                            return;
                        }
                        if (exception.endsSession()) {
                            clearLoadProgress();
                            devicePanel = null;
                            deviceReady = true;
                            showApiFailure(getString(R.string.pillbox_settings), exception);
                            return;
                        }
                        if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                            forgetLocalPillbox();
                            applyDevice(epoch, null, writes, target.patientId, null, false);
                            return;
                        }
                        String text = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                                ? getString(R.string.care_share_withheld)
                                : careMessage(exception);
                        applyDevice(epoch, null, writes, target.patientId, text, false);
                    });
                }
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    if (!isAlive() || epoch != settingsEpoch) {
                        return;
                    }
                    clearLoadProgress();
                    devicePanel = null;
                    deviceReady = true;
                    deviceStatusMessage = careMessage(exception);
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.pillbox_settings), exception);
                        return;
                    }
                    if (SettingsScreens.PILLBOX.equals(navigator.currentScreen())) {
                        refresh();
                    }
                });
            }
        });
    }

    private void applyDevice(
            int epoch,
            SettingsContracts.DeviceSnapshot snapshot,
            boolean writes,
            Long patientId,
            String status,
            boolean explainMissingPatient
    ) {
        clearLoadProgress();
        if (!isAlive() || epoch != settingsEpoch) {
            return;
        }
        deviceWritesAllowed = writes;
        settingsPatientId = patientId;
        devicePanel = snapshot == null ? null : panelFrom(snapshot);
        deviceStatusMessage = status;
        deviceReady = true;
        if (!SettingsScreens.PILLBOX.equals(navigator.currentScreen())) {
            return;
        }
        if (explainMissingPatient && status != null) {
            showNotice(getString(R.string.pillbox_settings), status);
        }
        refresh();
    }

    private static SettingsScreens.DevicePanel panelFrom(SettingsContracts.DeviceSnapshot snapshot) {
        return new SettingsScreens.DevicePanel(
                snapshot.serialNo,
                snapshot.online,
                snapshot.buzzerEnabled != null,
                snapshot.buzzerEnabled != null && snapshot.buzzerEnabled,
                snapshot.ledEnabled != null,
                snapshot.ledEnabled != null && snapshot.ledEnabled,
                snapshot.scheduleSyncedAt,
                snapshot.scheduleInSync
        );
    }

    private void forgetLocalPillbox() {
        YakyeonRepositoryProvider.get().medications().disconnectPillbox();
    }

    @Override
    public String deviceSerialDraft() {
        return deviceSerialDraft == null ? "" : deviceSerialDraft;
    }

    @Override
    public String deviceSecretDraft() {
        return deviceSecretDraft == null ? "" : deviceSecretDraft;
    }

    @Override
    public void registerDevice(String serialNo, String secret) {
        String serial = serialNo == null ? "" : serialNo;
        String secretValue = secret == null ? "" : secret;
        deviceSerialDraft = serial;
        deviceSecretDraft = secretValue;
        if (serial.trim().isEmpty() || secretValue.trim().isEmpty()) {
            showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_register_need_both));
            unlockPillboxRegisterButton();
            return;
        }
        if (!beginAccountRequest(getString(R.string.pillbox_register_loading))) {
            unlockPillboxRegisterButton();
            return;
        }
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                        patients,
                        accountType
                );
                if (target.resolution != SettingsContracts.Resolution.ONE) {
                    runOnUiThread(() -> {
                        finishDeviceRegisterAttempt();
                        if (!isAlive()) {
                            return;
                        }
                        showNotice(
                                getString(R.string.pillbox_settings),
                                getString(target.resolution == SettingsContracts.Resolution.SEVERAL
                                        ? R.string.settings_patient_several
                                        : R.string.settings_patient_none)
                        );
                    });
                    return;
                }
                SettingsContracts.ManagedPatient patient = findPatient(patients, target.patientId);
                if (!CareContracts.writesAllowed(patient)) {
                    runOnUiThread(() -> {
                        finishDeviceRegisterAttempt();
                        showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_read_only));
                    });
                    return;
                }
                if (CareContracts.shareWithheld(patient, CareContracts.DEVICE_EVENT)) {
                    runOnUiThread(() -> {
                        finishDeviceRegisterAttempt();
                        showNotice(getString(R.string.pillbox_settings), getString(R.string.care_share_withheld));
                    });
                    return;
                }
                pendingDevicePatientId = target.patientId;
                CareApi.registerDevice(getApplicationContext(), target.patientId, serial, secretValue, null);
                runOnUiThread(this::onDeviceRegistered);
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishDeviceRegisterAttempt();
                    if (!isAlive()) {
                        return;
                    }
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.pillbox_settings), exception);
                        return;
                    }
                    if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                        forgetLocalPillbox();
                    }
                    String specific = ApiMessages.deviceRegister(exception.status(), exception.code());
                    if (specific != null) {
                        boolean needsConsent = exception.status() == 403
                                && ApiCodes.CONSENT_REQUIRED.equals(exception.code());
                        showNotice(
                                getString(R.string.pillbox_settings),
                                specific,
                                needsConsent ? () -> openDetail(SettingsScreens.PILLBOX_CONSENT) : null
                        );
                        return;
                    }
                    showNotice(getString(R.string.pillbox_settings), careMessage(exception));
                });
            }
        });
    }

    private void clearDeviceDraft() {
        deviceSerialDraft = "";
        deviceSecretDraft = "";
        pendingDevicePatientId = null;
    }

    private void onDeviceRegistered() {
        finishAccountRequest();
        if (!isAlive()) {
            return;
        }
        clearDeviceDraft();
        showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_register_done));
        accountSyncKey = null;
        devicePanel = null;
        deviceReady = false;
        deviceStatusMessage = null;
        pillboxArmed = false;
        clearHomeCache();
        showPillboxTab();
    }

    private void finishDeviceNotice(String message) {
        runOnUiThread(() -> {
            finishAccountRequest();
            unlockPillboxRegisterButton();
            if (!isAlive()) {
                return;
            }
            showNotice(getString(R.string.pillbox_settings), message);
        });
    }

    private void failDeviceConsent(ApiException exception) {
        runOnUiThread(() -> {
            finishAccountRequest();
            unlockPillboxRegisterButton();
            if (!isAlive()) {
                return;
            }
            if (exception.endsSession()) {
                showApiFailure(getString(R.string.pillbox_settings), exception);
                return;
            }
            showNotice(getString(R.string.pillbox_settings), careMessage(exception));
        });
    }

    private void armHomeDateWatch() {
        verificationHandler.removeCallbacks(homeDateRollover);
        java.time.ZoneId seoul = java.time.ZoneId.of("Asia/Seoul");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(seoul);
        java.time.ZonedDateTime next = now.toLocalDate().plusDays(1).atStartOfDay(seoul);
        long delay = java.time.Duration.between(now, next).toMillis() + 1000L;
        if (delay < 1000L) {
            delay = 1000L;
        }
        verificationHandler.postDelayed(homeDateRollover, delay);
    }

    private boolean noteHomeDate() {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        boolean changed = homeShownDate != null && !homeShownDate.equals(today);
        homeShownDate = today;
        return changed;
    }

    private void rollHomeToToday() {
        homeShownDate = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        if (!isAlive()) {
            return;
        }
        if (isSignedIn()) {
            homeArmed = false;
            ensureHomeLoaded();
            if (needsServerHome(navigator.currentScreen())) {
                refresh();
            }
            return;
        }
        if (navigator.isMainArea() && navigator.currentTab() == ScreenId.HOME) {
            refresh();
        }
    }

    private void finishDeviceRegisterAttempt() {
        finishAccountRequest();
        unlockPillboxRegisterButton();
    }

    private void unlockPillboxRegisterButton() {
        if (screenContainer == null) {
            return;
        }
        View button = screenContainer.findViewById(R.id.button_pillbox_register_submit);
        if (button == null) {
            return;
        }
        button.setTag(null);
        YakyeonTextField serial = screenContainer.findViewById(R.id.field_pillbox_serial);
        YakyeonTextField secret = screenContainer.findViewById(R.id.field_pillbox_secret);
        boolean ready = serial != null
                && secret != null
                && !serial.getText().trim().isEmpty()
                && !secret.getText().trim().isEmpty();
        button.setEnabled(ready);
    }

    private String shareText(java.util.List<String> items) {
        if (items == null) {
            return null;
        }
        if (items.isEmpty()) {
            return getString(R.string.care_share_none);
        }
        StringBuilder text = new StringBuilder();
        for (String item : items) {
            appendDetail(text, shareLabel(item));
        }
        return text.toString();
    }

    private String shareLabel(String item) {
        if (CareContracts.PERSONAL_INFO.equals(item)) {
            return getString(R.string.care_share_personal);
        }
        if (HomeContracts.MED_INFO.equals(item)) {
            return getString(R.string.care_share_med);
        }
        if ("PHOTO".equals(item)) {
            return getString(R.string.care_share_photo);
        }
        if (HomeContracts.DOSE_RECORD.equals(item)) {
            return getString(R.string.care_share_dose);
        }
        if (CareContracts.DEVICE_EVENT.equals(item)) {
            return getString(R.string.care_share_device);
        }
        if (CareContracts.DUR_INFO.equals(item)) {
            return getString(R.string.care_share_dur);
        }
        return item;
    }

    private static String linkDetail(
            String status,
            String scope,
            String relationship,
            String phone,
            String shares
    ) {
        StringBuilder text = new StringBuilder();
        appendDetail(text, status);
        appendDetail(text, scope);
        appendDetail(text, relationship);
        appendDetail(text, phone);
        appendDetail(text, shares);
        return text.toString();
    }

    private static void appendDetail(StringBuilder text, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (text.length() > 0) {
            text.append(" · ");
        }
        text.append(value);
    }

    @Override
    public boolean usesServerRecords() {
        return isSignedIn();
    }

    @Override
    public void requireRegisteredPillbox(Runnable onReady) {
        if (!isSignedIn()) {
            if (YakyeonRepositoryProvider.get().medications().pillboxConnected()) {
                onReady.run();
            } else {
                showUnregisteredPillbox();
            }
            return;
        }
        if (hasRegisteredPillbox()) {
            onReady.run();
            return;
        }
        if (pillboxView == MainScreens.CareView.ABSENT && devicePanel == null) {
            showUnregisteredPillbox();
            return;
        }
        if (!beginAccountRequest(getString(R.string.med_into_pillbox))) {
            return;
        }
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                        patients,
                        accountType
                );
                if (target.resolution != SettingsContracts.Resolution.ONE) {
                    runOnUiThread(() -> {
                        finishAccountRequest();
                        if (isAlive()) {
                            showUnregisteredPillbox();
                        }
                    });
                    return;
                }
                SettingsContracts.ManagedPatient patient = findPatient(patients, target.patientId);
                if (CareContracts.shareWithheld(patient, CareContracts.DEVICE_EVENT)) {
                    runOnUiThread(() -> {
                        finishAccountRequest();
                        if (isAlive()) {
                            showNotice(
                                    getString(R.string.med_into_pillbox),
                                    getString(R.string.care_share_withheld)
                            );
                        }
                    });
                    return;
                }
                SettingsApi.device(getApplicationContext(), target.patientId);
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (isAlive()) {
                        onReady.run();
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                        forgetLocalPillbox();
                        showUnregisteredPillbox();
                        return;
                    }
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.med_into_pillbox), exception);
                        return;
                    }
                    showNotice(getString(R.string.med_into_pillbox), careMessage(exception));
                });
            }
        });
    }

    private boolean hasRegisteredPillbox() {
        if (devicePanel != null) {
            return true;
        }
        return pillboxSnapshot != null && pillboxSnapshot.device != null;
    }

    private void showUnregisteredPillbox() {
        showNotice(getString(R.string.med_into_pillbox), getString(R.string.med_pillbox_unregistered));
    }

    @Override
    public void confirmMedicationConsent() {
        ApiClient.execute(() -> {
            try {
                boolean agreed = false;
                for (SettingsContracts.AgreementRecord record : SettingsApi.agreements(getApplicationContext())) {
                    if (SettingsContracts.MEDICATION.equals(record.termsType) && record.agreed) {
                        agreed = true;
                        break;
                    }
                }
                if (!agreed) {
                    Long version = HomeApi.medicationTermsVersion(getApplicationContext());
                    if (version == null) {
                        runOnUiThread(() -> showNotice(
                                getString(R.string.med_consent_title),
                                getString(R.string.med_consent_version_missing)
                        ));
                        return;
                    }
                    SettingsApi.agree(getApplicationContext(), version, true);
                }
                runOnUiThread(() -> {
                    if (!isAlive()) {
                        return;
                    }
                    openScreen(MedScreen.ENVELOPE);
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    if (exception.status() == 404) {
                        showNotice(
                                getString(R.string.med_consent_title),
                                getString(R.string.med_consent_unavailable)
                        );
                        return;
                    }
                    showApiFailure(getString(R.string.med_consent_title), exception);
                });
            }
        });
    }

    @Override
    public void submitServerRegistration() {
        if (homeWriteBusy) {
            return;
        }
        MedicationStore store = YakyeonRepositoryProvider.get().medications();
        HomeRegistration.Prepared prepared = HomeRegistration.prepare(store.draft());
        MedicationRules.SaveBlock times = MedicationRules.validate(store.draft());
        if (times == MedicationRules.SaveBlock.MISSING_TIME || times == MedicationRules.SaveBlock.TOO_CLOSE) {
            SavedToast.dismiss(this);
            showMessage(getString(R.string.med_time_rule));
            return;
        }
        if (prepared.block != HomeRegistration.Block.NONE || prepared.prescription == null) {
            SavedToast.dismiss(this);
            showMessage(getString(R.string.med_required));
            return;
        }
        EnvelopeDraft submitted = store.draft();
        homeWriteBusy = true;
        ApiClient.execute(() -> {
            try {
                PatientGate gate = gatePatients();
                if (gate.blocked != null) {
                    runOnUiThread(() -> failRegistration(gate.blocked));
                    return;
                }
                if (!CareContracts.writesAllowed(gate.patient)) {
                    runOnUiThread(() -> failRegistration(getString(R.string.med_readonly)));
                    return;
                }
                if (CareContracts.shareWithheld(gate.patient, HomeContracts.MED_INFO)) {
                    runOnUiThread(() -> failRegistration(getString(R.string.care_share_withheld)));
                    return;
                }
                HomeContracts.CreatedPrescription created = HomeApi.createPrescription(
                        getApplicationContext(),
                        gate.patient.patientId,
                        prepared.prescription
                );
                StringBuilder partial = new StringBuilder();
                boolean[] missingPillbox = {false};
                if (prepared.schedule != null) {
                    try {
                        HomeApi.saveSchedule(getApplicationContext(), created.prescriptionId, prepared.schedule);
                    } catch (ApiException exception) {
                        if (exception.endsSession()) {
                            throw exception;
                        }
                        partial.append(getString(R.string.med_server_partial_schedule));
                        partial.append('\n').append(careMessage(exception));
                    }
                }
                if (prepared.compartmentNo != null) {
                    if (CareContracts.shareWithheld(gate.patient, CareContracts.DEVICE_EVENT)) {
                        if (partial.length() > 0) {
                            partial.append('\n');
                        }
                        partial.append(getString(R.string.care_share_withheld));
                    } else {
                        try {
                            org.json.JSONObject assigned = HomeApi.assignCompartment(
                                    getApplicationContext(),
                                    created.prescriptionId,
                                    prepared.compartmentNo
                            );
                            CareContracts.Assignment assignment = CareContracts.readAssignment(assigned);
                            if (!CareContracts.assignmentCompleted(
                                    assignment,
                                    created.prescriptionId,
                                    prepared.compartmentNo
                            )) {
                                throw ApiException.response(201);
                            }
                        } catch (ApiException exception) {
                            if (exception.endsSession()) {
                                throw exception;
                            }
                            if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                                missingPillbox[0] = true;
                            } else {
                                if (partial.length() > 0) {
                                    partial.append('\n');
                                }
                                partial.append(getString(R.string.med_server_partial_slot));
                                partial.append('\n').append(careMessage(exception));
                            }
                        }
                    }
                }
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    if (!isAlive()) {
                        return;
                    }
                    MedicationStore medications = YakyeonRepositoryProvider.get().medications();
                    if (medications.draft() == submitted) {
                        medications.abandonDraft();
                    }
                    if (!MedScreen.LIST.equals(navigator.currentScreen())) {
                        showRegisteredMedications();
                    }
                    if (prepared.compartmentNo != null && !missingPillbox[0]) {
                        listPillboxArmed = false;
                    }
                    reloadServerSurfaces();
                    if (missingPillbox[0]) {
                        showUnregisteredPillbox();
                    } else if (partial.length() > 0) {
                        showMessage(partial.toString());
                    }
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    SavedToast.dismiss(this);
                    showApiFailure(getString(R.string.med_envelope_title), exception);
                });
            }
        });
    }

    @Override
    public void capturePrescription() {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setTitle(R.string.med_capture_title)
                .setItems(
                        new CharSequence[] {
                                getString(R.string.med_capture_camera),
                                getString(R.string.med_capture_gallery)
                        },
                        (dialog, which) -> {
                            if (which == 0) {
                                openPrescriptionCamera();
                            } else {
                                prescriptionImage.launch("image/*");
                            }
                        }
                )
                .show();
    }

    private void openPrescriptionCamera() {
        java.io.File dir = new java.io.File(getCacheDir(), "camera");
        if (!dir.exists() && !dir.mkdirs()) {
            showMessage(getString(R.string.med_camera_unavailable));
            return;
        }
        java.io.File file = new java.io.File(dir, "prescription.jpg");
        try {
            if (file.exists() && !file.delete()) {
                showMessage(getString(R.string.med_camera_unavailable));
                return;
            }
            if (!file.createNewFile()) {
                showMessage(getString(R.string.med_camera_unavailable));
                return;
            }
        } catch (java.io.IOException exception) {
            showMessage(getString(R.string.med_camera_unavailable));
            return;
        }
        Intent probe = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (probe.resolveActivity(getPackageManager()) == null) {
            showMessage(getString(R.string.med_camera_unavailable));
            return;
        }
        pendingCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        prescriptionCamera.launch(pendingCameraUri);
    }

    @Override
    public void searchMedicineNames(String query, MedicineNamePicker.ResultListener listener) {
        String text = query == null ? "" : query.trim();
        if (text.length() < 2) {
            listener.onResult(java.util.Collections.emptyList(), null);
            return;
        }
        if (text.length() > HomeContracts.SEARCH_MAX_CHARS) {
            listener.onResult(
                    java.util.Collections.emptyList(),
                    ApiMessages.fallback(ApiCodes.INVALID_SEARCH_QUERY)
            );
            return;
        }
        ApiClient.execute(() -> {
            try {
                java.util.List<HomeContracts.DrugSuggestion> rows = HomeApi.autocomplete(
                        getApplicationContext(),
                        text,
                        HomeContracts.AUTOCOMPLETE_LIMIT
                );
                runOnUiThread(() -> listener.onResult(rows, null));
            } catch (ApiException exception) {
                runOnUiThread(() -> listener.onResult(java.util.Collections.emptyList(), careMessage(exception)));
            }
        });
    }

    @Override
    public void endServerPrescription(long prescriptionId) {
        runHomeWrite(() -> {
            HomeContracts.EndedPrescription ended = HomeApi.endPrescription(getApplicationContext(), prescriptionId);
            return getString(R.string.med_server_saved) + " (" + ended.endedMedicationCount + ")";
        });
    }

    @Override
    public void endServerMedication(long medicationId) {
        runHomeWrite(() -> {
            HomeApi.endMedication(getApplicationContext(), medicationId);
            return getString(R.string.med_server_saved);
        });
    }

    @Override
    public void deleteServerMedication(long medicationId) {
        runHomeWrite(() -> {
            HomeApi.deleteMedication(getApplicationContext(), medicationId);
            return getString(R.string.med_server_saved);
        });
    }

    @Override
    public void openServerSchedule(long prescriptionId) {
        if (!homeWrites) {
            showMessage(getString(R.string.med_readonly));
            return;
        }
        ApiClient.execute(() -> {
            try {
                HomeContracts.ScheduleSettings settings = HomeApi.schedule(getApplicationContext(), prescriptionId);
                java.util.EnumMap<com.example.yakyeon2.data.DoseRound, Integer> minutes =
                        new java.util.EnumMap<>(com.example.yakyeon2.data.DoseRound.class);
                java.util.EnumSet<com.example.yakyeon2.data.DoseRound> selected =
                        java.util.EnumSet.noneOf(com.example.yakyeon2.data.DoseRound.class);
                for (HomeContracts.ScheduleSetting setting : settings.settings) {
                    com.example.yakyeon2.data.DoseRound round = roundForSlot(setting.timingSlot);
                    if (round == null) {
                        continue;
                    }
                    String[] parts = setting.doseTime.split(":");
                    int value = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                    minutes.put(round, value);
                    selected.add(round);
                }
                runOnUiThread(() -> {
                    if (!isAlive()) {
                        return;
                    }
                    if (serverAlarmDialog != null) {
                        serverAlarmDialog.dismiss();
                    }
                    serverAlarmDialog = MedScreens.serverAlarmDialog(
                            this,
                            hospitalFor(prescriptionId),
                            minutes,
                            selected,
                            () -> saveServerSchedule(prescriptionId, minutes, selected)
                    );
                    serverAlarmDialog.show();
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> showApiFailure(getString(R.string.med_alarm_title), exception));
            }
        });
    }

    @Override
    public void openServerCompartment(long prescriptionId) {
        if (serverSlotBusy) {
            return;
        }
        if (!homeWrites) {
            showMessage(getString(R.string.med_readonly));
            return;
        }
        if (prescriptionFor(prescriptionId) == null) {
            String waiting = prescriptionMessage();
            showMessage(waiting == null || waiting.isEmpty()
                    ? getString(R.string.home_loading)
                    : waiting);
            return;
        }
        serverSlotBusy = true;
        showProgress(getString(R.string.med_change_slot));
        ApiClient.execute(() -> {
            try {
                PatientGate gate = gatePatients();
                if (gate.blocked != null) {
                    runOnUiThread(() -> finishSlotNotice(gate.blocked));
                    return;
                }
                if (!CareContracts.writesAllowed(gate.patient)) {
                    runOnUiThread(() -> finishSlotNotice(getString(R.string.med_readonly)));
                    return;
                }
                if (CareContracts.shareWithheld(gate.patient, CareContracts.DEVICE_EVENT)) {
                    runOnUiThread(() -> finishSlotNotice(getString(R.string.care_share_withheld)));
                    return;
                }
                CareContracts.Pillbox loaded = CareApi.pillbox(getApplicationContext(), gate.patient.patientId);
                runOnUiThread(() -> {
                    finishServerSlotRequest();
                    if (!isAlive() || !isSignedIn()) {
                        return;
                    }
                    publishListPillbox(loaded);
                    if (CareContracts.assignmentAmbiguous(loaded, prescriptionId)) {
                        showMessage(getString(R.string.api_response_error));
                        refreshOpenList();
                        return;
                    }
                    CareContracts.Compartment current = CareContracts.compartmentForPrescription(
                            loaded,
                            prescriptionId
                    );
                    if (current == null && !CareContracts.hasEmptyCompartment(loaded)) {
                        showMessage(getString(R.string.med_slot_none_available));
                        refreshOpenList();
                        return;
                    }
                    serverSlotPillbox = loaded;
                    serverSlotPrescriptionId = prescriptionId;
                    serverSlotChoice = current != null && CareContracts.compartmentNoAllowed(current.compartmentNo)
                            ? (int) current.compartmentNo
                            : null;
                    openScreen(MedScreen.SLOT);
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishServerSlotRequest();
                    if (!isAlive() || !isSignedIn()) {
                        return;
                    }
                    if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                        forgetLocalPillbox();
                        publishListPillboxAbsent();
                        showMessage(getString(R.string.pillbox_slot_blocked));
                        refreshOpenList();
                        return;
                    }
                    showApiFailure(getString(R.string.med_change_slot), exception);
                });
            }
        });
    }

    @Override
    public void chooseServerCompartment(int compartmentNo) {
        if (serverSlotBusy || serverSlotPillbox == null || serverSlotPrescriptionId == null) {
            return;
        }
        CareContracts.Compartment compartment = CareContracts.compartmentByNo(serverSlotPillbox, compartmentNo);
        if (!CareContracts.compartmentSelectable(compartment, serverSlotPrescriptionId)) {
            showMessage(getString(R.string.pillbox_slot_full));
            return;
        }
        if (serverSlotChoice != null && serverSlotChoice == compartmentNo) {
            serverSlotChoice = null;
        } else {
            serverSlotChoice = compartmentNo;
        }
        refresh();
    }

    @Override
    public void confirmServerCompartment() {
        if (serverSlotBusy || serverSlotPrescriptionId == null || serverSlotPillbox == null) {
            return;
        }
        long prescriptionId = serverSlotPrescriptionId;
        Integer choice = serverSlotChoice;
        CareContracts.Compartment current = CareContracts.compartmentForPrescription(
                serverSlotPillbox,
                prescriptionId
        );
        if (choice != null && current != null && current.compartmentNo == choice) {
            clearServerSlot();
            popTo(MedScreen.LIST);
            return;
        }
        if (current != null
                && !CareContracts.EMPTY.equals(current.state)
                && (current.mappingId == null || !CareContracts.canRelease(current))) {
            restoreSlotChoice(prescriptionId);
            showMessage(getString(R.string.api_response_error));
            return;
        }
        if (choice == null) {
            if (current == null || CareContracts.EMPTY.equals(current.state)) {
                clearServerSlot();
                popTo(MedScreen.LIST);
                return;
            }
            long mappingId = current.mappingId;
            serverSlotBusy = true;
            lockSlotSaveButton();
            showProgress(getString(R.string.med_change_slot));
            CareContracts.Pillbox known = serverSlotPillbox;
            ApiClient.execute(() -> {
                try {
                    PatientGate gate = gatePatients();
                    if (gate.blocked != null) {
                        runOnUiThread(() -> finishSlotRestore(prescriptionId, gate.blocked));
                        return;
                    }
                    if (!CareContracts.writesAllowed(gate.patient)) {
                        runOnUiThread(() -> finishSlotRestore(prescriptionId, getString(R.string.med_readonly)));
                        return;
                    }
                    if (CareContracts.shareWithheld(gate.patient, CareContracts.DEVICE_EVENT)) {
                        runOnUiThread(() -> finishSlotRestore(
                                prescriptionId,
                                getString(R.string.care_share_withheld)
                        ));
                        return;
                    }
                    SlotPlacement placement = releaseOnlyServerCompartment(
                            gate.patient.patientId,
                            mappingId,
                            known
                    );
                    runOnUiThread(() -> applySlotPlacement(prescriptionId, placement));
                } catch (ApiException exception) {
                    runOnUiThread(() -> {
                        finishServerSlotRequest();
                        restoreSlotChoice(prescriptionId);
                        if (isAlive()) {
                            showApiFailure(getString(R.string.med_change_slot), exception);
                        }
                    });
                }
            });
            return;
        }
        int compartmentNo = choice;
        CareContracts.Compartment target = CareContracts.compartmentByNo(serverSlotPillbox, compartmentNo);
        if (!CareContracts.compartmentSelectable(target, prescriptionId)) {
            showMessage(getString(R.string.pillbox_slot_full));
            return;
        }
        Long releaseMappingId = null;
        Long restoreNo = null;
        if (current != null && !CareContracts.EMPTY.equals(current.state)) {
            restoreNo = current.compartmentNo;
            releaseMappingId = current.mappingId;
        }
        serverSlotBusy = true;
        lockSlotSaveButton();
        showProgress(getString(R.string.med_change_slot));
        Long mappingToRelease = releaseMappingId;
        Long compartmentToRestore = restoreNo;
        ApiClient.execute(() -> {
            try {
                PatientGate gate = gatePatients();
                if (gate.blocked != null) {
                    runOnUiThread(() -> finishSlotRestore(prescriptionId, gate.blocked));
                    return;
                }
                if (!CareContracts.writesAllowed(gate.patient)) {
                    runOnUiThread(() -> finishSlotRestore(prescriptionId, getString(R.string.med_readonly)));
                    return;
                }
                if (CareContracts.shareWithheld(gate.patient, CareContracts.DEVICE_EVENT)) {
                    runOnUiThread(() -> finishSlotRestore(
                            prescriptionId,
                            getString(R.string.care_share_withheld)
                    ));
                    return;
                }
                SlotPlacement placement = assignServerCompartment(
                        gate.patient.patientId,
                        prescriptionId,
                        compartmentNo,
                        mappingToRelease,
                        compartmentToRestore,
                        serverSlotPillbox
                );
                runOnUiThread(() -> applySlotPlacement(prescriptionId, placement));
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishServerSlotRequest();
                    restoreSlotChoice(prescriptionId);
                    if (isAlive()) {
                        showApiFailure(getString(R.string.med_change_slot), exception);
                    }
                });
            }
        });
    }

    private void finishSlotRestore(long prescriptionId, String message) {
        finishServerSlotRequest();
        restoreSlotChoice(prescriptionId);
        if (!isAlive()) {
            return;
        }
        if (message != null && !message.isEmpty()) {
            showMessage(message);
        }
    }

    @Override
    public void releaseCompartment(long mappingId) {
        if (compartmentReleaseInFlight || !isSignedIn()) {
            return;
        }
        if (!deviceWritesAllowed) {
            showMessage(getString(R.string.pillbox_read_only));
            return;
        }
        CareContracts.Compartment current = CareContracts.compartmentForMapping(pillboxSnapshot, mappingId);
        if (!CareContracts.canRelease(current)) {
            return;
        }
        compartmentReleaseInFlight = true;
        showProgress(getString(R.string.pillbox_release));
        Long patientId = settingsPatientId;
        CareContracts.Pillbox known = pillboxSnapshot;
        ApiClient.execute(() -> {
            try {
                try {
                    CareApi.releaseCompartment(getApplicationContext(), mappingId);
                } catch (ApiException exception) {
                    if (exception.endsSession() || !mappingAlreadyGone(exception)) {
                        throw exception;
                    }
                }
                CareContracts.Pillbox fresh = patientId == null ? null : readPillboxOrNull(patientId);
                CareContracts.Pillbox next = fresh != null
                        ? fresh
                        : CareContracts.clearMapping(known, mappingId);
                runOnUiThread(() -> finishCompartmentRelease(next, null));
            } catch (ApiException exception) {
                runOnUiThread(() -> finishCompartmentRelease(null, exception));
            }
        });
    }

    private void finishCompartmentRelease(CareContracts.Pillbox next, ApiException error) {
        compartmentReleaseInFlight = false;
        clearProgress();
        if (!isAlive() || !isSignedIn()) {
            return;
        }
        if (error != null) {
            showApiFailure(getString(R.string.pillbox_release), error);
            return;
        }
        if (next != null) {
            publishAssignedPillbox(next);
        }
        showMessage(getString(R.string.pillbox_release_done));
        invalidateDoseCaches();
        ensureHomeLoaded();
        if (showingSchedule()) {
            ensureScheduleLoaded();
        }
        if (MedScreen.HISTORY.equals(navigator.currentScreen())) {
            ensureHistoryLoaded();
        }
    }

    private static boolean mappingAlreadyGone(ApiException exception) {
        return ApiCodes.COMPARTMENT_MAPPING_NOT_FOUND.equals(exception.code())
                || ApiCodes.MAPPING_ALREADY_ENDED.equals(exception.code());
    }

    private void applySlotPlacement(long prescriptionId, SlotPlacement placement) {
        finishServerSlotRequest();
        if (!isAlive() || !isSignedIn()) {
            return;
        }
        if (placement.pillbox != null) {
            publishAssignedPillbox(placement.pillbox);
            if (serverSlotPrescriptionId != null && serverSlotPrescriptionId == prescriptionId) {
                serverSlotPillbox = placement.pillbox;
                CareContracts.Compartment now = CareContracts.compartmentForPrescription(
                        placement.pillbox,
                        prescriptionId
                );
                serverSlotChoice = now != null && CareContracts.compartmentNoAllowed(now.compartmentNo)
                        ? (int) now.compartmentNo
                        : null;
            }
        } else if (placement.error == null) {
            listPillboxArmed = false;
        }
        if (placement.error != null) {
            if (MedScreen.SLOT.equals(navigator.currentScreen())) {
                refresh();
            } else {
                refreshOpenList();
            }
            showApiFailure(getString(R.string.med_change_slot), placement.error);
            return;
        }
        clearServerSlot();
        if (MedScreen.SLOT.equals(navigator.currentScreen())) {
            popTo(MedScreen.LIST);
        } else {
            refreshOpenList();
        }
        showMessage(slotSavedMessage(placement.pillbox, prescriptionId));
        invalidateDoseCaches();
        ensureHomeLoaded();
        if (showingSchedule()) {
            ensureScheduleLoaded();
        }
        if (MedScreen.HISTORY.equals(navigator.currentScreen())) {
            ensureHistoryLoaded();
        }
    }

    private String slotSavedMessage(CareContracts.Pillbox pillbox, long prescriptionId) {
        CareContracts.Compartment placed = CareContracts.compartmentForPrescription(pillbox, prescriptionId);
        if (placed == null || CareContracts.EMPTY.equals(placed.state)) {
            return getString(R.string.pillbox_release_done);
        }
        return getString(R.string.med_slot_server_saved);
    }

    private void lockSlotSaveButton() {
        View done = findViewById(R.id.button_slot_done);
        if (done != null) {
            done.setEnabled(false);
        }
    }

    private void restoreSlotChoice(long prescriptionId) {
        if (serverSlotPillbox == null) {
            return;
        }
        CareContracts.Compartment now = CareContracts.compartmentForPrescription(
                serverSlotPillbox,
                prescriptionId
        );
        serverSlotChoice = now != null && CareContracts.compartmentNoAllowed(now.compartmentNo)
                ? (int) now.compartmentNo
                : null;
        if (MedScreen.SLOT.equals(navigator.currentScreen())) {
            refresh();
        }
    }

    private SlotPlacement releaseOnlyServerCompartment(
            long patientId,
            long mappingId,
            CareContracts.Pillbox known
    ) throws ApiException {
        try {
            CareApi.releaseCompartment(getApplicationContext(), mappingId);
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                throw exception;
            }
            if (!mappingAlreadyGone(exception)) {
                CareContracts.Pillbox fresh = readPillboxOrNull(patientId);
                return new SlotPlacement(fresh != null ? fresh : known, exception);
            }
        }
        CareContracts.Pillbox fresh = readPillboxOrNull(patientId);
        return new SlotPlacement(
                fresh != null ? fresh : CareContracts.clearMapping(known, mappingId),
                null
        );
    }

    private SlotPlacement assignServerCompartment(
            long patientId,
            long prescriptionId,
            int compartmentNo,
            Long releaseMappingId,
            Long restoreNo,
            CareContracts.Pillbox knownPillbox
    ) throws ApiException {
        boolean released = false;
        if (releaseMappingId != null) {
            try {
                CareApi.releaseCompartment(getApplicationContext(), releaseMappingId);
                released = true;
            } catch (ApiException exception) {
                if (exception.endsSession()) {
                    throw exception;
                }
                if (!ApiCodes.COMPARTMENT_MAPPING_NOT_FOUND.equals(exception.code())
                        && !ApiCodes.MAPPING_ALREADY_ENDED.equals(exception.code())) {
                    return new SlotPlacement(null, exception);
                }
            }
        }
        try {
            org.json.JSONObject body = CareApi.assignCompartment(
                    getApplicationContext(),
                    prescriptionId,
                    compartmentNo
            );
            CareContracts.Assignment assignment = CareContracts.readAssignment(body);
            if (assignment.prescriptionId != prescriptionId || assignment.compartmentNo != compartmentNo) {
                throw ApiException.response(201);
            }
            if (!CareContracts.ACTIVE.equals(assignment.status)) {
                return new SlotPlacement(readPillboxOrNull(patientId), ApiException.response(201));
            }
            CareContracts.Pillbox fresh = readPillboxOrNull(patientId);
            Long releasedNo = released ? restoreNo : null;
            return new SlotPlacement(
                    CareContracts.applyActiveAssignment(
                            fresh != null ? fresh : knownPillbox,
                            assignment,
                            releasedNo
                    ),
                    null
            );
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                throw exception;
            }
            if (exception.failure() == ApiFailure.Kind.RESPONSE) {
                CareContracts.Pillbox fresh = readPillboxOrNull(patientId);
                if (placementMatches(fresh, prescriptionId, compartmentNo)) {
                    return new SlotPlacement(fresh, null);
                }
            }
            if (released && restoreNo != null) {
                restoreServerCompartment(prescriptionId, restoreNo);
            }
            return new SlotPlacement(readPillboxOrNull(patientId), exception);
        }
    }

    private boolean restoreServerCompartment(long prescriptionId, long compartmentNo) throws ApiException {
        try {
            org.json.JSONObject body = CareApi.assignCompartment(
                    getApplicationContext(),
                    prescriptionId,
                    compartmentNo
            );
            return CareContracts.assignmentCompleted(
                    CareContracts.readAssignment(body),
                    prescriptionId,
                    compartmentNo
            );
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                throw exception;
            }
            return false;
        }
    }

    private CareContracts.Pillbox readPillboxOrNull(long patientId) throws ApiException {
        try {
            return CareApi.pillbox(getApplicationContext(), patientId);
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                throw exception;
            }
            return null;
        }
    }

    private static boolean placementMatches(
            CareContracts.Pillbox pillbox,
            long prescriptionId,
            int compartmentNo
    ) {
        if (pillbox == null || CareContracts.assignmentAmbiguous(pillbox, prescriptionId)) {
            return false;
        }
        CareContracts.Compartment placed = CareContracts.compartmentForPrescription(pillbox, prescriptionId);
        return placed != null
                && placed.compartmentNo == compartmentNo
                && CareContracts.ACTIVE.equals(placed.state);
    }

    private void publishAssignedPillbox(CareContracts.Pillbox snapshot) {
        publishListPillbox(snapshot);
        pillboxEpoch++;
        pillboxView = MainScreens.CareView.READY;
        pillboxSnapshot = snapshot;
        pillboxMessage = null;
        if (navigator.isMainArea() && navigator.currentTab() == ScreenId.PILLBOX) {
            refresh();
        }
    }

    private void finishServerSlotRequest() {
        serverSlotBusy = false;
        clearProgress();
    }

    private void finishSlotNotice(String message) {
        finishServerSlotRequest();
        if (!isAlive()) {
            return;
        }
        if (message != null && !message.isEmpty()) {
            showMessage(message);
        }
    }

    private void clearServerSlot() {
        serverSlotPrescriptionId = null;
        serverSlotChoice = null;
        serverSlotPillbox = null;
    }

    private void refreshOpenList() {
        if (isMedicationListSurface(navigator.currentScreen())) {
            refresh();
        }
    }

    private void syncListPillbox(String screen) {
        boolean list = isSignedIn() && isMedicationListSurface(screen);
        if (!list) {
            listPillboxArmed = false;
            return;
        }
        ensureListPillbox();
    }

    private void ensureListPillbox() {
        if (!isSignedIn() || listPillboxArmed) {
            return;
        }
        listPillboxArmed = true;
        int epoch = ++listPillboxEpoch;
        if (listPillbox == null) {
            listPillboxView = MainScreens.CareView.LOADING;
        }
        String accountType = AuthTokenStore.accountType(this);
        ApiClient.execute(() -> {
            try {
                java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(
                        getApplicationContext()
                );
                SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                        patients,
                        accountType
                );
                if (target.resolution != SettingsContracts.Resolution.ONE) {
                    runOnUiThread(() -> applyListPillbox(epoch, MainScreens.CareView.BLOCKED, null));
                    return;
                }
                SettingsContracts.ManagedPatient patient = findPatient(patients, target.patientId);
                if (CareContracts.shareWithheld(patient, CareContracts.DEVICE_EVENT)) {
                    runOnUiThread(() -> applyListPillbox(epoch, MainScreens.CareView.BLOCKED, null));
                    return;
                }
                CareContracts.Pillbox loaded = CareApi.pillbox(getApplicationContext(), target.patientId);
                runOnUiThread(() -> applyListPillbox(epoch, MainScreens.CareView.READY, loaded));
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    if (!isAlive() || epoch != listPillboxEpoch) {
                        return;
                    }
                    if (exception.endsSession()) {
                        applyListPillbox(epoch, MainScreens.CareView.FAILED, null);
                        showApiFailure(getString(R.string.med_list_title), exception);
                        return;
                    }
                    if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                        forgetLocalPillbox();
                        applyListPillbox(epoch, MainScreens.CareView.ABSENT, null);
                        return;
                    }
                    MainScreens.CareView failed = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                            ? MainScreens.CareView.BLOCKED
                            : MainScreens.CareView.FAILED;
                    applyListPillbox(epoch, failed, null);
                });
            }
        });
    }

    private void applyListPillbox(int epoch, MainScreens.CareView view, CareContracts.Pillbox snapshot) {
        if (!isAlive() || epoch != listPillboxEpoch) {
            return;
        }
        listPillboxView = view;
        listPillbox = snapshot;
        refreshOpenList();
    }

    private void publishListPillbox(CareContracts.Pillbox snapshot) {
        listPillboxEpoch++;
        listPillboxView = MainScreens.CareView.READY;
        listPillbox = snapshot;
        listPillboxArmed = isMedicationListSurface(navigator.currentScreen());
    }

    private void publishListPillboxAbsent() {
        listPillboxEpoch++;
        listPillboxView = MainScreens.CareView.ABSENT;
        listPillbox = null;
        listPillboxArmed = isMedicationListSurface(navigator.currentScreen());
    }

    private void resetListPillbox() {
        listPillboxArmed = false;
        listPillboxEpoch++;
        listPillboxView = MainScreens.CareView.LOADING;
        listPillbox = null;
        serverSlotBusy = false;
        clearServerSlot();
    }

    @Override
    public void markUncheckedDose(long scheduledDoseId) {
        if (!isSignedIn()) {
            checkHomeDoses();
            return;
        }
        if (!homeWrites) {
            showMessage(getString(R.string.med_readonly));
            return;
        }
        if (scheduledDoseId <= 0L || homeWriteBusy || !doseChecksInFlight.add(scheduledDoseId)) {
            return;
        }
        runHomeWrite(() -> {
            try {
                HomeContracts.Dose dose = HomeApi.markTaken(
                        getApplicationContext(),
                        scheduledDoseId,
                        HomeContracts.takenAt(System.currentTimeMillis())
                );
                return serverStatusText(dose.status);
            } finally {
                doseChecksInFlight.remove(scheduledDoseId);
            }
        });
    }

    @Override
    public void checkHomeDoses() {
        if (isSignedIn()) {
            if (!homeWrites) {
                showMessage(getString(R.string.med_readonly));
                return;
            }
            if (homeToday == null
                    || homeToday.kind != HomeContracts.Section.Kind.READY
                    || homeToday.value == null) {
                showMessage(getString(R.string.home_remaining_empty));
                return;
            }
            RecordScreens.checkServerDoses(this, homeToday.value, this);
            return;
        }
        RecordScreens.checkLocalDoses(this, YakyeonRepositoryProvider.get().medications(), this);
    }

    @Override
    public void recordServerDose(long doseId, long takenAtMillis) {
        if (!homeWrites) {
            showMessage(getString(R.string.med_readonly));
            return;
        }
        runHomeWrite(() -> {
            HomeContracts.Dose dose = HomeApi.markTaken(
                    getApplicationContext(),
                    doseId,
                    HomeContracts.takenAt(takenAtMillis)
            );
            return serverStatusText(dose.status);
        });
    }

    @Override
    public void revertServerDose(long doseId) {
        if (!homeWrites) {
            showMessage(getString(R.string.med_readonly));
            return;
        }
        runHomeWrite(() -> {
            HomeContracts.Dose dose = HomeApi.markNotTaken(getApplicationContext(), doseId);
            return serverStatusText(dose.status);
        });
    }

    @Override
    public void selectServerScheduleDate(java.time.LocalDate date) {
        scheduleDate = date;
        scheduleLoaded = false;
        ensureScheduleLoaded();
        refresh();
    }

    @Override
    public void selectServerHistoryStatus(String statusOrNull) {
        historyStatus = statusOrNull;
        historyLoaded = false;
        ensureHistoryLoaded();
        refresh();
    }

    @Override
    public void selectServerHistoryRange(java.time.LocalDate from, java.time.LocalDate to) {
        if (HomeContracts.historySpanDays(from, to) == null) {
            showMessage(getString(R.string.med_history_server_max));
            return;
        }
        historyFrom = from;
        historyTo = to;
        historyLoaded = false;
        ensureHistoryLoaded();
        refresh();
    }

    private void onPrescriptionChosen(Uri uri) {
        if (uri == null) {
            return;
        }
        if (homeWriteBusy) {
            return;
        }
        homeWriteBusy = true;
        showProgress(getString(R.string.home_loading));
        ApiClient.execute(() -> {
            try {
                byte[] bytes = readImage(uri);
                String mime = getContentResolver().getType(uri);
                if (bytes.length > HomeContracts.IMAGE_MAX_BYTES) {
                    runOnUiThread(() -> finishHomeWrite(getString(R.string.med_image_size)));
                    return;
                }
                if (!HomeContracts.imageAllowed(bytes, mime)) {
                    runOnUiThread(() -> finishHomeWrite(getString(R.string.med_image_type)));
                    return;
                }
                PatientGate gate = gatePatients();
                if (gate.blocked != null) {
                    runOnUiThread(() -> finishHomeWrite(gate.blocked));
                    return;
                }
                String filename = "image/png".equals(mime) ? "prescription.png" : "prescription.jpg";
                String contentType = "image/png".equals(mime) ? "image/png" : "image/jpeg";
                HomeContracts.OcrDraft recognized = HomeApi.ocr(
                        getApplicationContext(),
                        gate.patient.patientId,
                        filename,
                        contentType,
                        bytes
                );
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    if (!isAlive()) {
                        return;
                    }
                    MedScreens.fillOcr(YakyeonRepositoryProvider.get().medications(), recognized);
                    refresh();
                    showMessage(recognized.recognized.isEmpty()
                            ? getString(R.string.med_ocr_empty)
                            : getString(R.string.med_ocr_mock));
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    showApiFailure(getString(R.string.med_envelope_title), exception);
                });
            } catch (java.io.IOException exception) {
                runOnUiThread(() -> finishHomeWrite(getString(R.string.med_image_type)));
            }
        });
    }

    private void saveServerSchedule(
            long prescriptionId,
            java.util.Map<com.example.yakyeon2.data.DoseRound, Integer> minutes,
            java.util.Set<com.example.yakyeon2.data.DoseRound> selected
    ) {
        java.util.List<String> slots = new java.util.ArrayList<>();
        java.util.List<String> times = new java.util.ArrayList<>();
        addSchedule(slots, times, minutes, selected, com.example.yakyeon2.data.DoseRound.MORNING, HomeContracts.SLOT_MORNING);
        addSchedule(slots, times, minutes, selected, com.example.yakyeon2.data.DoseRound.LUNCH, HomeContracts.SLOT_LUNCH);
        addSchedule(slots, times, minutes, selected, com.example.yakyeon2.data.DoseRound.EVENING, HomeContracts.SLOT_DINNER);
        addSchedule(slots, times, minutes, selected, com.example.yakyeon2.data.DoseRound.BEDTIME, HomeContracts.SLOT_BEDTIME);
        if (slots.isEmpty() || slots.size() > 4) {
            showMessage(getString(R.string.med_required));
            return;
        }
        org.json.JSONObject body;
        try {
            body = HomeContracts.scheduleBody(slots, times);
        } catch (ApiException exception) {
            showMessage(getString(R.string.med_time_rule));
            return;
        }
        runHomeWrite(() -> {
            HomeApi.saveSchedule(getApplicationContext(), prescriptionId, body);
            return getString(R.string.med_server_saved);
        });
    }

    private static void addSchedule(
            java.util.List<String> slots,
            java.util.List<String> times,
            java.util.Map<com.example.yakyeon2.data.DoseRound, Integer> minutes,
            java.util.Set<com.example.yakyeon2.data.DoseRound> selected,
            com.example.yakyeon2.data.DoseRound round,
            String slot
    ) {
        if (!selected.contains(round) || minutes.get(round) == null) {
            return;
        }
        int value = minutes.get(round);
        slots.add(slot);
        times.add(String.format(java.util.Locale.US, "%02d:%02d", value / 60, value % 60));
    }

    private void runHomeWrite(HomeWrite work) {
        if (homeWriteBusy) {
            return;
        }
        homeWriteBusy = true;
        showProgress(getString(R.string.home_loading));
        ApiClient.execute(() -> {
            try {
                String notice = work.run();
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    if (!isAlive()) {
                        return;
                    }
                    showMessage(notice);
                    boxTakenWhileWriting = false;
                    reloadServerSurfaces();
                    ignoreBoxTakenUntil = android.os.SystemClock.elapsedRealtime() + 3000L;
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    showApiFailure(getString(R.string.tab_home), exception);
                    if (boxTakenWhileWriting) {
                        boxTakenWhileWriting = false;
                        onBoxTakenRefresh();
                    }
                });
            }
        });
    }

    private void finishHomeWrite(String message) {
        homeWriteBusy = false;
        clearProgress();
        if (message != null && !message.isEmpty()) {
            showMessage(message);
        }
    }

    private void failRegistration(String message) {
        finishHomeWrite(null);
        SavedToast.dismiss(this);
        if (message != null && !message.isEmpty()) {
            showMessage(message);
        }
    }

    private void reloadHomeAfterLinkSync() {
        scheduleLoaded = false;
        historyLoaded = false;
        homeArmed = false;
        ensureHomeLoaded();
        if (needsServerHome(navigator.currentScreen())) {
            refresh();
        }
    }

    private void onBoxTakenRefresh() {
        if (!isAlive() || !isSignedIn()) {
            Log.i("BoxTaken", "onBoxTakenRefresh skip alive=" + isAlive() + " signedIn=" + isSignedIn());
            return;
        }
        if (homeWriteBusy) {
            boxTakenWhileWriting = true;
            Log.i("BoxTaken", "onBoxTakenRefresh defer homeWriteBusy");
            return;
        }
        long now = android.os.SystemClock.elapsedRealtime();
        if (now < ignoreBoxTakenUntil || doseSurfacesJustReloaded()) {
            Log.i("BoxTaken", "onBoxTakenRefresh skip recentReload=" + doseSurfacesJustReloaded()
                    + " ignoreUntil=" + (now < ignoreBoxTakenUntil));
            return;
        }
        Log.i("BoxTaken", "onBoxTakenRefresh");
        reloadServerSurfaces();
    }

    private void invalidateDoseCaches() {
        doseSurfacesReloadedAt = android.os.SystemClock.elapsedRealtime();
        homeArmed = false;
        scheduleLoaded = false;
        historyLoaded = false;
    }

    private boolean doseSurfacesJustReloaded() {
        return android.os.SystemClock.elapsedRealtime() - doseSurfacesReloadedAt < 800L;
    }

    private void reloadServerSurfaces() {
        Log.i("BoxTaken", "reloadServerSurfaces screen=" + navigator.currentScreen()
                + " scheduleDate=" + scheduleDate
                + " history=" + historyFrom + ".." + historyTo
                + " historyStatus=" + historyStatus);
        invalidateDoseCaches();
        ensureHomeLoaded();
        if (showingSchedule()) {
            ensureScheduleLoaded();
        } else {
            Log.i("BoxTaken", "schedule GET skipped screenClosed");
        }
        if (MedScreen.HISTORY.equals(navigator.currentScreen())) {
            ensureHistoryLoaded();
        } else {
            Log.i("BoxTaken", "history GET skipped screenClosed");
        }
        Log.i("BoxTaken", "refresh after reload");
        refresh();
    }

    private boolean needsServerHome(String screen) {
        if (!isSignedIn()) {
            return false;
        }
        if (navigator.isMainArea() && navigator.currentTab() == ScreenId.HOME) {
            return true;
        }
        return MedScreen.LIST.equals(screen)
                || MedScreen.CONSENT.equals(screen)
                || MedScreen.ENVELOPE.equals(screen)
                || MedScreen.EDIT.equals(screen)
                || MedScreen.SLOT.equals(screen)
                || MedScreen.SCHEDULE.equals(screen)
                || MedScreen.HISTORY.equals(screen);
    }

    private void ensureHomeLoaded() {
        if (!isSignedIn() || homeArmed) {
            Log.i("BoxTaken", "ensureHomeLoaded skip signedIn=" + isSignedIn() + " homeArmed=" + homeArmed);
            return;
        }
        homeArmed = true;
        homeEpoch++;
        homeView = MainScreens.CareView.LOADING;
        loadHome(homeEpoch);
    }

    private void ensureScheduleLoaded() {
        if (!isSignedIn() || scheduleLoaded) {
            Log.i("BoxTaken", "ensureScheduleLoaded skip signedIn=" + isSignedIn()
                    + " scheduleLoaded=" + scheduleLoaded);
            return;
        }
        if (scheduleDate == null) {
            scheduleDate = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        }
        scheduleLoaded = true;
        scheduleEpoch++;
        scheduleView = MainScreens.CareView.LOADING;
        loadSchedule(scheduleEpoch, scheduleDate);
    }

    private void ensureHistoryLoaded() {
        if (!isSignedIn() || historyLoaded) {
            Log.i("BoxTaken", "ensureHistoryLoaded skip signedIn=" + isSignedIn()
                    + " historyLoaded=" + historyLoaded);
            return;
        }
        historyLoaded = true;
        historyEpoch++;
        historyView = MainScreens.CareView.LOADING;
        loadHistory(historyEpoch, historyFrom, historyTo, historyStatus);
    }

    private void loadHome(int epoch) {
        ApiClient.execute(() -> {
            try {
                PatientGate gate = gatePatients();
                if (gate.blocked != null) {
                    runOnUiThread(() -> applyHome(
                            epoch,
                            MainScreens.CareView.BLOCKED,
                            gate.blocked,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false,
                            null,
                            null
                    ));
                    return;
                }
                SettingsContracts.ManagedPatient patient = gate.patient;
                String linkedName = guardianPatientName(patient);
                java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
                HomeContracts.Section<HomeContracts.HomeSummary> summary = readSummary(patient);
                HomeContracts.Section<java.util.List<HomeContracts.Medication>> active = readActive(patient);
                HomeContracts.Section<CareContracts.Summary> interactions = readInteractions(patient);
                HomeContracts.Section<java.util.List<HomeContracts.Dose>> todayDoses = readToday(patient, today);
                HomeContracts.Section<java.util.List<HomeContracts.Prescription>> prescriptions = readPrescriptions(patient);
                boolean writes = CareContracts.writesAllowed(patient);
                runOnUiThread(() -> applyHome(
                        epoch,
                        MainScreens.CareView.READY,
                        null,
                        summary,
                        active,
                        interactions,
                        todayDoses,
                        prescriptions,
                        writes,
                        patient.patientId,
                        linkedName
                ));
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    applyHome(
                            epoch,
                            MainScreens.CareView.FAILED,
                            careMessage(exception),
                            null,
                            null,
                            null,
                            null,
                            null,
                            false,
                            null,
                            null
                    );
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.tab_home), exception);
                    }
                });
            }
        });
    }

    private HomeContracts.Section<HomeContracts.HomeSummary> readSummary(SettingsContracts.ManagedPatient patient)
            throws ApiException {
        if (CareContracts.shareWithheld(patient, HomeContracts.DOSE_RECORD)) {
            return HomeContracts.Section.withheld(getString(R.string.care_share_withheld));
        }
        try {
            return HomeContracts.Section.ready(HomeApi.homeSummary(getApplicationContext(), patient.patientId));
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                throw exception;
            }
            return failedSection(exception);
        }
    }

    private HomeContracts.Section<java.util.List<HomeContracts.Medication>> readActive(
            SettingsContracts.ManagedPatient patient
    ) throws ApiException {
        if (CareContracts.shareWithheld(patient, HomeContracts.MED_INFO)) {
            return HomeContracts.Section.withheld(getString(R.string.care_share_withheld));
        }
        try {
            return HomeContracts.Section.ready(HomeApi.medications(
                    getApplicationContext(),
                    patient.patientId,
                    HomeContracts.STATUS_ACTIVE
            ));
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                throw exception;
            }
            return failedSection(exception);
        }
    }

    private HomeContracts.Section<CareContracts.Summary> readInteractions(SettingsContracts.ManagedPatient patient)
            throws ApiException {
        if (CareContracts.shareWithheld(patient, CareContracts.DUR_INFO)) {
            return HomeContracts.Section.withheld(getString(R.string.care_share_withheld));
        }
        try {
            return HomeContracts.Section.ready(CareApi.summary(getApplicationContext(), patient.patientId));
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                throw exception;
            }
            return failedSection(exception);
        }
    }

    private HomeContracts.Section<java.util.List<HomeContracts.Dose>> readToday(
            SettingsContracts.ManagedPatient patient,
            java.time.LocalDate today
    ) throws ApiException {
        if (CareContracts.shareWithheld(patient, HomeContracts.DOSE_RECORD)) {
            return HomeContracts.Section.withheld(getString(R.string.care_share_withheld));
        }
        try {
            Log.i("BoxTaken", "GET " + HomeContracts.dosesPath(patient.patientId, today));
            java.util.List<HomeContracts.Dose> doses = HomeApi.doses(
                    getApplicationContext(),
                    patient.patientId,
                    today
            );
            logDoseRows("home", today, today, doses);
            return HomeContracts.Section.ready(doses);
        } catch (ApiException exception) {
            Log.i("BoxTaken", "home doses failed http=" + exception.status() + " code=" + exception.code());
            if (exception.endsSession()) {
                throw exception;
            }
            return failedSection(exception);
        }
    }

    private HomeContracts.Section<java.util.List<HomeContracts.Prescription>> readPrescriptions(
            SettingsContracts.ManagedPatient patient
    ) throws ApiException {
        if (CareContracts.shareWithheld(patient, HomeContracts.MED_INFO)) {
            return HomeContracts.Section.withheld(getString(R.string.care_share_withheld));
        }
        try {
            return HomeContracts.Section.ready(HomeApi.prescriptions(getApplicationContext(), patient.patientId));
        } catch (ApiException exception) {
            if (exception.endsSession()) {
                throw exception;
            }
            return failedSection(exception);
        }
    }

    private <T> HomeContracts.Section<T> failedSection(ApiException exception) {
        if (ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())) {
            return HomeContracts.Section.withheld(getString(R.string.care_share_withheld));
        }
        return HomeContracts.Section.failed(careMessage(exception));
    }

    private void applyHome(
            int epoch,
            MainScreens.CareView view,
            String message,
            HomeContracts.Section<HomeContracts.HomeSummary> summary,
            HomeContracts.Section<java.util.List<HomeContracts.Medication>> active,
            HomeContracts.Section<CareContracts.Summary> interactions,
            HomeContracts.Section<java.util.List<HomeContracts.Dose>> today,
            HomeContracts.Section<java.util.List<HomeContracts.Prescription>> prescriptions,
            boolean writes,
            Long patientId,
            String linkedPatientName
    ) {
        if (!isAlive() || epoch != homeEpoch) {
            Log.i("BoxTaken", "applyHome skip stale epoch");
            return;
        }
        homeView = view;
        homeMessage = message;
        homeSummary = summary;
        homeActive = active;
        homeInteractions = interactions;
        homeToday = today;
        homePrescriptions = prescriptions;
        homeWrites = writes;
        homePatientId = patientId;
        homeLinkedPatientName = linkedPatientName;
        boolean show = needsServerHome(navigator.currentScreen());
        Log.i("BoxTaken", "applyHome refresh=" + show + " view=" + view);
        if (show) {
            refresh();
        }
    }

    private void loadSchedule(int epoch, java.time.LocalDate date) {
        ApiClient.execute(() -> {
            try {
                PatientGate gate = gatePatients();
                if (gate.blocked != null) {
                    runOnUiThread(() -> applySchedule(epoch, MainScreens.CareView.BLOCKED, gate.blocked, null));
                    return;
                }
                if (CareContracts.shareWithheld(gate.patient, HomeContracts.DOSE_RECORD)) {
                    runOnUiThread(() -> applySchedule(
                            epoch,
                            MainScreens.CareView.BLOCKED,
                            getString(R.string.care_share_withheld),
                            null
                    ));
                    return;
                }
                Log.i("BoxTaken", "GET " + HomeContracts.dosesPath(gate.patient.patientId, date));
                java.util.List<HomeContracts.Dose> doses = HomeApi.doses(
                        getApplicationContext(),
                        gate.patient.patientId,
                        date
                );
                logDoseRows("schedule", date, date, doses);
                runOnUiThread(() -> applySchedule(epoch, MainScreens.CareView.READY, null, doses));
            } catch (ApiException exception) {
                Log.i("BoxTaken", "schedule doses failed http=" + exception.status() + " code=" + exception.code());
                runOnUiThread(() -> {
                    MainScreens.CareView view = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                            ? MainScreens.CareView.BLOCKED
                            : MainScreens.CareView.FAILED;
                    applySchedule(epoch, view, careMessage(exception), null);
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.med_schedule_title), exception);
                    }
                });
            }
        });
    }

    private void applySchedule(
            int epoch,
            MainScreens.CareView view,
            String message,
            java.util.List<HomeContracts.Dose> doses
    ) {
        if (!isAlive() || epoch != scheduleEpoch) {
            Log.i("BoxTaken", "applySchedule skip stale epoch");
            return;
        }
        scheduleView = view;
        scheduleMessage = message;
        scheduleDoses = doses;
        boolean show = showingSchedule();
        Log.i("BoxTaken", "applySchedule refresh=" + show + " view=" + view + " selected=" + scheduleDate);
        if (show) {
            refresh();
        }
    }

    private void loadHistory(
            int epoch,
            java.time.LocalDate from,
            java.time.LocalDate to,
            String status
    ) {
        ApiClient.execute(() -> {
            try {
                PatientGate gate = gatePatients();
                if (gate.blocked != null) {
                    runOnUiThread(() -> applyHistory(epoch, MainScreens.CareView.BLOCKED, gate.blocked, null));
                    return;
                }
                if (CareContracts.shareWithheld(gate.patient, HomeContracts.DOSE_RECORD)) {
                    runOnUiThread(() -> applyHistory(
                            epoch,
                            MainScreens.CareView.BLOCKED,
                            getString(R.string.care_share_withheld),
                            null
                    ));
                    return;
                }
                Log.i("BoxTaken", "GET " + HomeContracts.historyPath(gate.patient.patientId, from, to, status));
                java.util.List<HomeContracts.Dose> doses = HomeApi.doseHistory(
                        getApplicationContext(),
                        gate.patient.patientId,
                        from,
                        to,
                        status
                );
                logDoseRows("history", from, to, doses);
                runOnUiThread(() -> applyHistory(epoch, MainScreens.CareView.READY, null, doses));
            } catch (ApiException exception) {
                Log.i("BoxTaken", "history doses failed http=" + exception.status() + " code=" + exception.code());
                runOnUiThread(() -> {
                    MainScreens.CareView view = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                            ? MainScreens.CareView.BLOCKED
                            : MainScreens.CareView.FAILED;
                    String text = ApiCodes.SHARE_NOT_GRANTED.equals(exception.code())
                            ? getString(R.string.care_share_withheld)
                            : careMessage(exception);
                    applyHistory(epoch, view, text, null);
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.med_history_title), exception);
                    }
                });
            }
        });
    }

    private void applyHistory(
            int epoch,
            MainScreens.CareView view,
            String message,
            java.util.List<HomeContracts.Dose> doses
    ) {
        if (!isAlive() || epoch != historyEpoch) {
            Log.i("BoxTaken", "applyHistory skip stale epoch");
            return;
        }
        historyView = view;
        historyMessage = message;
        historyDoses = doses;
        boolean show = MedScreen.HISTORY.equals(navigator.currentScreen());
        Log.i("BoxTaken", "applyHistory refresh=" + show + " view=" + view
                + " range=" + historyFrom + ".." + historyTo);
        if (show) {
            refresh();
        }
    }

    private static void logDoseRows(
            String source,
            java.time.LocalDate queryStart,
            java.time.LocalDate queryEnd,
            java.util.List<HomeContracts.Dose> doses
    ) {
        if (doses == null) {
            Log.i("BoxTaken", source + " doses=null query=" + queryStart + ".." + queryEnd);
            return;
        }
        Log.i("BoxTaken", source + " count=" + doses.size() + " query=" + queryStart + ".." + queryEnd);
        java.time.Instant now = java.time.Instant.now();
        for (HomeContracts.Dose dose : doses) {
            if (dose == null) {
                continue;
            }
            java.time.LocalDate day = HomeContracts.seoulDate(dose.scheduledAt);
            boolean match = day != null && queryStart != null && queryEnd != null
                    && !day.isBefore(queryStart) && !day.isAfter(queryEnd);
            String shown = HomeContracts.scheduleStatus(dose, now);
            Log.i("BoxTaken", source
                    + " id=" + dose.scheduledDoseId
                    + " at=" + dose.scheduledAt
                    + " status=" + dose.status
                    + " taken_at=" + (dose.takenAt == null ? "" : dose.takenAt)
                    + " shown=" + shown
                    + " queryMatch=" + match);
        }
    }

    private boolean awaitingPatientLink() {
        return homeView == MainScreens.CareView.BLOCKED
                && AccountRoles.GUARDIAN.equals(AuthTokenStore.accountType(this))
                && getString(R.string.settings_patient_none).equals(homeMessage);
    }

    private PatientGate gatePatients() throws ApiException {
        java.util.List<SettingsContracts.ManagedPatient> patients = SettingsApi.managedPatients(getApplicationContext());
        SettingsContracts.ResolvedPatient target = SettingsContracts.resolveForSettings(
                patients,
                AuthTokenStore.accountType(this)
        );
        if (target.resolution != SettingsContracts.Resolution.ONE) {
            String text = getString(target.resolution == SettingsContracts.Resolution.SEVERAL
                    ? R.string.settings_patient_several
                    : R.string.settings_patient_none);
            return new PatientGate(null, text);
        }
        SettingsContracts.ManagedPatient patient = findPatient(patients, target.patientId);
        if (patient == null) {
            return new PatientGate(null, getString(R.string.settings_patient_none));
        }
        return new PatientGate(patient, null);
    }

    private void clearHomeCache() {
        homeArmed = false;
        homeEpoch++;
        homeView = MainScreens.CareView.LOADING;
        homeMessage = null;
        homeSummary = null;
        homeActive = null;
        homeInteractions = null;
        homeToday = null;
        homePrescriptions = null;
        homePatientId = null;
        homeLinkedPatientName = null;
        homeWrites = false;
        homeWriteBusy = false;
        scheduleLoaded = false;
        scheduleEpoch++;
        scheduleView = MainScreens.CareView.LOADING;
        scheduleMessage = null;
        scheduleDoses = null;
        historyLoaded = false;
        historyEpoch++;
        historyView = MainScreens.CareView.LOADING;
        historyMessage = null;
        historyDoses = null;
        historyStatus = null;
        historyFrom = null;
        historyTo = null;
        resetListPillbox();
    }

    private void offerCatchUp() {
        if (!isAlive() || !isSignedIn() || !homeWrites) {
            return;
        }
        if (!navigator.isMainArea() || navigator.currentTab() != ScreenId.HOME) {
            return;
        }
        if (homeToday == null || homeToday.kind != HomeContracts.Section.Kind.READY) {
            return;
        }
        HomeContracts.Dose dose = HomeContracts.catchUpDose(homeToday.value, java.time.Instant.now());
        if (dose == null) {
            return;
        }
        notifyCatchUp(dose);
        if (catchUpAsked.contains(dose.scheduledDoseId)) {
            return;
        }
        if (catchUpDialog != null && catchUpDialog.isShowing()) {
            return;
        }
        catchUpAsked.add(dose.scheduledDoseId);
        String title = getString(
                R.string.med_catchup_title,
                HomeContracts.clock(dose.scheduledAt),
                MainScreens.slotLabel(this, dose.timingSlot)
        );
        screenContainer.post(() -> {
            if (!isAlive() || !navigator.isMainArea() || navigator.currentTab() != ScreenId.HOME) {
                catchUpAsked.remove(dose.scheduledDoseId);
                return;
            }
            if (catchUpDialog != null && catchUpDialog.isShowing()) {
                return;
            }
            catchUpDialog = RecordScreens.askTakenTime(this, dose, this, title);
            catchUpDialog.setOnDismissListener(dialog -> {
                if (catchUpDialog == dialog) {
                    catchUpDialog = null;
                }
            });
        });
    }

    private void openPushIfPresent(Intent intent) {
        if (intent == null) {
            return;
        }
        String id = intent.getStringExtra(YakyeonNotifications.EXTRA_NOTIFICATION_ID);
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        intent.removeExtra(YakyeonNotifications.EXTRA_NOTIFICATION_ID);
        if (id.trim().equals(openedPushId)) {
            return;
        }
        openedPushId = id.trim();
        if (!isSignedIn()) {
            pendingPushId = id.trim();
            return;
        }
        loadPushDetail(id.trim());
    }

    private void loadPushDetail(String notificationId) {
        ApiClient.execute(() -> {
            try {
                NotificationInbox.Item item = NotificationInbox.find(getApplicationContext(), notificationId);
                if (item != null) {
                    try {
                        NotificationInbox.markRead(getApplicationContext(), item.id);
                    } catch (ApiException ignored) {
                        Log.d(FcmRegistrar.TAG, "알림 읽음 처리 실패");
                    }
                }
                runOnUiThread(() -> {
                    if (!isAlive()) {
                        return;
                    }
                    if (item == null) {
                        showNotice(getString(R.string.app_name), getString(R.string.push_detail_missing), null);
                        return;
                    }
                    String title = item.title == null || item.title.isEmpty()
                            ? getString(R.string.app_name)
                            : item.title;
                    String body = item.body == null ? "" : item.body;
                    showNotice(title, body, null);
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> showApiFailure(getString(R.string.app_name), exception));
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(FcmRegistrar.TAG, "알림 권한 허용됨");
            return;
        }
        if (notificationPermissionRequested || noticePermissionAsked) {
            return;
        }
        notificationPermissionRequested = true;
        noticePermissionAsked = true;
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void notifyCatchUp(HomeContracts.Dose dose) {
        pendingCatchUp = dose;
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (!noticePermissionAsked) {
                noticePermissionAsked = true;
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
            return;
        }
        postCatchUpNotice(dose);
    }

    private void postCatchUpNotice(HomeContracts.Dose dose) {
        if (dose == null || !isAlive()) {
            return;
        }
        String when = HomeContracts.clock(dose.scheduledAt);
        String slot = MainScreens.slotLabel(this, dose.timingSlot);
        DoseCatchUpNotice.post(
                this,
                dose,
                getString(R.string.med_catchup_notice_title),
                getString(R.string.med_catchup_notice_body, when, slot)
        );
    }

    private java.util.List<HomeContracts.Prescription> prescriptionRows() {
        if (homePrescriptions != null && homePrescriptions.kind == HomeContracts.Section.Kind.READY) {
            return homePrescriptions.value;
        }
        return null;
    }

    private MainScreens.CareView prescriptionView() {
        if (homeView != MainScreens.CareView.READY) {
            return homeView;
        }
        if (homePrescriptions == null) {
            return MainScreens.CareView.LOADING;
        }
        if (homePrescriptions.kind == HomeContracts.Section.Kind.READY) {
            return MainScreens.CareView.READY;
        }
        if (homePrescriptions.kind == HomeContracts.Section.Kind.WITHHELD) {
            return MainScreens.CareView.BLOCKED;
        }
        return MainScreens.CareView.FAILED;
    }

    private String prescriptionMessage() {
        if (homeView != MainScreens.CareView.READY) {
            return homeMessage;
        }
        return homePrescriptions == null ? null : homePrescriptions.message;
    }

    private HomeContracts.Prescription prescriptionFor(long prescriptionId) {
        if (homePrescriptions == null || homePrescriptions.value == null) {
            return null;
        }
        for (HomeContracts.Prescription prescription : homePrescriptions.value) {
            if (prescription.prescriptionId == prescriptionId) {
                return prescription;
            }
        }
        return null;
    }

    private String hospitalFor(long prescriptionId) {
        if (homePrescriptions == null || homePrescriptions.value == null) {
            return "";
        }
        for (HomeContracts.Prescription prescription : homePrescriptions.value) {
            if (prescription.prescriptionId == prescriptionId) {
                return prescription.hospitalName == null ? "" : prescription.hospitalName;
            }
        }
        return "";
    }

    private String serverStatusText(String status) {
        if (HomeContracts.STATUS_NORMAL.equals(status)) {
            return getString(R.string.med_status_completed);
        }
        if (HomeContracts.STATUS_DELAYED.equals(status)) {
            return getString(R.string.med_status_delayed);
        }
        if (HomeContracts.STATUS_MISSED.equals(status)) {
            return getString(R.string.med_status_missed);
        }
        if (HomeContracts.STATUS_PENDING.equals(status)) {
            return getString(R.string.med_status_scheduled);
        }
        return status == null ? "" : status;
    }

    private static com.example.yakyeon2.data.DoseRound roundForSlot(String slot) {
        if (HomeContracts.SLOT_MORNING.equals(slot)) {
            return com.example.yakyeon2.data.DoseRound.MORNING;
        }
        if (HomeContracts.SLOT_LUNCH.equals(slot)) {
            return com.example.yakyeon2.data.DoseRound.LUNCH;
        }
        if (HomeContracts.SLOT_DINNER.equals(slot)) {
            return com.example.yakyeon2.data.DoseRound.EVENING;
        }
        if (HomeContracts.SLOT_BEDTIME.equals(slot)) {
            return com.example.yakyeon2.data.DoseRound.BEDTIME;
        }
        return null;
    }

    private byte[] readImage(Uri uri) throws java.io.IOException {
        try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new java.io.IOException();
            }
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) >= 0) {
                total += count;
                if (total > HomeContracts.IMAGE_MAX_BYTES) {
                    return new byte[HomeContracts.IMAGE_MAX_BYTES + 1];
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private interface HomeWrite {
        String run() throws ApiException;
    }

    private static final class PatientGate {
        private final SettingsContracts.ManagedPatient patient;
        private final String blocked;

        private PatientGate(SettingsContracts.ManagedPatient patient, String blocked) {
            this.patient = patient;
            this.blocked = blocked;
        }
    }

    private static final class SlotPlacement {
        private final CareContracts.Pillbox pillbox;
        private final ApiException error;

        private SlotPlacement(CareContracts.Pillbox pillbox, ApiException error) {
            this.pillbox = pillbox;
            this.error = error;
        }
    }

    private static int menuFor(ScreenId screen) {
        switch (screen) {
            case HOME:
                return R.id.tab_home;
            case SCHEDULE:
                return R.id.tab_home;
            case GRAPH:
                return R.id.tab_graph;
            case PILLBOX:
                return R.id.tab_pillbox;
            case SETTINGS:
                return R.id.tab_settings;
            default:
                return R.id.tab_home;
        }
    }

    private static ScreenId screenFor(int menuId) {
        if (menuId == R.id.tab_graph) {
            return ScreenId.GRAPH;
        }
        if (menuId == R.id.tab_pillbox) {
            return ScreenId.PILLBOX;
        }
        if (menuId == R.id.tab_settings) {
            return ScreenId.SETTINGS;
        }
        return ScreenId.HOME;
    }
}
