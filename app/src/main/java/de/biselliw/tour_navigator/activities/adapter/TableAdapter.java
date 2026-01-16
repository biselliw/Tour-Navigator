package de.biselliw.tour_navigator.activities.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.dialogs.SearchResultDialog;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {

    private final SearchResultDialog parent;
    private final List<String[]> data;
    private List<Integer> selectedPositions = null;

    public TableAdapter(SearchResultDialog parent, List<String[]> data) {
        this.parent = parent;
        this.data = data;
        selectedPositions = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result, parent, false);
        return new ViewHolder(v);
    }

    boolean position_selected = false;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.col1.setText(data.get(position)[0]);
        holder.col2.setText(data.get(position)[1]);

        // Highlight selection
        holder.itemView.setBackgroundColor(0xFFFFFFFF);
            holder.itemView.setBackgroundColor(
                selectedPositions.contains(position)
                        ? 0xFFE0E0E0
                        : 0xFFFFFFFF
        );

        holder.itemView.setOnLongClickListener(v -> {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove((Integer) position);
//                selectedPositions.remove(selectedPositions.indexOf(position));
            } else {
                selectedPositions.add(position);
            }
            notifyItemChanged(position);
            parent.notifySelectionChanged(position, selectedPositions);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            position_selected = selectedPositions.contains(position);
            selectedPositions.clear();
            if (!position_selected) {
                selectedPositions.add(position);
            }

            notifyDataSetChanged();
            parent.notifySelectionChanged(position, selectedPositions);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView col1, col2;

        ViewHolder(View itemView) {
            super(itemView);
            col1 = itemView.findViewById(R.id.search_result_name);
            col2 = itemView.findViewById(R.id.search_result_distance);
        }
    }
}
