package com.example.taskmatee;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvUpcomingTasks;
    private FloatingActionButton fabAdd;
    private Button btnShowAll, btnFilterHigh, btnFilterMedium;

    private ArrayList<Task> allTasksList;
    private ArrayList<Task> displayedTasksList;
    private TaskAdapter adapter;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        rvUpcomingTasks = findViewById(R.id.rvUpcomingTasks);
        fabAdd = findViewById(R.id.fabAdd);
        btnShowAll = findViewById(R.id.btnShowAll);
        btnFilterHigh = findViewById(R.id.btnFilterHigh);
        btnFilterMedium = findViewById(R.id.btnFilterMedium);

        rvUpcomingTasks.setLayoutManager(new LinearLayoutManager(this));
        allTasksList = new ArrayList<>();
        displayedTasksList = new ArrayList<>();

        // *** FIXED: Correctly initialize and set the adapter on separate lines ***
        adapter = new TaskAdapter(this, displayedTasksList);
        rvUpcomingTasks.setAdapter(adapter);

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
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
        });
    }

    private void setupFirebaseListener() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasksList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Task task = dataSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setTaskId(dataSnapshot.getKey());
                        allTasksList.add(task);
                    }
                }
                sortTasksByDeadline(allTasksList);
                filterTasksByPriority(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MainActivity", "Failed to read database.", error.toException());
                Toast.makeText(MainActivity.this, "Failed to load tasks.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilterButtonListeners() {
        btnShowAll.setOnClickListener(v -> filterTasksByPriority(null));
        btnFilterHigh.setOnClickListener(v -> filterTasksByPriority("High"));
        btnFilterMedium.setOnClickListener(v -> filterTasksByPriority("Medium"));
    }

    private void filterTasksByPriority(String priority) {
        displayedTasksList.clear();
        if (priority == null) {
            displayedTasksList.addAll(allTasksList);
        } else {
            for (Task task : allTasksList) {
                if (task.getPriority() != null && task.getPriority().equalsIgnoreCase(priority)) {
                    displayedTasksList.add(task);
                }
            }
        }
        adapter.notifyDataSetChanged();
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
