package com.example.yakyeon.ui.medication;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.yakyeon.R;
import com.example.yakyeon.data.MedicationSearchRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MedicationInputControls {

    private static final Pattern TIME = Pattern.compile("(\\d{2}):(\\d{2})");
    private static final Pattern PERIOD = Pattern.compile(
            "(\\d{4}\\.\\d{2}\\.\\d{2})\\s*~\\s*(\\d{4}\\.\\d{2}\\.\\d{2})"
    );

    private MedicationInputControls() {
    }

    public static void bindName(
            AutoCompleteTextView field,
            MedicationSearchRepository repository,
            String current
    ) {
        field.setAdapter(new NameAdapter(field.getContext(), repository));
        field.setText(current == null ? "" : current, false);
    }

    public static void bindChoice(AutoCompleteTextView field, String[] options, String current) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                field.getContext(),
                android.R.layout.simple_list_item_1,
                options
        ) {
            private final Filter choiceFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = options;
                    results.count = options.length;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };

            @NonNull
            @Override
            public Filter getFilter() {
                return choiceFilter;
            }
        };
        field.setAdapter(adapter);
        field.setKeyListener(null);
        field.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                field.showDropDown();
            }
            return false;
        });
        if (contains(options, current)) {
            field.setText(current, false);
        }
    }

    public static void bindTime(TextView field, String current) {
        Matcher matcher = TIME.matcher(current == null ? "" : current);
        if (matcher.find()) {
            field.setText(matcher.group(1) + ":" + matcher.group(2));
        }
        field.setOnClickListener(view -> showTimePicker(field));
    }

    @Nullable
    public static Calendar[] parsePeriod(String value) {
        Matcher matcher = PERIOD.matcher(value == null ? "" : value);
        if (!matcher.matches()) {
            return null;
        }
        Calendar start = parseDate(matcher.group(1));
        Calendar end = parseDate(matcher.group(2));
        if (start == null || end == null || end.before(start)) {
            return null;
        }
        return new Calendar[]{start, end};
    }

    public static String formatDate(Calendar calendar) {
        return dateFormat().format(calendar.getTime());
    }

    public static String formatPeriod(Calendar start, Calendar end) {
        return formatDate(start) + " ~ " + formatDate(end);
    }

    @Nullable
    public static Integer validationError(
            Context context,
            MedicationSearchRepository searchRepository,
            String selectedName,
            String amountText,
            String unit,
            String times,
            String time,
            String timing,
            Calendar startDate,
            Calendar endDate
    ) {
        if (!searchRepository.isKnownName(selectedName)) {
            return R.string.medication_edit_name_invalid;
        }
        if (!contains(context.getResources().getStringArray(R.array.medication_dose_quantities), amountText)
                || !contains(context.getResources().getStringArray(R.array.medication_dose_units), unit)
                || !contains(context.getResources().getStringArray(R.array.medication_times_per_day), times)
                || !TIME.matcher(time).matches()
                || !contains(context.getResources().getStringArray(R.array.medication_dose_timings), timing)
                || startDate == null
                || endDate == null) {
            return R.string.medication_edit_required;
        }
        if (endDate.before(startDate)) {
            return R.string.medication_edit_period_order;
        }
        return null;
    }

    public static boolean contains(String[] options, String value) {
        if (value == null) {
            return false;
        }
        for (String option : options) {
            if (option.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static String textOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    public static Calendar dateOnly(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static void showTimePicker(TextView field) {
        int hour = 8;
        int minute = 0;
        Matcher matcher = TIME.matcher(textOf(field));
        if (matcher.matches()) {
            hour = Integer.parseInt(matcher.group(1));
            minute = Integer.parseInt(matcher.group(2));
        }
        new TimePickerDialog(
                field.getContext(),
                (picker, selectedHour, selectedMinute) -> field.setText(
                        String.format(Locale.KOREA, "%02d:%02d", selectedHour, selectedMinute)
                ),
                hour,
                minute,
                true
        ).show();
    }

    @Nullable
    private static Calendar parseDate(String value) {
        try {
            Date parsed = dateFormat().parse(value);
            if (parsed == null) {
                return null;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return dateOnly(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
        } catch (ParseException exception) {
            return null;
        }
    }

    private static SimpleDateFormat dateFormat() {
        return new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
    }

    private static final class NameAdapter extends ArrayAdapter<String> {

        private final MedicationSearchRepository repository;
        private final Filter nameFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<String> matched = repository.searchMedicationNames(
                        constraint == null ? "" : constraint.toString()
                );
                FilterResults filterResults = new FilterResults();
                filterResults.values = matched;
                filterResults.count = matched.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults filterResults) {
                Object values = filterResults.values;
                if (values instanceof List) {
                    results = new ArrayList<>();
                    for (Object value : (List<?>) values) {
                        if (value instanceof String && !TextUtils.isEmpty((String) value)) {
                            results.add((String) value);
                        }
                    }
                } else {
                    results = new ArrayList<>();
                }
                if (results.isEmpty()) {
                    notifyDataSetInvalidated();
                } else {
                    notifyDataSetChanged();
                }
            }
        };
        private List<String> results = new ArrayList<>();

        private NameAdapter(Context context, MedicationSearchRepository repository) {
            super(context, android.R.layout.simple_list_item_1);
            this.repository = repository;
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Nullable
        @Override
        public String getItem(int position) {
            return results.get(position);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return nameFilter;
        }
    }
}
