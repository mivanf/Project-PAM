package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

//import android.net.Uri;
//import android.os.Bundle;
//
//import com.bumptech.glide.Glide;
//import com.cloudinary.Transformation;
//import com.cloudinary.android.MediaManager;
//import com.cloudinary.android.callback.ErrorInfo;
//import com.cloudinary.android.callback.UploadCallback;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.util.Log;
//
//import com.cloudinary.cloudinaryquickstart.databinding.ActivityMainBinding;
//
//import java.util.HashMap;
//import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Cek apakah user sudah login
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Set default fragment
        loadFragment(new HomeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                fragment = new HomeFragment();
//            } else if (itemId == R.id.navigation_add) {
//                fragment = new AddFragment();
            } else if (itemId == R.id.navigation_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }

//            if (itemId == R.id.navigation_add) {
//                startActivity(new Intent(MainActivity.this, AddNotesActivity.class));
//                return true;
//            }

            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Set kembali ke NotesFragment dan highlight ikon Notes setelah kembali dari AddActivity
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }
}
