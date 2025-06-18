package com.example.projectpamcoba1;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DetailToDo extends AppCompatActivity {

    private TextView detailTitle, detailDate, detailFileName;
    private Button btnDownloadFile;

    private FirebaseFirestore db;
    private String noteId;
    private String uploadedFileUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_todo);

        detailTitle = findViewById(R.id.detailTitle);
        detailFileName = findViewById(R.id.detailFileName);
        btnDownloadFile = findViewById(R.id.btnDownloadFile);

        db = FirebaseFirestore.getInstance();

        // Ambil data dari intent
        String title = getIntent().getStringExtra("title");
        noteId = getIntent().getStringExtra("noteId");

        // Tampilkan judul dan tanggal langsung
        detailTitle.setText(title != null ? title : "(Tidak ada judul)");

        // Ambil fileUri dari Firestore berdasarkan noteId
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(uid)
                .collection("todos")
                .document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        uploadedFileUrl = documentSnapshot.getString("fileUri");
                        if (uploadedFileUrl != null && !uploadedFileUrl.isEmpty()) {
                            detailFileName.setText(getFileNameFromUrl(uploadedFileUrl));
                            btnDownloadFile.setOnClickListener(v ->
                                    downloadFile(uploadedFileUrl, getFileNameFromUrl(uploadedFileUrl))
                            );
                        } else {
                            detailFileName.setText("(Tidak ada file)");
                            btnDownloadFile.setEnabled(false);
                        }
                    } else {
                        detailFileName.setText("(Data tidak ditemukan)");
                        btnDownloadFile.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    detailFileName.setText("(Gagal memuat file)");
                    btnDownloadFile.setEnabled(false);
                });
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

    private String getFileNameFromUrl(String fileUrl) {
        try {
            Uri uri = Uri.parse(fileUrl);
            return uri.getLastPathSegment();
        } catch (Exception e) {
            return "(Nama file tidak diketahui)";
        }
    }
}
