package com.trackiq.ummah.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.trackiq.ummah.R;

import java.util.List;
import java.util.Map;

/**
 * PollAdapter - Displays Shura Polls and handles voting UI
 */
public class PollAdapter extends RecyclerView.Adapter<PollAdapter.ViewHolder> {

    private final List<Map<String, Object>> polls;
    private final OnVoteClickListener listener;

    public interface OnVoteClickListener {
        void onVoteClick(String pollId, int optionIndex);
    }

    public PollAdapter(List<Map<String, Object>> polls, OnVoteClickListener listener) {
        this.polls = polls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // You will need an item_poll.xml layout with a tvQuestion and a RadioGroup (rgOptions)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poll, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> poll = polls.get(position);
        String pollId = (String) poll.get("id");
        String question = (String) poll.get("question");
        List<String> options = (List<String>) poll.get("options");

        holder.tvQuestion.setText(question);
        holder.rgOptions.removeAllViews();

        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                RadioButton rb = new RadioButton(holder.itemView.getContext());
                rb.setText(options.get(i));
                rb.setId(i); // Use index as ID for easy retrieval
                holder.rgOptions.addView(rb);
            }
        }

        holder.rgOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (listener != null) {
                listener.onVoteClick(pollId, checkedId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return polls.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion;
        RadioGroup rgOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match your item_poll.xml file
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            rgOptions = itemView.findViewById(R.id.rgOptions);
        }
    }
}
