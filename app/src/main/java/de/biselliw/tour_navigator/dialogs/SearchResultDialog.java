package de.biselliw.tour_navigator.dialogs;

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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.Html;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.TableAdapter;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim_prune.function.search.GenericDownloaderFunction;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;

import static android.view.View.GONE;
import static androidx.constraintlayout.widget.ConstraintSet.VISIBLE;
import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.OUT_OF_TRACK;

/**
 * Search dialog to add items to the track
 */

public abstract class SearchResultDialog extends FullScreenDialog {
    private final SearchResultDialog _dialog = this;

    protected Context context;
    protected GenericDownloaderFunction searchFunction = null;

    private final Handler _timerHandler = new Handler();

    /** Status label */
    private final TextView _statusLabel;

    /** list model */
    protected TrackListModel trackListModel = null;
    /** Description box */
    private TextView _descriptionBox = null;
    private List<Integer> _selectedPositions;
    /** Load button */
    protected Button loadButton = null, loadButtonAll = null;
     /** Show button */
    protected Button showButton = null;
    /** Cancelled flag */
    protected boolean cancelled = false;

    /** language code used for search */
    protected String lang;

    /** prefix for waypoint types (needed for GPX file) */
    protected String prefixWaypointType;

    /** data point to search for points around */
    protected DataPoint dataPoint;

    /**
     * Basic dialog for searching POIs
     * @param inContext context of the class
     * @param inTitle dialog title
     * @param inPoint data point to search for points around
     */
    public SearchResultDialog(Context inContext, String inTitle, DataPoint inPoint) {
        super(inContext, R.layout.search_result_dialog);
        dataPoint = inPoint;
        context = inContext;
        lang = inContext.getString(R.string.lang);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // dialog title
        TextView view = findViewById(R.id.search_title);
        view.setText(inTitle);

        // Status label
        _statusLabel = findViewById(R.id.statusLabel);

        // Main panel with track list
        trackListModel = new TrackListModel(getColumnKey(0), getColumnKey(1));

        // description view supporting basic HTML tags
        _descriptionBox = findViewById(R.id.desc_search_result_view);
        _descriptionBox.setText("");

        /* define OnClick event to load the results into the track */
        loadButton = findViewById(R.id.btn_load);
        loadButton.setOnClickListener(v -> loadSelected(_selectedPositions));
        loadButton.setEnabled(false);

        /* define OnClick event to load all results into the track */
        loadButtonAll = findViewById(R.id.btn_load_all);
        loadButtonAll.setOnClickListener(v -> loadAll());
        loadButtonAll.setVisibility(GONE);
        loadButtonAll.setEnabled(false);

        /* define OnClick event to show the selected result */
        showButton = findViewById(R.id.btn_show);
        showButton.setOnClickListener(v -> showSelected(_selectedPositions.get(0)));
        showButton.setVisibility(VISIBLE);
        showButton.setEnabled(false);

        /* define OnClick event to cancel the dialog */
        Button cancelButton = findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(v -> {
            cancelled = true;
            dismiss();
        });
        cancelled = false;

        // Clear list
        trackListModel.clear();

        /* Install a timer to handle all activities */
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (trackListModel.changed) {
                    if (searchFunction.getErrorMessage().isEmpty()) {
                        showStatus("");

                        List<String[]> data = new ArrayList<>();
                        for (int i = 0; i < trackListModel.getRowCount(); i++) {
                            data.add(new String[]{trackListModel.getValueAt(i,0).toString(),
                                            trackListModel.getValueAt(i,1).toString()});
                        }

                        TableAdapter adapter = new TableAdapter(_dialog, data);
                        RecyclerView recyclerView = findViewById(R.id.recyclerView);
                        recyclerView.setAdapter(adapter);
                    }
                    else
                        showErrorMessage(searchFunction.getErrorMessage());
                    trackListModel.changed = false;
                    if (loadButtonAll.getVisibility() == VISIBLE)
                        loadButtonAll.setEnabled(true);
                }
                _timerHandler.postDelayed(this, 100);
            }
        };
        _timerHandler.postDelayed(timerRunnable, 100);
    }

    protected void showStatus(String inStatus) {
        _statusLabel.setText(inStatus);
        _statusLabel.setTextColor(R.color.black);
    }

    protected void showErrorMessage(String inErrorMessage) {
        _statusLabel.setText(inErrorMessage);
        _statusLabel.setTextColor(R.color.red);
    }

    /**
     * Get keys for column titles
     * @param inColNum index of column, 0 or 1
     * @return key for this column
     * @implNote: not used
     */
    protected abstract String getColumnKey(int inColNum);

    /**
     * Set the description in the box
     * @param inDescription description to set, or null for no description
     */
    public void setDescription(String inDescription)
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            _descriptionBox.setText(Html.fromHtml(inDescription, Html.FROM_HTML_MODE_LEGACY));
        } else {
            _descriptionBox.setText(Html.fromHtml(inDescription));
        }
        _descriptionBox.scrollTo(0, 0);
    }

    /**
     * notify change of selected position(s)
     * @param selectedPositions list of selected positions
     */
    public void notifySelectionChanged(int position, List<Integer> selectedPositions) {
        _selectedPositions = selectedPositions;
        boolean foundUrl = false;
        showStatus("");
        setDescription("");
        if (selectedPositions.isEmpty())
        {
            // nothing selected
            loadButton.setEnabled(false);
            loadButtonAll.setEnabled(false);
            showButton.setEnabled(false);
        }
        else {
            if (selectedPositions.size() == 1)
            {
                // single row selected
                SearchResult searchResult = trackListModel.getTrack(selectedPositions.get(0));
                if (searchResult != null) {
                    foundUrl = !searchResult.getWebUrl().isEmpty();
                }
                loadButton.setEnabled(true);
                loadButtonAll.setEnabled(true);
                showButton.setEnabled(foundUrl);
                setDescription(trackListModel.getTrack(position).getDescription());
            }
            else {
                loadButton.setEnabled(true);
                loadButtonAll.setEnabled(true);
                showButton.setEnabled(false);
            }
        }
    }

    /**
     * Load the selected point(s)
     */
    protected void loadSelected(List<Integer> selectedPositions)
    {
        for (int i = 0; i < selectedPositions.size(); i++) {
            int selected = selectedPositions.get(i);
            // Find the row selected in the table and get the corresponding coords
            loadItem(trackListModel.getTrack(selected));
        }
        App.app.recalculate();

        // Close the dialog
        cancelled = true;
        dismiss();
	}

    /**
     * Load all points
     */
    protected void loadAll()
    {
        for (int i = 0; i < trackListModel.getRowCount(); i++) {
            loadItem(trackListModel.getTrack(i));
        }
        App.app.recalculate();

        // Close the dialog
        cancelled = true;
        dismiss();
    }

    /**
     * Load the selected point
     */
    protected void loadItem(SearchResult searchResult)
    {
        if (searchResult != null) {
            DataPoint point = searchResult.getDataPoint();
            if (point != null) {
                point.makeProtectedWaypoint();
                if (prefixWaypointType != null) {
                    String waypointType = point.getWaypointType();
                    if (waypointType.isEmpty())
                        waypointType = prefixWaypointType;
                    else
                        waypointType = prefixWaypointType + ": " + waypointType;
                    point.setFieldValue(Field.WAYPT_TYPE, waypointType, false);
                }
                // add a new waypoint to the track
                if (point.getLinkIndex() != OUT_OF_TRACK)
                    searchFunction.track.appendPoint(point);
            }
        }
    }

    /**
     * Show the selected point in the web browser
     */
    void showSelected(int selected) {
        SearchResult searchResult = trackListModel.getTrack(selected);
        if (searchResult != null) {
            String url = searchResult.getWebUrl();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(Intent.createChooser(intent, "Open with"));
        }
    }
}
