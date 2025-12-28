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

    Copyright 2025 Walter Biselli (BiselliW)
*/

import java.util.List;

import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import tim.prune.data.Distance;
import tim.prune.data.FieldList;
import tim.prune.data.PointCreateOptions;

import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Track;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.helpers.Log;


/**
 * class to hold all details of a track
 *
 * @author BiselliW
 */
public class TrackDetails extends Track {

    public String Creator = App.resources.getString(R.string.app_name);
    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackDetails";
	private static final boolean _DEBUG = false; // Set to true to enable logging
	private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /* maximum distance of a waypoint to the track */
    protected final static double MAX_DISTANCE_WP_TRACK = 0.3;


    private boolean _hasNamedTrackpoint = false;
    private boolean _hasAltitude = false;
    /**
     * Nearest distance of a track point to the specified Latitude and Longitude coordinates
     */
    private double _nearestDist_km = -1.0;


    TrackTiming _trackTiming = null;

	/**
	 * Recalculate all selection details
	 */
    public List<RecordAdapter.Record> recalculate() {
        interleaveWaypoints();
        _trackTiming = new TrackTiming(this);
        try {
            return _trackTiming.recalculate();
        }
        catch (Exception e) {
            Log.e(TAG,"No records created yet");
        }
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
        int nearestIndex = DataPoint.INVALID_INDEX;
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
                            min_h2 = h2;
                            if ((x >= 0) && (x <= c)) {
                                nearestIndex = first;
                                if (min_h2 < low_h2) {
                                    return nearestIndex;
                                }
                            }
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
        if (nearestIndex >= 0)
            if (min_h2 <= max_h2)
                return nearestIndex;
            else if (nearestIndex == 0)
                // special use case: index 0 outside max distance
                return DataPoint.INVALID_INDEX;
            else
                return -nearestIndex;
        else
            return DataPoint.INVALID_INDEX;
    }

    /**
     * @return distance of nearest track point [km]
     */
    public double getNearestDistance() {
        if (min_h2 >= 0)
            return Math.sqrt (min_h2);
        else
            return 999.9;
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
    public int getOutsidePointIndex(int inStart, int inEnd, double inLatitude, double inLongitude, double inMinDist, boolean inJustTrackPoints) {

        if (inStart < 0) inStart = 0;
        if (inStart > inEnd) return DataPoint.INVALID_INDEX;
        if (inEnd >= _numPoints) return DataPoint.INVALID_INDEX;

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

        return DataPoint.INVALID_INDEX;
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
     * Load method, for initialising and reinitialising data
     *
     * @param inFieldArray array of Field objects describing fields
     * @param inPointArray 2d object array containing data
     * @param inOptions    load options such as units
     * @implNote bugfix of outdooractive GPX tracks with GPX coordinates as track point names
     * @author BiselliW
     */
    public void load(Field[] inFieldArray, Object[][] inPointArray, PointCreateOptions inOptions) {
        if (DEBUG) Log.d(TAG, "Loaded");
        if (inFieldArray == null || inPointArray == null) {
            _numPoints = 0;
            return;
        }
        // copy field list
        _masterFieldList = new FieldList(inFieldArray);
        // make DataPoint object from each point in inPointList
        _dataPoints = new DataPoint[inPointArray.length];
        int pointIndex = 0;
        for (Object[] objects : inPointArray) {
            // Convert to DataPoint objects
            DataPoint point = new DataPoint((String[]) objects, _masterFieldList, inOptions);
            if (point.isValid()) {
                // bugfix of outdooractive GPX tracks with GPX coordinates as track point names
                String _pointName = point.getWaypointName();
                if (_pointName.length() > 6) {
                    _pointName = _pointName.substring(0, 6);
                    try {
                        double lat = Double.parseDouble(_pointName);
                        if ((lat >= 0) && point.getWaypointSymbol().isEmpty())
                            // remove same name as in previous track point
                            point.setWaypointName("");
                    } catch (NumberFormatException ignored) {
                    }
                }
                _dataPoints[pointIndex] = point;
                pointIndex++;
            }
        }
        _numPoints = pointIndex;
        // Set first track point to be start of segment
        DataPoint firstTrackPoint = getNextTrackPoint(0);
        if (firstTrackPoint != null) {
            firstTrackPoint.setSegmentStart(true);
        }
        // needs to be scaled
        _scaled = false;
    }


    /**
     * Load the track by transferring the contents from a loaded Track object
     *
     * @param inOther Track object containing loaded data
     * @author BiselliW
     */
    public void load(TrackDetails inOther) {
        _numPoints = inOther._numPoints;
        _masterFieldList = inOther._masterFieldList;
        _dataPoints = inOther._dataPoints;
        // needs to be scaled
        _scaled = false;
    }

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
// todo        UpdateMessageBroker.informSubscribers();
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
// todo        UpdateMessageBroker.informSubscribers();
    }

    static DataPoint[] _waypoints;
    static int _numWaypoints;
    static int[] _pointIndices;

    /**
     * Interleave all waypoints by each nearest track point
     */
    public void interleaveWaypoints() {
        // Separate waypoints and find nearest track point
        _numWaypoints = 0;
        _waypoints = new DataPoint[_numPoints];
        _pointIndices = new int[_numPoints];
        if (!_scaled) scalePoints();
        if (DEBUG) Log.d(TAG, "interleaveWaypoints()");

        // find nearest track points for all way points
        for (int i = 0; i < _numPoints; i++) {
            DataPoint point = _dataPoints[i];
            // remove link from track point to way point
            point.clearWayPointLink();
            // if point is a way point outside the track
            if (point.isWayPoint()) {
                // find nearest track point
                _waypoints[_numWaypoints] = point;
                _waypoints[_numWaypoints].clearWayPointLink();
                _pointIndices[_numWaypoints] = getNearestPointIndex(_xValues[i], _yValues[i], 15.0E-7, true);
                _numWaypoints++;
            }
        }
        // Exit if data not mixed
        if (_numWaypoints == _numPoints) return;

        // Loop round points copying to correct order
        reorderPoints();

        // make last trackpoint to end point
        makeEndPoint();

        // find all nearest track points for all way points
        findNearestTrackPoints();

        // needs to be scaled again to recalc x, y
        _scaled = false;
// todo        UpdateMessageBroker.informSubscribers();
    }

    /**
     * Clone the array of DataPoints
     *
     * @return shallow copy of DataPoint objects
     */
    public DataPoint[] cloneContents() {
        DataPoint[] clone = new DataPoint[_numPoints];
        System.arraycopy(_dataPoints, 0, clone, 0, _numPoints);
        return clone;
    }

    /**
     * Reverse the route
     *
     */
    public void reverseRoute() {
        reverseRange(0, _numPoints - 1);
    }

    /**
     * Find the nearest track point to a given location
     *
     * @param inStart start index within the track
     * @param inLatitude Latitude of the location in degrees
     * @param inLongitude Longitude of the location in degrees
     * @param inMaxDist maximum distance [km] between location and track
     * @return index of nearest track point / INVALID_INDEX if no point is within the specified max distanceS
     *
     * - all coordinates in [km]
     */
    private int getNearestPointIndex(int inStart, double inLatitude, double inLongitude, double inMaxDist) {
        /* index of the nearest track point to the specified Latitude and Longitude coordinates */
        int nearestPoint = 0;
        double nearestDist_km = -1.0;
        if (inStart < 0) inStart = 0;
        if (inStart >= _numPoints - 1) { return DataPoint.INVALID_INDEX; }
        for (int i = inStart; i < _numPoints; i++) {
            if (!_dataPoints[i].isWaypoint()) {
                DataPoint point = _dataPoints[i];
                double radians = point.calculateRadiansBetween(inLatitude, inLongitude);
                double currDist_km = Distance.convertRadiansToDistance(radians);
                if ((currDist_km < nearestDist_km) || (nearestDist_km < 0.0)) {
                    nearestPoint = i;
                    nearestDist_km = currDist_km;
                    if (currDist_km == 0) break;
                }
            }
        }

        // Check whether it's within required distance
        if ((nearestDist_km > inMaxDist) && (inMaxDist > 0.0)) {
            return -nearestPoint;
        }
        return nearestPoint;
    }

    /**
     * Loop round points copying to correct order
     * @author BiselliW
     */
    void reorderPoints() {
        DataPoint[] dataCopy = new DataPoint[_numPoints];
        int copyIndex = 0;
        boolean setStart = true;
        DataPoint point;
        for (int i = 0; i < _numPoints; i++) {
            point = _dataPoints[i];
            // if it's a track point, copy it
            if (!point.isWayPoint()) {
                dataCopy[copyIndex] = point;
                copyIndex++;
                if (setStart) {
                    if (point.isRoutePoint()) {
                        setStart = false;
                    } else {
                        // name first track point as "Start"
                        point.setWaypointName(App.resources.getString(R.string.start));
                        point.setLinkIndex(0);
                        setStart = false;
                    }
                } else {
                    // check for way points with this index
                    boolean foundWP = false;
                    int linkedTP = DataPoint.INVALID_INDEX;
                    for (int j = 0; j < _numWaypoints; j++) {
                        if ((_pointIndices[j] >= 0) && (_pointIndices[j] <= i)) {
                            /*
                             *  is this way point the nearest to the track point?
                             *  - link the track point to this way point
                             */
                            if (_waypoints[j] != null) {
                                if (!foundWP) {
                                    foundWP = true;
                                    linkedTP = copyIndex - 1;
                                    dataCopy[linkedTP].makeRoutePoint(_waypoints[j].getWaypointName(), copyIndex);
                                }
                                _waypoints[j].setLinkIndex(linkedTP);
                                // else link the following track point to this way point
                                _pointIndices[j] = DataPoint.INVALID_INDEX;

                                dataCopy[copyIndex] = _waypoints[j];
                                copyIndex++;
                            } else
                                _waypoints[j] = null;
                        }
                    }
                }
            }
        }

        // check for way points without index
        for (int j = 0; j < _numWaypoints; j++) {
            if (_pointIndices[j] != DataPoint.INVALID_INDEX) {
                dataCopy[copyIndex] = _waypoints[j];
                copyIndex++;
            }
        }
        // Copy data back to track
        _dataPoints = dataCopy;
    }


    /**
     * make last trackpoint to end point
     *
     * @author BiselliW
     * @since 22.2.006
     */
    private void makeEndPoint() {
        int endPoint = 0;
        DataPoint point;

        for (int i = 0; i < _numPoints; i++) {
            point = _dataPoints[i];
            if (!point.isWayPoint()) {
                endPoint = i;
            }
        }

        point = _dataPoints[endPoint];
        if (point.getWaypointName().isEmpty()) {
            point.setWaypointName(App.resources.getString(R.string.destination));
        }
    }

    /**
     * find all nearest track points for all way points
     *
     * @author BiselliW
     */
    void findNearestTrackPoints() {
        DataPoint point = null;
        for (int j = 0; j < _numWaypoints; j++) {
            int linkedTP, prevLinkedTP=0, linkedWP;
            point = _waypoints[j];
            linkedWP = getPointIndex(point);
            double lat = point.getLatitude().getDouble();
            double lon = point.getLongitude().getDouble();
            // get the index to the currently linked track point
            linkedTP = point.getLinkIndex();
            while ((linkedTP > prevLinkedTP) && (linkedTP < _numPoints)) {
                prevLinkedTP = linkedTP;
                // find the next track point which is considered as outside of the track
                linkedTP = getOutsidePointIndex(linkedTP + 1, _numPoints - 1, lat, lon, MAX_DISTANCE_WP_TRACK, true);

                // find the next track point after this one which is considered as inside the track again
                if (linkedTP > prevLinkedTP) {
                    prevLinkedTP = linkedTP;
                    linkedTP = getNearestPointIndex(linkedTP + 1, lat, lon, 0.020);
                    while ((linkedTP > prevLinkedTP) && (linkedTP < _numPoints - 1)) {
                        prevLinkedTP = linkedTP;
                        point = _dataPoints[linkedTP];
                        if (point != null) {
                            if ((point.isTrackPoint()) && (point.getLinkIndex() <= 0)) {
                                point.makeRoutePoint(_waypoints[j].getWaypointName(), linkedWP);
                                break;
                            }
                        }
                    }
                } else
                    break;
            }
        }
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
                if (point != null && point.isValid()) {
                    if (point.hasAltitude()){
                        _hasAltitude = true;
                        break;
                    }
                }
            }
        }
        return _hasAltitude;
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

}



