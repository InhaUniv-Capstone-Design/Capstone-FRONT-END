package com.example.yakyeon2.share;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.yakyeon2.R;
import com.example.yakyeon2.data.ClinicalShareOptions;
import com.example.yakyeon2.data.MedicationStore;
import com.example.yakyeon2.data.ShareSummary;
import com.example.yakyeon2.ui.ConfirmDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ShareSummaryExporter {

    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ShareSummaryExporter() {
    }

    public static void open(AppCompatActivity activity, MedicationStore store, ClinicalShareOptions options) {
        open(activity, ShareSummary.build(
                store,
                options,
                null,
                LocalDate.now(ZoneId.systemDefault())
        ));
    }

    public static void open(AppCompatActivity activity, ShareSummary summary) {
        Bitmap bitmap = ShareSummaryPage.render(activity, summary);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, encoded);
        byte[] png = encoded.toByteArray();
        ScrollView scroll = new ScrollView(activity);
        ImageView image = new ImageView(activity);
        image.setAdjustViewBounds(true);
        image.setImageBitmap(bitmap);
        image.setContentDescription(activity.getString(R.string.share_png_title));
        scroll.addView(image, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int previewWidth = Math.max(1, metrics.widthPixels - Math.round(48f * metrics.density));
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        int maxHeight = Math.round(metrics.heightPixels * 0.62f);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.min(previewHeight, maxHeight)
        ));
        new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setTitle(R.string.share_png_title)
                .setView(scroll)
                .setPositiveButton(R.string.share_png_save, (dialog, which) -> confirmSave(activity, png))
                .setNegativeButton(R.string.share_png_close, null)
                .setOnDismissListener(dialog -> {
                    image.setImageDrawable(null);
                    bitmap.recycle();
                })
                .show();
    }

    private static void confirmSave(AppCompatActivity activity, byte[] png) {
        ConfirmDialog.show(
                activity,
                activity.getString(R.string.share_png_save_title),
                activity.getString(R.string.share_png_save_body),
                activity.getString(R.string.share_png_save),
                activity.getString(R.string.action_cancel),
                new ConfirmDialog.Callback() {
                    @Override
                    public void onConfirm() {
                        String name = "약연-복약요약-" + FILE_DAY.format(LocalDate.now(ZoneId.systemDefault())) + ".png";
                        if (saveToPictures(activity, png, name)) {
                            tell(activity, activity.getString(R.string.share_png_saved));
                        } else if (shareFile(activity, png, name)) {
                            tell(activity, activity.getString(R.string.share_png_send));
                        } else {
                            tell(activity, activity.getString(R.string.share_png_save_failed));
                        }
                    }

                    @Override
                    public void onCancel() {
                    }
                }
        );
    }

    private static boolean saveToPictures(AppCompatActivity activity, byte[] png, String displayName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/약연");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            return false;
        }
        try (OutputStream out = activity.getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                activity.getContentResolver().delete(uri, null, null);
                return false;
            }
            out.write(png);
        } catch (IOException exception) {
            activity.getContentResolver().delete(uri, null, null);
            return false;
        }
        values.clear();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        return activity.getContentResolver().update(uri, values, null, null) == 1;
    }

    private static boolean shareFile(AppCompatActivity activity, byte[] png, String displayName) {
        File dir = new File(activity.getCacheDir(), "share");
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }
        File file = new File(dir, displayName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(png);
        } catch (IOException exception) {
            return false;
        }
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/png");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(send, activity.getString(R.string.share_png_send)));
        return true;
    }

    private static void tell(AppCompatActivity activity, String message) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }
}
