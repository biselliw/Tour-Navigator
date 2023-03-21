package de.biselliw.tour_navigator.helpers;

import android.location.Location;
import android.os.Build;
import android.util.Log;

import java.util.TimeZone;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.tim_prune.data.Track;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import static de.biselliw.tour_navigator.tim_prune.config.TimezoneHelper.getSelectedTimezone;

/**
 * GPS simulator with replay functionality
 * <p>
 * </p>GPS simulation is automatically activated after start up of the app and following these steps:
 * <ul>
 * <li>load a recorded GPX track file which does NOT include any waypoints or named track-points</li>
 * <li>load a GPX file including at least waypoints or named track-points</li>
 * </ul>
 *
 * From that on all GPS location data (time stamp, latitude, longitude) will be simulated by this
 * class
 *
 * @author BiselliW
 */
public class GpsSimulator {
    /**
     * TAG for log messages.
     */
    static final String TAG = "GpsSimulator";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    public static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    DataPoint[] gpsData;
    int numPoints;
    int gpsIndex;
    Location location;
    TimeZone timeZone;

    /**
     * GPS simulator with replay functionality
     */
    public static GpsSimulator gpsSimulation = null;

    /**
     * constructor for class GpsSimulator
     *
     * @param track track data for the initial recorded track
     */
    public GpsSimulator(Track track)
    {
        location = new Location("");
        timeZone = getSelectedTimezone();

        // Copy track points including GPS coordinates and time stamps to use as GPS locations
        numPoints = track.getNumPoints();
        gpsData = new DataPoint[numPoints];
        for (int i=0; i<numPoints; i++)
        {
            gpsData[i] = track.getPoint(i);
        }
    }

    /**
     * Reset the GPS simulation
     */
    public void Reset()
    {
        gpsIndex = 0;
    }

    /**
     * @return next GPS location data or null
     */
    public Location getLocation()
    {
        if (gpsIndex < numPoints)
        {
            if (DEBUG) {
                Log.d(TAG, "getLocation(" + gpsIndex + ")");
            }
            DataPoint dataPoint = gpsData[gpsIndex++];
            location.setLongitude(dataPoint.getLongitude().getDouble());
            location.setLatitude(dataPoint.getLatitude().getDouble());
            location.setTime(dataPoint.getTimestamp().getMilliseconds(timeZone));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.setMock(true);
            }
            return location;
        }
        else
            return null;
    }
}
