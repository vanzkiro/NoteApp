package com.example.noteapp;

public class Plan {
    private int id;
    private String title;
    private String content;
    private long startTime;
    private long endTime;
    private boolean isCompleted;
    private int reminderHour;
    private int reminderMinute;
    private String category;
    private int progress;
    private String imagePath;

    public Plan(int id, String title, String content, long startTime, long endTime, boolean isCompleted, int reminderHour, int reminderMinute, String category, int progress, String imagePath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isCompleted = isCompleted;
        this.reminderHour = reminderHour;
        this.reminderMinute = reminderMinute;
        this.category = category;
        this.progress = progress;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public boolean isCompleted() { return isCompleted; }
    public int getReminderHour() { return reminderHour; }
    public int getReminderMinute() { return reminderMinute; }
    public String getCategory() { return category; }
    public int getProgress() { return progress; }
    public String getImagePath() { return imagePath; }

    @Override
    public String toString() {
        return title;
    }
}