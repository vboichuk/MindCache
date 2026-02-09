package com.myapp.mindcache.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.security.AndroidKeystoreKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.AEADBadTagException;

public class AuthFragment extends Fragment {

    private static final String TAG = "AuthFragment";
    private Button btnLogin;
    private EditText editPassword;
    private LinearLayout passwordLayout;

    private final Executor executor = Executors.newSingleThreadExecutor();

    private PasswordManager passwordManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        try {
            AndroidKeystoreKeyManager secureKeyManager = new AndroidKeystoreKeyManager();
            passwordManager = new PasswordManagerImpl(getContext(), secureKeyManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnLogin = view.findViewById(R.id.btn_auth);
        editPassword = view.findViewById(R.id.edittext_password);
        passwordLayout = view.findViewById(R.id.input_password_layout);

        btnLogin.setOnClickListener(v -> onLoginClick());
        passwordLayout.setVisibility(passwordManager.isPasswordSet() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean passwordSet = passwordManager.isPasswordSet();
        Log.i(TAG, passwordSet ? "password is set" : "password is NOT set");

        if (passwordSet) {
            showBiometricPrompt();
        }
    }

    private void onLoginClick() {
        if (editPassword.getText().length() < 4) {
            Toast.makeText(getContext(), "Password too short", Toast.LENGTH_SHORT).show();
            return;
        }

        showBiometricPrompt();
    }


    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.login_into_app))
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
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
        boolean isPasswordSet = passwordManager.isPasswordSet();

        if (!isPasswordSet) {
            passwordManager.setUserPassword(editPassword.getText().toString().toCharArray());
        }

        String userPassword = null;
        try {
            userPassword = passwordManager.getUserPassword();
            Log.d(TAG, "userPassword: [" + userPassword + "]");
            getActivity().runOnUiThread(this::navigateToDiary);
        } catch (AEADBadTagException e) {
            System.out.println("e.getCause() = " + e.getCause());
            e.printStackTrace();
            passwordManager.resetPassword();
            getActivity().runOnUiThread(this::showPasswordPrompt);
        }
    }

    private void showPasswordPrompt() {

        System.out.println("AuthFragment.showPasswordPrompt");
        passwordLayout.setVisibility(View.VISIBLE);
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
