package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import com.example.projectpamcoba1.Audio;

public class AudioActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AudioAdapter audioAdapter;
    private List<Audio> audioList;
    private FirebaseFirestore db;
    private ImageView ivBack;
    private FloatingActionButton btnAddAudio;
    private EditText etCari;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        // Inisialisasi View
        recyclerView = findViewById(R.id.recyclerView);
        ivBack = findViewById(R.id.iv_back);
        btnAddAudio = findViewById(R.id.btn_add_audio);
        etCari = findViewById(R.id.et_cari); // Untuk fitur pencarian (bisa dikembangkan)

        // RecyclerView setup
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        audioList = new ArrayList<>();
        audioAdapter = new AudioAdapter(this, audioList);
        recyclerView.setAdapter(audioAdapter);

        db = FirebaseFirestore.getInstance();

        // Load audio dari Firestore
        loadAudio();

        // Aksi tombol tambah audio
        btnAddAudio.setOnClickListener(v ->
                startActivity(new Intent(AudioActivity.this, AddAudioNoteActivity.class))
        );

        // Aksi tombol kembali
        ivBack.setOnClickListener(v -> onBackPressed());

        // Ubah warna status bar
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.notes_bar));
    }

    private void loadAudio() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("notes")
                .orderBy("judul", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        audioList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            if (doc.contains("audioUrl")) {
                                String id = doc.getId();
                                String title = doc.getString("judul");   // samakan dengan field Firestore
                                String audioUrl = doc.getString("audioUrl");
                                String color = doc.getString("color");
                                String date = doc.contains("tanggal") ? String.valueOf(doc.getLong("tanggal")) : "";

                                Audio audio = new Audio(id, title, audioUrl, color, date);
                                audio.setId(id);
                                audioList.add(audio);
                            }
                        }
                        audioAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(AudioActivity.this, "Gagal memuat audio", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAudio(); // Perbarui list saat kembali dari tambah/detail
    }
}
