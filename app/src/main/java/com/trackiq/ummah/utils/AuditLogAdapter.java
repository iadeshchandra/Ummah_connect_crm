package com.trackiq.ummah.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.trackiq.ummah.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AuditLogAdapter - RecyclerView adapter for audit logs
 */
public class AuditLogAdapter extends RecyclerView.Adapter<AuditLogAdapter.AuditLogViewHolder> {

    private List<Map<String, Object>> logs;
    private SimpleDateFormat dateFormat;

    public AuditLogAdapter(List<Map<String, Object>> logs) {
        this.logs = logs;
        this.dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);
    }

    @NonNull
    @Override
    public AuditLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audit_log, parent, false);
        return new AuditLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuditLogViewHolder holder, int position) {
        Map<String, Object> log = logs.get(position);
        holder.bind(log, dateFormat);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class AuditLogViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTimestamp;
        private TextView tvAction;
        private TextView tvUser;
        private TextView tvDetails;

        public AuditLogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvAction = itemView.findViewById(R.id.tvAction);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvDetails = itemView.findViewById(R.id.tvDetails);
        }

        public void bind(Map<String, Object> log, SimpleDateFormat dateFormat) {
            Long timestamp = (Long) log.get("timestamp");
            String action = (String) log.get("action");
            String userType = (String) log.get("user_type");
            String userId = (String) log.get("user_id");
            String details = (String) log.get("details");

            tvTimestamp.setText(timestamp != null ? 
                    dateFormat.format(new Date(timestamp)) : "Unknown");
            
            tvAction.setText(action != null ? action : "UNKNOWN");
            
            // Color code actions
            int actionColor;
            if (action != null) {
                switch (action) {
                    case AuditLogger.ACTION_LOGIN:
                        actionColor = android.R.color.holo_green_dark;
                        break;
                    case AuditLogger.ACTION_LOGOUT:
                        actionColor = android.R.color.holo_orange_dark;
                        break;
                    case AuditLogger.ACTION_MEMBER_DELETE:
                    case AuditLogger.ACTION_TRANSACTION_ADD:
                        actionColor = android.R.color.holo_blue_dark;
                        break;
                    default:
                        actionColor = android.R.color.darker_gray;
                }
                tvAction.setTextColor(itemView.getContext().getColor(actionColor));
            }
            
            tvUser.setText((userType != null ? userType : "unknown") + 
                    " | " + (userId != null ? userId : "unknown"));
            tvDetails.setText(details != null ? details : "");
        }
    }
}
