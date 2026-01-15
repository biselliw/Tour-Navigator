package de.biselliw.tour_navigator.data;

import android.util.Log;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.functions.LinearFit3D;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import tim.prune.data.Distance;

import static de.biselliw.tour_navigator.tim_prune.config.TimezoneHelper.getSelectedTimezone;
import static tim.prune.data.Timestamp.Format.LOCALE;

public class EstimateParams extends BaseSegments {

    /**
     * TAG for log messages.
     */
    static final String TAG = "EstimateParams";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    private EstimationResult _estimationResult = new EstimationResult();

    private static String _report = "";

    public EstimateParams() {
        trackHasTimeStamps = true;
    }

    public static class EstimationResult {
        public final boolean successful;
        public final int segments;
        public final double estHorSpeed, estSpeedClimb, estSpeedDescent;
        public final double rmse;
        public final double r2;

        public EstimationResult() {
            this.successful = false;
            this.segments = 0;
            this.estHorSpeed = 0.0;
            this.estSpeedClimb = 0.0;
            this.estSpeedDescent = 0.0;
            this.rmse = 0;
            this.r2 = 0;
        }

        public EstimationResult(boolean successful, int segments, double estHorSpeed, double estSpeedClimb, double estSpeedDescent,
                                double rmse, double r2) {
            this.successful = successful;
            this.segments = segments;
            this.estHorSpeed = estHorSpeed;
            this.estSpeedClimb = estSpeedClimb;
            this.estSpeedDescent = estSpeedDescent;
            this.rmse = rmse;
            this.r2 = r2;
        }

        public EstimationResult(EstimationResult fromOther) {
            this.successful = fromOther.successful;
            this.segments = fromOther.segments;
            this.estHorSpeed = fromOther.estHorSpeed;
            this.estSpeedClimb = fromOther.estSpeedClimb;
            this.estSpeedDescent = fromOther.estSpeedDescent;
            this.rmse = fromOther.rmse;
            this.r2 = fromOther.r2;
        }

        @NonNull
        @Override
        public String toString() {
            return "rmse = " + baseSegments.formatDouble(rmse) + "; r2 = " + baseSegments.formatDouble(r2);
        }
    }

    private static class EstimationVariation {
        EstimationResult result;
        int gradientThresholdClimb, gradientThresholdDesc;

        @NonNull
        public String toString() {
            return "rmse = " + baseSegments.formatDouble(result.rmse) + "; r2 = " + baseSegments.formatDouble(result.r2);
        }
    }

    EstimationResult estimateAll(List<Segment> inSegments) {
        final boolean DEBUG_this = false;
        int size = inSegments.size();
        boolean result = size > 0;
        EstimationResult estimationResult = null;
        int segments = 0;
        double estHorSpeed = 0.0, estVertSpeedClimb = 0.0, estVertSpeedDescent = 0.0;
        if (result) {
            double[] x = new double[size];
            double[] y = new double[size];
            double[] z = new double[size];
            double[] t = new double[size];
            boolean distanceTooSmall = false, error = false;

            for (int i = 0; i < size; i++) {
                Segment segment = inSegments.get(i);
                boolean useSegment;
//                if (segment.distance_km > 0.06)
                {
                    switch (segment.segmentType) {
                        case SEG_FLAT:
                            x[segments] = segment.distance_km;
                            y[segments] = 0;
                            z[segments] = 0;
                            useSegment = true;
                            break;
                        case SEG_UP_MODERATE:
                            x[segments] = segment.distance_km;
                            y[segments] = segment.climb_m / 2.0;
                            z[segments] = 0;
                            useSegment = true;
                            break;
                        case SEG_UP_STEEP:
                            x[segments] = segment.distance_km / 2.0;
                            y[segments] = segment.climb_m;
                            z[segments] = 0;
                            useSegment = true;
                            break;
                        case SEG_DOWN_MODERATE:
                            x[segments] = segment.distance_km;
                            y[segments] = 0;
                            z[segments] = segment.descent_m / 2.0;
                            useSegment = true;
                            break;
                        case SEG_DOWN_STEEP:
                            x[segments] = segment.distance_km / 2.0;
                            y[segments] = 0;
                            z[segments] = segment.descent_m;
                            useSegment = true;
                            break;
                        default:
                            useSegment = false;
                    }
                    if (useSegment) {
                        t[segments] = segment.totalSeconds - segment.totalBreakTime_s;
                        if (t[segments] <= 0)
                            error = true;
                        else
                            segments++;
                    }
                } // else                    distanceTooSmall = true;
            }
            if (segments > 1) {
                LinearFit3D.Result res = LinearFit3D.estimate(x, y, z, t);

                /* flat part in [km/h] */
                if (res.a > 0) {
                    estHorSpeed = 3600.0 / res.a;
                    if (DEBUG_this) Log.d(TAG, "horSpeed = " + formatDouble(estHorSpeed) + " km/h");
                } else {
                    if (DEBUG_this) Log.e(TAG, "horSpeed NA");
                    result = false;
                }
                /* climbing part in [km/h] */
                if (res.b > 0) {
                    estVertSpeedClimb = 3.6 / res.b;
                    if (DEBUG_this) Log.d(TAG, "vertSpeedClimb = " + formatDouble(estVertSpeedClimb) + " km/h");
                } else {
                    if (DEBUG_this) Log.e(TAG, "vertSpeedClimb = NA");
                    result = false;
                }
                /* descending part in [km/h] */
                if (res.c > 0) {
                    estVertSpeedDescent = 3.6 / res.c;
                    if (DEBUG_this) Log.d(TAG, "vertSpeedDescent = " + formatDouble(estVertSpeedDescent) + " km/h");
                } else {
                    if (DEBUG_this) Log.e(TAG, "vertSpeedDescent = NA");
                    result = false;
                }
                estimationResult = new EstimationResult(result, segments, estHorSpeed, estVertSpeedClimb, estVertSpeedDescent,
                        res.rmse, res.r2);
            } else
                result = false;
        }

        return estimationResult;
    }

    private final static double MIN_HOR_SPEED = 1.0, MAX_HOR_SPEED = 10.0;
    private final static double MIN_SPEED_CLIMB = 0.1, MAX_SPEED_CLIMB = 1.0;
    private final static double MIN_SPEED_DESCENT = 0.1, MAX_SPEED_DESCENT = 1.0;
    private final static int MIN_GRADIENT_THRESHOLD_CLIMB = (int)(MIN_SPEED_CLIMB / MAX_HOR_SPEED * 100.0);
    private final static int MAX_GRADIENT_THRESHOLD_CLIMB = (int)(MAX_SPEED_CLIMB / MIN_HOR_SPEED * 100.0);
    private final static int MIN_GRADIENT_THRESHOLD_DESCENT = (int)(MIN_SPEED_DESCENT / MAX_HOR_SPEED * 100.0);
    private final static int MAX_GRADIENT_THRESHOLD_DESCENT = (int)(MAX_SPEED_DESCENT / MIN_HOR_SPEED * 100.0);

    public EstimationResult estimateGradients(List<Segment> inSegments) {
        // start values for min/max gradient calculation
        int minGradientThresholdClimb = MAX_GRADIENT_THRESHOLD_CLIMB,
                maxGradientThresholdClimb = MIN_GRADIENT_THRESHOLD_CLIMB,
                minGradientThresholdDesc = MAX_GRADIENT_THRESHOLD_DESCENT,
                maxGradientThresholdDesc = MIN_GRADIENT_THRESHOLD_DESCENT;
        int size = inSegments.size();
        EstimationResult estimateResult = null;
        List<EstimationVariation> variations = new ArrayList<>();
        boolean gradientsChanged = true;
        double min_rmse = 999.99; int optVariant = -1;

        boolean result = size > 0;
        if (result) {
            if (DEBUG) Log.i(TAG, "estimateGradients(): Estimation of hiking parameters");
            // calculate min/max gradient values of all segments
            for (int i = 0; i < inSegments.size(); i++) {
                Segment segment = inSegments.get(i);
                if (segment.climb_m > 0) {
                    if (segment.gradient < minGradientThresholdClimb)
                        minGradientThresholdClimb = segment.gradient;
                    else if (segment.gradient > maxGradientThresholdClimb)
                        maxGradientThresholdClimb = segment.gradient;
                } else if (segment.descent_m > 0) {
                    if (segment.gradient < minGradientThresholdDesc)
                        minGradientThresholdDesc = segment.gradient;
                    else if (segment.gradient > maxGradientThresholdDesc)
                        maxGradientThresholdDesc = segment.gradient;
                }
            }

            gradientThresholdClimb = minGradientThresholdClimb;
            while (gradientThresholdClimb <= maxGradientThresholdClimb) {
                gradientThresholdDesc = minGradientThresholdDesc;
                while (gradientThresholdDesc <= maxGradientThresholdDesc) {

                    for (int i = 0; i < inSegments.size(); i++) {
                        Segment segment = inSegments.get(i);
                        Segment.type prevType = segment.segmentType;
                        updateSegmentGradient(segment);
                        if (segment.segmentType != prevType)
                            gradientsChanged = true;
                    }

                    if (gradientsChanged) {
                        gradientsChanged = false;
                        estimateResult = estimateAll(inSegments);
                        if (estimateResult.successful) {
                            EstimationVariation variation = new EstimationVariation();
                            variation.result = estimateResult;
                            variation.gradientThresholdClimb = gradientThresholdClimb;
                            variation.gradientThresholdDesc = gradientThresholdDesc;
                            if ((estimateResult.estSpeedClimb < MAX_SPEED_CLIMB) &&
                                    (estimateResult.estSpeedDescent < MAX_SPEED_DESCENT))
                            {
                                if (estimateResult.rmse < min_rmse) {
                                    min_rmse = estimateResult.rmse;
                                    optVariant = variations.size();
                                }
                                variations.add(variation);
                            }
                        }
                        gradientThresholdDesc++;
                    }
                    else {
                        // recalculate min gradient values of all descending segments
                        int _minGradientThresholdDesc = maxGradientThresholdDesc+1;
                        gradientThresholdDesc++;
                        for (int i = 0; i < inSegments.size(); i++) {
                            Segment segment = inSegments.get(i);
                             if (segment.descent_m > 0) {
                                if ((segment.gradient > gradientThresholdDesc) && (segment.gradient < _minGradientThresholdDesc))
                                    _minGradientThresholdDesc = segment.gradient;
                            }
                        }
                        gradientThresholdDesc = _minGradientThresholdDesc;
                    }
                }
                // recalculate min gradient values of all climbing segments
                int _minGradientThresholdClimb = maxGradientThresholdClimb+1;
                for (int i = 0; i < inSegments.size(); i++) {
                    Segment segment = inSegments.get(i);
                    if (segment.climb_m > 0) {
                        if ((segment.gradient > gradientThresholdClimb) && (segment.gradient < _minGradientThresholdClimb))
                            _minGradientThresholdClimb = segment.gradient;
                    }
                }
                gradientThresholdClimb = _minGradientThresholdClimb;

            }
            if (optVariant >= 0)
            {
                EstimationVariation variation = variations.get(optVariant);
                gradientThresholdClimb = variation.gradientThresholdClimb;
                gradientThresholdDesc = variation.gradientThresholdDesc;
                for (int i = 0; i < inSegments.size(); i++) {
                    updateSegmentGradient(inSegments.get(i));
                }
                _estimationResult = new EstimationResult(variation.result);
            }
        }

        if (DEBUG) Log.i(TAG, "estimateGradients(): results");
        return _estimationResult;
    }

    public void addReport(String inReport) {
        _report = _report + inReport;
    }

    public void applyEstimatedHikingParametersFrom(EstimationResult fromOther) {
        _horSpeed = fromOther.estHorSpeed;
        _speedClimb = fromOther.estSpeedClimb;
        _speedDescent = fromOther.estSpeedDescent;
//        _minHeightChange = fromOther._minHeightChange;
    }

    /**
     * clear the resulting HTML string
     */
    public void clearRecordedTrackFileInfo() {
        _report = "";
    }

    public String getRecordedTrackFileInfo() {
        return _report;
    }

    public String getRecordedTrackFileInfo_Start() {
        // calculate the distance between start and end point to determine whether the tour ends at its start
        DataPoint start = _track.getPoint(0), end = _track.getPoint(_track.getNumPoints() - 1);
        double radians = DataPoint.calculateRadiansBetween(start, end);
        double dist = Distance.convertRadiansToDistance(radians);
        String tourType = dist < 1.0 ? "Rundtour" : "Streckentour";

        String warnTrackHasAltitudeJumps = trackHasAltitudeJumps ? "<br><b>Der Track weist Sprünge im Höhenprofil auf!</b>" : "";

        String description = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "\n" +
                "<head>\n" +
                "<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />\n" +
                "<title>Ohne_Titel_1</title>\n" +
                "</head><body>\n" +
                "<table style=\"width: 100%; border=\"0\" cellpadding=\"0\" cellspacing=\"5\" summary=\"\">\n\n" +
                "\t<tr>\n" +
                "\t\t<td colspan=\"5\"><b>Informationen</b></td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>Startzeit: </td>\n" +
                "\t\t<td align=\"right\">" + _track.getPoint(0).getTimestamp().getText(LOCALE, getSelectedTimezone()) + "</td>\n" +
                "\t\t<td>&nbsp;&nbsp;</td>\n" +
                "\t\t<td>Ende:</td>\n" +
                "\t\t<td align=\"right\">" + _track.getPoint(_track.getNumPoints() - 1).getTimestamp().getText(LOCALE, getSelectedTimezone()) + "</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>Trackzeit:</td>\n" +
                "\t\t<td align=\"right\">" + formatIntToTime((int) (getTotalSeconds() / 60L)) + "</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>Strecke: </td>\n" +
                "\t\t<td align=\"right\">" + formatDouble(summary.totalDistance_km) + " km</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td colspan=\"2\" align=\"right\">" + tourType + "</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td colspan=\"4\"><b>Statistiken</b></td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>Dauer in Bewegung:</td>\n" +
                "\t\t<td align=\"right\">" + formatIntToTime((int) (getTotalSeconds() / 60 - summary.totalBreakTime_min)) + "</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>Pausen:</td>\n" +
                "\t\t<td align=\"right\">" + formatIntToTime((int) (summary.totalBreakTime_min)) + "</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td colspan=\"4\"><b>Höhenmeter</b>\n" +
                warnTrackHasAltitudeJumps + " </td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>Aufstieg: </td>\n" +
                "\t\t<td align=\"right\">" + (int) (summary.sum_climb_m) + " hm </td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>Abstieg:</td>\n" +
                "\t\t<td align=\"right\">" + (int) (-summary.sum_descent_m) + " hm</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>Distanz eben:</td>\n" +
                "\t\t<td align=\"right\">" + formatDouble(summary.totalDistance_km - summary.totalDistanceClimb_km - summary.totalDistanceDescent_km) + " km</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>Distanz bergauf:</td>\n" +
                "\t\t<td align=\"right\">" + formatDouble(summary.totalDistanceClimb_km) + " km</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>Distanz bergab:</td>\n" +
                "\t\t<td align=\"right\">" + formatDouble(summary.totalDistanceDescent_km) + " km</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>Höchster Punkt: </td>\n" +
                "\t\t<td align=\"right\">" + (int) (BaseSegments.getMaxAltitude()) + " m</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>Tiefster Punkt:</td>\n" +
                "\t\t<td align=\"right\">" + (int) (BaseSegments.getMinAltitude()) + " m</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td colspan=\"5\"><b>Ermittelte Parameter zur Gehzeitberechnung</b><br>" +
                "Zuverlässigkeit</td>\n";
        return description;
    }

    public String getRecordedTrackFileInfo_Success() {
            String description =
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>RMSE:<br>(≈ 0 → exzellent): </td>\n" +
                "\t\t<td align=\"right\">" + baseSegments.formatDouble(_estimationResult.rmse) + "</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>R²:<br>(1 → stark)</td>\n" +
                "\t\t<td align=\"right\">" + baseSegments.formatDouble(_estimationResult.r2) + "</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td colspan=\"5\">Geschwindigkeiten</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>in der Ebene:</td>\n" +
                "\t\t<td align=\"right\">" + formatDouble(_estimationResult.estHorSpeed) + " km/h</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t</tr>\n" +
                "\t<tr>\n" +
                "\t\t<td>im Aufstieg:</td>\n" +
                "\t\t<td align=\"right\">" + (int) (_estimationResult.estSpeedClimb * 100.0) * 10 + " hm/h</td>\n" +
                "\t\t<td>&nbsp;</td>\n" +
                "\t\t<td>im Abstieg:</td>\n" +
                "\t\t<td align=\"right\">" + (int) (_estimationResult.estSpeedDescent * 100.0) * 10 + " hm/h</td>\n" +
                "\t</tr>\n";
        return description;
    }

    public String getRecordedTrackFileInfo_Failed() {
        String description =
                "\t<tr>\n" +
                        "\t\t<td colspan=\"5\"><b>es konnten keine Parameter ermittelt werden!</b></td>\n" +
                        "\t</tr>\n";
        return description;
    }
    public String getRecordedTrackFileInfo_Prove(List<Segment> inSegments) {
        // calculate the distance between start and end point to determine whether the tour ends at its start
        String description =
                "\t<tr>\n" +
                        "\t\t<td colspan=\"5\"><b>Neuberechnung auf Basis dieser Parameter</b></td>\n" +
                        "\t</tr>\n" +
                         "\t<tr>\n" +
                        "\t\t<td>Dauer mit " + inSegments.size() + " Segmenten:</td>\n" +
                        "\t\t<td align=\"right\">" + formatIntToTime((int) (calcTotalTimeFromSegments(inSegments) / 60L)) + "</td>\n" +
                        "\t</tr>\n";
        return description;
    }

    public String getRecordedTrackFileInfo_UsingSettings(List<Segment> inSegments) {
        // calculate the distance between start and end point to determine whether the tour ends at its start
        String description =
                "\t<tr>\n" +
                        "\t\t<td colspan=\"5\"><b>Eingestellte Parameter zur Gehzeitberechnung</b><br>" +
                        "Geschwindigkeiten</td>\n" +
                        "\t</tr>\n" +
                        "\t<tr>\n" +
                        "\t\t<td>in der Ebene:</td>\n" +
                        "\t\t<td align=\"right\">" + formatDouble(_horSpeed) + " km/h</td>\n" +
                        "\t\t<td>&nbsp;</td>\n" +
                        "\t\t<td>&nbsp;</td>\n" +
                        "\t\t<td>&nbsp;</td>\n" +
                        "\t</tr>\n" +
                        "\t<tr>\n" +
                        "\t\t<td>im Aufstieg:</td>\n" +
                        "\t\t<td align=\"right\">" + (int) (_speedClimb * 100.0) * 10 + " hm/h</td>\n" +
                        "\t\t<td>&nbsp;</td>\n" +
                        "\t\t<td>im Abstieg:</td>\n" +
                        "\t\t<td align=\"right\">" + (int) (_speedDescent * 100.0) * 10 + " hm/h</td>\n" +
                        "\t</tr>\n" +
                        "\t<tr>\n" +
                        getRecordedTrackFileInfo_Prove(inSegments) +
                        "</table></body></html>\n";
        return description;
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

