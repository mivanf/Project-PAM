package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectpamcoba1.data.model.ToDoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ToDoListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ToDoAdapter adapter;
    private List<ToDoItem> todoList;
    private List<String> noteIdList;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private final ActivityResultLauncher<Intent> addTodoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Refresh list dari Firestore setelah kembali dari AddEditActivity
                if (result.getResultCode() == RESULT_OK || result.getResultCode() == RESULT_CANCELED) {
                    fetchTodos();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);
        ImageButton backButton = findViewById(R.id.backButton);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        todoList = new ArrayList<>();
        noteIdList = new ArrayList<>();

        adapter = new ToDoAdapter(todoList, noteIdList, this, position -> {
            ToDoItem item = todoList.get(position);
            String noteId = noteIdList.get(position);

            Intent intent = new Intent(ToDoListActivity.this, AddEditActivity.class);
            intent.putExtra("title", item.getTitle());
            intent.putExtra("noteId", noteId);
            addTodoLauncher.launch(intent);
        });

        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        backButton.setOnClickListener(v -> finish());

        fetchTodos(); // Ambil data awal

        FloatingActionButton fab = findViewById(R.id.btn_add_todo);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(ToDoListActivity.this, AddEditActivity.class);
            addTodoLauncher.launch(intent);
        });
    }

    private void fetchTodos() {
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("todos")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        todoList.clear();
                        noteIdList.clear();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String title = doc.getString("title");
                            String date = doc.getString("date");
                            boolean isDone = Boolean.TRUE.equals(doc.getBoolean("isDone"));

                            todoList.add(new ToDoItem(title, date, isDone));
                            noteIdList.add(doc.getId());
                        }

                        adapter.setItems(todoList, noteIdList);
                    });
        }
    }

    private String getCurrentFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        return sdf.format(new Date());
    }
}
