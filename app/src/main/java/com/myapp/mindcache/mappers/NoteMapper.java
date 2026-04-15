package com.myapp.mindcache.mappers;

import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;


public final class NoteMapper {

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

    public static NotePreview toPreview(Note note, String preview) {
        return new NotePreview(
                note.getId(),
                note.getTitle(),
                preview,
                note.getCreatedAt()
        );
    }

    public static Note fromDto(NoteCreateDto dto) {
        return new Note(
                dto.getTitle(),
                dto.getContent(),
                dto.getCreatedAt());
    }
}
