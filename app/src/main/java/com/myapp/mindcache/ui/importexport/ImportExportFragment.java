package com.myapp.mindcache.ui.importexport;

import android.accounts.OperationCanceledException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.myapp.mindcache.MainActivity;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentImportExportBinding;
import com.myapp.mindcache.exception.WrongKeyException;
import com.myapp.mindcache.ui.notes.BaseFragment;
import com.myapp.mindcache.viewmodel.ImportExportViewModel;
import com.myapp.mindcache.viewmodel.ImportExportViewModelFactory;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ImportExportFragment extends BaseFragment {

    private static final String TAG = ImportExportFragment.class.getSimpleName();
    private FragmentImportExportBinding binding;
    private ImportExportViewModel viewModel;

    private final ActivityResultLauncher<String[]> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::performImport);

    private void performImport(Uri uri) {
        if (uri == null)
            return;

        Disposable disposable = showPasswordDialog("Password for database to import")
                .flatMapCompletable(password -> viewModel.importDatabase(uri, password))
                .retryWhen(errors -> errors.flatMap(e -> {
                    if (e instanceof WrongKeyException) {
                        return Flowable.just(1);
                    } else {
                        return Flowable.error(e);
                    }
                }))
                .subscribe(() -> showMessage(R.string.import_successful), this::processError);

        disposables.add(disposable);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentImportExportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViewModel();
        setupClickListeners();
    }

    private void initViewModel() {
        MainActivity activity = (MainActivity) requireActivity();
        ImportExportViewModelFactory factory = activity.getImportExportViewModelFactory();
        viewModel = new ViewModelProvider(requireActivity(), factory).get(ImportExportViewModel.class);
    }

    private void setupClickListeners() {
        binding.btnExport.setOnClickListener(v -> onExportClick());
        binding.btnImport.setOnClickListener(v -> onImportClick());
    }


    private void onImportClick() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }


    private void onExportClick() {
        Disposable disposable = viewModel.exportDatabase()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(s -> Log.d(TAG, "subscribe"))
                .doFinally(() -> Log.d(TAG, "finish"))
                .doOnSuccess(this::showExportSuccessDialog)
                .doOnError(this::processError)
                .ignoreElement()
                .subscribe(() -> {},
                        this::processError);

        disposables.add(disposable);
    }

    private void showExportSuccessDialog(String path) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.export_successful)
                .setMessage(path)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());

        if (!isRemoving() && !isDetached()) {
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    public Single<char[]> showPasswordDialog(String prompt) {

        return Single.create((SingleEmitter<char[]> emitter) -> {
                    if (Looper.myLooper() != Looper.getMainLooper())
                        throw new IllegalStateException("showPasswordDialog must be called from main thread");
                    showDialog(emitter, prompt);
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io());
    }

    private void showDialog(SingleEmitter<char[]> emitter, String prompt) {
        LinearLayout layout = new LinearLayout(requireActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText passwordInput = new EditText(requireActivity());
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(R.string.password);
        passwordInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        layout.addView(passwordInput);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.import_confirmation);
        builder.setMessage(prompt);
        builder.setView(layout);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            dialog.dismiss();
            char[] password = passwordInput.getText().toString().toCharArray();
            emitter.onSuccess(password);
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
            emitter.onError(new OperationCanceledException("Operation was cancelled by user"));
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        emitter.setCancellable(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}