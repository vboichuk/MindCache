package com.myapp.mindcache.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.myapp.mindcache.R;

public class AuthFragment extends Fragment {

    private Button btnLogin;
    private ProgressBar progressBar;

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
            btnLogin.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            // Имитация процесса авторизации
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Здесь должна быть реальная логика авторизации
                navigateToDiary();
            }, 1500);
        });
    }

    void navigateToDiary() {
            // После успешной проверки учетных данных
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_auth_to_diary);
        }
}
