package de.biselliw.tour_navigator.data;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.functions.Filters;
import de.biselliw.tour_navigator.functions.RDP;
import de.biselliw.tour_navigator.functions.SegmentedDefault;
import de.biselliw.tour_navigator.functions.SegmentedLeastSquares;
import de.biselliw.tour_navigator.functions.TrackPoint;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import tim.prune.data.Altitude;
import tim.prune.data.Distance;

import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.INVALID_VALUE;

/**
 * see tim.prune.data.AltitudeRange
 */
public class TrackSegments {
    // hiking speed parameters
    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackSegments";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;


    public static final int PROFILE_ANALYSIS_DEFAULT = 1;
    public static final int PROFILE_ANALYSIS_RDP = 2;
    public static final int SEGMENTED_LEAST_SQUARES = 3;

    public static final int USE_PROFILE_ANALYSIS_FOR_PLANNED_TOUR = PROFILE_ANALYSIS_DEFAULT;
    public static final int USE_PROFILE_ANALYSIS_FOR_RECORDED_TRACK = PROFILE_ANALYSIS_DEFAULT;

    public static final boolean USE_PROFILE_ANALYSIS_DEFAULT =
            (USE_PROFILE_ANALYSIS_FOR_PLANNED_TOUR == PROFILE_ANALYSIS_DEFAULT) ||
            (USE_PROFILE_ANALYSIS_FOR_RECORDED_TRACK == PROFILE_ANALYSIS_DEFAULT);
    public static final boolean USE_RDP_PROFILE_ANALYSIS =
            (USE_PROFILE_ANALYSIS_FOR_PLANNED_TOUR == PROFILE_ANALYSIS_RDP) ||
            (USE_PROFILE_ANALYSIS_FOR_RECORDED_TRACK == PROFILE_ANALYSIS_RDP);
    public static final boolean USE_SEGMENTED_LEAST_SQUARES =
            (USE_PROFILE_ANALYSIS_FOR_PLANNED_TOUR == SEGMENTED_LEAST_SQUARES) ||
            (USE_PROFILE_ANALYSIS_FOR_RECORDED_TRACK == SEGMENTED_LEAST_SQUARES);


    public final static double DEF_HOR_SPEED = 4.5, DEF_SPEED_CLIMB = 0.35, DEF_SPEED_DESCENT = 0.5;

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
    protected int gradientThresholdClimb = (int) (DEF_GRADIENT_THRESHOLD_CLIMB);
    /**
     * descending part in [km/h]
     */
    protected double _speedDescent = DEF_SPEED_DESCENT;
    protected int gradientThresholdDesc = (int) (DEF_GRADIENT_THRESHOLD_DESC);
    protected TrackDetails _track = null;
    private static double _minAltitude_m, _maxAltitude_m;

    public static SummarySegments summary = new SummarySegments();

    /**
     * Calculate min/max altitude values of a track
     *
     * @param inTrack track
     */
    protected void calculateMinMaxAltitudes (TrackDetails inTrack) {
        _minAltitude_m = _maxAltitude_m = INVALID_VALUE;
        for (int i = 0; i < inTrack.getNumPoints(); i++) {
            DataPoint currPoint = inTrack.getPoint(i);
            if (!currPoint.isWayPoint()) {
                // does the current point has a valid altitude?
                if (currPoint.hasAltitude()) {
                    Altitude altitude = currPoint.getAltitude();
                    if (altitude.isValid()) {
                        double altitudeValue_m = altitude.getValue();
                        if ((altitudeValue_m < _minAltitude_m) || (_minAltitude_m == INVALID_VALUE))
                            _minAltitude_m = altitudeValue_m;
                        if (altitudeValue_m > _maxAltitude_m)
                            _maxAltitude_m = altitudeValue_m;
                    }
                }
            }
        }
    }

    public static class SummarySegments {
        public final boolean valid;
        public final double totalDistance_km, totalDistanceClimb_km, totalDistanceDescent_km;
        public final double minAltitude_m, maxAltitude_m;
        public final double sum_climb_m, sum_descent_m;
        public final long totalBreakTime_min;
        public final long totalSeconds;

        public SummarySegments() {
            this.valid = false;
            this.totalDistance_km = 0.0;
            this.totalDistanceClimb_km = 0.0;
            this.totalDistanceDescent_km = 0.0;
            this.minAltitude_m = 0.0;
            this.maxAltitude_m = 0.0;
            this.sum_climb_m = 0;
            this.sum_descent_m = 0;
            this.totalSeconds = 0;
            this.totalBreakTime_min = 0;
        }

        public SummarySegments(double totalDistance_km, double totalDistanceClimb_km, double totalDistanceDescent_km,
                               double minAltitude_m, double  maxAltitude_m,
                               double sum_climb_m, double sum_descent_m, long totalSeconds, long totalBreakTime_min) {
            this.valid = true;
            this.totalDistance_km = totalDistance_km;
            this.totalDistanceClimb_km = totalDistanceClimb_km;
            this.totalDistanceDescent_km = totalDistanceDescent_km;
            this.minAltitude_m = minAltitude_m;
            this.maxAltitude_m = maxAltitude_m;
            this.sum_climb_m = sum_climb_m;
            this.sum_descent_m = sum_descent_m;
            this.totalSeconds = totalSeconds;
            this.totalBreakTime_min = totalBreakTime_min;
        }
    }

    public void calcSegmentsValues(TrackDetails inTrack, boolean inHasTimeStamps,
       List<Segment> inSegments) {
        summary = updateSegmentsValues(inTrack, inHasTimeStamps, inSegments);
    }

    /**
     * Calculate break and overall times within all segments
     * @param inTrack track
     * @param inTrackHasTimeStamps use timestamps of a recorded track
     * @param inSegments list of segments
     */
    public SummarySegments updateSegmentsValues(TrackDetails inTrack, boolean inTrackHasTimeStamps, List<Segment> inSegments) {
        if (inSegments == null) return null;
        long totalBreakTime_s = 0L;
        long totalSeconds = 0L;
        long vertSeconds;
        double gradientThreshold;
        double totalDistance_km = 0.0, totalDistanceClimb_km = 0.0, totalDistanceDescent_km = 0.0;
        double sum_climb_m = 0.0, sum_descent_m = 0.0;

        for (int i = 0; i < inSegments.size(); i++) {
            Segment segment = inSegments.get(i);

            if (segment.getDeltaX() > 0)
                segment.setGradient(Math.abs((int)(segment.getDeltaY() / segment.getDeltaX()) / 10));
            if (segment.segmentType == Segment.type.SEG_INVALID) {
                if (segment.getDeltaY() > 0)
                    segment.segmentType = Segment.type.SEG_UP;
                else if (segment.getDeltaY() < 0)
                    segment.segmentType = Segment.type.SEG_DOWN;
                else
                    segment.segmentType = Segment.type.SEG_FLAT;
            }

            DataPoint start = _track.getPoint(segment.getStartIndex());
            DataPoint end   = _track.getPoint(segment.getEndIndex());
            long horSeconds = 0;

            segment.setBreakTime_s(calcTotalBreakTimeBetween(
                    inTrack, inTrackHasTimeStamps, segment.getStartIndex(), segment.getEndIndex()));
            if (segment.getBreakTime_s() > 0)
                totalBreakTime_s += segment.getBreakTime_s();
            if (inTrackHasTimeStamps) {
                long activeTime_s = 0;
                if (start != null && start.hasTimestamp() && end != null && end.hasTimestamp())
                    activeTime_s = end.getTimestamp().getSecondsSince(start.getTimestamp()) - segment.getBreakTime_s();
                if (segment.getActiveTime_s() < 0)
                    activeTime_s = 0;
                segment.setActiveTime_s(activeTime_s);
            }
            else
                horSeconds = (long) (segment.getDeltaX() / _horSpeed * 3600.0);

            // Calculate moderate/steep type of a segment depending on the gradient threshold
            switch (segment.segmentType) {
                case SEG_FLAT:
                    if (segment.getDeltaY() > 0)
                        sum_climb_m += segment.getDeltaY(); // todo
                    else
                        sum_descent_m -= segment.getDeltaY();
                    break;
                case SEG_UP:
                case SEG_UP_MODERATE:
                case SEG_UP_STEEP:
                    sum_climb_m += segment.getDeltaY();
                    totalDistanceClimb_km += segment.getDeltaX();
                    break;
                case SEG_DOWN:
                case SEG_DOWN_MODERATE:
                case SEG_DOWN_STEEP:
                    sum_descent_m -= segment.getDeltaY();
                    totalDistanceDescent_km += segment.getDeltaX();
                    break;
            }

            if (!inTrackHasTimeStamps) {
                switch (segment.segmentType) {
                    case SEG_FLAT:
                        segment.setActiveTime_s(horSeconds);
                        break;
                    case SEG_UP:
                    case SEG_UP_MODERATE:
                    case SEG_UP_STEEP:
                        vertSeconds = (long) (segment.getDeltaY() / _speedClimb * 3.6);
                        if (DEBUG) gradientThreshold = _speedClimb / _horSpeed * 100;
                        if (horSeconds > vertSeconds) {
                            segment.setActiveTime_s((long) (horSeconds + vertSeconds / 2.0));
                            segment.segmentType = Segment.type.SEG_UP_MODERATE;
                        } else {
                            segment.setActiveTime_s((long) (horSeconds / 2.0 + vertSeconds));
                            segment.segmentType = Segment.type.SEG_UP_STEEP;
                        }
                        totalDistanceClimb_km += segment.getDeltaX();
                        break;
                    case SEG_DOWN:
                    case SEG_DOWN_MODERATE:
                    case SEG_DOWN_STEEP:
                        vertSeconds = (long) (-segment.getDeltaY() / _speedDescent * 3.6);
                        if (DEBUG) gradientThreshold = _speedDescent / _horSpeed * 100;
                        if (horSeconds > vertSeconds) {
                            segment.setActiveTime_s((long) (horSeconds + vertSeconds / 2.0));
                            segment.segmentType = Segment.type.SEG_DOWN_MODERATE;
                        } else {
                            segment.setActiveTime_s((long) (horSeconds / 2.0 + vertSeconds));
                            segment.segmentType = Segment.type.SEG_DOWN_STEEP;
                        }
                        totalDistanceDescent_km += segment.getDeltaX();
                        break;
                }
            }

            totalSeconds += segment.getActiveTime_s() + segment.getBreakTime_s();
            totalDistance_km += segment.getDeltaX();
        }

        /*
        if (DEBUG) {
            if (inTrackHasTimeStamps) {
                Log.d(TAG, "gradient = " + _segment.gradient);
                double hor_speed = _segment.deltaX / _segment.activeTime_s  * 3600.0;
                Log.d(TAG, _segment.getSegmentType() + ": speed = " + formatDouble(hor_speed) + " km/h");
            }
        }
         */

        return new SummarySegments(totalDistance_km, totalDistanceClimb_km, totalDistanceDescent_km, _minAltitude_m, _maxAltitude_m, sum_climb_m, sum_descent_m, totalSeconds, totalBreakTime_s / 60L);
    }

    /**
     * Calculate the total break time within a range of track points
     * @param inTrack track
     * @param inTrackHasTimeStamps if true: use recorded track with timestamps
     * @param inStart index of the first point within the range
     * @param inEnd index of the last point within the range
     * @return total break time [s]
     */
    private long calcTotalBreakTimeBetween(TrackDetails inTrack, boolean inTrackHasTimeStamps, int inStart, int inEnd) {
        DataPoint prevPoint = null, currPoint;
        long totalBreakTime_s = 0L;
        for (int ptIndex = inStart; ptIndex <= inEnd; ptIndex++) {
            currPoint = inTrack.getPoint(ptIndex);
            if ((currPoint != null) && (!currPoint.isWayPoint())) {
//                if (inTrackHasTimeStamps) {
                if (currPoint.hasTimestamp()) {
                    if (prevPoint != null && prevPoint.hasTimestamp()) {
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
                } else {
                    // get break time from track point in GPX file
                    long breakTime_s = currPoint.getWaypointDuration() * 60L;
                    totalBreakTime_s += breakTime_s;
                }
            }
        }
        return totalBreakTime_s;
    }

    /**
     * set hiking parameters:
     *
     * @param horSpeed        horizontal part in [km/h]
     * @param speedClimb      ascending part in [km/h]
     * @param speedDescent    descending part in [km/h]
     * param toleranceMetres min. required change of altitude in metres
     */
    public void setHikingParameters(double horSpeed, double speedClimb, double speedDescent) {
        if (horSpeed > 0)
            _horSpeed = horSpeed;
        if (speedClimb > 0)
            _speedClimb = speedClimb;
        if (speedDescent > 0)
            _speedDescent = speedDescent;
    }

    public long calcTotalTimeFromSegments(List<Segment> inSegments) {
        long total_Seconds = 0L;
        /* Analyze segments */
        for (int i = 0; i < inSegments.size(); i++) {
            Segment segment = inSegments.get(i);
            long horSeconds = (long) (segment.getDeltaX() / _horSpeed * 3600.0);
            long vertSeconds = 0;
            long totalSeconds = 0L;

            switch (segment.segmentType) {
                case SEG_FLAT:
                    totalSeconds = horSeconds;
                    break;
                case SEG_UP_MODERATE:
                    vertSeconds = (long) (segment.getDeltaY() / _speedClimb * 3.6);
                    totalSeconds = horSeconds + vertSeconds / 2;
                    break;
                case SEG_UP_STEEP:
                    vertSeconds = (long) (segment.getDeltaY() / _speedClimb * 3.6);
                    totalSeconds = horSeconds / 2 + vertSeconds;
                    break;
                case SEG_DOWN_MODERATE:
                    vertSeconds = (long) (-segment.getDeltaY() / _speedDescent * 3.6);
                    totalSeconds = horSeconds + vertSeconds / 2;
                    break;
                case SEG_DOWN_STEEP:
                    vertSeconds = (long) (-segment.getDeltaY() / _speedDescent * 3.6);
                    totalSeconds = horSeconds / 2 + vertSeconds;
                    break;
            }
            total_Seconds += totalSeconds;
        }
        return total_Seconds;
    }

    public long getTotalSeconds() {
        if (summary == null) return 0;
        else return summary.totalSeconds;
    }

    public static double getMinAltitude() {
        return summary.minAltitude_m;
    }

    public static double getMaxAltitude() {
        return summary.maxAltitude_m;
    }

    public static String formatDouble(double inValue) {
        DecimalFormat formatter = new DecimalFormat("#0.00");
        return formatter.format(inValue);
    }

    /**
     * Find start and end index of each segment with regard to the track distance
     * for plausibility: check timestamps and distances
     *
     * @param inSegments list of all segments
     * @param inTrack track
     * @param inTrackHasTimeStamps if true: apply parameters for analysis of recorded tracks
     * @return true if successful
     */
    public boolean linkSegmentsToTrack(List<Segment> inSegments, TrackDetails inTrack, boolean inTrackHasTimeStamps) {
        if (inSegments == null || inTrack == null) return false;
        boolean success = true, check = false;
        Segment currSegment, prevSegment = null;
        DataPoint firstPoint = null, prevPoint = null, currPoint = null;
        double distance, prevDistance = -999.0;
        long totalTime = 0L, failureTimes = 0L;
        if (!inSegments.isEmpty() && inTrack.getNumPoints() > 0) {
            int segmentIndex = 0;
            currSegment = inSegments.get(segmentIndex);
            currSegment.setStartIndex(-1);
            currSegment.setEndIndex(-1);
            DataPoint lastPoint = inTrack.getPoint(inTrack.getNumPoints() - 1);
            for (int i = 0; i < inTrack.getNumPoints(); i++) {
                currPoint = inTrack.getPoint(i);
                if (!currPoint.isWayPoint() && (!inTrackHasTimeStamps || currPoint.hasTimestamp())) {
                    if (firstPoint == null)
                        firstPoint = currPoint;
                    if (currPoint == lastPoint)
                        check = true;
                    distance = currPoint.getDistance();
                    if (distance > prevDistance) {
                        if (currSegment.getStartIndex() < 0) {
                            currSegment.setStartIndex(i);
                            if (prevSegment != null)
                                prevSegment.setEndIndex(i);
                            prevSegment = currSegment;
                            prevDistance = distance;
                        }
                        if (prevPoint != null) {
                            if (inTrackHasTimeStamps) {
                                if (currPoint.hasTimestamp() && prevPoint.hasTimestamp()) {
                                    long deltaT = currPoint.getTimestamp().getMillisecondsSince(prevPoint.getTimestamp());
                                    if (deltaT > 0) {
                                        totalTime += deltaT;
                                        deltaT = currPoint.getTimestamp().getMillisecondsSince(firstPoint.getTimestamp());
                                        if (deltaT - failureTimes != totalTime)
                                            check = true;
                                    }
                                    else
                                        // time failure from GPS receiver
                                        failureTimes += deltaT;
                                }
                            }
                        }
                        prevPoint = currPoint;
                    }
                    else
                        // failure in GPX file
                        check = true;
                    if (distance + 0.005 >= currSegment.getDistance() + currSegment.getDeltaX())
                        if (segmentIndex < inSegments.size() - 1) {
                            currSegment = inSegments.get(++segmentIndex);
                            currSegment.setStartIndex(-1);
                            currSegment.setEndIndex(-1);
                        }
                }
                else
                    check = true;
            }
            currSegment.setEndIndex(inTrack.getNumPoints() - 1);
            if (inTrackHasTimeStamps) {
                success = currPoint != null && currPoint.hasTimestamp() && firstPoint != null && firstPoint.hasTimestamp();
                if (success) {
                    long deltaT = currPoint.getTimestamp().getMillisecondsSince(firstPoint.getTimestamp());
                    success = deltaT - failureTimes == totalTime;
                }
            }
        }
        return success;
    }

    /**
     * Make List<TrackPoint> from track
     */
    public List<TrackPoint> makeTrackPointList (TrackDetails inTrack) {
        // create a list of simplified track points out of the provided track
        List<TrackPoint> trackPoints = new ArrayList<>();
        DataPoint previousPoint = null;
        double totalDistance_km = 0.0;

        for (int i = 0; i < inTrack.getNumPoints(); i++) {
            DataPoint currPoint = inTrack.getPoint(i);
            if (!currPoint.isWayPoint()) {
                // Calculate the distance to the previous trackpoint
                if (previousPoint != null) {
                    double radians = DataPoint.calculateRadiansBetween(previousPoint, currPoint);
                    if (radians >= 0) {
                        double distance_km = Distance.convertRadiansToDistance(radians);
                        // summarize distances in current segment - only temporary!
                        totalDistance_km += distance_km;
                        // calc distance since start for each data point
                        currPoint.setDistance(totalDistance_km);
                    }
                    else
                        currPoint.setDistance(0.0);
                } else
                    currPoint.setDistance(0.0);
                previousPoint = currPoint;

                // does the current point has a valid altitude?
                if (currPoint.hasAltitude()) {
                    Altitude altitude = currPoint.getAltitude();
                    if (altitude.isValid())
                        trackPoints.add(new TrackPoint(totalDistance_km, altitude.getValue(), currPoint.isRoutePoint()));
                }
            }
        }

        return trackPoints;
    }

    /**
     * Analyze all track points of a track and divide the track into segments
     * using the default algorithm provided by ActivityWorkshop
     *
     * @param inTrack track
     * @param inTrackHasTimeStamps if true: apply parameters for analysis of recorded tracks
     * @param inBreakSegmentsAtRoutePoint if true: force segment end at a route point
     * @return list of track segments
     */
    public List<Segment> calcSegmentsByDefault(TrackDetails inTrack, boolean inTrackHasTimeStamps,
               boolean inBreakSegmentsAtRoutePoint) {
        _track = inTrack;
        if (!USE_PROFILE_ANALYSIS_DEFAULT || inTrack == null) return null;

        int window = (inTrackHasTimeStamps ? 30 : 7);
        /* Tolerance value in metres : hysteresis value [m] for a change of altitude */
        final int wiggleLimit = 15;

        // create a list of simplified track points out of the provided track
        List<TrackPoint> trackPoints = makeTrackPointList(inTrack);

        calculateMinMaxAltitudes(inTrack);

        // Smooth GPS noise using a median filter
        List<TrackPoint> filtered = Filters.medianFilter(trackPoints, window);

        // Approximate the profile as a sequence of linear segments using the default method
        SegmentedDefault segmentedDefault = new SegmentedDefault();
        List<Segment> segments =
                segmentedDefault.calcSegments(filtered, wiggleLimit, inBreakSegmentsAtRoutePoint);

        // update the linear segments (indices refer to simplified track!)
        linkSegmentsToTrack(segments, inTrack, inTrackHasTimeStamps);

        return segments;
    }

    /**
     * Analyze all track points of a track and divide the track into segments
     * using Segmented Least Squares
     *
     * @param inTrack track
     * @param inTrackHasTimeStamps if true: apply parameters for analysis of recorded tracks
     * @return list of track segments
     */
    public List<Segment> calcSegmentsByLeastSquares(TrackDetails inTrack, boolean inTrackHasTimeStamps) {
        _track = inTrack;
        if (!USE_SEGMENTED_LEAST_SQUARES || inTrack == null) return null;

        int window = (inTrackHasTimeStamps ? 5 : 5);
        /* lambda penalty per segment (controls number of segments): λ ≈ 5–20 */
        double lambda = (inTrackHasTimeStamps ? 10.0 : 5.0);

        // create a list of simplified track points out of the provided track
        List<TrackPoint> trackPoints = makeTrackPointList(inTrack);

        calculateMinMaxAltitudes(inTrack);

        // Smooth GPS noise using a median filter
        List<TrackPoint> filtered = Filters.medianFilter(trackPoints, window);

        // Approximate the profile as a sequence of linear segments using SegmentedLeastSquares
        List<SegmentedLeastSquares.Segment> _segments =
                SegmentedLeastSquares.fit(trackPoints, lambda);

        if (_segments == null) return null;

        // transfer the linear segments
        List<Segment> segments = new ArrayList<>();

        for (int i = 0; i < _segments.size(); i++) {
            SegmentedLeastSquares.Segment _segment = _segments.get(i);
            TrackPoint start   = trackPoints.get(_segment.startIndex);
            // NOTE for SegmentedLeastSquares.Segment: _segment.get(i+1).startIndex = _segment.get(i).endIndex + 1 !
            int endIndex = _segment.endIndex;
            if (i < _segments.size() - 1) endIndex++;
            TrackPoint end    = trackPoints.get(endIndex);
            Segment segment = new Segment(start.distance, start.elevation,
                    end.distance - start.distance, end.elevation - start.elevation);
            segments.add(segment);
        }

        linkSegmentsToTrack(segments, inTrack, inTrackHasTimeStamps);

        return segments;
    }


    /**
     * Analyze all track points of a track and divide the track into segments
     * using RDP
     *
     * @param inTrack track
     * @param inTrackHasTimeStamps if true: apply parameters for analysis of recorded tracks
     * @return list of track segments
     */
    public List<Segment> calcSegmentsByRDP(TrackDetails inTrack, boolean inTrackHasTimeStamps) {
        _track = inTrack;
        if (!USE_RDP_PROFILE_ANALYSIS || inTrack == null) return null;

        int window = (inTrackHasTimeStamps ? 30 : 7);
        double epsilon = (inTrackHasTimeStamps ? 10.0 : 2.0);

        // create a list of simplified track points out of the provided track
        List<TrackPoint> trackPoints = makeTrackPointList(inTrack);

        calculateMinMaxAltitudes(inTrack);

        // Smooth GPS noise using a median filter
        List<TrackPoint> filtered = Filters.medianFilter(trackPoints, window);

        // Approximate the profile as a sequence of linear segments using the Ramer-Douglas-Peucker method
        List<TrackPoint> simplified = RDP.simplify(filtered, epsilon);

        // transfer the linear segments
        List<Segment> segments = new ArrayList<>();

        for (int i = 0; i < simplified.size() - 1; i++) {
            TrackPoint start = simplified.get(i);
            TrackPoint end = simplified.get(i + 1);
            double deltaX = end.distance - start.distance;
            if (deltaX > 0.0) {
                Segment segment = new Segment(start.distance, start.elevation,
                    deltaX, end.elevation - start.elevation);
                segments.add (segment);
            }
        }

        linkSegmentsToTrack(segments, inTrack, inTrackHasTimeStamps);

        return segments;
    }
}



