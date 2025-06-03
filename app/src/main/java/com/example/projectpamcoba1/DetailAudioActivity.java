package com.example.projectpamcoba1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DetailAudioActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_REQUEST = 100;

    private ImageView backButton;
    private CardView audioPicker;
    private TextView nameFileTextView;
    private EditText editTextTitle;
    private RadioGroup radioGroupColors;
    private Button editButton;
    private Button downloadButton;

    private Uri audioUri = null;
    private FirebaseFirestore db;
    private String userId;
    private String noteId;
    private String existingAudioUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_audio);

        // Inisialisasi Cloudinary dengan unsigned preset
        CloudinaryManager.init(this);

        backButton = findViewById(R.id.iv_back);
        audioPicker = findViewById(R.id.btn_pilih_audio);
        nameFileTextView = findViewById(R.id.tv_tambah_audio);
        editTextTitle = findViewById(R.id.et_judul);
        radioGroupColors = findViewById(R.id.radioGroupColors);
        editButton = findViewById(R.id.btn_edit);
        downloadButton = findViewById(R.id.btn_download_audio);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Ambil data dari intent
        Intent intent = getIntent();
        noteId = intent.getStringExtra("noteId");
        editTextTitle.setText(intent.getStringExtra("title"));
        existingAudioUrl = intent.getStringExtra("audioUrl");
        nameFileTextView.setText(getFileNameFromUri(Uri.parse(existingAudioUrl)));
        setSelectedColor(intent.getStringExtra("color"));

        backButton.setOnClickListener(v -> onBackPressed());

        audioPicker.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
            pickIntent.setType("audio/*");
            startActivityForResult(pickIntent, PICK_AUDIO_REQUEST);
        });

        editButton.setOnClickListener(v -> {
            if (audioUri != null) {
                uploadAudioToCloudinary(audioUri);
            } else {
                saveDataToFirestore(existingAudioUrl); // gunakan audio lama jika tidak ganti
            }
        });

        downloadButton.setOnClickListener(v -> {
            if (existingAudioUrl != null) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(existingAudioUrl));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "Tidak ada audio untuk diunduh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            audioUri = data.getData();
            nameFileTextView.setText(getFileNameFromUri(audioUri));
        }
    }

    private void uploadAudioToCloudinary(Uri audioUri) {
        Toast.makeText(this, "Uploading audio...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(audioUri)
                .option("resource_type", "video") // audio dianggap sebagai video di Cloudinary
                .unsigned("YOUR_UNSIGNED_UPLOAD_PRESET") // ganti dengan preset unsigned kamu
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String audioUrl = (String) resultData.get("secure_url");
                        saveDataToFirestore(audioUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(DetailAudioActivity.this, "Upload gagal: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveDataToFirestore(String audioUrl) {
        String title = editTextTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Judul harus diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = radioGroupColors.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Pilih warna terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadio = findViewById(selectedId);
        String selectedColor = selectedRadio.getText().toString();

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("title", title);
        updatedData.put("color", selectedColor);
        updatedData.put("audioUrl", audioUrl);

        db.collection("users").document(userId)
                .collection("notes")
                .document(noteId)
                .update(updatedData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memperbarui: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setSelectedColor(String color) {
        for (int i = 0; i < radioGroupColors.getChildCount(); i++) {
            View view = radioGroupColors.getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) view;
                if (radioButton.getText().toString().equalsIgnoreCase(color)) {
                    radioButton.setChecked(true);
                    break;
                }
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null && uri != null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
