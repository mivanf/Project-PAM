package com.example.projectpamcoba1.data;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectpamcoba1.R;
import com.example.projectpamcoba1.data.model.ToDoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.*;

public class ToDoListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ToDoAdapter adapter;
    private List<ToDoItem> todoList;

    // Launcher untuk tambah atau edit to-do
    private final ActivityResultLauncher<Intent> addTodoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String title = result.getData().getStringExtra("title");
                    boolean isDone = result.getData().getBooleanExtra("isDone", false);
                    int taskId = result.getData().getIntExtra("taskId", -1);

                    if (taskId == -1) {
                        // Tambah item baru
                        String currentDate = getCurrentFormattedDate();
                        ToDoItem newItem = new ToDoItem(title, currentDate, isDone);
                        todoList.add(newItem);
                        adapter.notifyItemInserted(todoList.size() - 1);
                    } else {
                        // Edit item lama
                        ToDoItem updatedItem = todoList.get(taskId);
                        updatedItem.setTitle(title);
                        updatedItem.setDone(isDone);
                        adapter.notifyItemChanged(taskId);
                    }
                } else if (result.getResultCode() == RESULT_CANCELED && result.getData() != null) {
                    // Hapus item
                    int taskId = result.getData().getIntExtra("taskId", -1);
                    if (taskId != -1) {
                        todoList.remove(taskId);
                        adapter.notifyItemRemoved(taskId);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);

        todoList = new ArrayList<>();
        // Contoh data awal dengan tanggal manual
        todoList.add(new ToDoItem("Makan sayur", "Sabtu, 12 April 2025", true));
        todoList.add(new ToDoItem("Tugas PAM", "Sabtu, 12 April 2025", false));

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ToDoAdapter(todoList, this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.btn_add_todo);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(ToDoListActivity.this, AddEditActivity.class);
            intent.putExtra("title", "To-Do Baru " + System.currentTimeMillis());
            addTodoLauncher.launch(intent);
        });
    }

    // Fungsi untuk ambil tanggal saat ini dalam format Indonesia
    private String getCurrentFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        return sdf.format(new Date());
    }
}