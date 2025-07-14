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
import java.util.Objects;
import java.util.stream.Collectors;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class DiaryViewModel extends AndroidViewModel {
    private static final String TAG = DiaryViewModel.class.getSimpleName();

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
        final char[] password = "password".toCharArray();
        disposables.add(repository.getAllDecryptedNotes(password)
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

        final char[] password = "password".toCharArray();
        isLoading.postValue(true);
        return repository.addNote(title, content, password)
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
        if (note == null || note.getTitle() == null || note.getTitle().isEmpty() ||
                note.getContent() == null || note.getContent().isEmpty()) {
            return Completable.error(new IllegalArgumentException("Note data is invalid"));
        }

        final char[] password = "password".toCharArray();
        isLoading.postValue(true);
        return repository.updateNote(note, password)
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
        final char[] password = "password".toCharArray();
        return repository.updateNote(id, title, content, password)
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    loadFeedItems(); // Обновляем список после изменения
                })
                .doOnError(error -> {
                    Log.e(TAG, Objects.requireNonNull(error.getMessage()));
                    errors.postValue(error);
                    isLoading.postValue(false);
                });
    }

    private List<FeedItem> convertToFeedItems(List<Note> notes) {
        return notes.stream()
                .map(note -> new FeedItem(
                        note.getId(),
                        note.getTitle(),
                        note.getContent(),
                        LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(note.getCreatedAt()),
                                ZoneId.systemDefault()),
                        getEmojiForNote(note)
                ))
                .sorted(Comparator.comparing(FeedItem::getDateTime).reversed())
                .collect(Collectors.toList());
    }

    private String getEmojiForNote(Note note) {
        String lowerTitle = note.getTitle().toLowerCase();
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
        final char[] password = "password".toCharArray();
        disposables.add(repository.getNoteById(noteId, password)
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

    public Completable deleteNote(long id) {
        isLoading.postValue(true);
        isLoading.postValue(true);

        // Убедитесь, что добавляете disposable в CompositeDisposable

        Completable completable = repository.deleteNote(id) // Уже возвращает Completable
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    loadFeedItems(); // Обновляем список после изменения
                })
                .doOnError(error -> {
                    errors.postValue(error);
                    isLoading.postValue(false);
                });

//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        () -> {
//                            isLoading.postValue(false);
//                            loadFeedItems();
//                            Log.d(TAG, "Note deleted successfully");
//                        },
//                        error -> {
//                            errors.postValue(error);
//                            isLoading.postValue(false);
//                            Log.e(TAG, "Error deleting note", error);
//                        }
//                );
        return completable;
    }
}