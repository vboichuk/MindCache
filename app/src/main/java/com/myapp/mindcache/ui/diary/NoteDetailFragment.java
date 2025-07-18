package com.myapp.mindcache.ui.diary;

import android.app.Activity;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.datastorage.DiaryViewModel;
import com.myapp.mindcache.datastorage.DiaryViewModelFactory;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.security.AndroidKeystoreKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;

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
    private DiaryViewModel viewModel;
    private Long noteId = 0L;

    private TextView textDate;
    private EditText editTextTitle;
    private EditText editTextContent;

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMMM, HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_detail, container, false);

        initViewModel();

        if (getArguments() != null) {
            long id = getArguments().getLong(ARG_NOTE_ID);
            if (id > 0L) {
                this.noteId = id;
                loadNoteData(noteId);
            }
        }
        else {
            setupEmptyNote();
        }

        editTextTitle = view.findViewById(R.id.note_title);
        editTextContent = view.findViewById(R.id.note_content);
        textDate = view.findViewById(R.id.note_date);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Получаем Toolbar из разметки фрагмента
        MaterialToolbar toolbar = view.findViewById(R.id.note_details_toolbar);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                saveNote();
                return true;
            }
            return false;
        });
    }


    private void initViewModel() {
        AndroidKeystoreKeyManager secureKeyManager = null;
        try {
            Activity activity = this.getActivity();
            assert activity != null;
            secureKeyManager = new AndroidKeystoreKeyManager();
            PasswordManager passwordManager = new PasswordManagerImpl(activity.getApplication(), secureKeyManager);
            DiaryViewModelFactory factory = new DiaryViewModelFactory(activity.getApplication(), passwordManager);
            viewModel = new ViewModelProvider(this, factory).get(DiaryViewModel.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadNoteData(long noteId) {
        viewModel.getNoteById(noteId).observe(getViewLifecycleOwner(), note -> {
            if (note != null) {
                displayNote(note);
            } else {
                Toast.makeText(requireContext(), "Failed to load note id:" + noteId, Toast.LENGTH_LONG).show();
                navigateBack();
            }
        });
    }

    private void setupEmptyNote() {
    }

    private void displayNote(Note note) {
        View view = getView();
        if (view == null)
            return;

        editTextTitle.setText(note.getTitle());
        editTextContent.setText(note.getContent());

        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(note.getCreatedAt()),
                ZoneId.systemDefault());

        textDate.setText(dateTime.format(dateTimeFormatter));
    }
    
    private void saveNote() {
        String title = editTextTitle.getText().toString();
        String content = editTextContent.getText().toString();

        if (noteId > 0) {
            //noinspection unused
            Disposable subscribe = viewModel.updateNote(noteId, title, content)
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
        }
        else {
            // Подписка на Completable из ViewModel
            disposables.add(
                    viewModel.addNote(title, content)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe(d -> {})
                            .doFinally(() -> {})
                            .subscribe(
                                    () -> {
                                        // Успешное добавление
                                        Toast.makeText(requireContext(), "SAVED", Toast.LENGTH_SHORT).show();
                                        navigateBack();
                                    },
                                    throwable -> {
                                        // Обработка ошибок
                                        Log.e(TAG, "Error adding note", throwable);
                                        String errorMsg = throwable.getMessage();
                                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                                    }
                            )
            );
        }
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