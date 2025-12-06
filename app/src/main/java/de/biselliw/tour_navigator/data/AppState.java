package de.biselliw.tour_navigator.data;

import android.net.Uri;
import android.os.Bundle;

import de.biselliw.tour_navigator.activities.LocationActivity;

/**
 * important app data for saving/restoring the application state after relaunching the app on
 * Android device
 */

public abstract class AppState {
    private static boolean _valid = false;
    private static boolean _paused = false;

    /**
     * values to app state
     */
    private static LocationActivity.gpsStatus _GpsStatus = LocationActivity.gpsStatus.NOT_REGISTERED;
    private static LocationActivity.locationStatus _LocationStatus = LocationActivity.locationStatus.INITIAL;
    private static int _StartGpsIndex = 0;
    private static boolean _isTracking = false;


    /**
     * values to handle GPX files
     */
    private static Uri _GpxSimulationUri = null;
    private static int _GpxSimulationIndex = 0;
    private static boolean _GpxFileCached = false;
    // static int gpxInitialPlace = -1;


    public static boolean destroyed = false;
//    public static boolean started = false;
    public static boolean restarted = false;
    public static boolean stopped = false;
    public static boolean lowMemory = false;
    public static long trimMemoryLevel = 0;


    public AppState () {
    }

    /**
     * Clear the app states on normal start
     */
    public static void clearState() {
        _GpxSimulationUri = null;
        clearNavigationState();
    }

    /**
     * Clear the navigation states on normal start
     */
    public static void clearNavigationState() {
        _GpxFileCached = false;
        _StartGpsIndex = 0;
//        gpxInitialPlace = 0;
        _GpsStatus = LocationActivity.gpsStatus.NOT_REGISTERED;
        _LocationStatus = LocationActivity.locationStatus.INITIAL;
        _valid = false;
    }

    public static boolean isValid () { return _valid; }
    public static void getValues (Bundle instanceState) {
    }

    public static void putValues (Bundle instanceState) {
        if (_valid)
        {
        }
    }

    public static boolean getPaused() { return _paused; }
    public static void setPaused(boolean inPaused) { _paused = inPaused; }

    public static LocationActivity.gpsStatus getGpsStatus() { return _GpsStatus; }
    public static void setGpsStatus(LocationActivity.gpsStatus inGpsStatus) { _GpsStatus = inGpsStatus; }

    public static LocationActivity.locationStatus getLocationStatus () { return _LocationStatus; }
    public static void setLocationStatus (LocationActivity.locationStatus inLocationStatus) { _LocationStatus = inLocationStatus; }

    public static boolean isTracking() { return _isTracking; }
    public static void setTracking(boolean isTracking) { _isTracking = isTracking; }


    public static Uri getGpxSimulationUri() { return _GpxSimulationUri; }
    public static void setGpxSimulationUri(Uri inUri) { _GpxSimulationUri = inUri; }

    public static int getGpxSimulationIndex() { return _GpxSimulationIndex; }
    public static void setGpxSimulationIndex(int inIndex) { _GpxSimulationIndex = inIndex; }

    public static boolean isGpxFileCached() { return _GpxFileCached; }
    public static void setGpxFileCached(boolean cached) {
        _GpxFileCached = cached;
        if (cached) _valid = true;
    }

    public static int getStartGpsIndex() { return _StartGpsIndex; }
    public static void setStartGpsIndex(int inIndex) {
        _StartGpsIndex = inIndex;
        _valid = true;
    }

    public static String getString() {
        if (_valid)
            return "gpxFileCached = " + _GpxFileCached +
                    "; _StartGpsIndex = " + _StartGpsIndex;
        else
            return "";
    }
}
