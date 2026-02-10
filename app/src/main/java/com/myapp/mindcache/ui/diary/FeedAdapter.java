package com.myapp.mindcache.ui.diary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.mindcache.R;
import com.myapp.mindcache.model.FeedItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {
    private List<FeedItem> items = new ArrayList<>();

    private final OnItemClickListener clickListener;
    private final OnItemVisibleListener visibleListener;
    private final OnItemLongClickListener longClickListener;

    private final Map<Long, Integer> positionsMap = new HashMap<>();

    private final DateTimeFormatter formatterDay =
            DateTimeFormatter.ofPattern("dd", Locale.getDefault());
    private final DateTimeFormatter formatterMon =
            DateTimeFormatter.ofPattern("MMMM", Locale.getDefault());



    public interface OnItemClickListener {
        void onItemClick(FeedItem note);
    }

    public interface OnItemVisibleListener {
        void onItemVisible(long noteId);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(FeedItem note);
    }

    public void updateItems(List<FeedItem> items) {
        this.items = items;
        positionsMap.clear();
        IntStream.range(0, items.size())
                .forEach(i -> positionsMap.put(items.get(i).getId(), i));

        notifyDataSetChanged();
    }

    public void updateItem(Long id, FeedItem feedItem) {

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == feedItem.getId()) {
                items.set(i, feedItem);
                notifyItemChanged(i);
                break;
            }
        }

//        Integer pos = positionsMap.get(id);
//        if (pos != null)
//            notifyItemChanged(pos);

    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateDay, tvDateMon, tvEmoji, tvTitle, tvContent;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDateDay = itemView.findViewById(R.id.tvDateDay);
            tvDateMon = itemView.findViewById(R.id.tvDateMon);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }

    public FeedAdapter(OnItemClickListener listener, OnItemVisibleListener visibleListener,
                       OnItemLongClickListener longClickListener) {
        this.clickListener = listener;
        this.visibleListener = visibleListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.feed_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FeedItem note = getNote(position);

        // Если заметка не расшифрована и видна - сообщаем через коллбэк
        if (note.isEncrypted() /* && !note.isLoading() */) {
            visibleListener.onItemVisible(note.getId());
        }

        holder.tvDateDay.setText(note.getDateTime().format(formatterDay));
        holder.tvDateMon.setText(note.getDateTime().format(formatterMon));

        holder.tvEmoji.setText(note.getEmoji());
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(note));
        holder.itemView.setOnLongClickListener(v -> { longClickListener.onItemLongClick(note); return true; });
    }


    private FeedItem getNote(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}