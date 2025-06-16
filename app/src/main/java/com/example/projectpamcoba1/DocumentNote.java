package com.example.projectpamcoba1;

import com.google.firebase.Timestamp;

public class DocumentNote {
    private String id;
    private String title;
    private String fileUrl;
    private Timestamp timestamp;
    private String color;

    public DocumentNote() {}

    public DocumentNote(String id, String title, String fileUrl, Timestamp timestamp, String color) {
        this.id = id;
        this.title = title;
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
        this.color = color;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public String getFileUrl() { return fileUrl; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getColor() { return color; }
}
