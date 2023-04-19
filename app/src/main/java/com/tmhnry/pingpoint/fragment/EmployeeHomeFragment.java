package com.tmhnry.pingpoint.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tmhnry.pingpoint.CameraActivity;
import com.tmhnry.pingpoint.VisionModelProvider;
import com.tmhnry.pingpoint.databinding.FragmentEmployeeHomeBinding;
import com.tmhnry.pingpoint.model.Attendance;
import com.tmhnry.pingpoint.recyclerview.AttendanceAdapter;

import java.util.ArrayList;
import java.util.List;

public class EmployeeHomeFragment extends Fragment {
    public static final String TAG = "org.tensorflow.lite.examples.transfer.fragment.employeehome";
    FragmentEmployeeHomeBinding binding;
    RecyclerView recyclerView;
    AttendanceAdapter adapter;
    List<Attendance> models;

    public EmployeeHomeFragment() {
    }

    public static EmployeeHomeFragment Builder() {
        EmployeeHomeFragment fragment = new EmployeeHomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        models = new ArrayList<>(Attendance.getModels().values());
        adapter = new AttendanceAdapter(context, models);
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
        binding = FragmentEmployeeHomeBinding.inflate(inflater, container, false);
        recyclerView = binding.recyclerview;
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.add.setOnClickListener(view -> {
            if (!VisionModelProvider.isReady()) {
                Toast.makeText(getContext(), "Please wait for a while...", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(getActivity(), CameraActivity.class));
            }
        });
        return binding.getRoot();
    }

    public void updateAttendances(){
        models.clear();
        models.addAll(Attendance.getModels().values());
        adapter.notifyDataSetChanged();
    }
}