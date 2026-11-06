package com.example.dailytodolist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private ArrayList<TaskWithDate> taskList;
    private ArrayList<TaskWithDate> masterList;
    private Context context;
    private OnTaskChangedListener listener;

    public interface OnTaskChangedListener {
        void onTaskChanged();
    }

    // 3-parameter constructor
    public TaskAdapter(ArrayList<TaskWithDate> taskList, Context context, OnTaskChangedListener listener) {
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.masterList = null;
        this.context = context;
        this.listener = listener;
    }

    // 4-parameter constructor with master list
    public TaskAdapter(ArrayList<TaskWithDate> taskList, ArrayList<TaskWithDate> masterList, Context context, OnTaskChangedListener listener) {
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.masterList = masterList;
        this.context = context;
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
        try {
            TaskWithDate task = taskList.get(position);

            holder.taskText.setText(task.getTaskName());
            holder.dateText.setText(task.getFormattedDate());
            holder.checkBox.setChecked(task.isCompleted());

            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    task.setCompleted(isChecked);
                    if (listener != null) {
                        listener.onTaskChanged();
                    }
                }
            });

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        TaskWithDate taskToDelete = taskList.get(pos);

                        // Remove from displayed list
                        taskList.remove(pos);

                        // Remove from master list if it exists
                        if (masterList != null) {
                            masterList.remove(taskToDelete);
                        }

                        notifyItemRemoved(pos);
                        notifyItemRangeChanged(pos, taskList.size());

                        // Notify listener to save changes
                        if (listener != null) {
                            listener.onTaskChanged();
                        }

                        Toast.makeText(context, "Task deleted: " + taskToDelete.getTaskName(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView taskText;
        TextView dateText;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBoxTask);
            taskText = itemView.findViewById(R.id.taskText);
            dateText = itemView.findViewById(R.id.dateText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}