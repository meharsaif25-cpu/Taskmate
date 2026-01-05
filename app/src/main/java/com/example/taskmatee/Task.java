package com.example.taskmatee;

import java.io.Serializable;

public class Task implements Serializable {

    // It's a good practice to add a serialVersionUID for Serializable classes
    private static final long serialVersionUID = 1L;    // --- FIELDS ---
    // Make sure all fields that you save to Firebase are declared here
    public String taskId;
    public String taskName;
    public String deadline;
    public String priority;

    // A no-argument constructor is REQUIRED for Firebase to deserialize data
    public Task() {
    }

    // --- CONSTRUCTORS ---
    // This constructor is used by AddTaskActivity and UpdateTaskActivity
    public Task(String taskName, String deadline, String priority) {
        this.taskName = taskName;
        this.deadline = deadline;
        this.priority = priority;
    }

    // --- GETTERS AND SETTERS ---
    // These are essential for your code to access the fields
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
