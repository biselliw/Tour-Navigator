package de.biselliw.tour_navigator.adapter;

/*
    This file is part of Tour Navigator

    Tour Navigator is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    Tour Navigator is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.fragments.SearchResultDialogFragment;

/**
 * Adapter provide a binding from an app-specific data set to views that are displayed within a RecyclerView.
 * @see SearchResultDialogFragment
 */
public class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {

    private final SearchResultDialogFragment _parent;
    /** contents of the table */
    private final List<String[]> _data;
    /**
     * Number of columns (1,2,3)
     */
    private int _numColumns;

    /** list of all selected items of the table */
    private List<Integer> _selectedPositions;


    /**
     * constructs a new TableAdapter
     * @param parent  parent fragment
     * @param data    contents of the table
     * @param numColumns number of columns of the table
     */
    public TableAdapter(SearchResultDialogFragment parent, List<String[]> data, int numColumns) {
        _parent = parent;
        _data = data;
        _numColumns = numColumns;
        _selectedPositions = new ArrayList<>();
    }

    /**
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return         new ViewHolder
     * FIXME           Class 'ViewHolder' is exposed outside its defined visibility scope
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result, parent, false);
        return new ViewHolder(v);
    }

    boolean position_selected = false;

    /**
     *
     * @param holder
     * @param position The position of a data item within the Adapter
     */
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        for (int col = 0; col < _numColumns; col++)
            holder.col[col].setText(_data.get(position)[col]);

        // Highlight selection
        holder.itemView.setBackgroundColor(0xFFFFFFFF);
        holder.itemView.setBackgroundColor(
            _selectedPositions.contains(position)
                    ? 0xFFE0E0E0
                    : 0xFFFFFFFF
        );

        holder.itemView.setOnLongClickListener(v -> {
            if (_selectedPositions.contains(position)) {
                _selectedPositions.remove((Integer) position);
            } else {
                _selectedPositions.add(position);
            }
            notifyItemChanged(position);
            _parent.notifySelectionChanged(position, _selectedPositions);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            position_selected = _selectedPositions.contains(position);
            _selectedPositions.clear();
            if (!position_selected) {
                _selectedPositions.add(position);
            }

            notifyDataSetChanged();
            _parent.notifySelectionChanged(position, _selectedPositions);
        });
    }

    @Override
    public int getItemCount() {
        return _data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView [] col = new TextView[3];

        ViewHolder(View itemView) {
            super(itemView);
            col[0] = itemView.findViewById(R.id.search_result_distance);
            col[1] = itemView.findViewById(R.id.search_result_name);
            col[2] = itemView.findViewById(R.id.search_result_type);
        }
    }
}
