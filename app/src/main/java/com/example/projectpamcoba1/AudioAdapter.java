package com.example.projectpamcoba1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.AudioViewHolder> {

    private Context context;
    private List<Audio> audioList;

    // Konstruktor: menerima context dan daftar audio yang akan ditampilkan
    public AudioAdapter(Context context, List<Audio> audioList) {
        this.context = context;
        this.audioList = audioList;
    }

    // Membuat ViewHolder baru dari layout item_audio.xml
    @Override
    public AudioViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audio, parent, false);
        return new AudioViewHolder(view);
    }

    // Mengikat data audio ke setiap item dalam RecyclerView
    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        Audio audio = audioList.get(position);

        // Set teks judul dan tanggal
        holder.tvAudioTitle.setText(audio.getTitle());
        holder.tvAudioDate.setText(audio.getDate());

        // Mengatur warna latar belakang berdasarkan warna yang dipilih
        switch (audio.getColor()) {
            case "Oranye":
                holder.audioContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_oranye));
                break;
            case "Pink":
                holder.audioContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_pink));
                break;
            case "Biru":
                holder.audioContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_biru));
                break;
            case "Ungu":
            default:
                holder.audioContainer.setBackgroundColor(ContextCompat.getColor(context, R.color.warna_ungu));
                break;
        }

        // Menentukan URL cover atau menggunakan gambar default jika tidak ada URL
        String audioUrl = audio.getAudioUrl();
        int defaultImageRes = getDefaultImageByColor(audio.getColor());

        // Jika audio memiliki URL gambar, load menggunakan Glide. Jika tidak, pakai default
        if (audioUrl != null && !audioUrl.isEmpty()) {
            Glide.with(context)
                    .load(audioUrl)
                    .placeholder(defaultImageRes)
                    .error(defaultImageRes)
                    .into(holder.ivAudioCover);
        } else {
            holder.ivAudioCover.setImageResource(defaultImageRes);
        }

        // Klik pada item akan membuka DetailAudioActivity dan mengirim data melalui intent
        holder.audioContainer.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailAudioActivity.class);
            intent.putExtra("noteId", audio.getId());
            intent.putExtra("title", audio.getTitle());
            intent.putExtra("audioUrl", audio.getAudioUrl());
            intent.putExtra("color", audio.getColor());
            intent.putExtra("tanggal", audio.getDate());
            context.startActivity(intent);
        });
    }

    // Mengembalikan jumlah item dalam daftar audio
    @Override
    public int getItemCount() {
        return audioList.size();
    }

    // Fungsi untuk memilih gambar default berdasarkan warna (jika tidak ada gambar dari URL)
    private int getDefaultImageByColor(String color) {
        switch (color.toLowerCase()) {
            case "oranye":
                return R.drawable.ic_music_orange;
            case "pink":
                return R.drawable.ic_music_pink;
            case "biru":
                return R.drawable.ic_music_blue;
            case "ungu":
            default:
                return R.drawable.ic_music_purple;
        }
    }

    // ViewHolder: merepresentasikan komponen layout item_audio
    public static class AudioViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAudioCover;
        TextView tvAudioTitle, tvAudioDate;
        View audioContainer;

        // Konstruktor: menghubungkan elemen layout dengan variabel
        public AudioViewHolder(View itemView) {
            super(itemView);
            ivAudioCover = itemView.findViewById(R.id.ivAudioCover);
            tvAudioTitle = itemView.findViewById(R.id.tvAudioTitle);
            tvAudioDate = itemView.findViewById(R.id.tvAudioDate);
            audioContainer = itemView.findViewById(R.id.card_audio_layout);
        }
    }
}