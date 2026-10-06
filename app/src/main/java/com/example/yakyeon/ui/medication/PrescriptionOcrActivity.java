package com.example.yakyeon.ui.medication;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.yakyeon.R;
import com.example.yakyeon.data.MedicationRepository;
import com.example.yakyeon.data.MedicationSearchRepository;
import com.example.yakyeon.data.PrescriptionOcrRepository;
import com.example.yakyeon.data.mock.MockMedicationRepository;
import com.example.yakyeon.data.mock.MockMedicationSearchRepository;
import com.example.yakyeon.data.mock.MockPrescriptionOcrRepository;
import com.example.yakyeon.data.model.Medication;
import com.example.yakyeon.data.model.PrescriptionOcrResult;
import com.example.yakyeon.ui.common.BottomNav;
import com.example.yakyeon.ui.common.WindowSpacing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PrescriptionOcrActivity extends AppCompatActivity implements OcrConsentDialog.Listener {

    private final PrescriptionOcrRepository ocrRepository = MockPrescriptionOcrRepository.getInstance();
    private final MedicationRepository medications = MockMedicationRepository.getInstance();
    private final MedicationSearchRepository searchRepository = MockMedicationSearchRepository.getInstance();

    private View statusRow;
    private TextView reviewCount;
    private View resultCard;
    private LinearLayout resultList;
    private View registerButton;
    private final List<Draft> drafts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prescription_ocr);
        WindowSpacing.applySystemBars(findViewById(R.id.ocr_root));
        BottomNav.bind(this);

        ImageView photo = findViewById(R.id.ocr_photo);
        photo.setClipToOutline(true);
        photo.setOutlineProvider(ViewOutlineProvider.BACKGROUND);

        statusRow = findViewById(R.id.ocr_status_row);
        reviewCount = findViewById(R.id.ocr_review_count);
        resultCard = findViewById(R.id.ocr_card);
        resultList = findViewById(R.id.ocr_results);
        registerButton = findViewById(R.id.ocr_register);

        findViewById(R.id.ocr_capture).setOnClickListener(
                view -> OcrConsentDialog.open(getSupportFragmentManager())
        );
        registerButton.setOnClickListener(view -> registerResults());
        findViewById(R.id.ocr_add).setOnClickListener(view -> addBlankResult());
    }

    @Override
    public void onOcrConsentGranted() {
        showResults(ocrRepository.recognize());
    }

    private void showResults(List<PrescriptionOcrResult> recognized) {
        int reviews = 0;
        for (PrescriptionOcrResult result : recognized) {
            if (result.needsReview()) {
                reviews++;
            }
        }
        reviewCount.setText(getString(R.string.ocr_status_review, reviews));
        statusRow.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.VISIBLE);
        resultList.removeAllViews();
        drafts.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (PrescriptionOcrResult result : recognized) {
            View item = inflater.inflate(R.layout.item_ocr_result, resultList, false);
            drafts.add(bindDraft(item, result));
            resultList.addView(item);
        }
    }

    private void addBlankResult() {
        Calendar start = Calendar.getInstance();
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 7);
        String timing = getResources().getStringArray(R.array.medication_dose_timings)[0];
        PrescriptionOcrResult blank = new PrescriptionOcrResult(
                "",
                "",
                1,
                "정",
                "1회",
                "08:00",
                timing,
                MedicationInputControls.formatPeriod(
                        MedicationInputControls.dateOnly(
                                start.get(Calendar.YEAR),
                                start.get(Calendar.MONTH),
                                start.get(Calendar.DAY_OF_MONTH)
                        ),
                        MedicationInputControls.dateOnly(
                                end.get(Calendar.YEAR),
                                end.get(Calendar.MONTH),
                                end.get(Calendar.DAY_OF_MONTH)
                        )
                ),
                false,
                "",
                null
        );
        View item = LayoutInflater.from(this).inflate(R.layout.item_ocr_result, resultList, false);
        drafts.add(bindDraft(item, blank));
        resultList.addView(item);
    }

    private Draft bindDraft(View item, PrescriptionOcrResult result) {
        TextView title = item.findViewById(R.id.ocr_item_name);
        TextView badge = item.findViewById(R.id.ocr_item_badge);
        AutoCompleteTextView name = item.findViewById(R.id.ocr_name);
        AutoCompleteTextView doseAmount = item.findViewById(R.id.ocr_dose_amount);
        AutoCompleteTextView doseUnit = item.findViewById(R.id.ocr_dose_unit);
        AutoCompleteTextView timesPerDay = item.findViewById(R.id.ocr_times_per_day);
        TextView doseTimes = item.findViewById(R.id.ocr_dose_times);
        AutoCompleteTextView doseTiming = item.findViewById(R.id.ocr_dose_timing);
        TextView startView = item.findViewById(R.id.ocr_start_date);
        TextView endView = item.findViewById(R.id.ocr_end_date);

        Draft draft = new Draft(
                result,
                name,
                doseAmount,
                doseUnit,
                timesPerDay,
                doseTimes,
                doseTiming,
                startView,
                endView
        );
        Calendar[] period = MedicationInputControls.parsePeriod(result.getDuration());
        if (period != null) {
            draft.start = period[0];
            draft.end = period[1];
            startView.setText(MedicationInputControls.formatDate(draft.start));
            endView.setText(MedicationInputControls.formatDate(draft.end));
        }
        title.setText(result.getName());
        badge.setVisibility(result.needsReview() ? View.VISIBLE : View.GONE);
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                title.setText(s == null ? "" : s.toString());
            }
        });
        MedicationInputControls.bindName(name, searchRepository, result.getName());
        MedicationInputControls.bindChoice(
                doseAmount,
                getResources().getStringArray(R.array.medication_dose_quantities),
                Medication.formatAmount(result.getDoseAmount())
        );
        MedicationInputControls.bindChoice(
                doseUnit,
                getResources().getStringArray(R.array.medication_dose_units),
                result.getDoseUnit()
        );
        MedicationInputControls.bindChoice(
                timesPerDay,
                getResources().getStringArray(R.array.medication_times_per_day),
                result.getTimesPerDay()
        );
        MedicationInputControls.bindChoice(
                doseTiming,
                getResources().getStringArray(R.array.medication_dose_timings),
                result.getDoseTiming()
        );
        MedicationInputControls.bindTime(doseTimes, result.getDoseTimes());
        startView.setOnClickListener(view -> showDatePicker(draft, true));
        endView.setOnClickListener(view -> showDatePicker(draft, false));
        return draft;
    }

    private void showDatePicker(Draft draft, boolean startField) {
        Calendar current = startField ? draft.start : draft.end;
        Calendar initial = current == null ? Calendar.getInstance() : current;
        new DatePickerDialog(
                this,
                (picker, year, month, day) -> applyDate(draft, startField, year, month, day),
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void applyDate(Draft draft, boolean startField, int year, int month, int day) {
        Calendar picked = MedicationInputControls.dateOnly(year, month, day);
        Calendar other = startField ? draft.end : draft.start;
        if (other != null && (startField ? other.before(picked) : picked.before(other))) {
            Toast.makeText(this, R.string.medication_edit_period_order, Toast.LENGTH_SHORT).show();
            return;
        }
        if (startField) {
            draft.start = picked;
            draft.startView.setText(MedicationInputControls.formatDate(picked));
        } else {
            draft.end = picked;
            draft.endView.setText(MedicationInputControls.formatDate(picked));
        }
    }

    private void registerResults() {
        if (drafts.isEmpty()) {
            return;
        }
        List<PrescriptionOcrResult> ready = new ArrayList<>();
        for (Draft draft : drafts) {
            String selectedName = MedicationInputControls.textOf(draft.name);
            String amountText = MedicationInputControls.textOf(draft.amount);
            String unit = MedicationInputControls.textOf(draft.unit);
            String times = MedicationInputControls.textOf(draft.times);
            String time = MedicationInputControls.textOf(draft.clock);
            String timing = MedicationInputControls.textOf(draft.timing);
            Integer error = MedicationInputControls.validationError(
                    this,
                    searchRepository,
                    selectedName,
                    amountText,
                    unit,
                    times,
                    time,
                    timing,
                    draft.start,
                    draft.end
            );
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
            ready.add(draft.source.withEdits(
                    selectedName,
                    Double.parseDouble(amountText),
                    unit,
                    times,
                    time,
                    timing,
                    MedicationInputControls.formatPeriod(draft.start, draft.end)
            ));
        }
        for (PrescriptionOcrResult result : ready) {
            medications.addMedication(
                    result.getName(),
                    result.getDosage(),
                    result.getDoseAmount(),
                    result.getDoseUnit(),
                    result.getTimesPerDay(),
                    result.getDoseTimes(),
                    result.getDoseTiming(),
                    result.getDuration()
            );
        }
        finish();
    }

    private static final class Draft {
        private final PrescriptionOcrResult source;
        private final AutoCompleteTextView name;
        private final AutoCompleteTextView amount;
        private final AutoCompleteTextView unit;
        private final AutoCompleteTextView times;
        private final TextView clock;
        private final AutoCompleteTextView timing;
        private final TextView startView;
        private final TextView endView;
        private Calendar start;
        private Calendar end;

        private Draft(
                PrescriptionOcrResult source,
                AutoCompleteTextView name,
                AutoCompleteTextView amount,
                AutoCompleteTextView unit,
                AutoCompleteTextView times,
                TextView clock,
                AutoCompleteTextView timing,
                TextView startView,
                TextView endView
        ) {
            this.source = source;
            this.name = name;
            this.amount = amount;
            this.unit = unit;
            this.times = times;
            this.clock = clock;
            this.timing = timing;
            this.startView = startView;
            this.endView = endView;
        }
    }
}
