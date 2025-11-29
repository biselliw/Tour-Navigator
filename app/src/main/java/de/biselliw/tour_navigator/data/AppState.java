package de.biselliw.tour_navigator.data;

import android.net.Uri;
import android.os.Bundle;

/**
 * important app data for saving/restoring the application state after relaunching the app on
 * Android device
 */

public abstract class AppState {
    private static boolean valid = false;
    /**
     * values to handle GPX files
     */
    private static boolean gpxFileCached = false;
    static int gpxInitialPlace = -1;

    /**
     * values to handle GPX simulation file
     */
    public static Uri gpxSimulationUri = null;
    private static boolean gpxSimulationFileCached = false;
    private static String gpxSimulationFile = "";
    private static String loadedFilePath = "";

    public static boolean destroyed = false;
    public static boolean started = false;
    public static boolean restarted = false;
    public static boolean stopped = false;
    public static boolean paused = false;
    public static boolean resumed = false;
    public static boolean lowMemory = false;
    public static long trimMemoryLevel = 0;




    public AppState () {
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

    public static boolean isGpxFileCached() { return gpxFileCached; }
    public static void setGpxFileCached(boolean cached) {
        gpxFileCached = cached;
        if (cached) valid = true;
    }

    public static boolean isGpxSimulationFileCached() { return gpxSimulationFileCached; }
    public static void setGpxSimulationFileCached(boolean cached) {
        gpxFileCached = cached;
        if (cached) gpxSimulationFileCached = true;
    }

    public static int getGpxInitialPlace() { return gpxInitialPlace; }
    public static void setGpxInitialPlace(int value) {
        gpxInitialPlace = value;
        valid = true;
    }

    public static String getString() {
        if (valid)
            return "gpxFileCached = " + gpxFileCached +
                    "; initialPlace = " + gpxInitialPlace;
        else
            return "";
    }
}
