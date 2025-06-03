package com.example.projectpamcoba1;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddAudioNoteActivity extends AppCompatActivity {

    private EditText etJudulAudio;
    private View audioPicker;
    private Button btnSimpanAudio;
    private TextView tvAudioFilename;
    private ImageView ivBack;
    private RadioGroup colorRadioGroup;
    private Uri audioUri = null;
    private FirebaseFirestore firestore;
    private String selectedColor = "ungu"; // default warna

    private final Map<String, String> cloudinaryConfig = new HashMap<String, String>() {{
        put("cloud_name", "dk7ayxsny"); // Ganti dengan cloud name Cloudinary kamu
    }};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_audio_note);

        etJudulAudio = findViewById(R.id.editTextTitle);
        ivBack = findViewById(R.id.iv_back);
        audioPicker = findViewById(R.id.audioPicker);
        btnSimpanAudio = findViewById(R.id.save_button);
        tvAudioFilename = findViewById(R.id.name_file);
        colorRadioGroup = findViewById(R.id.radioGroupColors);

        firestore = FirebaseFirestore.getInstance();
        CloudinaryManager.init(this);

        audioPicker.setOnClickListener(v -> pilihFileAudio());
        btnSimpanAudio.setOnClickListener(v -> simpanAudioNote());

        // Listener untuk RadioGroup warna
        colorRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_blue) {
                    selectedColor = "biru";
                } else if (checkedId == R.id.rb_orange) {
                    selectedColor = "oranye";
                } else if (checkedId == R.id.rb_pink) {
                    selectedColor = "pink";
                } else if (checkedId == R.id.rb_purple) {
                    selectedColor = "ungu";
                }
            }
        });

        ivBack.setOnClickListener(v -> onBackPressed());
    }

    private final ActivityResultLauncher<Intent> audioPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    audioUri = result.getData().getData();
                    String fileName = getFileName(audioUri);
                    tvAudioFilename.setText(fileName);
                }
            });

    private void pilihFileAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        audioPickerLauncher.launch(Intent.createChooser(intent, "Pilih File Audio"));
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void simpanAudioNote() {
        String judul = etJudulAudio.getText().toString().trim();

        if (judul.isEmpty()) {
            etJudulAudio.setError("Judul harus diisi");
            etJudulAudio.requestFocus();
            return;
        }

        if (audioUri == null) {
            Toast.makeText(this, "Pilih file audio terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Mengupload audio...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(audioUri)
                .unsigned("pam-project")
                .option("resource_type", "video")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String audioUrl = (String) resultData.get("secure_url");
                        simpanKeFirestore(judul, audioUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AddAudioNoteActivity.this, "Upload gagal: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void simpanKeFirestore(String judul, String audioUrl) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> dataNote = new HashMap<>();
        dataNote.put("judul", judul);
        dataNote.put("audioUrl", audioUrl);
        dataNote.put("type", "audio");
        dataNote.put("tanggal", System.currentTimeMillis());
        dataNote.put("color", selectedColor);  // Simpan warna pilihan

        firestore.collection("users")
                .document(uid)
                .collection("notes")
                .add(dataNote)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Audio note berhasil disimpan", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal menyimpan data: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
