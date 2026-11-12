package com.example.yakyeon2.main;

import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon2.R;
import com.example.yakyeon2.api.HomeContracts;
import com.example.yakyeon2.data.MedicineNameCatalog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public final class MedicineNamePicker {

    public interface Callback {
        void onChosen(String name);
    }

    public interface RemoteChoice {
        void onChosen(String name, Long drugId);
    }

    public interface ResultListener {
        void onResult(java.util.List<HomeContracts.DrugSuggestion> rows, String error);
    }

    public interface Searcher {
        void search(String query, ResultListener listener);
    }

    private MedicineNamePicker() {
    }

    public static void show(AppCompatActivity activity, MedicineNameCatalog catalog, Callback callback) {
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_medicine_name, null);
        EditText query = root.findViewById(R.id.field_name_query);
        TextView empty = root.findViewById(R.id.text_name_empty);
        LinearLayout results = root.findViewById(R.id.name_results);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setTitle(activity.getString(R.string.med_search))
                .setView(root)
                .setNegativeButton(activity.getString(R.string.action_cancel), null)
                .create();
        Runnable fill = () -> fillResults(activity, catalog, results, empty, query.getText().toString(), name -> {
            callback.onChosen(name);
            dialog.dismiss();
        });
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fill.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        fill.run();
        dialog.setOnShowListener(shown -> {
            int white = activity.getColor(R.color.white);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(white));
            }
            View panel = dialog.findViewById(com.google.android.material.R.id.parentPanel);
            if (panel != null) {
                panel.setBackgroundColor(white);
            }
        });
        dialog.show();
    }

    public static void showRemote(AppCompatActivity activity, Searcher searcher, RemoteChoice callback) {
        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_medicine_name, null);
        EditText query = root.findViewById(R.id.field_name_query);
        TextView empty = root.findViewById(R.id.text_name_empty);
        LinearLayout results = root.findViewById(R.id.name_results);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Yakyeon_Dialog)
                .setTitle(activity.getString(R.string.med_search))
                .setView(root)
                .setNegativeButton(activity.getString(R.string.action_cancel), null)
                .create();
        int[] generation = new int[] {0};
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int ticket = ++generation[0];
                String text = s == null ? "" : s.toString();
                searcher.search(text, (rows, error) -> {
                    if (ticket != generation[0] || !dialog.isShowing()) {
                        return;
                    }
                    fillRemote(activity, results, empty, text, rows, error, (name, drugId) -> {
                        callback.onChosen(name, drugId);
                        dialog.dismiss();
                    });
                });
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        dialog.show();
    }

    private static void fillRemote(
            AppCompatActivity activity,
            LinearLayout results,
            TextView empty,
            String query,
            java.util.List<HomeContracts.DrugSuggestion> rows,
            String error,
            RemoteChoice callback
    ) {
        results.removeAllViews();
        if (error != null && !error.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(error);
            return;
        }
        if (rows == null || rows.isEmpty()) {
            String typed = query == null ? "" : query.trim();
            if (typed.length() >= 2) {
                empty.setVisibility(View.VISIBLE);
                empty.setText(R.string.med_search_empty);
                results.addView(directEntry(activity, () -> callback.onChosen(typed, null)));
            } else if (!typed.isEmpty()) {
                empty.setVisibility(View.GONE);
                results.addView(directEntry(activity, () -> callback.onChosen(typed, null)));
            } else {
                empty.setVisibility(View.GONE);
            }
            return;
        }
        empty.setVisibility(View.GONE);
        for (HomeContracts.DrugSuggestion row : rows) {
            TextView item = new TextView(activity);
            String label = row.manufacturer == null || row.manufacturer.isEmpty()
                    ? row.productName
                    : row.productName + " · " + row.manufacturer;
            item.setText(label);
            item.setTextColor(activity.getColor(R.color.yakyeon_text));
            item.setPadding(0, 16, 0, 16);
            item.setOnClickListener(v -> callback.onChosen(row.productName, row.drugId));
            results.addView(item);
        }
    }

    private static void fillResults(
            AppCompatActivity activity,
            MedicineNameCatalog catalog,
            LinearLayout results,
            TextView empty,
            String query,
            Callback callback
    ) {
        results.removeAllViews();
        List<MedicineNameCatalog.Match> hits = catalog.search(query);
        String typed = query == null ? "" : query.trim();
        if (hits.isEmpty() && !typed.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            empty.setText(R.string.med_search_empty);
            results.addView(directEntry(activity, () -> callback.onChosen(typed)));
        } else {
            empty.setVisibility(View.GONE);
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (MedicineNameCatalog.Match hit : hits) {
            View row = inflater.inflate(R.layout.item_medicine_name, results, false);
            TextView name = row.findViewById(R.id.text_name_hit);
            TextView source = row.findViewById(R.id.text_name_source);
            name.setText(hit.name);
            source.setText(activity.getString(hit.source == MedicineNameCatalog.Source.EXAMPLE
                    ? R.string.med_name_example_tag
                    : R.string.med_name_custom_tag));
            row.setOnClickListener(v -> callback.onChosen(hit.name));
            results.addView(row);
        }
    }

    private static TextView directEntry(AppCompatActivity activity, Runnable onClick) {
        TextView action = new TextView(activity);
        action.setText(R.string.med_name_type_in);
        action.setTextColor(activity.getColor(R.color.yakyeon_brand));
        action.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        action.setTypeface(action.getTypeface(), android.graphics.Typeface.BOLD);
        action.setPadding(0, 16, 0, 16);
        action.setOnClickListener(v -> onClick.run());
        return action;
    }
}
