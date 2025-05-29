package com.example.projectpamcoba1;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DetailNoteActivity extends AppCompatActivity {
    private TextView tvTitle, tvDate, tvWordCountValue;
    private EditText etTitle, etNoteContent;
    private ImageView ivBack, ivCover;
    private CardView cardViewCover;
    private RadioGroup radioGroupColors;
    private RadioButton rbBlue, rbOrange, rbPink, rbPurple;
    private String noteId;
    private FirebaseFirestore db;
    private Button btnEdit, btnDelete;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri = null;
    private Note note;
    private View rootLayout;
    private String userId;
    private boolean isCloudinaryInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_note);

        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tv_detail);
        tvDate = findViewById(R.id.tv_tanggal);
        tvWordCountValue = findViewById(R.id.tv_word_count);
        etTitle = findViewById(R.id.et_judul);
        etNoteContent = findViewById(R.id.et_isi_note);
        ivBack = findViewById(R.id.iv_back);
        ivCover = findViewById(R.id.iv_cover);
        cardViewCover = findViewById(R.id.btn_pilih_gambar);
        radioGroupColors = findViewById(R.id.radioGroupColors);
        rbBlue = findViewById(R.id.rb_blue);
        rbOrange = findViewById(R.id.rb_orange);
        rbPink = findViewById(R.id.rb_pink);
        rbPurple = findViewById(R.id.rb_purple);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_hapus);
        rootLayout = findViewById(R.id.detail_note_root);

        initCloudinary();

        note = (Note) getIntent().getSerializableExtra("note");

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (note != null) {
            noteId = note.getId();
            etTitle.setText(note.getTitle());
            etNoteContent.setText(note.getContent());
            tvDate.setText(note.getDate());
            updateCoverAndColor(note.getColor(), note.getImagePath());
        } else {
            Toast.makeText(this, "Catatan tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update jumlah kata secara dinamis
        etNoteContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int wordCount = s.toString().trim().isEmpty() ? 0 : s.toString().trim().split("\\s+").length;
                tvWordCountValue.setText(wordCount + "/5000 Kata");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Pilih gambar dari galeri
        cardViewCover.setOnClickListener(v -> openGallery());

        // Tombol kembali
        ivBack.setOnClickListener(v -> onBackPressed());

        // Status bar warna biru
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.notes_bar));

        btnEdit.setOnClickListener(v -> {
            if (noteId == null) {
                Toast.makeText(this, "ID catatan tidak ditemukan", Toast.LENGTH_SHORT).show();
                return;
            }
            showConfirmDialog(
                    "Simpan Catatan",
                    "Apakah kamu ingin menyimpan perubahan pada catatan ini?",
                    "Simpan",
                    () -> {
                        String selectedColor = getSelectedColor();
                        if (selectedImageUri != null) {
                            uploadImageToCloudinaryAndSaveNote(selectedColor);
                        } else {
                            updateNoteInFirestore(selectedColor, null);
                        }
                    });
        });

        btnDelete.setOnClickListener(v -> {
            if (noteId == null) {
                Toast.makeText(this, "ID catatan tidak ditemukan", Toast.LENGTH_SHORT).show();
                return;
            }
            showConfirmDialog(
                    "Hapus Catatan",
                    "Yakin ingin menghapus catatan ini?",
                    "Hapus",
                    this::deleteNote
            );
        });

        // Tombol download gambar cover
        Button btnDownload = findViewById(R.id.btn_download);
        btnDownload.setOnClickListener(v -> downloadCoverImage());
    }

    // Inisialisasi Cloudinary
    private void initCloudinary() {
        if (!isCloudinaryInitialized) {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dk7ayxsny"); // Menggunakan cloud dk7ayxsny
                MediaManager.init(this, config);
                isCloudinaryInitialized = true;
            } catch (IllegalStateException e) {
                Log.e("Cloudinary", "MediaManager already initialized");
            }
        }
    }

    // Upload gambar ke Cloudinary dan update Firestore
    private void uploadImageToCloudinaryAndSaveNote(String selectedColor) {
        Toast.makeText(this, "Mengunggah gambar...", Toast.LENGTH_SHORT).show();

        try {
            MediaManager.get().upload(selectedImageUri)
                    .unsigned("pam-project") // Menggunakan preset pam-project
                    .option("public_id", "project-pam/cover_" + System.currentTimeMillis()) // Menyimpan ke folder project-pam
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            updateNoteInFirestore(selectedColor, imageUrl);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(DetailNoteActivity.this, "Upload gagal: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();

        } catch (Exception e) {
            Toast.makeText(this, "Gagal upload gambar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Update data note di Firestore
    private void updateNoteInFirestore(String color, @Nullable String newImageUrl) {
        Map<String, Object> updatedNote = new HashMap<>();
        updatedNote.put("title", etTitle.getText().toString().trim());
        updatedNote.put("content", etNoteContent.getText().toString().trim());
        updatedNote.put("color", color);
        if (newImageUrl != null) {
            updatedNote.put("imagePath", newImageUrl);
        }

        db.collection("users")
                .document(userId)
                .collection("notes")
                .document(noteId)
                .update(updatedNote)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Catatan berhasil disimpan", Toast.LENGTH_SHORT).show();
                    updateCoverAndColor(color, newImageUrl != null ? newImageUrl : note.getImagePath());
                    selectedImageUri = null; // reset image uri setelah upload sukses
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal menyimpan catatan", Toast.LENGTH_SHORT).show());
    }

    // Menghapus note pada Firestore
    private void deleteNote() {
        db.collection("users")
                .document(userId)
                .collection("notes")
                .document(noteId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Gagal menghapus catatan", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Mengambil warna yang dipilih dari radio button
    private String getSelectedColor() {
        int selectedId = radioGroupColors.getCheckedRadioButtonId();
        if (selectedId == rbOrange.getId()) return "oranye";
        if (selectedId == rbPink.getId()) return "pink";
        if (selectedId == rbPurple.getId()) return "ungu";
        return "biru"; // Warna default
    }

    // Mengupdate cover dan warna
    private void updateCoverAndColor(String color, String coverUrl) {
        int colorRes = R.color.bg_card_blue;
        int defaultCoverRes = R.drawable.ic_default_blue;

        switch (color.toLowerCase()) {
            case "oranye":
                colorRes = R.color.bg_card_orange;
                defaultCoverRes = R.drawable.ic_default_orange;
                rbOrange.setChecked(true);
                break;
            case "pink":
                colorRes = R.color.bg_card_pink;
                defaultCoverRes = R.drawable.ic_default_pink;
                rbPink.setChecked(true);
                break;
            case "ungu":
                colorRes = R.color.bg_card_purple;
                defaultCoverRes = R.drawable.ic_default_purple;
                rbPurple.setChecked(true);
                break;
            default:
                colorRes = R.color.bg_card_blue;
                defaultCoverRes = R.drawable.ic_default_blue;
                rbBlue.setChecked(true);
                break;
        }

        cardViewCover.setCardBackgroundColor(getResources().getColor(colorRes));
        rootLayout.setBackgroundColor(getResources().getColor(colorRes));

        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this).load(coverUrl).into(ivCover);
        } else {
            ivCover.setImageResource(defaultCoverRes);
        }
    }

    // Menampilkan popup konfirmasi
    private void showConfirmDialog(String title, String message, String confirmButtonText, Runnable onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_alert, null);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvMessage = view.findViewById(R.id.tvMessage);
        AppCompatButton btnCancel = view.findViewById(R.id.btnCancel);
        AppCompatButton btnConfirm = view.findViewById(R.id.btnConfirm);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(confirmButtonText);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            onConfirm.run();
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    // Membuka galeri
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Mengambil gambar dari galeri
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ivCover.setImageURI(selectedImageUri); // Menampilkan gambar sementara
        }
    }

    // Mendownload cover gambar ke penyimpanan
    private void downloadCoverImage() {
        ivCover.setDrawingCacheEnabled(true);
        ivCover.buildDrawingCache();
        Bitmap bitmap = ivCover.getDrawingCache();

        if (bitmap != null) {
            try {
                String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
                File file = new File(getExternalFilesDir(null), fileName);
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();

                Toast.makeText(this, "Gambar disimpan: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Gambar tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }
}
