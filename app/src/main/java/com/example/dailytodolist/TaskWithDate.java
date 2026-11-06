package com.example.dailytodolist;

public class TaskWithDate {
    private String taskName;
    private int year;
    private int month;
    private int day;
    private boolean isChecked;

    // Constructor
    public TaskWithDate(String taskName, int year, int month, int day) {
        this.taskName = taskName;
        this.year = year;
        this.month = month;
        this.day = day;
        this.isChecked = false;
    }

    // Getters
    public String getTaskName() {
        return taskName;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public boolean isChecked() {
        return isChecked;
    }

    // Setters
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}