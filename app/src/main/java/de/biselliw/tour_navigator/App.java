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
import static de.biselliw.tour_navigator.helpers.GpsSimulator.gpsSimulation;
import static de.biselliw.tour_navigator.ui.ControlElements.control;

/**
 * @author BiselliW
 * @since 26.1
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
    private static Track _track = null;
    private static List<DataPoint> _points = null;
    private final TrackInfo _trackInfo;
    static TrackDetails _stats = null;
    private final MainActivity _main;
    private final RecordAdapter _recordAdapter;

    public static App app = null;
    static long _totalPause_min = 0L;

    // hiking speed parameters
    private double horSpeed, vertSpeedClimb, vertSpeedDescent;
    private int minHeightChange = 10;

    public String trackName = "";

    public App(MainActivity main)  {
        app = this;
        _main = main;
        resources = main.getResources();

        _recordAdapter = _main.recordAdapter;

        _track = new Track();
        _trackInfo = new TrackInfo(_track);
    }

    /**
     * @return the current TrackInfo
     */
    public TrackInfo getTrackInfo()
    {
        return _trackInfo;
    }

    public void deleteAllPoints() {
        _track.deleteAllPoints();
    }

    public void appendRange(List<DataPoint> inPoints) {
        _points = inPoints;
        _trackInfo.clearFileInfo();
        _trackInfo.appendRange(inPoints);
    }

    /**
     * Inform the app that the GPS simulation file has been loaded either successfully or cancelled
     */
    public void informUriFileLoadComplete() {
        if (DEBUG) Log.i(TAG, "informUriFileLoadComplete");

        SourceInfo sourceInfo = null;
        Track loadedTrack = _track;
        if (loadedTrack.getNumPoints() > 0) {
            _sourceInfo = loadedTrack.getPoint(0).getSourceInfo();

            // update sources in TrackInfo
            _trackInfo.getFileInfo();

            recalculate();
//            _main.handleState(this, TASK_COMPLETE);

            gpsSimulation = new GpsSimulator(_track);

            _main.OpenCachedFileGPX();
        }
    }

    /**
     * Inform the app that a file load process is complete, either successfully or cancelled
     */
    public void informDataLoadComplete()
    {
        SourceInfo sourceInfo = null;
        if (DEBUG) Log.d(TAG, "informDataLoadComplete");

        // Check whether loaded array can be properly parsed into a Track
        Track loadedTrack = _track;
        if (loadedTrack.getNumPoints() <= 0) {
            control.showErrorMessage(_main.getString(R.string.gpx_error_no_points));
            return;
        }
        else if (!loadedTrack.hasTrackPoints()) {
            control.showErrorMessage(_main.getString(R.string.gpx_error_no_trackpoints));
            return;
        }
        else if (!loadedTrack.hasAltitudes()) {
            control.showErrorMessage(_main.getString(R.string.gpx_error_no_altitudes));
            return;
        }
        else // if (loadedTrack.hasWaypoints())
            trackName = "";

        if (loadedTrack.getNumPoints() > 0) {
            sourceInfo = loadedTrack.getPoint(0).getSourceInfo();
        }
        // go directly to load
        informDataLoaded(loadedTrack, sourceInfo);
    }

    /**
     * Receive loaded data and optionally replace with current Track
     *
     * @param inLoadedTrack loaded track
     * @param inSourceInfo  information about the source of the data
     */
    public void informDataLoaded(Track inLoadedTrack, SourceInfo inSourceInfo) {
        // Decide whether to load or append
        _sourceInfo = inSourceInfo;
        // update sources in TrackInfo
        _trackInfo.getFileInfo();

        recalculate();
        _main.handleState(this, TASK_COMPLETE);

        if (!_track.hasWaypoints() && !_track.hasNamedTrackpoints())
        {
            // GPS simulation can only be used after initial loading of a GPX file after opening
            // the app
            if (gpsSimulation == null)
            {
                control.showErrorMessage(_main.getString(R.string.gpx_info_simulation));
                // remember the uri of the loaded file in case of automatic reload
                AppState.gpxSimulationUri = gpxUri;
                gpsSimulation = new GpsSimulator(_track);
            }
        }
        else
        {
            if (gpsSimulation != null)
                gpsSimulation.Reset();
        }
    }

    /**
     * Recalculate all track points
     */
    public void recalculate() {
        double sum_distance = 0.0;
        double sum_climb = 0;
        double sum_descent = 0;
        long sum_seconds = 0L;
        int PauseMin = 0;

        int numPoints = _track.getNumPoints();

        RecordAdapter.Record record;
        if (numPoints <= 0) return;

        if (DEBUG) {
            Log.d(TAG, "recalculate(): " + numPoints + " Trackpoints");
        }
        _stats = new TrackDetails(_track);

        _stats.setHikingParameters(horSpeed, vertSpeedClimb, vertSpeedDescent, minHeightChange);
        _track.interleaveWaypoints();
        if (_recordAdapter == null) return;
        _recordAdapter.RemoveRecords();
        _totalPause_min = 0L;

        for (int ptIndex = 0; ptIndex <= numPoints - 1; ptIndex++) {
            DataPoint currPoint = _track.getPoint(ptIndex);

            _stats.addPoint(ptIndex);
            if (currPoint.isRoutePoint())
            {

                record = new RecordAdapter.Record(
                        currPoint,
                        ptIndex,

                        _stats.getTotalDistance() - sum_distance,
                        _stats.getTotalClimb() - sum_climb,
                        _stats.getTotalDescent() - sum_descent,
                        currPoint.getTime() - sum_seconds - PauseMin * 60L
                );

                sum_distance = _stats.getTotalDistance();
                sum_climb = _stats.getTotalClimb();
                sum_descent = _stats.getTotalDescent();
                sum_seconds = currPoint.getTime();

                PauseMin = currPoint.getWaypointDuration();
                _totalPause_min += PauseMin;

                _recordAdapter.add(record);

            }
            if (DEBUG) {
                Log.d(TAG, "timetable built");
                Log.d(TAG, "Sclimb: " + _stats.getTotalClimb());
                Log.d(TAG, "Sdescent: " + _stats.getTotalDescent());
            }
            _main.TotalDistance = _stats.getTotalDistance();
        }

        control.updateGpxFile = true;
    }

    /**
     * set hiking parameters:
     *
     * @param inHorSpeed         horizontal part in [km/h]
     * @param inVertSpeedClimb   ascending part in [km/h]
     * @param inVertSpeedDescent descending part in [km/h]
     * @param inMinHeightChange  min. required altitude change between two trackpoints for calc.
     */
    public void setHikingParameters(double inHorSpeed, double inVertSpeedClimb, double inVertSpeedDescent, int inMinHeightChange) {
        horSpeed = inHorSpeed;
        vertSpeedClimb = inVertSpeedClimb;
        vertSpeedDescent = inVertSpeedDescent;
        minHeightChange = inMinHeightChange;

        Update();
    }

    public void Update() {
        if (_stats == null) return;

        int numPoints = _track.getNumPoints();

        if (DEBUG) {
            Log.d(TAG, "Update: " + numPoints + " Trackpoints");
        }
        if (numPoints > 0) {
            _stats.setHikingParameters(horSpeed, vertSpeedClimb, vertSpeedDescent, minHeightChange);
            recalculate();
        }
        _recordAdapter.notifyDataSetChanged();
    }

    public static SourceInfo getSourceInfo() {
        return _sourceInfo;
    }

    public DataPoint getPoint(int ptIndex) {
        return _track.getPoint(ptIndex);
    }

    public static Track getTrack()
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

    public static double getClimb() {
        if (_stats == null) return 0;
        return _stats.getTotalClimb();
    }

    public static double getDescent() {
        if (_stats == null) return 0;
        return _stats.getTotalDescent ();
    }

    public static long getTotalSeconds() {
        if (_stats == null) return 0L;
        return _stats.getTotalSeconds();
    }

    public static long getTotalPauseInMins() { return _totalPause_min; }

    public double getTotalDistance() {
        if (_stats == null) return 0.0;
        return _stats.getTotalDistance();
    }

    public double getMinAltitude() {
        if (_stats == null) return 0;
        return _stats.getMinAltitude();
    }

    public double getMaxAltitude() {
        if (_stats == null) return 0;
        return _stats.getMaxAltitude();
    }
}

