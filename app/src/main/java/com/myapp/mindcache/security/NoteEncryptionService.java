package com.myapp.mindcache.security;

import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NotePreview;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class NoteEncryptionService {
    private static final String TAG = NoteEncryptionService.class.getSimpleName();
    private final KeyGenerator keyGenerator;

    public NoteEncryptionService(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public Note encryptNote(Note note, byte[] masterKey) throws Exception {

        assert note.isSecret();

        SecretKey key = new SecretKeySpec(masterKey, "AES");
        // SecretKey key = keyGenerator.deriveSecretKey(password, salt);

        String title = CryptoHelper.encrypt(note.getTitle(), key);
        String content = CryptoHelper.encrypt(note.getContent(), key);
        String preview = CryptoHelper.encrypt(note.getPreview(), key);

        return new Note(
                note.getId(),
                title,
                content,
                preview,
                note.getCreatedAt(),
                true
        );
    }

    public NotePreview decryptPreview(NotePreview encryptedNote, byte[] masterKey) throws Exception {

        assert encryptedNote.isSecret();

        String title = new String(CryptoHelper.decrypt(encryptedNote.getTitle(), masterKey));
        String preview = encryptedNote.getPreview().isEmpty()
                ? ""
                : new String(CryptoHelper.decrypt(encryptedNote.getPreview(), masterKey));

        NotePreview notePreview = new NotePreview(encryptedNote);
        notePreview.setTitle(title);
        notePreview.setPreview(preview);

        return notePreview;
    }

    public Note decryptNote(Note encryptedNote, byte[] masterKey) throws Exception {

        assert encryptedNote.isSecret();

        return new Note(
                encryptedNote.getId(),
                new String(CryptoHelper.decrypt(encryptedNote.getTitle(), masterKey)),
                new String(CryptoHelper.decrypt(encryptedNote.getContent(), masterKey)),
                encryptedNote.getCreatedAt(),
                true);
    }
}