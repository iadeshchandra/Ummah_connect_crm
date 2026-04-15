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
import com.trackiq.ummah.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * TransactionAdapter - RecyclerView adapter for ledger transactions
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private OnTransactionClickListener listener;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
        this.transactions = transactions;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction, listener, currencyFormat);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView tvType;
        private TextView tvDescription;
        private TextView tvAmount;
        private TextView tvDate;
        private TextView tvPerson;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvType = itemView.findViewById(R.id.tvType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPerson = itemView.findViewById(R.id.tvPerson);
        }

        public void bind(Transaction transaction, OnTransactionClickListener listener, 
                        NumberFormat currencyFormat) {
            // Type badge
            tvType.setText(transaction.getTypeDisplay());
            
            int typeColor;
            String type = transaction.getType();
            if (Transaction.TYPE_SADAQAH.equals(type)) {
                typeColor = R.color.sadaqah_green;
            } else if (Transaction.TYPE_ZAKAT.equals(type)) {
                typeColor = R.color.zakat_gold;
            } else if (Transaction.TYPE_MAINTENANCE.equals(type) || 
                       Transaction.TYPE_IFTAR.equals(type)) {
                typeColor = R.color.expense_outflow;
            } else {
                typeColor = R.color.islamic_blue;
            }
            
            tvType.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), typeColor));
            tvType.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));

            // Description
            tvDescription.setText(transaction.getDescription());

            // Amount with color
            boolean isIncome = Transaction.CATEGORY_INCOME.equals(transaction.getCategory());
            tvAmount.setText((isIncome ? "+" : "-") + currencyFormat.format(transaction.getAmount()));
            tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    isIncome ? R.color.donation_income : R.color.expense_outflow));

            // Date
            tvDate.setText(transaction.getDate());

            // Person (if donation)
            if (transaction.getPersonName() != null) {
                tvPerson.setVisibility(View.VISIBLE);
                tvPerson.setText("From: " + transaction.getPersonName());
            } else if (transaction.getPersonId() != null) {
                tvPerson.setVisibility(View.VISIBLE);
                tvPerson.setText("ID: " + transaction.getPersonId());
            } else {
                tvPerson.setVisibility(View.GONE);
            }

            // Click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            });
        }
    }
}
