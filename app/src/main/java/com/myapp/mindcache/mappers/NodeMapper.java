package com.myapp.mindcache.mappers;

import com.myapp.mindcache.model.FeedItem;
import com.myapp.mindcache.model.Note;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public abstract class NodeMapper {
    public static FeedItem toFeed(Note note) {
        return new FeedItem(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(note.getCreatedAt()),
                        ZoneId.systemDefault()),
                getEmojiForNote(note)
        );
    }

    private static String getEmojiForNote(Note note) {
        String lowerTitle = note.getTitle().toLowerCase();
        if (lowerTitle.contains("важно")) return "❗";
        if (lowerTitle.contains("идея")) return "💡";
        if (lowerTitle.contains("задача")) return "✅";
        if (lowerTitle.contains("мысли")) return "💭";
        return "📘";
    }
}
