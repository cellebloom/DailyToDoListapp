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

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder> {

    private ArrayList<TodoItem> todoList;
    private Context context;
    private OnTaskChangedListener listener;

    public interface OnTaskChangedListener {
        void onTaskChanged();
    }

    // FIXED: Constructor name must match class name
    public TodoAdapter(ArrayList<TodoItem> todoList, Context context, OnTaskChangedListener listener) {
        this.todoList = todoList != null ? todoList : new ArrayList<>();
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            TodoItem item = todoList.get(position);

            holder.checkBox.setText(item.getTask());
            holder.checkBox.setChecked(item.isChecked());

            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    item.setChecked(isChecked);
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
                        TodoItem itemToDelete = todoList.get(pos);
                        todoList.remove(pos);
                        notifyItemRemoved(pos);
                        notifyItemRangeChanged(pos, todoList.size());

                        if (listener != null) {
                            listener.onTaskChanged();
                        }

                        Toast.makeText(context, "Task deleted: " + itemToDelete.getTask(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return todoList != null ? todoList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBoxTask);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}