package com.example.projectpamcoba1.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.projectpamcoba1.DocumentDetailActivity;
import com.example.projectpamcoba1.R;
import com.example.projectpamcoba1.DocumentNote;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.*;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {

    private final Context context;
    private final List<DocumentNote> documentList;

    public DocumentAdapter(Context context, List<DocumentNote> documentList) {
        this.context = context;
        this.documentList = documentList;
    }

    @NonNull
    @Override
    public DocumentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentAdapter.ViewHolder holder, int position) {
        DocumentNote note = documentList.get(position);

        holder.tvDocumentTitle.setText(note.getTitle());

        // Format tanggal dari Timestamp Firestore
        if (note.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            String formattedDate = sdf.format(note.getTimestamp().toDate());
            holder.tvDocumentDate.setText(formattedDate);
        } else {
            holder.tvDocumentDate.setText("-");
        }

        // Gambar ikon dokumen (static)
        Glide.with(context)
                .load(R.drawable.ic_document_purple)
                .into(holder.ivDocumentIcon);

        // Aksi klik → buka detail dokumen
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DocumentDetailActivity.class);
            intent.putExtra("title", note.getTitle());
            intent.putExtra("fileUrl", note.getFileUrl());
            intent.putExtra("color", note.getColor());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDocumentTitle, tvDocumentDate;
        ShapeableImageView ivDocumentIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocumentTitle = itemView.findViewById(R.id.tvDocumentTitle);
            tvDocumentDate = itemView.findViewById(R.id.tvDocumentDate);
            ivDocumentIcon = itemView.findViewById(R.id.ivDocumentIcon);
        }
    }
}
