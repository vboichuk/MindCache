package com.myapp.mindcache.ui.diary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.myapp.mindcache.R;
import com.myapp.mindcache.databinding.FragmentDiaryBinding;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DiaryFragment extends Fragment {

    private FragmentDiaryBinding binding;
    private RecyclerView recyclerView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DiaryViewModel diaryViewModel =
                new ViewModelProvider(this).get(DiaryViewModel.class);

        binding = FragmentDiaryBinding.inflate(inflater, container, false);

        binding.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show());

        View root = binding.getRoot();

        recyclerView = root.findViewById(R.id.recyclerView);
        setupRecyclerView();

//        final TextView textView = binding.textDiary;
//        diaryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        return root;
    }

    private void setupRecyclerView() {
        // 1. Создаём тестовые данные
        List<FeedItem> notes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            notes.add(new FeedItem(
                    LocalDateTime.of(2025, 06, 24, 12, 0, 0),
                    "📘",
                    "Запись " + (i+1),
                    "Первая строка текста номер " + (i+1)
                            + " Вторая строка с подробностями, Третья строка с дополнительной информацией"
            ));
        }

        // 2. Настраиваем RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        FeedAdapter adapter = new FeedAdapter(notes, note -> {
            // Обработка клика
            Toast.makeText(getContext(), "Выбрано: " + note.getTitle(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true); // Оптимизация
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}