package com.example.dailytodolist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TodoAdapter adapter;
    private ArrayList<TodoItem> todoList;
    private EditText editTextTask;
    private Button buttonAdd;
    private TextView textDate, homeTab, calendarTab, profileTab;
    private ImageButton settingsButton;

    private static final String PREF_NAME = "TodoAppPrefs";
    private static final String TASKS_KEY = "tasks_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load theme
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(SettingsActivity.THEME_KEY, "light");

        switch (theme) {
            case "pink":
                setTheme(R.style.Theme_DailyToDoList_Pink);
                break;
            case "dark":
                setTheme(R.style.Theme_DailyToDoList_Dark);
                break;
            default:
                setTheme(R.style.Theme_DailyToDoList_Light);
                break;
        }

        setContentView(R.layout.activity_main);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        editTextTask = findViewById(R.id.editTextTask);
        buttonAdd = findViewById(R.id.buttonAdd);
        textDate = findViewById(R.id.textDate);
        homeTab = findViewById(R.id.homeTab);
        calendarTab = findViewById(R.id.calendarTab);
        profileTab = findViewById(R.id.profileTab);
        settingsButton = findViewById(R.id.settingsButton);

        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        textDate.setText(currentDate);

        // Load saved tasks
        todoList = loadTasks();

        // If no saved tasks, add sample tasks
        if (todoList.isEmpty()) {
            todoList.add(new TodoItem("Study for 1 hour", false));
            todoList.add(new TodoItem("Do maths Homework", false));
            todoList.add(new TodoItem("Wash the dish", false));
        }

        // Setup RecyclerView with TodoAdapter
        adapter = new TodoAdapter(todoList, this, new TodoAdapter.OnTaskChangedListener() {
            @Override
            public void onTaskChanged() {
                saveTasks();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Add task button
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskText = editTextTask.getText().toString().trim();
                if (!taskText.isEmpty()) {
                    TodoItem newTask = new TodoItem(taskText, false);
                    todoList.add(newTask);
                    adapter.notifyItemInserted(todoList.size() - 1);
                    saveTasks();
                    editTextTask.setText("");
                    recyclerView.smoothScrollToPosition(todoList.size() - 1);
                    Toast.makeText(MainActivity.this, "Task added!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a task", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Settings button
        if (settingsButton != null) {
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            });
        }

        // Navigation tabs
        setupNavigationListeners();
    }

    private void setupNavigationListeners() {
        homeTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeTab.setTextColor(getColor(R.color.gradient_start));
                calendarTab.setTextColor(getColor(R.color.text_secondary));
                profileTab.setTextColor(getColor(R.color.text_secondary));

                homeTab.setBackgroundResource(R.drawable.tab_background_active);
                calendarTab.setBackgroundResource(R.drawable.tab_background_inactive);
                profileTab.setBackgroundResource(R.drawable.tab_background_inactive);
            }
        });

        calendarTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendarTab.setTextColor(getColor(R.color.gradient_start));
                homeTab.setTextColor(getColor(R.color.text_secondary));
                profileTab.setTextColor(getColor(R.color.text_secondary));

                calendarTab.setBackgroundResource(R.drawable.tab_background_active);
                homeTab.setBackgroundResource(R.drawable.tab_background_inactive);
                profileTab.setBackgroundResource(R.drawable.tab_background_inactive);

                Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
                startActivity(intent);
            }
        });

        profileTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileTab.setTextColor(getColor(R.color.gradient_start));
                homeTab.setTextColor(getColor(R.color.text_secondary));
                calendarTab.setTextColor(getColor(R.color.text_secondary));

                profileTab.setBackgroundResource(R.drawable.tab_background_active);
                homeTab.setBackgroundResource(R.drawable.tab_background_inactive);
                calendarTab.setBackgroundResource(R.drawable.tab_background_inactive);

                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            }
        });
    }

    private void saveTasks() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(todoList);
        editor.putString(TASKS_KEY, json);
        editor.apply();
    }

    private ArrayList<TodoItem> loadTasks() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(TASKS_KEY, null);
        Type type = new TypeToken<ArrayList<TodoItem>>() {}.getType();
        ArrayList<TodoItem> tasks = gson.fromJson(json, type);

        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return tasks;
    }
}