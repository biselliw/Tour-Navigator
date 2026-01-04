package de.biselliw.tour_navigator.data;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.List;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;

import static de.biselliw.tour_navigator.tim_prune.config.TimezoneHelper.getSelectedTimezone;
import static tim.prune.data.Timestamp.Format.LOCALE;

public class EstimateParams extends BaseSegments {

    /**
     * TAG for log messages.
     */
    static final String TAG = "EstimateParams";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    private double _estHorSpeed, _estVertSpeedClimb, _estVertSpeedDescent;

    public EstimateParams () {
        trackHasTimeStamps = true;
    }

    public void estimate(List<Segment> inSegments) {

        double[] x, y, z;
        long totalBreakTime_s = 0L;
        int size = inSegments.size();
        if (size > 0) {
            /* 1. Analyze flat and ascending segments */
            int segments = 0, segments1, segments2;
            for (int i = 0; i < size; i++) {
                Segment segment = inSegments.get(i);
                totalBreakTime_s += segment.totalBreakTime_s;
                switch (segment.segmentType) {
                    case SEG_FLAT:
                    case SEG_UP_MODERATE:
                    case SEG_UP_STEEP:
                        segments++;
                        break;
                }
            }
            segments1 = segments;
            x = new double[segments];
            y = new double[segments];
            z = new double[segments];
            segments = 0;
            for (int i = 0; i < size; i++) {
                Segment segment = inSegments.get(i);
                switch (segment.segmentType) {
                    case SEG_FLAT:
                        x[segments] = segment.distance_km;
                        y[segments] = 0;
                        z[segments] = segment.totalSeconds - segment.totalBreakTime_s;
                        segments++;
                        break;
                    case SEG_UP_MODERATE:
                        x[segments] = segment.distance_km;
                        y[segments] = segment.climb_m / 2.0;
                        z[segments] = segment.totalSeconds - segment.totalBreakTime_s;
                        segments++;
                        break;
                    case SEG_UP_STEEP:
                        x[segments] = segment.distance_km / 2.0;
                        y[segments] = segment.climb_m;
                        z[segments] = segment.totalSeconds - segment.totalBreakTime_s;
                        segments++;
                        break;
                }
            }
            Result result = estimate(x, y, z);

            double _horSpeed1 = 3600.0 / result.a;
            /* climbing part in [km/h] */
            System.out.println("horSpeed = " + _horSpeed1);
            if (result.b > 0) {
                _estVertSpeedClimb = 3.6 / result.b;
                System.out.println("vertSpeedClimb = " + _estVertSpeedClimb);
            }
            else {
                _estVertSpeedClimb = 0;
                System.out.println("vertSpeedClimb = NA");
            }

            /* 2. Analyze descending segments */
            segments = 0;
            for (int i = 0; i < size; i++) {
                Segment segment = inSegments.get(i);
                switch (segment.segmentType) {
                    case SEG_FLAT:
                    case SEG_DOWN_MODERATE:
                    case SEG_DOWN_STEEP:
                        segments++;
                        break;
                }
            }
            segments2 = segments;
            x = new double[segments];
            y = new double[segments];
            z = new double[segments];
            segments = 0;
            for (int i = 0; i < size; i++) {
                Segment segment = inSegments.get(i);
                switch (segment.segmentType) {
                    case SEG_FLAT:
                        x[segments] = segment.distance_km;
                        y[segments] = 0;
                        z[segments] = segment.totalSeconds - segment.totalBreakTime_s;
                        segments++;
                        break;
                    case SEG_DOWN_MODERATE:
                        x[segments] = segment.distance_km;
                        y[segments] = segment.descent_m / 2.0;
                        z[segments] = segment.totalSeconds - segment.totalBreakTime_s;
                        segments++;
                        break;
                    case SEG_DOWN_STEEP:
                        x[segments] = segment.distance_km / 2.0;
                        y[segments] = segment.descent_m;
                        z[segments] = segment.totalSeconds - segment.totalBreakTime_s;
                        segments++;
                        break;
                }
            }
            result = estimate(x, y, z);

            double _horSpeed2 = 3600.0 / result.a;
            System.out.println("horSpeed = " + _horSpeed2);
            /* descending part in [km/h] */
            if (result.b > 0) {
                _estVertSpeedDescent = 3.6 / result.b;
                System.out.println("vertSpeedDescent = " + _estVertSpeedDescent);
            }
            else {
                _estVertSpeedDescent = 0;
                System.out.println("vertSpeedDescent = NA");
            }

            System.out.println("totalBreakTime_min = " + totalBreakTime_s/60);
            _estHorSpeed = (_horSpeed1 * segments1 + _horSpeed2 * segments2) / (segments1 + segments2);
            System.out.println("ave horSpeed = " + _estHorSpeed);
        }
    }

    public static class Result {
        public final double a;
        public final double b;

        public Result(double a, double b) {
            this.a = a;
            this.b = b;
        }
    }

    public Result estimate(double[] x, double[] y, double[] z) {
        if (x.length != y.length || y.length != z.length) {
            throw new IllegalArgumentException("Arrays must have same length");
        }

        int n = x.length;
        double a, b;

        double sumZdivX  = 0.0;
        double sumXX = 0.0;
        double sumYY = 0.0;
        double sumXY = 0.0;
        double sumXZ = 0.0;
        double sumYZ = 0.0;

        for (int i = 0; i < n; i++) {
            sumZdivX  += z[i] / x[i];
            sumXX += x[i] * x[i];
            sumYY += y[i] * y[i];
            sumXY += x[i] * y[i];
            sumXZ += x[i] * z[i];
            sumYZ += y[i] * z[i];
        }

        double det = sumXX * sumYY - sumXY * sumXY;

        if (Math.abs(det) < 1e-12) {
            // Determinant is zero; variables may be collinear
            a = sumZdivX / n;
            b = 0.0;
        }
        else {
            a = ( sumYY * sumXZ - sumXY * sumYZ ) / det;
            b = ( sumXX * sumYZ - sumXY * sumXZ ) / det;
        }

        return new Result(a, b);
    }

    public TourDetails.AdditionalInfo getRecordedTrackFileInfo() {
        TourDetails.AdditionalInfo info = new TourDetails.AdditionalInfo();

        info.title = "Informationen zum aufgezeichneten Track";
        String warnTrackHasAltitudeJumps = trackHasAltitudeJumps ? "<b>Der Track weist Sprünge im Höhenprofil auf!</b><br>" : "";
        info.description =
            "<p><b>Information</b></p>" +
                "<p>Startzeit: " + _track.getPoint(0).getTimestamp().getText(LOCALE,getSelectedTimezone()) + "<br>" +
                "Ende: " + _track.getPoint(_track.getNumPoints()-1).getTimestamp().getText(LOCALE,getSelectedTimezone()) + "</p>" +
                "<p>Trackzeit: " + formatIntToTime((int)(_totalSeconds / 60L)) + "<br>" +
                "<p>Strecke: " + formatDouble(_totalDistance_km) + " km</p>" +
            "<p><b>Statistiken</b></p>" +
                "Dauer in Bewegung: "+ formatIntToTime((int)((_totalSeconds - _totalBreakTime_s) / 60L)) + "<br>" +
                "Pausen: "+ formatIntToTime((int)(_totalBreakTime_s / 60L)) + "<br>" +
            "<p><b>Höhenmeter</b></p>" +
                "<p>Aufstieg: " + (int)(_totalClimb_m) + " hm<br>" +
                "Abstieg: " + (int)(-_totalDescent_m) + " hm<br>" +
                warnTrackHasAltitudeJumps +
                "Distanz eben: " + formatDouble(_totalDistance_km - _totalDistanceClimb_km - _totalDistanceDescent_km) + " km<br>" +
                "Distanz bergauf: " + formatDouble(_totalDistanceClimb_km) + " km<br>" +
                "Distanz bergab: " + formatDouble(_totalDistanceDescent_km) + " km<br>" +
                "Höchster Punkt: " + (int)(_maxAltitude_m) + " m<br>" +
                "Tiefster Punkt: " + (int)(_minAltitude_m) + " m</p>" +
            "<p><b>" + App.resources.getString(R.string.pref_header_hiking_parameters) + "</b></p>" +
                "<p>" + App.resources.getString(R.string.pref_hiking_par_horSpeed) + ": " + (int)(_estHorSpeed * 1000.0) + "<br>" +
                App.resources.getString(R.string.pref_hiking_par_vertSpeedClimb) + ": " + (int)(_estVertSpeedClimb * 1000.0) + "<br>" +
                App.resources.getString(R.string.pref_hiking_par_vertSpeedDescent) + ": " + (int)(_estVertSpeedDescent * 1000.0) + "</p>";

        return info;
    }

    public String formatDouble (double inValue) {
        DecimalFormat formatter = new DecimalFormat(" #0.0");
        return formatter.format(inValue);
    }

    public String formatIntToTime(int inMinutes) {
        // separate minutes and hours
        int minute = inMinutes % 60;
        int hour = (inMinutes / 60) % 60;

        // create output format
        Object[] arguments = {
                hour,
                minute
        };

        // First format the cell value as required
        return MessageFormat.format(
                "  {0,number,00}:{1,number,00}",
                arguments);
    }

}

