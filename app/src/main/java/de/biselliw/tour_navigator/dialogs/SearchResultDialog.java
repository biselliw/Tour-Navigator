package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.SearchResultAdapter;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim_prune.function.search.GenericDownloaderFunction;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import tim.prune.data.Latitude;
import tim.prune.data.Longitude;

public abstract class SearchResultDialog extends FullScreenDialog {
    Context _context;
    GenericDownloaderFunction _searchFunction = null;

    Handler timerHandler = new Handler();

    /** Status label */
    protected TextView _statusLabel;
    /** track table */
    protected SearchResultAdapter _trackTable = null;
    /** list model */
    protected TrackListModel _trackListModel = null;
    /** Description box */
    private TextView _descriptionBox = null;
    /** Load button */
    private Button _loadButton = null;
     /** Show button */
    private Button _showButton = null;
    /** Cancelled flag */
    protected boolean _cancelled = false;

    /** language code used for search */
    protected String lang;

    protected String _waypointType = "";
    protected boolean _protectWaypoint = false;

    DataPoint _dataPoint;

    public SearchResultDialog(Context context, DataPoint inPoint) {
        super(context, R.layout.search_result_dialog);
        _dataPoint = inPoint;
        _context = context;
        lang = context.getString(R.string.lang);

        // Status label
        _statusLabel = findViewById(R.id.statusLabel);

        // Main panel with track list
        _trackListModel = new TrackListModel(getColumnKey(0), getColumnKey(1));

        _trackTable = new SearchResultAdapter(this, findViewById(R.id.search_result_view), _trackListModel);

        _descriptionBox = findViewById(R.id.desc_search_result_view);

        /* define OnClick event for changing the start time */
        _loadButton = findViewById(R.id.bt_load);
        _loadButton.setOnClickListener(v -> {
            loadSelected();;
        });

        _showButton = findViewById(R.id.bt_show);
        _showButton.setOnClickListener(v -> showSelected());

        Button cancelButton = findViewById(R.id.bt_cancel);
        cancelButton.setOnClickListener(v -> {
            _cancelled = true;
            dismiss();
        });

        // Clear list
        _trackListModel.clear();

        _loadButton.setEnabled(false);
        _showButton.setEnabled(false);
        _cancelled = false;

        _descriptionBox.setText("");

        /* Install a timer to handle all activities */
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (_trackListModel.changed) {
                    if (_searchFunction.getErrorMessage().isEmpty()) {
                        showStatus("");
                        _trackTable.notifyDataSetChanged();
                    }
                    else
                        showErrorMessage(_searchFunction.getErrorMessage());
                    _trackListModel.changed = false;
                }
                timerHandler.postDelayed(this, 100);
            }
        };
        timerHandler.postDelayed(timerRunnable, 100);
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
     * @param inColNum index of column, 0 or 1
     * @return key for this column
     */
    protected abstract String getColumnKey(int inColNum);

    /**
     * Set the description in the box
     * @param inDesc description to set, or null for no description
     */
    public void setDescription(String inDesc)
    {
        _descriptionBox.setText(inDesc);
    }

    /**
     * Get the selected item in the list of places
     *
     * @return Index (starting at 0) / -1 if nothing is selected
     */
    public int getSelected() {
        return _trackTable.getSelected();
    }

    /**
     * update search result
     *
     * @param inRow Index (starting at 0) of the data item to be selected or -1 if nothing
     */
    public void setTableRow(int inRow) {
        boolean foundUrl = false;
        SearchResult searchResult = _trackListModel.getTrack(inRow);
        if (searchResult != null) {
            foundUrl = searchResult.getWebUrl() != null;
        }
        _loadButton.setEnabled(true);
        _showButton.setEnabled(getSelected() >=0 && foundUrl);
    }

    /**
     * Load the selected point
     */
    protected void loadSelected()
    {
		// Find the row selected in the table and get the corresponding coords
        SearchResult searchResult = _trackListModel.getTrack(getSelected());
        if (searchResult != null) {
            String lat = searchResult.getLatitude();
            String lon = searchResult.getLongitude();
            if (lat != null && lon != null)
            {
                DataPoint point = new DataPoint(Latitude.make(lat), Longitude.make(lon));
                point.setWaypointName(searchResult.getTrackName());
                point.setFieldValue(Field.DESCRIPTION,searchResult.getDescription(),false);
                point.setFieldValue(Field.WAYPT_TYPE,_waypointType,false);
                if (_protectWaypoint)
                    point.makeProtectedWaypoint();
                point.setFieldValue(Field.WAYPT_LINK,searchResult.getWebUrl(),false);
                // Check if the track already contains the point
                if (App.getTrack().contains(point))
                    showErrorMessage(_context.getString(R.string.point_already_loaded));
                else {
                    App.getTrack().appendPoint(point);
                    App.app.recalculate();

                    // Close the dialog
                    _cancelled = true;
                    dismiss();
                }
           }
        }
	}

    abstract void showSelected();
}
