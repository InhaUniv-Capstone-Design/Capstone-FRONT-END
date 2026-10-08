package com.example.yakyeon.ui.graph;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.yakyeon.R;
import com.example.yakyeon.data.model.DurInteraction;

public class InteractionDetailDialog extends DialogFragment {

    public static final String TAG = "interaction_detail";
    private static final String ARG_ID = "interaction_id";

    public static void open(FragmentManager manager, String interactionId) {
        if (manager.findFragmentByTag(TAG) != null || interactionId == null) {
            return;
        }
        InteractionDetailDialog dialog = new InteractionDetailDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ID, interactionId);
        dialog.setArguments(args);
        dialog.show(manager, TAG);
    }

    @Override
    public int getTheme() {
        return R.style.Theme_Yakyeon_ConsentDialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.dialog_interaction_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String interactionId = requireArguments().getString(ARG_ID);
        DurInteraction interaction = getActivity() instanceof DrugRelationGraphActivity
                ? ((DrugRelationGraphActivity) getActivity()).findInteraction(interactionId)
                : null;
        if (interaction == null) {
            dismiss();
            return;
        }
        ((TextView) view.findViewById(R.id.graph_detail_drug_a)).setText(interaction.getDrugAName());
        ((TextView) view.findViewById(R.id.graph_detail_drug_a_caption)).setText(interaction.getDrugACaption());
        ((TextView) view.findViewById(R.id.graph_detail_drug_b)).setText(interaction.getDrugBName());
        ((TextView) view.findViewById(R.id.graph_detail_drug_b_caption)).setText(interaction.getDrugBCaption());
        ((TextView) view.findViewById(R.id.graph_detail_description)).setText(interaction.getInteractionDescription());
        view.findViewById(R.id.graph_detail_confirm).setOnClickListener(button -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
