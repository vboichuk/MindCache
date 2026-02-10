package com.myapp.mindcache.mappers;

import com.myapp.mindcache.model.FeedItem;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public abstract class NodeMapper {
    public static FeedItem toFeed(Note note) {
        return FeedItem.of(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(note.getCreatedAt()),
                        ZoneId.systemDefault()),
                getEmojiForNote(note)
        );
    }

    public static FeedItem toFeed(NoteMetadata metadata) {
        return FeedItem.of(
                metadata.id,
                metadata.titleHint,
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(metadata.createdAt),
                        ZoneId.systemDefault())
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
