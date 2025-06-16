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

        // Inisialisasi komponen tampilan dari layout
        recyclerView = findViewById(R.id.recyclerView);
        ivBack = findViewById(R.id.iv_back);
        btnAddAudio = findViewById(R.id.btn_add_audio);
        etCari = findViewById(R.id.et_cari); // EditText pencarian (fitur tambahan jika dikembangkan)

        // Mengatur layout grid untuk RecyclerView dengan 2 kolom
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        audioList = new ArrayList<>();
        audioAdapter = new AudioAdapter(this, audioList);
        recyclerView.setAdapter(audioAdapter);

        // Inisialisasi koneksi ke Firestore
        db = FirebaseFirestore.getInstance();

        // Memuat data audio dari Firestore
        loadAudio();

        // Listener tombol tambah audio: membuka activity AddAudioNoteActivity
        btnAddAudio.setOnClickListener(v ->
                startActivity(new Intent(AudioActivity.this, AddAudioNoteActivity.class))
        );

        // Listener tombol kembali
        ivBack.setOnClickListener(v -> onBackPressed());

        // Mengubah warna status bar agar sesuai dengan tema aplikasi
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.audio_bar));
    }

    // Method untuk memuat daftar audio dari Firestore dan menampilkannya di RecyclerView
    private void loadAudio() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Ambil UID pengguna saat ini

        db.collection("users")
                .document(uid)
                .collection("notes")
                .orderBy("judul", Query.Direction.ASCENDING) // Mengurutkan berdasarkan judul (judul lama)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        audioList.clear(); // Hapus data lama sebelum mengisi ulang
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            // Hanya catatan yang memiliki audioUrl akan ditampilkan
                            if (doc.contains("audioUrl")) {
                                String id = doc.getId();
                                String title = doc.contains("title") ? doc.getString("title") : doc.getString("judul"); // Kompatibel dengan field lama
                                String audioUrl = doc.getString("audioUrl");
                                String color = doc.getString("color");
                                String date = doc.contains("tanggal") ? doc.getString("tanggal") : "";

                                // Buat objek Audio dan tambahkan ke list
                                Audio audio = new Audio(id, title, audioUrl, color, date);
                                audio.setId(id);
                                audioList.add(audio);
                            }
                        }
                        audioAdapter.notifyDataSetChanged(); // Refresh RecyclerView
                    } else {
                        Toast.makeText(AudioActivity.this, "Gagal memuat audio", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method ini akan dipanggil setiap kali activity ini muncul kembali dari background (misal setelah tambah/edit)
    @Override
    protected void onResume() {
        super.onResume();
        loadAudio(); // Muat ulang data audio agar tampilan selalu terbaru
    }
}