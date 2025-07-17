package com.myapp.mindcache.datastorage;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.myapp.mindcache.mappers.NodeMapper;
import com.myapp.mindcache.model.FeedItem;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.security.CryptoHelper;
import com.myapp.mindcache.security.KeyGenerator;
import com.myapp.mindcache.security.KeyGeneratorImpl;
import com.myapp.mindcache.security.NoteEncryptionService;
import com.myapp.mindcache.security.PasswordManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
    private List<Note> cachedNotes = null;

    private final PasswordManager passwordManager;

    public DiaryViewModel(@NonNull Application application, PasswordManager passwordManager) {
        super(application);
        this.passwordManager = passwordManager;
        try {
            KeyGenerator generator = new KeyGeneratorImpl();
            CryptoHelper cryptoHelper = new CryptoHelper();
            NoteEncryptionService service = new NoteEncryptionService(generator, cryptoHelper);
            repository = new NoteRepository(application, service);
            // loadFeedItems();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize NoteRepository", e);
        }
    }

    public LiveData<List<FeedItem>> getFeedItems() {
        return notes;
    }

    public LiveData<Note> getNoteById(long noteId) {
        MutableLiveData<Note> result = new MutableLiveData<>();
        char[] password = passwordManager.getUserPassword().toCharArray();
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

    public LiveData<Throwable> getErrors() {
        return errors;
    }

    public void loadAllNotes(boolean forceRefresh) {
        Log.d(TAG, "loadAllNotes");
        if (cachedNotes != null && !forceRefresh) {
            notes.postValue(convertToFeedItems(cachedNotes));
            return;
        }
        isLoading.postValue(true);
        char[] password = passwordManager.getUserPassword().toCharArray();
        disposables.add(repository.getAllNotes(password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        notes -> {
                            cachedNotes = notes;
                            this.notes.postValue(convertToFeedItems(notes));
                            isLoading.postValue(false);
                        },
                        error -> {
                            errors.postValue(error);
                            isLoading.postValue(false);
                        }
                ));
    }

    public Completable addNote(String title, String content) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        isLoading.postValue(true);
        char[] password = passwordManager.getUserPassword().toCharArray();

        Note newNote = new Note(title, content, System.currentTimeMillis());

        return repository.insertNote(newNote, password)
                .doOnSuccess(id -> {
                    Log.i(TAG, "Successfully added note with id: " + id);
                    newNote.setId(id);
                    if (cachedNotes != null)
                        cachedNotes.add(0, newNote);
                })
                .ignoreElement() // Преобразуем Single<Long> в Completable
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                })
                .doOnError(error -> {
                    Log.e(TAG, "Error adding note", error);
                    errors.postValue(error);
                    isLoading.postValue(false);
                })
                .doOnDispose(() -> {
                    // Очистка пароля при отмене операции
                    if (password != null) {
                        Arrays.fill(password, '\0');
                    }
                });
    }

    public Completable updateNote(long id, String title, String content) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            return Completable.error(new IllegalArgumentException("Title and content cannot be empty"));
        }

        isLoading.postValue(true);
        char[] password = passwordManager.getUserPassword().toCharArray();

        return repository.updateNote(id, title, content, password)
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    // Не нужно обновлять notes вручную - Flowable из Room сделает это автоматически
                })
                .doOnError(error -> {
                    Log.e(TAG, "Error updating note", error);
                    errors.postValue(error);
                    isLoading.postValue(false);
                })
                .doOnDispose(() -> {
                    // Очистка пароля при отмене операции
                    if (password != null) {
                        Arrays.fill(password, '\0');
                    }
                });
    }

    public Completable deleteNote(long id) {
        isLoading.postValue(true);

        Completable completable = repository.deleteNote(id) // Уже возвращает Completable
                .doOnComplete(() -> {
                    isLoading.postValue(false);
                    if (cachedNotes != null) {
                        cachedNotes.removeIf(note -> note.getId() == id);
                        notes.postValue(convertToFeedItems(cachedNotes));
                    }
                })
                .doOnError(error -> {
                    errors.postValue(error);
                    isLoading.postValue(false);
                });
        return completable;
    }


    private List<FeedItem> convertToFeedItems(List<Note> notes) {
        return notes.stream()
                .map(NodeMapper::toFeed)
                .sorted(Comparator.comparing(FeedItem::getDateTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
        Log.d(TAG, "ViewModel cleared");
    }
}