package com.example.taskmatee;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;

public class UpdateTaskActivity extends AppCompatActivity {

    private EditText etTaskName;
    private TextView tvDeadline, tvScreenTitle;
    private RadioGroup rgPriority;
    private Button btnSaveTask, btnDeleteTask;

    private DatabaseReference databaseReference;
    private Task currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task); // Reuse the improved layout

        // *** FIX: Initialize ALL views first, immediately after setContentView() ***
        etTaskName = findViewById(R.id.etTaskName);
        tvDeadline = findViewById(R.id.tvDeadline);
        rgPriority = findViewById(R.id.rgPriority);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnDeleteTask = findViewById(R.id.btnDeleteTask);
        // This was the view causing the crash because it was used before it was initialized
        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        // *** END OF FIX ***

        // Retrieve the passed task object
        currentTask = (Task) getIntent().getSerializableExtra("task");

        // --- Data Integrity Check ---
        if (currentTask == null || currentTask.getTaskId() == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Error: Task data could not be loaded.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Setup Firebase Reference ---
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Tasks")
                .child(userId).child(currentTask.getTaskId());

        // --- Configure UI for "Update" Mode ---
        // Now that tvScreenTitle is initialized, this line is safe to call
        tvScreenTitle.setText("Update Task");
        btnSaveTask.setText("Update Task");
        btnDeleteTask.setVisibility(View.VISIBLE);

        populateUiWithTaskData();

        // --- Set Listeners ---
        btnSaveTask.setOnClickListener(v -> updateTask());
        btnDeleteTask.setOnClickListener(v -> deleteTask());
        tvDeadline.setOnClickListener(v -> showDatePickerDialog());
    }

    private void populateUiWithTaskData() {
        etTaskName.setText(currentTask.getTaskName());
        tvDeadline.setText(currentTask.getDeadline());

        // Set the correct radio button for priority
        if (currentTask.getPriority() != null) {
            switch (currentTask.getPriority().toLowerCase()) {
                case "high":
                    rgPriority.check(R.id.rbHigh);
                    break;
                case "medium":
                    rgPriority.check(R.id.rbMedium);
                    break;
            }
        }
    }

    private void updateTask() {
        String taskName = etTaskName.getText().toString().trim();
        String deadline = tvDeadline.getText().toString();
        int selectedRadioButtonId = rgPriority.getCheckedRadioButtonId();

        if (selectedRadioButtonId == -1) {
            Toast.makeText(this, "Please select a priority", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
        String priority = selectedRadioButton.getText().toString();

        if (TextUtils.isEmpty(taskName) || deadline.equals("Select Date")) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Task updatedTask = new Task(taskName, deadline, priority);
        databaseReference.setValue(updatedTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UpdateTaskActivity.this, "Task Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(UpdateTaskActivity.this, "Update Failed", Toast.LENGTH_SHORT).show());
    }

    private void deleteTask() {
        databaseReference.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UpdateTaskActivity.this, "Task Deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(UpdateTaskActivity.this, "Delete Failed", Toast.LENGTH_SHORT).show());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            tvDeadline.setText(selectedDate);
        };
        new DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}
