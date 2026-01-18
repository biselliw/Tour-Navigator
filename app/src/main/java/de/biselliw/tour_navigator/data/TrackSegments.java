package de.biselliw.tour_navigator.data;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import tim.prune.data.Altitude;
import tim.prune.data.Distance;

/**
 * see tim.prune.data.AltitudeRange
 */
public class TrackSegments {
    // hiking speed parameters
    /**
     * TAG for log messages.
     */
    static final String TAG = "BaseSegments";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    private static final boolean TEST = false;
    private static final boolean FEATURED = false;
    private static final boolean CHECK_FLAT_SEGMENTS = true;
    private static final double MIN_DISTANCE_FLAT_SEGMENT = 0.25;
    private static final boolean FIX_ALTITUDE_JUMPS = false;


//    public static TrackSegments baseSegments = null;
    public final static double DEF_HOR_SPEED = 4.5;
    public final static double DEF_SPEED_CLIMB = 0.35;
    public final static double DEF_SPEED_DESCENT = 0.5;
    /** Tolerance value in metres */
    public final static int DEF_MIN_HEIGHT_CHANGE = 15;

    public final static double DEF_GRADIENT_THRESHOLD_CLIMB = DEF_SPEED_CLIMB / DEF_HOR_SPEED * 100.0;
    public final static double DEF_GRADIENT_THRESHOLD_DESC = DEF_SPEED_DESCENT / DEF_HOR_SPEED * 100.0;
    public final static double MIN_HOR_SPEED = 0.1 * DEF_HOR_SPEED;

    /**
     * horizontal part in [km/h]
     */
    protected double _horSpeed = DEF_HOR_SPEED;
    /**
     * climbing part in [km/h]
     */
    protected double _speedClimb = DEF_SPEED_CLIMB;
    protected int gradientThresholdClimb = (int)(DEF_GRADIENT_THRESHOLD_CLIMB);
    /**
     * descending part in [km/h]
     */
    protected double _speedDescent = DEF_SPEED_DESCENT;
    protected int gradientThresholdDesc = (int)(DEF_GRADIENT_THRESHOLD_DESC);
    /**
     * hysteresis value [m] for a change of altitude
     */
    protected double _wiggleLimit = DEF_MIN_HEIGHT_CHANGE;
    protected TrackDetails _track = null;

    protected boolean trackHasTimeStamps = false;

    protected final double maxAltitudeJump_m = 25.0;
    protected boolean trackHasAltitudeJumps = false;
    private double _offsetAltitudeJump = 0.0;

    private DataPoint _prevPoint = null;

    /** Flag for whether previous altitude value exists or not */
    private boolean _gotPreviousAltitudeValue;
    private double _altitudeValue_m = 0.0,
    /** Value of previous minimum or maximum, if any */
    _previousExtreme,
    /** Previous metric value */
    _previousValue_m, _lastAltitude_m;
    double _lastAltitudeDiff_m, _sumLastAltitudes = 0;
    int _indexPreviousExtreme;
    private static double _minAltitude_m = 0, _maxAltitude_m = 0;


    private boolean _overallUp, _overallDn;

    /** Flags for whether minimum or maximum has been found */
    private boolean _gotPreviousMinimum, _gotPreviousMaximum;
    private double _previousMinimum = -1, _previousMaximum = -1;
    int _indexPreviousMinimum, _indexPreviousMaximum;
    /** Flag for whether previous value exists or not */

    public static SummarySegments summary = new SummarySegments();

    private Segment _segment = null, _prevSegment;

    boolean checkFlatSegment = false;

    /** if true: data must be recalculated in current segment of the track */
    protected boolean _segRecalc = false;

    private void clear () {
        _prevSegment = null;
        _gotPreviousAltitudeValue = false;
        _previousValue_m = 0;

        /* Flags for whether minimum or maximum has been found */
        _gotPreviousMinimum = _gotPreviousMaximum = false;
        _previousExtreme = 0;


        trackHasAltitudeJumps = false; _offsetAltitudeJump = 0.0;
        _lastAltitude_m = -1;
        if (CHECK_FLAT_SEGMENTS) {
            checkFlatSegment = false;
            _lastAltitudeDiff_m = 0.0;
            _sumLastAltitudes = 0.0;
        }

        _previousMinimum = -1; _previousMaximum = -1;
        if (DEBUG) {
            _indexPreviousMinimum = _indexPreviousMaximum = -1;
        }
        if (FEATURED)
            _indexPreviousExtreme = -1;

        _prevPoint = null;

        /* if true: data must be recalculated in current segment of the track */
        _segRecalc = false;
        _minAltitude_m = 0; _maxAltitude_m = 0;
    }

    /**
     * Analyze all track points and divide the track into segments
     * @param inTrack track
     * @return list of track segments
     */
    public List<Segment> calcSegments(TrackDetails inTrack) {
        _track = inTrack;
//        baseSegments = this;

        clear ();
        double totalDistance_km = 0.0;
        List<Segment> segments = new ArrayList<>();
        // create a new segment, start with up/down movement
        _segment = new Segment();

        int _numPoints = inTrack.getNumPoints();
        if (_numPoints <= 0) return null;

        if (DEBUG) Log.d(TAG, "calcSegments(): " + _numPoints + " Trackpoints");

        boolean finish = false;
        for (int ptIndex = 0; ptIndex < _numPoints; ptIndex++) {
            DataPoint currPoint = inTrack.getPoint(ptIndex);

            if (ptIndex >= _numPoints - 1)
                finish = true;

            if ((currPoint != null) && (!currPoint.isWayPoint() || finish)) {
                calc1(ptIndex);

                // Calculate the distance to the previous trackpoint
                if (_prevPoint != null) {
                    double radians = DataPoint.calculateRadiansBetween(_prevPoint, currPoint);
                    double dist = Distance.convertRadiansToDistance(radians);
                    // summarize distances in current segment - only temporary!
                    totalDistance_km += dist;
                    // calc distance since start for each data point
                    currPoint.setDistance(totalDistance_km);
                } else
                    currPoint.setDistance(0.0);

                // remember previous  point
                _prevPoint = currPoint;

                // need to calculate time?
                if (_segRecalc || finish) {
                    updateSegment(ptIndex);

                    if (_segment.segmentType != Segment.type.SEG_INVALID) {
                        if (_prevSegment != null && _prevSegment.segmentType == Segment.type.SEG_FLAT && _segment.segmentType == Segment.type.SEG_FLAT)
                            _prevSegment.distance_km += _segment.distance_km;
                        else
//                            if (_segment.distance_km >= MIN_DISTANCE_FLAT_SEGMENT)
                                segments.add(_segment);
                        _prevSegment = _segment;
                    }

                    if (finish || (checkFlatSegment && CHECK_FLAT_SEGMENTS))
                    {
                        DataPoint start = _track.getPoint(
                                (_segment.segmentType == Segment.type.SEG_INVALID) ?
                                _segment.startIndex : _segment.endIndex);
                        double distance_km = currPoint.getDistance() - start.getDistance(); // segment.startDistance_km - segment.distance_km;
                        if (distance_km > 0)
                            if ((distance_km >= MIN_DISTANCE_FLAT_SEGMENT) || finish) {
                                // start with new segment
                                int endIndex = _segment.endIndex;
                                _segment = new Segment(_segment);
                                _segment.segmentType = Segment.type.SEG_FLAT;
                                _segment.endIndex = ptIndex;
                                _segment.distance_km = distance_km;

                                if (DEBUG)
                                    _segment.altitudeDiff = currPoint.getAltitude().getValue() - start.getAltitude().getValue();

                                // update overall data at the end of the segment from section data
                                if (_prevSegment != null && _prevSegment.segmentType == Segment.type.SEG_FLAT && _segment.segmentType == Segment.type.SEG_FLAT)
                                    _prevSegment.distance_km += _segment.distance_km;
                                else
                                    segments.add(_segment);
                                _prevSegment = _segment;
/*
                                if (_overallUp) {
                                    _gotPreviousMinimum = true;
                                    _gotPreviousMaximum = false;
                                } else if (_overallDn) {
                                    _gotPreviousMinimum = false;
                                    _gotPreviousMaximum = true;
                                }

 */
//                                _gotPreviousMinimum = false;
//                                _gotPreviousMaximum = false;
                                if (_overallUp)
                                    _gotPreviousMinimum = true;
                                if (_overallDn)
                                    _gotPreviousMaximum = true;
                                if (_overallUp || _overallDn) {
                                }
                            }

                        if (CHECK_FLAT_SEGMENTS) {
                            checkFlatSegment = false;
                            _sumLastAltitudes = -_lastAltitudeDiff_m;
                        }
                    }
                    _segRecalc = false;

                    // start with new segment
//                    segment.startIndex = ptIndex;
                    if (_segment.segmentType != Segment.type.SEG_INVALID)
                        _segment = new Segment(_segment);
//                    segment.startDistance_km = start.getDistance();
                }

                if (_altitudeValue_m > 0) {
                    if ((_altitudeValue_m < _minAltitude_m) || (_minAltitude_m <= 0))
                        _minAltitude_m = _altitudeValue_m;
                    else if (_altitudeValue_m > _maxAltitude_m)
                        _maxAltitude_m = _altitudeValue_m;
                }
            }
        }

        return segments;
    }

    public static class SummarySegments {
        public final boolean valid;
        public final double totalDistance_km, totalDistanceClimb_km, totalDistanceDescent_km;
        public final double sum_climb_m, sum_descent_m;
        public final long totalBreakTime_min;
        public final long totalSeconds;

        public SummarySegments() {
            this.valid = false;
            this.totalDistance_km = 0;
            this.totalDistanceClimb_km = 0;
            this.totalDistanceDescent_km = 0;
            this.sum_climb_m = 0;
            this.sum_descent_m = 0;
            this.totalSeconds = 0;
            this.totalBreakTime_min = 0;
        }
        public SummarySegments(double totalDistance_km, double totalDistanceClimb_km, double totalDistanceDescent_km, double sum_climb_m, double sum_descent_m, long totalSeconds, long totalBreakTime_s) {
            this.valid = true;
            this.totalDistance_km = totalDistance_km;
            this.totalDistanceClimb_km = totalDistanceClimb_km;
            this.totalDistanceDescent_km = totalDistanceDescent_km;
            this.sum_climb_m = sum_climb_m;
            this.sum_descent_m = sum_descent_m;
            this.totalSeconds = totalSeconds;
            this.totalBreakTime_min = totalBreakTime_s / 60L;
        }
    }

    public void calcSegmentsValues(List<Segment> inSegments) {
        summary = updateSegmentsValues(inSegments);
    }

    /**
     * Calculate break and overall times within all segments
     * @param inSegments list of segments
     */
    public SummarySegments updateSegmentsValues(List<Segment> inSegments) {
        long totalBreakTime_s = 0L;
        long totalSeconds = 0L;
        long vertSeconds;
        double gradientThreshold;
        double totalDistance_km = 0.0, totalDistanceClimb_km = 0.0, totalDistanceDescent_km = 0.0;
        double sum_climb_m = 0.0, sum_descent_m = 0.0;

        for (int i = 0; i < inSegments.size(); i++) {
            Segment segment = inSegments.get(i);

            sum_climb_m += segment.climb_m;
            sum_descent_m += segment.descent_m;

            DataPoint start = _track.getPoint(segment.startIndex);
            DataPoint end = _track.getPoint(segment.endIndex);
            long horSeconds = 0;

            if (i == 0) {
                segment.startSeconds = 0;
            }
            else {
                Segment prevSegment = inSegments.get(i - 1);
                segment.startSeconds = prevSegment.startSeconds + prevSegment.totalSeconds;
            }
            if (trackHasTimeStamps)
                segment.totalSeconds = end.getTimestamp().getSecondsSince(start.getTimestamp());
            else
                horSeconds = (long) (segment.distance_km / _horSpeed * 3600.0);

            segment.totalBreakTime_s =
                    calcTotalBreakTimeBetween(segment.startIndex, segment.endIndex);
            totalBreakTime_s += segment.totalBreakTime_s;

            // Calculate moderate/steep type of a segment depending on the gradient threshold for
            // tracks with timestamps
            if (trackHasTimeStamps) {
                switch (segment.segmentType) {
                    case SEG_FLAT:
                        break;
                    case SEG_UP:
                    case SEG_UP_MODERATE:
                    case SEG_UP_STEEP:
                        totalDistanceClimb_km += segment.distance_km;
                        break;
                    case SEG_DOWN:
                    case SEG_DOWN_MODERATE:
                    case SEG_DOWN_STEEP:
                        totalDistanceDescent_km += segment.distance_km;
                        break;
                }
                //                updateSegmentGradient(segment);
            }
            else
                switch (segment.segmentType) {
                    case SEG_FLAT:
                        segment.totalSeconds = horSeconds;
                        break;
                    case SEG_UP:
                    case SEG_UP_MODERATE:
                    case SEG_UP_STEEP:
                        vertSeconds = (long) (segment.climb_m / _speedClimb * 3.6);
                        if (DEBUG) gradientThreshold = _speedClimb /_horSpeed * 100;
                        if (horSeconds > vertSeconds) {
                            segment.totalSeconds = (long) (horSeconds + vertSeconds / 2.0);
                            segment.segmentType = Segment.type.SEG_UP_MODERATE;
                        } else {
                            segment.totalSeconds = (long) (horSeconds / 2.0 + vertSeconds);
                            segment.segmentType = Segment.type.SEG_UP_STEEP;
                        }
                        totalDistanceClimb_km += segment.distance_km;
                        break;
                    case SEG_DOWN:
                    case SEG_DOWN_MODERATE:
                    case SEG_DOWN_STEEP:
                        vertSeconds = (long) (segment.descent_m / _speedDescent * 3.6);
                        if (DEBUG) gradientThreshold = _speedDescent /_horSpeed * 100;
                        if (horSeconds > vertSeconds) {
                            segment.totalSeconds = (long) (horSeconds + vertSeconds / 2.0);
                            segment.segmentType = Segment.type.SEG_DOWN_MODERATE;
                        } else {
                            segment.totalSeconds = (long) (horSeconds / 2.0 + vertSeconds);
                            segment.segmentType = Segment.type.SEG_DOWN_STEEP;
                        }
                        totalDistanceDescent_km += segment.distance_km;
                        break;
            }
            if (!trackHasTimeStamps)
                segment.totalSeconds += segment.totalBreakTime_s;
            totalSeconds += segment.totalSeconds;
            totalDistance_km += segment.distance_km;
        }

        if (DEBUG) {
            if (trackHasTimeStamps) {
                Log.d(TAG,"gradient = " + _segment.gradient);
                double hor_speed = _segment.distance_km / (_segment.totalSeconds - _segment.totalBreakTime_s) * 3600.0;
                Log.d(TAG, _segment.getSegmentType() + ": speed = " + formatDouble(hor_speed) + " km/h");
            }
        }

        return new SummarySegments(totalDistance_km, totalDistanceClimb_km, totalDistanceDescent_km, sum_climb_m, sum_descent_m, totalSeconds, totalBreakTime_s);
    }

    /**
     * Calculate moderate/steep type of a segment depending on the gradient threshold for
     * tracks with timestamps
     *
     * @param inSegment segment
     */
    public void updateSegmentGradient(Segment inSegment) {
        if (trackHasTimeStamps) {
            switch (inSegment.segmentType) {
                case SEG_UP:
                case SEG_UP_MODERATE:
                case SEG_UP_STEEP:
                    if (inSegment.gradient <= gradientThresholdClimb)
                        inSegment.segmentType = Segment.type.SEG_UP_MODERATE;
                    else
                        inSegment.segmentType = Segment.type.SEG_UP_STEEP;
                    break;
                case SEG_DOWN:
                case SEG_DOWN_MODERATE:
                case SEG_DOWN_STEEP:
                    if (inSegment.gradient <= gradientThresholdDesc)
                        inSegment.segmentType = Segment.type.SEG_DOWN_MODERATE;
                    else
                        inSegment.segmentType = Segment.type.SEG_DOWN_STEEP;
                    break;
            }

            if (DEBUG) {
                Log.d(TAG,"gradient = " + inSegment.gradient);
                double hor_speed = inSegment.distance_km / (inSegment.totalSeconds - inSegment.totalBreakTime_s) * 3600.0;
                Log.d(TAG, inSegment.getSegmentType() + ": speed = " + formatDouble(hor_speed) + " km/h");
            }
        }
    }

    private double getPrevDistance(DataPoint currPoint) {
        double radians = DataPoint.calculateRadiansBetween(_track.getPoint(_segment.endIndex), currPoint);
        double distance_km = Distance.convertRadiansToDistance(radians);
       return distance_km;
    }

    protected void calc1(int inIndex) {
        _overallUp = _overallDn = false;

        DataPoint currPoint = _track.getPoint(inIndex);

        // does the current point has a valid altitude?
        _altitudeValue_m = 0.0; // todo check valid altitude
        if (currPoint.hasAltitude()) {
            Altitude altitude = currPoint.getAltitude();
            if (altitude.isValid())
                _altitudeValue_m = altitude.getValue();
        }
        if (_altitudeValue_m > 0) {
            // did the previous point have a valid altitude?
            if (_lastAltitude_m > 0) {
                if (CHECK_FLAT_SEGMENTS)                    _sumLastAltitudes += _lastAltitudeDiff_m;
                double altitudeDiff_m = _altitudeValue_m - _lastAltitude_m;
                _lastAltitudeDiff_m = altitudeDiff_m;
                if (Math.abs(altitudeDiff_m) > maxAltitudeJump_m) {
                    trackHasAltitudeJumps = true;
                    _offsetAltitudeJump += altitudeDiff_m;
                    if (Math.abs(_offsetAltitudeJump) > 100)
                        _offsetAltitudeJump = 0;
                }
            }
            // check jumps in altitude values of GPX file
            if (trackHasAltitudeJumps && FIX_ALTITUDE_JUMPS) {
                // try to fix jumps
                _altitudeValue_m -= _offsetAltitudeJump;
                if (_altitudeValue_m > 2500)
                    _altitudeValue_m = 2500;
                DecimalFormat formatter = new DecimalFormat("#0.0");
                currPoint.setFieldValue(Field.ALTITUDE, formatter.format(_altitudeValue_m), false);
            }

            // Compare with previous value if any
            if (_gotPreviousAltitudeValue) {
                if ((_altitudeValue_m != _previousValue_m)) {
                    // Got an altitude value which is different from the previous one
                    final boolean locallyUp = (_altitudeValue_m > _previousValue_m);
                    _overallUp = _gotPreviousMinimum && (_previousValue_m > _previousExtreme);
                    _overallDn = _gotPreviousMaximum && _previousValue_m < _previousExtreme;
                    final boolean moreThanWiggle = Math.abs(_altitudeValue_m - _previousValue_m) > _wiggleLimit;

                    if (DEBUG) {
                        if (locallyUp) {
                            if ((_altitudeValue_m < _previousMinimum) || (_previousMinimum <= 0)) {
                                _previousMinimum = _altitudeValue_m;
                                _indexPreviousMinimum = inIndex;
                            }
                        } else if (_altitudeValue_m > _previousMaximum) {
                            _previousMaximum = _altitudeValue_m;
                            _indexPreviousMaximum = inIndex;
                        }
                    }

                    // Do we know whether we're going up or down yet?
                    if (!_gotPreviousMinimum && !_gotPreviousMaximum) {
                        // we don't know whether we're going up or down yet - check limit
                        if (moreThanWiggle) {
                            if (locallyUp) {
                                _gotPreviousMinimum = true;
                                if (TEST)
                                    _previousExtreme = _previousMinimum;
//                                _indexPreviousExtreme = _indexPreviousMinimum;
                            }
                            else {
                                _gotPreviousMaximum = true;
                                if (TEST)
                                    _previousExtreme = _previousMaximum;
//                                _indexPreviousExtreme = _indexPreviousMaximum;
                            }
                            _previousExtreme = _previousValue_m;
// todo                            _indexPreviousExtreme = inIndex;
                            _previousValue_m = _altitudeValue_m;
// todo                           _prevAltitude_m = _lastAltitude_m;
                            if (FEATURED)
                                _segment.endIndex = _indexPreviousExtreme;
                            if (CHECK_FLAT_SEGMENTS) {
                                checkFlatSegment = true;
// todo check
                                _segRecalc = true;
                            }
                        }
                        else {
                            _indexPreviousExtreme = inIndex;
                        }
                    } else if (_overallUp) {
                        if (locallyUp) {
                            // we're still going up - do nothing
                            if (CHECK_FLAT_SEGMENTS)
                                if (checkFlatSegment) {
                                    double dist = getPrevDistance(currPoint);
                                    if (dist >= MIN_DISTANCE_FLAT_SEGMENT)
                                        _segRecalc = true;
                                }
                            if (!_segRecalc) {
                                _previousValue_m = _altitudeValue_m;

                                if (FEATURED)
                                    _indexPreviousExtreme = inIndex;
                                if (CHECK_FLAT_SEGMENTS)
                                    _segment.endIndex = inIndex;
                            }
                        }
                        else if (moreThanWiggle)
                            // we're going up but have dropped over a maximum
                            _segRecalc = true;
                        else {
                            if (CHECK_FLAT_SEGMENTS)
                                if (!checkFlatSegment) {
                                    _sumLastAltitudes = -_lastAltitudeDiff_m;
                                    checkFlatSegment = true;
                                }
                        }
                    } else if (_overallDn) {
                        if (locallyUp)
                            if (moreThanWiggle)
                                // we're going down but have climbed up from a minimum
                                _segRecalc = true;
                            else {
                                if (CHECK_FLAT_SEGMENTS)
                                    if (!checkFlatSegment) {
                                        _sumLastAltitudes = -_lastAltitudeDiff_m;
                                        checkFlatSegment = true;
                                    }
                            }
                        else {
                            if (CHECK_FLAT_SEGMENTS) {
                                if (checkFlatSegment)
                                {
                                    double dist = getPrevDistance(currPoint);
                                    if (dist >= MIN_DISTANCE_FLAT_SEGMENT) {
                                        _segRecalc = true;
                                    }
                                }
                            }
                            if (!_segRecalc) {
                                // we're still going down - do nothing
                                _previousValue_m = _altitudeValue_m;

                                if (FEATURED)
                                    _indexPreviousExtreme = inIndex;
                                if (CHECK_FLAT_SEGMENTS)
                                    _segment.endIndex = inIndex;
                            }
                        }
                    }
                    else if (FEATURED) {
                        if (locallyUp) {
                            if (_gotPreviousMinimum)
                            {
                                // we're still going up - do nothing
                                _previousValue_m = _altitudeValue_m;
                                _indexPreviousExtreme = inIndex;
                                _segment.endIndex = inIndex;
                            }
                        }
                        else {
                            if (_gotPreviousMaximum)
                            {
                                // we're still going down - do nothing
                                _previousValue_m = _altitudeValue_m;
                                _indexPreviousExtreme = inIndex;
                                _segment.endIndex = inIndex;
                            }

                        }
                    }
                    else {
                        if (moreThanWiggle)
                        {
                            _previousValue_m = _altitudeValue_m;
                        }
                    }
                }
                else {
                }
            }
            else {
                // we haven't got a previous value at all, so it's the start of a new segment
                _previousValue_m = _altitudeValue_m;
                if (FEATURED) _indexPreviousExtreme = inIndex;
                _gotPreviousAltitudeValue = true;
            }
            _lastAltitude_m = _altitudeValue_m;
        }
    }


    private long calcTotalBreakTimeBetween(int inStart, int inEnd) {
        DataPoint prevPoint = null, currPoint;
        long totalBreakTime_s = 0L;
        if (trackHasTimeStamps) {
            for (int ptIndex = inStart-1; ptIndex >= 0; ptIndex--) {
                currPoint = _track.getPoint(ptIndex);
                if ((currPoint != null) && (!currPoint.isWayPoint())) {
                    prevPoint = currPoint;
                    break;
                }
            }
        }
        for (int ptIndex = inStart; ptIndex <= inEnd; ptIndex++) {
            currPoint = _track.getPoint(ptIndex);
            if ((currPoint != null) && (!currPoint.isWayPoint())) {
                if (trackHasTimeStamps) {
                    if (prevPoint != null) {
                        // Calculate the distance to the previous trackpoint
                        double distance_km = currPoint.getDistance() - prevPoint.getDistance();
                        // Calculate the time difference to the previous trackpoint
                        long elapsedTime = currPoint.getTimestamp().getSecondsSince(prevPoint.getTimestamp());
                        if (elapsedTime > 0) {
                            double speed = distance_km / (double) elapsedTime * 3600.0;
                            if (speed < MIN_HOR_SPEED) {
                                // regard as break
                                totalBreakTime_s += elapsedTime;
                            }
                        }
                        prevPoint = currPoint;
                    } else if (!currPoint.isWayPoint())
                        prevPoint = currPoint;
                }
                else {
                    // get break time from track point in GPX file
                    long breakTime_s = currPoint.getWaypointDuration() * 60L;
                    totalBreakTime_s += breakTime_s;
                }
            }
        }
        return totalBreakTime_s;
    }

    private void updateSegment(int inIndex) {
        if (_segment.startIndex == _segment.endIndex)
            _segment.endIndex = inIndex;
        DataPoint start = _track.getPoint(_segment.startIndex);
        DataPoint end = _track.getPoint(_segment.endIndex);
        _segment.distance_km = end.getDistance() - start.getDistance();

//        if (DEBUG)            segment.altitudeDiff = end.getAltitude().getValue() - start.getAltitude().getValue();

        // calculate section times
//        if (_segment.distance_km > 0.05) {
            if (_overallUp) {
                if (_segment.distance_km > 0.05)
                    _segment.segmentType = Segment.type.SEG_UP;
                // Add the climb from _previousExtreme up to _previousValue
                _segment.climb_m = _previousValue_m - _previousExtreme;
                _segment.gradient = (int)(_segment.climb_m / _segment.distance_km) / 10;
                _previousExtreme = _previousValue_m;
                _gotPreviousMinimum = false;
                _gotPreviousMaximum = true;
// todo
                if (_altitudeValue_m > 0) _previousValue_m = _altitudeValue_m;
            } else if (_overallDn) {
                if (_segment.distance_km > 0.05)
                    _segment.segmentType = Segment.type.SEG_DOWN;
                // Add the descent from _previousExtreme down to _previousValue
                _segment.descent_m = _previousExtreme - _previousValue_m;
                _segment.gradient = (int)(_segment.descent_m / _segment.distance_km) / 10;
                _previousExtreme = _previousValue_m;
                _gotPreviousMinimum = true;
                _gotPreviousMaximum = false;
// todo
                if (_altitudeValue_m > 0) _previousValue_m = _altitudeValue_m;
            } else {
                if (_segment.distance_km > 0.05)
                    _segment.segmentType = Segment.type.SEG_FLAT;
                /* todo check
                _gotPreviousMinimum = false;
                _gotPreviousMaximum = false;

                 */
// todo                if (_altitude_m > 0)                    _prevAltitude_m = _altitude_m;
            }

//        else            _segment.distance_km = 0;
// todo        _previousMinimum = -1; _previousMaximum = -1;
// todo        _indexPreviousMinimum = -1; _indexPreviousMaximum = -1;
    }

    /**
     * set hiking parameters:
     *
     * @param horSpeed      horizontal part in [km/h]
     * @param speedClimb    ascending part in [km/h]
     * @param speedDescent  descending part in [km/h]
     * @param toleranceMetres  min. required change of altitude in metres
     */
    public void setHikingParameters(double horSpeed, double speedClimb, double speedDescent, double toleranceMetres) {
        if (horSpeed > 0)
            _horSpeed = horSpeed;
        if (speedClimb > 0)
            _speedClimb = speedClimb;
        if (speedDescent > 0)
            _speedDescent = speedDescent;
        if (toleranceMetres > 0)
            _wiggleLimit = toleranceMetres;
    }

    public void setHikingParametersFrom(TrackSegments fromOther) {
        _horSpeed = fromOther._horSpeed;
        _speedClimb = fromOther._speedClimb;
        _speedDescent = fromOther._speedDescent;
        _wiggleLimit = fromOther._wiggleLimit;
    }

    public long calcTotalTimeFromSegments(List<Segment> inSegments) {
        long total_Seconds = 0L;
        /* Analyze segments */
        for (int i = 0; i < inSegments.size(); i++) {
            Segment segment = inSegments.get(i);
            long horSeconds = (long) (segment.distance_km / _horSpeed * 3600.0);
            long vertSeconds = 0;
            long totalSeconds = 0L;

            switch (segment.segmentType) {
                case SEG_FLAT:
                    totalSeconds = horSeconds;
                    break;
                case SEG_UP_MODERATE:
                    vertSeconds = (long) (segment.climb_m / _speedClimb * 3.6);
                    totalSeconds = horSeconds + vertSeconds / 2;
                    break;
                case SEG_UP_STEEP:
                    vertSeconds = (long) (segment.climb_m / _speedClimb * 3.6);
                    totalSeconds = horSeconds / 2 + vertSeconds;
                    break;
                case SEG_DOWN_MODERATE:
                    vertSeconds = (long) (segment.descent_m / _speedDescent * 3.6);
                    totalSeconds = horSeconds + vertSeconds / 2;
                    break;
                case SEG_DOWN_STEEP:
                    vertSeconds = (long) (segment.descent_m / _speedDescent * 3.6);
                    totalSeconds = horSeconds / 2 + vertSeconds;
                    break;
            }
            total_Seconds += totalSeconds;
        }
        return total_Seconds;
    }

    public long getTotalSeconds() { if (summary == null) return 0; else return summary.totalSeconds; }

    public static double getMinAltitude() {
        return _minAltitude_m;
    }

    public static double getMaxAltitude() {
        return _maxAltitude_m;
    }


    public static String formatDouble (double inValue) {
        DecimalFormat formatter = new DecimalFormat("#0.00");
        return formatter.format(inValue);
    }


}
