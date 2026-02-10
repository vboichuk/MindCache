package com.myapp.mindcache;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.myapp.mindcache.databinding.ActivityMainBinding;
import com.myapp.mindcache.viewmodel.NotesViewModelFactory;
import com.myapp.mindcache.security.AndroidKeystoreKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;
import com.myapp.mindcache.utils.KeyboardUtils;

import net.sqlcipher.BuildConfig;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NotesViewModelFactory viewModelFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavigationView navigationView = binding.navView;

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupWithNavController(navigationView, navController);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // 1. Создаем фабрику ОДИН РАЗ
        try {
            AndroidKeystoreKeyManager secureKeyManager = new AndroidKeystoreKeyManager();
            PasswordManager passwordManager = new PasswordManagerImpl(getApplication(), secureKeyManager);
            viewModelFactory
                    = new NotesViewModelFactory(getApplication(), passwordManager);


        } catch (Exception e) {
            Log.e("MainActivity", "Failed to initialize", e);
            Toast.makeText(this, "App initialization failed", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();
        if (v != null) {
            KeyboardUtils.dispatchTouchEvent(v, ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    public NotesViewModelFactory getViewModelFactory() {
        return viewModelFactory;
    }
}