package de.biselliw.tour_navigator.data;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.SettingsActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.functions.GpxAltitudeSmoother;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

public class TrackTiming {
    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackTiming";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    private final TrackDetails _track;

    private List<Segment> _segments = null;

    public TrackTiming (TrackDetails inTrack)
    {
        _track = inTrack;
    }

    /**
     * Recalculate all track points
     */
    private List<RecordAdapter.Record> recalculatePlannedTrack() {
        List<RecordAdapter.Record> recordList = null;
        TrackSegments _trackSegments = new TrackSegments();
        SettingsActivity.getHikingParameters(_trackSegments);
        _trackSegments.trackHasTimeStamps = false;
        double sumClimb_m = 0, sumDescent_m = 0;

        _segments = _trackSegments.calcSegments(_track);
        _trackSegments.calcSegmentsValues(_segments);
        if (!_segments.isEmpty()) {
            int seg = 0;
            Segment segment = _segments.get(seg);
            recordList = new ArrayList<>();
            RecordAdapter.Record record;

            double sumDistance_km = 0;
            long sumSeconds = 0L;
            int breakTime_min = 0, segSumBreakTime_min = 0;
            for (int ptIndex = 0; ptIndex <= _track.getNumPoints()  - 1; ptIndex++) {
                DataPoint currPoint = _track.getPoint(ptIndex);
                if (currPoint.isRoutePoint()) {
                    double segDistance = currPoint.getDistance() - segment.startDistance_km;
                    if (segment.distance_km > 0) {
                        double relDistance = segDistance / segment.distance_km;
                        double climb = segment.startClimb_m + relDistance * segment.climb_m;
                        double descent = segment.startDescent_m + relDistance * segment.descent_m;
                        long seconds = segment.startSeconds + (segSumBreakTime_min * 60L) + (long) (relDistance * (segment.totalSeconds - segment.totalBreakTime_s));

                        record = new RecordAdapter.Record(
                                currPoint,
                                ptIndex,
                                currPoint.getDistance() - sumDistance_km,
                                climb - sumClimb_m,
                                descent - sumDescent_m,
                                seconds - sumSeconds
                        );

                        sumDistance_km = currPoint.getDistance();
                        sumClimb_m = climb;
                        sumDescent_m = descent;
                        breakTime_min = currPoint.getWaypointDuration();
                        segSumBreakTime_min += breakTime_min;
                        sumSeconds = seconds + breakTime_min * 60L;

                        recordList.add(record);
                        currPoint.setTime(seconds);
                    }
                }
                if (ptIndex >= segment.endIndex) {
                    if (_segments.size() > ++seg) {
                        segment = _segments.get(seg);
                        segSumBreakTime_min = 0;
                    } else
                        break;
                }
            }
        }
        return recordList;
    }

    public List<RecordAdapter.Record> recalculate()  {
        if (_track.getNumPoints() == 0) return null;

        // does the track only contain trackpoints?
        if (_track.isValidRecordedTrackFile()) {
            // smooth all its altitudes
            GpxAltitudeSmoother.smoothTrack(_track);

            if (DEBUG) de.biselliw.tour_navigator.helpers.Log.i(TAG, "recalculate(): 1. calculate all segments of the recorded track");
            EstimateParams estimate = new EstimateParams();
            _segments = estimate.recalculateRecordedTrack();

            return null;
        }
        else
            return recalculatePlannedTrack();
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
                return _segments.get(inIndex).startDistance_km;
        return 0.0;
    }
    public double getSegmentEndDistance(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex).startDistance_km + _segments.get(inIndex).distance_km;
        return 0.0;
    }
}

