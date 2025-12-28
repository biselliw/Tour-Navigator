package de.biselliw.tour_navigator.data;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import tim.prune.data.Altitude;
import tim.prune.data.Distance;

import org.w3c.dom.*;

public class TrackTiming {
    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackTiming";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public static TrackTiming trackTiming = null;

    /** Flags for whether minimum or maximum has been found */
    private boolean _gotPreviousMinimum = false, _gotPreviousMaximum = false;
    /** values of previous minimum and maximum, if any */
    private double  _previousExtreme = 0;

    private double _minAltitude_m = 0, _maxAltitude_m = 0;
    private double _prevAltitude_m = -1;

    private static double _totalDistance_km = 0.0;
    static long _totalBreakTime_min = 0L;
    private long   _totalSeconds = 0L;
    private static double  _totalClimb_m = 0;
    private static double  _totalDescent_m = 0;

    /** if true: data must be recalculated in current segment of the track */
    private boolean _segRecalc = false;
    private double  _segDistance_km = 0.0;
    private int     _segStart = 0;
    private long    _segStart_s = 0L;
    private double  _segStartClimb_m = 0;
    private double  _segStartDescent_m = 0;

    private int     _ptIndex;
    private DataPoint _prevPoint = null;


    // hiking speed parameters

    public final static double DEF_HOR_SPEED 			= 4.5;
    public final static double DEF_VERT_SPEED_CLIMB 	= 0.35;
    public final static double DEF_VERT_SPEED_DESC 		= 0.5;
    public final static int    DEF_MIN_HEIGHT_CHANGE 	= 15;

    final static double _secondsHour 			= 3600.0;
    final static double _vertSecondsHour 		= 3.6;

    /** horizontal part in [km/h] */
    private static double _horSpeed = 			DEF_HOR_SPEED;
    /** climbing part in [km/h] */
    private static double _vertSpeedClimb = 	DEF_VERT_SPEED_CLIMB;
    /** descending part in [km/h] */
    private static double _vertSpeedDescent = 	DEF_VERT_SPEED_DESC;
    /** hysteresis value for a change of altitude */
    private static double _minHeightChange = 	DEF_MIN_HEIGHT_CHANGE;

    private static final double INVALID_VALUE = -9999.99;

    private final TrackDetails _track;

    public TrackTiming (TrackDetails inTrack)
    {
        _track = inTrack;
        trackTiming = this;
    }

    /**
     * set hiking parameters:
     *
     * @param inHorSpeed         horizontal part in [km/h]
     * @param inVertSpeedClimb   ascending part in [km/h]
     * @param inVertSpeedDescent descending part in [km/h]
     * @param inMinHeightChange  min. required change of altitude
     */
    public static void setHikingParameters(double inHorSpeed, double inVertSpeedClimb, double inVertSpeedDescent, double inMinHeightChange) {
        if (inHorSpeed > 0)
            _horSpeed = inHorSpeed;
        if (inVertSpeedClimb > 0)
            _vertSpeedClimb = inVertSpeedClimb;
        if (inVertSpeedDescent > 0)
            _vertSpeedDescent = inVertSpeedDescent;
        if (inMinHeightChange > 0)
            _minHeightChange = inMinHeightChange;
    }

    /**
     * Recalculate all track points
     */
    public List<RecordAdapter.Record> recalculate() throws Exception {
        List<RecordAdapter.Record> recordList = null;
        boolean smooth = false;

        double sum_distance = 0.0;
        double sum_climb = 0;
        double sum_descent = 0;
        long sum_seconds = 0L;
        int breakTime_min = 0;

        RecordAdapter.Record record;

        int _numPoints = _track.getNumPoints();
        if (_numPoints <= 0) return null;

        if (DEBUG) {
            Log.d(TAG, "recalculate(): " + _numPoints + " Trackpoints");
        }

        recordList = new ArrayList<>();

        _totalDistance_km = 0L;
        _prevAltitude_m = -1;
        _totalClimb_m = 0L;
        _totalDescent_m = 0L;
        _totalBreakTime_min = 0L;

        if (smooth) {
            /* test better smoothing - poor results ! */
            double sigmaMeters = 30;
            double maxGradeUp = 0.30; // 15% allowed climbing gradient for ascent/descent metrics
            double maxGradeDown = 0.10; // 15% allowed climbing gradient for ascent/descent metrics

            double[] distances = cumulativeDistances();
            double[] elevations = new double[_numPoints];
            for (int i = 0; i < _numPoints; i++) {
                DataPoint currPoint = _track.getPoint(i);
                double altitude_m = 0.0;
                elevations[i] = INVALID_VALUE;
                if ((currPoint != null) && !currPoint.isWayPoint())
                    // does the current point has a valid altitude?
                    if (currPoint.hasAltitude()) {
                        Altitude altitude = currPoint.getAltitude();
                        if (altitude.isValid())
                            elevations[i] = altitude.getValue();
                    }
            }

            double origUp = computeAscent(elevations, distances, maxGradeUp);
            double origDown = computeDescent(elevations, distances, maxGradeDown);
            System.out.printf("Original ascent: %.1f m | descent: %.1f m%n", origUp, origDown);

            double[] smoothed = gaussianSmoothByDistance(elevations, distances, sigmaMeters);
            double[] finalAlt = preserveAscentDescent(smoothed, elevations);
            double smoothUp = computeAscent(finalAlt, distances, maxGradeUp);
            double smoothDown = computeDescent(finalAlt, distances, maxGradeDown);
            System.out.printf("Smoothed ascent: %.1f m | descent: %.1f m%n", smoothUp, smoothDown);


            throw new Exception("No records created yet");
        }
        else {
            for (int ptIndex = 0; ptIndex <= _numPoints - 1; ptIndex++) {
                DataPoint currPoint = _track.getPoint(ptIndex);

                addPoint(ptIndex);
                if (currPoint.isRoutePoint()) {
                    double calc_climb = _totalClimb_m - sum_climb;
                    double calc_descent = _totalDescent_m - sum_descent;
                    /*
                     * @todo fix workaround for negative values
                     */
                    double use_climb = calc_climb;
                    double use_descent = calc_descent;
                    if (calc_climb < 0) {
                        // exchange parts
                        // - sum of climb values remains unchanged -> invalid _totalDescent_m
                        use_climb = 0;
                        use_descent -= calc_climb;
                        Log.d(TAG,"fixed negative climb value for point " + currPoint.toString());
                    }
                    if (calc_descent < 0) {
                        // exchange parts
                        // - sum of descent values remains unchanged -> invalid _totalClimb_m
                        use_descent = 0;
                        use_climb -= calc_descent;
                        Log.d(TAG,"fixed negative descent value for point " + currPoint.toString());
                    }

                    record = new RecordAdapter.Record(
                            currPoint,
                            ptIndex,
                            _totalDistance_km - sum_distance,
                            use_climb,
                            use_descent,
                            currPoint.getTime() - sum_seconds - breakTime_min * 60L
                    );

                    sum_distance = _totalDistance_km;
                    sum_climb = _totalClimb_m;
                    sum_descent = _totalDescent_m;
                    sum_seconds = currPoint.getTime();

                    breakTime_min = currPoint.getWaypointDuration();
                    _totalBreakTime_min += breakTime_min;

                    recordList.add(record);
                }
                /*
                if (DEBUG) {
                    Log.d(TAG, "timetable built");
                    Log.d(TAG, "Sclimb: " + _totalClimb_m);
                    Log.d(TAG, "Sdescent: " + _totalDescent_m);
                }
                 */
            }
        }
        return recordList;
    }

    /**
     * add location data of a point to calculate its time and distance since start
     * @param ptIndex index of current data point
     * see clearPointCalc ()
     */
    public boolean addPoint(int ptIndex)
    {
        _ptIndex = ptIndex;
        DataPoint currPoint = _track.getPoint(ptIndex);

        boolean result = false;
        boolean overallUp = false, overallDn = false;
        double altitude_m = 0.0;

        if ((currPoint == null) || currPoint.isWayPoint())
            return false;

        // does the current point has a valid altitude?
        if (currPoint.hasAltitude())
        {
            Altitude altitude = currPoint.getAltitude();
            if (altitude.isValid())
                altitude_m = altitude.getValue();
        }
        if (altitude_m > 0)
        {
            // did the previous point have a valid altitude?
            if (_prevAltitude_m >= 0)
            {
                // Got an altitude value which is different from the previous one
                boolean segClimbing = (altitude_m > _prevAltitude_m);
                overallUp = _gotPreviousMinimum && (_prevAltitude_m > _previousExtreme);
                overallDn = _gotPreviousMaximum && _prevAltitude_m < _previousExtreme;
                final boolean moreThanWiggle = Math.abs(altitude_m - _prevAltitude_m) > _minHeightChange;

                // Do we know whether we're going up or down yet?
                if (!_gotPreviousMinimum && !_gotPreviousMaximum)
                {
                    // we don't know whether we're going up or down yet - check limit
                    if (moreThanWiggle)
                    {
                        if (segClimbing)
                            _gotPreviousMinimum = true;
                        else
                            _gotPreviousMaximum = true;
                        _previousExtreme = _prevAltitude_m;
                        _prevAltitude_m = altitude_m;
                    }
                }
                else if (overallUp)
                {
                    if (segClimbing)
                        // we're still going up - do nothing
                        _prevAltitude_m = altitude_m;
                    else if (moreThanWiggle)
                        // we're going up but have dropped over a maximum
                        _segRecalc = true;
                }
                else if (overallDn)
                {
                    if (segClimbing)
                    {
                        if (moreThanWiggle)
                            // we're going down but have climbed up from a minimum
                            _segRecalc = true;
                    }
                    else
                        // we're still going down - do nothing
                        _prevAltitude_m = altitude_m;
                }
            }
            else
                // we haven't got a previous value at all, so it's the start of a new segment
                _prevAltitude_m = altitude_m;
        }

        // Calculate the distance to the previous trackpoint
        if (_prevPoint != null)
        {
            double radians = DataPoint.calculateRadiansBetween(_prevPoint, currPoint);
            double dist = Distance.convertRadiansToDistance(radians);
            _totalDistance_km += dist;
            currPoint.setDistance(_totalDistance_km);
            _segDistance_km += dist;
        }
        else
            currPoint.setDistance(0.0);
        currPoint.setTime(0L);

        // remember previous  point
        _prevPoint = currPoint;

        if (altitude_m > 0)
        {
            // need to calculate intermediate time?
            if (currPoint.isRoutePoint() || _segRecalc)
            {
                long segSeconds, horSeconds, vertSeconds;
                // calculate section times
                horSeconds = (long) (_segDistance_km / _horSpeed * _secondsHour);

                if (overallUp) {
                    // Add the climb from _previousExtreme up to _previousValue
                    double climb_m;
                    if (_segRecalc)
                    {
                        climb_m = _prevAltitude_m - _previousExtreme;
                        _previousExtreme = _prevAltitude_m;
                        _gotPreviousMinimum = false; _gotPreviousMaximum = true;
                        _prevAltitude_m = altitude_m;
                    }
                    else
                    {
                        climb_m = altitude_m - _previousExtreme;
                    }
                    _totalClimb_m = _segStartClimb_m + climb_m;

                    vertSeconds = (long) (climb_m / _vertSpeedClimb * _vertSecondsHour);
                } else if (overallDn) {
                    // Add the descent from _previousExtreme down to _previousValue
                    double descent_m;
                    if (_segRecalc)
                    {
                        descent_m = _previousExtreme - _prevAltitude_m;
                        _previousExtreme = _prevAltitude_m;
                        _gotPreviousMinimum = true; _gotPreviousMaximum = false;
                        _prevAltitude_m = altitude_m;
                    }
                    else
                    {
                        descent_m = _previousExtreme - altitude_m;
                    }
                    _totalDescent_m = _segStartDescent_m + descent_m;

                    vertSeconds = (long) (descent_m / _vertSpeedDescent * _vertSecondsHour);
                } else {
                    vertSeconds = 0;
                }

                if (horSeconds > vertSeconds) {
                    segSeconds = (long) (horSeconds + vertSeconds / 2.0);
                } else {
                    segSeconds = (long) (horSeconds / 2.0 + vertSeconds);
                }

                calcSegmentTimes (segSeconds);
                result = true;
            }

            if (_segRecalc) {
                resetSegment();
            }

            if ((altitude_m < _minAltitude_m) || (_minAltitude_m <= 0))
                _minAltitude_m = altitude_m;
            else if (altitude_m > _maxAltitude_m)
                _maxAltitude_m = altitude_m;
        }

        return result;
    }

    /**
     * Calculate distances and times of all points within the segment after analysis of the segment
     * @param inSegSeconds calculated total time within the segment
     */
    private void calcSegmentTimes (long inSegSeconds) {
        double segStartDistance_km = 0.0;
        int totalBreakTime_min = 0;
        for (int ptIndex = _segStart; ptIndex <= _ptIndex; ptIndex++) {
            DataPoint currPoint = _track.getPoint(ptIndex);

            if (_segDistance_km > 0.0)
            {
                if (ptIndex == _segStart)
                    segStartDistance_km = currPoint.getDistance();
                else
                {
                    double segRelDistance = (currPoint.getDistance() - segStartDistance_km) / _segDistance_km;
                    double segCurrTime = segRelDistance * (double)inSegSeconds + 31;
                    long Time_s = _segStart_s + (long)segCurrTime + totalBreakTime_min * 60L;
                    long prevTime_s = currPoint.getTime();
                    if (Time_s > prevTime_s)
                        currPoint.setTime(Time_s);
                    totalBreakTime_min += currPoint.getWaypointDuration();
                }
            }
        }

        // update current overall data from section data
        _totalSeconds  = _segStart_s + inSegSeconds + totalBreakTime_min*60;
    }

    private void resetSegment()
    {
        // update overall data at the end of the segment from section data
        _segDistance_km = 0.0;
        _segStart = _ptIndex;
        _segStart_s = _totalSeconds;
        _segStartClimb_m = _totalClimb_m;
        _segStartDescent_m = _totalDescent_m;
        _segRecalc = false;
    }

    public long getTotalBreakTime_min() { return _totalBreakTime_min; }
    public double getMinAltitude() { return _minAltitude_m; }
    public double getMaxAltitude() { return _maxAltitude_m; }
    public double getTotalDistance () {	return _totalDistance_km;}
    public double getTotalClimb () { return _totalClimb_m; }
    public double getTotalDescent () { return _totalDescent_m; }
    public long getTotalSeconds () {
        return _totalSeconds;
    }

    /**
     * How it works (business-level explanation)
     * Stage	Purpose
     * Gaussian smoothing	Remove local noise
     * Compute ascent/descent totals	Anchor performance metrics
     * Scaling positive & negative deltas separately	Maintain climb/desc symmetry
     * Re-integrate from first altitude	Guarantee continuity
     *
     * This approach ensures:
     *
     * No distortion of total ascent
     *
     * No distortion of total descent
     *
     * Smooth, natural-looking profile
     *
     * Robust to arbitrary noise levels
     *
     * Numerical Stability Considerations
     *
     * The algorithm handles:
     *
     * flat segments
     *
     * very small σ
     *
     * very noisy GPS tracks
     *
     * zero-ascent or zero-descent routes
     *
     * If original ascent or descent truly equals 0, scaling defaults to 1.0 to avoid exploding values.
     *
     * Recommended σ (for distance-tracked GPX)
     * Activity	σ
     * Running	1.0–1.5
     * Road cycling	1.5–2.5
     * MTB	1.0–2.0
     * Hiking	1.0–2.0
     *
     *
     */


    /**
     * Gaussian smoothing overview (for altitudes)
     * For samples x[i] and Gaussian kernel w[k]:
     * y[i]=∑w[k]*x[i+k]
     *
     *
     * Gaussian smoothing based on distance spacing (meters), not point index
     *
     * Ignore unrealistically steep segments
     *
     * Report ascent/descent before & after smoothing (and still preserve totals)
     *
     *
     * Key Design Choices (Summary)
     *
     * We compute true path distance between points using the haversine formula.
     *
     * The Gaussian kernel is evaluated continuously by distance, not by index.
     *
     * Unrealistic gradients (configurable) are excluded from ascent/descent metrics.
     *
     * Ascent/descent totals are preserved after smoothing.
     *
     *
     * Defaults (you can tune):
     *
     * σDistanceMeters = 30 m
     *
     * Max gradient for ascent accounting = 150 m per km (15%), adjust if desired.
     * Tuning Guidance
     * 1. Smoothing strength (σ in meters)
     * σ (m)	Use case
     * 10–20	running, dense sampling
     * 20–40	road cycling
     * 40–80	MTB / noisy barometer
     * 100+	very noisy data
     * 2. Gradient threshold
     * maxGrade = 0.15
     *
     *
     * Means 15% grade.
     * If your GPS is noisy, reduce to 0.10.
     *
     * This setting affects ascent/descent computation only, not smoothing.
     */



    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double[] cumulativeDistances() {
        int n = _track.getNumPoints();
        double[] d = new double[n];
        d[0] = 0;
        _track.getPoint(0).setDistance(0);
        for (int i = 1; i < n; i++) {
            d[i] = d[i - 1] + haversine(
                    _track.getPoint(i - 1).getLatitude().getDouble(),
                    _track.getPoint(i - 1).getLongitude().getDouble(),
                    _track.getPoint(i).getLatitude().getDouble(),
                    _track.getPoint(i).getLongitude().getDouble());
            _track.getPoint(i).setDistance(d[i]);
        }
        return d;
    }

    private double computeAscent(double[] vals, double[] dist, double maxGrade) {
        double up = 0;
        int n = vals.length;
        for (int i = 0; i < n-1; i++) {
            if (vals[i] != INVALID_VALUE) {
                double lastVal = vals[i];
                for (int j = i+1; j < n; j++) {
                    if (vals[j] != INVALID_VALUE) {
                        double currVal = vals[j];
                        double d = currVal - lastVal;
                        double dx = dist[j] - dist[i];
                        if (dx > 0) {
                            double grade = Math.abs(d / dx);
                            if (d >= 0)
                                if (grade <= maxGrade) up += d;
                                else
                                    up = up;
                        }
                        break;
                    }
                }
            }
        }
        return up;
    }

    private double computeDescent(double[] vals, double[] dist, double maxGrade) {
        double down = 0;
        int n = _track.getNumPoints();
        for (int i = 0; i < n-1; i++) {
            if (vals[i] != INVALID_VALUE) {
                double lastVal = vals[i];
                for (int j = i+1; j < n; j++) {
                    if (vals[j] != INVALID_VALUE) {
                        double currVal = vals[j];
                        double d = currVal - lastVal;
                        double dx = dist[j] - dist[i];
                        if (dx > 0) {
                            double grade = Math.abs(d / dx);
                            if (d <= 0)
                                if (grade <= maxGrade) down -= d;
                                else
                                    down = down;
                        }
                        break;
                    }
                }
            }
        }
        return down;
    }

    private double[] gaussianSmoothByDistance(double[] values,
                                              double[] dist,
                                              double sigmaMeters) {
        int n = values.length;
        double[] out = new double[n];
        double twoSigma2 = 2 * sigmaMeters * sigmaMeters;

        for (int i = 0; i < n; i++) {
            if (values[i] != INVALID_VALUE) {
                double sum = 0;
                double wsum = 0;

                for (int j = 0; j < n; j++) {
                    if (values[j] != INVALID_VALUE) {
                        double d = dist[j] - dist[i];
                        double w = Math.exp(-(d * d) / twoSigma2);
                        sum += w * values[j];
                        wsum += w;
                    }
                }
                out[i] = sum / wsum;
            }
            else
                out[i] = INVALID_VALUE;
        }
        return out;
    }

    private double[] preserveAscentDescent(double[] smoothed, double[] original) {

        int n = smoothed.length;

        double origUp = 0, origDown = 0;
        for (int i = 0; i < n-1; i++) {
            if (original[i] != INVALID_VALUE) {
                double lastVal = original[i];
                for (int j = i + 1; j < n; j++) {
                    if (original[j] != INVALID_VALUE) {
                        double currVal = original[j];
                        double d = currVal - lastVal;
                        if (d > 0) origUp += d;
                        else origDown -= d;
                        break;
                    }
                }
            }
        }

        double smoothUp = 0, smoothDown = 0;
        for (int i = 0; i < n-1; i++) {
            if (smoothed[i] != INVALID_VALUE) {
                double lastVal = smoothed[i];
                for (int j = i + 1; j < n; j++) {
                    if (smoothed[j] != INVALID_VALUE) {
                        double currVal = smoothed[j];
                        double d = currVal - lastVal;
                        if (d > 0) smoothUp += d;
                        else smoothDown -= d;
                        break;
                    }
                }
            }
        }

        double upScale = (smoothUp > 0) ? (origUp / smoothUp) : 1.0;
        double downScale = (smoothDown > 0) ? (origDown / smoothDown) : 1.0;

        double[] adjusted = new double[n];
        adjusted[0] = smoothed[0];

        for (int i = 0; i < n-1; i++) {
            if (smoothed[i] != INVALID_VALUE) {
                double lastVal = smoothed[i];
                for (int j = i + 1; j < n; j++) {
                    if (smoothed[j] != INVALID_VALUE) {
                        double currVal = smoothed[j];
                        double d = currVal - lastVal;
                        if (d > 0) d *= upScale;
                        else d *= downScale;
                        adjusted[j] = adjusted[i] + d;
                        break;
                    }
                }
            }
            else
                adjusted[i] = INVALID_VALUE;
        }

        return adjusted;
    }

}

