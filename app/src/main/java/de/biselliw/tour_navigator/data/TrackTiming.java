package de.biselliw.tour_navigator.data;

import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import org.w3c.dom.*;


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

    long _totalBreakTime_min = 0L;

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

        recordList = new ArrayList<>();
        RecordAdapter.Record record;

        int _numPoints = _track.getNumPoints();
        if (_numPoints <= 0) return null;

        if (DEBUG) {
            android.util.Log.d(TAG, "recalculate(): " + _numPoints + " Trackpoints");
        }

        segments = new Segments();
        List<Segment> _segments = segments.calcSegments(_track);
        if (!_segments.isEmpty()) {
            int seg = 0;
            Segment segment = _segments.get(seg);

            double sumDistance_km = 0;
            double sumClimb_m = 0, sumDescent_m = 0;
            long sumSeconds = 0L;
            int breakTime_min = 0;
            int sumBreakTime_min = 0;
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
                    sumBreakTime_min += breakTime_min;

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
            _totalBreakTime_min = sumBreakTime_min;
        }

        return recordList;
    }

    public long getTotalBreakTime_min() { return _totalBreakTime_min; }
    public double getMinAltitude() { return segments.getMinAltitude(); }
    public double getMaxAltitude() { return segments.getMaxAltitude(); }
    public double getTotalDistance () {	return segments.getTotalDistance();}
    public double getTotalClimb () { return segments.getTotalClimb(); }
    public double getTotalDescent () { return segments.getTotalDescent(); }
    public long getTotalSeconds () { return segments.getTotalSeconds(); }
}

