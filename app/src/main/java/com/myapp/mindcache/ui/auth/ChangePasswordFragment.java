package com.myapp.mindcache.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.myapp.mindcache.MainActivity;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentChangePasswordBinding;
import com.myapp.mindcache.ui.notes.BaseFragment;
import com.myapp.mindcache.viewmodel.AuthViewModel;
import com.myapp.mindcache.viewmodel.AuthViewModelFactory;

import java.util.Arrays;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ChangePasswordFragment extends BaseFragment {
    private static final String TAG = ChangePasswordFragment.class.getSimpleName();
    private FragmentChangePasswordBinding binding;
    private AuthViewModel authViewModel;

    private char[] passwordOld;
    private char[] passwordNew1;
    private char[] passwordNew2;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
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
        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::showMessage);
    }

    private void setupClickListeners() {
        binding.btnChangePassword.setOnClickListener(v -> onChangePasswordClick());
        binding.edittextPassword.setOnFocusChangeListener(this::onFocusChanged);
        binding.edittextNewPassword1.setOnFocusChangeListener(this::onFocusChanged);
        binding.edittextNewPassword2.setOnFocusChangeListener(this::onFocusChanged);
    }

    void onFocusChanged(View view, Boolean hasFocus) {
        if (hasFocus) {
            clearErrorForView((TextInputEditText) view);
        }
    }

    private void onChangePasswordClick() {

        clearPasswords();
        clearAllErrors();

        passwordOld = binding.edittextPassword.getText().toString().toCharArray();
        passwordNew1 = binding.edittextNewPassword1.getText().toString().toCharArray();
        passwordNew2 = binding.edittextNewPassword2.getText().toString().toCharArray();

        if (!validatePasswords()) {
            return;
        }

        Disposable disposable = authViewModel.changePassword(passwordOld, passwordNew1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(this::clearPasswords)
                .subscribe(this::onPasswordChangeSuccess,
                        this::onPasswordChangeError);

        disposables.add(disposable);
    }

    private TextInputLayout findTextInputLayout(View view) {
        ViewParent parent = view.getParent();

        while (parent != null) {
            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            }
            parent = parent.getParent();
        }

        return null;
    }

    private void markError(TextInputEditText editText, String errorMsg) {
        TextInputLayout parent = findTextInputLayout(editText);
        if (parent != null) {
            parent.setError(errorMsg);
        }
    }

    private void clearPasswords() {
        if (passwordOld != null) {
            Arrays.fill(passwordOld, '\0');
            passwordOld = null;
        }
        if (passwordNew1 != null) {
            Arrays.fill(passwordNew1, '\0');
            passwordNew1 = null;
        }
        if (passwordNew2 != null) {
            Arrays.fill(passwordNew2, '\0');
            passwordNew2 = null;
        }
    }

    @Override
    public void onDestroyView() {
        clearPasswords();
        super.onDestroyView();
    }

    private boolean validatePasswords() {
        boolean isValid = true;

        // Проверка старый пароль
        if (passwordOld.length < 4) {
            markError(binding.edittextPassword, "Current password must be at least 4 characters");
            isValid = false;
        }

        // Проверка новый пароль
        if (passwordNew1.length < 4) {
            markError(binding.edittextNewPassword1, "New password must be at least 4 characters");
            isValid = false;
        }

        // Проверка подтверждение пароля
        if (passwordNew2.length < 4) {
            markError(binding.edittextNewPassword2, "Password confirmation must be at least 4 characters");
            isValid = false;
        }

        // Проверка совпадения новых паролей
        if (isValid && !Arrays.equals(passwordNew1, passwordNew2)) {
            markError(binding.edittextNewPassword2, "Passwords do not match");
            isValid = false;
        }

        // Проверка что новый пароль отличается от старого
        if (isValid && Arrays.equals(passwordOld, passwordNew1)) {
            markError(binding.edittextNewPassword1, "New password must be different from current");
            isValid = false;
        }

        return isValid;
    }

    private void onPasswordChangeSuccess() {
        showMessage(R.string.password_changed);
        clearPasswords();
        // isProcessing = false;
        binding.btnChangePassword.setEnabled(true);
        navigateBack();
    }

    private void onPasswordChangeError(Throwable error) {
        clearPasswords();
        // isProcessing = false;
        binding.btnChangePassword.setEnabled(true);

        if (error instanceof SecurityException) {
            markError(binding.edittextPassword, error.getMessage());
        } else {
            showError(error);
        }
    }

    private void clearErrorForView(TextInputEditText editText) {
        TextInputLayout parent = findTextInputLayout(editText);
        if (parent != null) {
            parent.setError(null);
            parent.setErrorEnabled(false);
        }
    }

    private void clearAllErrors() {
        clearErrorForView(binding.edittextPassword);
        clearErrorForView(binding.edittextNewPassword1);
        clearErrorForView(binding.edittextNewPassword2);
    }
}
