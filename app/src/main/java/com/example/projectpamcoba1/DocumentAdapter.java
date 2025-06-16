package com.example.projectpamcoba1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectpamcoba1.DocumentDetailActivity;
import com.example.projectpamcoba1.DocumentNote;
import com.example.projectpamcoba1.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {

    private final Context context;
    private final List<DocumentNote> documentList;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public DocumentAdapter(Context context, List<DocumentNote> documentList) {
        this.context = context;
        this.documentList = documentList;
    }

    @NonNull
    @Override
    public DocumentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentAdapter.ViewHolder holder, int position) {
        DocumentNote note = documentList.get(position);

        // 1. Judul
        holder.tvDocumentTitle.setText(note.getTitle());

        // 2. Tanggal (pakai Timestamp → Date)
        String dateText = "";
        Timestamp ts = note.getTimestamp();
        if (ts != null) {
            Date d = ts.toDate();
            dateText = dateFormat.format(d);
        }
        holder.tvDocumentDate.setText(dateText);

        // 3. Background sesuai warna
        int bgColorRes = getBackgroundColorRes(note.getColor());
        holder.cardLayout.setBackgroundColor(
                ContextCompat.getColor(context, bgColorRes)
        );

        // 4. Ikon sesuai warna
        int iconRes = getIconRes(note.getColor());
        holder.ivDocumentIcon.setImageResource(iconRes);

        // 5. Klik → buka Detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DocumentDetailActivity.class);
            intent.putExtra("documentId", note.getId());
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

    private int getBackgroundColorRes(String color) {
        if (color == null) color = "";
        switch (color.toLowerCase()) {
            case "oranye":
            case "orange":
                return R.color.bg_card_orange;
            case "pink":
                return R.color.bg_card_pink;
            case "ungu":
            case "purple":
                return R.color.bg_card_purple;
            default:
                return R.color.bg_card_blue;
        }
    }

    private int getIconRes(String color) {
        if (color == null) color = "";
        switch (color.toLowerCase()) {
            case "oranye":
            case "orange":
                return R.drawable.ic_document_orange;
            case "pink":
                return R.drawable.ic_document_pink;
            case "ungu":
            case "purple":
                return R.drawable.ic_document_purple;
            default:
                return R.drawable.ic_document_blue;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivDocumentIcon;
        TextView tvDocumentTitle, tvDocumentDate;
        LinearLayout cardLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDocumentIcon  = itemView.findViewById(R.id.ivDocumentIcon);
            tvDocumentTitle = itemView.findViewById(R.id.tvDocumentTitle);
            tvDocumentDate  = itemView.findViewById(R.id.tvDocumentDate);
            cardLayout      = itemView.findViewById(R.id.card_document_layout);
        }
    }
}
