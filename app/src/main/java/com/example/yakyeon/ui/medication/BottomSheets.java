package com.example.yakyeon.ui.medication;

import android.app.Dialog;
import android.graphics.Color;
import android.widget.FrameLayout;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

final class BottomSheets {

    private BottomSheets() {
    }

    static void expand(DialogFragment fragment) {
        Dialog dialog = fragment.getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) {
            return;
        }
        sheet.setBackgroundColor(Color.TRANSPARENT);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
