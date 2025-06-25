package com.myapp.mindcache.datastorage;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.myapp.mindcache.model.FeedItem;
import com.myapp.mindcache.model.Note;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class DiaryViewModel extends AndroidViewModel {
    private static final String TAG = "DiaryViewModel";

    private final NoteRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final MutableLiveData<List<FeedItem>> notes = new MutableLiveData<>();
    private final MutableLiveData<Throwable> errors = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public DiaryViewModel(@NonNull Application application) {
        super(application);
        try {
            repository = new NoteRepository(application);
            loadFeedItems();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize NoteRepository", e);
        }
    }

    public LiveData<List<FeedItem>> getFeedItems() {
        return notes;
    }
    public LiveData<Throwable> getErrors() {
        return errors;
    }

    public void loadFeedItems() {
        isLoading.postValue(true);
        disposables.add(repository.getAllDecryptedNotes()
                .subscribeOn(Schedulers.io())
                .map(this::convertToFeedItems) // Конвертируем Note в FeedItem
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        notes::postValue,
                        errors::postValue
                ));
    }

    public Completable addNote(String title, String content) {
        if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        isLoading.postValue(true);
        return repository.addNote(title, content)
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    loadFeedItems(); // Обновляем список после добавления
                })
                .doOnError(error -> {
                    errors.postValue(error);
                    isLoading.postValue(false);
                });
    }

    // Новый метод для обновления заметки
    public Completable updateNote(Note note) {
        if (note == null || note.title == null || note.title.isEmpty() ||
                note.content == null || note.content.isEmpty()) {
            return Completable.error(new IllegalArgumentException("Note data is invalid"));
        }

        isLoading.postValue(true);
        return repository.updateNote(note)
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    loadFeedItems(); // Обновляем список после изменения
                })
                .doOnError(error -> {
                    errors.postValue(error);
                    isLoading.postValue(false);
                });
    }

    // Альтернативный метод для обновления (по ID и полям)
    public Completable updateNote(long id, String title, String content) {
        if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        isLoading.postValue(true);
        return repository.updateNote(id, title, content)
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    loadFeedItems(); // Обновляем список после изменения
                })
                .doOnError(error -> {
                    errors.postValue(error);
                    isLoading.postValue(false);
                });
    }

    private List<FeedItem> convertToFeedItems(List<Note> notes) {
        return notes.stream()
                .map(note -> new FeedItem(
                        note.id,
                        note.title,
                        note.content,
                        LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(note.createdAt),
                                ZoneId.systemDefault()),
                        getEmojiForNote(note)
                ))
                .sorted(Comparator.comparing(FeedItem::getDateTime).reversed())
                .collect(Collectors.toList());
    }

    private String getEmojiForNote(Note note) {
        String lowerTitle = note.title.toLowerCase();
        if (lowerTitle.contains("важно")) return "❗";
        if (lowerTitle.contains("идея")) return "💡";
        if (lowerTitle.contains("задача")) return "✅";
        return "📘";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
        Log.d(TAG, "ViewModel cleared");
    }

    public LiveData<Note> getNoteById(long noteId) {
        MutableLiveData<Note> result = new MutableLiveData<>();

        disposables.add(repository.getNoteById(noteId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        note -> {
                            if (note != null) {
                                result.postValue(note);
                            } else {
                                result.postValue(null);
                                errors.postValue(new Exception("Note not found"));
                            }
                        },
                        error -> {
                            errors.postValue(error);
                            result.postValue(null);
                        }
                ));

        return result;
    }
}