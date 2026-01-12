package de.biselliw.tour_navigator.data;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.SettingsActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.functions.GpxAltitudeSmoother;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import static de.biselliw.tour_navigator.data.BaseSegments.baseSegments;


public class TrackTiming {
    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackTiming";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public static TrackTiming trackTiming = null;

    public static Segments segments = null;

    private final TrackDetails _track;

    public static EstimateParams estimate = null, check = null;
    private List<Segment> _segments = null;

    public TrackTiming (TrackDetails inTrack)
    {
        _track = inTrack;
        trackTiming = this;
    }

    /**
     * Recalculate all track points
     */
    public List<RecordAdapter.Record> recalculate()  {
        List<RecordAdapter.Record> recordList = null;
        List<Segment> checkedSegments = null;
        int _numPoints = _track.getNumPoints();
        if (_numPoints <= 0) return null;

        if (DEBUG) {
            android.util.Log.d(TAG, "recalculate(): " + _numPoints + " Trackpoints");
        }

        // does the track only contain trackpoints?
        if (_track.isValidRecordedTrackFile()) {
            // smooth all its altitudes
            GpxAltitudeSmoother.smoothTrack(_track);

            if (DEBUG) Log.i(TAG, "recalculate(): 1. calculate all segments of the recorded track");
            estimate = new EstimateParams();
            estimate.clearRecordedTrackFileInfo();
            _segments = estimate.calcSegments(_track);
            estimate.updateSegmentsTiming(_segments);

            EstimateParams.EstimationResult estimateResult = estimate.estimateGradients(_segments);

//            EstimateParams.EstimationResult estimateResult = estimate.estimateGradients(_segments);
            estimate.addReport(estimate.getRecordedTrackFileInfo1());
            check = estimate; // new EstimateParams();
            check.trackHasTimeStamps = false;
            if (estimateResult.successful)
            {
                estimate.addReport(estimate.getRecordedTrackFileInfo4());
                if (DEBUG) Log.i(TAG, "recalculate(): 2. recalculate all segments without timestamps using estimated hiking parameters");
                check.applyEstimatedHikingParametersFrom(estimateResult);
                checkedSegments = _segments; // check.calcSegments(_track);
                estimate.updateSegmentsTiming(_segments);
                if (check.estimateAll(checkedSegments).successful) {
                    check.addReport(check.getRecordedTrackFileInfo2());
                }
            }
            else
                estimate.addReport(estimate.getRecordedTrackFileInfo5());

            checkedSegments = _segments;

            if (DEBUG) Log.i(TAG, "recalculate(): 2. recalculate all segments using hiking parameters from app settings");
            SettingsActivity.getHikingParameters(check);
//            checkedSegments = check.calcSegments(_track);
            check.updateSegmentsTiming(checkedSegments);
            check.estimateAll(checkedSegments);
            check.addReport(check.getRecordedTrackFileInfo3());
        }
        else {
            segments = new Segments();
            _segments = segments.calcSegments(_track);
            segments.updateSegmentsTiming(_segments);
            if (!_segments.isEmpty()) {
                int seg = 0;
                Segment segment = _segments.get(seg);
                recordList = new ArrayList<>();
                RecordAdapter.Record record;

                double sumDistance_km = 0;
                double sumClimb_m = 0, sumDescent_m = 0;
                long sumSeconds = 0L;
                int breakTime_min = 0;
                for (int ptIndex = 0; ptIndex <= _numPoints - 1; ptIndex++) {
                    DataPoint currPoint = _track.getPoint(ptIndex);

                    if (currPoint.isRoutePoint()) {
                        double segDistance = currPoint.getDistance() - segment.startDistance_km;
                        double climb = 0, descent = 0;
                        long seconds = 0L;
                        if (segment.distance_km > 0) {
                            double relDistance = segDistance / segment.distance_km;
                            climb = segment.startClimb_m + relDistance * segment.climb_m;
                            descent = segment.startDescent_m + relDistance * segment.descent_m;
                            seconds = segment.startSeconds + (long)(relDistance * segment.totalSeconds);
                        }
                        record = new RecordAdapter.Record(
                                currPoint,
                                ptIndex,
                                currPoint.getDistance() - sumDistance_km,
                                climb - sumClimb_m,
                                descent - sumDescent_m,
                                seconds - sumSeconds - breakTime_min * 60L
                        );

                        sumDistance_km = currPoint.getDistance();
                        sumClimb_m = climb;
                        sumDescent_m = descent;
                        breakTime_min = currPoint.getWaypointDuration();
                        sumSeconds = seconds + breakTime_min * 60L;

                        recordList.add(record);
                    }
                    if (DEBUG) {
                        android.util.Log.d(TAG, "timetable built");
                        android.util.Log.d(TAG, "Sclimb: " + sumClimb_m);
                        android.util.Log.d(TAG, "Sdescent: " + sumDescent_m);
                    }
                    currPoint.setTime(sumSeconds);
                    if (ptIndex == segment.endIndex) {
                        if(_segments.size() > ++seg)
                            segment = _segments.get(seg);
                        else
                            break;
                    }
                }
            }
        }

        return recordList;
    }

    public long getTotalBreakTime_min() { return (baseSegments == null ? 0 : segments.getTotalBreakTime_min()); }
    public double getMinAltitude() { return (baseSegments == null ? 0.0 : baseSegments.getMinAltitude()); }
    public double getMaxAltitude() { return (baseSegments == null ? 0.0 : baseSegments.getMaxAltitude()); }
    public double getTotalDistance () { return (baseSegments == null ? 0.0 :  baseSegments.getTotalDistance());}
    public double getTotalClimb () { return (baseSegments == null ? 0.0 : segments.getTotalClimb()); }
    public double getTotalDescent () { return (baseSegments == null ? 0.0 : segments.getTotalDescent()); }
    public long getTotalSeconds () { return (baseSegments == null ? 0 :  segments.getTotalSeconds()); }
    public int getSegmentsCount() { return _segments.size(); }
    public int getSegmentStart(int inIndex) {
        if (inIndex < _segments.size())
            return _segments.get(inIndex).getStartIndex();
        else return 0;
    }
    public int getSegmentEnd(int inIndex) {
        if (inIndex < _segments.size())
            return _segments.get(inIndex).getEndIndex();
        else return 0;
    }
}

