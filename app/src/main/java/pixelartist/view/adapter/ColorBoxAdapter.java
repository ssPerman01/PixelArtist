package pixelartist.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import pixelartist.listeners.ItemListeners;
import rodrigodavy.com.github.pixelartist.R;

public class ColorBoxAdapter extends RecyclerView.Adapter<ColorBoxAdapter.TextViewHolder> {

    private List<Integer> _dataSource;
    private ItemListeners<Integer> itemListeners;

    public ColorBoxAdapter(List<Integer> dataSource, ItemListeners<Integer> itemListeners) {
        _dataSource = dataSource;
        this.itemListeners = itemListeners;
    }

    @NonNull
    @Override
    public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_color, parent, false);
        return new TextViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
        holder.item.setBackgroundColor(_dataSource.get(position));
        holder.item.setOnClickListener( view -> itemListeners.onClick(holder.getAdapterPosition(), _dataSource.get(position)));
    }

    @Override
    public int getItemCount() {
        return _dataSource.size();
    }

    class TextViewHolder extends RecyclerView.ViewHolder {

        private Button item;
        TextViewHolder(View cardView) {
            super(cardView);
            item = cardView.findViewById(R.id.btn_color_item);
        }

    }

}
