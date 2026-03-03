package com.myapp.mindcache.mappers;

import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.repositories.NoteRepository;


public abstract class NodeMapper {

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
                NoteRepository.getPreview(note.getContent()),
                note.getCreatedAt()
        );
    }

    public static Note fromDto(NoteCreateDto dto) {
        return new Note(
                dto.getTitle(),
                dto.getContent(),
                NoteRepository.getPreview(dto.getContent()),
                dto.getCreatedAt());
    }

    public static Note fromDto(NoteUpdateDto dto) {
        return new Note(
                dto.getId(),
                dto.getTitle(),
                dto.getContent(),
                dto.getCreatedAt());
    }
}
