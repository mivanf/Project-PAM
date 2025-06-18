package com.example.projectpamcoba1.data.model;

public class ToDoItem {

    private String title;
    private String date;
    private boolean isChecked;

    // Diperlukan oleh Firestore saat deserialisasi
    public ToDoItem() {
    }

    public ToDoItem(String title, String date, boolean isChecked) {
        this.title = title;
        this.date = date;
        this.isChecked = isChecked;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { this.isChecked = checked; }

    // Untuk mapping field 'isDone' dari Firestore ke 'isChecked'
    public void setDone(boolean isDone) {

        this.isChecked = isDone;
    }

    public boolean getDone() {
        return isChecked;
    }
}
