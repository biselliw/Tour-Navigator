package de.biselliw.tour_navigator.functions;

/*
 * This file is part of GpsPrune
 *
 * GpsPrune is a tool to visualize, edit, convert and prune GPS data
 * Please see the included readme.txt or https://activityworkshop.net
 * This software is copyright activityworkshop.net 2006-2022 and made available through the Gnu GPL version 2.
 * For license details please see the included license.txt.
 *
 * modified by Walter Biselli (BiselliW):
 */

/*
    This file is part of Tour Navigator

    Tour Navigator is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    Tour Navigator is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/


import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.data.Segment;
import de.biselliw.tour_navigator.helpers.Log;

import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.INVALID_INDEX;
import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.INVALID_VALUE;

/**
 * Analyze all track points and divide the track into segments according to original design
 * @author tim.prune
 * @implNote BiselliW: optimized segmentation
 */
public class SegmentedDefault {

    /**
     * TAG for log messages.
     */
    static final String TAG = "SegmentedDefault";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    private double _wiggleLimit;

    /** @implNote design extensions */
    private static final boolean SET_END_INDEX_OF_PREVIOUS_EXTREME = true;
    private static final boolean CHECK_FLAT_SEGMENTS = true;
    private static final boolean USE_INITIAL_FLAT = true;
    private static final boolean REORGANIZE_SEGMENTS_AT_END = false;

    private static final double MIN_DISTANCE_FLAT_SEGMENT = 0.25;
    boolean checkFlatSegment = false;
    boolean _initialFlat = true;

    /**
     * Flags for whether we are going up or down
     */
    private boolean _overallUp, _overallDn;

    /**
     * Flags for whether minimum or maximum has been found
     */
    private boolean _gotPreviousMinimum, _gotPreviousMaximum;
    private double _previousMinimum = -1, _previousMaximum = -1;

    /**
     * current altitude value (elevation))
     */
    private double _altitudeValue;
    /**
     * previous altitude value (elevation))
     */
    private double _previousValue_m;
    /**
     * Value of previous minimum or maximum, if any
     */
    private double _previousExtreme;
    /**
     * Previous metric values
     */
    private double _lastAltitude_m;
    double _lastAltitudeDiff_m, _sumLastAltitudes = 0;
    boolean _gotPreviousAltitudeValue;
    int _indexPreviousExtreme;

    /** compressed list of all trackpoints containing valid locations and altitudes */
    private List<TrackPoint> _trackPoints = null;
    TrackPoint _prevPoint;

    private Segment _segment = null, _prevSegment;

    /** if true: data must be recalculated in current segment of the track */
    protected boolean _segmentEndDetected = false;

    /** Clear all data used for analysis of the segments within a track */
    private void clear() {
        _prevSegment = null;
        _gotPreviousAltitudeValue = false;
        _previousValue_m = 0;

        _gotPreviousMinimum = _gotPreviousMaximum = false;
        _previousExtreme = 0;

        _lastAltitude_m = INVALID_VALUE;
        checkFlatSegment = false;
        if (CHECK_FLAT_SEGMENTS && DEBUG)
            _lastAltitudeDiff_m = _sumLastAltitudes = 0.0;

        if (DEBUG)
            _previousMinimum = _previousMaximum = INVALID_VALUE;
        if (SET_END_INDEX_OF_PREVIOUS_EXTREME)
            _indexPreviousExtreme = INVALID_INDEX;

        _prevPoint = null;

        _segmentEndDetected = false;
    }

    /**
     * Analyze all track points and divide the track into segments
     *
     * @param inTrackPoints list of simple track points
     * @param inBreakSegmentsAtRoutePoint if true: force segment end at a route point (original design)
     * @return list of simplified track segments
     */
    public List<Segment> calcSegments(List<TrackPoint> inTrackPoints, double inWiggleLimit,
                                      boolean inBreakSegmentsAtRoutePoint) {
        _wiggleLimit = inWiggleLimit;
        _trackPoints = inTrackPoints;
        if (inTrackPoints == null) return null;

        clear();

        // create an empty list of segments
        List<Segment> segments = new ArrayList<>();
        // create a new segment
        _segment = new Segment();

        int numPoints = inTrackPoints.size();
        if (DEBUG) Log.d(TAG, "calcSegments(): " + numPoints + " Trackpoints");

        boolean segmentEndForced = false, finished = false;
        for (int ptIndex = 0; ptIndex < numPoints; ptIndex++) {
            TrackPoint currPoint = inTrackPoints.get(ptIndex);

            if (ptIndex == 0)
                _segment.setElevation(currPoint.elevation);
            else if (ptIndex >= numPoints - 1)
                finished = true;
            else
                segmentEndForced = (inBreakSegmentsAtRoutePoint && currPoint.isRoutePoint);

            analyzeProfile(ptIndex);

            if (segmentEndForced)
                _segment.setEndIndex(ptIndex);

            if (segmentEndForced || finished)
                updateSegmentEndForced(ptIndex);
            else if (_segmentEndDetected)
                updateSegmentEndDetected(ptIndex, false);

            if (_segmentEndDetected || segmentEndForced || finished) {
                if (_segment.segmentType != Segment.type.SEG_INVALID) {
                    if (REORGANIZE_SEGMENTS_AT_END) {
                        segments.add(_segment);
                    }
                    else {
                        if (_prevSegment != null && _prevSegment.segmentType == Segment.type.SEG_FLAT
                                && _segment.segmentType == Segment.type.SEG_FLAT
                                && !segmentEndForced) {
                            _prevSegment.setDeltaX(_prevSegment.getDeltaX() + _segment.getDeltaX());
                            checkFlatSegment = false;
                            _prevSegment.setEndIndex(_segment.getEndIndex());
                            _segment = _prevSegment;
                        }
                        else {
                            segments.add(_segment);
                            _prevSegment = _segment;
                        }
                    }
                }

                if (segmentEndForced || finished || checkFlatSegment) {
                    TrackPoint start = inTrackPoints.get(_segment.getStartIndex());
                    int endIndex = _segment.getEndIndex();
                    TrackPoint end = inTrackPoints.get(endIndex);
                    if (_segment.segmentType != Segment.type.SEG_INVALID) {
                        start = end;
                        // todo check new segment at end
                        endIndex = ptIndex;
                        if (!finished)
                            endIndex--;
                        end = inTrackPoints.get(endIndex);
                    }
                    double deltaX = end.distance - start.distance;
                    double deltaY = end.elevation - start.elevation;

                    if (deltaX > 0) {
                        if (REORGANIZE_SEGMENTS_AT_END) {
                            _segment = new Segment(_segment);
                            _segment.setDeltaX(deltaX);
                            _segment.setDeltaY(deltaY);
                            _segment.segmentType = Segment.type.SEG_FLAT;
                            _segment.setEndIndex(endIndex); // todo check index
                            segments.add(_segment);
                        }
                        else {
                            if ((deltaX >= MIN_DISTANCE_FLAT_SEGMENT) || segmentEndForced || finished) {
                                // start with new segment
                                //                            _segment = new Segment(start.distance, start.elevation, deltaX, 0.0);
                                Segment prevSegment = _segment;
                                _segment = new Segment(_segment);
                                _segment.setDeltaX(deltaX);
// todo                                _segment.setDeltaY(deltaY);
                                _segment.segmentType = Segment.type.SEG_FLAT;
                                _segment.setEndIndex(endIndex); // todo check index

                                // update overall data at the end of the segment from section data
                                if (_prevSegment != null && _prevSegment.segmentType == Segment.type.SEG_FLAT
                                        && _segment.segmentType == Segment.type.SEG_FLAT
                                        && !segmentEndForced)
                                    _prevSegment.setDeltaX(_prevSegment.getDeltaX() + _segment.getDeltaX());
                                else
                                    segments.add(_segment);
                                _prevSegment = _segment;
                                /*
                                if (_overallUp)
                                    _gotPreviousMinimum = true;
                                if (_overallDn)
                                    _gotPreviousMaximum = true;
                                 */
                                _gotPreviousMinimum = _gotPreviousMaximum = false;
                            }
                        }
                        if (segmentEndForced && _segment.getEndIndex() < ptIndex)
                            _segment.setEndIndex(ptIndex);
                    }
                    if (CHECK_FLAT_SEGMENTS) {
                        checkFlatSegment = false;
                        if (DEBUG) _sumLastAltitudes = -_lastAltitudeDiff_m;
                    }
                }
                _segmentEndDetected = false;

                // start with new segment
                if (_segment.segmentType != Segment.type.SEG_INVALID)
                    _segment = new Segment(_segment);
            }
            _prevPoint = currPoint;

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
        if (DEBUG) {
            if (_lastAltitude_m != INVALID_VALUE) // always true by design
            {
                double altitudeDiff_m = _altitudeValue - _lastAltitude_m;
                if (CHECK_FLAT_SEGMENTS)
                    _sumLastAltitudes += _lastAltitudeDiff_m;
                _lastAltitudeDiff_m = altitudeDiff_m;
            }
        }

        // Compare with previous value if any
        if (_gotPreviousAltitudeValue)
        {
            if ((_altitudeValue != _previousValue_m)) {
                // Got an altitude value which is different from the previous one
                final boolean locallyUp = (_altitudeValue > _previousValue_m);
                _overallUp = _gotPreviousMinimum && (_previousValue_m > _previousExtreme);
                _overallDn = _gotPreviousMaximum && _previousValue_m < _previousExtreme;
                final boolean moreThanWiggle = Math.abs(_altitudeValue - _previousValue_m) > _wiggleLimit;

                // Do we know whether we're going up or down yet?
                if ((!USE_INITIAL_FLAT || _initialFlat) && !_gotPreviousMinimum && !_gotPreviousMaximum) {
                    /* we don't know whether we're going up or down yet - check limit
                     * --------------------------------------------------------------- */
                    if (moreThanWiggle) {
                        if (USE_INITIAL_FLAT)
                            _initialFlat = false;
                        if (locallyUp) {
                            _gotPreviousMinimum = true;
                        } else {
                            _gotPreviousMaximum = true;
                        }
                        _previousExtreme = _previousValue_m;
                        _previousValue_m = _altitudeValue;
                        if (SET_END_INDEX_OF_PREVIOUS_EXTREME)
                            _segment.setEndIndex(_indexPreviousExtreme);
                        if (CHECK_FLAT_SEGMENTS) {
                            checkFlatSegment = true;
                            _segmentEndDetected = true;
                        }
                    } else {
                        if (SET_END_INDEX_OF_PREVIOUS_EXTREME) // todo
                            _indexPreviousExtreme = inIndex;
                    }
                } else if (_overallUp) {
                    /* we're going up
                     * --------------------------------------------------------------- */
                    if (locallyUp) {
                        // we're still going up - do nothing
                        if (checkFlatSegment) {
                            double dist = getPrevDistance(currPoint);
                            if (dist >= MIN_DISTANCE_FLAT_SEGMENT)
                                _segmentEndDetected = true;
                        }
                        if (!_segmentEndDetected) {
                            _previousValue_m = _altitudeValue;

                            if (SET_END_INDEX_OF_PREVIOUS_EXTREME)
                                _indexPreviousExtreme = inIndex;
                            if (CHECK_FLAT_SEGMENTS)
                                _segment.setEndIndex(inIndex);
                        }
                    } else if (moreThanWiggle)
                        // we're going up but have dropped over a maximum
                        _segmentEndDetected = true;
                    else {
                        if (CHECK_FLAT_SEGMENTS && !checkFlatSegment) {
                            if (DEBUG) _sumLastAltitudes = -_lastAltitudeDiff_m;
                            checkFlatSegment = true;
                        }
                    }
                } else if (_overallDn) {
                    /* we're going down
                     * --------------------------------------------------------------- */
                    if (locallyUp) {
                        if (moreThanWiggle)
                            // we're going down but have climbed up from a minimum
                            _segmentEndDetected = true;
                        else {
                            if (CHECK_FLAT_SEGMENTS && !checkFlatSegment) {
                                if (DEBUG) _sumLastAltitudes = -_lastAltitudeDiff_m;
                                checkFlatSegment = true;
                            }
                        }
                    } else {
                        if (checkFlatSegment) {
                            double dist = getPrevDistance(currPoint);
                            if (dist >= MIN_DISTANCE_FLAT_SEGMENT)
                                _segmentEndDetected = true;
                        }
                        if (!_segmentEndDetected) {
                            // we're still going down - do nothing
                            _previousValue_m = _altitudeValue;

                            if (SET_END_INDEX_OF_PREVIOUS_EXTREME)
                                _indexPreviousExtreme = inIndex;
                            if (CHECK_FLAT_SEGMENTS)
                                _segment.setEndIndex(inIndex);
                        }
                    }
                } else {
                    /* we're not going up or down
                     * --------------------------------------------------------------- */
                    if (SET_END_INDEX_OF_PREVIOUS_EXTREME) {
                        if (locallyUp) {
                            if (_gotPreviousMinimum) {
                                // we're still going up - do nothing
                                _previousValue_m = _altitudeValue;
                                _indexPreviousExtreme = inIndex;
                                _segment.setEndIndex(inIndex);
                            } else if (moreThanWiggle) {
                                _gotPreviousMaximum = false;
                                _gotPreviousMinimum = true;
                                _segmentEndDetected = true;
                            }
                        } else {
                            if (_gotPreviousMaximum) {
                                // we're still going down - do nothing
                                _previousValue_m = _altitudeValue;
                                _indexPreviousExtreme = inIndex;
                                _segment.setEndIndex(inIndex);
                            } else if (moreThanWiggle) {
                                _gotPreviousMaximum = true;
                                _gotPreviousMinimum = false;
                                _segmentEndDetected = true;
                            }
                        }
                    } else {
                        if (moreThanWiggle)
                            _previousValue_m = _altitudeValue;
                    }
                }
            }
        }
        else {
            // we haven't got a previous value at all, so it's the start of a new segment
            _previousValue_m = _altitudeValue;
            if (SET_END_INDEX_OF_PREVIOUS_EXTREME)
                _indexPreviousExtreme = inIndex;
            _gotPreviousAltitudeValue = true;
        }
        _lastAltitude_m = _altitudeValue;
    }

    private void updateSegmentEndDetected(int inIndex, boolean inFinish) {
        if (_segment.getStartIndex() == _segment.getEndIndex())
            _segment.setEndIndex(inIndex);
        TrackPoint start = _trackPoints.get(_segment.getStartIndex());
        TrackPoint end   = _trackPoints.get(_segment.getEndIndex());
        _segment.setDistance(start.distance); // todo
        _segment.setDeltaX(end.distance - start.distance);

        if (inFinish) {
            _overallUp = _gotPreviousMinimum && (_previousValue_m > _previousExtreme);
            _overallDn = _gotPreviousMaximum && _previousValue_m < _previousExtreme;
        }
        double deltaY = _previousValue_m - _previousExtreme;
        if (_overallUp) {
            if (_segment.getDeltaX()  > 0.05)
                _segment.segmentType = Segment.type.SEG_UP;
            // Add the climb from _previousExtreme up to _previousValue
            if (DEBUG)
                _previousMinimum = _previousExtreme;
            _segment.setDeltaY(deltaY);
            _previousExtreme = _previousValue_m;
            _gotPreviousMinimum = false;
            _gotPreviousMaximum = true;
            _previousValue_m = _altitudeValue;
        } else if (_overallDn) {
            if (_segment.getDeltaX()  > 0.05)
                _segment.segmentType = Segment.type.SEG_DOWN;
            // Add the descent from _previousExtreme down to _previousValue
            if (DEBUG)
                _previousMaximum = _previousExtreme;
            _segment.setDeltaY(deltaY);
            _previousExtreme = _previousValue_m;
            _gotPreviousMinimum = true;
            _gotPreviousMaximum = false;
            _previousValue_m = _altitudeValue;
        } else {
            _segment.setDeltaY(0); // todo deltaY ?
            if (_segment.getDeltaX()  > 0.05)
                _segment.segmentType = Segment.type.SEG_FLAT;
        }
    }

    private void updateSegmentEndForced(int inIndex) {
        _segment.segmentEndForced = true;
        if (_segment.getEndIndex() <= _segment.getStartIndex())
            _segment.setEndIndex(inIndex);
        TrackPoint start = _trackPoints.get(_segment.getStartIndex());
        TrackPoint end = _trackPoints.get(_segment.getEndIndex());
        _segment.setDistance(start.distance); // todo
        _segment.setDeltaX(end.distance - start.distance);
        double deltaY = _previousValue_m - _previousExtreme;
        double deltaElevation = end.elevation - _segment.getElevation();
        double dY = deltaElevation - deltaY;
        _segment.setDeltaY(deltaElevation);
// todo        if (_previousExtreme > 0)        _segment.setDeltaY(deltaY);

        if (_segment.getDeltaY() > 0) {
            _segment.segmentType = Segment.type.SEG_UP;
            if (DEBUG)
                _previousMinimum = _previousExtreme;
            _gotPreviousMinimum = true;
            _previousExtreme = _previousValue_m;
        } else if (_segment.getDeltaY() < 0) {
            _segment.segmentType = Segment.type.SEG_DOWN;
            if (DEBUG)
                _previousMaximum = _previousExtreme;
            _gotPreviousMaximum = true;
            _previousExtreme = _previousValue_m;
        } else {
            _segment.segmentType = Segment.type.SEG_FLAT;
        }
    }
}