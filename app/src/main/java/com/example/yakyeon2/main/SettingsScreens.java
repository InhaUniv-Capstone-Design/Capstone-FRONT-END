package com.example.yakyeon2.main;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.net.Uri;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import com.example.yakyeon2.R;
import com.example.yakyeon2.ui.ConfirmDialog;
import com.example.yakyeon2.ui.YakyeonTextField;
import com.example.yakyeon2.data.AccountRoles;
import com.example.yakyeon2.data.ClinicalShareOptions;
import com.example.yakyeon2.data.DrugRelations;
import com.example.yakyeon2.data.LegalMarks;
import com.example.yakyeon2.data.LocalAppSettings;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.NotificationPreferences;
import com.example.yakyeon2.data.ProfileSnapshot;
import com.example.yakyeon2.data.YakyeonSession;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class SettingsScreens {

    public static final String NOTI_DETAIL = "settings-noti-02";
    public static final String TERMS = "settings-terms";
    public static final String PRIVACY = "settings-privacy";
    public static final String DRUG_LEGAL = "settings-drug-legal";
    public static final String DEVICE_LEGAL = "settings-device-legal";
    public static final String CARE_QR = "settings-care-qr";
    public static final String PILLBOX = "pillbox-settings";
    public static final String PILLBOX_SEARCH = "pillbox-search";
    public static final String PILLBOX_CONSENT = "pillbox-consent";
    public static final String PILLBOX_GATE = "pillbox-gate";

    public interface AccountHost {
        boolean isSignedIn();

        ProfileSnapshot currentProfile();

        void submitName(String name);

        void submitPassword(String currentPassword, String newPassword, String newPasswordConfirm);

        void logout();

        void submitAccountDelete(String password);

        void commitNotificationEdit(Runnable apply);

        boolean agreementsReady();

        LegalMarks agreementMarks();

        void submitAgreement(String termsType, boolean withdrawn);

        boolean careReady();

        boolean showGuardianSection();

        java.util.List<LinkLine> managedLinks();

        java.util.List<LinkLine> guardianLinks();

        DevicePanel devicePanel();

        boolean deviceReady();

        void setDeviceFlag(String field, boolean enabled);

        void syncDevice();

        void disconnectDevice();

        boolean deviceWritesAllowed();

        String deviceStatusMessage();

        void confirmDeviceConsent();

        void registerDevice(String serialNo, String secret);

        String deviceSerialDraft();

        String deviceSecretDraft();

        void unlinkCareLink(long linkId);
    }

    public static final class LinkLine {
        public final long linkId;
        public final String title;
        public final String detail;
        public final boolean canUnlink;

        public LinkLine(long linkId, String title, String detail, boolean canUnlink) {
            this.linkId = linkId;
            this.title = title;
            this.detail = detail;
            this.canUnlink = canUnlink;
        }
    }

    public static final class DevicePanel {
        public final String serial;
        public final boolean online;
        public final boolean buzzerKnown;
        public final boolean buzzer;
        public final boolean ledKnown;
        public final boolean led;
        public final String syncedAt;
        public final Boolean scheduleInSync;

        public DevicePanel(
                String serial,
                boolean online,
                boolean buzzerKnown,
                boolean buzzer,
                boolean ledKnown,
                boolean led,
                String syncedAt,
                Boolean scheduleInSync
        ) {
            this.serial = serial;
            this.online = online;
            this.buzzerKnown = buzzerKnown;
            this.buzzer = buzzer;
            this.ledKnown = ledKnown;
            this.led = led;
            this.syncedAt = syncedAt;
            this.scheduleInSync = scheduleInSync;
        }
    }

    private SettingsScreens() {
    }

    public static boolean isSettings(String key) {
        return MainScreens.PROFILE.equals(key)
                || MainScreens.NOTIFICATIONS.equals(key)
                || NOTI_DETAIL.equals(key)
                || MainScreens.SHARE.equals(key)
                || MainScreens.LEGAL.equals(key)
                || TERMS.equals(key)
                || PRIVACY.equals(key)
                || DRUG_LEGAL.equals(key)
                || DEVICE_LEGAL.equals(key)
                || CARE_QR.equals(key)
                || PILLBOX.equals(key)
                || PILLBOX_SEARCH.equals(key)
                || PILLBOX_CONSENT.equals(key)
                || PILLBOX_GATE.equals(key);
    }

    public static String titleFor(AppCompatActivity activity, String key) {
        if (MainScreens.PROFILE.equals(key)) {
            return activity.getString(R.string.settings_profile_heading);
        }
        if (NOTI_DETAIL.equals(key)) {
            return activity.getString(R.string.settings_noti_detail_title);
        }
        if (MainScreens.NOTIFICATIONS.equals(key)) {
            return activity.getString(R.string.settings_noti);
        }
        if (MainScreens.SHARE.equals(key)) {
            return activity.getString(R.string.settings_share);
        }
        if (TERMS.equals(key)) {
            return activity.getString(R.string.settings_terms_open);
        }
        if (PRIVACY.equals(key)) {
            return activity.getString(R.string.settings_privacy_title);
        }
        if (DRUG_LEGAL.equals(key)) {
            return activity.getString(R.string.settings_drug_open);
        }
        if (DEVICE_LEGAL.equals(key)) {
            return activity.getString(R.string.settings_device_title);
        }
        if (CARE_QR.equals(key)) {
            return activity.getString(R.string.care_qr_title);
        }
        if (MainScreens.LEGAL.equals(key)) {
            return activity.getString(R.string.settings_legal);
        }
        if (PILLBOX.equals(key)) {
            return activity.getString(R.string.pillbox_settings);
        }
        if (PILLBOX_SEARCH.equals(key)) {
            return activity.getString(R.string.pillbox_register_title);
        }
        if (PILLBOX_CONSENT.equals(key)) {
            return activity.getString(R.string.settings_device_title);
        }
        if (PILLBOX_GATE.equals(key)) {
            return activity.getString(R.string.pillbox_screen_title);
        }
        return null;
    }

    public static View show(
            AppCompatActivity activity,
            String key,
            YakyeonSession session,
            MedicationStore store,
            MainScreens.Host host,
            AccountHost account
    ) {
        if (MainScreens.PROFILE.equals(key)) {
            return profile(activity, host, account);
        }
        if (MainScreens.NOTIFICATIONS.equals(key)) {
            return notifications(activity, session, host, account);
        }
        if (NOTI_DETAIL.equals(key)) {
            return notificationDetail(activity, session, host, account);
        }
        if (MainScreens.SHARE.equals(key)) {
            return share(activity, store, host);
        }
        if (MainScreens.LEGAL.equals(key)) {
            return legal(activity, host, account);
        }
        if (TERMS.equals(key)) {
            return reading(activity, R.raw.legal_terms);
        }
        if (PRIVACY.equals(key)) {
            return reading(activity, R.raw.legal_privacy);
        }
        if (DRUG_LEGAL.equals(key)) {
            return reading(activity, R.raw.legal_drug);
        }
        if (DEVICE_LEGAL.equals(key)) {
            return reading(activity, R.raw.legal_device);
        }
        if (CARE_QR.equals(key)) {
            return careQr(activity, host);
        }
        if (PILLBOX.equals(key)) {
            return pillbox(activity, store, host, account);
        }
        if (PILLBOX_SEARCH.equals(key)) {
            return pillboxSearch(activity, account);
        }
        if (PILLBOX_CONSENT.equals(key)) {
            return pillboxConsent(activity, store, host, account);
        }
        return pillboxGate(activity, host);
    }

    private static View profile(AppCompatActivity activity, MainScreens.Host host, AccountHost account) {
        boolean signedIn = account.isSignedIn();
        ProfileSnapshot profile = account.currentProfile();
        String empty = activity.getString(R.string.settings_profile_value_empty);
        String loading = activity.getString(R.string.settings_profile_loading);
        String name = !signedIn ? empty : profile == null ? loading : profile.name;
        String birth = !signedIn ? empty : profile == null ? loading : profile.birthDate;
        String phone = !signedIn ? empty : profile == null ? loading : profile.phoneMasked;
        LinearLayout page = canvasPage(activity);
        page.addView(subhead(activity, R.string.settings_profile_sub));
        LinearLayout nameCard = pillCard(activity, 0);
        nameCard.addView(nameRow(activity, name, signedIn
                ? () -> promptName(activity, profile == null ? "" : profile.name, account)
                : null));
        page.addView(nameCard);
        LinearLayout info = pillCard(activity, 12);
        info.addView(pillRow(
                activity,
                R.drawable.ic_yakyeon_calendar,
                activity.getString(R.string.settings_profile_birth_label),
                birth,
                false,
                null
        ));
        info.addView(hairline(activity));
        info.addView(pillRow(
                activity,
                R.drawable.ic_settings_phone,
                activity.getString(R.string.settings_profile_phone_label),
                phone,
                false,
                null
        ));
        page.addView(info);
        LinearLayout actions = pillCard(activity, 12);
        actions.addView(actionRow(activity, R.drawable.ic_settings_key, R.string.settings_password, () -> {
            if (!signedIn) {
                host.showNotice(
                        activity.getString(R.string.settings_password),
                        activity.getString(R.string.settings_account_blocked)
                );
                return;
            }
            promptPassword(activity, account);
        }));
        actions.addView(hairline(activity));
        actions.addView(actionRow(activity, R.drawable.ic_settings_logout, R.string.settings_logout, () -> {
            if (!signedIn) {
                host.showNotice(
                        activity.getString(R.string.settings_logout),
                        activity.getString(R.string.settings_logout_blocked)
                );
                return;
            }
            confirmLogout(activity, account);
        }));
        actions.addView(hairline(activity));
        actions.addView(actionRow(activity, R.drawable.ic_settings_leave, R.string.settings_withdraw_account, () -> {
            if (!signedIn) {
                host.showNotice(
                        activity.getString(R.string.settings_withdraw_account),
                        activity.getString(R.string.settings_withdraw_blocked)
                );
                return;
            }
            promptAccountDelete(activity, account);
        }));
        page.addView(actions);
        return canvasScroll(page);
    }

    private static View notifications(
            AppCompatActivity activity,
            YakyeonSession session,
            MainScreens.Host host,
            AccountHost account
    ) {
        NotificationPreferences preferences = session.notifications();
        LinearLayout page = canvasPage(activity);
        page.addView(subhead(activity, R.string.settings_noti_sub));
        LinearLayout doses = pillCard(activity, 0);
        doses.addView(toggleLine(activity, R.drawable.ic_settings_clock, R.string.settings_noti_dose,
                R.string.settings_noti_dose_help, preferences.isDoseTime(), checked ->
                    account.commitNotificationEdit(() -> preferences.setDoseTime(checked))));
        doses.addView(hairline(activity));
        doses.addView(toggleLine(activity, R.drawable.ic_settings_alert, R.string.settings_noti_missed,
                R.string.settings_noti_missed_help, preferences.isMissedDose(), checked ->
                    account.commitNotificationEdit(() -> preferences.setMissedDose(checked))));
        doses.addView(hairline(activity));
        doses.addView(toggleLine(activity, R.drawable.ic_yakyeon_calendar, R.string.settings_noti_ending,
                R.string.settings_noti_ending_help, preferences.isEndingSoon(), checked ->
                    account.commitNotificationEdit(() -> preferences.setEndingSoon(checked))));
        page.addView(doses);
        LinearLayout pillboxCard = pillCard(activity, 12);
        LinearLayout pillboxTrailing = new LinearLayout(activity);
        pillboxTrailing.setOrientation(LinearLayout.HORIZONTAL);
        pillboxTrailing.setGravity(Gravity.CENTER_VERTICAL);
        pillboxTrailing.addView(pillSwitch(activity, preferences.isPillboxStatus(), checked ->
                account.commitNotificationEdit(() -> {
                    preferences.setPillboxStatus(checked);
                    host.refresh();
                })));
        ImageView pillboxMore = chevron(activity);
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(activity, 16), dp(activity, 16));
        moreParams.setMarginStart(dp(activity, 8));
        pillboxTrailing.addView(pillboxMore, moreParams);
        LinearLayout pillbox = pillRow(
                activity,
                R.drawable.ic_settings_bottle,
                activity.getString(R.string.settings_noti_pillbox),
                activity.getString(R.string.settings_noti_pillbox_help),
                false,
                pillboxTrailing
        );
        tap(activity, pillbox, () -> host.openDetail(NOTI_DETAIL));
        pillboxCard.addView(pillbox);
        page.addView(pillboxCard);
        page.addView(footnote(activity, permissionNote(activity)));
        return canvasScroll(page);
    }

    private static View notificationDetail(
            AppCompatActivity activity,
            YakyeonSession session,
            MainScreens.Host host,
            AccountHost account
    ) {
        NotificationPreferences preferences = session.notifications();
        boolean enabled = preferences.isPillboxStatus();
        LinearLayout page = canvasPage(activity);
        page.addView(subhead(activity, R.string.settings_noti_sub));
        if (!enabled) {
            page.addView(subhead(activity, R.string.settings_noti_detail_locked));
        }
        LinearLayout card = pillCard(activity, 0);
        addDetailToggle(card, activity, enabled, R.string.settings_noti_button,
                R.string.settings_noti_button_help, preferences.isDoseButton(), preferences::setDoseButton, account, true);
        addDetailToggle(card, activity, enabled, R.string.settings_noti_hour,
                R.string.settings_noti_hour_help, preferences.isMissedByDevice(), preferences::setMissedByDevice, account, false);
        addDetailToggle(card, activity, enabled, R.string.settings_noti_fault,
                R.string.settings_noti_fault_help, preferences.isDeviceFault(), preferences::setDeviceFault, account, false);
        page.addView(card);
        page.addView(footnote(activity, permissionNote(activity)));
        return canvasScroll(page);
    }

    private static View share(AppCompatActivity activity, MedicationStore store, MainScreens.Host host) {
        ClinicalShareOptions options = LocalAppSettings.loadShare(activity);
        LinearLayout page = canvasPage(activity);
        page.addView(subhead(activity, R.string.settings_share_note));
        LinearLayout card = pillCard(activity, 0);
        card.addView(shareLine(
                activity,
                R.drawable.ic_yakyeon_calendar,
                activity.getString(R.string.settings_share_period_label),
                sharePeriodText(activity, options),
                activity.getString(R.string.settings_share_change),
                0,
                () -> choosePeriod(activity, options, host)
        ));
        card.addView(hairline(activity));
        card.addView(shareChoiceRow(
                activity,
                R.drawable.ic_settings_doc,
                activity.getString(R.string.settings_share_dur),
                activity.getString(R.string.settings_share_dur_sub),
                options.isIncludeDur(),
                included -> {
                    options.setIncludeDur(included);
                    LocalAppSettings.saveShare(activity, options);
                    host.refresh();
                }
        ));
        card.addView(hairline(activity));
        card.addView(shareChoiceRow(
                activity,
                R.drawable.ic_settings_capsule,
                activity.getString(R.string.settings_share_current),
                activity.getString(R.string.settings_share_medicine_sub),
                options.isIncludeCurrent(),
                included -> {
                    options.setIncludeCurrent(included);
                    LocalAppSettings.saveShare(activity, options);
                    host.refresh();
                }
        ));
        card.addView(hairline(activity));
        card.addView(shareChoiceRow(
                activity,
                R.drawable.ic_settings_percent,
                activity.getString(R.string.settings_share_finished),
                activity.getString(R.string.settings_share_medicine_sub),
                options.isIncludeFinished(),
                included -> {
                    options.setIncludeFinished(included);
                    LocalAppSettings.saveShare(activity, options);
                    host.refresh();
                }
        ));
        page.addView(card);
        TextView export = new TextView(activity);
        export.setText(R.string.settings_share_preview);
        export.setTextColor(activity.getColor(R.color.white));
        export.setTextSize(16);
        export.setTypeface(Typeface.DEFAULT_BOLD);
        export.setGravity(Gravity.CENTER);
        export.setBackgroundResource(R.drawable.bg_settings_primary);
        LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(activity, 52)
        );
        exportParams.topMargin = dp(activity, 16);
        export.setLayoutParams(exportParams);
        export.setOnClickListener(v -> host.exportClinicalShare());
        page.addView(export);
        return canvasScroll(page);
    }

    private static View legal(AppCompatActivity activity, MainScreens.Host host, AccountHost account) {
        LegalMarks marks = account.agreementsReady() ? account.agreementMarks() : new LegalMarks();
        LinearLayout page = page(activity);
        float density = activity.getResources().getDisplayMetrics().density;
        int side = Math.round(18f * density);
        page.setPadding(side, Math.round(4f * density), side, Math.round(28f * density));
        TextView intro = new TextView(activity);
        intro.setText(R.string.settings_legal_note);
        intro.setTextColor(activity.getColor(R.color.yakyeon_muted));
        intro.setTextSize(13);
        intro.setLineSpacing(0f, 1.65f);
        page.addView(intro);
        page.addView(legalHeading(activity, R.string.settings_legal_section_docs, Math.round(20f * density)));
        LinearLayout docs = legalCard(activity);
        LinearLayout.LayoutParams docsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        docsParams.topMargin = Math.round(12f * density);
        docs.setLayoutParams(docsParams);
        docs.addView(docRow(activity, R.drawable.ic_legal_info, R.string.settings_terms_open,
                R.string.settings_terms_sub, 11, false, () -> host.openDetail(TERMS)));
        docs.addView(docRow(activity, R.drawable.ic_legal_info, R.string.settings_privacy_open,
                R.string.settings_privacy_sub, 11, true, () -> host.openDetail(PRIVACY)));
        docs.addView(docRow(activity, R.drawable.ic_legal_info, R.string.settings_drug_open,
                R.string.settings_drug_sub, 10, true, () -> host.openDetail(DRUG_LEGAL)));
        docs.addView(docRow(activity, R.drawable.ic_legal_info, R.string.settings_device_open,
                R.string.settings_device_sub, 10, true, () -> host.openDetail(DEVICE_LEGAL)));
        page.addView(docs);
        page.addView(legalHeading(activity, R.string.settings_legal_section_withdraw, Math.round(24f * density)));
        page.addView(withdrawCard(activity, R.string.settings_withdraw_card_terms, new int[] {
                R.string.settings_withdraw_terms
        }, new boolean[] {marks.isTerms()}, new Runnable[] {
                () -> account.submitAgreement("TOS", marks.isTerms())
        }, host));
        page.addView(withdrawCard(activity, R.string.settings_privacy_open, new int[] {
                R.string.settings_withdraw_privacy,
                R.string.settings_withdraw_sensitive
        }, new boolean[] {marks.isPrivacy(), marks.isSensitive()}, new Runnable[] {
                () -> account.submitAgreement("PRIVACY", marks.isPrivacy()),
                () -> account.submitAgreement("SENSITIVE_HEALTH", marks.isSensitive())
        }, host));
        page.addView(withdrawCard(activity, R.string.settings_drug_open, new int[] {
                R.string.settings_withdraw_drug,
                R.string.settings_withdraw_notice
        }, new boolean[] {marks.isDrugCollection(), marks.isDrugCollection()}, new Runnable[] {
                () -> account.submitAgreement("MEDICATION", marks.isDrugCollection()),
                () -> account.submitAgreement("MEDICATION", marks.isDrugCollection())
        }, host));
        page.addView(withdrawCard(activity, R.string.settings_device_open, new int[] {
                R.string.settings_withdraw_device
        }, new boolean[] {marks.isDevice()}, new Runnable[] {
                () -> account.submitAgreement("DEVICE", marks.isDevice())
        }, host));
        page.addView(legalHeading(activity, R.string.settings_legal_section_keep, Math.round(24f * density)));
        LinearLayout keep = legalCard(activity);
        LinearLayout.LayoutParams keepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        keepParams.topMargin = Math.round(12f * density);
        keep.setLayoutParams(keepParams);
        TextView keepBody = new TextView(activity);
        keepBody.setText(R.string.settings_legal_keep_body);
        keepBody.setTextColor(activity.getColor(R.color.yakyeon_text));
        keepBody.setTextSize(12);
        keepBody.setLineSpacing(0f, 1.5f);
        keep.addView(keepBody);
        page.addView(keep);
        return scroll(page);
    }

    private static View reading(AppCompatActivity activity, int rawId) {
        LinearLayout page = page(activity);
        TextView text = new TextView(activity);
        text.setText(readRaw(activity, rawId));
        text.setTextColor(activity.getColor(R.color.yakyeon_text));
        text.setTextSize(13);
        text.setLineSpacing(0, 1.45f);
        int bottom = activity.getResources().getDimensionPixelSize(R.dimen.space_m) * 3;
        text.setPadding(text.getPaddingLeft(), text.getPaddingTop(), text.getPaddingRight(), bottom);
        page.addView(text);
        return scroll(page);
    }

    private static View careQr(AppCompatActivity activity, MainScreens.Host host) {
        LinearLayout page = canvasPage(activity);
        fillCareRegister(activity, page, host);
        return canvasScroll(page);
    }

    private static void fillCareRegister(
            AppCompatActivity activity,
            LinearLayout page,
            MainScreens.Host host
    ) {
        ImageView code = new ImageView(activity);
        code.setImageResource(R.drawable.care_qr_code);
        code.setContentDescription(activity.getString(R.string.care_qr_title));
        code.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int size = dp(activity, 220);
        LinearLayout.LayoutParams codeParams = new LinearLayout.LayoutParams(size, size);
        codeParams.gravity = Gravity.CENTER_HORIZONTAL;
        codeParams.topMargin = dp(activity, 8);
        codeParams.bottomMargin = dp(activity, 16);
        page.addView(code, codeParams);
        page.addView(outlineButton(activity, R.string.care_qr_open, () -> openCareSite(activity, host)));
    }

    private static void openCareSite(AppCompatActivity activity, MainScreens.Host host) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.care_web_url)));
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            host.showMessage(activity.getString(R.string.care_web_unavailable));
        }
    }

    private static View pillbox(
            AppCompatActivity activity,
            MedicationStore store,
            MainScreens.Host host,
            AccountHost account
    ) {
        if (account.isSignedIn()) {
            return signedInPillbox(activity, host, account);
        }
        if (!store.pillboxConnected()) {
            LinearLayout page = page(activity);
            page.addView(heading(activity, R.string.pillbox_disconnected));
            page.addView(note(activity, R.string.pillbox_settings_disconnected_note));
            page.addView(note(activity, R.string.pillbox_wifi));
            page.addView(button(activity, R.string.pillbox_search, () -> host.openDetail(PILLBOX_SEARCH)));
            return scroll(page);
        }
        DevicePanel panel = account.devicePanel();
        if (panel == null) {
            LinearLayout waiting = page(activity);
            waiting.addView(heading(activity, R.string.pillbox_settings));
            int waitingText = R.string.pillbox_loading;
            if (!account.isSignedIn()) {
                waitingText = R.string.settings_login_required;
            } else if (account.deviceReady()) {
                waitingText = R.string.pillbox_unconfirmed;
            }
            waiting.addView(note(activity, waitingText));
            return scroll(waiting);
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        scroll.setBackgroundColor(activity.getColor(R.color.white));
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipToPadding(false);
        page.setClipChildren(false);
        int side = dp(activity, 18);
        page.setPadding(side, dp(activity, 8), side, dp(activity, 28));
        page.addView(deviceCard(activity, panel));
        page.addView(alarmCard(activity, panel, account));
        page.addView(disconnectCard(activity, account));
        scroll.addView(page, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scroll;
    }

    private static LinearLayout deviceCard(AppCompatActivity activity, DevicePanel panel) {
        LinearLayout card = pillCard(activity, 4);
        card.addView(pillRow(
                activity,
                R.drawable.ic_pillbox_device,
                panel.serial,
                null,
                true,
                null
        ));
        View line = new View(activity);
        line.setBackgroundColor(activity.getColor(R.color.yakyeon_line));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density))
        );
        lineParams.topMargin = dp(activity, 6);
        lineParams.bottomMargin = dp(activity, 6);
        card.addView(line, lineParams);
        card.addView(pillRow(
                activity,
                R.drawable.ic_pillbox_wifi,
                activity.getString(R.string.pillbox_connection_label),
                null,
                false,
                panel.online
                        ? connectedMark(activity)
                        : offlineMark(activity)
        ));
        return card;
    }

    private static TextView offlineMark(AppCompatActivity activity) {
        TextView mark = new TextView(activity);
        mark.setText(R.string.pillbox_offline);
        mark.setTextColor(activity.getColor(R.color.yakyeon_muted));
        mark.setTextSize(12);
        return mark;
    }

    private static LinearLayout alarmCard(
            AppCompatActivity activity,
            DevicePanel panel,
            AccountHost account
    ) {
        LinearLayout card = pillCard(activity, 12);
        card.addView(pillRow(
                activity,
                R.drawable.ic_settings_bell,
                activity.getString(R.string.pillbox_buzzer_alarm),
                activity.getString(R.string.pillbox_buzzer_help),
                false,
                pillSwitch(activity, panel.buzzerKnown && panel.buzzer, checked ->
                        account.setDeviceFlag("buzzer_enabled", checked))
        ));
        card.addView(pillRow(
                activity,
                R.drawable.ic_settings_bell,
                activity.getString(R.string.pillbox_led_alarm),
                activity.getString(R.string.pillbox_led_help),
                false,
                pillSwitch(activity, panel.ledKnown && panel.led, checked ->
                        account.setDeviceFlag("led_enabled", checked))
        ));
        String synced;
        if (Boolean.FALSE.equals(panel.scheduleInSync)) {
            synced = activity.getString(R.string.pillbox_sync_out_of_sync);
        } else if (Boolean.TRUE.equals(panel.scheduleInSync)) {
            synced = activity.getString(R.string.pillbox_sync_in_sync);
        } else if (panel.syncedAt == null || panel.syncedAt.isEmpty()) {
            synced = activity.getString(R.string.pillbox_sync_none);
        } else {
            synced = panel.syncedAt;
        }
        LinearLayout sync = pillRow(
                activity,
                R.drawable.ic_pillbox_sync,
                activity.getString(R.string.pillbox_sync),
                synced,
                false,
                null
        );
        sync.setOnClickListener(v -> account.syncDevice());
        card.addView(sync);
        return card;
    }

    private static LinearLayout disconnectCard(AppCompatActivity activity, AccountHost account) {
        LinearLayout card = pillCard(activity, 12);
        card.addView(pillRow(
                activity,
                R.drawable.ic_pillbox_unlink,
                activity.getString(R.string.pillbox_disconnect),
                activity.getString(R.string.pillbox_disconnect_help),
                false,
                chevron(activity)
        ));
        TypedValue ripple = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        card.setForeground(activity.getDrawable(ripple.resourceId));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> confirmDisconnect(activity, account));
        return card;
    }

    private static LinearLayout pillCard(AppCompatActivity activity, int topDp) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_pillbox_settings_card);
        card.setClipToOutline(true);
        int pad = dp(activity, 16);
        card.setPadding(pad, dp(activity, 8), pad, dp(activity, 8));
        card.setElevation(dp(activity, 3));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int shadow = 0x2617332C;
            card.setOutlineAmbientShadowColor(shadow);
            card.setOutlineSpotShadowColor(shadow);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(activity, topDp);
        card.setLayoutParams(params);
        return card;
    }

    private static LinearLayout pillRow(
            AppCompatActivity activity,
            int iconRes,
            CharSequence title,
            CharSequence subtitle,
            boolean deviceName,
            View trailing
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(activity, subtitle == null ? 6 : 8);
        row.setPadding(0, pad, 0, pad);
        row.addView(pillIcon(activity, iconRes));
        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textParams.setMarginStart(dp(activity, 12));
        textParams.setMarginEnd(dp(activity, 8));
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextColor(activity.getColor(R.color.yakyeon_text));
        label.setIncludeFontPadding(false);
        if (deviceName) {
            label.setTextSize(16);
            label.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            label.setTextSize(15);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        text.addView(label);
        if (subtitle != null) {
            TextView detail = new TextView(activity);
            detail.setText(subtitle);
            detail.setTextColor(activity.getColor(R.color.yakyeon_muted));
            detail.setTextSize(12);
            detail.setIncludeFontPadding(false);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            detailParams.topMargin = dp(activity, 4);
            text.addView(detail, detailParams);
        }
        row.addView(text, textParams);
        if (trailing != null) {
            row.addView(trailing);
        }
        return row;
    }

    private static ImageView pillIcon(AppCompatActivity activity, int iconRes) {
        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(activity.getColor(R.color.yakyeon_primary)));
        icon.setImageTintMode(PorterDuff.Mode.SRC_IN);
        icon.setBackgroundResource(R.drawable.bg_settings_icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int tile = dp(activity, 40);
        int inset = dp(activity, 10);
        icon.setPadding(inset, inset, inset, inset);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.setLayoutParams(new LinearLayout.LayoutParams(tile, tile));
        return icon;
    }

    private static TextView connectedMark(AppCompatActivity activity) {
        TextView mark = new TextView(activity);
        mark.setText(R.string.pillbox_connected);
        mark.setTextColor(activity.getColor(R.color.yakyeon_badge));
        mark.setTextSize(12);
        mark.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mark.setIncludeFontPadding(false);
        mark.setGravity(Gravity.CENTER);
        mark.setBackgroundResource(R.drawable.bg_status_pill);
        int horizontal = dp(activity, 10);
        int vertical = dp(activity, 5);
        mark.setPadding(horizontal, vertical, horizontal, vertical);
        return mark;
    }

    private static ImageView chevron(AppCompatActivity activity) {
        ImageView chevron = new ImageView(activity);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        int size = dp(activity, 16);
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        chevron.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return chevron;
    }

    private static FrameLayout pillSwitch(
            AppCompatActivity activity,
            boolean checked,
            java.util.function.Consumer<Boolean> onChange
    ) {
        int hit = dp(activity, 48);
        int trackWidth = dp(activity, 46);
        int trackHeight = dp(activity, 28);
        int knobSize = dp(activity, 22);
        int inset = dp(activity, 3);
        FrameLayout box = new FrameLayout(activity);
        box.setLayoutParams(new LinearLayout.LayoutParams(hit, hit));
        box.setClipChildren(false);
        View track = new View(activity);
        track.setBackgroundResource(checked ? R.drawable.bg_switch_track_on : R.drawable.bg_switch_track_off);
        FrameLayout.LayoutParams trackParams = new FrameLayout.LayoutParams(trackWidth, trackHeight);
        trackParams.gravity = Gravity.CENTER;
        box.addView(track, trackParams);
        View knob = new View(activity);
        knob.setBackgroundResource(R.drawable.bg_switch_knob);
        knob.setElevation(dp(activity, 1));
        FrameLayout.LayoutParams knobParams = new FrameLayout.LayoutParams(knobSize, knobSize);
        knobParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        knobParams.leftMargin = (hit - trackWidth) / 2 + inset;
        box.addView(knob, knobParams);
        float travel = trackWidth - knobSize - inset * 2f;
        knob.setTranslationX(checked ? travel : 0f);
        final boolean[] on = {checked};
        box.setOnClickListener(v -> {
            on[0] = !on[0];
            track.setBackgroundResource(on[0] ? R.drawable.bg_switch_track_on : R.drawable.bg_switch_track_off);
            knob.animate().translationX(on[0] ? travel : 0f).setDuration(160).start();
            onChange.accept(on[0]);
        });
        box.setClickable(true);
        box.setFocusable(true);
        return box;
    }

    private static void prepareRegisterField(YakyeonTextField field, String hint, boolean secret) {
        field.setHintEnabled(false);
        field.getEditText().setHint(hint);
        field.getEditText().setSingleLine(true);
        field.getEditText().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        if (!secret) {
            field.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
            return;
        }
        field.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        field.setEndIconContentDescription(R.string.auth_password_visibility);
        field.setEndIconTintList(ColorStateList.valueOf(field.getContext().getColor(R.color.yakyeon_muted)));
    }

    private static boolean registerReady(YakyeonTextField serial, YakyeonTextField secret) {
        return !serial.getText().trim().isEmpty() && !secret.getText().trim().isEmpty();
    }

    private static void keepRegisterVisible(View field, View button) {
        field.setOnFocusChangeListener((focused, hasFocus) -> {
            if (!hasFocus) {
                return;
            }
            focused.post(() -> {
                Rect fieldRect = new Rect(0, 0, focused.getWidth(), focused.getHeight());
                fieldRect.bottom += button.getHeight();
                focused.requestRectangleOnScreen(fieldRect, true);
                button.requestRectangleOnScreen(
                        new Rect(0, 0, button.getWidth(), button.getHeight()),
                        false
                );
            });
        });
    }

    private static int dp(AppCompatActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static View signedInPillbox(
            AppCompatActivity activity,
            MainScreens.Host host,
            AccountHost account
    ) {
        if (!account.deviceReady()) {
            LinearLayout waiting = page(activity);
            waiting.addView(heading(activity, R.string.pillbox_settings));
            waiting.addView(note(activity, R.string.pillbox_loading));
            return scroll(waiting);
        }
        DevicePanel panel = account.devicePanel();
        if (panel == null) {
            LinearLayout page = page(activity);
            String status = account.deviceStatusMessage();
            if (status != null && !status.isEmpty()) {
                page.addView(heading(activity, R.string.pillbox_settings));
                page.addView(note(activity, status));
                return scroll(page);
            }
            page.addView(heading(activity, R.string.pillbox_disconnected));
            page.addView(note(activity, R.string.pillbox_settings_disconnected_note));
            page.addView(note(activity, R.string.pillbox_wifi));
            page.addView(button(activity, R.string.pillbox_search, () -> host.openDetail(PILLBOX_SEARCH)));
            return scroll(page);
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        scroll.setBackgroundColor(activity.getColor(R.color.white));
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipToPadding(false);
        page.setClipChildren(false);
        int side = dp(activity, 18);
        page.setPadding(side, dp(activity, 8), side, dp(activity, 28));
        page.addView(deviceCard(activity, panel));
        if (account.deviceWritesAllowed()) {
            page.addView(alarmCard(activity, panel, account));
            page.addView(disconnectCard(activity, account));
        } else {
            page.addView(note(activity, R.string.pillbox_read_only));
        }
        scroll.addView(page, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return scroll;
    }

    private static View pillboxSearch(
            AppCompatActivity activity,
            AccountHost account
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_pillbox_search, null);
        YakyeonTextField serial = root.findViewById(R.id.field_pillbox_serial);
        YakyeonTextField secret = root.findViewById(R.id.field_pillbox_secret);
        MaterialButton register = root.findViewById(R.id.button_pillbox_register_submit);
        prepareRegisterField(serial, activity.getString(R.string.pillbox_serial_hint), false);
        prepareRegisterField(secret, activity.getString(R.string.pillbox_secret_hint), true);
        serial.setText(account.deviceSerialDraft());
        secret.setText(account.deviceSecretDraft());
        serial.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        secret.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);
        serial.getEditText().setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                secret.getEditText().requestFocus();
                return true;
            }
            return false;
        });
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (Boolean.TRUE.equals(register.getTag())) {
                    return;
                }
                register.setEnabled(registerReady(serial, secret));
            }
        };
        serial.getEditText().addTextChangedListener(watcher);
        secret.getEditText().addTextChangedListener(watcher);
        register.setEnabled(registerReady(serial, secret));
        keepRegisterVisible(serial.getEditText(), register);
        keepRegisterVisible(secret.getEditText(), register);
        register.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(register.getTag()) || !registerReady(serial, secret)) {
                return;
            }
            register.setTag(Boolean.TRUE);
            register.setEnabled(false);
            account.registerDevice(serial.getText(), secret.getText());
        });
        return root;
    }

    private static View pillboxConsent(
            AppCompatActivity activity,
            MedicationStore store,
            MainScreens.Host host,
            AccountHost account
    ) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_pillbox_consent, null);
        TextView body = root.findViewById(R.id.text_pillbox_consent_body);
        body.setText(deviceConsentCopy(activity));
        CheckBox consent = root.findViewById(R.id.check_pillbox_consent);
        root.findViewById(R.id.button_pillbox_consent_cancel).setOnClickListener(v -> host.back());
        root.findViewById(R.id.button_pillbox_consent_agree).setOnClickListener(v -> {
            if (!consent.isChecked()) {
                host.showMessage(activity.getString(R.string.pillbox_consent_required));
                return;
            }
            if (account.isSignedIn()) {
                account.confirmDeviceConsent();
                return;
            }
            store.connectPillbox();
            host.showPillboxTab();
        });
        return root;
    }

    private static String deviceConsentCopy(AppCompatActivity activity) {
        String raw = readRaw(activity, R.raw.legal_device);
        int end = raw.indexOf("\n문서 끝");
        if (end >= 0) {
            raw = raw.substring(0, end).trim();
        }
        return raw.replace(
                "기기 연결을 위해 블루투스 또는 근처 기기 권한이 필요해요.",
                activity.getString(R.string.pillbox_consent_wifi)
        );
    }

    private static View pillboxGate(AppCompatActivity activity, MainScreens.Host host) {
        View root = LayoutInflater.from(activity).inflate(R.layout.screen_pillbox_gate, null);
        root.findViewById(R.id.button_pillbox_register).setOnClickListener(v ->
                host.openDetail(PILLBOX_SEARCH)
        );
        return root;
    }

    private static void confirmDisconnect(AppCompatActivity activity, AccountHost account) {
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.pillbox_disconnect),
                activity.getString(R.string.pillbox_disconnect_body),
                activity.getString(R.string.pillbox_disconnect_confirm),
                activity.getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        account.disconnectDevice();
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    private static void choosePeriod(
            AppCompatActivity activity,
            ClinicalShareOptions options,
            MainScreens.Host host
    ) {
        final int[] days = {
                ClinicalShareOptions.DAYS_7,
                ClinicalShareOptions.DAYS_30,
                ClinicalShareOptions.DAYS_90
        };
        LocalDate today = LocalDate.now();
        final LocalDate[] rangeStart = {options.resolveStart(today)};
        final LocalDate[] rangeEnd = {options.resolveEnd(today)};
        final int[] picked = {presetIndex(rangeStart[0], rangeEnd[0], today)};
        final boolean[] editingEnd = {false};
        final YearMonth[] visible = {YearMonth.from(rangeEnd[0])};
        final Runnable[] render = new Runnable[1];

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_dialog_card);
        int pad = dp(activity, 20);
        card.setPadding(pad, dp(activity, 22), pad, pad);

        TextView title = new TextView(activity);
        title.setText(R.string.settings_share_period_title);
        title.setTextColor(activity.getColor(R.color.yakyeon_text));
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView help = new TextView(activity);
        help.setText(R.string.settings_share_period_help);
        help.setTextColor(activity.getColor(R.color.yakyeon_muted));
        help.setTextSize(13);
        help.setLineSpacing(0f, 1.4f);
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        helpParams.topMargin = dp(activity, 8);
        card.addView(help, helpParams);

        LinearLayout chips = new LinearLayout(activity);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams chipRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        chipRowParams.topMargin = dp(activity, 16);
        card.addView(chips, chipRowParams);
        TextView[] chipViews = new TextView[days.length];
        for (int index = 0; index < days.length; index++) {
            TextView chip = new TextView(activity);
            chip.setText(activity.getString(R.string.settings_share_days, days[index]));
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(13);
            chip.setIncludeFontPadding(false);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    0,
                    dp(activity, 36),
                    1f
            );
            if (index < days.length - 1) {
                chipParams.setMarginEnd(dp(activity, 8));
            }
            chip.setLayoutParams(chipParams);
            final int chipIndex = index;
            chip.setOnClickListener(v -> {
                LocalDate chipEnd = LocalDate.now();
                rangeEnd[0] = chipEnd;
                rangeStart[0] = chipEnd.minusDays(days[chipIndex] - 1L);
                picked[0] = chipIndex;
                editingEnd[0] = true;
                visible[0] = YearMonth.from(chipEnd);
                render[0].run();
            });
            chips.addView(chip);
            chipViews[index] = chip;
        }

        LinearLayout dates = new LinearLayout(activity);
        dates.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams datesParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        datesParams.topMargin = dp(activity, 16);
        card.addView(dates, datesParams);
        TextView startValue = dateColumn(activity, dates, R.string.settings_share_start, true);
        TextView endValue = dateColumn(activity, dates, R.string.settings_share_end, false);

        LinearLayout monthRow = new LinearLayout(activity);
        monthRow.setOrientation(LinearLayout.HORIZONTAL);
        monthRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        monthParams.topMargin = dp(activity, 14);
        card.addView(monthRow, monthParams);
        ImageView previous = navChevron(activity, true);
        previous.setContentDescription("이전 달");
        ImageView next = navChevron(activity, false);
        next.setContentDescription("다음 달");
        TextView monthLabel = new TextView(activity);
        monthLabel.setGravity(Gravity.CENTER);
        monthLabel.setTextColor(activity.getColor(R.color.yakyeon_text));
        monthLabel.setTextSize(15);
        monthLabel.setTypeface(Typeface.DEFAULT_BOLD);
        monthRow.addView(previous);
        monthRow.addView(monthLabel, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        monthRow.addView(next);
        previous.setOnClickListener(v -> {
            visible[0] = visible[0].minusMonths(1);
            render[0].run();
        });
        next.setOnClickListener(v -> {
            visible[0] = visible[0].plusMonths(1);
            render[0].run();
        });

        LinearLayout calendar = new LinearLayout(activity);
        calendar.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams calendarParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        calendarParams.topMargin = dp(activity, 6);
        card.addView(calendar, calendarParams);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dp(activity, 12);
        card.addView(buttons, buttonParams);
        TextView cancel = new TextView(activity);
        cancel.setText(R.string.action_cancel);
        cancel.setTextColor(activity.getColor(R.color.yakyeon_muted));
        cancel.setTextSize(15);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(dp(activity, 8), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        TextView apply = new TextView(activity);
        apply.setText(R.string.settings_share_period_apply);
        apply.setTextColor(activity.getColor(R.color.white));
        apply.setTextSize(15);
        apply.setTypeface(Typeface.DEFAULT_BOLD);
        apply.setGravity(Gravity.CENTER);
        apply.setBackgroundResource(R.drawable.bg_settings_primary);
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                0,
                dp(activity, 48),
                1f
        );
        applyParams.setMarginStart(dp(activity, 8));
        buttons.addView(cancel);
        buttons.addView(apply, applyParams);

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        View startField = (View) startValue.getParent();
        View endField = (View) endValue.getParent();
        startField.setOnClickListener(v -> {
            editingEnd[0] = false;
            render[0].run();
        });
        endField.setOnClickListener(v -> {
            editingEnd[0] = true;
            render[0].run();
        });
        render[0] = () -> {
            startValue.setText(format.format(rangeStart[0]));
            endValue.setText(format.format(rangeEnd[0]));
            startField.setBackgroundResource(editingEnd[0]
                    ? R.drawable.bg_date_field
                    : R.drawable.bg_date_field_on);
            endField.setBackgroundResource(editingEnd[0]
                    ? R.drawable.bg_date_field_on
                    : R.drawable.bg_date_field);
            for (int index = 0; index < chipViews.length; index++) {
                boolean on = index == picked[0];
                chipViews[index].setBackgroundResource(on
                        ? R.drawable.bg_period_chip_on
                        : R.drawable.bg_period_chip_off);
                chipViews[index].setTextColor(activity.getColor(on
                        ? R.color.white
                        : R.color.yakyeon_brand));
                chipViews[index].setTypeface(Typeface.create(
                        "sans-serif-medium",
                        on ? Typeface.BOLD : Typeface.NORMAL
                ));
            }
            monthLabel.setText(visible[0].getYear() + "년 " + visible[0].getMonthValue() + "월");
            calendar.removeAllViews();
            calendar.addView(weekdayHeader(activity));
            calendar.addView(monthDays(activity, visible[0], rangeStart[0], rangeEnd[0], date -> {
                if (editingEnd[0]) {
                    if (date.isBefore(rangeStart[0])) {
                        rangeEnd[0] = rangeStart[0];
                        rangeStart[0] = date;
                    } else {
                        rangeEnd[0] = date;
                    }
                } else {
                    if (date.isAfter(rangeEnd[0])) {
                        rangeStart[0] = rangeEnd[0];
                        rangeEnd[0] = date;
                    } else {
                        rangeStart[0] = date;
                    }
                    editingEnd[0] = true;
                }
                picked[0] = presetIndex(rangeStart[0], rangeEnd[0], LocalDate.now());
                if (!YearMonth.from(date).equals(visible[0])) {
                    visible[0] = YearMonth.from(date);
                }
                render[0].run();
            }));
        };
        render[0].run();

        Dialog dialog = new Dialog(activity, R.style.ThemeOverlay_Yakyeon_Popup);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(card);
        dialog.setContentView(scroll);
        dialog.setCancelable(true);
        cancel.setOnClickListener(v -> dialog.dismiss());
        apply.setOnClickListener(v -> {
            if (options.setPeriodRange(rangeStart[0], rangeEnd[0])) {
                LocalAppSettings.saveShare(activity, options);
                host.refresh();
            }
            dialog.dismiss();
        });
        dialog.setOnShowListener(shown -> {
            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int width = Math.min(metrics.widthPixels - dp(activity, 28), dp(activity, 420));
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        });
        dialog.show();
    }

    private static String currentMedicineLine(AppCompatActivity activity, MedicationStore store) {
        List<String> names = new ArrayList<>();
        for (DrugRelations.Node node : DrugRelations.takingNodes(store)) {
            names.add(node.getName());
        }
        if (names.isEmpty()) {
            return activity.getString(R.string.settings_share_no_medicines);
        }
        StringBuilder line = new StringBuilder();
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                line.append(", ");
            }
            line.append(names.get(index));
        }
        return activity.getString(R.string.settings_share_medicines, line);
    }

    private static int permissionNote(AppCompatActivity activity) {
        boolean enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled();
        return enabled ? R.string.settings_noti_os_on : R.string.settings_noti_os_off;
    }

    private static void confirmWithdraw(AppCompatActivity activity, Runnable confirm, MainScreens.Host host) {
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.settings_withdraw_title),
                activity.getString(R.string.settings_withdraw_body),
                activity.getString(R.string.settings_withdraw_confirm),
                activity.getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        confirm.run();
                        host.showMessage(activity.getString(R.string.settings_withdraw_saved));
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    private static TextView legalHeading(AppCompatActivity activity, int text, int top) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(activity.getColor(R.color.yakyeon_text));
        view.setTextSize(15);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = top;
        params.bottomMargin = 0;
        view.setLayoutParams(params);
        return view;
    }

    private static LinearLayout legalCard(AppCompatActivity activity) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16f * activity.getResources().getDisplayMetrics().density);
        card.setPadding(pad, pad, pad, pad);
        card.setBackgroundResource(R.drawable.bg_legal_card);
        card.setElevation(Math.round(4f * activity.getResources().getDisplayMetrics().density));
        return card;
    }

    private static LinearLayout withdrawCard(
            AppCompatActivity activity,
            int title,
            int[] labels,
            boolean[] marked,
            Runnable[] actions,
            MainScreens.Host host
    ) {
        LinearLayout card = legalCard(activity);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        gap.topMargin = Math.round(12f * activity.getResources().getDisplayMetrics().density);
        card.setLayoutParams(gap);
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_settings_person);
        icon.setBackgroundResource(R.drawable.bg_legal_icon);
        int size = Math.round(38f * activity.getResources().getDisplayMetrics().density);
        int iconPad = Math.round(9f * activity.getResources().getDisplayMetrics().density);
        icon.setPadding(iconPad, iconPad, iconPad, iconPad);
        header.addView(icon, new LinearLayout.LayoutParams(size, size));
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextColor(activity.getColor(R.color.yakyeon_text));
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        labelParams.setMarginStart(Math.round(12f * activity.getResources().getDisplayMetrics().density));
        header.addView(label, labelParams);
        card.addView(header);
        TextView limit = new TextView(activity);
        limit.setText(R.string.settings_legal_withdraw_limit);
        limit.setTextColor(activity.getColor(R.color.yakyeon_muted));
        limit.setTextSize(11);
        limit.setLineSpacing(0f, 1.65f);
        LinearLayout.LayoutParams limitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        limitParams.topMargin = gap.topMargin;
        card.addView(limit, limitParams);
        for (int index = 0; index < labels.length; index++) {
            card.addView(withdrawButton(activity, labels[index], marked[index], actions[index]));
        }
        return card;
    }

    private static View withdrawButton(
            AppCompatActivity activity,
            int label,
            boolean marked,
            Runnable mark
    ) {
        TextView button = button(activity, label, mark);
        button.setBackgroundResource(R.drawable.bg_withdraw_button);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(Math.round(44f * activity.getResources().getDisplayMetrics().density));
        button.setTextColor(activity.getColor(R.color.yakyeon_muted));
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(
                Math.round(12f * activity.getResources().getDisplayMetrics().density),
                Math.round(10f * activity.getResources().getDisplayMetrics().density),
                Math.round(12f * activity.getResources().getDisplayMetrics().density),
                Math.round(10f * activity.getResources().getDisplayMetrics().density)
        );
        ViewGroup.LayoutParams current = button.getLayoutParams();
        if (current instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) current).topMargin =
                    Math.round(12f * activity.getResources().getDisplayMetrics().density);
        }
        if (marked) {
            button.setText(R.string.settings_consent_restore);
            button.setBackgroundResource(R.drawable.bg_consent_agree);
            button.setTextColor(activity.getColor(R.color.yakyeon_badge_bg));
        }
        return button;
    }

    private static View childToggle(
            AppCompatActivity activity,
            NotificationPreferences preferences,
            boolean enabled,
            int title,
            int help,
            boolean checked,
            java.util.function.Consumer<Boolean> apply,
            MainScreens.Host host
    ) {
        LinearLayout row = toggle(activity, title, help, checked, value -> {
            apply.accept(value);
            LocalAppSettings.saveNotifications(activity, preferences);
            host.refresh();
        });
        row.setEnabled(enabled);
        SwitchCompat toggle = (SwitchCompat) row.getChildAt(row.getChildCount() - 1);
        toggle.setEnabled(enabled);
        return row;
    }

    private static View shareToggle(
            AppCompatActivity activity,
            ClinicalShareOptions options,
            int title,
            boolean checked,
            java.util.function.Consumer<Boolean> apply,
            MainScreens.Host host
    ) {
        return toggle(activity, title, 0, checked, value -> {
            apply.accept(value);
            LocalAppSettings.saveShare(activity, options);
            host.refresh();
        });
    }

    private static View fieldRow(AppCompatActivity activity, int label, int value) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.space_s);
        row.setPadding(0, pad, 0, pad);
        TextView title = new TextView(activity);
        title.setText(label);
        title.setTextColor(activity.getColor(R.color.yakyeon_text));
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        TextView body = new TextView(activity);
        body.setText(value);
        body.setTextColor(activity.getColor(R.color.yakyeon_muted));
        body.setTextSize(13);
        row.addView(title);
        row.addView(body);
        return row;
    }

    private static View docRow(
            AppCompatActivity activity,
            int iconRes,
            int title,
            int subtitle,
            int subtitleSize,
            boolean divider,
            Runnable action
    ) {
        LinearLayout block = new LinearLayout(activity);
        block.setOrientation(LinearLayout.VERTICAL);
        if (divider) {
            View line = new View(activity);
            line.setBackgroundColor(activity.getColor(R.color.yakyeon_line));
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density))
            );
            lineParams.topMargin = Math.round(16f * activity.getResources().getDisplayMetrics().density);
            lineParams.bottomMargin = lineParams.topMargin;
            block.addView(line, lineParams);
        }
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setBackgroundResource(R.drawable.bg_legal_icon);
        float density = activity.getResources().getDisplayMetrics().density;
        int size = Math.round(38f * density);
        int iconPad = Math.round(9f * density);
        icon.setPadding(iconPad, iconPad, iconPad, iconPad);
        row.addView(icon, new LinearLayout.LayoutParams(size, size));
        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(Math.round(12f * density));
        textParams.setMarginEnd(Math.round(8f * density));
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextColor(activity.getColor(R.color.yakyeon_text));
        label.setTextSize(14);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        TextView detail = new TextView(activity);
        detail.setText(subtitle);
        detail.setTextColor(activity.getColor(R.color.yakyeon_muted));
        detail.setTextSize(subtitleSize);
        detail.setLineSpacing(0f, 1.5f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = Math.round(5f * density);
        text.addView(label);
        text.addView(detail, detailParams);
        row.addView(text, textParams);
        ImageView chevron = new ImageView(activity);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        row.addView(chevron, new LinearLayout.LayoutParams(Math.round(16f * density), Math.round(16f * density)));
        row.setOnClickListener(v -> action.run());
        block.addView(row);
        return block;
    }

    private static View shareChoice(
            AppCompatActivity activity,
            int title,
            int subtitle,
            CharSequence subtitleText,
            String pill,
            boolean included,
            Runnable action
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.space_s);
        row.setPadding(0, pad, 0, pad);
        row.setMinimumHeight(activity.getResources().getDimensionPixelSize(R.dimen.min_touch_target));
        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextColor(activity.getColor(R.color.yakyeon_text));
        label.setTextSize(13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        text.addView(label);
        TextView detail = new TextView(activity);
        if (subtitle != 0) {
            detail.setText(subtitle);
        } else {
            detail.setText(subtitleText);
        }
        detail.setTextColor(activity.getColor(R.color.yakyeon_muted));
        detail.setTextSize(11);
        text.addView(detail);
        row.addView(text);
        TextView badge = new TextView(activity);
        badge.setText(pill);
        badge.setTextSize(11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setPadding(pad, pad / 2, pad, pad / 2);
        boolean change = activity.getString(R.string.settings_share_change).contentEquals(pill);
        if (change) {
            badge.setTextColor(activity.getColor(R.color.yakyeon_brand));
        } else if (included) {
            badge.setBackgroundResource(R.drawable.bg_share_pill);
            badge.setTextColor(activity.getColor(R.color.yakyeon_brand));
        } else {
            badge.setBackgroundResource(R.drawable.bg_share_pill_off);
            badge.setTextColor(activity.getColor(R.color.yakyeon_muted));
        }
        row.addView(badge);
        if (action != null) {
            row.setOnClickListener(v -> action.run());
        }
        return row;
    }

    private static String readRaw(AppCompatActivity activity, int rawId) {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                activity.getResources().openRawResource(rawId),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(line);
            }
        } catch (IOException exception) {
            return activity.getString(R.string.settings_legal_local);
        }
        return text.toString();
    }

    private static LinearLayout page(AppCompatActivity activity) {
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.space_m);
        page.setPadding(pad, pad, pad, pad * 2);
        return page;
    }

    private static ScrollView scroll(LinearLayout page) {
        ScrollView scroll = new ScrollView(page.getContext());
        scroll.setFillViewport(true);
        scroll.addView(page);
        return scroll;
    }

    private static TextView heading(AppCompatActivity activity, int text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(activity.getColor(R.color.yakyeon_text));
        view.setTextSize(16);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, 0, 0, activity.getResources().getDimensionPixelSize(R.dimen.space_s));
        return view;
    }

    private static TextView note(AppCompatActivity activity, CharSequence text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(activity.getColor(R.color.yakyeon_muted));
        view.setTextSize(14);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.space_s);
        view.setPadding(0, pad, 0, pad);
        return view;
    }

    private static TextView note(AppCompatActivity activity, int text) {
        return note(activity, activity.getString(text));
    }

    private static TextView button(AppCompatActivity activity, int text, Runnable action) {
        TextView button = new TextView(activity);
        if (text != 0) {
            button.setText(text);
        }
        button.setTextColor(activity.getColor(R.color.yakyeon_text));
        button.setTextSize(16);
        button.setBackgroundResource(R.drawable.yakyeon_quiet_button);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.space_s);
        button.setPadding(pad, pad, pad, pad);
        button.setMinHeight(activity.getResources().getDimensionPixelSize(R.dimen.min_touch_target));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = pad;
        button.setLayoutParams(params);
        if (action != null) {
            button.setOnClickListener(v -> action.run());
        }
        return button;
    }

    private static LinearLayout toggle(
            AppCompatActivity activity,
            int title,
            int help,
            boolean checked,
            java.util.function.Consumer<Boolean> onChange
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.space_s);
        row.setPadding(0, pad, 0, pad);
        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextColor(activity.getColor(R.color.yakyeon_text));
        label.setTextSize(16);
        text.addView(label);
        if (help != 0) {
            TextView caption = new TextView(activity);
            caption.setText(help);
            caption.setTextColor(activity.getColor(R.color.yakyeon_muted));
            caption.setTextSize(13);
            text.addView(caption);
        }
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        text.setLayoutParams(textParams);
        row.addView(text);
        SwitchCompat toggle = new SwitchCompat(activity);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, value) -> onChange.accept(value));
        row.addView(toggle);
        return row;
    }

    public static View careLink(AppCompatActivity activity, AccountHost account, MainScreens.Host host) {
        LinearLayout page = canvasPage(activity);
        if (!account.careReady()) {
            page.addView(sectionLabel(activity, R.string.care_link_section));
            page.addView(linkMessage(activity, R.string.care_link_loading));
        } else {
            addLinkSection(activity, page, R.string.care_link_section, account.managedLinks(), account);
            if (account.showGuardianSection()) {
                addLinkSection(activity, page, R.string.care_link_guardians, account.guardianLinks(), account);
            }
        }
        TextView scope = outlineButton(activity, R.string.care_scope_go, () -> host.openDetail(CARE_QR));
        ((LinearLayout.LayoutParams) scope.getLayoutParams()).topMargin = dp(activity, 18);
        page.addView(scope);
        return canvasScroll(page);
    }

    private static void addLinkSection(
            AppCompatActivity activity,
            LinearLayout page,
            int title,
            java.util.List<LinkLine> lines,
            AccountHost account
    ) {
        page.addView(sectionLabel(activity, title));
        LinearLayout card = pillCard(activity, 0);
        if (lines.isEmpty()) {
            card.addView(linkMessage(activity, R.string.care_link_empty));
        } else {
            for (int index = 0; index < lines.size(); index++) {
                LinkLine line = lines.get(index);
                if (index > 0) {
                    card.addView(hairline(activity));
                }
                card.addView(linkedPerson(activity, line.title, line.detail));
                if (line.canUnlink) {
                    card.addView(hairline(activity));
                    card.addView(actionRow(activity, R.drawable.ic_settings_unlink, R.string.care_link_unlink, () ->
                            confirmUnlink(activity, line.linkId, account)));
                }
            }
        }
        page.addView(card);
    }

    private static TextView linkMessage(AppCompatActivity activity, int text) {
        TextView empty = new TextView(activity);
        empty.setText(text);
        empty.setTextColor(activity.getColor(R.color.yakyeon_text));
        empty.setTextSize(14);
        empty.setPadding(0, dp(activity, 12), 0, dp(activity, 12));
        return empty;
    }

    private static LinearLayout canvasPage(AppCompatActivity activity) {
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipToPadding(false);
        page.setClipChildren(false);
        int side = dp(activity, 18);
        page.setPadding(side, dp(activity, 2), side, dp(activity, 28));
        return page;
    }

    private static ScrollView canvasScroll(LinearLayout page) {
        ScrollView scroll = scroll(page);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        return scroll;
    }

    private static TextView subhead(AppCompatActivity activity, int text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(activity.getColor(R.color.yakyeon_muted));
        view.setTextSize(13);
        view.setLineSpacing(0f, 1.4f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(activity, 14);
        view.setLayoutParams(params);
        return view;
    }

    private static TextView sectionLabel(AppCompatActivity activity, int text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(activity.getColor(R.color.yakyeon_muted));
        view.setTextSize(13);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(activity, 18);
        params.bottomMargin = dp(activity, 8);
        view.setLayoutParams(params);
        return view;
    }

    private static TextView footnote(AppCompatActivity activity, int text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(activity.getColor(R.color.yakyeon_muted));
        view.setTextSize(12);
        view.setGravity(Gravity.CENTER);
        view.setLineSpacing(0f, 1.4f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(activity, 16);
        view.setLayoutParams(params);
        return view;
    }

    private static LinearLayout nameRow(AppCompatActivity activity, String nameText, Runnable onClick) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
        row.addView(roundAvatar(activity, dp(activity, 52), dp(activity, 14)));
        TextView name = new TextView(activity);
        name.setText(nameText);
        name.setTextColor(activity.getColor(R.color.yakyeon_text));
        name.setTextSize(17);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setIncludeFontPadding(false);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        nameParams.setMarginStart(dp(activity, 14));
        row.addView(name, nameParams);
        if (onClick != null) {
            tap(activity, row, onClick);
        }
        return row;
    }

    private static void promptName(AppCompatActivity activity, String current, AccountHost account) {
        EditText field = new EditText(activity);
        field.setText(current == null ? "" : current);
        if (field.getText() != null) {
            field.setSelection(field.getText().length());
        }
        field.setSingleLine(true);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.settings_name_edit)
                .setView(field)
                .setPositiveButton(R.string.action_confirm, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = field.getText() == null ? "" : field.getText().toString().trim();
            if (name.isEmpty()) {
                field.setError(activity.getString(R.string.settings_name_empty));
                return;
            }
            dialog.dismiss();
            account.submitName(name);
        }));
        dialog.show();
    }

    private static void promptPassword(AppCompatActivity activity, AccountHost account) {
        EditText current = passwordInput(activity, R.string.settings_password_current);
        EditText next = passwordInput(activity, R.string.settings_password_new);
        EditText confirm = passwordInput(activity, R.string.settings_password_confirm);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int side = dp(activity, 20);
        form.setPadding(side, dp(activity, 8), side, 0);
        form.addView(current);
        form.addView(next);
        form.addView(confirm);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.settings_password)
                .setView(form)
                .setPositiveButton(R.string.action_confirm, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = textOf(current);
            String newPassword = textOf(next);
            String newPasswordConfirm = textOf(confirm);
            if (currentPassword.isEmpty() || newPassword.isEmpty() || newPasswordConfirm.isEmpty()) {
                current.setError(activity.getString(R.string.settings_password_empty));
                return;
            }
            if (!newPassword.equals(newPasswordConfirm)) {
                confirm.setError(activity.getString(R.string.settings_password_mismatch));
                return;
            }
            if (newPassword.length() < 8 || newPassword.length() > 64) {
                next.setError(activity.getString(R.string.settings_password_length));
                return;
            }
            dialog.dismiss();
            account.submitPassword(currentPassword, newPassword, newPasswordConfirm);
        }));
        dialog.show();
    }

    private static void confirmUnlink(AppCompatActivity activity, long linkId, AccountHost account) {
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.care_unlink_confirm),
                "",
                activity.getString(R.string.care_unlink_action),
                activity.getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        account.unlinkCareLink(linkId);
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    private static void confirmLogout(AppCompatActivity activity, AccountHost account) {
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.settings_logout),
                activity.getString(R.string.settings_logout_confirm_message),
                activity.getString(R.string.settings_logout),
                activity.getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        account.logout();
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    private static void promptAccountDelete(AppCompatActivity activity, AccountHost account) {
        EditText password = passwordInput(activity, R.string.settings_password_current);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int side = dp(activity, 20);
        form.setPadding(side, dp(activity, 8), side, 0);
        form.addView(password);
        ProfileSnapshot profile = account.currentProfile();
        boolean guardian = profile != null && AccountRoles.GUARDIAN.equals(profile.accountType);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.settings_withdraw_account)
                .setMessage(guardian
                        ? R.string.settings_account_delete_guardian
                        : R.string.settings_account_delete_patient)
                .setView(form)
                .setPositiveButton(R.string.settings_withdraw_account, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = textOf(password);
            if (value.isEmpty()) {
                password.setError(activity.getString(R.string.settings_password_empty));
                return;
            }
            dialog.dismiss();
            account.submitAccountDelete(value);
        }));
        dialog.show();
    }

    private static EditText passwordInput(AppCompatActivity activity, int hint) {
        EditText field = new EditText(activity);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return field;
    }

    private static String textOf(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private static LinearLayout linkedPerson(AppCompatActivity activity, String title, String detailText) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
        row.addView(roundAvatar(activity, dp(activity, 48), dp(activity, 12)));
        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textParams.setMarginStart(dp(activity, 12));
        textParams.setMarginEnd(dp(activity, 8));
        TextView name = new TextView(activity);
        name.setText(title);
        name.setTextColor(activity.getColor(R.color.yakyeon_text));
        name.setTextSize(16);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setIncludeFontPadding(false);
        text.addView(name);
        TextView detail = new TextView(activity);
        detail.setText(detailText == null || detailText.isEmpty()
                ? activity.getString(R.string.care_link_status_linked)
                : detailText);
        detail.setTextColor(activity.getColor(R.color.yakyeon_muted));
        detail.setTextSize(12);
        detail.setIncludeFontPadding(false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = dp(activity, 4);
        text.addView(detail, detailParams);
        row.addView(text, textParams);
        row.addView(statusBadge(activity, R.string.care_link_badge));
        return row;
    }

    private static ImageView roundAvatar(AppCompatActivity activity, int tile, int inset) {
        ImageView avatar = new ImageView(activity);
        avatar.setImageResource(R.drawable.ic_settings_person);
        avatar.setImageTintList(ColorStateList.valueOf(activity.getColor(R.color.yakyeon_primary)));
        avatar.setImageTintMode(PorterDuff.Mode.SRC_IN);
        avatar.setBackgroundResource(R.drawable.bg_icon_circle);
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        avatar.setPadding(inset, inset, inset, inset);
        avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(tile, tile));
        return avatar;
    }

    private static TextView statusBadge(AppCompatActivity activity, int text) {
        TextView mark = new TextView(activity);
        mark.setText(text);
        mark.setTextColor(activity.getColor(R.color.yakyeon_badge));
        mark.setTextSize(12);
        mark.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mark.setIncludeFontPadding(false);
        mark.setGravity(Gravity.CENTER);
        mark.setBackgroundResource(R.drawable.bg_status_pill);
        int horizontal = dp(activity, 10);
        int vertical = dp(activity, 5);
        mark.setPadding(horizontal, vertical, horizontal, vertical);
        return mark;
    }

    private static View hairline(AppCompatActivity activity) {
        View line = new View(activity);
        line.setBackgroundColor(activity.getColor(R.color.yakyeon_divider));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density))
        );
        params.topMargin = dp(activity, 2);
        params.bottomMargin = dp(activity, 2);
        line.setLayoutParams(params);
        return line;
    }

    private static LinearLayout actionRow(
            AppCompatActivity activity,
            int icon,
            int title,
            Runnable action
    ) {
        LinearLayout row = pillRow(activity, icon, activity.getString(title), null, false, chevron(activity));
        tap(activity, row, action);
        return row;
    }

    private static void tap(AppCompatActivity activity, View view, Runnable action) {
        view.setClickable(true);
        view.setFocusable(true);
        TypedValue ripple = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        view.setForeground(activity.getDrawable(ripple.resourceId));
        view.setOnClickListener(v -> action.run());
    }

    private static LinearLayout toggleLine(
            AppCompatActivity activity,
            int icon,
            int title,
            int help,
            boolean checked,
            java.util.function.Consumer<Boolean> onChange
    ) {
        return pillRow(
                activity,
                icon,
                activity.getString(title),
                activity.getString(help),
                false,
                pillSwitch(activity, checked, onChange)
        );
    }

    private static void addDetailToggle(
            LinearLayout card,
            AppCompatActivity activity,
            boolean enabled,
            int title,
            int help,
            boolean checked,
            java.util.function.Consumer<Boolean> apply,
            AccountHost account,
            boolean first
    ) {
        if (!first) {
            card.addView(hairline(activity));
        }
        FrameLayout toggle = pillSwitch(activity, checked, value -> account.commitNotificationEdit(() ->
                apply.accept(value)));
        if (!enabled) {
            toggle.setAlpha(0.4f);
            toggle.setOnClickListener(null);
            toggle.setClickable(false);
        }
        card.addView(textSwitchRow(activity, title, help, toggle));
    }

    private static LinearLayout textSwitchRow(
            AppCompatActivity activity,
            int title,
            int help,
            View trailing
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextColor(activity.getColor(R.color.yakyeon_text));
        label.setTextSize(15);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setIncludeFontPadding(false);
        text.addView(label);
        TextView detail = new TextView(activity);
        detail.setText(help);
        detail.setTextColor(activity.getColor(R.color.yakyeon_muted));
        detail.setTextSize(12);
        detail.setIncludeFontPadding(false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = dp(activity, 4);
        text.addView(detail, detailParams);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textParams.setMarginEnd(dp(activity, 8));
        row.addView(text, textParams);
        row.addView(trailing);
        return row;
    }

    private static LinearLayout shareLine(
            AppCompatActivity activity,
            int icon,
            CharSequence title,
            CharSequence subtitle,
            String pill,
            int kind,
            Runnable action
    ) {
        TextView badge = new TextView(activity);
        badge.setText(pill);
        badge.setTextSize(13);
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        badge.setIncludeFontPadding(false);
        badge.setGravity(Gravity.CENTER);
        int horizontal = dp(activity, 12);
        int vertical = dp(activity, 6);
        if (kind == 0) {
            badge.setTextColor(activity.getColor(R.color.yakyeon_brand));
            badge.setPadding(horizontal, vertical, 0, vertical);
        } else if (kind == 1) {
            badge.setBackgroundResource(R.drawable.bg_share_pill);
            badge.setTextColor(activity.getColor(R.color.yakyeon_brand));
            badge.setPadding(horizontal, vertical, horizontal, vertical);
        } else {
            badge.setBackgroundResource(R.drawable.bg_share_pill_off);
            badge.setTextColor(activity.getColor(R.color.yakyeon_placeholder));
            badge.setPadding(horizontal, vertical, horizontal, vertical);
        }
        LinearLayout row = pillRow(activity, icon, title, subtitle, false, badge);
        if (action != null) {
            tap(activity, row, action);
        }
        return row;
    }

    private static String sharePeriodText(AppCompatActivity activity, ClinicalShareOptions options) {
        LocalDate today = LocalDate.now();
        LocalDate start = options.resolveStart(today);
        LocalDate end = options.resolveEnd(today);
        int span = (int) (end.toEpochDay() - start.toEpochDay() + 1);
        if (end.equals(today) && (span == ClinicalShareOptions.DAYS_7
                || span == ClinicalShareOptions.DAYS_30
                || span == ClinicalShareOptions.DAYS_90)) {
            return activity.getString(R.string.settings_share_period, span);
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        return activity.getString(
                R.string.settings_share_custom_range,
                format.format(start),
                format.format(end)
        );
    }

    private static int presetIndex(LocalDate start, LocalDate end, LocalDate today) {
        int span = (int) (end.toEpochDay() - start.toEpochDay() + 1);
        if (!end.equals(today)) {
            return -1;
        }
        if (span == ClinicalShareOptions.DAYS_7) {
            return 0;
        }
        if (span == ClinicalShareOptions.DAYS_30) {
            return 1;
        }
        if (span == ClinicalShareOptions.DAYS_90) {
            return 2;
        }
        return -1;
    }

    private static LinearLayout shareChoiceRow(
            AppCompatActivity activity,
            int icon,
            CharSequence title,
            CharSequence subtitle,
            boolean included,
            java.util.function.Consumer<Boolean> onPick
    ) {
        LinearLayout pair = new LinearLayout(activity);
        pair.setOrientation(LinearLayout.HORIZONTAL);
        pair.setGravity(Gravity.CENTER_VERTICAL);
        pair.addView(choiceChip(
                activity,
                activity.getString(R.string.settings_share_exclude),
                !included,
                () -> onPick.accept(false)
        ));
        TextView include = choiceChip(
                activity,
                activity.getString(R.string.settings_share_include),
                included,
                () -> onPick.accept(true)
        );
        LinearLayout.LayoutParams includeParams = (LinearLayout.LayoutParams) include.getLayoutParams();
        includeParams.setMarginStart(dp(activity, 6));
        pair.addView(include);
        return pillRow(activity, icon, title, subtitle, false, pair);
    }

    private static TextView choiceChip(
            AppCompatActivity activity,
            String label,
            boolean selected,
            Runnable action
    ) {
        TextView chip = new TextView(activity);
        chip.setText(label);
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(12);
        chip.setIncludeFontPadding(false);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        int horizontal = dp(activity, 10);
        int vertical = dp(activity, 6);
        chip.setPadding(horizontal, vertical, horizontal, vertical);
        chip.setBackgroundResource(selected ? R.drawable.bg_period_chip_on : R.drawable.bg_period_chip_off);
        chip.setTextColor(activity.getColor(selected ? R.color.white : R.color.yakyeon_brand));
        chip.setOnClickListener(v -> action.run());
        chip.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return chip;
    }

    private static TextView outlineButton(AppCompatActivity activity, int text, Runnable action) {
        TextView button = new TextView(activity);
        button.setText(text);
        button.setTextColor(activity.getColor(R.color.yakyeon_brand));
        button.setTextSize(15);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_outline_button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(activity, 52)
        );
        params.topMargin = dp(activity, 12);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private static TextView dateColumn(
            AppCompatActivity activity,
            LinearLayout row,
            int label,
            boolean start
    ) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        if (start) {
            columnParams.setMarginEnd(dp(activity, 8));
        } else {
            columnParams.setMarginStart(dp(activity, 8));
        }
        column.setLayoutParams(columnParams);
        TextView caption = new TextView(activity);
        caption.setText(label);
        caption.setTextColor(activity.getColor(R.color.yakyeon_muted));
        caption.setTextSize(12);
        column.addView(caption);
        LinearLayout field = new LinearLayout(activity);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setBackgroundResource(R.drawable.bg_date_field);
        int fieldPad = dp(activity, 12);
        field.setPadding(fieldPad, dp(activity, 10), fieldPad, dp(activity, 10));
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        fieldParams.topMargin = dp(activity, 6);
        field.setLayoutParams(fieldParams);
        TextView value = new TextView(activity);
        value.setTextColor(activity.getColor(R.color.yakyeon_text));
        value.setTextSize(13);
        value.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        value.setIncludeFontPadding(false);
        field.addView(value, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_yakyeon_calendar);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int iconSize = dp(activity, 16);
        field.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));
        column.addView(field);
        row.addView(column);
        return value;
    }

    private static ImageView navChevron(AppCompatActivity activity, boolean left) {
        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_chevron_right);
        icon.setImageTintList(ColorStateList.valueOf(activity.getColor(R.color.yakyeon_text)));
        if (left) {
            icon.setRotation(180f);
        }
        int size = dp(activity, 32);
        int inset = dp(activity, 6);
        icon.setPadding(inset, inset, inset, inset);
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return icon;
    }

    private static LinearLayout weekdayHeader(AppCompatActivity activity) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"일", "월", "화", "수", "목", "금", "토"};
        for (int index = 0; index < labels.length; index++) {
            TextView label = new TextView(activity);
            label.setText(labels[index]);
            label.setGravity(Gravity.CENTER);
            label.setTextSize(12);
            label.setTextColor(activity.getColor(index == 0
                    ? R.color.yakyeon_sunday
                    : R.color.yakyeon_muted));
            row.addView(label, new LinearLayout.LayoutParams(0, dp(activity, 28), 1f));
        }
        return row;
    }

    private static LinearLayout monthDays(
            AppCompatActivity activity,
            YearMonth month,
            LocalDate start,
            LocalDate end,
            java.util.function.Consumer<LocalDate> onPick
    ) {
        LinearLayout grid = new LinearLayout(activity);
        grid.setOrientation(LinearLayout.VERTICAL);
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() % 7;
        int cells = offset + month.lengthOfMonth();
        int rows = (cells + 6) / 7;
        LocalDate cursor = first.minusDays(offset);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column < 7; column++) {
                LocalDate date = cursor.plusDays(rowIndex * 7L + column);
                row.addView(dayCell(activity, date, month, start, end, onPick));
            }
            grid.addView(row);
        }
        return grid;
    }

    private static FrameLayout dayCell(
            AppCompatActivity activity,
            LocalDate date,
            YearMonth month,
            LocalDate start,
            LocalDate end,
            java.util.function.Consumer<LocalDate> onPick
    ) {
        FrameLayout cell = new FrameLayout(activity);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, dp(activity, 36), 1f));
        if (onPick != null) {
            cell.setOnClickListener(v -> onPick.accept(date));
            cell.setClickable(true);
        }
        boolean inRange = !date.isBefore(start) && !date.isAfter(end);
        boolean endpoint = date.equals(start) || date.equals(end);
        boolean inMonth = YearMonth.from(date).equals(month);
        if (inRange && !start.equals(end)) {
            cell.setBackgroundColor(activity.getColor(R.color.yakyeon_mint));
        }
        TextView label = new TextView(activity);
        label.setText(String.valueOf(date.getDayOfMonth()));
        label.setGravity(Gravity.CENTER);
        label.setTextSize(13);
        label.setIncludeFontPadding(false);
        if (endpoint) {
            label.setBackgroundResource(R.drawable.bg_cal_day_on);
            label.setTextColor(activity.getColor(R.color.white));
            label.setTypeface(Typeface.DEFAULT_BOLD);
            int size = dp(activity, 30);
            cell.addView(label, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        } else {
            if (!inMonth && !inRange) {
                label.setTextColor(activity.getColor(R.color.yakyeon_placeholder));
            } else if (date.getDayOfWeek().getValue() % 7 == 0) {
                label.setTextColor(activity.getColor(R.color.yakyeon_sunday));
            } else {
                label.setTextColor(activity.getColor(R.color.yakyeon_text));
            }
            cell.addView(label, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            ));
        }
        return cell;
    }
}
