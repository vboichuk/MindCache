package com.myapp.mindcache.ui.diary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.myapp.mindcache.R;
import com.myapp.mindcache.datastorage.NoteRepository;
import com.myapp.mindcache.model.Note;

import java.text.DateFormat;
import java.util.Date;
import java.util.Optional;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class NoteDetailFragment extends Fragment {
    private static final String ARG_NOTE_ID = "note_id";
    private static final String TAG = NoteDetailFragment.class.getSimpleName();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private NoteRepository repository;
    private Optional<Long> noteId = Optional.empty();

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
        try {
            repository = new NoteRepository(requireContext());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error initializing repository", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_detail, container, false);

        if (getArguments() != null) {
            long noteId = getArguments().getLong(ARG_NOTE_ID);
            this.noteId = Optional.of(noteId);
            loadNoteData(noteId, view);
        }

        editTextTitle = view.findViewById(R.id.note_title);
        editTextContent = view.findViewById(R.id.note_content);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Настройка Toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        // Добавляем кнопку назад
        ActionBar supportActionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void loadNoteData(long noteId, View rootView) {
        disposables.add(repository.getNoteById(noteId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::displayNote,
                        throwable -> {
                            Toast.makeText(requireContext(), "Failed to load note", Toast.LENGTH_SHORT).show();
                            requireActivity().onBackPressed();
                        }
                ));
    }

    private void displayNote(Note note) {
        View view = getView();
        if (view == null) return;

        TextView dateView = view.findViewById(R.id.note_date);

        editTextTitle.setText(note.title);
        editTextContent.setText(note.content);
        dateView.setText(DateFormat.getDateTimeInstance().format(new Date(note.createdAt)));
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}