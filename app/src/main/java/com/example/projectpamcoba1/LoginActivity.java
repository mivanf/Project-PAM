package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etKataSandi;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private ImageView ivTogglePassword;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etKataSandi = findViewById(R.id.etKataSandi);
        btnLogin = findViewById(R.id.btnLogin);
        textView = findViewById(R.id.registerNow);
        etKataSandi = findViewById(R.id.etKataSandi);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        textView.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // Animasi perpindahan
            finish();
        });

        btnLogin.setOnClickListener(v -> loginUser());

        ivTogglePassword.setOnClickListener(v -> {
            if (etKataSandi.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Jika password tersembunyi, tampilkan
                etKataSandi.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_visibility); // Ganti icon
            } else {
                // Jika password terlihat, sembunyikan
                etKataSandi.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off); // Ganti icon
            }
            etKataSandi.setSelection(etKataSandi.length()); // Memastikan kursor tetap di akhir teks
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String kataSandi = etKataSandi.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(kataSandi)) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, kataSandi)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Login gagal: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
