package com.myapp.mindcache.mappers;

import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;


public final class NoteMapper {

    private static final int MAX_PREVIEW_LENGTH = 100;
    private static final String ELLIPSIS = "…";

    private NoteMapper() { }

    public static NotePreview toPreview(NoteMetadata metadata) {
        NotePreview notePreview = new NotePreview(
                metadata.id,
                "Secret note",
                "",
                metadata.createdAt);
        notePreview.setEncrypted(true);
        return notePreview;
    }

    public static NotePreview toPreview(Note note) {
        return new NotePreview(
                note.getId(),
                note.getTitle(),
                NoteMapper.generatePreview(note.getContent()),
                note.getCreatedAt()
        );
    }

    public static Note fromDto(NoteCreateDto dto) {
        return new Note(
                dto.getTitle(),
                dto.getContent(),
                dto.getCreatedAt());
    }

    public static String generatePreview(String content) {
        if (content == null || content.isBlank())
            return "";

        return content.length() <= MAX_PREVIEW_LENGTH
                ? content
                : content.substring(0, MAX_PREVIEW_LENGTH) + ELLIPSIS;
    }
}
