package com.myapp.mindcache.ui.notes;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.MainActivity;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentNoteDetailBinding;
import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
import com.myapp.mindcache.exception.AuthError;
import com.myapp.mindcache.viewmodel.NotesViewModel;
import com.myapp.mindcache.viewmodel.NotesViewModelFactory;
import com.myapp.mindcache.model.Note;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class NoteDetailFragment extends Fragment {
    private static final String ARG_NOTE_ID = "noteId";
    private static final String TAG = NoteDetailFragment.class.getSimpleName();

    private final CompositeDisposable disposables = new CompositeDisposable();
    private NotesViewModel viewModel;
    private Long noteId = 0L;

    private FragmentNoteDetailBinding binding;

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMMM, HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNoteDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupActions();
        initViewModel();
        observeViewModel();

        if (getArguments() != null) {
            long id = getArguments().getLong(ARG_NOTE_ID);
            if (id > 0L) {
                this.noteId = id;
                loadNoteData(noteId);
            }
        }
    }

    private void setupActions() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.note_details_toolbar_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();

                if (id == R.id.action_save) {
                    saveNote();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    private void initViewModel() {
        MainActivity activity = (MainActivity) requireActivity();
        NotesViewModelFactory factory = activity.getNotesViewModelFactory();
        viewModel = new ViewModelProvider(requireActivity(), factory).get(NotesViewModel.class);
    }

    private void observeViewModel() {
        Log.d(TAG, "observeViewModel");
        // Наблюдаем за ошибками аутентификации
        viewModel.getErrors().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.d(TAG, "getErrors updated");
                if (error instanceof AuthError) {
                    AuthError authError = (AuthError) error;
                    String message;
                    switch (authError.getReason()) {
                        case SESSION_EXPIRED:
                            message = "Сессия истекла. Войдите снова";
                            break;
                        case NOT_AUTHENTICATED:
                            message = "Требуется аутентификация";
                            break;
                        default:
                            message = "Ошибка доступа";
                    }
                    showMessage(message);
                    navigateToLogin();
                } else {
                    showMessage(error.getMessage());
                }

            }
        });
    }

    private void navigateToLogin() {
        Log.i(TAG, "navigateToLogin");
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_global_auth);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Navigation error", e);
        }
    }

    private void loadNoteData(long noteId) {
        Disposable disposable;
        disposable = viewModel.getNoteById(noteId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::displayNote,
                        error -> {
                            showError(error);
                            Log.e(TAG, "Failed to decrypt note: " + noteId, error); }
                );
        disposables.add(disposable);
    }

    private void displayNote(Note note) {
        if (!isAdded() || getView() == null)
            return;

        binding.noteTitle.setText(note.getTitle());
        binding.noteContent.setText(note.getContent());
        binding.switchSecret.setChecked(note.isSecret());
        binding.switchSecret.jumpDrawablesToCurrentState();

        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(note.getCreatedAt()),
                ZoneId.systemDefault());

        binding.noteDate.setText(dateTime.format(dateTimeFormatter));
    }
    
    private void saveNote() {
        String title = binding.noteTitle.getText().toString().trim();
        String content = binding.noteContent.getText().toString().trim();
        boolean isSecret = binding.switchSecret.isChecked();

        if (noteId > 0) {
            updateExistingNote(title, content, isSecret);
        } else {
            createNewNote(title, content, isSecret);
        }
    }

    private void createNewNote(String title, String content, boolean secret) {
        NoteCreateDto noteCreateDto = new NoteCreateDto(title, content, secret, System.currentTimeMillis());
        Disposable disposable = viewModel.addNote(noteCreateDto)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            showMessage("SAVED");
                            navigateBack();
                        },
                        error -> {
                            Log.e(TAG, "Error adding note", error);
                            showError(error);
                        }
                );

        disposables.add(disposable);
    }

    private void showError(@NonNull Throwable error) {
        if (error.getMessage() != null) {
            showMessage(error.getMessage());
        }
    }


    private void showMessage(String message) {
        if (isAdded() && getView() != null) {
            Snackbar.make(getView(), message, Toast.LENGTH_SHORT).show();
        }
    }


    private void updateExistingNote(String title, String content, boolean isSecret) {

        NoteUpdateDto updateDto = new NoteUpdateDto(noteId, title, content, isSecret);

        Disposable disposable;
        try {
            disposable = viewModel.updateNote(updateDto)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                Toast.makeText(requireContext(), "SAVED", Toast.LENGTH_SHORT).show();
                                navigateBack();
                            },
                            this::showError);
        } catch (AuthError | Exception e) {
            throw new RuntimeException(e);
        }
        disposables.add(disposable);
    }

    private void navigateBack() {
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.popBackStack();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!disposables.isDisposed()) {
            disposables.dispose();
        }
    }
}