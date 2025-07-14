package com.myapp.mindcache.security;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class SecureKeyCrypto implements KeyCrypto {

    private static final String TAG = SecureKeyCrypto.class.getSimpleName();
    private static final String KEY_STORE = "AndroidKeyStore";
    private static final String MASTER_KEY_ALIAS = "master_key";
    private static final String RSA_MODE = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    private final KeyStore keyStore;

    public SecureKeyCrypto() throws CryptoException {
        try {
            this.keyStore = KeyStore.getInstance(KEY_STORE);
            this.keyStore.load(null);
        } catch (Exception e) {
            throw new CryptoException("Failed to initialize KeyStore", e);
        }
    }

    // Шифруем ключ данных с помощью KeyStore
    @Override
    public byte[] encryptDataKey(SecretKey dataKey) throws CryptoException {
        if (dataKey == null) {
            throw new CryptoException("Data key cannot be null");
        }

        try {
            // Получаем публичный ключ из KeyStore
            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                throw new CryptoException("Master key not found in KeyStore");
            }

            PublicKey publicKey = keyStore.getCertificate(MASTER_KEY_ALIAS).getPublicKey();

            // Настраиваем параметры OAEP для лучшей безопасности
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );

            // Шифруем ключ данных
            Cipher cipher = Cipher.getInstance(RSA_MODE);
            cipher.init(Cipher.WRAP_MODE, publicKey, oaepParams);
            return cipher.wrap(dataKey);
        } catch (Exception e) {
            throw new CryptoException("Failed to encrypt data key", e);
        }
    }

    // Восстанавливаем ключ данных с помощью KeyStore
    @Override
    public SecretKey decryptDataKey(byte[] encryptedKey) throws CryptoException {
        if (encryptedKey == null || encryptedKey.length == 0) {
            throw new CryptoException("Encrypted key cannot be null or empty");
        }

        try {
            // Получаем приватный ключ из KeyStore
            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                throw new CryptoException("Master key not found in KeyStore");
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(MASTER_KEY_ALIAS, null);

            // Настраиваем параметры OAEP (должны соответствовать параметрам при шифровании)
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );

            // Дешифруем ключ данных
            Cipher cipher = Cipher.getInstance(RSA_MODE);
            cipher.init(Cipher.UNWRAP_MODE, privateKey, oaepParams);
            return (SecretKey) cipher.unwrap(encryptedKey, "AES", Cipher.SECRET_KEY);
        } catch (Exception e) {
            throw new CryptoException("Failed to decrypt data key", e);
        }
    }
}
