package de.biselliw.tour_navigator.function.search;

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

    You should have received a copy of the GNU General Public LicenseIf not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.content.res.AssetManager;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.stubs.Config;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import de.biselliw.tour_navigator.ui.ControlElements;
import tim.prune.data.Distance;
import tim.prune.data.Unit;

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

    protected final ControlElements _activity;
    public AssetManager assetManager;


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
     * find nearest trackpoint to a found point
     * @param inSearchResult search result data
     * @param inMaxDistance  max distance between found point ond track
     * @return true if nearest trackpoint was found;
     * if found: set the distance of the found point with regard to the start of the track
     */
    public boolean findNearestTrackpoint (SearchResult inSearchResult, double inMaxDistance) {
        boolean found = false;
        double minDistance = 999.99, foundDistance = -999.0;
        DataPoint searchPoint = inSearchResult.getDataPoint();
        Unit distUnit = Config.getUnitSet().getDistanceUnit();
        if (searchPoint != null && !searchResultIsDuplicate(inSearchResult)) {
            for (int i = 0; i < track.getNumPoints(); i++) {
                DataPoint trackPoint = track.getPoint(i);
                if (trackPoint != null && !trackPoint.isWaypoint()) {
                    double dist = DataPoint.calculateRadiansBetween(searchPoint, trackPoint);
                    if (dist > 0.0) {
                        double distance = Distance.convertRadiansToDistance(dist, distUnit);
                        if (inMaxDistance > 0.0 && distance < inMaxDistance) {
                            foundDistance = trackPoint.getDistance();
                            found = true;
                            break;
                        }
                        else {
                            if (distance < minDistance) {
                                minDistance = distance;
                                foundDistance = trackPoint.getDistance();
                            }
                        }
                    }
                }
            }
        }

        // set the distance of the found point with regard to the start of the track
        if (found || foundDistance >= 0.0)
            inSearchResult.setLength(foundDistance * 1000.0);
        return found;
    }

    /**
     * Check if a point is already loaded
     */
    public boolean searchResultIsDuplicate(SearchResult inSearchResult) {
        if (_activity != null && _activity.getRecordAdapter() != null)
            return (_activity.getRecordAdapter().contains(inSearchResult.getDataPoint()));
        else
            return false;
    }

}
