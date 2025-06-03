package com.example.projectpamcoba1;

public class Audio {
    private String id;
    private String title;
    private String audioUrl;
    private String color;
    private String date;

    public Audio(String id, String title, String audioUrl, String color, String date) {
        this.id = id;
        this.title = title;
        this.audioUrl = audioUrl;
        this.color = color;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }

    public String getAudioUrl() { return audioUrl; }

    public String getColor() { return color; }

    public String getDate() { return date; }

    public void setColor(String color) { this.color = color; }
}
