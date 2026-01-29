package com.example.taskmatee;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
        setContentView(R.layout.activity_add_task);

        etTaskName = findViewById(R.id.etTaskName);
        tvDeadline = findViewById(R.id.tvDeadline);
        rgPriority = findViewById(R.id.rgPriority);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        btnDeleteTask = findViewById(R.id.btnDeleteTask);
        tvScreenTitle = findViewById(R.id.tvScreenTitle);

        currentTask = (Task) getIntent().getSerializableExtra("task");

        if (currentTask == null || currentTask.getTaskId() == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Error: Task data could not be loaded.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Tasks").child(userId).child(currentTask.getTaskId());

        tvScreenTitle.setText("Update Task");
        btnSaveTask.setText("Update Task");
        btnDeleteTask.setVisibility(View.VISIBLE);

        populateUiWithTaskData();

        btnSaveTask.setOnClickListener(v -> updateTask());
        btnDeleteTask.setOnClickListener(v -> deleteTask());
        tvDeadline.setOnClickListener(v -> showDatePickerDialog());
    }

    private void populateUiWithTaskData() {
        etTaskName.setText(currentTask.getTaskName());
        tvDeadline.setText(currentTask.getDeadline());

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
        databaseReference.setValue(updatedTask).addOnSuccessListener(aVoid -> {
            cancelNotification(currentTask.getTaskId());
            scheduleNotification(updatedTask, currentTask.getTaskId());
            Toast.makeText(UpdateTaskActivity.this, "Task Updated", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(UpdateTaskActivity.this, "Update Failed", Toast.LENGTH_SHORT).show());
    }

    private void deleteTask() {
        databaseReference.removeValue().addOnSuccessListener(aVoid -> {
            cancelNotification(currentTask.getTaskId());
            Toast.makeText(UpdateTaskActivity.this, "Task Deleted", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(UpdateTaskActivity.this, "Delete Failed", Toast.LENGTH_SHORT).show());
    }

    private void scheduleNotification(Task task, String taskId) {
        try {
            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.putExtra("taskName", task.getTaskName());
            intent.putExtra("taskDeadline", task.getDeadline());
            int notificationId = taskId.hashCode();
            intent.putExtra("notificationId", notificationId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(task.getDeadline()));
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            long triggerTime = calendar.getTimeInMillis();

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    } else {
                        // Fallback for when exact alarm permission is not granted
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void cancelNotification(String taskId) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        int notificationId = taskId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
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
