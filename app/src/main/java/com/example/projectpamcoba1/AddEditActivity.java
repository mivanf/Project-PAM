package com.example.projectpamcoba1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddEditActivity extends AppCompatActivity {

    private EditText editTitle;
    private TextView nameFile;
    private Button btnSimpan, btnHapus, btnDownload;
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
        btnDownload = findViewById(R.id.btnDownloadFile);
        ImageButton backButton = findViewById(R.id.backButton);

        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> finish());
        filePicker.setOnClickListener(v -> pilihFile());
        btnSimpan.setOnClickListener(v -> showSaveDialog());
        btnHapus.setOnClickListener(v -> showDeleteDialog());

        // Sembunyikan tombol download default
        btnDownload.setVisibility(View.GONE);

        // Ambil data dari intent
        String titleFromIntent = getIntent().getStringExtra("title");
        noteId = getIntent().getStringExtra("noteId");
        String fileUriFromIntent = getIntent().getStringExtra("fileUri");

        Log.d("AddEditActivity", "fileUriFromIntent = " + fileUriFromIntent);

        if (titleFromIntent != null) {
            editTitle.setText(titleFromIntent);
        }

        if (fileUriFromIntent != null && !fileUriFromIntent.isEmpty()) {
            Uri fileUri = Uri.parse(fileUriFromIntent);
            String fileName = getFileName(fileUri);
            if (fileName == null) {
                fileName = "downloaded_file_" + System.currentTimeMillis();
            }

            nameFile.setText(fileName);
            btnDownload.setVisibility(View.VISIBLE);

            String finalFileName = fileName;
            btnDownload.setOnClickListener(v -> {
                if (fileUri.getScheme() != null && fileUri.getScheme().startsWith("http")) {
                    downloadFile(fileUriFromIntent, finalFileName);
                } else {
                    Toast.makeText(this, "File ini tersimpan di perangkat", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    String fileName = getFileName(selectedFileUri);
                    nameFile.setText(fileName != null ? fileName : "nama_file_tidak_dikenal");
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

    private void downloadFile(String fileUrl, String fileName) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(fileUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DownloadError", "Download failed", e);
                runOnUiThread(() ->
                        Toast.makeText(AddEditActivity.this, "Gagal mengunduh file", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(downloadsDir, fileName);

                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(response.body().bytes());
                    fos.close();

                    runOnUiThread(() ->
                            Toast.makeText(AddEditActivity.this, "File berhasil diunduh ke folder Download", Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }
}
