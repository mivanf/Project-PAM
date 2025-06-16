package com.example.projectpamcoba1;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectpamcoba1.data.model.ToDoItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.ToDoViewHolder> {
    private List<ToDoItem> todoList;
    private List<String> noteIdList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ToDoAdapter(List<ToDoItem> todoList, List<String> noteIdList, Context context, OnItemClickListener listener) {
        this.todoList = todoList;
        this.noteIdList = noteIdList;
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ToDoItem> newList, List<String> newIdList) {
        this.todoList = newList;
        this.noteIdList = newIdList;
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

        // Hindari trigger listener saat setChecked
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isChecked());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked); // update di memory

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String noteId = noteIdList.get(holder.getAdapterPosition());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("todos")
                    .document(noteId)
                    .update("isDone", isChecked)
                    .addOnFailureListener(e -> Toast.makeText(context, "Gagal update checkbox: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }
}
