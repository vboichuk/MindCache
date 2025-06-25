package com.myapp.mindcache.model;

import java.time.LocalDateTime;

public class FeedItem {


    private final long id;
    private LocalDateTime dateTime;
    private String emoji;
    private String title;
    private String content; // Все 3 строки объединены

    public FeedItem(long id, String title, String content, LocalDateTime dateTime, String emoji) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.dateTime = dateTime;
        this.emoji = emoji;
    }

    public long getId() { return id; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getEmoji() { return emoji; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}
