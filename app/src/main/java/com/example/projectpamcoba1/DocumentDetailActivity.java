package com.example.projectpamcoba1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class DocumentDetailActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etTitle;
    private CardView cvDocument;
    private TextView tvFilename;
    private AppCompatButton btnOpen, btnUpdate, btnDelete;
    private RadioGroup rgColors;
    private FirebaseFirestore db;
    private String userId, documentId, fileUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_document);

        // init Firestore & Auth
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // bind views
        ivBack      = findViewById(R.id.iv_back);
        etTitle     = findViewById(R.id.et_title);
        cvDocument  = findViewById(R.id.cv_document);
        tvFilename  = findViewById(R.id.tv_filename);
        btnOpen     = findViewById(R.id.btn_open);
        btnUpdate   = findViewById(R.id.btn_update);
        btnDelete   = findViewById(R.id.btn_delete);
        rgColors    = findViewById(R.id.rg_colors);

        // ambil data dari Intent
        Intent i = getIntent();
        documentId   = i.getStringExtra("documentId");
        etTitle.setText(i.getStringExtra("title"));
        fileUrl       = i.getStringExtra("fileUrl");
        String color  = i.getStringExtra("color");

        // tampilkan nama file
        tvFilename.setText(extractFileName(fileUrl));

        // set radio sesuai warna
        for (int idx = 0; idx < rgColors.getChildCount(); idx++) {
            View v = rgColors.getChildAt(idx);
            if (v instanceof RadioButton &&
                    ((RadioButton)v).getText().toString().equalsIgnoreCase(color)) {
                ((RadioButton)v).setChecked(true);
                break;
            }
        }

        // back
        ivBack.setOnClickListener(v -> finish());

        // unduh dokumen
        btnOpen.setOnClickListener(v -> {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl)));
            } else {
                Toast.makeText(this, "URL dokumen tidak tersedia", Toast.LENGTH_SHORT).show();
            }
        });

        // update judul & warna
        btnUpdate.setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            if (newTitle.isEmpty()) {
                etTitle.setError("Judul wajib diisi");
                return;
            }
            int selId = rgColors.getCheckedRadioButtonId();
            if (selId == -1) {
                Toast.makeText(this, "Pilih warna dokumen", Toast.LENGTH_SHORT).show();
                return;
            }
            String newColor = ((RadioButton)findViewById(selId)).getText().toString();

            Map<String,Object> upd = new HashMap<>();
            upd.put("title", newTitle);
            upd.put("color", newColor);
            // fileUrl tidak berubah
            upd.put("fileUrl", fileUrl);

            db.collection("users")
                    .document(userId)
                    .collection("documents")
                    .document(documentId)
                    .update(upd)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Dokumen diperbarui", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // delete
        btnDelete.setOnClickListener(v -> {
            db.collection("users")
                    .document(userId)
                    .collection("documents")
                    .document(documentId)
                    .delete()
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Dokumen dihapus", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal hapus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private String extractFileName(String url) {
        if (url == null) return "";
        Uri uri = Uri.parse(url);
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("?")) {
            // fallback ke query
            name = uri.getQueryParameter("filename");
        }
        return name != null ? name : "dokumen";
    }
}
