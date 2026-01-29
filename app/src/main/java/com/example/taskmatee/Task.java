package com.example.taskmatee;

import java.io.Serializable;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    public String taskId;
    public String taskName;
    public String deadline;
    public String priority;
    public boolean completed;

    public Task() {}

    public Task(String taskName, String deadline, String priority) {
        this.taskName = taskName;
        this.deadline = deadline;
        this.priority = priority;
        this.completed = false;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
