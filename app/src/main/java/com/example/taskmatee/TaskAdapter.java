package com.example.taskmatee;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private Context context;
    private List<Task> taskList;

    public TaskAdapter(Context context, List<Task> taskList) {
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
        Task task = taskList.get(position);
        holder.tvTaskName.setText(task.getTaskName());
        holder.tvTaskDeadline.setText(task.getDeadline());
        
        // Remove listener before setting check state to avoid triggering it
        holder.cbTask.setOnCheckedChangeListener(null);
        holder.cbTask.setChecked(task.isCompleted());

        holder.cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            task.setCompleted(isChecked);
            FirebaseDatabase.getInstance().getReference("Tasks")
                    .child(userId)
                    .child(task.getTaskId())
                    .setValue(task);
        });

        // Click listener to open UpdateTaskActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UpdateTaskActivity.class);
            intent.putExtra("task", task);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvTaskDeadline;
        CheckBox cbTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTaskDeadline = itemView.findViewById(R.id.tvTaskDeadline);
            cbTask = itemView.findViewById(R.id.cbTask);
        }
    }
}
