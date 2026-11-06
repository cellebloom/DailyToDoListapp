package com.example.dailytodolist;

import java.io.Serializable;

public class TodoItem implements Serializable {
    private String task;
    private boolean isChecked;

    public TodoItem(String task, boolean isChecked) {
        this.task = task;
        this.isChecked = isChecked;
    }

    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }
}