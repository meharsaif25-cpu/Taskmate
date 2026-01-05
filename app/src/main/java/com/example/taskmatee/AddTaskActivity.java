package com.example.taskmatee;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View; // Import the View class
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;

public class AddTaskActivity extends AppCompatActivity {

    private EditText etTaskName;
    private TextView tvDeadline;
    private RadioGroup rgPriority;
    private Button btnSaveTask;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Initialize UI components
        etTaskName = findViewById(R.id.etTaskName);
        tvDeadline = findViewById(R.id.tvDeadline);
        rgPriority = findViewById(R.id.rgPriority);
        btnSaveTask = findViewById(R.id.btnSaveTask);

        // *** ADDED: Explicitly hide the delete button on this screen ***
        findViewById(R.id.btnDeleteTask).setVisibility(View.GONE);

        // Initialize Firebase Auth to get the current user
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Security check: If no user is logged in, they can't add a task.
        if (currentUser == null) {
            Toast.makeText(this, "Authentication required to add a task.", Toast.LENGTH_LONG).show();
            finish();
            return; // Stop further execution
        }

        String userId = currentUser.getUid();

        // Set the database reference to the specific user's "Tasks" node
        databaseReference = FirebaseDatabase.getInstance().getReference("Tasks").child(userId);

        // Setup UI event listeners
        tvDeadline.setOnClickListener(v -> showDatePickerDialog());
        btnSaveTask.setOnClickListener(v -> saveTask());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // Format the selected date and update the TextView
                    String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    tvDeadline.setText(selectedDate);
                },
                year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void saveTask() {
        String taskName = etTaskName.getText().toString().trim();
        String deadline = tvDeadline.getText().toString();

        int selectedPriorityId = rgPriority.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedPriorityId);

        if (selectedRadioButton == null) {
            Toast.makeText(this, "Please select a priority", Toast.LENGTH_SHORT).show();
            return;
        }
        String priority = selectedRadioButton.getText().toString();

        if (TextUtils.isEmpty(taskName)) {
            etTaskName.setError("Task name is required");
            etTaskName.requestFocus();
            return;
        }

        if (deadline.equals("Select Date")) {
            Toast.makeText(this, "Please select a deadline", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(taskName, deadline, priority);

        databaseReference.push().setValue(newTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddTaskActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddTaskActivity.this, "Failed to add task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
