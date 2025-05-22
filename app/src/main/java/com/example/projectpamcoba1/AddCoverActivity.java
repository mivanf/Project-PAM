package com.example.projectpamcoba1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddCoverActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imagePreview, ivBack;
    private TextView tvJudul;
    private Uri selectedImageUri;
    private String selectedColor, title, content, id;
    private FirebaseFirestore db;
    private String imagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_cover);

        ivBack = findViewById(R.id.iv_back);
        imagePreview = findViewById(R.id.iv_cover);
        db = FirebaseFirestore.getInstance();

        // Ambil data dari intent sebelumnya (AddNoteActivity)
        selectedColor = getIntent().getStringExtra("color");
        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");

        // Tampilkan ke TextView
        tvJudul = findViewById(R.id.et_judul_note);
        tvJudul.setText(title);

        // Tombol pilih gambar dari galeri
        findViewById(R.id.btn_pilih_gambar).setOnClickListener(v -> openGallery());

        // Tombol simpan
        findViewById(R.id.btn_simpan).setOnClickListener(v -> {
            if (selectedImageUri != null) {
                saveImageToLocal();
            } else {
                // Gunakan gambar default jika tidak pilih gambar
                imagePath = "default_" + selectedColor;
                saveNoteToFirestore();
            }
        });

        // Tombol kembali
        ivBack.setOnClickListener(v -> onBackPressed());

        // Notifbar warna biru
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.notes_bar));
    }

    // Membuka galeri untuk memilih gambar
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imagePreview.setImageURI(selectedImageUri);
        }
    }

    // Simpan gambar
    private void saveImageToLocal() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            File file = new File(getFilesDir(), "cover_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();

            imagePath = file.getAbsolutePath();
            saveNoteToFirestore();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan gambar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Simpan ke Firestore
    private void saveNoteToFirestore() {
        String currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        Note note = new Note(title, content, selectedColor, imagePath, currentDate);

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