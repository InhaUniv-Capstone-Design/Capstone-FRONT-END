package com.example.yakyeon.ui.medication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yakyeon.R;
import com.example.yakyeon.data.model.Medication;

import java.util.ArrayList;
import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.Holder> {

    public interface OnMedicationLongClickListener {
        void onMedicationLongClick(Medication medication);
    }

    private final List<Medication> medications = new ArrayList<>();
    private OnMedicationLongClickListener longClickListener;

    public MedicationAdapter() {
    }

    public void setOnMedicationLongClickListener(OnMedicationLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void submit(List<Medication> items) {
        medications.clear();
        medications.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medication, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Medication medication = medications.get(position);
        boolean ended = medication.getClassification() == Medication.Classification.ENDED;
        boolean pill = medication.getForm() == Medication.Form.PILL;

        holder.name.setText(medication.getName());
        holder.frequency.setText(medication.getFrequency());
        holder.name.setTextColor(holder.itemView.getContext().getColor(
                ended ? R.color.muted : R.color.ink
        ));
        holder.frequency.setTextColor(holder.itemView.getContext().getColor(
                ended ? R.color.ended_hint : R.color.muted
        ));
        holder.iconTile.setBackgroundResource(
                ended ? R.drawable.bg_icon_tile_muted : R.drawable.bg_icon_tile
        );
        holder.icon.setImageResource(iconFor(ended, pill));
        holder.chevron.setImageResource(
                ended ? R.drawable.ic_chevron_right_muted : R.drawable.ic_chevron_right
        );
        holder.divider.setVisibility(position == medications.size() - 1 ? View.GONE : View.VISIBLE);
        holder.row.setOnLongClickListener(view -> {
            if (longClickListener == null) {
                return false;
            }
            longClickListener.onMedicationLongClick(medication);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    private static int iconFor(boolean ended, boolean pill) {
        if (ended) {
            return pill ? R.drawable.ic_pill_muted : R.drawable.ic_tablet;
        }
        return pill ? R.drawable.ic_pill : R.drawable.ic_tablet;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View row;
        final FrameLayout iconTile;
        final ImageView icon;
        final TextView name;
        final TextView frequency;
        final ImageView chevron;
        final View divider;

        Holder(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.medication_item);
            iconTile = itemView.findViewById(R.id.medication_icon_tile);
            icon = itemView.findViewById(R.id.medication_icon);
            name = itemView.findViewById(R.id.medication_name);
            frequency = itemView.findViewById(R.id.medication_frequency);
            chevron = itemView.findViewById(R.id.medication_chevron);
            divider = itemView.findViewById(R.id.medication_divider);
        }
    }
}
