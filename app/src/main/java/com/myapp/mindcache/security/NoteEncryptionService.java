package com.myapp.mindcache.security;

import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NotePreview;

import java.util.Base64;

import javax.crypto.SecretKey;

public class NoteEncryptionService {
    private static final String TAG = "NoteEncryptionService";
    private final KeyGenerator keyGenerator;
    private final CryptoHelper cryptoHelper;

    public NoteEncryptionService(KeyGenerator keyGenerator, CryptoHelper cryptoHelper) {
        this.keyGenerator = keyGenerator;
        this.cryptoHelper = cryptoHelper;
    }

    public Note encryptNote(Note note, char[] password) throws Exception {

        assert note.isSecret();

        byte[] salt = keyGenerator.generateSalt();
        SecretKey key = keyGenerator.generateDataKey(password, salt);

        String title = cryptoHelper.encrypt(note.getTitle(), key);
        String content = cryptoHelper.encrypt(note.getContent(), key);
        String preview = cryptoHelper.encrypt(note.getPreview(), key);

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
                cryptoHelper.decrypt(encryptedNote.getTitle(), key),
                cryptoHelper.decrypt(encryptedNote.getContent(), key),
                encryptedNote.getCreatedAt(),
                true);
    }

    public NotePreview decryptPreview(NotePreview encryptedNote, char[] password) throws Exception {

        assert encryptedNote.isSecret();

        byte[] salt = Base64.getDecoder().decode(encryptedNote.getSalt());
        SecretKey key = keyGenerator.generateDataKey(password, salt);

        String title = cryptoHelper.decrypt(encryptedNote.getTitle(), key);
        String preview = encryptedNote.getPreview().isEmpty() ? "" : cryptoHelper.decrypt(encryptedNote.getPreview(), key);

        NotePreview notePreview = new NotePreview(encryptedNote);
        notePreview.setTitle(title);
        notePreview.setPreview(preview);

        return notePreview;
    }
}