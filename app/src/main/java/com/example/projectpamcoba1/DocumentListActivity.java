package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.example.projectpamcoba1.DocumentAdapter;
import com.example.projectpamcoba1.DocumentNote;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class DocumentListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DocumentAdapter adapter;
    private List<DocumentNote> documentList = new ArrayList<>();
    private List<DocumentNote> filteredList = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId = FirebaseAuth.getInstance().getUid();

    private EditText etSearch;
    private ImageView ivBack;
    private FloatingActionButton btnAddDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);

        recyclerView = findViewById(R.id.recyclerViewDocuments);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new DocumentAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        etSearch = findViewById(R.id.et_search_document);
        ivBack = findViewById(R.id.iv_back);
        btnAddDocument = findViewById(R.id.btn_add_document);

        ivBack.setOnClickListener(v -> finish());

        btnAddDocument.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddDocumentActivity.class);
            startActivity(intent);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDocuments(s.toString());
            }
        });

        loadDocuments();
    }

    private void loadDocuments() {
        db.collection("users")
                .document(userId)
                .collection("documents")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    documentList.clear();
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        try {
                            DocumentNote note = snapshot.toObject(DocumentNote.class);
                            if (note != null) {
                                note.setId(snapshot.getId());    // ← isi ID di sini
                                documentList.add(note);
                            }
                        } catch (Exception e) {
                            Log.e("DocumentLoadError", "Gagal memuat dokumen: " + e.getMessage());
                        }
                    }
                    filterDocuments(etSearch.getText().toString());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal memuat dokumen: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void filterDocuments(String keyword) {
        filteredList.clear();
        for (DocumentNote note : documentList) {
            if (note.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(note);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();  // ini akan ambil ulang data terbaru dari Firestore
    }
}


