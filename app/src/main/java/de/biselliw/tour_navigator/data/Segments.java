package de.biselliw.tour_navigator.data;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import tim.prune.data.Altitude;
import tim.prune.data.Distance;

public class Segments {
    /**
     * TAG for log messages.
     */
    static final String TAG = "Segments";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    // hiking speed parameters

    public final static double DEF_HOR_SPEED 			= 4.5;
    public final static double DEF_VERT_SPEED_CLIMB 	= 0.35;
    public final static double DEF_VERT_SPEED_DESC 		= 0.5;
    public final static int    DEF_MIN_HEIGHT_CHANGE 	= 15;

    final static double _secondsHour 			= 3600.0;

    /** horizontal part in [km/h] */
    private static double _horSpeed = 			DEF_HOR_SPEED;
    /** climbing part in [km/h] */
    private static double _vertSpeedClimb = 	DEF_VERT_SPEED_CLIMB;
    /** descending part in [km/h] */
    private static double _vertSpeedDescent = 	DEF_VERT_SPEED_DESC;
    /** hysteresis value for a change of altitude */
    private static double _minHeightChange = 	DEF_MIN_HEIGHT_CHANGE;

    List<Segment> segments = null;


    private DataPoint _prevPoint = null;

    private double _minAltitude_m = 0, _maxAltitude_m = 0;
    private double _totalDistance_km = 0.0;
    private double _totalClimb_m = 0, _totalDescent_m = 0;
    private long _totalSeconds = 0L;

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
    public List<Segment> calcSegments(TrackDetails inTrack) {

        /** if true: data must be recalculated in current segment of the track */
        boolean _segRecalc = false;

        segments = new ArrayList<>();
        Segment segment = new Segment();

        double _vertSecondsHour 		= 3.6;

        /** Flags for whether minimum or maximum has been found */
        boolean _gotPreviousMinimum = false, _gotPreviousMaximum = false;

        double  _previousExtreme = 0, _prevAltitude_m = -1;


        int _numPoints = inTrack.getNumPoints();
        if (_numPoints <= 0) return null;

        if (DEBUG) {
            Log.d(TAG, "calcSegments(): " + _numPoints + " Trackpoints");
        }

        for (int ptIndex = 0; ptIndex < _numPoints; ptIndex++) {
            DataPoint currPoint = inTrack.getPoint(ptIndex);

            boolean overallUp = false, overallDn = false;
            double altitude_m = 0.0;
            boolean finish = (ptIndex >= _numPoints - 1);

            if ((currPoint != null) && !currPoint.isWayPoint()) {
                // does the current point has a valid altitude?
                if (currPoint.hasAltitude()) {
                    Altitude altitude = currPoint.getAltitude();
                    if (altitude.isValid())
                        altitude_m = altitude.getValue();
                }
                if (altitude_m > 0) {
                    // did the previous point have a valid altitude?
                    if (_prevAltitude_m >= 0) {
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
                            }
                        } else if (overallUp) {
                            if (segClimbing)
                                // we're still going up - do nothing
                                _prevAltitude_m = altitude_m;
                            else if (moreThanWiggle)
                                // we're going up but have dropped over a maximum
                                _segRecalc = true;
                        } else if (overallDn) {
                            if (segClimbing) {
                                if (moreThanWiggle)
                                    // we're going down but have climbed up from a minimum
                                    _segRecalc = true;
                            } else
                                // we're still going down - do nothing
                                _prevAltitude_m = altitude_m;
                        }
                    } else
                        // we haven't got a previous value at all, so it's the start of a new segment
                        _prevAltitude_m = altitude_m;
                }

                // Calculate the distance to the previous trackpoint
                if (_prevPoint != null) {
                    double radians = DataPoint.calculateRadiansBetween(_prevPoint, currPoint);
                    double dist = Distance.convertRadiansToDistance(radians);
                    _totalDistance_km += dist;
                    currPoint.setDistance(_totalDistance_km);
                    segment.distance_km += dist;
                } else
                    currPoint.setDistance(0.0);

                // remember previous  point
                _prevPoint = currPoint;
            }

            // need to calculate time?
            if (_segRecalc || finish) {
                // calculate section times
                segment.horSeconds = (long) (segment.distance_km / _horSpeed * _secondsHour);
                if (overallUp) {
                    // Add the climb from _previousExtreme up to _previousValue
                    segment.climb_m = _prevAltitude_m - _previousExtreme;
                    _previousExtreme = _prevAltitude_m;
                    _gotPreviousMinimum = false;
                    _gotPreviousMaximum = true;
                    if (altitude_m > 0)
                        _prevAltitude_m = altitude_m;
                    _totalClimb_m = segment.startClimb_m + segment.climb_m;
                    segment.vertSeconds = (long) (segment.climb_m / _vertSpeedClimb * _vertSecondsHour);
                } else if (overallDn) {
                    // Add the descent from _previousExtreme down to _previousValue
                    segment.descent_m = _previousExtreme - _prevAltitude_m;
                    _previousExtreme = _prevAltitude_m;
                    _gotPreviousMinimum = true;
                    _gotPreviousMaximum = false;
                    if (altitude_m > 0)
                        _prevAltitude_m = altitude_m;
                    _totalDescent_m = segment.startDescent_m + segment.descent_m;
                    segment.vertSeconds = (long) (segment.descent_m / _vertSpeedDescent * _vertSecondsHour);
                } else {
                    segment.vertSeconds = 0;
                    segment.segmentType = Segment.segment_type.SEG_FLAT;
                }

                if (segment.horSeconds > segment.vertSeconds) {
                    segment.totalSeconds = (long) (segment.horSeconds + segment.vertSeconds / 2.0);
                    if (overallUp)
                        segment.segmentType = Segment.segment_type.SEG_UP_MODERATE;
                    else
                        segment.segmentType = Segment.segment_type.SEG_DOWN_MODERATE;
                } else {
                    segment.totalSeconds = (long) (segment.horSeconds / 2.0 + segment.vertSeconds);
                    if (overallUp)
                        segment.segmentType = Segment.segment_type.SEG_UP_STEEP;
                    else
                        segment.segmentType = Segment.segment_type.SEG_DOWN_STEEP;
                }
                _totalSeconds += segment.totalSeconds;

                segment.endIndex = ptIndex;

                // update overall data at the end of the segment from section data
                segments.add(segment);
                _segRecalc = false;

                if (!finish) {
                    // start with new segment
                    segment = new Segment();
                    segment.startIndex = ptIndex;
                    segment.startDistance_km = _totalDistance_km;
                    segment.startClimb_m = _totalClimb_m;
                    segment.startDescent_m = _totalDescent_m;
                    segment.startSeconds = _totalSeconds;
                }

                if (altitude_m > 0) {
                    if ((altitude_m < _minAltitude_m) || (_minAltitude_m <= 0))
                        _minAltitude_m = altitude_m;
                    else if (altitude_m > _maxAltitude_m)
                        _maxAltitude_m = altitude_m;
                }
            }
        }
        return segments;
    }

    public double getMinAltitude() { return _minAltitude_m; }
    public double getMaxAltitude() { return _maxAltitude_m; }
    public double getTotalDistance () {	return _totalDistance_km;}
    public double getTotalClimb () { return _totalClimb_m; }
    public double getTotalDescent () { return _totalDescent_m; }
    public long getTotalSeconds () { return _totalSeconds; }

}


