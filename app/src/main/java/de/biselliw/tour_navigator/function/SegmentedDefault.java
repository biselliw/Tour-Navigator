package de.biselliw.tour_navigator.function;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.data.Segment;
import de.biselliw.tour_navigator.helpers.Log;

import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.INVALID_INDEX;
import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.INVALID_VALUE;

public class SegmentedDefault {

    /**
     * TAG for log messages.
     */
    static final String TAG = "SegmentedDefault";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;


    private double _wiggleLimit;
    private static final boolean TEST = false;
    private static final boolean FEATURED = false;
    private static final boolean CHECK_FLAT_SEGMENTS = true;
    private static final double MIN_DISTANCE_FLAT_SEGMENT = 0.25;
    boolean checkFlatSegment = false;

    private boolean _overallUp, _overallDn;

    /**
     * Flags for whether minimum or maximum has been found
     */
    private boolean _gotPreviousMinimum, _gotPreviousMaximum;
    private double _previousMinimum = -1, _previousMaximum = -1;
    int _indexPreviousMinimum, _indexPreviousMaximum;

    /**
     * Flag for whether previous altitude value exists or not
     */
    private double _altitudeValue,
    /**
     * Value of previous minimum or maximum, if any
     */
    _previousExtreme,
    /**
     * Previous metric value
     */
    _previousValue_m, _lastAltitude_m;
    double _lastAltitudeDiff_m, _sumLastAltitudes = 0;
    boolean _gotPreviousAltitudeValue;
    int _indexPreviousExtreme;

    private List<TrackPoint> _trackPoints = null;
    TrackPoint _prevPoint;

    private Segment _segment = null, _prevSegment;

    /**
     * if true: data must be recalculated in current segment of the track
     */
    protected boolean _segRecalc = false;

    private void clear() {
        _prevSegment = null;
        _gotPreviousAltitudeValue = false;
        _previousValue_m = 0;

        /* Flags for whether minimum or maximum has been found */
        _gotPreviousMinimum = _gotPreviousMaximum = false;
        _previousExtreme = 0;

        _lastAltitude_m = INVALID_VALUE;
        if (CHECK_FLAT_SEGMENTS) {
            checkFlatSegment = false;
            _lastAltitudeDiff_m = 0.0;
            _sumLastAltitudes = 0.0;
        }

        _previousMinimum = _previousMaximum = INVALID_VALUE;
        if (DEBUG) {
            _indexPreviousMinimum = _indexPreviousMaximum = INVALID_INDEX;
        }
        if (FEATURED)
            _indexPreviousExtreme = INVALID_INDEX;

        _prevPoint = null;

        /* if true: data must be recalculated in current segment of the track */
        _segRecalc = false;
    }

    /**
     * Analyze all track points and divide the track into segments
     *
     * @param inTrackPoints list of simple track points
     * @return list of simplified track segments
     */
    public List<Segment> calcSegments(List<TrackPoint> inTrackPoints, double inWiggleLimit) {
        _wiggleLimit = inWiggleLimit;
        _trackPoints = inTrackPoints;
        if (inTrackPoints == null) return null;

        clear();
        double totalDistance_km = 0.0;
        List<Segment> segments = new ArrayList<>();
        // create a new segment, start with up/down movement
        _segment = new Segment();

        int numPoints = inTrackPoints.size();
        if (DEBUG) Log.d(TAG, "calcSegments(): " + numPoints + " Trackpoints");

        boolean finish = false;
        for (int ptIndex = 0; ptIndex < numPoints; ptIndex++) {
            TrackPoint currPoint = inTrackPoints.get(ptIndex);

            if (ptIndex >= numPoints - 1)
                finish = true;

            analyzeProfile(ptIndex);

            if (finish)
                _segment.setEndIndex(ptIndex);
            else
                _prevPoint = currPoint;

            // need to calculate time?
            if (_segRecalc || finish) {
                updateSegment(ptIndex, finish);

                if (_segment.segmentType != Segment.type.SEG_INVALID) {
                    if (_prevSegment != null && _prevSegment.segmentType == Segment.type.SEG_FLAT && _segment.segmentType == Segment.type.SEG_FLAT)
                        _prevSegment.deltaX += _segment.deltaX;
                    else
//                            if (_segment.distance_km >= MIN_DISTANCE_FLAT_SEGMENT)
                        segments.add(_segment);
                    _prevSegment = _segment;
                }

                if (finish || (checkFlatSegment && CHECK_FLAT_SEGMENTS)) {
                    TrackPoint start = inTrackPoints.get(
                            (_segment.segmentType == Segment.type.SEG_INVALID) ?
                                    _segment.getStartIndex() : _segment.getEndIndex());
                    TrackPoint end = (finish ? _prevPoint : currPoint);
                    double deltaX = end.distance - start.distance; // segment.startDistance_km - segment.distance_km;
                    if (deltaX > 0)
                        if ((deltaX >= MIN_DISTANCE_FLAT_SEGMENT) || finish) {
                            // start with new segment
                            _segment = new Segment(_segment);
                            _segment.segmentType = Segment.type.SEG_FLAT;
                            _segment.elevation = end.elevation;
                            _segment.setEndIndex(ptIndex);
                            _segment.deltaX = deltaX;

                            // update overall data at the end of the segment from section data
                            if (_prevSegment != null && _prevSegment.segmentType == Segment.type.SEG_FLAT && _segment.segmentType == Segment.type.SEG_FLAT)
                                _prevSegment.deltaX += _segment.deltaX;
                            else
                                segments.add(_segment);
                            _prevSegment = _segment;
                            if (_overallUp)
                                _gotPreviousMinimum = true;
                            if (_overallDn)
                                _gotPreviousMaximum = true;
                        }

                    if (finish && _segment.getEndIndex() < ptIndex)
                        _segment.setEndIndex(ptIndex);

                    if (CHECK_FLAT_SEGMENTS) {
                        checkFlatSegment = false;
                        _sumLastAltitudes = -_lastAltitudeDiff_m;
                    }
                }
                _segRecalc = false;

                // start with new segment
                if (_segment.segmentType != Segment.type.SEG_INVALID)
                    _segment = new Segment(_segment);
            }
        }

        return segments;
    }


    private double getPrevDistance(TrackPoint currPoint) {
        return currPoint.distance - _trackPoints.get(_segment.getEndIndex()).distance;
    }

    private void analyzeProfile(int inIndex) {
        _overallUp = _overallDn = false;

        TrackPoint currPoint = _trackPoints.get(inIndex);

        // did the previous point have a valid altitude?
        _altitudeValue = currPoint.elevation;
        if (_lastAltitude_m != INVALID_VALUE) {
            if (CHECK_FLAT_SEGMENTS) _sumLastAltitudes += _lastAltitudeDiff_m;
            double altitudeDiff_m = _altitudeValue - _lastAltitude_m;
            _lastAltitudeDiff_m = altitudeDiff_m;
        }

        // Compare with previous value if any
        if (_gotPreviousAltitudeValue) {
            if ((_altitudeValue != _previousValue_m)) {
                // Got an altitude value which is different from the previous one
                final boolean locallyUp = (_altitudeValue > _previousValue_m);
                _overallUp = _gotPreviousMinimum && (_previousValue_m > _previousExtreme);
                _overallDn = _gotPreviousMaximum && _previousValue_m < _previousExtreme;
                final boolean moreThanWiggle = Math.abs(_altitudeValue - _previousValue_m) > _wiggleLimit;

                if (DEBUG) {
                    if (locallyUp) {
                        if ((_altitudeValue < _previousMinimum) || (_previousMinimum <= 0)) {
                            _previousMinimum = _altitudeValue;
                            _indexPreviousMinimum = inIndex;
                        }
                    } else if (_altitudeValue > _previousMaximum) {
                        _previousMaximum = _altitudeValue;
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
                        } else {
                            _gotPreviousMaximum = true;
                            if (TEST)
                                _previousExtreme = _previousMaximum;
//                                _indexPreviousExtreme = _indexPreviousMaximum;
                        }
                        _previousExtreme = _previousValue_m;
// todo                            _indexPreviousExtreme = inIndex;
                        _previousValue_m = _altitudeValue;
// todo                           _prevAltitude_m = _lastAltitude_m;
                        if (FEATURED)
                            _segment.setEndIndex(_indexPreviousExtreme);
                        if (CHECK_FLAT_SEGMENTS) {
                            checkFlatSegment = true;
// todo check
                            _segRecalc = true;
                        }
                    } else {
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
                            _previousValue_m = _altitudeValue;

                            if (FEATURED)
                                _indexPreviousExtreme = inIndex;
                            if (CHECK_FLAT_SEGMENTS)
                                _segment.setEndIndex(inIndex);
                        }
                    } else if (moreThanWiggle)
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
                            if (checkFlatSegment) {
                                double dist = getPrevDistance(currPoint);
                                if (dist >= MIN_DISTANCE_FLAT_SEGMENT) {
                                    _segRecalc = true;
                                }
                            }
                        }
                        if (!_segRecalc) {
                            // we're still going down - do nothing
                            _previousValue_m = _altitudeValue;

                            if (FEATURED)
                                _indexPreviousExtreme = inIndex;
                            if (CHECK_FLAT_SEGMENTS)
                                _segment.setEndIndex(inIndex);
                        }
                    }
                } else if (FEATURED) {
                    if (locallyUp) {
                        if (_gotPreviousMinimum) {
                            // we're still going up - do nothing
                            _previousValue_m = _altitudeValue;
                            _indexPreviousExtreme = inIndex;
                            _segment.setEndIndex(inIndex);
                        }
                    } else {
                        if (_gotPreviousMaximum) {
                            // we're still going down - do nothing
                            _previousValue_m = _altitudeValue;
                            _indexPreviousExtreme = inIndex;
                            _segment.setEndIndex(inIndex);
                        }
                    }
                } else {
                    if (moreThanWiggle)
                        _previousValue_m = _altitudeValue;
                }
            }
        } else {
            // we haven't got a previous value at all, so it's the start of a new segment
            _previousValue_m = _altitudeValue;
            if (FEATURED) _indexPreviousExtreme = inIndex;
            _gotPreviousAltitudeValue = true;
        }
        _lastAltitude_m = _altitudeValue;
    }

    private void updateSegment(int inIndex, boolean inFinish) {
        if (_segment.getStartIndex() == _segment.getEndIndex())
            _segment.setEndIndex(inIndex);
        TrackPoint start = _trackPoints.get(_segment.getStartIndex());
        TrackPoint end   = _trackPoints.get(_segment.getEndIndex());
        _segment.distance = start.distance;
        _segment.elevation = start.elevation;
        _segment.deltaX  = end.distance - start.distance;

        if (inFinish) {
            _overallUp = _gotPreviousMinimum && (_previousValue_m > _previousExtreme);
            _overallDn = _gotPreviousMaximum && _previousValue_m < _previousExtreme;
        }
        if (_overallUp) {
            if (_segment.deltaX > 0.05)
                _segment.segmentType = Segment.type.SEG_UP;
            // Add the climb from _previousExtreme up to _previousValue
            _segment.deltaY = _previousValue_m - _previousExtreme;
            _previousExtreme = _previousValue_m;
            _gotPreviousMinimum = false;
            _gotPreviousMaximum = true;
// todo
            _previousValue_m = _altitudeValue;
        } else if (_overallDn) {
            if (_segment.deltaX > 0.05)
                _segment.segmentType = Segment.type.SEG_DOWN;
            // Add the descent from _previousExtreme down to _previousValue
            _segment.deltaY = _previousValue_m - _previousExtreme;
            _previousExtreme = _previousValue_m;
            _gotPreviousMinimum = true;
            _gotPreviousMaximum = false;
// todo
            _previousValue_m = _altitudeValue;
        } else {
            _segment.deltaY = 0;
            if (_segment.deltaX > 0.05)
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
}