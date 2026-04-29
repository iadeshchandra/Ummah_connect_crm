package com.trackiq.ummah.utils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

// Note: You will need to create IncomeFragment and ExpenseFragment separately
// if they do not exist yet. This adapter simply holds them.
import com.trackiq.ummah.ui.ledger.IncomeFragment;
import com.trackiq.ummah.ui.ledger.ExpenseFragment;

/**
 * LedgerPagerAdapter - Manages tabs for Donations (Income) and Expenses
 */
public class LedgerPagerAdapter extends FragmentStateAdapter {

    public LedgerPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new IncomeFragment();
        } else {
            return new ExpenseFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
