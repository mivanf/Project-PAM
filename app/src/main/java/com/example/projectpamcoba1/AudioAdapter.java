package com.example.projectpamcoba1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.AudioViewHolder> {

    private Context context;
    private List<Audio> audioList;

    public AudioAdapter(Context context, List<Audio> audioList) {
        this.context = context;
        this.audioList = audioList;
    }

    @Override
    public AudioViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audio, parent, false);
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AudioViewHolder holder, int position) {
        Audio audio = audioList.get(position);
        holder.tvAudioTitle.setText(audio.getTitle());
        holder.tvAudioDate.setText(audio.getDate());

        // Set gambar default berdasarkan warna audio
        int defaultImageRes = getDefaultImageByColor(audio.getColor());
        holder.ivAudioCover.setImageResource(defaultImageRes);

        // Aksi saat item diklik
        holder.audioContainer.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailAudioActivity.class);
            intent.putExtra("audio_id", audio.getId());
            intent.putExtra("audioUrl", audio.getAudioUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

    // Fungsi untuk menentukan gambar default berdasarkan warna
    private int getDefaultImageByColor(String color) {
        if (color == null) return R.drawable.ic_music_purple;

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

    public static class AudioViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAudioCover;
        TextView tvAudioTitle, tvAudioDate;
        CardView audioContainer;

        public AudioViewHolder(View itemView) {
            super(itemView);
            ivAudioCover = itemView.findViewById(R.id.ivAudioCover);
            tvAudioTitle = itemView.findViewById(R.id.tvAudioTitle);
            tvAudioDate = itemView.findViewById(R.id.tvAudioDate);
            audioContainer = itemView.findViewById(R.id.audioContainer);
        }
    }
}
