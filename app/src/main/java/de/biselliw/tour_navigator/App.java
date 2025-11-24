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
import android.util.Log;

import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.helpers.GpsSimulator;

import de.biselliw.tour_navigator.tim_prune.I18nManager;
import de.biselliw.tour_navigator.tim_prune.data.Track;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim.prune.data.PointCreateOptions;
import de.biselliw.tour_navigator.tim_prune.data.SourceInfo;
import de.biselliw.tour_navigator.tim_prune.data.TrackInfo;
import de.biselliw.tour_navigator.tim.prune.load.TrackNameList;

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

    private static SourceInfo _sourceInfo = null;
    private static Track _track = null;
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
        _recordAdapter = _main.recordAdapter;
        _track = new Track();
        _trackInfo = new TrackInfo(_track);

        I18nManager.init(main);
    }

    /**
     * @return the current TrackInfo
     */
    public TrackInfo getTrackInfo()
    {
        return _trackInfo;
    }


    /**
     * Receive loaded data and determine whether to filter on tracks or not
     *
     * @param inFieldArray    array of fields
     * @param inDataArray     array of data
     * @param inOptions       creation options such as units
     * @param inSourceInfo    information about the source of the data
     * @param inTrackNameList information about the track names
     */
    public void informDataLoaded(Field[] inFieldArray, Object[][] inDataArray, PointCreateOptions inOptions,
                                 SourceInfo inSourceInfo, TrackNameList inTrackNameList) {
        if (DEBUG) {
            Log.d(TAG, "informDataLoaded 3");
        }
        // Check whether loaded array can be properly parsed into a Track
        Track loadedTrack = new Track();
        loadedTrack.load(inFieldArray, inDataArray, inOptions);
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
            trackName = inTrackNameList == null ? "" : inTrackNameList.getTrackName(0);

        // go directly to load
        informDataLoaded(loadedTrack, inSourceInfo);
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
        if (_track.getNumPoints() > 0)
        {
            // Don't append, replace data
            _track.load(inLoadedTrack);
            if (inSourceInfo != null)
            {
                // set source information
                inSourceInfo.populatePointObjects(_track, _track.getNumPoints());
                _trackInfo.getFileInfo().replaceSource(inSourceInfo);
            }
        }
        else
        {
            // Currently no data held, so transfer received data
            _track.load(inLoadedTrack);
            if (inSourceInfo != null)
            {
                inSourceInfo.populatePointObjects(_track, _track.getNumPoints());
                _trackInfo.getFileInfo().addSource(inSourceInfo);
            }
        }
        recalculate();
        _main.handleState(this, TASK_COMPLETE);

        if (!_track.hasWaypoints() && !_track.hasNamedTrackpoints())
        {
            if (gpsSimulation == null)
            {
                control.showErrorMessage(_main.getString(R.string.gpx_info_simulation));
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

    public int getNearestTrackpointIndex(int inStart, double inLatitude, double inLongitude, double inMaxDist, double inMaxDistDest) {
        return _track.getNearestTrackpointIndex(inStart, inLatitude, inLongitude, inMaxDist, inMaxDistDest);
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

