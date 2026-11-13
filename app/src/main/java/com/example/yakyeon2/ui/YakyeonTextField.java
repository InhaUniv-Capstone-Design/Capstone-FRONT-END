package com.example.yakyeon2.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.text.InputFilter;
import android.text.method.TextKeyListener;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Checkable;

import androidx.annotation.Nullable;

import com.example.yakyeon2.R;
import com.example.yakyeon2.auth.AuthInputTypes;
import com.example.yakyeon2.auth.LatinCredentialFilter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class YakyeonTextField extends TextInputLayout {

    private final TextInputEditText editText;
    private boolean passwordHidden = true;
    private int credentialLimit;
    private LatinCredentialFilter.Kind credentialKind;

    public YakyeonTextField(Context context) {
        this(context, null);
    }

    public YakyeonTextField(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, com.google.android.material.R.attr.textInputOutlinedStyle);
        editText = new TextInputEditText(context);
        addView(editText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        bindEditorId(getId());
        applyMeasuredFieldShape();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        bindEditorId(id);
    }

    private void applyMeasuredFieldShape() {
        float density = getResources().getDisplayMetrics().density;
        float radius = 26f * density;
        setBoxCornerRadii(radius, radius, radius, radius);
        int line = getContext().getColor(R.color.yakyeon_line);
        int primary = getContext().getColor(R.color.yakyeon_primary);
        setBoxStrokeColorStateList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                new int[]{primary, line}
        ));
        setBoxStrokeWidth(Math.max(1, Math.round(density)));
        setBoxBackgroundColor(getContext().getColor(R.color.white));
        editText.setMinHeight(Math.round(52f * density));
        editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        editText.setTextColor(getContext().getColor(R.color.yakyeon_text));
        editText.setHintTextColor(getContext().getColor(R.color.yakyeon_muted));
    }

    public void useDirectTextInput() {
        applyCredentialInput(LatinCredentialFilter.Kind.ID, false);
    }

    public void setCredentialLimit(int max) {
        credentialLimit = max;
        if (credentialKind != null) {
            lockLatinInput(credentialKind);
        }
    }

    public void setPasswordToggleEnabled(boolean enabled) {
        if (!enabled) {
            setEndIconMode(END_ICON_NONE);
            editText.setTransformationMethod(null);
            editText.setFilters(new InputFilter[0]);
            return;
        }
        applyCredentialInput(LatinCredentialFilter.Kind.PASSWORD, true);
    }

    public TextInputEditText getEditText() {
        return editText;
    }

    public String getText() {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    public void setText(CharSequence text) {
        editText.setText(text);
    }

    private void applyCredentialInput(LatinCredentialFilter.Kind kind, boolean password) {
        lockLatinInput(kind);
        if (!password) {
            setEndIconMode(END_ICON_NONE);
            editText.setTransformationMethod(null);
            return;
        }
        passwordHidden = true;
        setEndIconMode(END_ICON_CUSTOM);
        setEndIconDrawable(com.google.android.material.R.drawable.design_password_eye);
        setEndIconCheckable(true);
        setEndIconContentDescription(R.string.auth_password_visibility);
        setEndIconTintList(ColorStateList.valueOf(getContext().getColor(R.color.yakyeon_muted)));
        editText.setTransformationMethod(new MaskTransformation());
        lockLatinInput(LatinCredentialFilter.Kind.PASSWORD);
        setEndIconOnClickListener(v -> togglePassword());
        post(this::syncPasswordIcon);
    }

    private void togglePassword() {
        passwordHidden = !passwordHidden;
        int selection = editText.getSelectionEnd();
        editText.setTransformationMethod(passwordHidden ? new MaskTransformation() : null);
        lockLatinInput(LatinCredentialFilter.Kind.PASSWORD);
        if (selection >= 0) {
            editText.setSelection(Math.min(selection, editText.length()));
        }
        syncPasswordIcon();
    }

    private void lockLatinInput(LatinCredentialFilter.Kind kind) {
        credentialKind = kind;
        editText.setKeyListener(TextKeyListener.getInstance(false, TextKeyListener.Capitalize.NONE));
        editText.setRawInputType(kind == LatinCredentialFilter.Kind.PASSWORD
                ? AuthInputTypes.PASSWORD
                : AuthInputTypes.ID);
        // flagForceAscii makes Samsung Keyboard open and close at once.
        // The Latin filter already drops non-ASCII. This hint asks that keyboard for English.
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setPrivateImeOptions("defaultInputmode=english");
        InputFilter latin = LatinCredentialFilter.filter(kind);
        if (credentialLimit > 0) {
            editText.setFilters(new InputFilter[]{latin, new InputFilter.LengthFilter(credentialLimit)});
        } else {
            editText.setFilters(new InputFilter[]{latin});
        }
    }

    private void syncPasswordIcon() {
        View icon = findViewById(com.google.android.material.R.id.text_input_end_icon);
        if (icon instanceof Checkable) {
            ((Checkable) icon).setChecked(!passwordHidden);
        }
        if (icon != null) {
            icon.setFocusable(false);
            icon.setFocusableInTouchMode(false);
        }
    }

    /**
     * The inner editor is created in code, so it has no XML id. Without a stable id,
     * rotation and process death drop whatever the user typed.
     */
    private void bindEditorId(int fieldId) {
        if (editText == null || fieldId == View.NO_ID) {
            return;
        }
        int editorId = 0x01000000 | (fieldId & 0x00FFFFFF);
        if (editText.getId() != editorId) {
            editText.setId(editorId);
        }
    }

    private static final class MaskTransformation implements TransformationMethod {
        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return new Masked(source);
        }

        @Override
        public void onFocusChanged(
                View view,
                CharSequence sourceText,
                boolean focused,
                int direction,
                Rect previouslyFocusedRect
        ) {
        }
    }

    private static final class Masked implements CharSequence {
        private final CharSequence source;

        private Masked(CharSequence source) {
            this.source = source == null ? "" : source;
        }

        @Override
        public int length() {
            return source.length();
        }

        @Override
        public char charAt(int index) {
            return '\u2022';
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            char[] dots = new char[Math.max(0, end - start)];
            java.util.Arrays.fill(dots, '\u2022');
            return new String(dots);
        }
    }
}
