package com.example.projectpamcoba1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectpamcoba1.DocumentNote;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.*;

public class AddDocumentActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;

    private EditText editTextTitle;
    private LinearLayout documentPicker;
    private TextView nameFileText, fileInfoText;
    private RadioGroup radioGroupColors;
    private Button saveButton;

    private Uri fileUri;
    private String selectedColor = "biru"; // default
    private final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/dsi0bqoc8/upload";
    private final String UPLOAD_PRESET = "pam_unsigned";
    private final String userId = FirebaseAuth.getInstance().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        editTextTitle = findViewById(R.id.editTextTitle);
        documentPicker = findViewById(R.id.documentPicker);
        nameFileText = findViewById(R.id.name_file);
        fileInfoText = findViewById(R.id.file_info);
        radioGroupColors = findViewById(R.id.radioGroupColors);
        saveButton = findViewById(R.id.save_button);

        // handle warna default
        radioGroupColors.check(R.id.rb_blue);

        documentPicker.setOnClickListener(v -> openFileChooser());
        saveButton.setOnClickListener(v -> {
            if (fileUri != null) {
                uploadToCloudinary(fileUri,
                        editTextTitle.getText().toString(),
                        getSelectedColor());
            } else {
                Toast.makeText(this, "Silakan pilih file dokumen", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Pilih Dokumen"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();
            nameFileText.setText("Dokumen dipilih");
            fileInfoText.setText(fileUri.getLastPathSegment());
        }
    }

    private String getSelectedColor() {
        int selectedId = radioGroupColors.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_blue) return "biru";
        if (selectedId == R.id.rb_purple) return "ungu";
        if (selectedId == R.id.rb_pink) return "pink";
        if (selectedId == R.id.rb_orange) return "oranye";
        return "biru"; // default
    }

    private void uploadToCloudinary(Uri fileUri, String title, String color) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            byte[] fileBytes = IOUtils.toByteArray(inputStream);
            String encodedFile = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "data:application/octet-stream;base64," + encodedFile)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url(CLOUDINARY_UPLOAD_URL)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(AddDocumentActivity.this, "Upload gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> Toast.makeText(AddDocumentActivity.this, "Upload gagal: " + response.message(), Toast.LENGTH_SHORT).show());
                        return;
                    }

                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String fileUrl = json.getString("secure_url");

                        saveToFirestore(title, fileUrl, color);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToFirestore(String title, String fileUrl, String color) {
        DocumentNote note = new DocumentNote(title, fileUrl, Timestamp.now(), color);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("documents")
                .add(note)
                .addOnSuccessListener(docRef ->
                        runOnUiThread(() ->
                                Toast.makeText(this, "Dokumen berhasil disimpan", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e ->
                        runOnUiThread(() ->
                                Toast.makeText(this, "Gagal simpan Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
    }
}
