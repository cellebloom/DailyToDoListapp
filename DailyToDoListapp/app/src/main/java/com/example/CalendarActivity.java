package com.example.dailytodolist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private TextView backButton;  // Changed from Button to TextView
    private TextView monthYearText;
    private LinearLayout calendarGrid;
    private TextView selectedDateText;
    private EditText taskInput;
    private TextView addTaskButton;  // Changed from Button to TextView
    private TextView prevMonthButton; // Changed from Button to TextView
    private TextView nextMonthButton; // Changed from Button to TextView
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private ArrayList<TaskWithDate> allTasks;
    private ArrayList<TaskWithDate> tasksForSelectedDate;

    private Calendar currentCalendar;
    private int selectedYear, selectedMonth, selectedDay;
    private int todayYear, todayMonth, todayDay;

    private static final String TASKS_PREFS = "TasksWithDatePrefs";
    private static final String TASKS_KEY = "tasks_with_date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_dynamic);

        // Initialize views - now using TextViews
        backButton = findViewById(R.id.backButton);
        monthYearText = findViewById(R.id.monthYearText);
        calendarGrid = findViewById(R.id.calendarGrid);
        selectedDateText = findViewById(R.id.selectedDateText);
        taskInput = findViewById(R.id.taskInput);
        addTaskButton = findViewById(R.id.addTaskButton);
        prevMonthButton = findViewById(R.id.prevMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);

        // Get today's actual date
        Calendar today = Calendar.getInstance();
        todayYear = today.get(Calendar.YEAR);
        todayMonth = today.get(Calendar.MONTH);
        todayDay = today.get(Calendar.DAY_OF_MONTH);

        // Initialize calendar to March 2026
        currentCalendar = Calendar.getInstance();
        currentCalendar.set(2026, Calendar.MARCH, 1);

        selectedYear = 2026;
        selectedMonth = Calendar.MARCH;
        selectedDay = 21;

        // Load tasks
        allTasks = loadTasks();
        tasksForSelectedDate = new ArrayList<>();

        // Setup RecyclerView
        taskAdapter = new TaskAdapter(tasksForSelectedDate, allTasks, this, new TaskAdapter.OnTaskChangedListener() {
            @Override
            public void onTaskChanged() {
                saveTasks();
                updateTasksForSelectedDate();
                generateCalendar();
            }
        });
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);

        // Generate calendar
        generateCalendar();

        // Back button click listener
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Add task button click listener
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskName = taskInput.getText().toString().trim();
                if (!taskName.isEmpty()) {
                    TaskWithDate newTask = new TaskWithDate(taskName, selectedYear, selectedMonth, selectedDay);
                    allTasks.add(newTask);
                    saveTasks();
                    updateTasksForSelectedDate();
                    generateCalendar();
                    taskInput.setText("");
                    Toast.makeText(CalendarActivity.this, "Task added!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CalendarActivity.this, "Please enter a task", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Previous month button click listener
        prevMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, -1);
                selectedYear = currentCalendar.get(Calendar.YEAR);
                selectedMonth = currentCalendar.get(Calendar.MONTH);
                selectedDay = 1;
                generateCalendar();
                updateTasksForSelectedDate();
            }
        });

        // Next month button click listener
        nextMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, 1);
                selectedYear = currentCalendar.get(Calendar.YEAR);
                selectedMonth = currentCalendar.get(Calendar.MONTH);
                selectedDay = 1;
                generateCalendar();
                updateTasksForSelectedDate();
            }
        });
    }

    private void generateCalendar() {
        calendarGrid.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        monthYearText.setText(sdf.format(currentCalendar.getTime()));

        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        int startDay = (firstDayOfWeek == Calendar.SUNDAY) ? 0 : firstDayOfWeek - 1;

        for (int week = 0; week < 6; week++) {
            LinearLayout weekRow = new LinearLayout(this);
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            weekRow.setOrientation(LinearLayout.HORIZONTAL);

            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                int dayNumber = week * 7 + dayOfWeek - startDay + 1;

                TextView dateCell = new TextView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 100, 1);
                params.setMargins(2, 2, 2, 2);
                dateCell.setLayoutParams(params);
                dateCell.setGravity(Gravity.CENTER);
                dateCell.setTextSize(16);
                dateCell.setTypeface(null, Typeface.BOLD);

                if (dayNumber >= 1 && dayNumber <= daysInMonth) {
                    dateCell.setText(String.valueOf(dayNumber));

                    boolean hasTasks = hasTasksOnDate(year, month, dayNumber);

                    // TODAY'S DATE - Green
                    if (year == todayYear && month == todayMonth && dayNumber == todayDay) {
                        dateCell.setTextColor(Color.WHITE);
                        dateCell.setBackgroundColor(Color.parseColor("#4CAF50"));
                    }
                    // Selected date - Purple
                    else if (year == selectedYear && month == selectedMonth && dayNumber == selectedDay) {
                        dateCell.setTextColor(Color.WHITE);
                        dateCell.setBackgroundColor(Color.parseColor("#9C27B0"));
                    }
                    // Dates with tasks - Blue text
                    else if (hasTasks) {
                        dateCell.setTextColor(Color.parseColor("#2196F3"));
                        dateCell.setBackgroundColor(Color.TRANSPARENT);
                    }
                    // Normal dates - White text
                    else {
                        dateCell.setTextColor(Color.WHITE);
                        dateCell.setBackgroundColor(Color.TRANSPARENT);
                    }

                    final int fYear = year;
                    final int fMonth = month;
                    final int fDay = dayNumber;

                    dateCell.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedYear = fYear;
                            selectedMonth = fMonth;
                            selectedDay = fDay;
                            selectedDateText.setText("📅 Selected: " + fYear + "-" + (fMonth+1) + "-" + fDay);
                            updateTasksForSelectedDate();
                            generateCalendar();
                        }
                    });
                } else {
                    dateCell.setText("");
                    dateCell.setBackgroundColor(Color.TRANSPARENT);
                }

                weekRow.addView(dateCell);
            }
            calendarGrid.addView(weekRow);
        }

        selectedDateText.setText("📅 Selected: " + selectedYear + "-" + (selectedMonth+1) + "-" + selectedDay);
        updateTasksForSelectedDate();
    }

    private boolean hasTasksOnDate(int year, int month, int day) {
        if (allTasks == null) return false;
        for (TaskWithDate task : allTasks) {
            if (task != null && task.getYear() == year && task.getMonth() == month && task.getDay() == day) {
                return true;
            }
        }
        return false;
    }

    private void updateTasksForSelectedDate() {
        tasksForSelectedDate.clear();
        if (allTasks != null) {
            for (TaskWithDate task : allTasks) {
                if (task != null && task.getYear() == selectedYear &&
                        task.getMonth() == selectedMonth && task.getDay() == selectedDay) {
                    tasksForSelectedDate.add(task);
                }
            }
        }
        if (taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
        }
    }

    private void saveTasks() {
        SharedPreferences sharedPreferences = getSharedPreferences(TASKS_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(allTasks);
        editor.putString(TASKS_KEY, json);
        editor.apply();
    }

    private ArrayList<TaskWithDate> loadTasks() {
        SharedPreferences sharedPreferences = getSharedPreferences(TASKS_PREFS, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(TASKS_KEY, null);
        Type type = new TypeToken<ArrayList<TaskWithDate>>() {}.getType();
        ArrayList<TaskWithDate> tasks = gson.fromJson(json, type);

        if (tasks == null) {
            tasks = new ArrayList<>();
            tasks.add(new TaskWithDate("make cookies", 2026, 2, 28));
            tasks.add(new TaskWithDate("go hang-out", 2026, 2, 21));
            tasks.add(new TaskWithDate("go celebrate mom's birthday", 2026, 2, 21));
        }
        return tasks;
    }
}