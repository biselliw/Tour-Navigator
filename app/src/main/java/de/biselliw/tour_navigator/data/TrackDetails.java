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

import de.biselliw.tour_navigator.adapter.RecordAdapter;
import tim.prune.data.Distance;

import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Track;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.helpers.Log;

import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.INVALID_INDEX;
import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.INVALID_VALUE;


/**
 * class to hold all details of a track
 *
 * @author BiselliW
 */
public class TrackDetails extends Track {

    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackDetails";
	private static final boolean _DEBUG = true; // Set to true to enable logging
	private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /** maximum distance of a waypoint to the track [km] */
    protected final static double MAX_DISTANCE_WP_TRACK = 0.3;

    /** out of track distance of a waypoint to the track: scaled (0 ... 1) */
    protected final static double SCALED_DISTANCE_OUT_OF_TRACK = MAX_DISTANCE_WP_TRACK / 40000.0;
    /** maximum distance of a waypoint to the track: scaled (0 ... 1) */
    protected final static double MAX_SCALED_DISTANCE_WP_TO_TRACK = 15.0E-7;

    private boolean _hasNamedTrackpoint = false;
    private boolean _hasAltitude = false;
    private boolean _hasTimestamps = false;

    private TrackTiming _trackTiming = null;

    /** number of trackpoints - all trackpoints are arranged at the start of the track */
    protected int _numTrackPoints;
    /** number of waypoints - all waypoints are arranged after the trackpoints */
    static int _numWaypoints;

    public TrackDetails () {  }

    /**
	 * Recalculate all selection details
	 */
    public List<RecordAdapter.Record> recalculate() {
        separateWayAndTrackpoints();
        getNearestTrackpoints();
        _trackTiming = new TrackTiming();
        try {
            return _trackTiming.recalculate(this);
        }
        catch (Exception e) {
            Log.e(TAG,"No records created yet");
        }
        return null;
    }

    public List<RecordAdapter.Record> updateRecords() {
        if (_trackTiming != null)
            return _trackTiming.updateRecords();
        return null;
    }


    double min_h2 = -1.0;

    /**
     * Find the nearest track point to a given location within a given range of track points
     *
     * @param inStart     start index within the track
     * @param inEnd       end index within the track
     * @param inLatitude  Latitude of the location in degrees
     * @param inLongitude Longitude of the location in degrees
     * @param inMaxDist   maximum distance [km] between location and track
     * @return <ul>
     * 	<li>&gt;= 0: index of nearest track point within the specified max distance</li>
     * 	<li>&lt; 0: index of nearest track point outside the specified max distance</li>
     * 	<li>&nbsp;DataPoint.INVALID_INDEX if no point is within the specified max distance </li>
     * </ul>
     * check the shortest distance between location and track:
     * Within a triangle A-B-C calculate the height h from corner C projected to a line given by A and B
     * The projected x value along A - B must be < c
     * A: current point
     * B: next point
     * C: GPS location
     * a distance B - C
     * b distance A - C
     * c distance A - B
     */
    public int getNearestTrackpointIndex(int inStart, int inEnd, double inLatitude, double inLongitude, double inMaxDist) {
        // init index of the nearest track point to the specified Latitude and Longitude coordinates
        int nearestIndex = INVALID_INDEX, index_min_h2 = INVALID_INDEX;
        min_h2 = -1.0;
        if (inStart < 0) inStart = 0;
        if (inEnd >= _numPoints - 1) inEnd = _numPoints - 1;
        DataPoint A = null, B = null;
        double a = 0, b = 0, c;
        int first = inStart;
        double max_h2 = inMaxDist*inMaxDist, low_h2 = 0.005*0.005;

        while (first <= inEnd) {
            DataPoint point = _dataPoints[first];
            if (point.isTrackPoint()) {
                if (A == null) {
                    A = point;
                    b = A.distanceTo(inLatitude, inLongitude);
                }

                int next = first + 1;
                while (next <= inEnd) {
                    point = _dataPoints[next];
                    if (point.isTrackPoint()) {
                        B = point;
                        a = B.distanceTo(inLatitude, inLongitude);
                        c = A.distanceTo(B);
                        double x = (b * b - a * a + c * c) / (2 * c);
                        double cb = c / b;
                        double h2 = a * a / (2 + 2 * cb * cb);

                        if ((h2 < min_h2) || (min_h2 < 0)) {
                            if ((x >= 0) && (x <= c)) {
                                nearestIndex = first;
                                if (h2 < low_h2) {
                                    min_h2 = h2;
                                    return nearestIndex;
                                }
                            }
                            index_min_h2 = first;
                            min_h2 = h2;
                        }
                        break;
                    } else
                        next++;
                }
                // Move point A -> B
                A = B; b = a;
                first = next;
            }
        }

        // Check whether it's within required distance
        if (nearestIndex >= 0) {
            if (min_h2 <= max_h2) {
                return nearestIndex;
            } else if (nearestIndex == 0) {
                // special use case: index 0 outside max distance
                return INVALID_INDEX;
            } else {
                return -nearestIndex;
            }
        }
        else {
            if (index_min_h2 >= 0) {
                if (min_h2 <= max_h2) {
                    return index_min_h2;
                } else {
                    return -index_min_h2;
                }
            } else {
                return INVALID_INDEX;
            }
        }
    }

    /**
     * @return distance of nearest track point [km]
     */
    public double getNearestDistance() {
        if (min_h2 >= 0) {
            return Math.sqrt (min_h2); }
        else {
            return 999.9;
        }
    }

    /**
     * Find the next track point which is considered as outside of the track
     * or DataPoint.INVALID_INDEX if no
     *
     * @param inStart           start index
     * @param inEnd             end index
     * @param inLatitude        Latitude in degrees
     * @param inLongitude       Longitude in degrees
     * @param inMinDist         minimum distance from selected coordinates [km] to point
     * @param inJustTrackPoints true if waypoints should be ignored
     * @return index of the next track point which is considered as outside of the track
     * @since BiselliW
     * - all coordinates in [km]
     */
    private int getOutsidePointIndex(int inStart, int inEnd, double inLatitude, double inLongitude, double inMinDist, boolean inJustTrackPoints) {

        if (inStart < 0) inStart = 0;
        if (inStart > inEnd) return INVALID_INDEX;
        if (inEnd >= _numPoints) return INVALID_INDEX;

        for (int i = inStart; i < inEnd; i++) {
            DataPoint point = _dataPoints[i];
            if (point != null) {
                if (!inJustTrackPoints || !point.isWaypoint()) {
                    double radians = point.calculateRadiansBetween(inLatitude, inLongitude);
                    double currDist = Distance.convertRadiansToDistance(radians);
                    if (currDist > inMinDist) {
                        if (DEBUG) {
                            int d = (int) (currDist * 1000);
                            Log.d(TAG, "getOutsidePointIndex() Dist = " + d + "m");
                        }
                        return i;
                    }
                }
            }
        }

        return INVALID_INDEX;
    }

    /**
     * Delete the specified range of points from the Track
     *
     * @param inStart start of range (inclusive)
     * @param inEnd   end of range (inclusive)
     * @return true if successful
     * @author BiselliW
     */
    public boolean deleteRange(int inStart, int inEnd) {
        if (inStart < 0 || inEnd < 0 || inEnd < inStart) {
            // no valid range selected so can't delete
            return false;
        }
        // check through range to be deleted, and see if any new segment flags present
        boolean hasSegmentStart = false;
        DataPoint nextTrackPoint = getNextTrackPoint(inEnd + 1);
        if (nextTrackPoint != null) {
            for (int i = inStart; i <= inEnd && !hasSegmentStart; i++) {
                hasSegmentStart = _dataPoints[i].getSegmentStart();
            }
            // If segment break found, make sure next trackpoint also has break
            if (hasSegmentStart) {
                nextTrackPoint.setSegmentStart(true);
            }
        }
        // valid range, let's delete it
        int numToDelete = inEnd - inStart + 1;
        DataPoint[] newPointArray = new DataPoint[_numPoints - numToDelete];
        // Copy points before the selected range
        if (inStart > 0) {
            System.arraycopy(_dataPoints, 0, newPointArray, 0, inStart);
        }
        // Copy points after the deleted one(s)
        if (inEnd < (_numPoints - 1)) {
            System.arraycopy(_dataPoints, inEnd + 1, newPointArray, inStart,
                    _numPoints - inEnd - 1);
        }
        // Copy points over original array
        _dataPoints = newPointArray;
        _numPoints -= numToDelete;
        // needs to be scaled again
        _scaled = false;
        return true;
    }


    ////////////////////////////////////////

    /**
     * Reverse the specified range of points
     *
     * @param inStart start index
     * @param inEnd   end index
     */
    public void reverseRange(int inStart, int inEnd) {
        if (inStart < 0 || inEnd < 0 || inStart >= inEnd || inEnd >= _numPoints) { return; }
        /* calculate how many point swaps are required */
        int numPointsToReverse = (inEnd - inStart + 1) / 2;
        for (int i = 0; i < numPointsToReverse; i++) {
            // swap pairs of points
            DataPoint p = _dataPoints[inStart + i];
            _dataPoints[inStart + i] = _dataPoints[inEnd - i];
            _dataPoints[inEnd - i] = p;
        }
        /* adjust segment starts */
        boolean prevFlag = true;
        for (int i = inStart; i <= inEnd; i++) {
            DataPoint point = getPoint(i);
            if (point != null && !point.isWaypoint()) {
                // remember flag
                boolean currFlag = point.getSegmentStart();
                // shift flag by 1
                point.setSegmentStart(prevFlag);
                prevFlag = currFlag;
            }
        }

        /* Find first track point and following track point, and set segment starts to true */
        DataPoint firstTrackPoint = getNextTrackPoint(inStart);
        if (firstTrackPoint != null) {
            firstTrackPoint.setSegmentStart(true);
        }
        DataPoint nextTrackPoint = getNextTrackPoint(inEnd + 1);
        if (nextTrackPoint != null) {
            nextTrackPoint.setSegmentStart(true);
        }
        // needs to be scaled again
        _scaled = false;
    }

    /**
     * set a new starting point of the route
     *
     * @param inStart start index
     * @author BiselliW
     */
    public void setNewStart(int inStart) {
        if (inStart < 0 || inStart >= _numPoints) { return; }
        DataPoint[] newPointArray = new DataPoint[_numPoints];

        // calculate how many point swaps are required
        // Copy points from the new start
        System.arraycopy(_dataPoints, inStart, newPointArray, 0, _numPoints - inStart);
        // Copy points from the previous start
        System.arraycopy(_dataPoints, 0, newPointArray, _numPoints - inStart, inStart);
        // Copy points from new to current array
        System.arraycopy(newPointArray, 0, _dataPoints, 0, _numPoints);

        // needs to be scaled again
        _scaled = false;
    }

    /**
     * todo Checks if the track has waypoints, trackpoints, altitudes
     */
    public void checkProperties() {
        _hasWaypoint = _hasTrackpoint = _hasAltitude = false;
        if (_numTrackPoints > 0) {
            for (int p = 0; p < _numTrackPoints; p++) {
                DataPoint point = getPoint(p);
                if (point != null && point.isValid() && point.getAltitude() != null &&
                    point.getAltitude().isValid()) {
                        _hasAltitude = true;
                        break;
                    }
            }
            for (int p = _numTrackPoints; p < getNumPoints(); p++) {
                DataPoint point = getPoint(p);
                if (point != null && point.isValid())
                    if (point.isWaypoint()) {
                        if (p > 0 && p < getNumPoints() - 1)
                            _hasWaypoint = true;
                    } else {
                        _hasTrackpoint = true;
                    }
                if (_hasWaypoint && _hasTrackpoint)
                    break;
            }
        } else {
            for (int p = 0; p < getNumPoints(); p++) {
                DataPoint point = getPoint(p);
                if (point != null && point.isValid()) {
                    if (point.isWaypoint()) {
                        if (p > 0 && p < getNumPoints() - 1)
                            _hasWaypoint = true;
                    } else {
                        _hasTrackpoint = true;
                    }
                    if (point.getAltitude() != null && point.getAltitude().isValid())
                        _hasAltitude = true;
                }
                if (_hasWaypoint && _hasTrackpoint && _hasAltitude)
                    break;
            }
        }
    }


    /**
     * Append the given point to the end of the way point list and link nearest trackpoints to it
     * @param inWaypoint point to append
     */
    public void linkWaypoint(DataPoint inWaypoint) {
        // find nearest track points
        scalePoints();
        findNearestTrackPoints(inWaypoint);
    }

    /**
     * Rearrange points: move all waypoints behind trackpoints and ignore invalid points
     */
    private void separateWayAndTrackpoints() {
        DataPoint[] dataPoints = new DataPoint[_numPoints];
        int numDataPoints, numInvalidPoints = 0;
        ArrayList<DataPoint> wayPoints = new ArrayList<>();

        // reorder points
        _numTrackPoints = 0;
        for (int i = 0; i < _numPoints; i++) {
            DataPoint point = _dataPoints[i];
            // if point is a waypoint outside the track
            if (point.isWayPoint()) {
                wayPoints.add(point);
            }
            else {
                // app needs altitude values for trackpoints
                if (point.isValid() && point.hasAltitude()) {
                    point.setIndex(_numTrackPoints);
                    dataPoints[_numTrackPoints++] = point;
                } else {
                    // ignore invalid points
                    numInvalidPoints++;
                }
            }
        }

        // append waypoints
        numDataPoints = _numTrackPoints;
        _numWaypoints = wayPoints.size();
        for (DataPoint wayPoint : wayPoints) {
            wayPoint.setIndex(numDataPoints);
            dataPoints[numDataPoints++] = wayPoint;
        }

        wayPoints.clear();
        _dataPoints = dataPoints;
        _numPoints = numDataPoints;

        if (DEBUG) Log.d(TAG, "separateWayAndTrackpoints(): found " + _numWaypoints + " waypoints; " +
                _numTrackPoints + " trackpoints; " + numInvalidPoints + " invalid points");
    }

    /**
     * Find nearest trackpoints for all waypoints
     */
    private void getNearestTrackpoints() {
        final List<String> protectedWaypointTypes = List.of(
                "Wikipedia", "OSM", "WPT"
        );

        if (DEBUG) Log.d(TAG, "getNearestTrackpoints()");

        int waypointIndex = 0;
        boolean setStart = true;
        scalePoints();

        // find nearest trackpoints for all waypoints
        for (int i = _numTrackPoints; i < _numPoints; i++) {
            DataPoint point = _dataPoints[i];
            // remove link from track point to waypoint
            point.clearWayPointLink();
            // is it a protected waypoint which the user demanded?
            for (int j = 0; j < protectedWaypointTypes.size(); j++) {
                if (point.getWaypointType().startsWith(protectedWaypointTypes.get(j))) {
                    point.makeProtectedWaypoint();
                    break;
                }
            }
        }

        // find nearest waypoint for all trackpoints
        for (int i = 0; i < _numTrackPoints; i++) {
            DataPoint point = _dataPoints[i];
            // remove link from track point to waypoint
            point.clearWayPointLink();

            if (setStart) {
                // does the track point already have a name?
                if (point.isRoutePoint()) {
                    // this is the starting point
                    setStart = false;
                } else {
                    // name first track point as "Start"
                    point.setWaypointName(Resources.getString(R.string.start));
                    point.setLinkIndex(0);
                    setStart = false;
                }
            }
        }

        /* make last trackpoint to end point */
        DataPoint point = _dataPoints[_numTrackPoints - 1];
        if (point.getWaypointName().isEmpty()) {
            point.setWaypointName(Resources.getString(R.string.destination));
        }

        // find all nearest track points for all way points
        findNearestTrackPoints();
    }

    /**
     * Reverse the route
     */
    public void reverseRoute() {
        reverseRange(0, _numPoints - 1);
    }


    /**
     * find all nearest trackpoints for all waypoints
     *
     * @author BiselliW
     */
    void findNearestTrackPoints() {
        // for all waypoints:
        for (int j = 0; j < _numWaypoints; j++) {
            DataPoint point = getWaypoint(j);
            findNearestTrackPoints(point);
        }
    }

    /**
     * find all nearest trackpoints for a waypoint
     *
     * @author BiselliW
     */
    boolean findNearestTrackPoints(DataPoint inWaypoint) {
        boolean result = true;
        double mDist, yDist;

        if (!_scaled) {scalePoints();}

        // get the index to the waypoint within the point list
        int index_wpt = inWaypoint.getIndex();
        // get the coordinates of this waypoint
        double x_wpt, y_wpt;
        try {
            x_wpt = _xValues[index_wpt];
            y_wpt = _yValues[index_wpt];
        } catch (ArrayIndexOutOfBoundsException obe) {
            return false;
        }

        /* get the index to the first trackpoint nearest to this waypoint */
        int linkedIndexFirst = INVALID_INDEX, linkedIndexPrevious, _trackIndex = 0;
        double nearestDist = INVALID_VALUE;
        while (_trackIndex < _numTrackPoints) {
            try {
                yDist = Math.abs(_yValues[_trackIndex] - y_wpt);
                if (yDist < nearestDist || nearestDist == INVALID_VALUE) {
                    // y dist is within range, so check x too
                    mDist = yDist + getMinXDist(_xValues[_trackIndex] - x_wpt);
                    if (mDist < nearestDist || nearestDist == INVALID_VALUE) {
                        // is the corresponding trackpoint not yet linked?
                        if (_dataPoints[_trackIndex].getLinkIndex() <= 0)
                            linkedIndexFirst = _trackIndex;
                        nearestDist = mDist;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException obe) {
                return false;
            }
            _trackIndex++;
        }
        // Check whether it's within required distance
        if (!inWaypoint.isProtectedWayPoint() &&
            nearestDist > MAX_SCALED_DISTANCE_WP_TO_TRACK)
                return false; // OUT_OF_TRACK;

        // apply the found index
        if (linkedIndexFirst != INVALID_INDEX) {
            _dataPoints[linkedIndexFirst].setLinkIndex(index_wpt);
            inWaypoint.setLinkIndex(linkedIndexFirst);
            linkedIndexPrevious = linkedIndexFirst;
            _dataPoints[linkedIndexFirst].makeRoutePoint(inWaypoint.getWaypointName(),index_wpt);
        }
        else {
            return false;
        }

        /* find the next track point which is considered as outside of the track */
        _trackIndex = linkedIndexFirst;
        while (_trackIndex < _numTrackPoints) {
            int _indexNext = INVALID_INDEX;
            while (_trackIndex < _numTrackPoints) {
                try {
                    yDist = Math.abs(_yValues[_trackIndex] - y_wpt);
                    mDist = yDist + getMinXDist(_xValues[_trackIndex] - x_wpt);
                    if ((yDist > SCALED_DISTANCE_OUT_OF_TRACK) || (mDist > SCALED_DISTANCE_OUT_OF_TRACK)) {
                        // is the corresponding trackpoint not yet linked?
                        if (_dataPoints[_trackIndex].getLinkIndex() <= 0) {
                            _indexNext = _trackIndex;
                            break;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException obe) {
                    return false;
                }
                // Check whether it's within required distance
                if (!inWaypoint.isProtectedWayPoint() && nearestDist > MAX_SCALED_DISTANCE_WP_TO_TRACK)
                    return false; // OUT_OF_TRACK;
                _trackIndex++;
            }

            // none found?
            if (_indexNext == INVALID_INDEX)
                // finished : only one linked trackpoint was found
                return true;

            /* get the index to the next trackpoint nearest to this waypoint */
            int linkedIndexNext = INVALID_INDEX;
            nearestDist = INVALID_VALUE;
            while (_trackIndex < _numTrackPoints) {
                try {
                    yDist = Math.abs(_yValues[_trackIndex] - y_wpt);
                    if (yDist < nearestDist || nearestDist == INVALID_VALUE) {
                        // y dist is within range, so check x too
                        mDist = yDist + getMinXDist(_xValues[_trackIndex] - x_wpt);
                        if (mDist < nearestDist || nearestDist == INVALID_VALUE) {
                            // is the corresponding trackpoint not yet linked?
                            if (_dataPoints[_trackIndex].getLinkIndex() <= 0)
                                linkedIndexNext = _trackIndex;
                            nearestDist = mDist;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException obe) {
                    return false;
                }
                _trackIndex++;
            }
            if (nearestDist > MAX_SCALED_DISTANCE_WP_TO_TRACK)
                // finished : no further linked trackpoint was found
                return true; // OUT_OF_TRACK;

            // apply the found index
            if (linkedIndexNext != INVALID_INDEX) {
                _dataPoints[linkedIndexPrevious].setLinkIndexNext(linkedIndexNext);
                _dataPoints[linkedIndexNext].setLinkIndex(index_wpt);
                _dataPoints[linkedIndexNext].setLinkIndexNext(INVALID_INDEX);
                inWaypoint.setLinkIndexNext(linkedIndexNext);
                _dataPoints[linkedIndexNext].makeRoutePoint(inWaypoint.getWaypointName(),index_wpt);

                linkedIndexPrevious = linkedIndexNext;
            }
        }

        return result;
    }

    /**
     * @return true if track contains named trackpoints
     * @author BiselliW
     */
    public boolean hasNamedTrackpoints() {
        if (!_scaled) {
            _hasNamedTrackpoint = false;
            scalePoints();
        }

        if (!_hasNamedTrackpoint) {
            for (int p = 0; p < _numPoints; p++) {
                DataPoint point = getPoint(p);
                if (point != null && point.isValid()) {
                    if (!point.isWaypoint() &&
                            point.isNamedTrackpoint()) {
                        _hasNamedTrackpoint = true;
                        break;
                    }
                }
            }
        }
        return _hasNamedTrackpoint;
    }

    /**
     * @return true if track contains trackpoints with altitude
     * @author BiselliW
     */
    public boolean hasAltitudes() {
        if (!_scaled) {
            _hasAltitude = false;
            scalePoints();
        }
        if (!_hasAltitude) {
            for (int p = 0; p < _numPoints; p++) {
                DataPoint point = getPoint(p);
                if (point != null && point.isValid() && point.hasAltitude() )
                {
                    _hasAltitude = true;
                    break;
                }
            }
        }
        return _hasAltitude;
    }

    /**
     * @return true if track contains trackpoints with time stamps
     * @author BiselliW
     */
    public boolean hasTimestamps() {
        if (!_hasTimestamps) {
            for (int p = 1; p < _numPoints; p++) {
                DataPoint point = getPoint(p);
                if (point != null && point.isValid() && point.hasTimestamp())
                {
                    _hasTimestamps = true;
                    break;
                }
            }
        }
        return _hasTimestamps;
    }

    /**
     * Delete all points
     *
     * @return true if successful
     * @author BiselliW
     */
    public boolean deleteAllPoints() {
        _dataPoints = new DataPoint[0];
        _numPoints = 0;
        // needs to be scaled again
        _scaled = false;
        return true;
    }

    public boolean isValid() {
        return hasAltitudes() && (hasWaypoints() || hasNamedTrackpoints());
    }

    /** Get the number of trackpoints
     * @return number of trackpoints
     * @implNote all trackpoints are arranged at the start of the track
     */
    public int getNumTrackPoints() { return _numTrackPoints; }

    /** Get the number of waypoints
     * @return number of waypoints
     * @implNote all waypoints are arranged after the trackpoints
     */
    public int getNumWayPoints() { return _numWaypoints; }

    /**
     * Return a list of waypoints which have been excluded before
     * @return list of waypoints
     */
    public ArrayList<DataPoint> getWayPointsOutOfTrack() {
        ArrayList<DataPoint> _wayPointsOutOfTrack = new ArrayList<>();
        if (_numPoints > 0) {
            try {
                // check for way points without link index
                for (int i = _numTrackPoints; i < getNumPoints(); i++) {
                    DataPoint point = getPoint(i);
                    if (point != null && point.isValid() && point.getLinkIndex() < 0)
                        _wayPointsOutOfTrack.add(_dataPoints[_numTrackPoints + i]);
                }
            } catch (Exception ignored) {
                _wayPointsOutOfTrack = null;
            }
        }
        return  _wayPointsOutOfTrack;
    }

    public boolean hasWayPointsOutOfTrack() {
        ArrayList<DataPoint> list = getWayPointsOutOfTrack();
        return (list != null) && !list.isEmpty();
    }

    public boolean isValidRecordedTrackFile() {
        return hasAltitudes() && hasTimestamps();
    }

    public int getSegmentsCount() {
        if (_trackTiming != null)
            return _trackTiming.getSegmentsCount();
        else
            return 0;
    }

    public Segment getSegment(int inIndex) {
        if (_trackTiming != null)
            return _trackTiming.getSegment(inIndex);
        else
            return null;
    }

    public double getSegmentStartDistance(int inIndex) {
        if (_trackTiming != null)
            return _trackTiming.getSegmentStartDistance(inIndex);
        else
            return 0.0;
    }

    public double getSegmentEndDistance(int inIndex) {
        if (_trackTiming != null)
            return _trackTiming.getSegmentEndDistance(inIndex);
        else
            return 0.0;
    }
    public double getSegmentStartElevation(int inIndex) {
        if (_trackTiming != null)
            return _trackTiming.getSegmentStartElevation(inIndex);
        else
            return 0.0;
    }

    public double getSegmentEndElevation(int inIndex) {
        if (_trackTiming != null)
            return _trackTiming.getSegmentEndElevation(inIndex);
        else
            return 0.0;
    }

    public DataPoint getWaypoint (int inIndex) {
        inIndex += _numTrackPoints;
        if (inIndex < _numPoints)
            return _dataPoints[inIndex];
        else
            return null;
    }
}