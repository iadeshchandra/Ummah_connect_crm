package com.trackiq.ummah.ui.ledger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.trackiq.ummah.R;

public class IncomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // You will need to create fragment_ledger_list.xml layout for this
        return inflater.inflate(R.layout.fragment_ledger_list, container, false);
    }
}
