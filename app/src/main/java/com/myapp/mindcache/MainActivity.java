package com.myapp.mindcache;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.myapp.mindcache.databinding.ActivityMainBinding;
import com.myapp.mindcache.viewmodel.NotesViewModelFactory;
import com.myapp.mindcache.security.AndroidKeystoreKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;
import com.myapp.mindcache.utils.KeyboardUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private NotesViewModelFactory viewModelFactory;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;
        navigationView = binding.navView;

        MaterialToolbar toolbar = binding.topAppBar;
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.note_details_toolbar_menu);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        initializeFactory();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavigation();
    }

    private void setupNavigation() {
        try {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_notes_list,
                    R.id.nav_gallery,
                    R.id.nav_import_export)
                    .setOpenableLayout(drawerLayout)
                    .build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments)
                    -> setDrawerEnabled(destination.getId() != R.id.nav_auth));

        } catch (Exception e) {
            Log.e(TAG, "Navigation setup failed", e);
        }
    }

    private void setDrawerEnabled(boolean enabled) {
        if (enabled) {
            // Показываем гамбургер и разблокируем drawer
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            // Скрываем гамбургер и блокируем drawer
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    private void initializeFactory() {
        // 1. Создаем фабрику ОДИН РАЗ
        try {
            AndroidKeystoreKeyManager secureKeyManager = new AndroidKeystoreKeyManager();
            PasswordManager passwordManager = new PasswordManagerImpl(getApplication(), secureKeyManager);
            viewModelFactory
                    = new NotesViewModelFactory(getApplication(), passwordManager);


        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize", e);
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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public NotesViewModelFactory getViewModelFactory() {
        return viewModelFactory;
    }
}