package com.myapp.mindcache.ui.notes;

import android.os.Looper;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.exception.AuthError;
import com.myapp.mindcache.exception.WrongKeyException;
import com.myapp.mindcache.utils.BiometricAuthHelper;
import com.myapp.mindcache.viewmodel.NotesViewModel;

import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;

public abstract class BaseFragment extends Fragment {

    protected NotesViewModel viewModel;
    protected final CompositeDisposable disposables = new CompositeDisposable();
    private final BiometricAuthHelper authHelper = new BiometricAuthHelper();

    protected void processError(@Nullable Throwable error) {
        if (error == null)
            return;

        Log.e(tag(), "processError: " + error.getClass() + " " + error.getMessage());
        Log.e(tag(), "processError: ", error);
        showError(error);

        if (error instanceof AuthError) {
            // navigateToLogin();
        } else if (error instanceof UserNotAuthenticatedException) {
            // showBiometricPrompt();
        } else if (error instanceof WrongKeyException) {
            Log.i(getTag(), "Need to enter valid password for database!");
        }
    }

    protected void showError(@NonNull Throwable error) {
        if (error instanceof AuthError) {
            AuthError authError = (AuthError) error;
            String message;
            switch (authError.getReason()) {
                case SESSION_EXPIRED:
                    message = "Session expired. Login again";
                    break;
                case NOT_AUTHENTICATED:
                    message = "Authentication is required";
                    break;
                default:
                    message = "Access denied";
            }
            showMessage(message);
        } else {
            showMessage(error.getMessage());
        }
    }

    private String tag() {
        return this.getClass().getSimpleName();
    }

    protected void showMessage(String message) {
        if (isAdded() && getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    protected void showMessage(@StringRes int resId) {
        if (isAdded() && getView() != null) {
            Snackbar.make(getView(), resId, Snackbar.LENGTH_SHORT).show();
        }
    }

    protected void navigateBack() {
        String TAG = tag();
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.popBackStack();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    protected Completable showBiometricPrompt() {
        return Completable.create(emitter -> {
            Log.d(tag(), "showBiometricPrompt...");

            // Make sure we are on main thread
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Log.e(getClass().getSimpleName(), "Not on main thread!");
                emitter.onError(new IllegalStateException("Must be called from main thread"));
                return;
            }

            authHelper.authenticate(this, new BiometricAuthHelper.AuthCallback() {
                @Override
                public void onSuccess() {
                    emitter.onComplete();
                }

                @Override
                public void onFailure() { }

                @Override
                public void onError(int errorCode, String error) {
                    String text = "Error: " + errorCode + " (" + error + ")";
                    showMessage(text);
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        emitter.onError(new AuthError(AuthError.Reason.USER_CANCELED));
                    } else {
                        emitter.onError(new AuthError(AuthError.Reason.BIOMETRY_FAILED));
                    }
                }
            });
        });
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
