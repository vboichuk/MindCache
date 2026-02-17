package com.myapp.mindcache.security;

import android.security.keystore.UserNotAuthenticatedException;

import com.myapp.mindcache.exception.NoPasswordSavedException;

import javax.crypto.AEADBadTagException;

public interface PasswordManager {

    /**
     * Возвращает расшифрованный пароль пользователя
     * @throws AEADBadTagException - неверный пароль или данные повреждены
     * @throws UserNotAuthenticatedException - требуется биометрическая аутентификация
     * @throws NoPasswordSavedException - пароль не установлен
     */
    char[] getUserPassword() throws
            AEADBadTagException,
            UserNotAuthenticatedException,
            NoPasswordSavedException;

    /**
     * Устанавливает новый пароль пользователя
     * @param password новый пароль (будет очищен после использования)
     */
    void setUserPassword(char[] password);

    /**
     * Проверяет, установлен ли пароль
     */
    boolean isPasswordSet();

    /**
     * Сбрасывает пароль и удаляет ключ
     */
    void resetPassword();
}
