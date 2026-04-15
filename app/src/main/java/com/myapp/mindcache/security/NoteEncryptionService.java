package com.myapp.mindcache.security;

import androidx.annotation.NonNull;

import com.myapp.mindcache.model.EncryptedNote;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NotePreview;

import javax.crypto.SecretKey;

public class NoteEncryptionService {

    public String encrypt(String text, SecretKey masterKey) throws Exception {
        return CryptoHelper.encrypt(text, masterKey);
    }

    public EncryptedNote encryptNote(@NonNull Note note, String notePreview, SecretKey key) throws Exception {

        String title = CryptoHelper.encrypt(note.getTitle(), key);
        String content = CryptoHelper.encrypt(note.getContent(), key);
        String preview = CryptoHelper.encrypt(notePreview, key);

        return new EncryptedNote(
                note.getId(),
                title,
                content,
                preview,
                note.getCreatedAt()
        );
    }

    public NotePreview decryptPreview(@NonNull NotePreview note, SecretKey masterKey) throws Exception {

        String title = new String(CryptoHelper.decrypt(note.getTitle(), masterKey));
        String preview = new String(CryptoHelper.decrypt(note.getPreview(), masterKey));

        NotePreview notePreview = new NotePreview(note);
        notePreview.setTitle(title);
        notePreview.setPreview(preview);
        notePreview.setEncrypted(false);

        return notePreview;
    }

    public Note decryptNote(@NonNull EncryptedNote encryptedNote, SecretKey masterKey) throws Exception {
        return new Note(
                encryptedNote.getId(),
                new String(CryptoHelper.decrypt(encryptedNote.getTitle(), masterKey)),
                new String(CryptoHelper.decrypt(encryptedNote.getContent(), masterKey)),
                encryptedNote.getCreatedAt());
    }


}