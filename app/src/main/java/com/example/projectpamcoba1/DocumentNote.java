package com.example.projectpamcoba1;

import com.google.firebase.Timestamp;

public class DocumentNote {
    private String title;
    private String description;
    private String fileUrl;
    private Timestamp timestamp;

    public DocumentNote() {}

    public DocumentNote(String title, String description, String fileUrl, Timestamp timestamp) {
        this.title = title;
        this.description = description;
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFileUrl() { return fileUrl; }
    public Timestamp getTimestamp() { return timestamp; }
}
