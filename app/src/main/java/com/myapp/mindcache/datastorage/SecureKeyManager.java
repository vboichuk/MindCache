package com.myapp.mindcache.datastorage;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyStore;

public class SecureKeyManager {
        private final KeyStore keyStore;
        private final String keyAlias = "secure_db_key";

        public SecureKeyManager() throws Exception {
            this.keyStore = KeyStore.getInstance("AndroidKeyStore");
            this.keyStore.load(null);
        }

        public SecretKey getOrCreateKey() throws Exception {
            if (!keyStore.containsAlias(keyAlias)) {
                createKey();
            }
            return (SecretKey) keyStore.getKey(keyAlias, null);
        }

        private void createKey() throws Exception {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
            );

            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false) // ← ОТКЛЮЧАЕМ аутентификацию
                    .build();

            keyGenerator.init(keySpec);
            keyGenerator.generateKey();
        }
    }