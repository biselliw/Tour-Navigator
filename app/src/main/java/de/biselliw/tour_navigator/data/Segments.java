package de.biselliw.tour_navigator.data;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.SettingsActivity;

public class Segments extends TrackSegments {
    /**
     * TAG for log messages.
     */
    static final String TAG = "Segments";
    private final boolean _DEBUG = true; // Set to true to enable logging
    private final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    private static double inHorSpeed, inVertSpeedClimb, inVertSpeedDescent, inMinHeightChange;

    public Segments() {
        SettingsActivity.getHikingParameters(this);
        trackHasTimeStamps = false;
    }

}


