package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;
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
    private FirebaseFirestore db;
    private FirebaseUser user;

    private final ActivityResultLauncher<Intent> addTodoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String title = result.getData().getStringExtra("title");
                    boolean isDone = result.getData().getBooleanExtra("isDone", false);

                    String currentDate = getCurrentFormattedDate();
                    ToDoItem newItem = new ToDoItem(title, currentDate, isDone);

                    // Tambah ke list lokal dan refresh adapter
                    todoList.add(newItem);
                    adapter.notifyItemInserted(todoList.size() - 1);

                    // Simpan ke Firestore: /users/{uid}/notes
                    Map<String, Object> todoMap = new HashMap<>();
                    todoMap.put("title", title);
                    todoMap.put("date", currentDate);
                    todoMap.put("isDone", isDone);

                    if (user != null) {
                        db.collection("users")
                                .document(user.getUid())
                                .collection("notes")
                                .add(todoMap);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        todoList = new ArrayList<>();
        adapter = new ToDoAdapter(todoList, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Ambil data dari Firestore: /users/{uid}/notes
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("notes")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        todoList.clear(); // clear list lama dulu
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String title = doc.getString("title");
                            String date = doc.getString("date");
                            boolean isDone = Boolean.TRUE.equals(doc.getBoolean("isDone"));

                            todoList.add(new ToDoItem(title, date, isDone));
                        }
                        adapter.notifyDataSetChanged();
                    });
        }

        FloatingActionButton fab = findViewById(R.id.btn_add_todo);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(ToDoListActivity.this, AddEditActivity.class);
            addTodoLauncher.launch(intent);
        });
    }

    private String getCurrentFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        return sdf.format(new Date());
    }
}
