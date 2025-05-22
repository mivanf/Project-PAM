package com.example.projectpamcoba1;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context context;
    private List<Note> noteList;

    public NoteAdapter(Context context, List<Note> noteList) {
        this.context = context;
        this.noteList = noteList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.title.setText(note.getTitle());
        holder.date.setText(note.getDate());

        // Warna background berdasarkan nama warna
        switch (note.getColor()) {
            case "oranye":
                holder.container.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_oranye));
                break;
            case "pink":
                holder.container.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_pink));
                break;
            case "ungu":
                holder.container.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_ungu));
                break;
            case "biru":
                holder.container.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_biru));
                break;
            default:
                holder.container.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
                break;
        }

        // Menampilkan gambar
        String imagePath = note.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            // Jika cover default berdasarkan warna
            if (imagePath.startsWith("default_")) {
                holder.cover.setImageResource(getDefaultImageByColor(note.getColor()));
            } else {
                // Jika gambar cover yang diupload pengguna
                File imgFile = new File(imagePath);
                if (imgFile.exists()) {
                    holder.cover.setImageDrawable(Drawable.createFromPath(imgFile.getAbsolutePath()));
                } else {
                    // Jika gagal ditemukan, gunakan default
                    holder.cover.setImageResource(getDefaultImageByColor(note.getColor()));
                }
            }
        } else {
            // Jika cover kosong
            holder.cover.setImageResource(getDefaultImageByColor(note.getColor()));
        }

        // Klik ke detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailNoteActivity.class);
            intent.putExtra("note", note); // note = objek Note
            intent.putExtra("title", note.getTitle()); // Mengirim title ke DetailNoteActivity
            context.startActivity(intent);
        });
    }

    // Menentukan gambar default sesuai warna background
    private int getDefaultImageByColor(String color) {
        switch (color) {
            case "oranye":
                return R.drawable.ic_default_orange;
            case "pink":
                return R.drawable.ic_default_pink;
            case "ungu":
                return R.drawable.ic_default_purple;
            case "biru":
                return R.drawable.ic_default_blue;
            default:
                return R.drawable.ic_default_blue;
        }
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;
        ImageView cover;
        View container;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvNoteTitle);
            date = itemView.findViewById(R.id.tvNoteDate);
            cover = itemView.findViewById(R.id.ivNoteCover);
            container = itemView.findViewById(R.id.card_layout);
        }
    }
}