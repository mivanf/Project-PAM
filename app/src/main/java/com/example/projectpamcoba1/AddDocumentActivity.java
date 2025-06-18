package com.example.projectpamcoba1;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddDocumentActivity extends AppCompatActivity {

    private EditText editTextTitle;
    private LinearLayout documentPicker;
    private TextView nameFileText, fileInfoText;
    private RadioGroup radioGroupColors;
    private Button saveButton;
    private Uri fileUri;
    private FirebaseFirestore firestore;
    private String selectedColor = "biru";
    private boolean isCloudinaryInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        // Inisialisasi Cloudinary via CloudinaryManager versi lama
        CloudinaryManager.init(this);
        // Fallback init lokal
        initCloudinary();

        editTextTitle    = findViewById(R.id.editTextTitle);
        documentPicker   = findViewById(R.id.documentPicker);
        nameFileText     = findViewById(R.id.name_file);
        fileInfoText     = findViewById(R.id.file_info);
        radioGroupColors = findViewById(R.id.radioGroupColors);
        saveButton       = findViewById(R.id.save_button);
        firestore        = FirebaseFirestore.getInstance();

        radioGroupColors.check(R.id.rb_blue);

        documentPicker.setOnClickListener(v -> openFilePicker());
        saveButton.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            if (title.isEmpty()) {
                editTextTitle.setError("Judul harus diisi");
                return;
            }
            if (fileUri == null) {
                Toast.makeText(this, "Pilih file terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadToCloudinary(fileUri, title, getSelectedColor());
        });

        radioGroupColors.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.rb_blue)      selectedColor = "biru";
            else if (id == R.id.rb_orange) selectedColor = "oranye";
            else if (id == R.id.rb_pink)   selectedColor = "pink";
            else if (id == R.id.rb_purple) selectedColor = "ungu";
        });
    }

    private void initCloudinary() {
        if (!isCloudinaryInitialized) {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dk7ayxsny");
                MediaManager.init(this, config);
                isCloudinaryInitialized = true;
            } catch (IllegalStateException e) {
                Log.e("Cloudinary", "MediaManager already initialized");
            }
        }
    }

    private void openFilePicker() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
        filePickerLauncher.launch(Intent.createChooser(i, "Pilih dokumen"));
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    fileUri = result.getData().getData();
                    nameFileText.setText("Dokumen dipilih");
                    fileInfoText.setText(getFileName(fileUri));
                }
            });

    private String getSelectedColor() {
        int id = radioGroupColors.getCheckedRadioButtonId();
        return ((RadioButton) findViewById(id)).getText().toString().toLowerCase();
    }

    private String getFileName(Uri uri) {
        String res = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i >= 0) res = c.getString(i);
                }
            }
        }
        if (res == null) {
            String path = uri.getPath();
            int cut = (path != null ? path.lastIndexOf('/') : -1);
            res = (cut != -1 ? path.substring(cut + 1) : path);
        }
        return res;
    }

    private void uploadToCloudinary(Uri fileUri, String title, String color) {
        Toast.makeText(this, "Mengupload dokumen…", Toast.LENGTH_SHORT).show();

        String fileName = getFileName(fileUri).trim().replace(" ", "_");
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String publicId = ts + "_" + fileName;

        MediaManager.get().upload(fileUri)
                .unsigned("pam-project")
                .option("resource_type", "raw")
                .option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override public void onStart(String reqId) {}
                    @Override public void onProgress(String reqId, long bytes, long total) {}
                    @Override public void onReschedule(String reqId, ErrorInfo e) {}

                    @Override
                    public void onSuccess(String reqId, Map resultData) {
                        // Simpan URL download ke Firestore saja; download ditangani di DocumentDetailActivity
                        String secureUrl = (String) resultData.get("secure_url");
                        String downloadUrl = secureUrl.replace("/upload/", "/upload/fl_attachment/");
                        saveToFirestore(title, downloadUrl, color);
                        Toast.makeText(AddDocumentActivity.this,
                                        "Dokumen berhasil diupload", Toast.LENGTH_SHORT)
                                .show();
                        finish();
                    }

                    @Override
                    public void onError(String reqId, ErrorInfo err) {
                        Toast.makeText(AddDocumentActivity.this,
                                        "Upload gagal: " + err.getDescription(), Toast.LENGTH_LONG)
                                .show();
                    }
                })
                .dispatch();
    }


    private void saveToFirestore(String title, String fileUrl, String color) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String,Object> doc = new HashMap<>();
        doc.put("title", title);
        doc.put("fileUrl", fileUrl);
        doc.put("color", color);
        doc.put("timestamp", Timestamp.now());

        firestore.collection("users").document(uid).collection("documents")
                .add(doc)
                .addOnSuccessListener(ref -> {
                    ref.update("id", ref.getId());
                    Toast.makeText(this, "Dokumen berhasil disimpan", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal simpan: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}