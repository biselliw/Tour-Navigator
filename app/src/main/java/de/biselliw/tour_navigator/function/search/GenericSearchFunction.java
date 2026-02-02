package de.biselliw.tour_navigator.function.search;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import de.biselliw.tour_navigator.ui.ControlElements;

/**
 * Function to load track information from any source,
 * subclassed for special cases like wikipedia or OSM
 */
public abstract class GenericSearchFunction implements Runnable
{
    /** Reference to track */
    public TrackDetails track;

    /** error message */
	protected String _errorMessage = null;

    /** Coordinates to search for */
    protected double _searchLatitude = 0.0, _searchLongitude = 0.0;

    /** list model */
    protected TrackListModel _trackListModel;

    private final ControlElements _activity;
	/**
	 * Constructor
	 * @param inActivity parent activity
	 */
	public GenericSearchFunction(ControlElements inActivity, TrackListModel inTrackListModel) {
        _activity = inActivity;
        track = App.getTrack();
        _trackListModel = inTrackListModel;
    }

    public String getErrorMessage() {
            return _errorMessage;
    }

    /**
     * Get coordinates from current point (if any)
     * @param inPoint current point
     */
    protected void getSearchCoordinates(DataPoint inPoint) {
        if (inPoint == null) return;
        _searchLatitude  = inPoint.getLatitude().getDouble();
        _searchLongitude = inPoint.getLongitude().getDouble();
    }

    /**
     * Check if a point is already loaded
     */
    public boolean searchResultIsDuplicate(SearchResult inSearchResult) {
        if (_activity.getRecordAdapter() != null)
            return (_activity.getRecordAdapter().contains(inSearchResult.getDataPoint()));
        else
            return false;
    }

}
