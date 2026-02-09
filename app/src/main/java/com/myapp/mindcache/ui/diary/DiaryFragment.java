package com.myapp.mindcache.ui.diary;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentDiaryBinding;
import com.myapp.mindcache.datastorage.DiaryViewModel;
import com.myapp.mindcache.datastorage.DiaryViewModelFactory;
import com.myapp.mindcache.model.FeedItem;
import com.myapp.mindcache.security.AndroidKeystoreKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class DiaryFragment extends Fragment {

    private static final String TAG = DiaryFragment.class.getSimpleName();
    private FragmentDiaryBinding fragmentDiaryBinding;
    private RecyclerView recyclerView;
    private DiaryViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        fragmentDiaryBinding = FragmentDiaryBinding.inflate(inflater, container, false);
        return fragmentDiaryBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupClickListeners();
        setupRecyclerView();
        initViewModel();
        observeViewModel();
        loadNotes();
    }


    private void setupClickListeners() {
        fragmentDiaryBinding.addButton.setOnClickListener(v -> this.onAddClick());
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    private void observeViewModel() {
        // Подписываемся на LiveData из ViewModel
        viewModel.getFeedItems().observe(getViewLifecycleOwner(), this::updateAdapter);

        viewModel.getErrors().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
            }
        });

        viewModel.getNotesCount().observe(getViewLifecycleOwner(), this::onNotesCountChange);
    }

    private void onNotesCountChange(Integer count) {
        Toast.makeText(getContext(), "count: " + count, Toast.LENGTH_SHORT).show();

        FeedAdapter adapter = (FeedAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.updateWithCount(count);
            // binding.emptyView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }



    private void initViewModel() {
        Log.d(TAG, "initViewModel");
        AndroidKeystoreKeyManager keystoreKeyManager;
        try {
            Activity activity = this.getActivity();
            assert activity != null;
            keystoreKeyManager = new AndroidKeystoreKeyManager();
            PasswordManager passwordManager = new PasswordManagerImpl(activity.getApplication(), keystoreKeyManager);
            DiaryViewModelFactory factory = new DiaryViewModelFactory(activity.getApplication(), passwordManager);
            viewModel = new ViewModelProvider(this, factory).get(DiaryViewModel.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadNotes() {
        Log.d(TAG, "loadNotes");

        // Инициируем загрузку (если не делаем auto-load в ViewModel)
        viewModel.loadAllNotes(true);
    }

    private void onAddClick() {
        Log.d(TAG, "onAddClick");
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_diary_to_noteDetail);
    }

    private void onNoteClick(FeedItem feedItem) {
        long noteId = feedItem.getId();
        Log.d(TAG, "onNoteClick id:" + noteId);
        Bundle args = new Bundle();
        args.putLong("noteId", noteId);

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_diary_to_noteDetail, args);
    }



    private void updateAdapter(List<FeedItem> notes) {
        Log.d(TAG, "updateAdapter with " + notes.size() + " items");
        FeedAdapter adapter = (FeedAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.updateItems(notes);
            // binding.emptyView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void setupRecyclerView() {
        recyclerView = fragmentDiaryBinding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        FeedAdapter adapter = new FeedAdapter(new ArrayList<>(0), this::onNoteClick, this::onNoteLongClick);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    private void onNoteLongClick(FeedItem feedItem) {
        showBottomSheet(feedItem);
    }

    private void showBottomSheet(FeedItem feedItem) {
        BottomSheetDialog dialog = new BottomSheetDialog(this.getContext());

        if (dialog.getWindow() != null) {
            // dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            // dialog.setCancelable(false);
        }

        View sheetLayoutView = getLayoutInflater().inflate(R.layout.bottom_sheet_layout, null);
        Button btnDelete = sheetLayoutView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            showDeleteNoteDialog(feedItem);
            dialog.dismiss();
        });

        dialog.setContentView(sheetLayoutView);
        dialog.show();
    }

    private void showDeleteNoteDialog(FeedItem note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle(R.string.confirm_delete_note);
        builder.setMessage(getString(R.string.ask_delete_note) + " \"" + note.getTitle() + "\"?" );

        builder.setPositiveButton(R.string.delete, (dialog, which) -> {
            // Действие при подтверждении удаления
            deleteNote(note.getId());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Опционально: изменить цвет кнопки "Удалить" на красный
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        });
    }

    private void deleteNote(long id) {
        Disposable subscribe = viewModel.deleteNote(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "deleteNote OK");
                            Toast.makeText(requireContext(), "DELETED", Toast.LENGTH_SHORT).show();
                            viewModel.loadAllNotes(false);
                        },
                        error -> Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showError(Throwable throwable) {
        Log.e(TAG, "Error occurred", throwable);
        if (isAdded()) {
            Snackbar.make(fragmentDiaryBinding.getRoot(),
                            R.string.error_loading_notes,
                            Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry, v -> loadNotes())
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.dispose();
        fragmentDiaryBinding = null;
    }
}