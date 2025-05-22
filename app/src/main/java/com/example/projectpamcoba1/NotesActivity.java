package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private List<Note> noteList;
    private FirebaseFirestore db;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        ivBack = findViewById(R.id.iv_back);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));

        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(this, noteList);
        recyclerView.setAdapter(noteAdapter);

        db = FirebaseFirestore.getInstance();

        loadNotes();  // Load data dari Firestore

        findViewById(R.id.btn_add_note).setOnClickListener(v -> {
            startActivity(new Intent(NotesActivity.this, AddNoteActivity.class));
        });

        // Tombol kembali
        ivBack.setOnClickListener(v -> onBackPressed());

        // Notifbar warna biru
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.notes_bar));
    }

    private void loadNotes() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("notes")
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        noteList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String id = document.getId();
                            String title = document.getString("title");
                            String content = document.getString("content");
                            String color = document.getString("color");
                            String imagePath = document.getString("imagePath");
                            String date = document.getString("date");

                            Note note = new Note(title, content, color, imagePath, date);
                            note.setId(id);
                            noteList.add(note);
                        }
                        noteAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(NotesActivity.this, "Gagal memuat catatan", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }
}