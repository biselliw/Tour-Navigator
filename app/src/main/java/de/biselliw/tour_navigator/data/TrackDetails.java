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
    private double _nearestDist = -1.0;


    TrackTiming _trackTiming = null;

	/**
	 * Recalculate all selection details
	 */




    public List<RecordAdapter.Record> recalculate() {
        interleaveWaypoints();
        _trackTiming = new TrackTiming(this);
        return _trackTiming.recalculate();
    }

    /**
     * Find the nearest track point to the specified Latitude and Longitude coordinates
     * within a given range of track points
     *
     * @param inStart       start index
     * @param inEnd         end index
     * @param inLatitude    Latitude in degrees
     * @param inLongitude   Longitude in degrees
     * @param inMaxDist     maximum tolerated distance [km] between geo location and point
     * @return <ul>
     * 	<li>&gt;= 0: index of nearest track point within the specified max distance</li>
     * 	<li>&lt; 0: index of nearest track point outside the specified max distance</li>
     * 	<li>&nbsp;DataPoint.INVALID_INDEX if no point is within the specified max distance </li>
     * </ul>
     * @author BiselliW
     */
    public int getNearestTrackpointIndex(int inStart, int inEnd, double inLatitude, double inLongitude, double inMaxDist) {
        // init index of the nearest track point to the specified Latitude and Longitude coordinates
        int nearestPoint = DataPoint.INVALID_INDEX;
        _nearestDist = -1.0;

        if (inStart < 0) inStart = 0;

        if (inEnd >= _numPoints - 1) inEnd = _numPoints - 1;
        for (int i = inStart; i <= inEnd; i++) {
            DataPoint point = _dataPoints[i];
            if (point.isTrackPoint()) {
                double radians = point.calculateRadiansBetween(inLatitude, inLongitude);
                double currDist = Distance.convertRadiansToDistance(radians);
                if ((currDist < _nearestDist) || (_nearestDist < 0.0)) {
                    nearestPoint = i;
                    _nearestDist = currDist;
                    if (currDist < 0.005)
                        break;
                }
            }
        }
/*
        if (DEBUG) {
            int d = (int) (_nearestDist * 1000);
            Log.d(TAG, "getNearestTrackpointIndex inStart = " + inStart + ", inEnd = " + inEnd + ": nearestPoint = " + nearestPoint + "; nearestDist = " + d + "m");
        }
 */
        // Check whether it's within required distance
        if (nearestPoint >= 0)
            if (_nearestDist <= inMaxDist)
                return nearestPoint;
            else if (nearestPoint == 0)
                // special use case: index 0 outside max distance
                return DataPoint.INVALID_INDEX;
            else
                return -nearestPoint;
        else
            return DataPoint.INVALID_INDEX;
    }

    /**
     * Return the nearest distance of a track point to the specified Latitude and Longitude coordinates.
     * Index of nearest track point must have been calculated using @see "getNearestPointIndex2()"
     *
     * @return distance of nearest track point [km], negated if not within the specified max distance
     * @author BiselliW
     * @since BiselliW
     * - all coordinates in [km]
     */
    public double getNearestDistance() {
        return _nearestDist;
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
     * @return true if successful, false otherwise
     * @author BiselliW
     */
    public boolean reverseRange(int inStart, int inEnd) {
        if (inStart < 0 || inEnd < 0 || inStart >= inEnd || inEnd >= _numPoints) {
            return false;
        }
        // calculate how many point swaps are required
        int numPointsToReverse = (inEnd - inStart + 1) / 2;
        DataPoint p;
        for (int i = 0; i < numPointsToReverse; i++) {
            // swap pairs of points
            p = _dataPoints[inStart + i];
            _dataPoints[inStart + i] = _dataPoints[inEnd - i];
            _dataPoints[inEnd - i] = p;
        }
        // adjust segment starts
        shiftSegmentStarts(inStart, inEnd);
        // Find first track point and following track point, and set segment starts to true
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
        return true;
    }

    /**
     * set a new starting point of the route
     *
     * @param inStart start index
     * @return true if successful, false otherwise
     * @author BiselliW
     */
    public boolean setNewStart(int inStart) {
        if (inStart < 0 || inStart >= _numPoints) {
            return false;
        }
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
        return true;
    }

    static DataPoint[] _waypoints;
    static int _numWaypoints;
    static int[] _pointIndices;

    /**
     * Interleave all waypoints by each nearest track point
     *
     * @implNote major changes
     * @author BiselliW
     * @since 20.2.006
     */
    public void interleaveWaypoints() {
        // Separate waypoints and find nearest track point
        _numWaypoints = 0;
        _waypoints = new DataPoint[_numPoints];
        _pointIndices = new int[_numPoints];
        if (!_scaled) scalePoints();
        if (DEBUG) Log.d(TAG, "interleaveWaypoints()");

        // find nearest track points for all way points
        DataPoint point;
        for (int i = 0; i < _numPoints; i++) {
            point = _dataPoints[i];
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
    //////// information methods /////////////

//////// information methods /////////////


    /**
     * ///////// Internal processing methods ////////////////
     * <p>
     * /**
     * Shift all the segment start flags in the given range by 1
     * Method used by reverse range and its undo
     *
     * @param inStartIndex start of range, inclusive
     * @param inEndIndex   end of range, inclusive
     * @author BiselliW
     */
    public void shiftSegmentStarts(int inStartIndex, int inEndIndex) {
        boolean prevFlag = true;
        boolean currFlag;
        for (int i = inStartIndex; i <= inEndIndex; i++) {
            DataPoint point = getPoint(i);
            if (point != null && !point.isWaypoint()) {
                // remember flag
                currFlag = point.getSegmentStart();
                // shift flag by 1
                point.setSegmentStart(prevFlag);
                prevFlag = currFlag;
            }
        }
    }

    ////////////////// Cloning and replacing ///////////////////

    /**
     * Clone the array of DataPoints
     *
     * @return shallow copy of DataPoint objects
     * @author BiselliW
     */
    public DataPoint[] cloneContents() {
        DataPoint[] clone = new DataPoint[_numPoints];
        System.arraycopy(_dataPoints, 0, clone, 0, _numPoints);
        return clone;
    }

    /**
     * Reverse the route
     *
     * @author BiselliW
     */
    public boolean reverseRoute() {
        return reverseRange(0, _numPoints - 1);
    }

    /*
     * Find the nearest track point to the specified Latitude and Longitude coordinates
     * or DataPoint.INVALID_INDEX if no
     *
     * @param inStart start index
     * @param inLatitude Latitude in degrees
     * @param inLongitude Longitude in degrees
     * @param inMaxDist maximum distance from selected coordinates [km] to point
     * @param inMaxDistDest maximum distance along the track [km] towards the destination
     * @param inJustTrackPoints true if waypoints should be ignored
     * @return index of nearest track point or <= if no point is within the specified max distanceS
     *
     * @author BiselliW
     * @since 22.2.006
     * - all coordinates in [km]
     */
    private int getNearestPointIndex(int inStart, double inLatitude, double inLongitude, double inMaxDist, double inMaxDistDest, boolean inJustTrackPoints) {
        /* index of the nearest track point to the specified Latitude and Longitude coordinates */
        int nearestPoint = 0;
        double startTrack = -1;
        _nearestDist = -1.0;

        if (inStart < 0) inStart = 0;
        if (inStart >= _numPoints - 1) {
            return DataPoint.INVALID_INDEX;
        }

        double currDist;
        for (int i = inStart; i < _numPoints; i++) {
            if (!inJustTrackPoints || !_dataPoints[i].isWaypoint()) {
                DataPoint point = _dataPoints[i];
                double radians = point.calculateRadiansBetween(inLatitude, inLongitude);
                currDist = Distance.convertRadiansToDistance(radians);
                if ((currDist < _nearestDist) || (_nearestDist < 0.0)) {
                    nearestPoint = i;
                    _nearestDist = currDist;
                    if (currDist == 0) break;
                }

                if (inMaxDistDest > 0) {
                    if (startTrack < 0)
                        startTrack = _dataPoints[i].getDistance() + inMaxDistDest;
                    else if (startTrack < _dataPoints[i].getDistance())
                        break;
                }
            }
        }

        // Check whether it's within required distance
        if ((_nearestDist > inMaxDist) && (inMaxDist > 0.0)) {
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
                    linkedTP = getNearestPointIndex(linkedTP + 1, lat, lon, 0.020, 0.0, true);
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



