package com.example.taskmatee;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// *** FIXED: The correct import for your Task class ***
import com.example.taskmatee.Task;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final ArrayList<Task> taskList;
    private final Context context;

    public TaskAdapter(Context context, ArrayList<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // *** FIXED: This now uses the correct Task class ***
        Task task = taskList.get(position);
        holder.tvTaskName.setText(task.getTaskName());
        holder.tvTaskDeadline.setText(task.getDeadline());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UpdateTaskActivity.class);
            intent.putExtra("task", task); // Pass the serializable Task object
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName;
        TextView tvTaskDeadline;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTaskDeadline = itemView.findViewById(R.id.tvTaskDeadline);
        }
    }
}
