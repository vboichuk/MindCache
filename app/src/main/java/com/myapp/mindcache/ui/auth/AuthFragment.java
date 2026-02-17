package com.myapp.mindcache.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentAuthBinding;
import com.myapp.mindcache.security.AndroidKeystoreKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;
import com.myapp.mindcache.utils.BiometricAuthHelper;

import javax.crypto.AEADBadTagException;

public class AuthFragment extends Fragment {

    private static final String TAG = AuthFragment.class.getSimpleName();
    private static final int MIN_PASSWORD_LENGTH = 4;

    private final BiometricAuthHelper authHelper = new BiometricAuthHelper();
    private PasswordManager passwordManager;
    private FragmentAuthBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAuthBinding.inflate(inflater, container, false);
        return binding.getRoot();
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
        updateKeysIconVisibility();
        setupClickListeners();
    }

    private void updateKeysIconVisibility() {
        binding.imageKeys.setVisibility(passwordManager.isPasswordSet() ? View.VISIBLE : View.GONE);
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> onLoginClick());
        binding.logo.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        passwordManager.resetPassword();
        updateKeysIconVisibility();
        Toast.makeText(getContext(), "Password was reset", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean passwordSet = passwordManager.isPasswordSet();
        Log.i(TAG, passwordSet ? "password is set" : "password is NOT set");

        if (passwordSet) {
            // showBiometricPrompt();
        }
    }

    private void onLoginClick() {
        char[] pass = binding.edittextPassword.getText().toString().toCharArray();
        if (pass.length < MIN_PASSWORD_LENGTH) {
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
                    char[] pass = binding.edittextPassword.getText().toString().toCharArray();
                    passwordManager.setUserPassword(pass);
                }

                navigateToDiary();

//                } catch (AEADBadTagException e) {
//                    Log.e(TAG, "e.getCause() = " + e.getCause());
//                    e.printStackTrace();
//                }
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
}
