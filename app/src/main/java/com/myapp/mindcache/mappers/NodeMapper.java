package com.myapp.mindcache.mappers;

import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;

import java.util.Optional;


public abstract class NodeMapper {

    public static NotePreview toPreview(Note note) {
        return new NotePreview(
                note.getId(),
                note.getTitle(),
                note.getPreview(),
                note.getCreatedAt(),
                note.isSecret(),
                note.getSalt()
        );
    }

    public static NotePreview toPreview(NoteMetadata metadata) {
        NotePreview notePreview = new NotePreview(
                metadata.id,
                Optional.ofNullable(metadata.titleHint).orElse("Secret note"),
                "",
                metadata.createdAt,
                metadata.isSecret,
                null);
        notePreview.setEncrypted(metadata.isSecret);
        return notePreview;
    }

    public static Note fromDto(NoteCreateDto dto) {
        return new Note(
                dto.getTitle(),
                dto.getContent(),
                null,
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
