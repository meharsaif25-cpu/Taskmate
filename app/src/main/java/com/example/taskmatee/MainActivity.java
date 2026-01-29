package com.example.taskmatee;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvUpcomingTasks, rvCompletedTasks;
    private FloatingActionButton fabAdd, fabGemini;
    private Button btnShowAll, btnFilterHigh, btnFilterMedium;
    private EditText etSearch;
    private TextView tvFocusCount;

    private ArrayList<Task> allTasksList;
    private ArrayList<Task> upcomingTasksList, completedTasksList;
    private TaskAdapter upcomingAdapter, completedAdapter;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String currentPriorityFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        rvUpcomingTasks = findViewById(R.id.rvUpcomingTasks);
        rvCompletedTasks = findViewById(R.id.rvCompletedTasks);
        fabAdd = findViewById(R.id.fabAdd);
        fabGemini = findViewById(R.id.fabGemini);
        btnShowAll = findViewById(R.id.btnShowAll);
        btnFilterHigh = findViewById(R.id.btnFilterHigh);
        btnFilterMedium = findViewById(R.id.btnFilterMedium);
        etSearch = findViewById(R.id.etSearch);
        tvFocusCount = findViewById(R.id.tvFocusCount);

        rvUpcomingTasks.setLayoutManager(new LinearLayoutManager(this));
        rvCompletedTasks.setLayoutManager(new LinearLayoutManager(this));

        allTasksList = new ArrayList<>();
        upcomingTasksList = new ArrayList<>();
        completedTasksList = new ArrayList<>();

        upcomingAdapter = new TaskAdapter(this, upcomingTasksList);
        completedAdapter = new TaskAdapter(this, completedTasksList);

        rvUpcomingTasks.setAdapter(upcomingAdapter);
        rvCompletedTasks.setAdapter(completedAdapter);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String userId = currentUser.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Tasks").child(userId);
        setupFirebaseListener();

        setupFilterButtonListeners();
        setupSearchListener();

        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
        });

        fabGemini.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GeminiChatActivity.class));
        });
    }

    private void setupFirebaseListener() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasksList.clear();
                int todayTaskCount = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                String todayDateStr = sdf.format(Calendar.getInstance().getTime());

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Task task = dataSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setTaskId(dataSnapshot.getKey());
                        allTasksList.add(task);
                        
                        if (!task.isCompleted() && todayDateStr.equals(task.getDeadline())) {
                            todayTaskCount++;
                        }
                    }
                }
                
                tvFocusCount.setText("You have " + todayTaskCount + " tasks today. Stay organized!");
                sortTasksByDeadline(allTasksList);
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to read database.", error.toException());
                Toast.makeText(MainActivity.this, "Failed to load tasks.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilterButtonListeners() {
        btnShowAll.setOnClickListener(v -> {
            currentPriorityFilter = null;
            applyFilters();
        });
        btnFilterHigh.setOnClickListener(v -> {
            currentPriorityFilter = "High";
            applyFilters();
        });
        btnFilterMedium.setOnClickListener(v -> {
            currentPriorityFilter = "Medium";
            applyFilters();
        });
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        String searchQuery = etSearch.getText().toString().toLowerCase().trim();
        
        upcomingTasksList.clear();
        completedTasksList.clear();

        for (Task task : allTasksList) {
            boolean matchesPriority = (currentPriorityFilter == null || (task.getPriority() != null && task.getPriority().equalsIgnoreCase(currentPriorityFilter)));
            boolean matchesSearch = (searchQuery.isEmpty() || (task.getTaskName() != null && task.getTaskName().toLowerCase().contains(searchQuery)));

            if (matchesPriority && matchesSearch) {
                if (task.isCompleted()) {
                    completedTasksList.add(task);
                } else {
                    upcomingTasksList.add(task);
                }
            }
        }
        
        upcomingAdapter.notifyDataSetChanged();
        completedAdapter.notifyDataSetChanged();
    }

    private void sortTasksByDeadline(List<Task> tasks) {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        Collections.sort(tasks, (task1, task2) -> {
            try {
                Date date1 = sdf.parse(task1.getDeadline());
                Date date2 = sdf.parse(task2.getDeadline());
                return date1.compareTo(date2);
            } catch (ParseException | NullPointerException e) {
                return 0;
            }
        });
    }
}
