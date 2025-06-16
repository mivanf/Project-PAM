package com.example.projectpamcoba1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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

        // Set warna background sesuai color
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
            default:
                holder.container.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_biru));
                break;
        }

        // Menampilkan gambar cover menggunakan Glide dari URL Cloudinary
        String imageUrl = note.getImagePath(); // imagePath berisi URL Cloudinary
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(getDefaultImageByColor(note.getColor())) // placeholder saat loading
                    .error(getDefaultImageByColor(note.getColor()))       // fallback jika gagal load
                    .into(holder.cover);
        } else {
            // Jika kosong, pakai gambar default berdasarkan warna
            holder.cover.setImageResource(getDefaultImageByColor(note.getColor()));
        }

        // Klik item untuk buka detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailNoteActivity.class);
            intent.putExtra("note", note);
            intent.putExtra("title", note.getTitle());
            context.startActivity(intent);
        });
    }

    private int getDefaultImageByColor(String color) {
        switch (color) {
            case "oranye":
                return R.drawable.ic_default_orange;
            case "pink":
                return R.drawable.ic_default_pink;
            case "ungu":
                return R.drawable.ic_default_purple;
            case "biru":
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