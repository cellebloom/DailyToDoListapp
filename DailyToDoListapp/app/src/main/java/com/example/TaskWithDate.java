package com.example.dailytodolist;

import java.io.Serializable;
import java.util.Calendar;

public class TaskWithDate implements Serializable {
    private String taskName;
    private Calendar dueDate;
    private boolean isCompleted;
    private int year, month, day;

    public TaskWithDate(String taskName, int year, int month, int day) {
        this.taskName = taskName;
        this.year = year;
        this.month = month;
        this.day = day;
        this.isCompleted = false;

        this.dueDate = Calendar.getInstance();
        this.dueDate.set(year, month, day);
    }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public Calendar getDueDate() { return dueDate; }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getDay() { return day; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; } // This is the correct method name

    public String getFormattedDate() {
        return year + "-" + (month + 1) + "-" + day;
    }
}