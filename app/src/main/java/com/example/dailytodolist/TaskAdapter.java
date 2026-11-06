package com.example.dailytodolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private ArrayList<TaskWithDate> taskList;
    private OnTaskChangedListener listener;

    public interface OnTaskChangedListener {
        void onTaskChanged();
        void onTaskDeleted(int position);
    }

    public TaskAdapter(ArrayList<TaskWithDate> taskList, OnTaskChangedListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_with_date, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskWithDate task = taskList.get(position);

        holder.checkBoxTask.setText(task.getTaskName());
        holder.checkBoxTask.setChecked(task.isChecked());

        if (task.isChecked()) {
            holder.checkBoxTask.setPaintFlags(holder.checkBoxTask.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.checkBoxTask.setTextColor(0xFFB0B0B0);
        } else {
            holder.checkBoxTask.setPaintFlags(holder.checkBoxTask.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        holder.checkBoxTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setChecked(isChecked);
            if (listener != null) listener.onTaskChanged();
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) listener.onTaskDeleted(position);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateList(ArrayList<TaskWithDate> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBoxTask;
        TextView deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxTask = itemView.findViewById(R.id.checkBoxTask);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
