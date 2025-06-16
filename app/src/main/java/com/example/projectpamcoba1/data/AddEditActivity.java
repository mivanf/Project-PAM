package com.example.projectpamcoba1.data;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.projectpamcoba1.R;

public class AddEditActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;

    private CheckBox cbSelesai, cbBelum;
    private Button btnSimpan, btnHapus;
    private LinearLayout audioPicker;
    private TextView nameFile;
    private EditText editTitle;
    private Uri selectedFileUri;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        cbSelesai = findViewById(R.id.cbSelesai);
        cbBelum = findViewById(R.id.cbBelum);
        btnSimpan = findViewById(R.id.btnSimpan);
        btnHapus = findViewById(R.id.btnHapus);
        audioPicker = findViewById(R.id.audioPicker);
        nameFile = findViewById(R.id.name_file);
        editTitle = findViewById(R.id.editTitle);
        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        cbSelesai.setOnClickListener(v -> cbBelum.setChecked(!cbSelesai.isChecked()));
        cbBelum.setOnClickListener(v -> cbSelesai.setChecked(!cbBelum.isChecked()));

        btnSimpan.setOnClickListener(v -> showSaveDialog());
        btnHapus.setOnClickListener(v -> showDeleteDialog());

        audioPicker.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Pilih File"), PICK_FILE_REQUEST);
        });

        // Set title jika datang dari MainActivity
        String titleFromIntent = getIntent().getStringExtra("title");
        if (titleFromIntent != null) {
            editTitle.setText(titleFromIntent);
        } else {
            editTitle.setHint("Masukkan judul"); // Set hint untuk judul baru
        }
    }


    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_save, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnSimpan = view.findViewById(R.id.btnSimpan);
        Button btnBatal = view.findViewById(R.id.btnBatal);

        btnSimpan.setOnClickListener(v -> {
            String title = editTitle.getText().toString();
            boolean isDone = cbSelesai.isChecked();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("title", title);
            resultIntent.putExtra("isDone", isDone);

            if (selectedFileUri != null) {
                resultIntent.putExtra("fileUri", selectedFileUri.toString());
            }

            setResult(RESULT_OK, resultIntent);
            dialog.dismiss();
            finish();
        });

        btnBatal.setOnClickListener(v -> dialog.dismiss());
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
            String title = editTitle.getText().toString();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("deletedTitle", title);
            setResult(RESULT_OK, resultIntent);
            dialog.dismiss();
            finish();
        });

        btnBatal.setOnClickListener(v -> dialog.dismiss());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            String fileName = getFileName(selectedFileUri);
            nameFile.setText(fileName);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
