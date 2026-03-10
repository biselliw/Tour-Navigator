package de.biselliw.tour_navigator.functions;
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
import android.location.Location;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import static de.biselliw.tour_navigator.App.app;
import static de.biselliw.tour_navigator.data.AppState.gpsSimulation;


public class LocationHandler {

    /**
     * TAG for log messages.
     */
    static final String TAG = "LocationHandler";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    public static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public static final int LOC_IDLE                = 0;
    public static final int LOC_GOTO_START          = 1;
    public static final int LOC_TRACKING            = 2;
    public static final int LOC_APPROACHING         = 3;
    public static final int LOC_OUT_OF_TRACK        = 4;
    public static final int LOC_BREAK               = 5;
    public static final int LOC_DESTINATION_REACHED = 6;


    public static final double INVALID_DISTANCE = 9999.9999;
    final public static double maxOffsetStart_km = 0.100;
    final public static double maxOffsetTrack_km = 0.030;
    final public static double maxOffsetPOI_km = 0.015;

    private static class LocationResults {
        /** distance of the track point since start */
        static double distance;

        static int selectedPlace = -1, nextPlace = -1;

        /** delay between planned and real arrival times */
        static int delay_min = 0;
        static int remainingBreakTime_min = 0;
        static long endBreakTime = 0;
    }

    /** location handler status */
    private static int _locStatus = LOC_IDLE;
    private static int _startTime_min = 0;

    /** index of the track point from which to search for the nearest GPS location */
    static int startGpsIndex = -1;

    /** nearest distance of the track point to the received GPS location */
    static double nearestDistance;


    private static final List<DataPoint> _records = new ArrayList<>();
    private static int _endPlace = 0;
    private static int _endIndex = 1;
    static int locationStatusBeforeBreak = LOC_IDLE;
    private static double _distanceAtBreak = 0.0;

    /** distance of the track point along the route since leaving the previous place */
    static double distanceFromPlace;

    /** distance between track point and next place along the route [km] */
    static double distanceToNextPlace;

    public static int handleLocation(Location inLocation) {
        if (inLocation == null) return LOC_IDLE;
        if (_locStatus > LOC_IDLE)
            return handleGpsData(inLocation.getTime(), inLocation.getLatitude(), inLocation.getLongitude(), inLocation.getAccuracy());
        return LOC_IDLE;
    }

    /**
     * Change the status of the navigation
     * @param inStatus new status of the navigation
     */
    public static void setLocationStatus(int inStatus) {
        if (_locStatus != inStatus)
        {
            if (DEBUG) Log.d(TAG,"setLocationStatus: " +
                    getLocationStatus(_locStatus) + " -> " +
                    getLocationStatus(inStatus));

            if (inStatus != LOC_BREAK)
                locationStatusBeforeBreak = inStatus;
            _locStatus = inStatus;
        }
    }

    /**
     * Set the index of first track point used for navigation
     * @param inIndex index of the track point
     */
    public static void setStartGpsIndex(int inIndex)
    {
        if (DEBUG) Log.d(TAG,"setStartGPSindex("+inIndex+")");
        startGpsIndex = inIndex;
        if (inIndex == 0) {
            _endIndex = 1;
            _endPlace = 0;
            /*
            _arrivedPlace = -1;
            _destinationReached = false;
             */
            nearestDistance = -1.0;
            distanceFromPlace = -1.0;
            LocationResults.delay_min = 0;
            LocationResults.remainingBreakTime_min = 0;
        }
    }

    /**
     * @return index of first track point used for navigation
     */
    public static int getStartGpsIndex() {
        return startGpsIndex;
    }

    /**
     * Notify the handler about an update of the time table
     * @param records list of route points representing the table
     */
    public static void notifyDataSetChanged(List<DataPoint> records) {
        if (DEBUG) Log.i(TAG,"notifyDataSetChanged()");
        if (records != null) {
            _records.clear();
            _records.addAll(records);
            records.clear();
        }
    }

    /**
     * Select an item in the list of places
     *
     * @param inPlace Index (starting at 0) of the data item to be selected or -1 if nothing
     * @return true if a new place has been selected
     */
    public static boolean setPlace(int inPlace) {
        if (inPlace >= 0 && inPlace < _records.size() &&
                (inPlace != LocationResults.selectedPlace || inPlace == 0)) {
            LocationResults.selectedPlace = inPlace;
            LocationResults.nextPlace = inPlace;

            _endPlace = inPlace;

            DataPoint routePoint = _records.get(inPlace);
            if (routePoint != null) {
                if (DEBUG) Log.i(TAG, "setPlace(" + inPlace + "): " + routePoint.getRoutePointName());
                setStartGpsIndex(routePoint.getIndex());
                return true;
            }
        }
        return false;
    }


    /**
     * Refresh the timetable with actual position information from GPS location provider
     *
     * @param inGPStime   time stamp [ms] from system or GPS simulation file
     * @param inLatitude  GPS latitude
     * @param inLongitude GPS longitude
     * @param inAccuracy  horizontal accuracy [m] / 0
     * @return location status
     */
    public static int handleGpsData(long inGPStime, double inLatitude, double inLongitude, float inAccuracy) {
        /* check start of a record */
        int startPlace = LocationResults.selectedPlace;

        /* limit search to end of a record */
        int nextPlace = startPlace;
        DataPoint nextRoutePoint = null;
        for (nextPlace = startPlace + 1; nextPlace < _records.size(); nextPlace++) {
            nextRoutePoint = _records.get(nextPlace);
            if (nextRoutePoint != null) {
                int endIndex = nextRoutePoint.getIndex();
                if (endIndex > _endIndex) _endIndex = endIndex;
                break;
            }
        }

        /* Find nearest track point to the received GPS location */
        int nearestGPSindex = app.getNearestTrackpointIndex(startGpsIndex, _endIndex, inLatitude, inLongitude, getMaxOffset_km());
        /* Return the nearest distance of this track point to the received GPS location */
        nearestDistance = app.getNearestDistance();

        // nearby GPS location found ?
        if (nearestGPSindex >= 0)
            handlePosition(nearestGPSindex, inGPStime);
        else {
           _endIndex++;
            setLocationStatus(LOC_OUT_OF_TRACK);
        }

        DataPoint startRoutePoint = _records.get(startPlace);
        if (startRoutePoint == null) return LOC_IDLE;

        DataPoint trackPoint = app.getPoint(startGpsIndex);
        if (trackPoint == null) return LOC_IDLE;
        LocationResults.distance = trackPoint.getDistance();
        distanceFromPlace = trackPoint.getDistance() - startRoutePoint.getDistance();

        // calc distance between track point and next place along the route
        String distToPlace = "";
        if (nextRoutePoint != null && nearestDistance < maxOffsetStart_km) {
            distanceToNextPlace = nextRoutePoint.getDistance() - trackPoint.getDistance();
            /* arrived at the next place? */
            distToPlace = ", to Place: " + (int) (distanceToNextPlace * 1000.0) + " m";
            if (distanceToNextPlace <= 0.0) {
                if (setPlace(startPlace + 1))
                    Log.i(TAG, "arrived @" + nextRoutePoint.getRoutePointName());
            }
        }
        else
            distanceToNextPlace = INVALID_DISTANCE;

        if (DEBUG) {
            String simGPSindex = (gpsSimulation != null) ? ", simGPSindex = " + gpsSimulation.getGpsIndex() : "";
            Log.d(TAG, "handleGPSdata(): _locStatus = " + getLocationStatus(_locStatus) +
                    simGPSindex +
                    ", startGPSindex = " + startGpsIndex + ", endIndex = " + _endIndex +
                    ": nearestGPSindex = " + nearestGPSindex +
                    ", nearestDist = " + (int) (nearestDistance * 1000.0) + " m" +
                    distToPlace);
        }

        /* don't leave the place in case of break */
        if (_locStatus == LOC_BREAK) {
            checkEndOfBreak(inGPStime);
            if (LocationResults.remainingBreakTime_min > 0) {
                if (DEBUG)
                    Log.i(TAG, "BREAK: remainBreakTime_min = " + LocationResults.remainingBreakTime_min);
            }
        }
        else {
            int breakTime_min = startRoutePoint.getWaypointDuration();
            if (breakTime_min > 0) {
                // handle break if place is reached
                if (distanceFromPlace < maxOffsetStart_km) {
                    _distanceAtBreak = trackPoint.getDistance();
                    // are we within our timetable?
                    LocationResults.remainingBreakTime_min = breakTime_min - LocationResults.delay_min;
                    if (DEBUG)
                        Log.i(TAG, "BREAK: remainBreakTime_min = " + LocationResults.remainingBreakTime_min);
                    if (LocationResults.remainingBreakTime_min > 0) {
                        // calc time stamp of end of break
                        LocationResults.endBreakTime = inGPStime + (long) LocationResults.remainingBreakTime_min * 60000L;
                        setLocationStatus(LOC_BREAK);
                    }
                    else {
                        LocationResults.endBreakTime = 0;
                    }
                }
            }
        }

        /* leave the place? */
        if (distanceFromPlace > maxOffsetTrack_km) {
            setEndOfBreak();
            if (LocationResults.nextPlace == LocationResults.selectedPlace) {
                LocationResults.nextPlace = LocationResults.selectedPlace + 1;
                if (DEBUG)
                    if (nextRoutePoint != null)
                        Log.i(TAG, "next stop: " + nextRoutePoint.getRoutePointName());
            }
        }

        /* arrived at the next place? */
        // calc direct distance between geo location and next place
        if (nextRoutePoint != null) {
            double distanceSatToPlace_km = nextRoutePoint.distanceTo(inLatitude, inLongitude);
            boolean distanceSatToPlace = Math.abs(distanceSatToPlace_km) <= maxOffsetPOI_km;
            // calc distance between track point and next place along the route
            boolean distanceTpToPlace = distanceToNextPlace <= 0.0;
            if (distanceSatToPlace || distanceTpToPlace) {
                if (setPlace(startPlace + 1)) {
                    if (DEBUG) Log.i(TAG, "arrived @" + nextRoutePoint.getRoutePointName());
                    if (startPlace >= _records.size()) {
                        if (DEBUG) Log.i(TAG, "destination reached");
                        setLocationStatus(LOC_DESTINATION_REACHED);
                    }
                }
            }
        }
        else
            setLocationStatus(LOC_DESTINATION_REACHED);

        return _locStatus;
    }

    /**
     * Handle a nearby found GPS location
     *
     * @param inPosition index of the nearest track point
     * @param inTime     local time from the GPS receiver [ms]
     */
    private static void handlePosition(int inPosition, long inTime) {
        // use the position to start navigation
        setStartGpsIndex(inPosition);

        switch (_locStatus) {
            case LOC_GOTO_START:
            case LOC_APPROACHING:
            case LOC_OUT_OF_TRACK: {
                if (nearestDistance < maxOffsetTrack_km)
                    setLocationStatus(LOC_TRACKING);
                else
                    setLocationStatus(LOC_APPROACHING);
                break;
            }
        }

        /* Calculate the time shift between GPS and expected arrival time of the current point */
        DataPoint routePoint = app.getPoint(inPosition);
        if (routePoint == null) return;
        long destTime_s = routePoint.getTime();
        if ((_startTime_min > 0) && ((inPosition == 0) || (destTime_s > 0))) {
            // calculate delay
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(inTime);

            // ignore the day in simulation
            long gpsTime_s = calendar.get(Calendar.SECOND) + 60 * (calendar.get(Calendar.MINUTE) + 60L * calendar.get(Calendar.HOUR_OF_DAY));
            LocationResults.delay_min = (int) (gpsTime_s - (_startTime_min * 60L + destTime_s)) / 60;
        }
    }

    /**
     * @return true if the break time is over
     */
    private static boolean checkEndOfBreak(long inGPStime) {
        long remainingBreakTime_ms = 0;
        if (LocationResults.endBreakTime > 0) {
            remainingBreakTime_ms = LocationResults.endBreakTime - inGPStime;
            LocationResults.remainingBreakTime_min = (int)(remainingBreakTime_ms / 60000L);
        }

        if (remainingBreakTime_ms < 0L)
            setEndOfBreak();

        return remainingBreakTime_ms < 0L;
    }

    /**
     * Force end of break
     */
    private static void setEndOfBreak() {
        LocationResults.remainingBreakTime_min = 0;
        LocationResults.endBreakTime = 0;
        setLocationStatus(locationStatusBeforeBreak);
    }

    /**
     * Set the start time of the tour
     *
     * @param inStartTime time in [min] since midnight
     */
    public static void setStartTime(int inStartTime) {
        _startTime_min = inStartTime;
    }

    /**
     * @return max. allowed offset between a track point and the GPS location
     */
    private static double getMaxOffset_km() {
        double maxOffset_km;
        switch (_locStatus) {
            case LOC_GOTO_START:
                maxOffset_km = maxOffsetStart_km;
                break;
            case LOC_TRACKING:
            case LOC_APPROACHING:
            case LOC_BREAK:
                maxOffset_km = maxOffsetTrack_km;
                break;
            default:
                maxOffset_km = maxOffsetStart_km;
                break;
        }
        return maxOffset_km;
    }

    /**
     * @return status of the location handler
     * */
    public static int getStatus () {
        return _locStatus;
    }

    /**
     * @return index of the current track point
     */
    public static int getIndex() {
        return startGpsIndex;
    }

    /**
     * @return distance of the track point since start
     */
    public static double getDistance() {
        return LocationResults.distance;
    }

    /**
     * @return distance between track point and next place along the route
     */
    public static double getDistanceToNextPlace() {
        return distanceToNextPlace;
    }

    /**
     * @return nearest distance of the track point to the received GPS location
     */
    public static double getNearestDistance() {
        return nearestDistance;
    }

    /**
     * @return remaining break time [min]
     */
    public static int getRemainingBreakTime () {
        return LocationResults.remainingBreakTime_min;
    }

    /**
     * @return delay between planned and real arrival times [min]
     */
    public static int getDelay () {
        return LocationResults.delay_min;
    }

    /**
     * @return current place
     */
    public static int getPlace () {
        return LocationResults.selectedPlace;
    }

    /**
     * @return next place to arrive
     */
    public static int getNextPlace () {
        return LocationResults.nextPlace;
    }
    /**
     * Get the status of the navigation
     * @return status string
     */
    public static String getLocationStatus(int inStatus) {
        if (DEBUG) {
            String[] locationStatusStr = {
                    "IDLE",
                    "GOTO_START_POS",
                    "TRACKING",
                    "APPROACHING",
                    "OUT_OF_TRACK",
                    "BREAK",
                    "DESTINATION_REACHED"
            };

            if (inStatus >= LOC_IDLE && inStatus <= LOC_DESTINATION_REACHED) {
                return locationStatusStr[inStatus];
            }
        }
        return "INVALID";
    }

}

