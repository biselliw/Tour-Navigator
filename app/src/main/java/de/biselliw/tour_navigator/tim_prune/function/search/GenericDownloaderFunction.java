package de.biselliw.tour_navigator.tim_prune.function.search;

import android.content.res.Resources;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

/**
 * Function to load track information from any source,
 * subclassed for special cases like wikipedia or OSM
 */
public abstract class GenericDownloaderFunction implements Runnable
{
    public static Resources resources = App.resources;

    /** error message */
	protected String _errorMessage = null;

    /** Coordinates to search for */
    protected double _searchLatitude = 0.0, _searchLongitude = 0.0;

    protected DataPoint dataPoint = null;

    /** Reference to track */
    public TrackDetails track;

    /** list model */
    protected TrackListModel _trackListModel = null;

	/**
	 * Constructor
	 * @param inApp App object
	 */
	public GenericDownloaderFunction(App inApp, TrackListModel inTrackListModel) {
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


}
