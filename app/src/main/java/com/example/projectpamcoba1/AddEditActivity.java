package com.example.projectpamcoba1;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class AddEditActivity extends AppCompatActivity {

    private EditText editTitle;
    private TextView nameFile;
    private Button btnSimpan, btnHapus, btnDownload;
    private LinearLayout filePicker;
    private Uri selectedFileUri;
    private FirebaseFirestore db;
    private String noteId;
    private String uploadedFileUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        CloudinaryManager.init(this);

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

        // Dapatkan data dari Intent
        String titleFromIntent = getIntent().getStringExtra("title");
        noteId = getIntent().getStringExtra("noteId");
        String fileUriFromIntent = getIntent().getStringExtra("fileUri");

        if (titleFromIntent != null) editTitle.setText(titleFromIntent);

        if (fileUriFromIntent != null && !fileUriFromIntent.isEmpty()) {
            uploadedFileUrl = fileUriFromIntent;
            nameFile.setText(getFileNameFromUrl(fileUriFromIntent));
            btnDownload.setVisibility(View.VISIBLE);
        } else {
            btnDownload.setVisibility(View.GONE);
        }

        btnDownload.setOnClickListener(v -> {
            if (uploadedFileUrl != null && uploadedFileUrl.startsWith("http")) {
                String fileName = getFileNameFromUrl(uploadedFileUrl);
                downloadFile(uploadedFileUrl, fileName);
            } else {
                Toast.makeText(this, "File belum tersedia untuk diunduh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    String fileName = getFileName(selectedFileUri);
                    nameFile.setText(fileName != null ? fileName : "nama_file_tidak_dikenal");
                    uploadToCloudinary(selectedFileUri);
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

    private void uploadToCloudinary(Uri fileUri) {
        Toast.makeText(this, "Mengupload file ke Cloudinary...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(fileUri)
                .unsigned("pam-project") // Pastikan unsigned preset ini ada di Cloudinary
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        uploadedFileUrl = resultData.get("secure_url").toString();
                        Toast.makeText(AddEditActivity.this, "Upload berhasil", Toast.LENGTH_SHORT).show();
                        btnDownload.setVisibility(View.VISIBLE);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AddEditActivity.this, "Upload gagal: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
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
            todoMap.put("fileUri", uploadedFileUrl != null ? uploadedFileUrl : "");
            todoMap.put("isDone", false);
            todoMap.put("date", getCurrentFormattedDate());

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (noteId != null && !noteId.isEmpty()) {
                db.collection("users").document(uid)
                        .collection("todos")
                        .document(noteId)
                        .set(todoMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Catatan diperbarui", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
                        });
            } else {
                db.collection("users").document(uid)
                        .collection("todos")
                        .add(todoMap)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "Catatan ditambahkan", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
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
            db.collection("users").document(uid)
                    .collection("todos")
                    .document(noteId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Catatan dihapus", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        finish();
                    });
        });

        btnBatal.setOnClickListener(v -> dialog.dismiss());
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null && uri != null) result = uri.getLastPathSegment();
        return result;
    }

    private String getFileNameFromUrl(String url) {
        if (url == null) return "file_tidak_dikenal";
        return Uri.parse(url).getLastPathSegment();
    }

    private String getCurrentFormattedDate() {
        return new SimpleDateFormat("dd MMMM yyyy HH:mm", new Locale("id", "ID")).format(new Date());
    }

    private void downloadFile(String fileUrl, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle(fileName);
        request.setDescription("Mengunduh file...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            Toast.makeText(this, "Download dimulai", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "DownloadManager tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }
}