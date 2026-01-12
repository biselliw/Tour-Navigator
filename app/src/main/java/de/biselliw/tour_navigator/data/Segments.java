package de.biselliw.tour_navigator.data;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.SettingsActivity;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import tim.prune.data.Altitude;
import tim.prune.data.Distance;

public class Segments extends BaseSegments {
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


