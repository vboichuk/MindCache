package com.myapp.mindcache.ui.diary;

import java.time.LocalDateTime;

public class FeedItem {
    private LocalDateTime localDateTime;
    private String emoji;
    private String title;
    private String content; // Все 3 строки объединены

    public FeedItem(LocalDateTime date, String emoji, String title, String content) {
        this.localDateTime = date;
        this.emoji = emoji;
        this.title = title;
        this.content = content;
    }

    // Геттеры
    public LocalDateTime getLocalDateTime() { return localDateTime; }
    public String getEmoji() { return emoji; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}
