package com.myapp.mindcache.ui.importexport;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.mindcache.databinding.FragmentImportExportBinding;
import com.myapp.mindcache.datastorage.AppDatabase;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImportExportFragment extends Fragment {

    private static final String TAG = ImportExportFragment.class.getSimpleName();
    private FragmentImportExportBinding binding;

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    AppDatabase.importDatabase(requireActivity().getApplicationContext(), uri);
                }
            });;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SlideshowViewModel slideshowViewModel =
                new ViewModelProvider(this).get(SlideshowViewModel.class);

        binding = FragmentImportExportBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSlideshow;
        slideshowViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnExport.setOnClickListener(v -> exportDb());
        binding.btnImport.setOnClickListener(v -> importDb());
    }

    private void importDb() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }



    private void exportDb() {
        AppDatabase.exportDatabase(requireActivity().getApplicationContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public void verifyStoragePermissions(Activity activity) {
        boolean hasAppAccess = ActivityCompat.checkSelfPermission(
                requireActivity(),
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION) == PackageManager.PERMISSION_GRANTED;

        if (hasAppAccess)
            Log.d(TAG, "Granted");
        else
            requestAllFilesAccessPermission();
    }

    void requestAllFilesAccessPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData(Uri.parse("package:" + requireActivity().getPackageName()));
            startActivity(intent);
        } catch (ActivityNotFoundException anf) {
            Log.e(TAG, anf.getMessage());
//            try {
//                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
//                        .setData(Uri.parse("package:" + getPackageName()));
//                startActivity(intent);
//            } catch (Exception e) {
//                Log.e(TAG, "Failed to open settings", e);
//                showToast("Failed to request storage permission");
//            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to request all files access", e);
            // showToast("Failed to request storage permission");
        }
    }
}