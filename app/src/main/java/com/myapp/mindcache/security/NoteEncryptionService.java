package com.myapp.mindcache.security;

import android.util.Log;

import com.myapp.mindcache.model.Note;

import java.util.Arrays;
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

        return new Note(
                note.getId(),
                cryptoHelper.encrypt(note.getTitle(), key),
                cryptoHelper.encrypt(note.getContent(), key),
                note.getCreatedAt(),
                Base64.getEncoder().encodeToString(salt)
        );
    }

    public Note decryptNote(Note encryptedNote, char[] password) throws Exception {
        Log.d(TAG, "decryptNote " + encryptedNote.getId());
        // Log.d(TAG, "password = " + Arrays.toString(password));

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
}