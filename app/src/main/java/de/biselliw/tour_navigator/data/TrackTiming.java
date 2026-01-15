package de.biselliw.tour_navigator.data;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.SettingsActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.functions.GpxAltitudeSmoother;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;


public class TrackTiming {
    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackTiming";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public static TrackTiming trackTiming = null;

    public static BaseSegments baseSegments = null;

    private final TrackDetails _track;

    public static EstimateParams estimate = null;
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
            baseSegments = estimate;
            estimate.clearRecordedTrackFileInfo();
            estimate.trackHasTimeStamps = true;
            _segments = estimate.calcSegments(_track);
            estimate.calcSegmentsValues(_segments);

            if (DEBUG) Log.i(TAG, "recalculate(): 2. determine the best fitting parameters");
            EstimateParams.EstimationResult estimateResult = estimate.estimateGradients(_segments);

            estimate.addReport(estimate.getRecordedTrackFileInfo_Start());
            estimate.trackHasTimeStamps = false;
            if (estimateResult.successful)
            {
                estimate.addReport(estimate.getRecordedTrackFileInfo_Success());
                if (DEBUG) Log.i(TAG, "recalculate(): 3. recalculate all segments without timestamps using estimated hiking parameters");
                estimate.applyEstimatedHikingParametersFrom(estimateResult);
                estimate.updateSegmentsValues(_segments);
                // if (estimate.estimateAll(_segments).successful)
                    estimate.addReport(estimate.getRecordedTrackFileInfo_Prove(_segments));

            }
            else
                estimate.addReport(estimate.getRecordedTrackFileInfo_Failed());

            if (DEBUG) Log.i(TAG, "recalculate(): 3. recalculate all segments using hiking parameters from app settings");
            SettingsActivity.getHikingParameters(estimate);
            estimate.updateSegmentsValues(_segments);
            // estimate.estimateAll(_segments);
            estimate.addReport(estimate.getRecordedTrackFileInfo_UsingSettings(_segments));
        }
        else {
            baseSegments = new BaseSegments();
            SettingsActivity.getHikingParameters(baseSegments);
            baseSegments.trackHasTimeStamps = false;

            _segments = baseSegments.calcSegments(_track);
            baseSegments.calcSegmentsValues(_segments);
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

    public BaseSegments.SummarySegments getSummarySegments() { return baseSegments.summary; }

    public double getMinAltitude() { return (BaseSegments.baseSegments == null ? 0.0 : BaseSegments.baseSegments.getMinAltitude()); }
    public double getMaxAltitude() { return (BaseSegments.baseSegments == null ? 0.0 : BaseSegments.baseSegments.getMaxAltitude()); }




    public int getSegmentsCount() { return (_segments == null) ? 0 : _segments.size(); }

    public Segment getSegment(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex);
        return null;
    }

    public int getSegmentStart(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex).getStartIndex();
        return 0;
    }

    public double getSegmentStartDistance(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex).startDistance_km;
        return 0.0;
    }
    public int getSegmentEnd(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex).getEndIndex();
        return 0;
    }
    public double getSegmentEndDistance(int inIndex) {
        if (_segments != null)
            if (inIndex < _segments.size())
                return _segments.get(inIndex).startDistance_km + _segments.get(inIndex).distance_km;
        return 0.0;
    }
}

