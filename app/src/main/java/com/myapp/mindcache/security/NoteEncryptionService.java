package com.myapp.mindcache.security;

import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NotePreview;

import java.util.Base64;

import javax.crypto.SecretKey;

public class NoteEncryptionService {
    private static final String TAG = NoteEncryptionService.class.getSimpleName();
    private final KeyGenerator keyGenerator;

    public NoteEncryptionService(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public Note encryptNote(Note note, char[] password) throws Exception {

        assert note.isSecret();

        byte[] salt = keyGenerator.generateSalt();
        SecretKey key = keyGenerator.generateDataKey(password, salt);

        String title = CryptoHelper.encrypt(note.getTitle(), key);
        String content = CryptoHelper.encrypt(note.getContent(), key);
        String preview = CryptoHelper.encrypt(note.getPreview(), key);

        return new Note(
                note.getId(),
                title,
                content,
                preview,
                note.getCreatedAt(),
                Base64.getEncoder().encodeToString(salt)
        );
    }

    public Note decryptNote(Note encryptedNote, char[] password) throws Exception {

        assert encryptedNote.isSecret();

        byte[] salt = Base64.getDecoder().decode(encryptedNote.getSalt());
        SecretKey key = keyGenerator.generateDataKey(password, salt);

        return new Note(
                encryptedNote.getId(),
                new String(CryptoHelper.decrypt(encryptedNote.getTitle(), key)),
                new String(CryptoHelper.decrypt(encryptedNote.getContent(), key)),
                encryptedNote.getCreatedAt(),
                true);
    }

    public NotePreview decryptPreview(NotePreview encryptedNote, char[] password) throws Exception {

        assert encryptedNote.isSecret();

        byte[] salt = Base64.getDecoder().decode(encryptedNote.getSalt());
        SecretKey key = keyGenerator.generateDataKey(password, salt);

        String title = new String(CryptoHelper.decrypt(encryptedNote.getTitle(), key));
        String preview = encryptedNote.getPreview().isEmpty()
                ? ""
                : new String(CryptoHelper.decrypt(encryptedNote.getPreview(), key));

        NotePreview notePreview = new NotePreview(encryptedNote);
        notePreview.setTitle(title);
        notePreview.setPreview(preview);

        return notePreview;
    }
}