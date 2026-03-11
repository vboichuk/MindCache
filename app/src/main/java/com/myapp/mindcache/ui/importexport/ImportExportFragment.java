package com.myapp.mindcache.ui.importexport;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentImportExportBinding;
import com.myapp.mindcache.ui.notes.BaseFragment;
import com.myapp.mindcache.viewmodel.ImportExportViewModel;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class ImportExportFragment extends BaseFragment {

    private static final String TAG = ImportExportFragment.class.getSimpleName();
    private FragmentImportExportBinding binding;
    private ImportExportViewModel viewModel;

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::importDatabase);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentImportExportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ImportExportViewModel.class);
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnExport.setOnClickListener(v -> onExportClick());
        binding.btnImport.setOnClickListener(v -> onImportClick());
    }


    private void onImportClick() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }


    private void onExportClick() {
        Disposable disposable = viewModel.exportDb(requireActivity().getApplicationContext())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(s -> Log.d(TAG, "subscribe"))
                .doFinally(() -> Log.d(TAG, "finish"))
                .subscribe(() -> showMessage(R.string.export_successful),
                        this::processError);

        disposables.add(disposable);
    }

    private void importDatabase(Uri uri) {
        if (uri == null)
            return;

        Disposable disposable = viewModel.importDb(requireActivity().getApplicationContext(), uri)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(s -> Log.d(TAG, "subscribe"))
                .doFinally(() -> Log.d(TAG, "finish"))
                .subscribe(() -> showMessage(R.string.import_successful),
                        this::processError);

        disposables.add(disposable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}