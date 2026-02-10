package com.myapp.mindcache.ui.auth;

import android.content.Context;
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
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.security.AndroidKeystoreKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;
import com.myapp.mindcache.utils.BiometricAuthHelper;

import javax.crypto.AEADBadTagException;

public class AuthFragment extends Fragment {

    private static final String TAG = "AuthFragment";
    private Button btnLogin;
    private EditText editPassword;
    private LinearLayout passwordLayout;
    private final BiometricAuthHelper authHelper = new BiometricAuthHelper();
    private PasswordManager passwordManager;

    private static final int MIN_PASSWORD_LENGTH = 4;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            AndroidKeystoreKeyManager secureKeyManager = new AndroidKeystoreKeyManager();
            passwordManager = new PasswordManagerImpl(getContext(), secureKeyManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnLogin = view.findViewById(R.id.btn_auth);
        editPassword = view.findViewById(R.id.edittext_password);
        passwordLayout = view.findViewById(R.id.input_password_layout);

        passwordLayout.setVisibility(passwordManager.isPasswordSet() ? View.GONE : View.VISIBLE);

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> onLoginClick());
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
        if (editPassword.getText().length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(getContext(), "Password too short", Toast.LENGTH_SHORT).show();
            return;
        }

        showBiometricPrompt();
    }

    private void showBiometricPrompt() {
        authHelper.authenticate(this, new BiometricAuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                boolean isPasswordSet = passwordManager.isPasswordSet();
                if (!isPasswordSet) {
                    passwordManager.setUserPassword(editPassword.getText().toString().toCharArray());
                }

                try {
                    checkPasswordIsSet();
                    navigateToDiary();
                } catch (AEADBadTagException e) {
                    Log.e(TAG, "e.getCause() = " + e.getCause());
                    e.printStackTrace();
                    passwordManager.resetPassword();
                    showPasswordPrompt();
                }
            }

            @Override
            public void onFailure() {
                Snackbar.make(AuthFragment.this.getView(),
                                "Лицо не распознано",
                                Snackbar.LENGTH_LONG)
                        // .setAction(R.string.retry, v -> loadNotes())
                        .show();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    AuthFragment.this.getActivity().finish();
                }

                String text = "Error: " + errorCode + " (" + error + ")";
                Snackbar.make(AuthFragment.this.getView(),
                                text, Snackbar.LENGTH_LONG)
                        // .setAction(R.string.retry, v -> loadNotes())
                        .show();
            }
        });
    }

    private void navigateToDiary() {
        // После успешной проверки учетных данных
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_auth_to_diary);
    }

    /** @noinspection unused*/
    private void checkPasswordIsSet() throws AEADBadTagException {
        String userPassword;
        userPassword = passwordManager.getUserPassword();
        // Log.d(TAG, "userPassword: [" + userPassword + "]");
    }

    private void showPasswordPrompt() {
        Log.d(TAG, "AuthFragment.showPasswordPrompt");
        passwordLayout.setVisibility(View.VISIBLE);
    }
}
