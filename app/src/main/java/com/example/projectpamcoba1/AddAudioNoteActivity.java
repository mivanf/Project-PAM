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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

        // Inisialisasi komponen UI
        etJudulAudio = findViewById(R.id.editTextTitle);
        ivBack = findViewById(R.id.iv_back);
        audioPicker = findViewById(R.id.audioPicker);
        btnSimpanAudio = findViewById(R.id.save_button);
        tvAudioFilename = findViewById(R.id.name_file);
        colorRadioGroup = findViewById(R.id.radioGroupColors);

        // Inisialisasi Firestore dan Cloudinary
        firestore = FirebaseFirestore.getInstance();
        CloudinaryManager.init(this);

        // Listener saat memilih audio
        audioPicker.setOnClickListener(v -> pilihFileAudio());

        // Listener untuk menyimpan audio note
        btnSimpanAudio.setOnClickListener(v -> simpanAudioNote());

        // Listener perubahan pilihan warna
        colorRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Menyesuaikan nilai selectedColor berdasarkan radio button yang dipilih
                if (checkedId == R.id.rb_blue) {
                    selectedColor = "Biru";
                } else if (checkedId == R.id.rb_orange) {
                    selectedColor = "Oranye";
                } else if (checkedId == R.id.rb_pink) {
                    selectedColor = "Pink";
                } else if (checkedId == R.id.rb_purple) {
                    selectedColor = "Ungu";
                }
            }
        });

        // Tombol kembali ke halaman sebelumnya
        ivBack.setOnClickListener(v -> onBackPressed());
    }

    // Method untuk memproses hasil pemilihan file audio dari galeri
    private final ActivityResultLauncher<Intent> audioPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Ambil URI dari audio yang dipilih
                    audioUri = result.getData().getData();
                    // Ambil dan tampilkan nama file audio
                    String fileName = getFileName(audioUri);
                    tvAudioFilename.setText(fileName);
                }
            });

    // Method untuk membuka intent pemilihan file audio
    private void pilihFileAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        audioPickerLauncher.launch(Intent.createChooser(intent, "Pilih File Audio"));
    }

    // Method untuk mendapatkan nama file dari URI audio
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
        // Jika tidak ditemukan, gunakan path URI
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    // Method untuk menyimpan audio note (upload ke Cloudinary dan simpan metadata ke Firestore)
    private void simpanAudioNote() {
        String judul = etJudulAudio.getText().toString().trim();

        // Validasi input judul
        if (judul.isEmpty()) {
            etJudulAudio.setError("Judul harus diisi");
            etJudulAudio.requestFocus();
            return;
        }

        // Validasi apakah audio sudah dipilih
        if (audioUri == null) {
            Toast.makeText(this, "Pilih file audio terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Mengupload audio...", Toast.LENGTH_SHORT).show();

        // Upload file audio ke Cloudinary (sebagai resource_type video)
        MediaManager.get().upload(audioUri)
                .unsigned("pam-project")
                .option("resource_type", "video")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    // Jika upload berhasil, ambil URL dan simpan metadata ke Firestore
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String audioUrl = (String) resultData.get("secure_url");
                        simpanKeFirestore(judul, audioUrl);
                    }

                    // Jika terjadi error saat upload
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AddAudioNoteActivity.this, "Upload gagal: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    // Method untuk menyimpan data audio note ke Firebase Firestore
    private void simpanKeFirestore(String judul, String audioUrl) {
        // Ambil UID user yang sedang login
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Buat data map yang berisi informasi note
        Map<String, Object> dataNote = new HashMap<>();
        dataNote.put("judul", judul);
        dataNote.put("audioUrl", audioUrl);
        dataNote.put("type", "audio");

        // Format tanggal dan waktu saat ini
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault());
        String tanggalSekarang = sdf.format(new Date());
        dataNote.put("tanggal", tanggalSekarang);

        // Simpan warna yang dipilih
        dataNote.put("color", selectedColor);

        // Simpan data ke koleksi Firestore: users/{uid}/notes
        firestore.collection("users")
                .document(uid)
                .collection("notes")
                .add(dataNote)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Audio note berhasil disimpan", Toast.LENGTH_SHORT).show();
                    finish(); // Kembali ke halaman sebelumnya
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal menyimpan data: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}