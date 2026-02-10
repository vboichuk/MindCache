package com.myapp.mindcache.mappers;

import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;


public abstract class NodeMapper {

    public static NotePreview toPreview(Note note) {
        NotePreview notePreview = NotePreview.of(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(note.getCreatedAt()),
                        ZoneId.systemDefault()),
                getEmojiForNote(note)
        );
        notePreview.setSecret(note.isSecret());
        return notePreview;
    }

    public static NotePreview toPreview(NoteMetadata metadata) {
        NotePreview notePreview = NotePreview.of(
                metadata.id,
                Optional.ofNullable(metadata.titleHint).orElse("Secret note"),
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(metadata.createdAt),
                        ZoneId.systemDefault())
        );
        notePreview.setSecret(metadata.isSecret);
        return notePreview;
    }

    public static Note fromDto(NoteCreateDto dto) {
        return new Note(
                dto.getTitle(),
                dto.getContent(),
                dto.getCreatedAt(),
                dto.isSecret());
    }

    private static String getEmojiForNote(Note note) {
        String lowerTitle = note.getTitle().toLowerCase();
        if (lowerTitle.contains("важно")) return "❗";
        if (lowerTitle.contains("идея")) return "💡";
        if (lowerTitle.contains("задача")) return "✅";
        if (lowerTitle.contains("мысли")) return "💭";
        if (lowerTitle.contains("мысль")) return "💭";
        return "📘";
    }
}
