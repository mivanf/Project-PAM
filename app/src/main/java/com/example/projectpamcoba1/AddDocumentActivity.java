package com.example.projectpamcoba1;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Window;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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
    private ImageView ivBack;
    private RadioGroup radioGroupColors;
    private Button saveButton;
    private Uri fileUri;

    private String selectedColor = "biru"; // default
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        CloudinaryManager.init(this); // pastikan inisialisasi

        ivBack      = findViewById(R.id.iv_back);
        editTextTitle = findViewById(R.id.editTextTitle);
        documentPicker = findViewById(R.id.documentPicker);
        nameFileText = findViewById(R.id.name_file);
        fileInfoText = findViewById(R.id.file_info);
        radioGroupColors = findViewById(R.id.radioGroupColors);
        saveButton = findViewById(R.id.save_button);
        firestore = FirebaseFirestore.getInstance();

        ivBack.setOnClickListener(v -> finish());

        // default warna radio button
        radioGroupColors.check(R.id.rb_blue);

        // pilih file
        documentPicker.setOnClickListener(v -> openFilePicker());

        // simpan tombol
        saveButton.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "Judul harus diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            if (fileUri == null) {
                Toast.makeText(this, "Pilih file terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadToCloudinary(fileUri, title, getSelectedColor());
        });

        // update warna
        radioGroupColors.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_blue) selectedColor = "biru";
            else if (checkedId == R.id.rb_orange) selectedColor = "oranye";
            else if (checkedId == R.id.rb_pink) selectedColor = "pink";
            else if (checkedId == R.id.rb_purple) selectedColor = "ungu";
        });

        // Notifbar warna pink
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.card_pink));
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(Intent.createChooser(intent, "Pilih dokumen"));
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
        return selectedColor;
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
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void uploadToCloudinary(Uri fileUri, String title, String color) {
        Toast.makeText(this, "Mengupload dokumen...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(fileUri)
                .unsigned("pam-project")
                .option("resource_type", "raw")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = (String) resultData.get("secure_url");
                        saveToFirestore(title, fileUrl, color);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AddDocumentActivity.this, "Upload gagal: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirestore(String title, String fileUrl, String color) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> documentNote = new HashMap<>();
        documentNote.put("title", title);
        documentNote.put("fileUrl", fileUrl);
        documentNote.put("color", color);
        documentNote.put("timestamp", Timestamp.now());

        firestore.collection("users")
                .document(uid)
                .collection("documents")
                .add(documentNote)
                .addOnSuccessListener((DocumentReference docRef) -> {
                    // tulis juga ID dokumen ke dalam field "id"
                    docRef.update("id", docRef.getId());
                    Toast.makeText(
                            this,
                            "Dokumen berhasil disimpan",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Gagal simpan dokumen: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }
}
