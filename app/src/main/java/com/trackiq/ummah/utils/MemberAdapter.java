package com.trackiq.ummah.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.trackiq.ummah.R;
import com.trackiq.ummah.model.Member;

import java.util.List;

/**
 * MemberAdapter - RecyclerView adapter for member list
 */
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Member> members;
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(Member member);
    }

    public MemberAdapter(List<Member> members, OnMemberClickListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = members.get(position);
        holder.bind(member, listener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView tvId;
        private TextView tvName;
        private TextView tvPhone;
        private TextView tvStatus;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvId = itemView.findViewById(R.id.tvId);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        public void bind(Member member, OnMemberClickListener listener) {
            // Set ID with MBR- prefix
            tvId.setText(member.getDisplayId());
            
            // Set name
            tvName.setText(member.getName());
            
            // Set phone
            tvPhone.setText(member.getPhone() != null ? member.getPhone() : "No phone");
            
            // Set status with color
            String status = member.getStatus() != null ? member.getStatus() : "active";
            tvStatus.setText(status.toUpperCase());
            
            int statusColor;
            switch (status.toLowerCase()) {
                case "active":
                    statusColor = R.color.status_active;
                    break;
                case "inactive":
                    statusColor = R.color.status_inactive;
                    break;
                case "vip":
                    statusColor = R.color.status_vip;
                    break;
                default:
                    statusColor = R.color.status_active;
            }
            
            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColor));
            
            // Click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMemberClick(member);
                }
            });
        }
    }
}
