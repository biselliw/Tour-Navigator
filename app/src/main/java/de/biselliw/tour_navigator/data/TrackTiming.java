package de.biselliw.tour_navigator.data;

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
import de.biselliw.tour_navigator.activities.SettingsActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.function.GpxAltitudeSmoother;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import static de.biselliw.tour_navigator.data.TrackSegments.PROFILE_ANALYSIS_DEFAULT;
import static de.biselliw.tour_navigator.data.TrackSegments.PROFILE_ANALYSIS_RDP;
import static de.biselliw.tour_navigator.data.TrackSegments.USE_PROFILE_ANALYSIS_FOR_PLANNED_TOUR;
import static de.biselliw.tour_navigator.data.TrackSegments.SEGMENTED_LEAST_SQUARES;

public class TrackTiming {
    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackTiming";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

        private List<Segment> _segments = null;


    /**
     * Recalculate all track points
     */
    private List<RecordAdapter.Record> analysePlannedTour(TrackDetails inTrack) {
        List<RecordAdapter.Record> recordList = null;
        TrackSegments _trackSegments = new TrackSegments();
        SettingsActivity.getHikingParameters(_trackSegments);
        double sumClimb_m = 0, sumDescent_m = 0;

        if (USE_PROFILE_ANALYSIS_FOR_PLANNED_TOUR == PROFILE_ANALYSIS_DEFAULT){
            _segments = _trackSegments.calcSegmentsByDefault(inTrack, false);
            _trackSegments.calcSegmentsValues(inTrack,false, _segments);
        }
        else if (USE_PROFILE_ANALYSIS_FOR_PLANNED_TOUR == SEGMENTED_LEAST_SQUARES) {
            _segments = _trackSegments.calcSegmentsByLeastSquares(inTrack, false);
            _trackSegments.calcSegmentsValues(inTrack,false, _segments);
        }
        else if (USE_PROFILE_ANALYSIS_FOR_PLANNED_TOUR == PROFILE_ANALYSIS_RDP)
            _segments = _trackSegments.calcSegmentsByRDP(inTrack, false);

        if (!_segments.isEmpty()) {
            int seg = 0;
            Segment segment = _segments.get(seg);
            recordList = new ArrayList<>();
            RecordAdapter.Record record;

            double prevDistance_km = 0;
            long sumStartSeconds = 0L, sumSeconds = 0L;
            double segment_startClimb_m = 0.0, segment_startDescent_m = 0.0;
            int breakTime_min = 0, segSumBreakTime_min = 0;
            for (int ptIndex = 0; ptIndex <= inTrack.getNumPoints()  - 1; ptIndex++) {
                DataPoint currPoint = inTrack.getPoint(ptIndex);

                double segment_climb_m = (segment.deltaY > 0) ? segment.deltaY : 0;
                double segment_descent_m = (segment.deltaY < 0) ? -segment.deltaY : 0;
                if (currPoint.isRoutePoint()) {
                    double dX = currPoint.getDistance() - segment.distance;
                    if (segment.deltaX > 0) {
                        double relDistance = dX / segment.deltaX;
                        double total_climb   = segment_startClimb_m   + relDistance * segment_climb_m;
                        double total_descent = segment_startDescent_m + relDistance * segment_descent_m;
                        long seconds =
                                sumStartSeconds +
                                        (segSumBreakTime_min * 60L) + (long) (relDistance * segment.activeTime_s);

                        record = new RecordAdapter.Record(
                                currPoint,
                                ptIndex,
                                currPoint.getDistance() - prevDistance_km,
                                total_climb - sumClimb_m,
                                total_descent - sumDescent_m,
                                seconds - sumSeconds
                        );

                        prevDistance_km = currPoint.getDistance();
                        sumClimb_m = total_climb;
                        sumDescent_m = total_descent;
                        breakTime_min = currPoint.getWaypointDuration();
                        segSumBreakTime_min += breakTime_min;
                        sumSeconds = seconds + breakTime_min * 60L;

                        recordList.add(record);
                        currPoint.setTime(seconds);
                    }
                }
                if (currPoint.getDistance() >= segment.distance + segment.deltaX) {
                    if (_segments.size() > ++seg) {
                        segment_startClimb_m += segment_climb_m;
                        segment_startDescent_m += segment_descent_m;
                        sumStartSeconds += segment.activeTime_s + segSumBreakTime_min;
                        segment = _segments.get(seg);
                        segSumBreakTime_min = 0;
                    } //else
                      //  break;
                }
            }
        }
        return recordList;
    }

    public List<RecordAdapter.Record> recalculate(TrackDetails inTrack)  {
        if (inTrack.getNumPoints() == 0) return null;

        // does the track only contain trackpoints?
        if (inTrack.isValidRecordedTrackFile()) {
            // smooth all its altitudes
            GpxAltitudeSmoother.smoothTrack(inTrack);

            if (DEBUG) Log.i(TAG, "recalculate(): 1. calculate all segments of the recorded track");
            EstimateParams estimate = new EstimateParams();
            _segments = estimate.analyseRecordedTrack(inTrack);
            return null;
        }
        else {
            return analysePlannedTour(inTrack);
        }
    }

    public int getSegmentsCount() { return (_segments == null) ? 0 : _segments.size(); }

    public Segment getSegment(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex);
        return null;
    }

    public double getSegmentStartDistance(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex).distance;
        return 0.0;
    }
    public double getSegmentEndDistance(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex).distance + _segments.get(inIndex).deltaX;
        return 0.0;
    }

    public double getSegmentStartElevation(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex).elevation;
        return 0.0;
    }

    public double getSegmentEndElevation(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size()) {
                Segment segment = _segments.get(inIndex);
                return segment.elevation + segment.deltaY;
            }
        return 0.0;
    }
}

