package com.tmhnry.pingpoint.recyclerview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tmhnry.pingpoint.R;
import com.tmhnry.pingpoint.model.Attendance;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {
    private Context context;
    private List<Attendance> models;

    public AttendanceAdapter(Context context, List<Attendance> models) {
        this.context = context;
        this.models = models;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;
        view = inflater.inflate(R.layout.holder_attendance, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance model = models.get(position);
        holder.name.setText(model.entityName);
        holder.date.setText(model.date.toGMTString());
        holder.location.setText(model.location);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date, location, name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public ViewHolder(View view, int viewType) {
            super(view);
            location = view.findViewById(R.id.atten_location);
            name = view.findViewById(R.id.atten_name);
            date = view.findViewById(R.id.atten_date);
        }
    }
}
