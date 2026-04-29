package com.trackiq.ummah.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.trackiq.ummah.R;

import java.util.List;

/**
 * OrphanedIdAdapter - Displays orphaned transactions for rescue
 */
public class OrphanedIdAdapter extends RecyclerView.Adapter<OrphanedIdAdapter.ViewHolder> {

    private final List<String> orphanedIds;
    private final OnRescueClickListener listener;

    public interface OnRescueClickListener {
        void onRescueClick(String id);
    }

    public OrphanedIdAdapter(List<String> orphanedIds, OnRescueClickListener listener) {
        this.orphanedIds = orphanedIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // You will need an item_orphaned_id.xml layout with a textview (tvId) and button (btnRescue)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_orphaned_id, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String id = orphanedIds.get(position);
        holder.tvId.setText(id);
        
        holder.btnRescue.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRescueClick(id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orphanedIds.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId;
        Button btnRescue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match your item_orphaned_id.xml file
            tvId = itemView.findViewById(R.id.tvId);
            btnRescue = itemView.findViewById(R.id.btnRescue);
        }
    }
}
