package com.example.projectpamcoba1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddCoverActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imagePreview, ivBack;
    private TextView tvJudul;
    private Uri selectedImageUri;
    private String selectedColor, title, content, imageUrl = "";
    private FirebaseFirestore db;
    private boolean isCloudinaryInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_cover);

        // Inisialisasi view
        ivBack = findViewById(R.id.iv_back);
        imagePreview = findViewById(R.id.iv_cover);
        tvJudul = findViewById(R.id.et_judul_note);
        db = FirebaseFirestore.getInstance();

        initCloudinary();

        // Ambil data dari intent sebelumnya
        selectedColor = getIntent().getStringExtra("color");
        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        tvJudul.setText(title);

        // Tombol pilih gambar
        findViewById(R.id.btn_pilih_gambar).setOnClickListener(v -> openGallery());

        // Tombol simpan
        findViewById(R.id.btn_simpan).setOnClickListener(v -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                saveNoteToFirestore();
            } else if (selectedImageUri != null) {
                uploadImageToCloudinary(selectedImageUri);
            } else {
                imageUrl = "default_" + selectedColor;
                saveNoteToFirestore();
            }
        });

        // Tombol kembali
        ivBack.setOnClickListener(v -> onBackPressed());

        // Warna status bar
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.notes_bar));
    }

    // Inisialisasi Cloudinary
    private void initCloudinary() {
        if (!isCloudinaryInitialized) {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dk7ayxsny"); // Ganti sesuai akunmu
                MediaManager.init(this, config);
                isCloudinaryInitialized = true;
            } catch (IllegalStateException e) {
                // Sudah diinisialisasi
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Pilih Gambar"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Glide.with(this).load(selectedImageUri).into(imagePreview);
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        Toast.makeText(this, "Mengunggah gambar...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(imageUri)
                .unsigned("pam-project") // Ganti dengan upload preset kamu
                .option("public_id", "project-pam/cover_" + System.currentTimeMillis()) // Simpan ke folder
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        imageUrl = (String) resultData.get("secure_url");
                        Toast.makeText(AddCoverActivity.this, "Gambar berhasil diunggah", Toast.LENGTH_SHORT).show();
                        saveNoteToFirestore();
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AddCoverActivity.this, "Upload gagal: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveNoteToFirestore() {
        String currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        Note note = new Note(title, content, selectedColor, imageUrl, currentDate);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .collection("notes")
                .add(note)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, NotesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}