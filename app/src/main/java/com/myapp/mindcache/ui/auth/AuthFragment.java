package com.myapp.mindcache.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
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
import com.myapp.mindcache.viewmodel.AuthViewModel;
import com.myapp.mindcache.viewmodel.AuthViewModelFactory;

public class AuthFragment extends Fragment {

    private static final String TAG = AuthFragment.class.getSimpleName();

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
        disableBackPressed();

        authViewModel.checkRegistration();
    }

    private void disableBackPressed() {
        OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), "Please login", Snackbar.LENGTH_SHORT).show();
                }
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
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
                navigateToApp();
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
        if (binding.edittextPassword.getText() == null)
            return;
        char[] password = binding.edittextPassword.getText().toString().toCharArray();
        authViewModel.login(password);
    }

    private void onRegisterClick() {
        if (binding.edittextPassword.getText() == null)
            return;
        char[] password = binding.edittextPassword.getText().toString().toCharArray();
        authViewModel.register(password);
    }

    private void navigateToApp() {
        NavController navController = Navigation.findNavController(requireView());
        if (navController.getPreviousBackStackEntry() != null) {
            navController.popBackStack();
        } else {
            navController.navigate(R.id.action_auth_to_notes_list);
        }
    }
}
