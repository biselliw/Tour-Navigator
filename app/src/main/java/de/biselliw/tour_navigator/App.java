package de.biselliw.tour_navigator;
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

    Copyright 2025 Walter Biselli (BiselliW)
*/
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import java.util.List;

import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.data.AppState;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.helpers.GpsSimulator;

import de.biselliw.tour_navigator.tim_prune.data.Track;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.SourceInfo;
import de.biselliw.tour_navigator.tim_prune.data.TrackInfo;

import static de.biselliw.tour_navigator.activities.LocationActivity.TASK_COMPLETE;
import static de.biselliw.tour_navigator.activities.SettingsActivity.getConsentDebug;
import static de.biselliw.tour_navigator.data.AppState.gpsSimulation;
import static de.biselliw.tour_navigator.ui.ControlElements.control;

/**
 * @author BiselliW
 */
public class App {
    /**
     * TAG for log messages.
     */
    static final String TAG = "App";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    static public Resources resources = null;

    public static Uri gpxUri = null;

    private static SourceInfo _sourceInfo = null;
    private static TrackDetails _track = null;


    private static List<DataPoint> _refPointList = null;
    private static SourceInfo _refSourceInfo = null;
    private static boolean _refAppend = false;

    private TrackInfo _trackInfo = null;
    private final MainActivity _main;

    public static App app = null;



    public App(MainActivity main)  {
        app = this;
        _main = main;
        resources = main.getResources();

        // create empty track
        _track = new TrackDetails();
//        _trackInfo = new TrackInfo(_track);
    }

    /**
     * @return the current TrackInfo
     */
    public TrackInfo getTrackInfo()
    {
        return _trackInfo;
    }

    public void onLoadData(List<DataPoint> inPointList, SourceInfo inSourceInfo, boolean inAppend) {
        _refPointList = inPointList;
        _refSourceInfo = inSourceInfo;
        _refAppend = inAppend;
    }

    /**
     * Inform the app that the GPS simulation file has been loaded either successfully or cancelled
     */
    public void informUriFileLoadComplete() {
        if (DEBUG) Log.i(TAG, "informUriFileLoadComplete");

        Track loadedTrack = _track;
        if (loadedTrack.getNumPoints() > 0) {
            _sourceInfo = loadedTrack.getPoint(0).getSourceInfo();

            // update sources in TrackInfo
            _trackInfo.getFileInfo();

            recalculate();

            gpsSimulation = new GpsSimulator(_track);

            _main.OpenCachedFileGPX();
        }
    }

    /**
     * Inform the app that a file load process is complete, either successfully or cancelled
     */
    public synchronized void informDataLoadComplete()
    {
        boolean gpxFileValid = false;
        _sourceInfo = null;
        if (DEBUG) Log.d(TAG, "informDataLoadComplete");

        _sourceInfo = _refSourceInfo; _refSourceInfo = null;

        /* Check whether loaded array can be properly parsed into a Track */
        // delete all points of the current track
        _track = new TrackDetails();
        _trackInfo = new TrackInfo(_track);
        _track.deleteAllPoints();

        // append loaded points of the track
        _trackInfo.clearFileInfo();
        _track.appendRange(_refPointList); _refPointList = null;

        if (_track.getNumPoints() <= 0) {
            control.showErrorMessage(_main.getString(R.string.gpx_error_no_points));
        }
        else if (!_track.hasTrackPoints()) {
            control.showErrorMessage(_main.getString(R.string.gpx_error_no_trackpoints));
        }
        else if (!_track.hasAltitudes()) {
            control.showErrorMessage(_main.getString(R.string.gpx_error_no_altitudes));
        }
        else if (_track.isValid())// if (loadedTrack.hasWaypoints())
        {
            gpxFileValid = true;
        }
        else if (getConsentDebug()) {
            gpxFileValid = true;
        }
        else {
            control.showErrorMessage(_main.getString(R.string.gpx_error_no_waypoints));
        }

        if (gpxFileValid) {
            if (_track.getNumPoints() > 0) {
                _sourceInfo = _track.getPoint(0).getSourceInfo();
            }
            // update sources in TrackInfo
            _trackInfo.getFileInfo();
            recalculate();
        }

        if (gpxFileValid) {
            if (!_track.hasWaypoints() && !_track.hasNamedTrackpoints())
            {
                // GPS simulation can only be used after initial loading of a GPX file after opening
                // the app
                if (gpsSimulation == null)
                {
                    control.showErrorMessage(_main.getString(R.string.gpx_info_simulation));
                    // remember the uri of the loaded file in case of automatic reload
                    AppState.setGpxSimulationUri(gpxUri);
                    gpsSimulation = new GpsSimulator(_track);
                }
            }
            else
            {
                if (gpsSimulation != null)
                    gpsSimulation.Reset(AppState.getGpxSimulationIndex());
            }
        }

        _main.handleState(this, TASK_COMPLETE);
    }

    /**
     * Recalculate all track points
     */
    public void recalculate() {
        List<RecordAdapter.Record> recordList = _track.recalculate();
        control.notifyDataSetChanged(recordList);
        control.updateGpxFile = true;
    }


    public void Update() {
        if (_track == null) return;

        int numPoints = _track.getNumPoints();

        if (DEBUG) {
            Log.d(TAG, "Update: " + numPoints + " Trackpoints");
        }
        if (numPoints > 0) {
            recalculate();
        }
//        _recordAdapter.notifyDataSetChanged();
    }

    public static SourceInfo getSourceInfo() {
        return _sourceInfo;
    }

    public DataPoint getPoint(int ptIndex) {
        return _track.getPoint(ptIndex);
    }

    public static TrackDetails getTrack()
    {
        return _track;
    }

    /**
     * Reverse the route
     */
    public void reverseRoute()
    {
        if (_track != null)
        {
            _track.reverseRoute();
            Update();
        }
    }

    /**
     * Search for the given Point in the track and return the index
     * @param inPoint Point to look for
     * @return index of Point, if any or -1 if not found
     */
    public int getPointIndex(DataPoint inPoint) {
        return _track.getPointIndex(inPoint);
    }

    /**
     * Find the nearest track point to the specified Latitude and Longitude coordinates
     * within a given range of track points
     *
     * @param inStart       start index
     * @param inEnd         end index
     * @param inLatitude    Latitude in degrees
     * @param inLongitude   Longitude in degrees
     * @param inMaxDist     maximum tolerated distance [km] between geo location and point
     * @return <ul>
     * 	<li>&gt;= 0: index of nearest track point within the specified max distance</li>
     * 	<li>&lt; 0: index of nearest track point outside the specified max distance</li>
     * 	<li>&nbsp;DataPoint.INVALID_INDEX if no point is within the specified max distance </li>
     * </ul>
     * @author BiselliW
     */
    public int getNearestTrackpointIndex(int inStart, int inEnd, double inLatitude, double inLongitude, double inMaxDist) {
        return _track.getNearestTrackpointIndex(inStart, inEnd, inLatitude, inLongitude, inMaxDist);
    }

    /**
     * Return the nearest distance of a track point to the specified Latitude and Longitude coordinates.
     * Index of nearest track point must have been calculated using @see "getNearestPointIndex2()"
     * @return distance of nearest track point [km], negated if not within the specified max distance
     * @since BiselliW
     * - all coordinates in [km]
     */
    public double getNearestDistance() {
        return _track.getNearestDistance();
    }


}

