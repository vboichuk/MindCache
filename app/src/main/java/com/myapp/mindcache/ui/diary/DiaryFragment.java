package com.myapp.mindcache.ui.diary;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentDiaryBinding;
import com.myapp.mindcache.datastorage.DiaryViewModel;
import com.myapp.mindcache.model.FeedItem;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

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

        String title = "Вот еще одна заметка";
        String content = "Every ticket—a bug report or a feature request—is a short-term contract. You, the reporter, hire them to make a fix or implement a feature. They, the team of developers, do it for you—provided you pay, or their motivation is intrinsic—for example, in open source. The discussion that happens along the way may help clarify the requirements of the contract. It may also help the team convince you that the bug doesn’t deserve a fix. Also, it may help them deliver the fix to you and convince you to close the ticket. However, the discussion may also distract both parties if it loses focus.\n";

        // Подписка на Completable из ViewModel
        disposables.add(viewModel.addNote(title, content)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> {})
                .doFinally(() -> {})
                .subscribe(
                        () -> {
                            // Успешное добавление
                            Snackbar.make(binding.getRoot(),
                                    R.string.note_added_successfully,
                                    Snackbar.LENGTH_SHORT).show();

                            // Автоматическое обновление через LiveData в ViewModel
                        },
                        throwable -> {
                            // Обработка ошибок
                            Log.e(TAG, "Error adding note", throwable);
                            String errorMsg = throwable.getMessage();
                            Snackbar.make(binding.getRoot(),
                                            errorMsg,
                                            Snackbar.LENGTH_LONG)
                                    .show();
                        }
                ));
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
        FeedAdapter adapter = new FeedAdapter(new ArrayList<>(), this::onNoteClick);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        loadNotes();
    }

    private void onNoteClick(FeedItem feedItem) {
        Toast.makeText(getContext(), feedItem.getTitle() + " clicked", Toast.LENGTH_LONG).show();
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