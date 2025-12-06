package de.biselliw.tour_navigator.data;

import android.net.Uri;
import android.os.Bundle;

/**
 * important app data for saving/restoring the application state after relaunching the app on
 * Android device
 */

public abstract class AppState {
    private static boolean valid = false;
    private static boolean paused = false;

    /**
     * values to handle GPX files
     */
    private static Uri gpxSimulationUri = null;
    private static int gpxSimulationIndex = 0;

    private static boolean gpxFileCached = false;
    static int gpxInitialPlace = -1;
    static int startGpsIndex = 0;

    /**
     * values to handle GPX simulation file
     */

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
        gpxSimulationUri = null;
        clearNavigationState();
    }

    /**
     * Clear the navigation states on normal start
     */
    public static void clearNavigationState() {
        gpxFileCached = false;
        startGpsIndex = 0;
        gpxInitialPlace = 0;
        valid = false;
    }

    public boolean isValid () { return valid; }
    public static void getValues (Bundle instanceState) {
        /*
        gpxFileCached = instanceState.getBoolean("gpxFileCached");
        gpxInitialPlace = instanceState.getInt("gpxInitialPlace");
        valid = true;

         */
    }

    public static void putValues (Bundle instanceState) {
        if (valid)
        {
/*
            instanceState.putBoolean( "gpxFileCached", gpxFileCached );
            // remember current place
            instanceState.putInt("gpxInitialPlace", gpxInitialPlace);

 */
        }
    }

    public static boolean getPaused () { return paused; }
    public static void setPaused (boolean inPaused) { paused = inPaused; }
    public static Uri getGpxSimulationUri () { return gpxSimulationUri; }
    public static void setGpxSimulationUri (Uri inUri) { gpxSimulationUri = inUri; }

    public static int getGpxSimulationIndex () { return gpxSimulationIndex; }
    public static void setGpxSimulationIndex (int inIndex) { gpxSimulationIndex = inIndex; }

    public static boolean isGpxFileCached() { return gpxFileCached; }
    public static void setGpxFileCached(boolean cached) {
        gpxFileCached = cached;
        if (cached) valid = true;
    }

    public static int getGpxPlace() { return gpxInitialPlace; }
    public static void setGpxPlace(int value) {
        gpxInitialPlace = value;
        valid = true;
    }

    public static int getStartGpsIndex () { return startGpsIndex; }
    public static void setStartGpsIndex (int inIndex) { startGpsIndex = inIndex; }


    public static String getString() {
        if (valid)
            return "gpxFileCached = " + gpxFileCached +
                    "; initialPlace = " + gpxInitialPlace;
        else
            return "";
    }
}
