package com.example.yakyeon.ui.common;

import android.content.Context;
import android.content.Intent;

import com.example.yakyeon.MainActivity;
import com.example.yakyeon.ui.auth.SignupDraft;
import com.example.yakyeon.ui.auth.SignupFormActivity;
import com.example.yakyeon.ui.auth.SignupTypeActivity;
import com.example.yakyeon.ui.dur.DurInteractionDetailActivity;
import com.example.yakyeon.ui.dur.DurInteractionListActivity;
import com.example.yakyeon.ui.medication.MedicationListActivity;
import com.example.yakyeon.ui.medication.MedicationScheduleActivity;
import com.example.yakyeon.ui.medication.PrescriptionOcrActivity;

public final class AppNavigator {

    private AppNavigator() {
    }

    public static void openHome(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public static void openSignupType(Context context) {
        context.startActivity(new Intent(context, SignupTypeActivity.class));
    }

    public static void openSignupForm(Context context, SignupDraft.Type type) {
        Intent intent = new Intent(context, SignupFormActivity.class);
        intent.putExtra(SignupFormActivity.EXTRA_SIGNUP_TYPE, type.name());
        context.startActivity(intent);
    }

    public static void showHome(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public static void openMedicationList(Context context) {
        context.startActivity(new Intent(context, MedicationListActivity.class));
    }

    public static void openPrescriptionOcr(Context context) {
        context.startActivity(new Intent(context, PrescriptionOcrActivity.class));
    }

    public static void openContraindications(Context context) {
        context.startActivity(new Intent(context, DurInteractionListActivity.class));
    }

    public static void openDurInteractionDetail(Context context, String interactionId) {
        Intent intent = new Intent(context, DurInteractionDetailActivity.class);
        intent.putExtra(DurInteractionDetailActivity.EXTRA_ID, interactionId);
        context.startActivity(intent);
    }

    public static void openSchedule(Context context) {
        context.startActivity(new Intent(context, MedicationScheduleActivity.class));
    }

    public static void openGraph(Context context) {
        // TODO: open graph
    }

    public static void openPillbox(Context context) {
        // TODO: open pillbox
    }

    public static void openSettings(Context context) {
        // TODO: open settings
    }
}
