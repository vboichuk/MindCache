package com.myapp.mindcache.ui.diary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.mindcache.R;
import com.myapp.mindcache.model.FeedItem;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {
    private List<FeedItem> items;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;
    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(FeedItem note);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(FeedItem note);
    }

    public void updateItems(List<FeedItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvEmoji, tvTitle, tvContent;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }

    public FeedAdapter(List<FeedItem> notes,
                       OnItemClickListener listener,
                       OnItemLongClickListener longClickListener) {
        this.items = notes;
        this.clickListener = listener;
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
        FeedItem note = items.get(position);

        holder.tvDate.setText(note.getDateTime().format(formatter));
        holder.tvEmoji.setText(note.getEmoji());
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(note));
        holder.itemView.setOnLongClickListener(v -> { longClickListener.onItemLongClick(note); return true; });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}