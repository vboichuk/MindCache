package com.myapp.mindcache.ui.notes;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.myapp.mindcache.MainActivity;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentNoteDetailBinding;
import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
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
        setupClickListeners();
        initViewModel();

        if (getArguments() != null) {
            long id = getArguments().getLong(ARG_NOTE_ID);
            if (id > 0L) {
                this.noteId = id;
                loadNoteData(noteId);
            }
        }
    }

    private void setupClickListeners() {

        binding.noteDetailsToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                saveNote();
                return true;
            }
            return false;
        });
    }

    private void initViewModel() {
        MainActivity activity = (MainActivity) requireActivity();
        NotesViewModelFactory factory = activity.getViewModelFactory();
        viewModel = new ViewModelProvider(requireActivity(), factory).get(NotesViewModel.class);
    }

    private void loadNoteData(long noteId) {
        Disposable disposable = viewModel.getNoteById(noteId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::displayNote,
                        error -> Log.e("Diary", "Failed to decrypt note: " + noteId, error)
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
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "SAVED", Toast.LENGTH_SHORT).show();
                                navigateBack();
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Error adding note", throwable);
                            if (isAdded()) {
                                Toast.makeText(requireContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                );
        disposables.add(disposable);
    }

    private void updateExistingNote(String title, String content, boolean isSecret) {

        NoteUpdateDto updateDto = new NoteUpdateDto(noteId, title, content, isSecret);

        Disposable disposable = viewModel.updateNote(updateDto)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Toast.makeText(requireContext(), "SAVED", Toast.LENGTH_SHORT).show();
                            navigateBack();
                        },
                        error -> {
                            if (error instanceof UserNotAuthenticatedException)
                                Toast.makeText(requireContext(), "Авторизация протухла", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
        disposables.add(disposable);
    }

    private void navigateBack() {
        try {
            NavController navController = Navigation.findNavController(requireView());
            navController.popBackStack();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}