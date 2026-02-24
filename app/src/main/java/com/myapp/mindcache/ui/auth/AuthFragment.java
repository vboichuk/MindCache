package com.myapp.mindcache.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.MainActivity;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentAuthBinding;
import com.myapp.mindcache.utils.BiometricAuthHelper;
import com.myapp.mindcache.viewmodel.AuthViewModel;
import com.myapp.mindcache.viewmodel.AuthViewModelFactory;

import io.reactivex.disposables.CompositeDisposable;

public class AuthFragment extends Fragment {

    private static final String TAG = AuthFragment.class.getSimpleName();

    private final BiometricAuthHelper authHelper = new BiometricAuthHelper();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private FragmentAuthBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAuthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModel();
        observeViewModel();
        setupClickListeners();

        authViewModel.checkRegistration();
    }

    private void initViewModel() {
        MainActivity activity = (MainActivity) requireActivity();
        AuthViewModelFactory factory = activity.getAuthViewModelFactory();
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);
    }

    private void observeViewModel() {
        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            Log.i(TAG, "Auth state changed: " + state);
            if (state == AuthViewModel.AuthState.LOGIN) {
                showLoginMode();
            } else if (state == AuthViewModel.AuthState.REGISTER) {
                showRegisterMode();
            } else if (state == AuthViewModel.AuthState.AUTHENTICATED) {
                navigateToNotes();
            }
        });

        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            Log.i(TAG, "Auth error: " + errorMsg);
            if (errorMsg != null) {
                if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), errorMsg, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showRegisterMode() {
        binding.btnLogin.setVisibility(View.GONE);
    }

    private void showLoginMode() {
        binding.btnRegister.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> onLoginClick());
        binding.btnRegister.setOnClickListener(v -> onRegisterClick());
    }

    private void onLoginClick() {
        char[] password = binding.edittextPassword.getText().toString().toCharArray();
        authViewModel.login(password);
    }

    private void onRegisterClick() {
        char[] password = binding.edittextPassword.getText().toString().toCharArray();
        authViewModel.register(password);
    }

    private void showBiometricPrompt() {
        authHelper.authenticate(this, new BiometricAuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                navigateToNotes();
            }

            @Override
            public void onFailure() {
                if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), "Лицо не распознано", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(int errorCode, String error) {
                String text = "Error: " + errorCode + " (" + error + ")";
                if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), text, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void navigateToNotes() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_auth_to_diary);
    }
}
