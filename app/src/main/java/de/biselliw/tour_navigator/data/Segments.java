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

    /**
     * set hiking parameters:
     *
     * @param inHorSpeed         horizontal part in [km/h]
     * @param inVertSpeedClimb   ascending part in [km/h]
     * @param inVertSpeedDescent descending part in [km/h]
     * @param inMinHeightChange  min. required change of altitude
     */
    public void setHikingParameters(double inHorSpeed, double inVertSpeedClimb, double inVertSpeedDescent, double inMinHeightChange) {
        if (inHorSpeed > 0)
            _horSpeed = inHorSpeed;
        if (inVertSpeedClimb > 0)
            _vertSpeedClimb = inVertSpeedClimb;
        if (inVertSpeedDescent > 0)
            _vertSpeedDescent = inVertSpeedDescent;
        if (inMinHeightChange > 0)
            _minHeightChange = inMinHeightChange;
    }
}


