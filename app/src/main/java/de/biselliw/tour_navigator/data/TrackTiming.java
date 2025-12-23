package de.biselliw.tour_navigator.data;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import tim.prune.data.Altitude;
import tim.prune.data.Distance;


public class TrackTiming {
    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackTiming";
    private static final boolean _DEBUG = false; // Set to true to enable logging
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

    public final static double DEF_HOR_SPEED 			= 5.0;
    public final static double DEF_VERT_SPEED_CLIMB 	= 0.35;
    public final static double DEF_VERT_SPEED_DESC 		= 0.5;
    public final static int    DEF_MIN_HEIGHT_CHANGE 	= 3;

    final static double _secondsHour 			= 3600.0;
    final static double _vertSecondsHour 		= 3.6;

    /** horizontal part in [km/h] */
    private static double _horSpeed = 			DEF_HOR_SPEED;
    /** climbing part in [km/h] */
    private static double _vertSpeedClimb = 	DEF_VERT_SPEED_CLIMB;
    /** descending part in [km/h] */
    private static double _vertSpeedDescent = 	DEF_VERT_SPEED_DESC;
    /** hysteresis value for a change of altitude */
    private static int    _minHeightChange = 	DEF_MIN_HEIGHT_CHANGE;


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
    public static void setHikingParameters(double inHorSpeed, double inVertSpeedClimb, double inVertSpeedDescent, int inMinHeightChange) {
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
    public List<RecordAdapter.Record> recalculate() {
        List<RecordAdapter.Record> recordList = null;
        double sum_distance = 0.0;
        double sum_climb = 0;
        double sum_descent = 0;
        long sum_seconds = 0L;
        int breakTime_min = 0;

        RecordAdapter.Record record;

        int _numPoints = _track.getNumPoints();
        if (_numPoints <= 0) return null;

        if (DEBUG) {
            android.util.Log.d(TAG, "recalculate(): " + _numPoints + " Trackpoints");
        }

        recordList = new ArrayList<>();

        _totalDistance_km = 0L;
        _prevAltitude_m = -1;
        _totalClimb_m = 0L;
        _totalDescent_m = 0L;
        _totalBreakTime_min = 0L;

        for (int ptIndex = 0; ptIndex <= _numPoints - 1; ptIndex++) {
            DataPoint currPoint = _track.getPoint(ptIndex);

            addPoint(ptIndex);
            if (currPoint.isRoutePoint())
            {
                record = new RecordAdapter.Record(
                        currPoint,
                        ptIndex,
                        _totalDistance_km - sum_distance,
                        _totalClimb_m - sum_climb,
                        _totalDescent_m - sum_descent,
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
            if (DEBUG) {
                android.util.Log.d(TAG, "timetable built");
                android.util.Log.d(TAG, "Sclimb: " + _totalClimb_m);
                android.util.Log.d(TAG, "Sdescent: " + _totalDescent_m);
            }
        }
        return recordList;
    }

    public void addPoint(int ptIndex)
    {
        _ptIndex = ptIndex;
        DataPoint currPoint = _track.getPoint(ptIndex);
        addPoint(currPoint);
    }

    /**
     * add location data of a point to calculate its time and distance since start
     * @param inPoint current data point
     * @return true if successful
     * see clearPointCalc ()
     */
    private boolean addPoint(DataPoint inPoint)
    {
        boolean result = false;
        boolean overallUp = false, overallDn = false;
        double altitude_m = 0.0;

        if ((inPoint == null) || inPoint.isWayPoint())
            return false;

        // does the current point has a valid altitude?
        if (inPoint.hasAltitude())
        {
            Altitude altitude = inPoint.getAltitude();
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
            double radians = DataPoint.calculateRadiansBetween(_prevPoint, inPoint);
            double dist = Distance.convertRadiansToDistance(radians);
            _totalDistance_km += dist;
            inPoint.setDistance(_totalDistance_km);
            _segDistance_km += dist;
        }
        else
            inPoint.setDistance(0.0);
        inPoint.setTime(0L);

        // remember previous  point
        _prevPoint = inPoint;;

        if (altitude_m > 0)
        {
            // need to calculate intermediate time?
            if (inPoint.isRoutePoint() || _segRecalc)
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
}
