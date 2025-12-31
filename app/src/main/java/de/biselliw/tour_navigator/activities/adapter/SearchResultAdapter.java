package de.biselliw.tour_navigator.activities.adapter;

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

    You should have received a copy of the GNU General Public License. If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.dialogs.SearchResultDialog;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;

/**
 * class to handle all records of the timetable
 */
public class SearchResultAdapter extends BaseAdapter {

    /**
     * TAG for log messages.
     */
    static final String TAG = "SearchResultAdapter";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /**
     * Color Codes
     */
    private static final int COLOR_BG_SELECTED = 0xFFB3DBFB;
    private static final int COLOR_BG_RECORD = 0xFFFFFFFF;

    private SearchResultDialog _parent;
    TrackListModel _trackListModel = null;

    private int _selected = -1;

    private static class RecordViewHolder {
        public TextView nameView;
        public TextView distanceView;
    }

    /**
     * Constructor
     *
     * @param inDialog context
     *
     */
    public SearchResultAdapter(SearchResultDialog inDialog, ListView recordsView, TrackListModel inTrackListModel) {
        _parent = inDialog;
        _trackListModel = inTrackListModel;
        recordsView.setAdapter(this);

        // Create a Listener for this list view of places
        recordsView.setOnItemClickListener((adapter, v, inPlace, arg3) ->
                setTableRow(inPlace));
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    /**
     * update the list view of search results
     *
     * @param i         index of the result
     * @param view      returned view
     * @param viewGroup not used
     * @return view
     */
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        RecordViewHolder holder;

        if (view == null) {
            LayoutInflater recordInflater = (LayoutInflater) _parent.getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            // todo Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout's root element)
            view = recordInflater.inflate(R.layout.search_result, null);
            holder = new RecordViewHolder();
            holder.nameView = view.findViewById(R.id.search_result_name);
            holder.distanceView = view.findViewById(R.id.search_result_distance);
            view.setTag(holder);
        } else {
            holder = (RecordViewHolder) view.getTag();
        }

        if (i == _selected)
            view.setBackgroundColor(COLOR_BG_SELECTED);
        else
            view.setBackgroundColor(COLOR_BG_RECORD);

        SearchResult track = _trackListModel.getTrack(i);
        holder.nameView.setText(track.getTrackName());

        holder.distanceView.setText(_trackListModel.getValueAt(i,1).toString());

        return view;
    }

    @Override
    public int getCount() {
        int count = 0;
        if (_trackListModel != null)
            count = _trackListModel.getRowCount();
        return count;
    }

    @Override
    public RecordAdapter.Record getItem(int i) {

            return null;

    }

    /**
     * Get the selected item in the list of places
     *
     * @return Index (starting at 0) / -1 if nothing is selected
     */
    public int getSelected() {
        return _selected;
    }

    /**
     * Sets an item in the list of places
     *
     * @param inRow Index (starting at 0) of the data item to be selected or -1 if nothing
     */
    public void setTableRow(int inRow) {
        _selected = inRow;
        _parent.setDescription(_trackListModel.getTrack(inRow).getDescription());
        _parent.setTableRow(inRow);
        notifyDataSetChanged();
    }

}