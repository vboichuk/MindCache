package com.myapp.mindcache.ui.diary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.mindcache.R;
import com.myapp.mindcache.model.FeedItem;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {
    private List<FeedItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(FeedItem note);
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

    public FeedAdapter(List<FeedItem> notes, OnItemClickListener listener) {
        this.items = notes;
        this.listener = listener;
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
        holder.tvDate.setText(note.getDateTime().toString());
        holder.tvEmoji.setText(note.getEmoji());
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(note));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}