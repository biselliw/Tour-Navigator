package de.biselliw.tour_navigator.fragments;

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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.DialogFragment;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.adapter.TableAdapter;
import de.biselliw.tour_navigator.function.search.GenericSearchFunction;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import de.biselliw.tour_navigator.ui.ControlElements;

import static android.view.View.GONE;
import static androidx.core.content.ContextCompat.getColor;


public class SearchResultDialogFragment extends DialogFragment {

    private SearchResultDialogFragment _dialog = this;

    protected static GenericSearchFunction searchFunction = null;

    /**
     * notification routine to be called after adding route points
     */
    protected Runnable notification = null;

    private final Handler _timerHandler = new Handler();

    /**
     * Status label
     */
    private TextView _statusLabel;

    /**
     * Number of columns (1,2,3)
     */
    private int _numColumns = 0;

    /**
     * list model
     */
    protected static TrackListModel trackListModel = null;

    /**
     * Description box
     */
    private TextView _descriptionBox = null;

    private TableAdapter _adapter = null;

    private List<Integer> _selectedPositions;

    /**
     * Load button(s)
     */
    protected Button loadButton = null;
    protected static Button loadButtonAll = null;
    /**
     * Show button
     */
    protected static Button showButton = null;
    /**
     * Cancelled flag
     */
    protected boolean cancelled = false;

    /**
     * language code used for search
     */
    protected static String lang;

    /**
     * prefix for waypoint types (needed for GPX file)
     */
    protected static String prefixWaypointType;

    /**
     * data point to search for points around
     */
    protected static DataPoint dataPoint;

    /**
     * Basic dialog for searching POIs
     *
     * @param inActivity context of the class
     * @param inTitle    dialog title
     * @param inPoint    data point to search for points around
     */
    public static SearchResultDialogFragment newInstance(ControlElements inActivity, String inTitle, DataPoint inPoint) {
        SearchResultDialogFragment fragment = new SearchResultDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", inTitle);
        fragment.setArguments(args);
        dataPoint = inPoint;
        return fragment;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_FullScreenDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        lang = getString(R.string.lang);
        _numColumns = getColumnCount();

        // Main panel with track list
        trackListModel = new TrackListModel(_numColumns);
        // Clear list
        trackListModel.clear();

        super.onAttach(context);
    }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_search_result, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog == null) return;

        Window window = dialog.getWindow();
        if (window == null) return;
        window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String title = getArguments() != null
                ? getArguments().getString("title")
                : "";

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        TextView titleView = view.findViewById(R.id.search_title);
        titleView.setText(title);

        // Status label
        _statusLabel = view.findViewById(R.id.statusLabel);

        TextView [] columnTitleViewIDs = new TextView[3];

        // column titles
        columnTitleViewIDs[0] = view.findViewById(R.id.search_result_distance);
        columnTitleViewIDs[1] = view.findViewById(R.id.search_result_name);
        columnTitleViewIDs[2] = view.findViewById(R.id.search_result_type);
        for (int i = 0; i < _numColumns; i++)
            columnTitleViewIDs[i].setText(getColumnTitle(i));

        // description view supporting basic HTML tags
        _descriptionBox = view.findViewById(R.id.desc_search_result_view);
        _descriptionBox.setText("");

        /* define OnClick event to load the results into the track */
        loadButton = view.findViewById(R.id.btn_load);
        loadButton.setOnClickListener(v -> loadSelected(_selectedPositions));
        loadButton.setEnabled(false);

        /* define OnClick event to load all results into the track */
        loadButtonAll = view.findViewById(R.id.btn_load_all);
        loadButtonAll.setOnClickListener(v -> loadAll());
        loadButtonAll.setVisibility(GONE);
        loadButtonAll.setEnabled(false);

        /* define OnClick event to show the selected result */
        showButton = view.findViewById(R.id.btn_show);
        showButton.setOnClickListener(v -> showSelected(_selectedPositions.get(0)));
        showButton.setVisibility(View.VISIBLE);
        showButton.setEnabled(false);

        /* define OnClick event to cancel the dialog */
        Button cancelButton = view.findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(v -> {
            cancelled = true;
            dismiss();
        });
        cancelled = false;

        /* Install a timer to handle all activities */
        //                    if (loadButtonAll.getVisibility() == VISIBLE)
        Runnable _timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (trackListModel.changed) {
                    if (searchFunction.getErrorMessage().isEmpty()) {
                        showStatus("");

                        List<String[]> _results = new ArrayList<>();
                        for (int row = 0; row < trackListModel.getRowCount(); row++) {
                            switch (_numColumns) {
                                case 1:
                                    _results.add(new String[]{
                                        trackListModel.getValueAt(row, getColumnKey(0))});
                                    break;
                                case 2:
                                    _results.add(new String[]{
                                        trackListModel.getValueAt(row, getColumnKey(0)),
                                        trackListModel.getValueAt(row, getColumnKey(1))});
                                    break;
                                case 3:
                                    _results.add(new String[]{
                                        trackListModel.getValueAt(row, getColumnKey(0)),
                                        trackListModel.getValueAt(row, getColumnKey(1)),
                                        trackListModel.getValueAt(row, getColumnKey(2))});
                                    break;
                            }
                        }

                        // create new table holding the search results
                        _adapter = new TableAdapter(_dialog, _results, _numColumns);
                        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
                        recyclerView.setAdapter(_adapter);
                    } else
                        showErrorMessage(searchFunction.getErrorMessage());
                    trackListModel.changed = false;
//                    if (loadButtonAll.getVisibility() == VISIBLE)
                    loadButtonAll.setEnabled(!trackListModel.isEmpty());
                }
                _timerHandler.postDelayed(this, 100);
            }
        };
        _timerHandler.postDelayed(_timerRunnable, 100);
    }

    /**
     * Set a notification routine to be called after adding route points
     *
     * @param inNotification Runnable as callback function
     */
    public SearchResultDialogFragment setNotification(Runnable inNotification) {
        notification = inNotification;
        return this;
    }

    protected void showStatus(String inStatus) {
        _statusLabel.setText(inStatus);
        _statusLabel.setTextColor(getColor(requireContext(), R.color.black));
    }

    protected void showErrorMessage(String inErrorMessage) {
        _statusLabel.setText(inErrorMessage);
        _statusLabel.setTextColor(getColor(requireContext(), R.color.red));
    }

    /**
     * @return column count
     */
    public int getColumnCount()
    {
        return _numColumns;
    }

    /**
     * Get column titles
     *
     * @param inColNum index of column
     * @return key for this column
     * @implNote: not used
     */
    protected String getColumnTitle(int inColNum) {
        return "";
    }

    /**
     * Get column key for interpretation of contents
     *
     * @param inColNum index of column (1,2,3)
     * @return key for this column
     * @implNote: not used
     */
    protected String getColumnKey(int inColNum) {
        return "";
    }

    /**
     * Set the description in the box
     *
     * @param inDescription description to set, or null for no description
     */
    public void setDescription(String inDescription) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            _descriptionBox.setText(Html.fromHtml(inDescription, Html.FROM_HTML_MODE_LEGACY));
        } else {
            _descriptionBox.setText(Html.fromHtml(inDescription));
        }
        _descriptionBox.scrollTo(0, 0);
    }

    /**
     * notify change of selected position(s)
     *
     * @param selectedPositions list of selected positions
     */
    public void notifySelectionChanged(int position, List<Integer> selectedPositions) {
        _selectedPositions = selectedPositions;
        boolean foundUrl = false;
        showStatus("");
        setDescription("");
        if (selectedPositions.isEmpty()) {
            // nothing selected
            loadButton.setEnabled(false);
            loadButtonAll.setEnabled(false);
            showButton.setEnabled(false);
        } else {
            if (selectedPositions.size() == 1) {
                // single row selected
                SearchResult searchResult = trackListModel.getTrack(selectedPositions.get(0));
                if (searchResult != null) {
                    foundUrl = !searchResult.getWebUrl().isEmpty();
                }
                loadButton.setEnabled(true);
                loadButtonAll.setEnabled(true);
                showButton.setEnabled(foundUrl);
                setDescription(trackListModel.getTrack(position).getDescription());
            } else {
                loadButton.setEnabled(true);
                loadButtonAll.setEnabled(true);
                showButton.setEnabled(false);
            }
        }
    }

    /**
     * Load the selected point(s)
     */
    protected void loadSelected(List<Integer> selectedPositions) {
        for (int i = 0; i < selectedPositions.size(); i++) {
            int selected = selectedPositions.get(i);
            // Find the row selected in the table and get the corresponding coords
            loadItem(trackListModel.getTrack(selected));
        }
        if (notification != null)
            notification.run();

        // Close the dialog
        cancelled = true;
        dismiss();
    }

    /**
     * Load all points
     */
    protected void loadAll() {
        for (int i = 0; i < trackListModel.getRowCount(); i++) {
            loadItem(trackListModel.getTrack(i));
        }
        if (notification != null)
            notification.run();

        // Close the dialog
        cancelled = true;
        dismiss();
    }

    /**
     * Load the selected point
     */
    protected void loadItem(SearchResult searchResult) {
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
                if (point.getIndex() <= 0)
                    searchFunction.track.appendPoint(point);
                searchFunction.track.linkWaypoint(point);
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
            requireContext().startActivity(Intent.createChooser(intent, "Open with"));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        _timerHandler.removeCallbacksAndMessages(null);

        _dialog = null;
        _adapter = null;
        searchFunction = null;
        notification = null;
        _statusLabel = null;

        trackListModel = null;
        _descriptionBox = null;

        if (_selectedPositions != null) {
            _selectedPositions.clear();
            _selectedPositions = null;
        }

        loadButton = null; loadButtonAll = null;
        showButton = null;

        super.onDestroyView();
    }
}
