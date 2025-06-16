package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddNoteActivity extends AppCompatActivity {

    private EditText titleEditText, contentEditText;
    private TextView dateTextView, wordCountTextView;
    private String selectedColor = "biru"; // Default
    private FirebaseFirestore db;
    private RadioGroup colorRadioGroup;
    private SimpleDateFormat dateFormat;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        ivBack = findViewById(R.id.iv_back);
        titleEditText = findViewById(R.id.et_judul);
        contentEditText = findViewById(R.id.et_isi_note);
        dateTextView = findViewById(R.id.tv_tanggal);
        wordCountTextView = findViewById(R.id.tv_word_count);
        colorRadioGroup = findViewById(R.id.radioGroupColors);

        db = FirebaseFirestore.getInstance();

        // Menampilkan tanggal saat ini
        dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        dateTextView.setText(currentDate);

        // Mengupdate jumlah kata secara dinamis
        contentEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
                int wordCount = charSequence.toString().split("\\s+").length;
                wordCountTextView.setText(wordCount + "/5000 Kata");
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {}
        });

        // Memilih warna notes
        colorRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Pastikan menggunakan ID sesuai dengan komponen yang ada di XML
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

        // Tombol Lanjut ke AddCoverActivity
        findViewById(R.id.btn_selanjutnya).setOnClickListener(v -> goToAddCover());

        // Tombol kembali
        ivBack.setOnClickListener(v -> onBackPressed());

        // Notifbar warna biru
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.notes_bar));

    }

    // Mengirim data ke AddCoverActivity
    private void goToAddCover() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty()) {
            titleEditText.setError("Judul wajib diisi");
            return;
        }

        Intent intent = new Intent(this, AddCoverActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("color", selectedColor);
        intent.putExtra("date", dateFormat.format(new Date()));
        startActivity(intent);
    }
}