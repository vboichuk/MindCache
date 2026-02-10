package com.myapp.mindcache.ui.auth;


import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

import com.myapp.mindcache.R;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BiometricAuthHelper {

    private final Executor executor = Executors.newSingleThreadExecutor();

    public interface AuthCallback {
        void onSuccess();
        void onFailure();
        void onError(int errorCode, String error);
    }

    public void authenticate(Fragment fragment, AuthCallback callback) {

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(fragment.getString(R.string.login_into_app))
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                fragment,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        fragment.getActivity().runOnUiThread(callback::onSuccess);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        fragment.getActivity().runOnUiThread(callback::onFailure);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        fragment.getActivity().runOnUiThread(() -> callback.onError(errorCode, String.valueOf(errString)));
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }
}
