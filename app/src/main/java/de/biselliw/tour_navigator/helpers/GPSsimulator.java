package de.biselliw.tour_navigator.helpers;

import android.location.Location;

import de.biselliw.tour_navigator.data.BaseTrack;
import tim.prune.data.DataPoint;

public class GPSsimulator {
    DataPoint[] gpsData;
    int numPoints;
    int gpsIndex;
    Location location;

    public static GPSsimulator gpsSim = null;

    public GPSsimulator(BaseTrack track)
    {
        // Copy track points to use as GPS locations
        numPoints = track.getNumPoints();
        gpsData = new DataPoint[numPoints];
        for (int i=0; i<numPoints; i++)
        {
            gpsData[i] = track.getPoint(i);
        }
        location = new Location("");
    }

    public void Reset()
    {
        gpsIndex = 0;
    }

    public Location getLocation()
    {

        if (gpsIndex < numPoints)
        {
            DataPoint dataPoint = gpsData[gpsIndex++];
            location.setLongitude(dataPoint.getLongitude().getDouble());
            location.setLatitude(dataPoint.getLatitude().getDouble());
        }

        return location;
    }
}
