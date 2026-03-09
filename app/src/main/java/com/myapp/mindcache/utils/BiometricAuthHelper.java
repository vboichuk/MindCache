package com.myapp.mindcache.utils;


import android.app.Activity;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

import com.myapp.mindcache.R;

public class BiometricAuthHelper {

    private static final String TAG = BiometricAuthHelper.class.getSimpleName();

    public interface AuthCallback {
        void onSuccess();
        void onFailure();
        void onError(int errorCode, String error);
    }

    @MainThread
    public void authenticate(Fragment fragment, AuthCallback callback) {

        Activity activity = fragment.getActivity();
        if (activity == null || fragment.isDetached() || !fragment.isAdded()) {
            Log.w(TAG, "Fragment not attached, skipping authentication");
            return;
        }

        // 2. Проверяем доступность биометрии
        BiometricManager biometricManager = BiometricManager.from(activity);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            callback.onError(
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    "Biometric authentication not available"
            );
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(fragment.getString(R.string.login_into_app))
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                fragment,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        callback.onFailure();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        String s = String.valueOf(errString);
                        callback.onError(errorCode, s);
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }
}
