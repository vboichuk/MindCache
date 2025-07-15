package com.myapp.mindcache.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AuthFragment extends Fragment {

    private Button btnLogin;
    private ProgressBar progressBar;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnLogin = view.findViewById(R.id.btn_login);
        progressBar = view.findViewById(R.id.progress_bar);

        btnLogin.setOnClickListener(v -> {
            // Показываем прогресс бар
            // btnLogin.setVisibility(View.INVISIBLE);
            // progressBar.setVisibility(View.VISIBLE);

            showBiometricPrompt();

//            new Handler(Looper.getMainLooper()).postDelayed(() -> {  }, 1500);
        });
    }

    private void showBiometricPrompt() {

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Вход в приложение")
                .setSubtitle("Приложите палец к сканеру")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK |
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                AuthFragment.this.onAuthSuccess();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                AuthFragment.this.onAuthFailed();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                AuthFragment.this.onAuthError(errorCode, errString.toString());
            }
        });

        biometricPrompt.authenticate(promptInfo);
    }

    private void navigateToDiary() {
            // После успешной проверки учетных данных
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_auth_to_diary);
        }

    private void onAuthSuccess() {
        getActivity().runOnUiThread(this::navigateToDiary);
    }

    private void onAuthFailed() {
        getActivity().runOnUiThread(() -> {
            Snackbar.make(this.getView(),
                            "Лицо не распознано",
                            Snackbar.LENGTH_LONG)
                    // .setAction(R.string.retry, v -> loadNotes())
                    .show();
        });
    }

    private void onAuthError(int errorCode, String errString) {
        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
        ) {
            this.getActivity().finish();
        }

        getActivity().runOnUiThread(() -> {
            String text = "Error: " + errorCode + " (" + errString + ")";
            Snackbar.make(getView(),
                            text, Snackbar.LENGTH_LONG)
                    // .setAction(R.string.retry, v -> loadNotes())
                    .show();
        });
    }
}
