package com.example.projectpamcoba1;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {
    private FirebaseAuth mAuth;
    private TextView tv_name_notes;
    private CardView card1, card2, card3, card4;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        tv_name_notes = view.findViewById(R.id.tv_name_notes);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tv_name_notes.setText("Hola! " + user.getDisplayName());
        }

        // Inisialisasi card
        card1 = view.findViewById(R.id.card_1);
        card2 = view.findViewById(R.id.card_2);
        card3 = view.findViewById(R.id.card_3);
        card4 = view.findViewById(R.id.card_4);
        // Set listener untuk pindah ke NotesActivity
        card1.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotesActivity.class);
            startActivity(intent);
        });

        card2.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AudioActivity.class);
            startActivity(intent);
        });

        card3.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DocumentListActivity.class);
            startActivity(intent);
        });

        card4.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ToDoListActivity.class);
            startActivity(intent);
        });

        return view;
    }
}