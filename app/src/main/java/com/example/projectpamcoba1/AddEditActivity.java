package com.example.projectpamcoba1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.Window;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class AddEditActivity extends AppCompatActivity {

    private EditText editTitle;
    private TextView nameFile;
    private Button btnSimpan, btnHapus;
    private LinearLayout filePicker;
    private Uri selectedFileUri;
    private FirebaseFirestore db;
    private String noteId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        editTitle = findViewById(R.id.editTitle);
        nameFile = findViewById(R.id.name_file);
        btnSimpan = findViewById(R.id.btnSimpan);
        btnHapus = findViewById(R.id.btnHapus);
        filePicker = findViewById(R.id.audioPicker);
        ImageButton backButton = findViewById(R.id.backButton);

        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> finish());
        filePicker.setOnClickListener(v -> pilihFile());

        btnSimpan.setOnClickListener(v -> showSaveDialog());
        btnHapus.setOnClickListener(v -> showDeleteDialog());

        // Ambil data dari intent
        String titleFromIntent = getIntent().getStringExtra("title");
        noteId = getIntent().getStringExtra("noteId");

        if (titleFromIntent != null) {
            editTitle.setText(titleFromIntent);
        }

        // Notifbar warna kuning
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.header_yellow));
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    String fileName = getFileName(selectedFileUri);
                    nameFile.setText(fileName);
                }
            });

    private void pilihFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Pilih File Gambar atau PDF"));
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_save, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnDialogSimpan = view.findViewById(R.id.btnSimpan);
        Button btnDialogBatal = view.findViewById(R.id.btnBatal);

        btnDialogSimpan.setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();

            if (title.isEmpty()) {
                editTitle.setError("Judul tidak boleh kosong");
                editTitle.requestFocus();
                return;
            }

            Map<String, Object> todoMap = new HashMap<>();
            todoMap.put("title", title);
            todoMap.put("fileUri", selectedFileUri != null ? selectedFileUri.toString() : "");
            todoMap.put("isDone", false);
            todoMap.put("date", getCurrentFormattedDate());

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (noteId != null && !noteId.isEmpty()) {
                // Update dokumen
                db.collection("users")
                        .document(uid)
                        .collection("todos")
                        .document(noteId)
                        .set(todoMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Catatan berhasil diperbarui", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Gagal memperbarui: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                // Tambah dokumen baru
                db.collection("users")
                        .document(uid)
                        .collection("todos")
                        .add(todoMap)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "Catatan berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Gagal menambahkan: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });

        btnDialogBatal.setOnClickListener(v -> dialog.dismiss());
    }

    private void showDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnHapus = view.findViewById(R.id.btnHapus);
        Button btnBatal = view.findViewById(R.id.btnBatal);

        btnHapus.setOnClickListener(v -> {
            if (noteId == null || noteId.isEmpty()) {
                Toast.makeText(this, "Gagal menghapus: ID tidak tersedia", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            db.collection("users")
                    .document(uid)
                    .collection("todos")
                    .document(noteId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("noteId", noteId);
                        setResult(RESULT_CANCELED, resultIntent);
                        dialog.dismiss();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnBatal.setOnClickListener(v -> dialog.dismiss());
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null && uri != null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private String getCurrentFormattedDate() {
        return new SimpleDateFormat("dd MMMM yyyy HH:mm", new Locale("id", "ID")).format(new Date());
    }
}
