package com.myapp.mindcache.ui.diary;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleEventObserver;
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
import com.myapp.mindcache.security.KeystoreSecureKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class DiaryFragment extends Fragment {

    private static final String TAG = DiaryFragment.class.getSimpleName();
    private FragmentDiaryBinding binding;
    private RecyclerView recyclerView;
    private DiaryViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        getLifecycle().addObserver((LifecycleEventObserver) (source, event)
                -> Log.i(TAG, "onStateChanged: " + event.name()));
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        binding = FragmentDiaryBinding.inflate(inflater, container, false);
        binding.fab.setOnClickListener(view -> this.onAddClick());

        View root = binding.getRoot();
        recyclerView = root.findViewById(R.id.recyclerView);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModel();
        setupRecyclerView();
        loadNotes();
    }

    private void initViewModel() {
        Log.d(TAG, "initViewModel");
        KeystoreSecureKeyManager secureKeyManager = null;
        try {
            secureKeyManager = new KeystoreSecureKeyManager();
            PasswordManager passwordManager = new PasswordManagerImpl(secureKeyManager);
            Activity activity = this.getActivity();
            assert activity != null;
            DiaryViewModelFactory factory = new DiaryViewModelFactory(activity.getApplication(), passwordManager);
            viewModel = new ViewModelProvider(this, factory).get(DiaryViewModel.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private void loadNotes() {
        Log.d(TAG, "loadNotes");

        // Подписываемся на LiveData из ViewModel
        viewModel.getFeedItems().observe(getViewLifecycleOwner(), this::updateAdapter);

        viewModel.getErrors().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
            }
        });

        // Инициируем загрузку (если не делаем auto-load в ViewModel)
        viewModel.loadAllNotes(true);
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        FeedAdapter adapter = new FeedAdapter(new ArrayList<>(), this::onNoteClick, this::onNoteLongClick);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    private void onNoteLongClick(FeedItem feedItem) {
        showBottomSheet(feedItem);
    }

    private void showBottomSheet(FeedItem feedItem) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this.getContext());

        // Настройка элементов как в примере выше
        View sheetLayoutView = getLayoutInflater().inflate(R.layout.bottom_sheet_layout, null);
        Button btnDelete = sheetLayoutView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            showDeleteNoteDialog(feedItem);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(sheetLayoutView);
        bottomSheetDialog.show();
    }

    private void showDeleteNoteDialog(FeedItem note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle("Подтверждение удаления");
        builder.setMessage("Delete note '" + note.getTitle() + "'?" );

        builder.setPositiveButton("Delete", (dialog, which) -> {
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
            Snackbar.make(binding.getRoot(),
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
        binding = null;
    }
}