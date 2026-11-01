package com.example.yakyeon2;

import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.example.yakyeon2.api.NotificationSync;
import com.example.yakyeon2.api.SettingsApi;
import com.example.yakyeon2.api.SettingsContracts;
import com.example.yakyeon2.api.UserApi;
import com.example.yakyeon2.auth.AuthFlow;
import com.example.yakyeon2.auth.AuthScreens;
import com.example.yakyeon2.auth.AuthValidation;
import com.example.yakyeon2.auth.SignupConsent;
import com.example.yakyeon2.data.AccountRoles;
import com.example.yakyeon2.data.AccountType;
import com.example.yakyeon2.data.AuthTokenStore;
import com.example.yakyeon2.data.ClinicalShareOptions;
import com.example.yakyeon2.data.LegalMarks;
import com.example.yakyeon2.data.LocalAccountData;
import com.example.yakyeon2.data.LocalAppSettings;
import com.example.yakyeon2.data.NotificationPreferences;
import com.example.yakyeon2.data.ProfileSnapshot;
import com.example.yakyeon2.data.LocalMedicationState;
import com.example.yakyeon2.data.LocalMedicineNames;
import com.example.yakyeon2.data.YakyeonRepositoryProvider;
import com.example.yakyeon2.data.YakyeonSession;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.EnvelopeDraft;
import com.example.yakyeon2.main.MainScreens;
import com.example.yakyeon2.main.MedicineNamePicker;
import com.example.yakyeon2.main.MedScreens;
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
import com.example.yakyeon2.ui.ShortMessage;
import com.example.yakyeon2.ui.YakyeonHeader;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.color.MaterialColors;

public class MainActivity extends AppCompatActivity
        implements AuthScreens.Host, MainScreens.Host, MedScreens.Host, SettingsScreens.AccountHost {

    private static final String STATE_TAB = "state_tab";
    private static final String STATE_NON_MAIN = "state_non_main";
    private static final String STATE_AUTH = "state_auth";

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
    private AuthContracts.PreparedTerms signupTerms;
    private long verificationId;
    private String verifiedPhone = "";
    private final java.util.List<SettingsScreens.LinkLine> managedLines = new java.util.ArrayList<>();
    private final java.util.List<SettingsScreens.LinkLine> guardianLines = new java.util.ArrayList<>();
    private final java.util.List<SettingsContracts.AgreementRecord> agreementRecords = new java.util.ArrayList<>();
    private LegalMarks agreementMarks = new LegalMarks();
    private boolean showGuardians;
    private boolean careReady;
    private boolean agreementsReady;
    private SettingsScreens.DevicePanel devicePanel;
    private boolean deviceReady;
    private boolean deviceWritesAllowed;
    private String deviceStatusMessage;
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
    private final ActivityResultLauncher<String> prescriptionImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::onPrescriptionChosen
    );
    private boolean notificationReady;
    private Snackbar requestProgress;
    private String progressText;
    private String accountSyncKey;
    private int notificationEpoch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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
        }
        render();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_AUTH, navigator.isInAuthFlow());
        outState.putString(STATE_TAB, navigator.currentTab().name());
        outState.putStringArray(STATE_NON_MAIN, navigator.nonMainStack().toArray(new String[0]));
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
    public void onLogin(String id, String password) {
        switch (AuthValidation.login(id, password)) {
            case EMPTY_ID:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_id));
                break;
            case EMPTY_PASSWORD:
                ShortMessage.show(screenContainer, getString(R.string.auth_need_password));
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
        showProgress(getString(R.string.auth_login_loading));
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
        if (authRequestInFlight) {
            return;
        }
        authRequestInFlight = true;
        showProgress(getString(R.string.auth_login_loading));
        ApiClient.execute(() -> {
            try {
                long id = AuthApi.sendPhoneCode(getApplicationContext(), digits);
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    verificationId = id;
                    verifiedPhone = digits;
                    ShortMessage.show(screenContainer, getString(R.string.auth_code_sent));
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    showApiFailure(getString(R.string.auth_request_code), exception);
                });
            }
        });
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
    public void onDevHome() {
        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (DevHomeGate.open(navigator, debuggable)) {
            render();
        }
    }

    @Override
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
    public void refresh() {
        render();
    }

    @Override
    public void showPillboxTab() {
        navigator.selectTab(ScreenId.PILLBOX);
        render();
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
        if (verificationId <= 0L || !digits.equals(verifiedPhone)) {
            ShortMessage.show(
                    screenContainer,
                    verificationId <= 0L
                            ? getString(R.string.auth_verify_first)
                            : getString(R.string.auth_phone_changed)
            );
            return;
        }
        if (code == null || code.trim().isEmpty()) {
            ShortMessage.show(screenContainer, getString(R.string.auth_code_need));
            return;
        }
        if (!AuthContracts.isVerificationCode(code.trim())) {
            ShortMessage.show(screenContainer, getString(R.string.auth_code_format));
            return;
        }
        String accountType = AccountRoles.toServer(
                YakyeonRepositoryProvider.get().signupDraft().getAccountType()
        );
        if (accountType == null) {
            ShortMessage.show(screenContainer, getString(R.string.auth_need_type));
            return;
        }
        if (authRequestInFlight) {
            return;
        }
        authRequestInFlight = true;
        showProgress(getString(R.string.auth_login_loading));
        String loginId = id.trim();
        String birth = AuthContracts.isoDate(birthdate);
        String verificationCode = code.trim();
        ApiClient.execute(() -> {
            try {
                AuthContracts.PhoneConfirm confirm = AuthApi.confirmPhone(
                        getApplicationContext(),
                        verificationId,
                        verificationCode
                );
                if (!confirm.verified) {
                    runOnUiThread(() -> {
                        authRequestInFlight = false;
                        clearProgress();
                        if (isAlive()) {
                            ShortMessage.show(screenContainer, getString(R.string.auth_code_wrong));
                        }
                    });
                    return;
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
                        verificationId,
                        confirm.token,
                        signupTerms
                );
                runOnUiThread(() -> {
                    authRequestInFlight = false;
                    clearProgress();
                    if (!isAlive()) {
                        return;
                    }
                    verificationId = 0L;
                    verifiedPhone = "";
                    signupTerms = null;
                    authFlow.returnToLogin();
                    render();
                    showNotice(getString(R.string.auth_signup), getString(R.string.auth_signup_created), null);
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
        if (!MedScreen.SLOT.equals(navigator.currentScreen())) {
            YakyeonRepositoryProvider.get().medications().cancelSlotAssign();
        }
        if (SettingsScreens.PILLBOX.equals(navigator.currentScreen())
                && !isSignedIn()
                && !YakyeonRepositoryProvider.get().medications().pillboxConnected()) {
            navigator.selectTab(ScreenId.PILLBOX);
        }
        boolean main = navigator.isMainArea();
        String screen = navigator.currentScreen();
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
            header.setActionIcon(R.drawable.ic_yakyeon_comment, v -> {
                if (!MedScreen.HISTORY.equals(navigator.currentScreen())) {
                    openScreen(MedScreen.HISTORY);
                }
            });
        } else if (!login && !navigator.isInAuthFlow() && !MedScreen.HISTORY.equals(screen)) {
            header.setActionIcon(R.drawable.ic_yakyeon_comment, v -> openScreen(MedScreen.HISTORY));
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
        if (isSignedIn() && MedScreen.SCHEDULE.equals(screen)) {
            ensureScheduleLoaded();
        }
        if (isSignedIn() && MedScreen.HISTORY.equals(screen)) {
            ensureHistoryLoaded();
        }

        screenContainer.removeAllViews();
        View content;
        if (login) {
            content = AuthScreens.login(this, this);
        } else if (AuthScreen.SIGNUP_TYPE.equals(screen)) {
            content = AuthScreens.signupType(
                    this,
                    YakyeonRepositoryProvider.get().signupDraft().getAccountType(),
                    this
            );
        } else if (AuthScreen.SIGNUP_FORM.equals(screen)) {
            content = AuthScreens.signupForm(this, this);
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
        maybeSyncAccountScreens(navigator.currentScreen());
        if (AuthScreen.LOGIN.equals(navigator.currentScreen())) {
            setLoginEnabled(!loginInFlight);
        }
        if (progressText != null && (requestProgress == null || !requestProgress.isShown())) {
            showProgress(progressText);
        }
        syncConsentDialog(screen);
        syncAlarmDialog(screen);
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
        if (isSignedIn()) {
            return MainScreens.serverHome(
                    this,
                    homeView,
                    homeMessage,
                    homeSummary,
                    homeActive,
                    homeInteractions,
                    homeToday,
                    this,
                    debuggable()
            );
        }
        MedicationStore store = YakyeonRepositoryProvider.get().medications();
        return MainScreens.home(this, session, store, this, debuggable());
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
            return RecordScreens.schedule(this, store, this);
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
        devicePanel = null;
        deviceWritesAllowed = false;
        deviceStatusMessage = null;
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
        if (!beginAccountRequest(getString(R.string.settings_noti_saving))) {
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
                        finishAccountRequest();
                        if (!isAlive()) {
                            return;
                        }
                        showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_read_only));
                    });
                    return;
                }
                java.util.List<SettingsContracts.AgreementRecord> records = SettingsApi.agreements(
                        getApplicationContext()
                );
                Long versionId = SettingsContracts.versionId(records, SettingsContracts.DEVICE);
                SettingsContracts.AgreementRecord deviceTerms = deviceAgreement(records);
                if (versionId == null || deviceTerms == null) {
                    runOnUiThread(() -> {
                        finishAccountRequest();
                        if (!isAlive()) {
                            return;
                        }
                        showNotice(
                                getString(R.string.pillbox_settings),
                                getString(R.string.pillbox_consent_missing_version)
                        );
                    });
                    return;
                }
                if (!deviceTerms.agreed) {
                    SettingsApi.agree(getApplicationContext(), versionId, true);
                }
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_qr_required));
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
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
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    String text = document.text();
                    showNotice(
                            getString(R.string.settings_share),
                            text.isEmpty() ? getString(R.string.settings_share_empty) : text
                    );
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    showApiFailure(getString(R.string.settings_share), exception);
                });
            }
        });
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
                java.util.List<SettingsScreens.LinkLine> managed = new java.util.ArrayList<>();
                for (SettingsContracts.ManagedPatient patient : patients) {
                    if (!SettingsContracts.GUARDIAN.equals(patient.myRole) || patient.linkId == null) {
                        continue;
                    }
                    managed.add(new SettingsScreens.LinkLine(
                            patient.linkId,
                            patient.name,
                            linkDetail(null, patient.permissionScope, patient.relationship),
                            true
                    ));
                }
                java.util.List<SettingsScreens.LinkLine> guardians = new java.util.ArrayList<>();
                boolean patientAccount = AccountRoles.PATIENT.equals(accountType);
                if (patientAccount) {
                    SettingsContracts.ResolvedPatient self = SettingsContracts.resolveSelf(patients);
                    if (self.resolution == SettingsContracts.Resolution.ONE) {
                        for (SettingsContracts.GuardianLink link : SettingsApi.guardianLinks(
                                getApplicationContext(),
                                self.patientId
                        )) {
                            guardians.add(new SettingsScreens.LinkLine(
                                    link.linkId,
                                    link.guardianName,
                                    linkDetail(link.status, link.permissionScope, link.relationship),
                                    SettingsContracts.ACTIVE.equals(link.status)
                            ));
                        }
                    }
                }
                boolean show = patientAccount;
                runOnUiThread(() -> {
                    clearLoadProgress();
                    if (!isAlive() || epoch != settingsEpoch) {
                        return;
                    }
                    managedLines.clear();
                    managedLines.addAll(managed);
                    guardianLines.clear();
                    guardianLines.addAll(guardians);
                    showGuardians = show;
                    careReady = true;
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
    public void registerDevice(String serialNo, String secret) {
        String serial = serialNo == null ? "" : serialNo.trim();
        String secretValue = secret == null ? "" : secret.trim();
        if (serial.isEmpty() || secretValue.isEmpty()) {
            showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_register_need_both));
            return;
        }
        if (!beginAccountRequest(getString(R.string.pillbox_register_loading))) {
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
                        finishAccountRequest();
                        showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_read_only));
                    });
                    return;
                }
                if (CareContracts.shareWithheld(patient, CareContracts.DEVICE_EVENT)) {
                    runOnUiThread(() -> {
                        finishAccountRequest();
                        showNotice(getString(R.string.pillbox_settings), getString(R.string.care_share_withheld));
                    });
                    return;
                }
                CareApi.registerDevice(getApplicationContext(), target.patientId, serial, secretValue, null);
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (!isAlive()) {
                        return;
                    }
                    showNotice(getString(R.string.pillbox_settings), getString(R.string.pillbox_register_done));
                    accountSyncKey = null;
                    showPillboxTab();
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishAccountRequest();
                    if (exception.endsSession()) {
                        showApiFailure(getString(R.string.pillbox_settings), exception);
                        return;
                    }
                    if (ApiCodes.DEVICE_NOT_FOUND.equals(exception.code())) {
                        forgetLocalPillbox();
                    }
                    showNotice(getString(R.string.pillbox_settings), careMessage(exception));
                });
            }
        });
    }

    private static String linkDetail(String status, String scope, String relationship) {
        StringBuilder text = new StringBuilder();
        appendDetail(text, status);
        appendDetail(text, scope);
        appendDetail(text, relationship);
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
        if (prepared.block == HomeRegistration.Block.BEDTIME) {
            showMessage(getString(R.string.med_bedtime_server));
            return;
        }
        if (prepared.block != HomeRegistration.Block.NONE || prepared.prescription == null) {
            showMessage(getString(R.string.med_required));
            return;
        }
        homeWriteBusy = true;
        showProgress(getString(R.string.home_loading));
        ApiClient.execute(() -> {
            try {
                PatientGate gate = gatePatients();
                if (gate.blocked != null) {
                    runOnUiThread(() -> finishHomeWrite(gate.blocked));
                    return;
                }
                if (!CareContracts.writesAllowed(gate.patient)) {
                    runOnUiThread(() -> finishHomeWrite(getString(R.string.med_readonly)));
                    return;
                }
                if (CareContracts.shareWithheld(gate.patient, HomeContracts.MED_INFO)) {
                    runOnUiThread(() -> finishHomeWrite(getString(R.string.care_share_withheld)));
                    return;
                }
                HomeContracts.CreatedPrescription created = HomeApi.createPrescription(
                        getApplicationContext(),
                        gate.patient.patientId,
                        prepared.prescription
                );
                StringBuilder partial = new StringBuilder();
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
                            HomeApi.assignCompartment(
                                    getApplicationContext(),
                                    created.prescriptionId,
                                    prepared.compartmentNo
                            );
                        } catch (ApiException exception) {
                            if (exception.endsSession()) {
                                throw exception;
                            }
                            if (partial.length() > 0) {
                                partial.append('\n');
                            }
                            partial.append(getString(R.string.med_server_partial_slot));
                            partial.append('\n').append(careMessage(exception));
                        }
                    }
                }
                String notice = partial.length() == 0
                        ? getString(R.string.med_server_saved)
                        : partial.toString();
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    if (!isAlive()) {
                        return;
                    }
                    YakyeonRepositoryProvider.get().medications().abandonDraft();
                    popTo(MedScreen.LIST);
                    showMessage(notice);
                    reloadServerSurfaces();
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    showApiFailure(getString(R.string.med_envelope_title), exception);
                });
            }
        });
    }

    @Override
    public void capturePrescription() {
        prescriptionImage.launch("image/*");
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
                    if (round == null || com.example.yakyeon2.data.DoseRound.BEDTIME == round) {
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
                if (CareContracts.shareWithheld(gate.patient, HomeContracts.MED_INFO)) {
                    runOnUiThread(() -> finishHomeWrite(getString(R.string.care_share_withheld)));
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
        if (slots.isEmpty()) {
            showMessage(getString(R.string.med_required));
            return;
        }
        org.json.JSONObject body;
        try {
            body = HomeContracts.scheduleBody(slots, times);
        } catch (ApiException exception) {
            showMessage(getString(R.string.med_bedtime_server));
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
                    reloadServerSurfaces();
                });
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    finishHomeWrite(null);
                    showApiFailure(getString(R.string.tab_home), exception);
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

    private void reloadServerSurfaces() {
        homeArmed = false;
        scheduleLoaded = false;
        historyLoaded = false;
        ensureHomeLoaded();
        if (MedScreen.SCHEDULE.equals(navigator.currentScreen())) {
            ensureScheduleLoaded();
        }
        if (MedScreen.HISTORY.equals(navigator.currentScreen())) {
            ensureHistoryLoaded();
        }
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
            return;
        }
        homeArmed = true;
        homeEpoch++;
        homeView = MainScreens.CareView.LOADING;
        loadHome(homeEpoch);
    }

    private void ensureScheduleLoaded() {
        if (!isSignedIn() || scheduleLoaded) {
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
                            null
                    ));
                    return;
                }
                SettingsContracts.ManagedPatient patient = gate.patient;
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
                        patient.patientId
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
            return HomeContracts.Section.ready(HomeApi.doses(getApplicationContext(), patient.patientId, today));
        } catch (ApiException exception) {
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
            Long patientId
    ) {
        if (!isAlive() || epoch != homeEpoch) {
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
        if (needsServerHome(navigator.currentScreen())) {
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
                java.util.List<HomeContracts.Dose> doses = HomeApi.doses(
                        getApplicationContext(),
                        gate.patient.patientId,
                        date
                );
                runOnUiThread(() -> applySchedule(epoch, MainScreens.CareView.READY, null, doses));
            } catch (ApiException exception) {
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
            return;
        }
        scheduleView = view;
        scheduleMessage = message;
        scheduleDoses = doses;
        if (MedScreen.SCHEDULE.equals(navigator.currentScreen())) {
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
                java.util.List<HomeContracts.Dose> doses = HomeApi.doseHistory(
                        getApplicationContext(),
                        gate.patient.patientId,
                        from,
                        to,
                        status
                );
                runOnUiThread(() -> applyHistory(epoch, MainScreens.CareView.READY, null, doses));
            } catch (ApiException exception) {
                runOnUiThread(() -> {
                    applyHistory(epoch, MainScreens.CareView.FAILED, careMessage(exception), null);
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
            return;
        }
        historyView = view;
        historyMessage = message;
        historyDoses = doses;
        if (MedScreen.HISTORY.equals(navigator.currentScreen())) {
            refresh();
        }
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

    private static int menuFor(ScreenId screen) {
        switch (screen) {
            case HOME:
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
