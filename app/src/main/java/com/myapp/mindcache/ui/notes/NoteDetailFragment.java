package com.myapp.mindcache.ui.notes;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.myapp.mindcache.MainActivity;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentNoteDetailBinding;
import com.myapp.mindcache.dto.NoteCreateDto;
import com.myapp.mindcache.dto.NoteUpdateDto;
import com.myapp.mindcache.utils.PreviewGenerator;
import com.myapp.mindcache.viewmodel.NotesViewModel;
import com.myapp.mindcache.viewmodel.NotesViewModelFactory;
import com.myapp.mindcache.model.Note;

import org.reactivestreams.Publisher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.noties.markwon.Markwon;
import io.noties.markwon.editor.MarkwonEditor;
import io.noties.markwon.editor.MarkwonEditorTextWatcher;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class NoteDetailFragment extends BaseFragment {
    private static final String ARG_NOTE_ID = "noteId";
    private static final String TAG = NoteDetailFragment.class.getSimpleName();

    private Markwon markwon;

    private long noteId = 0L;
    private Long datetime = null;
    private String rawContent;

    private FragmentNoteDetailBinding binding;

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMMM, yyyy", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNoteDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        setupActions();
        setupUIListeners();
        initViewModel();
        observeViewModel();
        setupMarkwon();

        if (getArguments() != null) {
            noteId = getArguments().getLong(ARG_NOTE_ID);
        }

        loadNote();
    }

    private void setupUIListeners() {
        binding.noteDate.setOnClickListener(this::showDatePicker);
        binding.noteContent.setOnFocusChangeListener((v, hasFocus) -> setEditMode(hasFocus));
    }

    private void setEditMode(boolean editMode) {

        if (editMode) {
            binding.noteContent.setText(rawContent);
        } else {
            rawContent = binding.noteContent.getText().toString().trim();
            if (!rawContent.startsWith("##"))
                rawContent = "## " + rawContent;
             markwon.setMarkdown(binding.noteContent, rawContent);
            // Опционально: делаем ссылки кликабельными
            binding.noteContent.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void setupMarkwon() {
        markwon = Markwon.create(this.requireContext());
        MarkwonEditor markwonEditor = MarkwonEditor.create(markwon);

        binding.noteContent.addTextChangedListener(
                MarkwonEditorTextWatcher.withProcess(markwonEditor));
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

        viewModel.getErrors().observe(getViewLifecycleOwner(), this::processError);
    }

    public void loadNote() {
        Disposable disposable = viewModel.getNoteById(noteId)
                .retryWhen(errors -> errors.flatMap(e -> {
                    if (e instanceof UserNotAuthenticatedException) {
                        return showBiometricPrompt()
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .observeOn(Schedulers.io())
                                .andThen(Flowable.just(1));
//                    } else if (e instanceof WrongKeyException) {
//                        return viewModel.invalidateSecretKey()
//                                .subscribeOn(AndroidSchedulers.mainThread())
//                                .observeOn(Schedulers.io())
//                                .andThen(Flowable.just(1));
                    }
                    return Flowable.error(e);
                }))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::displayNote, this::processError);

        disposables.add(disposable);
    }

    private void displayNote(Note note) {

        if (!isAdded() || getView() == null)
            return;

        if (note == null)
            return;

        binding.noteContent.setText(note.getContent());

        datetime = note.getCreatedAt();
        rawContent = note.getContent();

        updateDateTimeText();
        setEditMode(false);
    }

    private void updateDateTimeText() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(datetime), ZoneId.systemDefault());
        binding.noteDate.setText(dateTime.format(dateTimeFormatter));
    }

    private void saveNote() {

        String title = PreviewGenerator.getTitle(binding.noteContent.getText().toString());
        String preview = PreviewGenerator.getPreview(binding.noteContent.getText().toString());
        String content = rawContent;

        if (datetime == null)
            datetime = System.currentTimeMillis();

        viewModel.saveDraft(noteId, title, content);

        Completable operation = noteId > 0L
                ? viewModel.updateNote(new NoteUpdateDto(noteId, title, content, preview, datetime))
                : viewModel.addNote(new NoteCreateDto(title, content, preview, datetime));

        Disposable disposable = operation
                .doOnError(e -> Log.d(TAG, "save note failed with " + e.getClass() + ": " + e.getMessage()))
                .retryWhen(retryOnAuthError())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            showMessage(R.string.msg_saved);
                            navigateBack();
                        },
                        this::showError);
        disposables.add(disposable);
    }

    private @NonNull Function<Flowable<Throwable>, Publisher<?>> retryOnAuthError() {
        return throwableFlowable -> throwableFlowable
                .ofType(UserNotAuthenticatedException.class)
                .zipWith(Flowable.range(1, 5), (throwable, attempt) -> attempt)
                .flatMap(attempt -> showBiometricPrompt()
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.io())
                        .andThen(Flowable.just(1))  // ← После onComplete эмитим 1
                        .onErrorResumeNext(error -> {
                            return Flowable.error(error);
                        })
                );
    }

    private void showDatePicker(View v) {

        FragmentManager fragmentManager = (requireActivity()).getSupportFragmentManager();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker
                .Builder
                .datePicker()
                .setSelection(datetime)
                .setTitleText(R.string.select_date)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            datetime = selection;
            updateDateTimeText();
        });

        datePicker.show(fragmentManager, "DATE_PICKER");
    }
}