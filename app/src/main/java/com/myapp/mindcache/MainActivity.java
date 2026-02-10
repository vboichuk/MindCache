package com.myapp.mindcache;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.myapp.mindcache.databinding.ActivityMainBinding;
import com.myapp.mindcache.datastorage.NotesViewModel;
import com.myapp.mindcache.utils.KeyboardUtils;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    // private NotesViewModel sharedViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavigationView navigationView = binding.navView;

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupWithNavController(navigationView, navController);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();
        if (v != null) {
            KeyboardUtils.dispatchTouchEvent(v, ev);
        }
        return super.dispatchTouchEvent(ev);
    }
}