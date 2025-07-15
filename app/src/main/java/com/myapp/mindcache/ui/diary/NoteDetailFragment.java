package com.myapp.mindcache.ui.diary;

import android.app.Activity;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import com.myapp.mindcache.R;
import com.myapp.mindcache.datastorage.DiaryViewModel;
import com.myapp.mindcache.datastorage.DiaryViewModelFactory;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.security.KeystoreSecureKeyManager;
import com.myapp.mindcache.security.PasswordManager;
import com.myapp.mindcache.security.PasswordManagerImpl;

import java.text.DateFormat;
import java.util.Date;

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

    private EditText editTextTitle;
    private EditText editTextContent;

    public static NoteDetailFragment newInstance(long noteId) {
        NoteDetailFragment fragment = new NoteDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    public static NoteDetailFragment newInstance() {
        return new NoteDetailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

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

        return view;
    }

    private void initViewModel() {
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

    private void setupEmptyNote() {
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        // Настройка Toolbar
//        Toolbar toolbar = view.findViewById(R.id.toolbar);
//        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
//
//        // Добавляем кнопку назад
//        ActionBar supportActionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
//        if (supportActionBar != null) {
//            supportActionBar.setDisplayHomeAsUpEnabled(true);
//            supportActionBar.setDisplayShowHomeEnabled(true);
//            supportActionBar.setDisplayShowTitleEnabled(false);
//        }
//
//        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
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

    private void displayNote(Note note) {
        View view = getView();
        if (view == null) return;

        editTextTitle.setText(note.getTitle());
        editTextContent.setText(note.getContent());

        TextView dateView = view.findViewById(R.id.note_date);
        dateView.setText(DateFormat.getDateTimeInstance().format(new Date(note.getCreatedAt())));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.note_toolbar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        String title = editTextTitle.getText().toString();
        String content = editTextContent.getText().toString();

        if (noteId > 0) {
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