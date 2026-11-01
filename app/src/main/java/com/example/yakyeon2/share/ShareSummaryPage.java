package com.example.yakyeon2.share;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.yakyeon2.data.ShareSummary;

/**
 * Draws the clinician summary at a fixed 1080px width so the saved PNG does not depend on screen density.
 */
public final class ShareSummaryPage {

    private static final int WIDTH = 1080;
    private static final float SCALE = WIDTH / 390f;
    private static final int PAGE = 0xFFF4F7F5;
    private static final int INK = 0xFF17332C;
    private static final int GREEN = 0xFF1B6B56;
    private static final int MUTED = 0xFF6C827C;
    private static final int RULE = 0xFFD5E3DD;
    private static final int GOLD = 0xFFC6A56E;

    private final Context context;

    private ShareSummaryPage(Context context) {
        this.context = context;
    }

    public static Bitmap render(Context context, ShareSummary summary) {
        LinearLayout page = new ShareSummaryPage(context).page(summary);
        page.measure(
                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int height = Math.max(page.getMeasuredHeight(), px(200));
        page.layout(0, 0, WIDTH, height);
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        page.draw(canvas);
        return bitmap;
    }

    private LinearLayout page(ShareSummary summary) {
        LinearLayout page = vertical();
        page.setBackgroundColor(PAGE);
        int pad = px(22);
        page.setPadding(pad, pad, pad, px(28));
        TextView title = text(summary.patientLine, 22, GREEN, true);
        title.setLineSpacing(px(2), 1f);
        page.addView(title);
        page.addView(period(summary), gap(22));
        if (summary.includeDur) {
            page.addView(durCard(summary), gap(16));
        }
        page.addView(medicineCard(ShareSummary.TITLE_TAKING, summary.taking, ShareSummary.EMPTY_TAKING, false), gap(12));
        if (!summary.finished.isEmpty()) {
            page.addView(medicineCard(ShareSummary.TITLE_FINISHED, summary.finished, null, false), gap(12));
        }
        page.addView(footer(summary), gap(22));
        return page;
    }

    private LinearLayout period(ShareSummary summary) {
        LinearLayout block = vertical();
        LinearLayout label = horizontal();
        label.addView(text(ShareSummary.PERIOD_LABEL, 13, MUTED, false), weight());
        label.addView(text(summary.periodDaysText, 13, GREEN, true));
        block.addView(label);
        LinearLayout dates = horizontal();
        dates.setGravity(Gravity.CENTER_VERTICAL);
        dates.addView(text(summary.periodStartText, 24, INK, true));
        View rule = new View(context);
        rule.setBackgroundColor(RULE);
        LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(0, Math.max(px(1), 2), 1f);
        ruleParams.leftMargin = px(12);
        ruleParams.rightMargin = px(12);
        dates.addView(rule, ruleParams);
        dates.addView(text(summary.periodEndText, 24, INK, true));
        block.addView(dates, gap(12));
        LinearLayout captions = horizontal();
        captions.addView(text(ShareSummary.LABEL_START, 12, MUTED, false), weight());
        captions.addView(text(ShareSummary.LABEL_END, 12, MUTED, false));
        block.addView(captions, gap(4));
        return block;
    }

    private LinearLayout durCard(ShareSummary summary) {
        Card card = card(true);
        card.body.addView(header(ShareSummary.TITLE_DUR, summary.interactions.size() + "건"));
        if (summary.interactions.isEmpty()) {
            card.body.addView(text(ShareSummary.EMPTY_DUR, 13, MUTED, false), gap(12));
        }
        for (int index = 0; index < summary.interactions.size(); index++) {
            if (index > 0) {
                card.body.addView(divider(), gap(12));
            }
            card.body.addView(interaction(summary.interactions.get(index)), gap(14));
        }
        return card.outer;
    }

    private View interaction(ShareSummary.Interaction item) {
        LinearLayout block = vertical();
        block.addView(text(item.leftName, 15, INK, true));
        TextView plus = text("+", 14, GREEN, true);
        block.addView(plus, gap(4));
        block.addView(text(item.rightName, 15, INK, true), gap(4));
        if (!item.note.isEmpty()) {
            block.addView(text(item.note, 12, MUTED, false), gap(10));
        }
        if (!item.overlap.isEmpty()) {
            block.addView(labelValue(ShareSummary.LABEL_OVERLAP, item.overlap), gap(10));
        }
        return block;
    }

    private LinearLayout medicineCard(
            String title,
            java.util.List<ShareSummary.MedicineLine> lines,
            String empty,
            boolean gold
    ) {
        Card card = card(gold);
        card.body.addView(header(title, String.valueOf(lines.size())));
        if (lines.isEmpty() && empty != null) {
            card.body.addView(text(empty, 13, MUTED, false), gap(12));
        }
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                card.body.addView(divider(), gap(12));
            }
            card.body.addView(medicine(lines.get(index)), gap(index == 0 ? 14 : 12));
        }
        return card.outer;
    }

    private View medicine(ShareSummary.MedicineLine line) {
        LinearLayout block = vertical();
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(line.name, 15, INK, true), weight());
        if (!line.range.isEmpty() || !line.duration.isEmpty()) {
            LinearLayout right = vertical();
            right.setGravity(Gravity.END);
            if (!line.range.isEmpty()) {
                right.addView(dateText(line.range));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.leftMargin = px(12);
            row.addView(right, params);
        }
        block.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        if (!line.prescription.isEmpty()) {
            block.addView(text(line.prescription, 12, MUTED, false), gap(6));
        }
        return block;
    }

    private TextView dateText(String value) {
        TextView view = text(value, 12, GREEN, true);
        view.setGravity(Gravity.END);
        view.setMinWidth(Math.round(view.getPaint().measureText(value)) + px(2));
        return view;
    }

    private LinearLayout footer(ShareSummary summary) {
        LinearLayout block = vertical();
        if (summary.sourceValue != null) {
            block.addView(text(ShareSummary.LABEL_SOURCE, 11, MUTED, false));
            block.addView(text(summary.sourceValue, 12, INK, false), gap(3));
        }
        TextView note = text(summary.disclaimer, 11, MUTED, false);
        note.setLineSpacing(px(3), 1f);
        block.addView(note, gap(summary.sourceValue == null ? 0 : 12));
        return block;
    }

    private LinearLayout header(String title, String count) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(title, 15, INK, true), weight());
        TextView badge = text(count, 12, GREEN, true);
        badge.setBackground(pill(0xFFE7F6F0));
        int h = px(8);
        int v = px(3);
        badge.setPadding(h, v, h, v);
        row.addView(badge);
        return row;
    }

    private LinearLayout labelValue(String label, String value) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(label, 12, MUTED, false), weight());
        TextView shown = text(value, 12, INK, true);
        shown.setGravity(Gravity.END);
        row.addView(shown);
        return row;
    }

    private View divider() {
        View line = new View(context);
        line.setBackgroundColor(0xFFF0F5F3);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(px(1), 1)
        ));
        return line;
    }

    private Card card(boolean goldTop) {
        LinearLayout outer = vertical();
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(px(18));
        background.setStroke(Math.max(px(1), 1), 0xFFE3EBE7);
        outer.setBackground(background);
        outer.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        outer.setClipToOutline(true);
        if (goldTop) {
            View bar = new View(context);
            bar.setBackgroundColor(GOLD);
            outer.addView(bar, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    px(4)
            ));
        }
        LinearLayout body = vertical();
        int pad = px(16);
        body.setPadding(pad, pad, pad, pad);
        outer.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return new Card(outer, body);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value == null ? "" : value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * SCALE);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    private GradientDrawable pill(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(px(20));
        return drawable;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private static LinearLayout.LayoutParams gap(int dp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = px(dp);
        return params;
    }

    private static int px(float dp) {
        return Math.round(dp * SCALE);
    }

    private static final class Card {
        final LinearLayout outer;
        final LinearLayout body;

        Card(LinearLayout outer, LinearLayout body) {
            this.outer = outer;
            this.body = body;
        }
    }
}
