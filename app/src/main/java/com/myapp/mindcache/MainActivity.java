package com.myapp.mindcache;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.myapp.mindcache.databinding.ActivityMainBinding;
import com.myapp.mindcache.repositories.NoteRepository;
import com.myapp.mindcache.security.AndroidKeystoreKeyManager;
import com.myapp.mindcache.security.KeyGeneratorImpl;
import com.myapp.mindcache.security.KeyManager;
import com.myapp.mindcache.security.KeyManagerImpl;
import com.myapp.mindcache.security.NoteEncryptionService;
import com.myapp.mindcache.viewmodel.AuthViewModelFactory;
import com.myapp.mindcache.viewmodel.ImportExportViewModelFactory;
import com.myapp.mindcache.viewmodel.NotesViewModelFactory;
import com.myapp.mindcache.utils.KeyboardUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private NotesViewModelFactory notesViewModelFactory;
    private AuthViewModelFactory authViewModelFactory;
    private ImportExportViewModelFactory importExportViewModelFactory;

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

        initializeFactories();
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
                    R.id.nav_change_password,
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
        drawerLayout.setDrawerLockMode(enabled ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null)
            supportActionBar.setDisplayHomeAsUpEnabled(enabled);
    }

    private void initializeFactories() {
        Application application = getApplication();

        AndroidKeystoreKeyManager keystoreKeyManager;
        NoteRepository repository;

        try {
            keystoreKeyManager = new AndroidKeystoreKeyManager();
            NoteEncryptionService service = new NoteEncryptionService();
            repository = new NoteRepository(application, service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        KeyManager keyManager = new KeyManagerImpl(application, new KeyGeneratorImpl(), keystoreKeyManager);
        notesViewModelFactory = new NotesViewModelFactory(application, keyManager, repository);
        authViewModelFactory = new AuthViewModelFactory(application, keyManager);
        importExportViewModelFactory = new ImportExportViewModelFactory(application, keyManager);
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

    public NotesViewModelFactory getNotesViewModelFactory() {
        if (notesViewModelFactory == null) {
            throw new IllegalStateException("Factories not initialized yet");
        }
        return notesViewModelFactory;
    }

    public AuthViewModelFactory getAuthViewModelFactory() {
        if (authViewModelFactory == null) {
            throw new IllegalStateException("Factories not initialized yet");
        }
        return authViewModelFactory;
    }

    public ImportExportViewModelFactory getImportExportViewModelFactory() {
        if (importExportViewModelFactory == null) {
            throw new IllegalStateException("Factories not initialized yet");
        }
        return importExportViewModelFactory;
    }
}