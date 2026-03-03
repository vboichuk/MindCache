package com.myapp.mindcache.security;

import androidx.annotation.NonNull;

import com.myapp.mindcache.model.EncryptedNote;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NotePreview;
import com.myapp.mindcache.repositories.NoteRepository;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class NoteEncryptionService {

    public NoteEncryptionService() {
    }

    public EncryptedNote encryptNote(@NonNull Note note, byte[] masterKey) throws Exception {

        SecretKey key = new SecretKeySpec(masterKey, "AES");

        String title = CryptoHelper.encrypt(note.getTitle(), key);
        String content = CryptoHelper.encrypt(note.getContent(), key);
        String preview = CryptoHelper.encrypt(NoteRepository.getPreview(note.getContent()) , key);

        return new EncryptedNote(
                note.getId(),
                title,
                content,
                preview,
                note.getCreatedAt()
        );
    }

    public NotePreview decryptPreview(@NonNull NotePreview note, byte[] masterKey) throws Exception {

        String title = new String(CryptoHelper.decrypt(note.getTitle(), masterKey));
        String preview = new String(CryptoHelper.decrypt(note.getPreview(), masterKey));

        NotePreview notePreview = new NotePreview(note);
        notePreview.setTitle(title);
        notePreview.setPreview(preview);
        notePreview.setEncrypted(false);

        return notePreview;
    }

    public Note decryptNote(@NonNull EncryptedNote encryptedNote, byte[] masterKey) throws Exception {

        return new Note(
                encryptedNote.getId(),
                new String(CryptoHelper.decrypt(encryptedNote.getTitle(), masterKey)),
                new String(CryptoHelper.decrypt(encryptedNote.getContent(), masterKey)),
                encryptedNote.getCreatedAt());
    }
}