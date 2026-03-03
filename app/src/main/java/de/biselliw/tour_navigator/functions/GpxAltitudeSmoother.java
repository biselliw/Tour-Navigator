package de.biselliw.tour_navigator.functions;

import android.annotation.SuppressLint;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.helpers.Log;
import tim.prune.data.UnitSetLibrary;

/**
 * class for smoothing GPX altitudes from GPX files:
 * Gaussian smoothing (also called a <a href = "https://en.wikipedia.org/wiki/Gaussian_blur">Gaussian blur</a>)
 * is a low-pass filtering technique used to reduce noise and fine-detail structure in signals or images.
 * It works by convolving the data with a Gaussian function so that nearby values influence each other,
 * with closer neighbors receiving more weight than distant ones.
 */
public class GpxAltitudeSmoother {
    /**
     * TAG for log messages.
     */
    static final String TAG = "GpxAltitudeSmoother";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /**
     * Max gradient for ascent accounting = 150 m per km (15%),
     */
    private static final double maxGrade = 15.0;

    /**
     * σDistanceMeters = 30 m
     */
    private static final double sigmaMeters = 30;

    /**
     * smooth all altitudes of a track
     * @param inTrack track with distances and altitudes but no way points
     */
    @SuppressLint("DefaultLocale")
    public static void smoothTrack(TrackDetails inTrack) {
        int numPoints = inTrack.getNumPoints();

        double[] lats = new double[numPoints];
        double[] lons = new double[numPoints];
        double[] elevations = new double[numPoints];

        for (int i = 0; i < numPoints; i++) {
            lats[i] = inTrack.getPoint(i).getLatitude().getDouble();
            lons[i] = inTrack.getPoint(i).getLongitude().getDouble();
            elevations[i] = inTrack.getPoint(i).getAltitude().getValue();
        }

        double[] distances = cumulativeDistances(lats, lons);

        double origUp = computeAscent(elevations, distances, maxGrade);
        double origDown = computeDescent(elevations, distances, maxGrade);

        double[] smoothed = gaussianSmoothByDistance(elevations, distances, sigmaMeters);
        double[] finalAlt = preserveAscentDescent(smoothed, elevations);

        double smoothUp = computeAscent(finalAlt, distances, maxGrade);
        double smoothDown = computeDescent(finalAlt, distances, maxGrade);

        if (DEBUG) {
            Log.d(TAG, String.format("Original ascent: %.1f m | descent: %.1f m%n", origUp, origDown));
            Log.d(TAG, String.format("Smoothed ascent: %.1f m | descent: %.1f m%n", smoothUp, smoothDown));
        }

        for (int i = 0; i < numPoints; i++) {
            inTrack.getPoint(i).setAltitude(String.valueOf(smoothed[i]), UnitSetLibrary.UNITS_METRES,false);
        }
    }

    private static double[] cumulativeDistances(double[] lats, double[] lons) {
        int n = lats.length;
        double[] d = new double[n];
        d[0] = 0;
        for (int i = 1; i < n; i++) {
            d[i] = d[i - 1] + haversine(lats[i - 1], lons[i - 1], lats[i], lons[i]);
        }
        return d;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double[] gaussianSmoothByDistance(double[] values,
                                                     double[] dist,
                                                     double sigmaMeters) {

        int n = values.length;
        double[] out = new double[n];
        double twoSigma2 = 2 * sigmaMeters * sigmaMeters;

        for (int i = 0; i < n; i++) {
            double sum = 0;
            double wsum = 0;

            for (int j = 0; j < n; j++) {
                double d = dist[j] - dist[i];
                double w = Math.exp(-(d * d) / twoSigma2);
                sum += w * values[j];
                wsum += w;
            }
            out[i] = sum / wsum;
        }
        return out;
    }

    private static double[] preserveAscentDescent(double[] smoothed, double[] original) {

        int n = smoothed.length;

        double origUp = 0, origDown = 0;
        for (int i = 1; i < n; i++) {
            double d = original[i] - original[i - 1];
            if (d > 0) origUp += d;
            else origDown -= d;
        }

        double smoothUp = 0, smoothDown = 0;
        for (int i = 1; i < n; i++) {
            double d = smoothed[i] - smoothed[i - 1];
            if (d > 0) smoothUp += d;
            else smoothDown -= d;
        }

        double upScale = (smoothUp > 0) ? (origUp / smoothUp) : 1.0;
        double downScale = (smoothDown > 0) ? (origDown / smoothDown) : 1.0;

        double[] adjusted = new double[n];
        adjusted[0] = smoothed[0];

        for (int i = 1; i < n; i++) {
            double d = smoothed[i] - smoothed[i - 1];
            if (d > 0) d *= upScale;
            else d *= downScale;
            adjusted[i] = adjusted[i - 1] + d;
        }
        return adjusted;
    }

    private static double computeAscent(double[] vals, double[] dist, double maxGrade) {
        double up = 0;
        for (int i = 1; i < vals.length; i++) {
            double d =  vals[i] - vals[i - 1];
            double dx = dist[i] - dist[i - 1];

            if (dx > 0 && Math.abs(d / dx) <= maxGrade && d > 0) up += d;
        }
        return up;
    }

    private static double computeDescent(double[] vals, double[] dist, double maxGrade) {
        double down = 0;
        for (int i = 1; i < vals.length; i++) {
            double d = vals[i] - vals[i - 1];
            double dx = dist[i] - dist[i - 1];

            if (dx > 0 && Math.abs(d / dx) <= maxGrade && d < 0) down -= d;
        }
        return down;
    }

    private static List<Double> toList(double[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (double v : arr) list.add(v);
        return list;
    }
}
