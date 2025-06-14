package com.example.projectpamcoba1;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectpamcoba1.AddEditActivity;
import com.example.projectpamcoba1.data.model.ToDoItem;
import java.util.List;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.ToDoViewHolder> {
    private List<ToDoItem> todoList;
    private Context context;

    public ToDoAdapter(List<ToDoItem> todoList, Context context) {
        this.todoList = todoList;
        this.context = context;
    }

    // Tambahkan method ini agar data bisa diperbarui
    public void setItems(List<ToDoItem> newList) {
        this.todoList = newList;
        notifyDataSetChanged();
    }

    public static class ToDoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;
        CheckBox checkBox;

        public ToDoViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvDate = view.findViewById(R.id.tvDate);
            checkBox = view.findViewById(R.id.checkBox);
        }
    }

    @NonNull
    @Override
    public ToDoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_todo, parent, false);
        return new ToDoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ToDoViewHolder holder, int position) {
        ToDoItem item = todoList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(item.getDate());
        holder.checkBox.setChecked(item.isChecked());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddEditActivity.class);
            intent.putExtra("title", item.getTitle());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }
}