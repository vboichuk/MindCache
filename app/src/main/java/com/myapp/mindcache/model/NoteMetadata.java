package com.myapp.mindcache.model;


import androidx.room.Ignore;

import java.text.DateFormat;
import java.util.Date;

public class NoteMetadata {
    public final Long id;

    @Ignore
    public final boolean isEncrypted = true;
    public final Long createdAt;

    @Ignore
    public String titleHint;

    public NoteMetadata(Long id, Long createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.titleHint = "Заметка " + id;
    }

    public String getFormattedDate() {
        return DateFormat.getDateInstance().format(new Date(createdAt));
    }
}
