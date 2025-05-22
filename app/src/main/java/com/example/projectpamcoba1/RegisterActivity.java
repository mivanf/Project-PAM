package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNama, etEmail, etKataSandi, etKonfirmasiKataSandi;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Inisialisasi Firestore

        etNama = findViewById(R.id.etNama);
        etEmail = findViewById(R.id.etEmail);
        etKataSandi = findViewById(R.id.etKataSandi);
        etKonfirmasiKataSandi = findViewById(R.id.etKonfirmasiKataSandi);
        btnRegister = findViewById(R.id.btnRegister);
        textView = findViewById(R.id.loginNow);
        ImageView ivToggleKataSandi = findViewById(R.id.ivToggleKataSandi);
        ImageView ivToggleKonfirmasiKataSandi = findViewById(R.id.ivToggleKonfirmasiKataSandi);

        ivToggleKataSandi.setOnClickListener(v -> {
            if (etKataSandi.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Jika password tersembunyi, tampilkan
                etKataSandi.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivToggleKataSandi.setImageResource(R.drawable.ic_visibility); // Ganti icon
            } else {
                // Jika password terlihat, sembunyikan
                etKataSandi.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleKataSandi.setImageResource(R.drawable.ic_visibility_off); // Ganti icon
            }
            etKataSandi.setSelection(etKataSandi.length()); // Memastikan kursor tetap di akhir teks
        });

        ivToggleKonfirmasiKataSandi.setOnClickListener(v -> {
            if (etKonfirmasiKataSandi.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Jika password tersembunyi, tampilkan
                etKonfirmasiKataSandi.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivToggleKonfirmasiKataSandi.setImageResource(R.drawable.ic_visibility); // Ganti icon
            } else {
                // Jika password terlihat, sembunyikan
                etKonfirmasiKataSandi.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleKonfirmasiKataSandi.setImageResource(R.drawable.ic_visibility_off); // Ganti icon
            }
            etKonfirmasiKataSandi.setSelection(etKonfirmasiKataSandi.length()); // Memastikan kursor tetap di akhir teks
        });

        textView.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left); // Animasi perpindahan
            finish();
        });

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = etNama.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etKataSandi.getText().toString().trim();
        String confirmPassword = etKonfirmasiKataSandi.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            String userId = user.getUid();
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("name", name);
                                            userData.put("email", email);

                                            db.collection("users").document(userId)
                                                    .set(userData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(RegisterActivity.this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(RegisterActivity.this, "Gagal menyimpan data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Gagal memperbarui profil pengguna.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registrasi gagal: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
