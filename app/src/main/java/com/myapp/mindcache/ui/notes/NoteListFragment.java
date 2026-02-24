package com.myapp.mindcache.ui.notes;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.MainActivity;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentNotesListBinding;
import com.myapp.mindcache.exception.AuthError;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.viewmodel.NotesViewModel;
import com.myapp.mindcache.viewmodel.NotesViewModelFactory;
import com.myapp.mindcache.model.NotePreview;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class NoteListFragment extends Fragment {

    private static final String TAG = NoteListFragment.class.getSimpleName();
    private static final int PREFETCH_LIMIT = 10;

    private FragmentNotesListBinding binding;
    private NotesViewModel viewModel;
    private NoteListAdapter noteListAdapter;
    private BottomSheetDialog bottomSheetDialog;

    private final CompositeDisposable disposables = new CompositeDisposable();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotesListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupClickListeners();
        setupRecyclerView();
        initViewModel();
        observeViewModel();
    }

    private void setupClickListeners() {
        binding.addButton.setOnClickListener(v -> this.onAddClick());
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        noteListAdapter = new NoteListAdapter(this::onNoteClick, this::onNoteVisible, this::onNoteLongClick);
        recyclerView.setAdapter(noteListAdapter);
        recyclerView.setHasFixedSize(true);
    }

    private void initViewModel() {
        MainActivity activity = (MainActivity) requireActivity();
        NotesViewModelFactory factory = activity.getNotesViewModelFactory();
        viewModel = new ViewModelProvider(requireActivity(), factory).get(NotesViewModel.class);
    }

    private void observeViewModel() {

        Log.i(TAG, "observeViewModel");

        viewModel.getErrors().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.e(TAG, "Error occurred", error);
                if (isAdded()) {
                    Snackbar.make(binding.getRoot(),
                                    R.string.error_loading_notes,
                                    Snackbar.LENGTH_LONG)
                            .setAction(R.string.retry, v -> {
                            })
                            .show();
                }
            }
        });

        viewModel.getNotesMetadata().observe(getViewLifecycleOwner(), this::onFetchMetadata);

        viewModel.getCachedPreviews().observe(getViewLifecycleOwner(), items -> {
            Log.d(TAG, "decryptedNotes were updated with " + items.size() + " items");
            noteListAdapter.updateItems(items.values());
        });
    }

    private void onFetchMetadata(List<NoteMetadata> metadataList) {
        Log.i(TAG, "notes metadata was updated with " + metadataList.size() + " items");
        noteListAdapter.submitMetadata(metadataList);

        List<Long> ids = metadataList.stream()
                .map(NoteMetadata::getId)
                .collect(Collectors.toList());

        // send cached to adapter
        Map<Long, NotePreview> cachedPreviews = viewModel.getCachedPreviews().getValue();
        if (cachedPreviews != null) {
            Set<Long> idSet = new HashSet<>(ids);
            List<NotePreview> cached = cachedPreviews.entrySet().stream()
                    .filter(kv -> idSet.contains(kv.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            noteListAdapter.updateItems(cached);
        }

        // prefetch non-cached notes
        List<Long> missing = ids.stream()
                .filter(id -> cachedPreviews == null || !cachedPreviews.containsKey(id))
                .limit(PREFETCH_LIMIT)
                .collect(Collectors.toList());
        try {
            viewModel.prefetchNotes(missing);
//        } catch (UserNotAuthenticatedException e) {
//            throw new RuntimeException(e);
        } catch (AuthError e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void onAddClick() {
         NavController navController = Navigation.findNavController(requireView());
         navController.navigate(R.id.action_diary_to_noteDetail);
    }

    private void onNoteClick(NotePreview notePreview) {
        long noteId = notePreview.getId();
        Log.d(TAG, "onNoteClick id:" + noteId);
        Bundle args = new Bundle();
        args.putLong("noteId", noteId);

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_diary_to_noteDetail, args);
    }

    private void onNoteVisible(long l) {
        try {
            viewModel.prefetchNote(l);
//        } catch (UserNotAuthenticatedException e) {
//            throw new RuntimeException(e);
        } catch (AuthError | Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void onNoteLongClick(NotePreview notePreview) {
        showBottomSheet(notePreview);
    }


    private void showBottomSheet(NotePreview notePreview) {
        Context context = this.getContext();
        if (context == null || !isAdded()) {
            return;
        }

        View sheetLayoutView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_layout, null);

        Button btnDelete = sheetLayoutView.findViewById(R.id.btnDelete);

        btnDelete.setOnClickListener(v -> {
            showDeleteNoteDialog(notePreview);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(sheetLayoutView);
        bottomSheetDialog.setOnDismissListener(dialog -> bottomSheetDialog = null);

        if (!isDetached() && !isRemoving()) {
            bottomSheetDialog.show();
        }
    }

    private void showDeleteNoteDialog(NotePreview note) {
        if (!isAdded() || isDetached()) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
                requireContext()
        );

        builder.setTitle(R.string.confirm_delete_note)
                .setMessage(getString(R.string.ask_delete_note) + " \"" + note.getTitle() + "\"?" )
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (isAdded()) {
                        deleteNote(note.getId());
                    }
        })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            // Только если фрагмент еще жив
            if (isAdded()) {
                // Если нужно изменить цвет - делаем это безопасно
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    positiveButton.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.delete_red)
                    );
                }
            }
        });

        if (!isRemoving() && !isDetached()) {
            dialog.show();
        }
    }

    private void deleteNote(long id) {
        Disposable subscribe = viewModel.deleteNote(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Toast.makeText(requireContext(), "DELETED", Toast.LENGTH_LONG).show(),
                        error -> Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show()
                );
        disposables.add(subscribe);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!disposables.isDisposed()) {
            disposables.dispose();
        }
    }
}