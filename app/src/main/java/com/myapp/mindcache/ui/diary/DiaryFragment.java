package com.myapp.mindcache.ui.diary;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentDiaryBinding;
import com.myapp.mindcache.datastorage.DiaryViewModel;
import com.myapp.mindcache.model.FeedItem;

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

    public DiaryFragment() {

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        this.viewModel = new ViewModelProvider(this).get(DiaryViewModel.class);

        binding = FragmentDiaryBinding.inflate(inflater, container, false);
        binding.fab.setOnClickListener(view -> this.onAddClick());

        View root = binding.getRoot();

        recyclerView = root.findViewById(R.id.recyclerView);
        setupRecyclerView();

        return root;
    }

    private void onAddClick() {
        // Получаем FragmentManager из Activity
        FragmentManager fragmentManager = ((AppCompatActivity) getContext())
                .getSupportFragmentManager();

        // Создаем транзакцию
        fragmentManager.beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in,
                        R.anim.slide_out,
                        R.anim.slide_in,
                        R.anim.slide_out)
                .replace(R.id.fragment_container, NoteDetailFragment.newInstance())
                .addToBackStack(null) // Добавляем в back stack
                .commit();
    }

    private void addNoteTest() {


    }

    private void loadNotes() {

        // Подписываемся на LiveData из ViewModel
        viewModel.getFeedItems().observe(getViewLifecycleOwner(), notes -> {
            updateAdapter(notes);
        });

        viewModel.getErrors().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
            }
        });

        // Инициируем загрузку (если не делаем auto-load в ViewModel)
        viewModel.loadFeedItems();
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
        loadNotes();
    }

    private void onNoteLongClick(FeedItem feedItem) {
        showBottomSheet(feedItem);
    }

    private void onNoteClick(FeedItem feedItem) {

        long noteId = feedItem.getId();
        // Получаем FragmentManager из Activity
        FragmentManager fragmentManager = ((AppCompatActivity)getContext())
                .getSupportFragmentManager();

        // Создаем транзакцию
        fragmentManager.beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in,
                        R.anim.slide_out,
                        R.anim.slide_in,
                        R.anim.slide_out)
                .replace(R.id.fragment_container, NoteDetailFragment.newInstance(noteId))
                .addToBackStack(null) // Добавляем в back stack
                .commit();
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
                        () -> Toast.makeText(requireContext(), "DELETED", Toast.LENGTH_SHORT).show(),
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