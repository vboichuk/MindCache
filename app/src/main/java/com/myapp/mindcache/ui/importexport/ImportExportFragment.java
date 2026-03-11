package com.myapp.mindcache.ui.importexport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.mindcache.databinding.FragmentImportExportBinding;
import com.myapp.mindcache.datastorage.AppDatabase;

public class ImportExportFragment extends Fragment {

    private static final String TAG = ImportExportFragment.class.getSimpleName();
    private FragmentImportExportBinding binding;

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    AppDatabase.importDatabase(requireActivity().getApplicationContext(), uri);
                }
            });

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
}