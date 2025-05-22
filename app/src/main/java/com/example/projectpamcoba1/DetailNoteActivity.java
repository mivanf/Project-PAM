package com.example.projectpamcoba1;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
    private String imagePath = "";
    private Note note;
    private View rootLayout;
    private String userId;

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
        note = (Note) getIntent().getSerializableExtra("note");

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ambil data judul dari Intent
        String title = getIntent().getStringExtra("title");

        // Menampilkan toast
        if (title != null) {
            Toast.makeText(this, "Membuka halaman detail dari " + title, Toast.LENGTH_SHORT).show();
        }

        // Mengupdate jumlah kata secara dinamis
        etNoteContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
                int wordCount = charSequence.toString().split("\\s+").length;
                tvWordCountValue.setText(wordCount + "/5000 Kata");
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {}
        });

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

        // Tombol pilih gambar dari galeri
        findViewById(R.id.btn_pilih_gambar).setOnClickListener(v -> openGallery());

        // Tombol kembali
        ivBack.setOnClickListener(v -> onBackPressed());

        // Notifbar warna biru
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
                            // jika user memilih gambar baru
                            saveImageToLocalAndUpdateFirestore(selectedColor);
                        } else {
                            // hanya update teks dan warna
                            updateNoteInFirestore(selectedColor, null);
                        }
                    }
            );
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

        // Untuk download gambar cover
        Button btnDownload = findViewById(R.id.btn_download);
        btnDownload.setOnClickListener(v -> downloadCoverImage());
    }

    // Hapus note
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

    // Menentukan warna dari Radio Button
    private String getSelectedColor() {
        int selectedId = radioGroupColors.getCheckedRadioButtonId();
        if (selectedId == rbOrange.getId()) return "oranye";
        if (selectedId == rbPink.getId()) return "pink";
        if (selectedId == rbPurple.getId()) return "ungu";
        return "biru"; // default
    }

    // Perbarui warna dan default cover
    private void updateCoverAndColor(String color, String coverUrl) {
        int colorRes = R.color.bg_card_blue;
        int defaultCoverRes = R.drawable.ic_default_blue;

        switch (color) {
            case "orange":
            case "oranye": // untuk konsistensi jika ada input "oranye"
                colorRes = R.color.bg_card_orange;
                defaultCoverRes = R.drawable.ic_default_orange;
                rbOrange.setChecked(true);
                break;
            case "pink":
                colorRes = R.color.bg_card_pink;
                defaultCoverRes = R.drawable.ic_default_pink;
                rbPink.setChecked(true);
                break;
            case "purple":
            case "ungu": // sama, dukung "ungu"
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

        // Ubah warna background card cover
        cardViewCover.setCardBackgroundColor(getResources().getColor(colorRes));

        // Ubah warna background keseluruhan layout
        rootLayout.setBackgroundColor(getResources().getColor(colorRes));

        // Tampilkan gambar cover
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this).load(coverUrl).into(ivCover);
        } else {
            ivCover.setImageResource(defaultCoverRes);
        }
    }

    // Menampilkan pop-up
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

    // Membuka galeri untuk memilih gambar
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Simpan gambar
    private void saveImageToLocalAndUpdateFirestore(String selectedColor) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            File file = new File(getFilesDir(), "cover_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();

            imagePath = file.getAbsolutePath();

            updateNoteInFirestore(selectedColor, imagePath);

        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan gambar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Perbari data di Firestore
    private void updateNoteInFirestore(String color, @Nullable String newImagePath) {
        Map<String, Object> updatedNote = new HashMap<>();
        updatedNote.put("title", etTitle.getText().toString().trim());
        updatedNote.put("content", etNoteContent.getText().toString().trim());
        updatedNote.put("color", color);
        if (newImagePath != null) {
            updatedNote.put("imagePath", newImagePath);
        }

        db.collection("users")
                .document(userId)
                .collection("notes")
                .document(noteId)
                .update(updatedNote)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Catatan berhasil disimpan", Toast.LENGTH_SHORT).show();
                    updateCoverAndColor(color, newImagePath != null ? newImagePath : note.getImagePath());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal menyimpan catatan", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ivCover.setImageURI(selectedImageUri); // Menamplkan gambar sementara pada ivCover
        }
    }

    // Method untuk download gambar cover
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