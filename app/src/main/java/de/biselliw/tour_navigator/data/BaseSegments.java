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
public class BaseSegments {
    // hiking speed parameters
    /**
     * TAG for log messages.
     */
    static final String TAG = "BaseSegments";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public static BaseSegments baseSegments = null;
    public final static double DEF_HOR_SPEED = 4.5;
    public final static double DEF_VERT_SPEED_CLIMB = 0.35;
    public final static double DEF_VERT_SPEED_DESC = 0.5;
    /** Tolerance value in metres */
    public final static int DEF_MIN_HEIGHT_CHANGE = 15;
    public final static double MIN_HOR_SPEED = 0.1 * DEF_HOR_SPEED;

    /**
     * horizontal part in [km/h]
     */
    protected double _horSpeed = DEF_HOR_SPEED;
    /**
     * climbing part in [km/h]
     */
    protected static double _vertSpeedClimb = DEF_VERT_SPEED_CLIMB;
    /**
     * descending part in [km/h]
     */
    protected static double _vertSpeedDescent = DEF_VERT_SPEED_DESC;
    /**
     * hysteresis value for a change of altitude
     */
    protected static double _minHeightChange = DEF_MIN_HEIGHT_CHANGE;
    protected TrackDetails _track =null;

    protected boolean trackHasTimeStamps = false;
    protected final double maxAltitudeJump_m = 25.0;
    protected boolean trackHasAltitudeJumps = false;
    protected boolean fixAltitudeJumps = false;
    private double _offsetAltitudeJump = 0.0;

    protected List<Segment> segments = null;

    protected DataPoint _prevPoint = null;

    /** Flag for whether previous altitude value exists or not */
    private boolean _gotPreviousValue = false;
    protected double altitude_m = 0.0,
    /** Value of previous minimum or maximum, if any */
    _previousExtreme = 0,
    /** Previous metric value */
    _prevAltitude_m = -1, _lastAltitude_m = -1;
    protected double _minAltitude_m = 0, _maxAltitude_m = 0;
    protected double _totalDistance_km = 0.0, _totalDistanceClimb_km = 0.0, _totalDistanceDescent_km = 0.0;
    protected double _totalClimb_m = 0, _totalDescent_m = 0;
    protected long _totalSeconds = 0L;

    protected boolean overallUp = false, overallDn = false;

    /** Flags for whether minimum or maximum has been found */
    protected boolean _gotPreviousMinimum = false, _gotPreviousMaximum = false;
    /** Flag for whether previous value exists or not */

    protected long _totalBreakTime_s = 0L;

    protected Segment segment = null;
    protected Segment upDownSegment = null, flatSegment = null;
    boolean checkFlatSegment = false;

    /** if true: data must be recalculated in current segment of the track */
    protected boolean _segRecalc = false;

    /**
     * Recalculate all track points
     */
    public List<Segment> calcSegments(TrackDetails inTrack) {

        _track = inTrack;
        trackHasAltitudeJumps = false; _offsetAltitudeJump = 0.0;
        baseSegments = this;
        boolean checkFlatSegments = true;

        /** if true: data must be recalculated in current segment of the track */
        _segRecalc = false;

        _gotPreviousValue = false;

        segments = new ArrayList<>();
        // create a new segment, start with up/down movement
        upDownSegment = new Segment();
        segment = upDownSegment;

        int _numPoints = inTrack.getNumPoints();
        if (_numPoints <= 0) return null;

        if (DEBUG) {
            Log.d(TAG, "calcSegments(): " + _numPoints + " Trackpoints");
        }

        for (int ptIndex = 0; ptIndex < _numPoints; ptIndex++) {
            DataPoint currPoint = inTrack.getPoint(ptIndex);

            boolean finish = (ptIndex >= _numPoints - 1);

            if ((currPoint != null) && !currPoint.isWayPoint()) {
                calc1(ptIndex);

                if (checkFlatSegment && (flatSegment == null) && checkFlatSegments)
                {
                    // assume start of a flat segment
                    flatSegment = new Segment();
                    segment = flatSegment;
                    segment.segmentType = Segment.segment_type.SEG_FLAT;
                    segment.startIndex = ptIndex;
                    segment.startDistance_km = _totalDistance_km;
                    segment.startSeconds = _totalSeconds;
                }
                calc3(currPoint);
            }

            // need to calculate time?
            if (_segRecalc || finish) {
                calc2(ptIndex);

                if (flatSegment != null) {
                    flatSegment.startIndex = upDownSegment.endIndex;
                    flatSegment.endIndex = ptIndex;
                    flatSegment.startDistance_km = upDownSegment.startDistance_km + upDownSegment.distance_km;
                    flatSegment.startClimb_m = _totalClimb_m;
                    flatSegment.startDescent_m = _totalDescent_m;
                    flatSegment.startSeconds = upDownSegment.startSeconds + upDownSegment.totalSeconds;
                    flatSegment.distance_km = _track.getPoint(flatSegment.endIndex).getDistance() -
                            _track.getPoint(flatSegment.startIndex).getDistance();
                    flatSegment.horSeconds = (long) (flatSegment.distance_km / _horSpeed * 3600.0);
                    flatSegment.totalSeconds = flatSegment.horSeconds;

                    if (trackHasTimeStamps)
                        upDownSegment.totalSeconds = inTrack.getPoint(upDownSegment.endIndex).getTimestamp().
                                getSecondsSince(inTrack.getPoint(upDownSegment.startIndex).getTimestamp());
                    _totalSeconds += upDownSegment.totalSeconds;
                    // update overall data at the end of the segment from section data
                    segments.add(upDownSegment);
                    segment = flatSegment;
                }
                else
                    segment.endIndex = ptIndex;

                if (segment.distance_km > 0) {
                    if (trackHasTimeStamps)
                        segment.totalSeconds = currPoint.getTimestamp().getSecondsSince(
                                inTrack.getPoint(segment.startIndex).getTimestamp());
                    _totalSeconds += segment.totalSeconds;

                    // update overall data at the end of the segment from section data
                    segments.add(segment);
                }
                _segRecalc = false;
                flatSegment = null; upDownSegment = null;

                if (!finish) {
                    // start with new segment
                    segment = new Segment();
                    segment.startIndex = ptIndex;
                    segment.startDistance_km = _totalDistance_km;
                    segment.startClimb_m = _totalClimb_m;
                    segment.startDescent_m = _totalDescent_m;
                    segment.startSeconds = _totalSeconds;
                    upDownSegment = segment;
                }
            }
            if (altitude_m > 0) {
                if ((altitude_m < _minAltitude_m) || (_minAltitude_m <= 0))
                    _minAltitude_m = altitude_m;
                else if (altitude_m > _maxAltitude_m)
                    _maxAltitude_m = altitude_m;
            }
        }
        return segments;
    }


    protected void calc1(int inIndex) {
        overallUp = false;
        overallDn = false;
        checkFlatSegment = false;

        DataPoint currPoint = _track.getPoint(inIndex);

        // does the current point has a valid altitude?
        altitude_m = 0.0; // todo check valid altitude
        if (currPoint.hasAltitude()) {
            Altitude altitude = currPoint.getAltitude();
            if (altitude.isValid())
                altitude_m = altitude.getValue();
        }
        if (altitude_m > 0) {
            // did the previous point have a valid altitude?
            if (_lastAltitude_m > 0) {
                final double altitudeDiff_m = Math.abs(altitude_m - _lastAltitude_m);
                if (altitudeDiff_m > maxAltitudeJump_m) {
                    trackHasAltitudeJumps = true;
                    _offsetAltitudeJump += altitude_m - _lastAltitude_m;
                }
            }
            // check jumps in altitude values of GPX file
            _lastAltitude_m = altitude_m;
            if (trackHasAltitudeJumps && fixAltitudeJumps) {
                // try to fix jumps
                altitude_m -= _offsetAltitudeJump;
                DecimalFormat formatter = new DecimalFormat("#0.0");
                currPoint.setFieldValue(Field.ALTITUDE, formatter.format(altitude_m), false);
            }
            if (_gotPreviousValue) {
                if ((altitude_m != _prevAltitude_m)) {
                    // Got an altitude value which is different from the previous one
                    boolean segClimbing = (altitude_m > _prevAltitude_m);
                    overallUp = _gotPreviousMinimum && (_prevAltitude_m > _previousExtreme);
                    overallDn = _gotPreviousMaximum && _prevAltitude_m < _previousExtreme;
                    final boolean moreThanWiggle = Math.abs(altitude_m - _prevAltitude_m) > _minHeightChange;

                    // Do we know whether we're going up or down yet?
                    if (!_gotPreviousMinimum && !_gotPreviousMaximum) {
                        // we don't know whether we're going up or down yet - check limit
                        if (moreThanWiggle) {
                            if (segClimbing)
                                _gotPreviousMinimum = true;
                            else
                                _gotPreviousMaximum = true;
                            _previousExtreme = _prevAltitude_m;
                            _prevAltitude_m = altitude_m;
                            _segRecalc = true; // todo check
                        }
                    } else if (overallUp) {
                        if (segClimbing) {
                            // we're still going up - do nothing
                            upDownSegment.endIndex = inIndex;
                            _prevAltitude_m = altitude_m;
                        }
                        else if (moreThanWiggle)
                            // we're going up but have dropped over a maximum
                            _segRecalc = true;
                        else
                            checkFlatSegment = true;
                    } else if (overallDn) {
                        if (segClimbing)
                            if (moreThanWiggle)
                                // we're going down but have climbed up from a minimum
                                _segRecalc = true;
                            else
                                checkFlatSegment = true;
                        else {
                            // we're still going down - do nothing
                            _prevAltitude_m = altitude_m;
                            upDownSegment.endIndex = inIndex;
                        }
                    }
                }
            }
            else {
                // we haven't got a previous value at all, so it's the start of a new segment
                _prevAltitude_m = altitude_m;
                _gotPreviousValue = true;
            }
        }
    }

    private void calc3(DataPoint currPoint) {
        // Calculate the distance to the previous trackpoint
        if (_prevPoint != null) {
            double radians = DataPoint.calculateRadiansBetween(_prevPoint, currPoint);
            double dist = Distance.convertRadiansToDistance(radians);
            _totalDistance_km += dist;
            currPoint.setDistance(_totalDistance_km);
            segment.distance_km += dist;

            // Calculate the time difference to the previous trackpoint
            if (trackHasTimeStamps) {
                long elapsedTime = currPoint.getTimestamp().getSecondsSince(_prevPoint.getTimestamp());
                if (elapsedTime > 0) {
                    double speed = dist / (double) elapsedTime * 3600.0;
                    if (speed < MIN_HOR_SPEED) {
                        // regard as break
                        segment.totalBreakTime_s += elapsedTime;
                        _totalBreakTime_s += elapsedTime;
                    }
                }
            }
            else {
                // get break time from track point in GPX file
                long breakTime_s = currPoint.getWaypointDuration() * 60L;
                segment.totalBreakTime_s += breakTime_s;
                _totalBreakTime_s += breakTime_s;
            }
        } else
            currPoint.setDistance(0.0);

        // remember previous  point
        _prevPoint = currPoint;
    }

    protected void calc2(int inIndex) {
        // calculate section times
        segment.horSeconds = (long) (segment.distance_km / _horSpeed * 3600.0);
        segment.totalSeconds = segment.horSeconds;
        if (overallUp || overallDn) {
            if (flatSegment != null) {
                segment = upDownSegment;
                if (segment.endIndex == 0)
                    segment.endIndex = inIndex;
                segment.distance_km = _track.getPoint(segment.endIndex).getDistance() -
                        _track.getPoint(segment.startIndex).getDistance();
                segment.horSeconds = (long) (segment.distance_km / _horSpeed * 3600.0);
            }
        }
        if (overallUp) {
            // Add the climb from _previousExtreme up to _previousValue
            segment.climb_m = _prevAltitude_m - _previousExtreme;
            _totalDistanceClimb_km += segment.distance_km;
            _previousExtreme = _prevAltitude_m;
            _gotPreviousMinimum = false;
            _gotPreviousMaximum = true;
            if (altitude_m > 0)
                _prevAltitude_m = altitude_m;
            _totalClimb_m = segment.startClimb_m + segment.climb_m;
            segment.vertSeconds = (long) (segment.climb_m / _vertSpeedClimb * 3.6);
        } else if (overallDn) {
            // Add the descent from _previousExtreme down to _previousValue
            segment.descent_m = _previousExtreme - _prevAltitude_m;
            _totalDistanceDescent_km += segment.distance_km;
            _previousExtreme = _prevAltitude_m;
            _gotPreviousMinimum = true;
            _gotPreviousMaximum = false;
            if (altitude_m > 0)
                _prevAltitude_m = altitude_m;
            _totalDescent_m = segment.startDescent_m + segment.descent_m;
            segment.vertSeconds = (long) (segment.descent_m / _vertSpeedDescent * 3.6);
        } else {
            segment.vertSeconds = 0;
            segment.segmentType = Segment.segment_type.SEG_FLAT;

            /* todo check
            _gotPreviousMinimum = false;
            _gotPreviousMaximum = false;
            if (altitude_m > 0)
                _prevAltitude_m = altitude_m;

             */
        }

        if (overallUp || overallDn) {
            if (segment.horSeconds > segment.vertSeconds) {
                if (!trackHasTimeStamps)
                    segment.totalSeconds = (long) (segment.horSeconds + segment.vertSeconds / 2.0);
                if (overallUp)
                    segment.segmentType = Segment.segment_type.SEG_UP_MODERATE;
                else
                    segment.segmentType = Segment.segment_type.SEG_DOWN_MODERATE;
            } else {
                if (!trackHasTimeStamps)
                    segment.totalSeconds = (long) (segment.horSeconds / 2.0 + segment.vertSeconds);
                if (overallUp)
                    segment.segmentType = Segment.segment_type.SEG_UP_STEEP;
                else
                    segment.segmentType = Segment.segment_type.SEG_DOWN_STEEP;
            }
        }
    }

    public long getTotalBreakTime_min() { return _totalBreakTime_s / 60L; }

    public double getMinAltitude() {
        return _minAltitude_m;
    }

    public double getMaxAltitude() {
        return _maxAltitude_m;
    }

    public double getTotalDistance() {
        return _totalDistance_km;
    }

    public double getTotalClimb() {
        return _totalClimb_m;
    }

    public double getTotalDescent() {
        return _totalDescent_m;
    }

    public long getTotalSeconds() {
        return _totalSeconds;
    }

}
