package com.myapp.mindcache.ui.notes;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.mindcache.R;
import com.myapp.mindcache.mappers.NodeMapper;
import com.myapp.mindcache.model.Note;
import com.myapp.mindcache.model.NoteMetadata;
import com.myapp.mindcache.model.NotePreview;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NoteListAdapter extends RecyclerView.Adapter<NoteListAdapter.ViewHolder> {
    private static final String TAG = NoteListAdapter.class.getSimpleName();

    private List<NotePreview> displayList = new ArrayList<>();
    private final Map<Long, Integer> positionCache = new HashMap<>();

    private final OnItemClickListener clickListener;
    private final OnItemVisibleListener visibleListener;
    private final OnItemLongClickListener longClickListener;

    private final DateTimeFormatter formatterDay =
            DateTimeFormatter.ofPattern("dd", Locale.getDefault());
    private final DateTimeFormatter formatterMon =
            DateTimeFormatter.ofPattern("MMMM", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(NotePreview note);
    }

    public interface OnItemVisibleListener {
        void onItemVisible(long noteId);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(NotePreview note);
    }


    public void submitMetadata(List<NoteMetadata> metadata) {

        List<Long> ids = metadata.stream().map(NoteMetadata::getId).collect(Collectors.toList());
        Log.d(TAG, "submitMetadata with ids: " + ids);

        if (metadata.size() == displayList.size()) {
            throw new IllegalStateException("submitMetadata");
        }

        // Строим новый displayList в правильном порядке
        List<NotePreview> newDisplayList = new ArrayList<>();

        for (NoteMetadata meta : metadata) {
            long id = meta.getId();
            Integer pos = positionCache.get(id);
            if (pos != null) {
                NotePreview existing = displayList.get(pos);
                newDisplayList.add(existing);
            } else {
                newDisplayList.add(NodeMapper.toPreview(meta));
            }
        }
        updateDisplayList(newDisplayList);
    }

    private void updateDisplayList(List<NotePreview> newList) {
        List<NotePreview> oldList = new ArrayList<>(displayList);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return oldList.size(); }

            @Override
            public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldList.get(oldPos).getId() == newList.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                NotePreview oldItem = oldList.get(oldPos);
                NotePreview newItem = newList.get(newPos);
                // Сравниваем по ID + уровень детализации
                return oldItem.getId() == newItem.getId() &&
                        Objects.equals(oldItem.getTitle(), newItem.getTitle());
            }

            @Override
            @Nullable
            public Object getChangePayload(int oldPos, int newPos) {
                // Можно вернуть только изменившиеся поля для частичного обновления
                return super.getChangePayload(oldPos, newPos);
            }
        });

        displayList = newList;

        positionCache.clear();
        IntStream.range(0, displayList.size())
                .forEach(i -> positionCache.put(displayList.get(i).getId(), i));
        diffResult.dispatchUpdatesTo(this);

        Log.d(TAG, "Display list updated. Size: " + displayList.size());
    }

    boolean notesAreEqual(NotePreview preview, Note note) {
        return preview.getId() == note.getId() && preview.getTitle().equals(note.getTitle());
    }

    public void updateItems(Collection<Note> updatedNotes) {
        boolean changed = false;
        for (Note note : updatedNotes) {
            Integer pos = positionCache.get(note.getId());
            if (pos != null && !notesAreEqual(displayList.get(pos), note)) {
                displayList.set(pos, NodeMapper.toPreview(note));
                notifyItemChanged(pos);
                changed = true;
            }
        }

        if (changed) {
            Log.d(TAG, "Updated " + updatedNotes.size() + " notes in place");
        }
    }

    public NoteListAdapter(OnItemClickListener clickListener,
                           OnItemVisibleListener visibleListener,
                           OnItemLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.visibleListener = visibleListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotePreview note = getNote(position);

        // Если заметка не расшифрована и видна - сообщаем через коллбэк
        if (note.isEncrypted()) {
            visibleListener.onItemVisible(note.getId());
        }

        holder.tvDateDay.setText(note.getDateTime().format(formatterDay));
        holder.tvDateMon.setText(note.getDateTime().format(formatterMon));

        holder.tvEmoji.setText(note.getEmoji());
        holder.tvLock.setVisibility(note.isSecret() ? View.VISIBLE : View.INVISIBLE);
        
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(note));
        holder.itemView.setOnLongClickListener(v -> { longClickListener.onItemLongClick(note); return true; });
    }


    private NotePreview getNote(int position) {
        return displayList.get(position);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateDay, tvDateMon, tvLock, tvEmoji, tvTitle, tvContent;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDateDay = itemView.findViewById(R.id.tvDateDay);
            tvDateMon = itemView.findViewById(R.id.tvDateMon);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvLock = itemView.findViewById(R.id.tvLock);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }
}