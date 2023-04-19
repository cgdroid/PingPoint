package com.tmhnry.pingpoint.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tmhnry.pingpoint.databinding.FragmentAdminHomeBinding;
import com.tmhnry.pingpoint.model.Attendance;
import com.tmhnry.pingpoint.recyclerview.AttendanceAdapter;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeFragment extends Fragment {
    public static final String TAG = "org.tensorflow.lite.examples.transfer.fragment.adminhome";
    AttendanceAdapter adapter;
    List<Attendance> models;
    RecyclerView recyclerView;
    FragmentAdminHomeBinding binding;

    public AdminHomeFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        models = new ArrayList<>(Attendance.getModels().values());
        this.adapter = new AttendanceAdapter(context, models);
    }

    public static AdminHomeFragment Builder() {
        AdminHomeFragment fragment = new AdminHomeFragment();
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
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false);
        this.recyclerView = binding.recyclerview;
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return binding.getRoot();
    }

    public void updateAttendances(){
        models.clear();
        models.addAll(Attendance.getModels().values());
        adapter.notifyDataSetChanged();
    }
}