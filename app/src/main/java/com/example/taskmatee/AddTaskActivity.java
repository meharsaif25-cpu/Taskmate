package com.example.taskmatee;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private EditText etTaskName;
    private TextView tvDeadline;
    private RadioGroup rgPriority;
    private Button btnSaveTask;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    
    // API key for Gemini assistant
    private static final String GEMINI_API_KEY = "AIzaSyBQuI9zaiJd3ydLiiwNEBm2IyRBk77mqOU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        etTaskName = findViewById(R.id.etTaskName);
        tvDeadline = findViewById(R.id.tvDeadline);
        rgPriority = findViewById(R.id.rgPriority);
        btnSaveTask = findViewById(R.id.btnSaveTask);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Authentication required.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Tasks").child(userId);

        tvDeadline.setOnClickListener(v -> showDatePickerDialog());
        btnSaveTask.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                } else {
                    saveTask();
                }
            } else {
                saveTask();
            }
        });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
            tvDeadline.setText(selectedDate);
        }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void saveTask() {
        String taskName = etTaskName.getText().toString().trim();
        String deadline = tvDeadline.getText().toString();
        int selectedPriorityId = rgPriority.getCheckedRadioButtonId();

        if (TextUtils.isEmpty(taskName) || deadline.equals("Select Date") || selectedPriorityId == -1) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedPriorityId);
        String priority = selectedRadioButton.getText().toString();
        String taskId = databaseReference.push().getKey();
        Task newTask = new Task(taskName, deadline, priority);

        if (taskId != null) {
            databaseReference.child(taskId).setValue(newTask).addOnSuccessListener(aVoid -> {
                scheduleAllNotifications(newTask, taskId);
                Toast.makeText(AddTaskActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }).addOnFailureListener(e -> Toast.makeText(AddTaskActivity.this, "Failed to add task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void scheduleAllNotifications(Task task, String taskId) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            
            Calendar deadlineCalendar = Calendar.getInstance();
            deadlineCalendar.setTime(sdf.parse(task.getDeadline()));
            // Set a default time of 9:00 AM for the deadline
            deadlineCalendar.set(Calendar.HOUR_OF_DAY, 9);
            deadlineCalendar.set(Calendar.MINUTE, 0);
            deadlineCalendar.set(Calendar.SECOND, 0);

            // 1. Schedule 1-Day Reminder
            Calendar oneDayReminderCalendar = (Calendar) deadlineCalendar.clone();
            oneDayReminderCalendar.add(Calendar.DAY_OF_YEAR, -1);
            scheduleSingleNotification(alarmManager, task, taskId, oneDayReminderCalendar.getTimeInMillis(), "Your task is due tomorrow: ", 0);

            // 2. Schedule 1-Hour Reminder
            Calendar oneHourReminderCalendar = (Calendar) deadlineCalendar.clone();
            oneHourReminderCalendar.add(Calendar.HOUR_OF_DAY, -1);
            scheduleSingleNotification(alarmManager, task, taskId, oneHourReminderCalendar.getTimeInMillis(), "Your task is due in one hour: ", 1);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void scheduleSingleNotification(AlarmManager alarmManager, Task task, String taskId, long triggerTime, String message, int requestCodeOffset) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskName", task.getTaskName());
        intent.putExtra("taskDeadline", task.getDeadline());
        intent.putExtra("notification_message", message);
        
        int notificationId = taskId.hashCode() + requestCodeOffset;
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveTask();
            } else {
                Toast.makeText(this, "Notification permission is required for reminders.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
