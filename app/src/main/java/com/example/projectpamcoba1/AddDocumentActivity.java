package com.example.projectpamcoba1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectpamcoba1.DocumentNote;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddDocumentActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 101;

    private EditText editTextTitle;
    private LinearLayout documentPicker;
    private TextView nameFile, fileInfo;
    private MaterialButton saveButton;

    private Uri fileUri;
    private String userId;

    private final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/dsi0bqoc8/upload";
    private final String UPLOAD_PRESET = "pam_unsigned";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        editTextTitle = findViewById(R.id.editTextTitle);
        documentPicker = findViewById(R.id.documentPicker);
        nameFile = findViewById(R.id.name_file);
        fileInfo = findViewById(R.id.file_info);
        saveButton = findViewById(R.id.save_button);

        userId = FirebaseAuth.getInstance().getUid();

        documentPicker.setOnClickListener(view -> openFileChooser());

        saveButton.setOnClickListener(view -> {
            String title = editTextTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show();
            } else if (fileUri == null) {
                Toast.makeText(this, "Pilih file dokumen terlebih dahulu", Toast.LENGTH_SHORT).show();
            } else {
                uploadToCloudinary(fileUri, title);
            }
        });

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Pilih Dokumen"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            String fileName = fileUri.getLastPathSegment();
            nameFile.setText(fileName);
            fileInfo.setText("File siap diunggah");
        }
    }

    private void uploadToCloudinary(Uri uri, String title) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] fileBytes = IOUtils.toByteArray(inputStream);
            String encoded = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

            OkHttpClient client = new OkHttpClient();

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "data:application/octet-stream;base64," + encoded)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url(CLOUDINARY_UPLOAD_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(AddDocumentActivity.this, "Upload gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> Toast.makeText(AddDocumentActivity.this, "Upload gagal: " + response.message(), Toast.LENGTH_SHORT).show());
                        return;
                    }

                    String res = response.body().string();
                    try {
                        JSONObject json = new JSONObject(res);
                        String fileUrl = json.getString("secure_url");

                        saveToFirestore(title, fileUrl);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membaca file", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFirestore(String title, String fileUrl) {
        DocumentNote note = new DocumentNote(title, "", fileUrl, Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("documents")
                .add(note)
                .addOnSuccessListener(ref -> runOnUiThread(() ->
                        Toast.makeText(this, "Dokumen berhasil disimpan", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> runOnUiThread(() ->
                        Toast.makeText(this, "Firestore gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
    }
}
