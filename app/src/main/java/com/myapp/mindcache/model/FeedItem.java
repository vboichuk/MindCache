package com.myapp.mindcache.model;

import java.time.LocalDateTime;

public class FeedItem {
    private final long id;
    private final LocalDateTime dateTime;
    private String emoji;
    private String title;
    private String content; // Все 3 строки объединены
    private boolean isEncrypted = true;

    public FeedItem(long id, LocalDateTime dateTime) {
        this.id = id;
        this.dateTime = dateTime;
    }

    public static FeedItem of (long id, String title, String content, LocalDateTime dateTime, String emoji) {
        FeedItem feedItem = new FeedItem(id, dateTime);
        feedItem.title = title;
        feedItem.content = content;
        feedItem.emoji = emoji;
        feedItem.isEncrypted = false;
        return feedItem;
    }

    public static FeedItem of (long id, String titleHint, LocalDateTime dateTime) {
        FeedItem feedItem = new FeedItem(id, dateTime);
        feedItem.title = titleHint;
        feedItem.content = "";
        feedItem.emoji = "";
        return feedItem;
    }

    public long getId() { return id; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getEmoji() { return emoji; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isEncrypted() { return isEncrypted; }
}
