package com.myapp.mindcache.ui.notes;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.exception.AuthError;
import com.myapp.mindcache.viewmodel.NotesViewModel;

import io.reactivex.disposables.CompositeDisposable;

public abstract class BaseFragment extends Fragment {

    protected final CompositeDisposable disposables = new CompositeDisposable();
    protected NotesViewModel viewModel;

    protected void processError(@Nullable Throwable error) {
        if (error == null)
            return;

        showError(error);

        if (error instanceof AuthError) {
            navigateToLogin();
        }
    }

    protected void showError(@NonNull Throwable error) {
        if (error instanceof AuthError) {
            AuthError authError = (AuthError) error;
            String message;
            switch (authError.getReason()) {
                case SESSION_EXPIRED:
                    message = "Сессия истекла. Войдите снова";
                    break;
                case NOT_AUTHENTICATED:
                    message = "Требуется аутентификация";
                    break;
                default:
                    message = "Ошибка доступа";
            }
            showMessage(message);
        } else {
            showMessage(error.getMessage());
        }
    }

    protected void showMessage(String message) {
        if (isAdded() && getView() != null) {
            Snackbar.make(getView(), message, Toast.LENGTH_SHORT).show();
        }
    }

    protected void showMessage(@StringRes int resId) {
        if (isAdded() && getView() != null) {
            Snackbar.make(getView(), resId, Toast.LENGTH_SHORT).show();
        }
    }

    protected void navigateToLogin() {
        String TAG = this.getClass().getSimpleName();
        Log.i(TAG, "navigateToLogin");
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_global_auth);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Navigation error", e);
        }
    }

    protected void navigateBack() {
        String TAG = this.getClass().getSimpleName();
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.popBackStack();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!disposables.isDisposed()) {
            disposables.dispose();
        }
    }
}
