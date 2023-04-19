package com.tmhnry.pingpoint.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.tmhnry.pingpoint.MainActivity;
import com.tmhnry.pingpoint.R;
import com.tmhnry.pingpoint.databinding.FragmentAccountBinding;
import com.tmhnry.pingpoint.model.Company;
import com.tmhnry.pingpoint.model.User;

public class AccountFragment extends Fragment {
    public static final String TAG = "org.tensorflow.lite.examples.transfer.fragment.account";
    FragmentAccountBinding binding;
    OnCompanySetup listener;

    public interface OnCompanySetup {
        void onCompanySetup();
    }

    public AccountFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (OnCompanySetup) context;
    }

    public static AccountFragment Builder() {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        binding.setUser(User.getUser());
        if (Company.getModels().size() != 0) {
            updateCompanyButton();
        } else {
            binding.btnCompany.setOnClickListener(view -> {
                listener.onCompanySetup();
            });
        }
        binding.navBack.setOnClickListener(view -> {
            ((MainActivity) getActivity()).navigate(R.id.nav_home);
        });


        binding.btnLogout.setOnClickListener(view -> {
            ((MainActivity) getActivity()).openLogOutDialog();
        });

        return binding.getRoot();
    }

    public void updateCompanyButton() {
        binding.companyButtonText.setText("Company Details");
        binding.btnCompany.setOnClickListener(view -> {
            Toast.makeText(getContext(), "To company activity", Toast.LENGTH_SHORT).show();
        });
    }
}