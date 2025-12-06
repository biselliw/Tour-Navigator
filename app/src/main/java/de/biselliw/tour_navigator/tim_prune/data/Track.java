package de.biselliw.tour_navigator.tim_prune.data;
/*
 * This file is part of GpsPrune
 *
 * GpsPrune is a tool to visualize, edit, convert and prune GPS data
 * Please see the included readme.txt or https://activityworkshop.net
 * This software is copyright activityworkshop.net 2006-2022 and made available through the Gnu GPL version 2.
 * For license details please see the included license.txt.
 * 
 * modified by Walter Biselli (BiselliW):
 * v. 22.2.006 - 2022-11-29
 *             - new functions: getOutsidePointIndex(), getNearestPointIndex()
 */
import java.util.List;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.tim.prune.data.Distance;
import de.biselliw.tour_navigator.tim.prune.data.DoubleRange;
import de.biselliw.tour_navigator.tim.prune.data.FieldList;
import de.biselliw.tour_navigator.tim.prune.data.PointCreateOptions;
import de.biselliw.tour_navigator.tim_prune.UpdateMessageBroker;
import de.biselliw.tour_navigator.tim_prune.gui.MapUtils.MapUtils;
import de.biselliw.tour_navigator.helpers.Log;

/**
 * Class to hold all track information,
 * including track points and waypoints
 * @since 26.1
 * @author tim.prune
 * @author BiselliW
 * @implNote BiselliW
 */
public class Track {
    /**
     * TAG for log messages.
     */
    static final String TAG = "Track";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /* maximum distance of a waypoint to the track */
    private final static double MAX_DISTANCE_WP_TRACK = 0.3;

    // Data points
    private DataPoint[] _dataPoints;
    // Scaled x, y values
    private double[] _xValues = null;
    private double[] _yValues = null;
    private boolean _scaled = false;
    private int _numPoints;
    private boolean _hasTrackpoint = false;
    private boolean _hasNamedTrackpoint = false;
    private boolean _hasWaypoint = false;
    private boolean _hasAltitude = false;
    /**
     * Nearest distance of a track point to the specified Latitude and Longitude coordinates
     */
    private double _nearestDist = -1.0;
    // Master field list
    private FieldList _masterFieldList;
    // variable ranges
    private DoubleRange _latRange = null, _longRange = null;
    private DoubleRange _xRange = null, _yRange = null;


    /**
     * Constructor for empty track
     *
     * @implNote BiselliW
     */
    public Track() {
        // create field list
        _masterFieldList = new FieldList();
        // make empty DataPoint array
        _dataPoints = new DataPoint[0];
        _numPoints = 0;
        requestRescale();
    }

    /**
     * Constructor using fields and points from another Track
     *
     * @param inFieldList Field list from another Track object
     * @param inPoints    (edited) point array
     * @implNote BiselliW
     */
    public Track(FieldList inFieldList, DataPoint[] inPoints) {
        _masterFieldList = inFieldList;
        _dataPoints = inPoints;
        if (_dataPoints == null) {
            _dataPoints = new DataPoint[0];
        }
        _numPoints = _dataPoints.length;
        requestRescale();
    }

    /**
     * Request that a rescale be done to recalculate derived values
     */
    public void requestRescale() {
        _scaled = false;
    }

    /**
     * Extend the track's field list with the given additional fields
     *
     * @param inFieldList list of fields to be added
     */
    private void extendFieldList(FieldList inFieldList) {
        _masterFieldList = _masterFieldList.merge(inFieldList);
    }

    ////////////////// Modification methods //////////////////////


    /**
     * Crop the track to the given size - subsequent points are not (yet) deleted
     *
     * @param inNewSize new number of points in track
     */
    public void cropTo(int inNewSize) {
        if (inNewSize >= 0 && inNewSize < getNumPoints()) {
            _numPoints = inNewSize;
            // needs to be scaled again
            _scaled = false;
            UpdateMessageBroker.informSubscribers();
        }
    }

    /**
     * Delete the specified point
     *
     * @param inIndex point index
     * @return true if successful
     */
    public boolean deletePoint(int inIndex) {
        DataPoint point = getPoint(inIndex);
        if (point == null) {
            return false;
        }
        if (point.getSegmentStart()) {
            DataPoint nextTrackPoint = getNextTrackPoint(inIndex + 1);
            if (nextTrackPoint != null) {
                nextTrackPoint.setSegmentStart(true);
            }
        }
        DataPoint[] newPointArray = new DataPoint[_numPoints - 1];
        // Copy points before the selected point
        if (inIndex > 0) {
            System.arraycopy(_dataPoints, 0, newPointArray, 0, inIndex);
        }
        // Copy points after
        int numAfter = _numPoints - inIndex - 1;
        if (numAfter > 0) {
            System.arraycopy(_dataPoints, inIndex + 1, newPointArray, inIndex, numAfter);
        }
        // Copy points over original array
        _dataPoints = newPointArray;
        _numPoints--;
        // needs to be scaled again
        _scaled = false;
        return true;
    }

    /**
     * Rearrange all the points in the track according to the given list
     *
     * @param inIndexes point indexes for new ordering
     */
    public boolean rearrangePoints(List<Integer> inIndexes) {
        if (inIndexes == null || inIndexes.size() != _numPoints) {
            return false;
        }
        DataPoint[] newPointArray = new DataPoint[_numPoints];
        // Move points around
        for (int i = 0; i < _numPoints; i++) {
            newPointArray[i] = getPoint(inIndexes.get(i));
        }
        // Copy array references
        _dataPoints = newPointArray;
        _scaled = false;
        return true;
    }


    //////// information methods /////////////


    /**
     * Get the point at the given index
     *
     * @param inPointNum index number, starting at 0
     * @return DataPoint object, or null if out of range
     */
    public DataPoint getPoint(int inPointNum) {
        if (inPointNum > -1 && inPointNum < getNumPoints()) {
            return _dataPoints[inPointNum];
        }
        return null;
    }

    /**
     * @return the number of (valid) points in the track
     */
    public int getNumPoints() {
        return _numPoints;
    }

    /**
     * @return The range of x values as a DoubleRange object
     */
    public DoubleRange getXRange() {
		if (!_scaled) {scalePoints();}
        return _xRange;
    }

    /**
     * @return The range of y values as a DoubleRange object
     */
    public DoubleRange getYRange() {
		if (!_scaled) {scalePoints();}
        return _yRange;
    }

    /**
     * @return The range of lat values as a DoubleRange object
     */
    public DoubleRange getLatRange() {
		if (!_scaled) {scalePoints();}
        return _latRange;
    }

    /**
     * @return The range of lon values as a DoubleRange object
     */
    public DoubleRange getLonRange() {
		if (!_scaled) {scalePoints();}
        return _longRange;
    }

    /**
     * @param inPointNum point index, starting at 0
     * @return scaled x value of specified point
     */
    public double getX(int inPointNum) {
		if (!_scaled) {scalePoints();}
        return _xValues[inPointNum];
    }

    /**
     * @param inPointNum point index, starting at 0
     * @return scaled y value of specified point
     */
    public double getY(int inPointNum) {
		if (!_scaled) {scalePoints();}
        return _yValues[inPointNum];
    }

    /**
     * @return the master field list
     */
    public FieldList getFieldList() {
        return _masterFieldList;
    }


    /**
     * Checks if any data exists for the specified field
     *
     * @param inField Field to examine
     * @return true if data exists for this field
     */
    public boolean hasData(Field inField) {
        return hasData(inField, 0, _numPoints - 1);
    }


    /**
     * Checks if any data exists for the specified field in the specified range
     *
     * @param inField Field to examine
     * @param inStart start of range to check
     * @param inEnd   end of range to check (inclusive)
     * @return true if data exists for this field
     */
    public boolean hasData(Field inField, int inStart, int inEnd) {
        // Loop over selected point range
        for (int i = inStart; i <= inEnd; i++) {
            DataPoint point = getPoint(i);
            if (point == null) {
                continue;
            }
            final boolean hasValue;
            if (inField == Field.ALTITUDE) {
                hasValue = point.hasAltitude();
            } else if (inField == Field.TIMESTAMP) {
                hasValue = point.hasTimestamp();
            } else if (inField == Field.PHOTO) {
                hasValue = point.getPhoto() != null;
            } else if (inField == Field.AUDIO) {
                hasValue = point.getAudio() != null;
            } else {
                hasValue = point.getFieldValue(inField) != null;
            }
            if (hasValue) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if track has altitude data
     */
    public boolean hasAltitudeData() {
        return hasData(Field.ALTITUDE);
    }

    /**
     * @return true if track contains at least one trackpoint
     */
    public boolean hasTrackPoints() {
		if (!_scaled) {scalePoints();}
        return _hasTrackpoint;
    }

    /**
     * @return true if track contains waypoints
     */
    public boolean hasWaypoints() {
		if (!_scaled) {scalePoints();}
        return _hasWaypoint;
    }

    /**
     * Collect all the waypoints into the given List
     *
     * @param inList List to fill with waypoints
     */
    public void getWaypoints(List<DataPoint> inList) {
        // clear list
        inList.clear();
        // loop over points and copy all waypoints into list
        for (int i = 0; i <= _numPoints - 1; i++) {
            if (_dataPoints[i] != null && _dataPoints[i].isWaypoint()) {
                inList.add(_dataPoints[i]);
            }
        }
    }


    /**
     * Search for the given Point in the track and return the index
     *
     * @param inPoint Point to look for
     * @return index of Point, if any or DataPoint.INVALID_INDEX if not found
     * @implNote BiselliW
     */
    public int getPointIndex(DataPoint inPoint) {
        if (inPoint != null) {
            // Loop over points in track
            for (int i = 0; i <= _numPoints - 1; i++) {
                if (_dataPoints[i] == inPoint) {
                    return i;
                }
            }
        }
        // not found
        return DataPoint.INVALID_INDEX;
    }

    /**
     * @return true if all the points in the track have the same source file
     */
    public boolean hasSingleSourceFile() {
        SourceInfo prevInfo = null;
        for (int p = 0; p < getNumPoints(); p++) {
            DataPoint point = getPoint(p);
            SourceInfo info = (point == null ? null : point.getSourceInfo());
            if (info == null || (prevInfo != null && info != prevInfo)) {
                return false;
            }
            if (info.getFile() == null) {
                return false;
            }
            prevInfo = info;
        }
        return true;
    }

    ///////// Internal processing methods ////////////////


    /**
     * Scale all the points in the track to gain x and y values
     * ready for plotting
     *
     * @implNote BiselliW range values are relative (NOT in km!)
     */
    private synchronized void scalePoints() {
        // Loop through all points in track, to see limits of lat, long
        _longRange = new DoubleRange();
        _latRange = new DoubleRange();
		_hasWaypoint = false; _hasTrackpoint = false;
        _hasNamedTrackpoint = false;
        _hasAltitude = false;
        for (int p = 0; p < getNumPoints(); p++) {
            DataPoint point = getPoint(p);
            if (point != null && point.isValid()) {
                _longRange.addValue(point.getLongitude().getDouble());
                _latRange.addValue(point.getLatitude().getDouble());
                if (point.isWaypoint()) {
                    if (p > 0 && p < getNumPoints() - 1)
                        _hasWaypoint = true;
                } else {
                    _hasTrackpoint = true;
                    if (point.isNamedTrackpoint())
                        _hasNamedTrackpoint = true;
                    if (point.hasAltitude())
                        _hasAltitude = true;
                }
            }
        }

        // Loop over points and calculate scales
        _xValues = new double[getNumPoints()];
        _yValues = new double[getNumPoints()];
        _xRange = new DoubleRange();
        _yRange = new DoubleRange();
        for (int p = 0; p < getNumPoints(); p++) {
            DataPoint point = getPoint(p);
            if (point != null) {
                _xValues[p] = MapUtils.getXFromLongitude(point.getLongitude().getDouble());
                _xRange.addValue(_xValues[p]);
                _yValues[p] = MapUtils.getYFromLatitude(point.getLatitude().getDouble());
                _yRange.addValue(_yValues[p]);
            }
        }
        _scaled = true;
    }

    /**
     * Find the nearest track point to the specified point
     *
     * @param inPointIndex index of point within track
     * @return point index of nearest track point
     */
    public int getNearestTrackPointIndex(int inPointIndex) {
        return getNearestPointIndex(_xValues[inPointIndex], _yValues[inPointIndex], -1.0, true);
    }

	/**
	 * Find the nearest point to the specified x and y coordinates
	 * or -1 if no point is within the specified max distance
	 * @param inX x coordinate
	 * @param inY y coordinate
	 * @param inMaxDist maximum distance from selected coordinates
	 * @param inJustTrackPoints true if waypoints should be ignored
	 * @return index of nearest point or -1 if not found
	 */
	public int getNearestPointIndex(double inX, double inY, double inMaxDist, boolean inJustTrackPoints)
	{
		int nearestPoint = 0;
		double nearestDist = -1.0;
		double mDist, yDist;
		try {
			for (int i=0; i < getNumPoints(); i++)
			{
				if (!inJustTrackPoints || !_dataPoints[i].isWaypoint())
				{
					yDist = Math.abs(_yValues[i] - inY);
					if (yDist < nearestDist || nearestDist < 0.0)
					{
						// y dist is within range, so check x too
						mDist = yDist + getMinXDist(_xValues[i] - inX);
						if (mDist < nearestDist || nearestDist < 0.0)
						{
							nearestPoint = i;
							nearestDist = mDist;
						}
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException obe) {
			return -1; // probably moving the mouse while data is changing
		}
		// Check whether it's within required distance
		if (nearestDist > inMaxDist && inMaxDist > 0.0) {
			return -1;
		}
		return nearestPoint;
	}

    /**
     * Find the nearest point to the specified x and y coordinates
     * or -1 if no point is within the specified max distance
     *
     * @param inX               x coordinate: scaled value from 0 to 1
     * @param inY               y coordinate: scaled value from 0 to 1
     * @param inMaxDist         maximum distance from selected coordinates: scaled value from 0 to 1
     * @param inJustTrackPoints true if waypoints should be ignored
     * @return index of nearest point or -1 if not found
     * @implNote BiselliW
     */
    public int _getNearestPointIndex(double inX, double inY, double inMaxDist, boolean inJustTrackPoints) {
        int nearestPoint = -1;
//		double nearestDist = -1.0;
        double mDist, xDist = 0, yDist = 0;
        double nearestSqDist = -1.0, maxSqDist = inMaxDist * inMaxDist;
        try {
            for (int i = 0; i < getNumPoints(); i++) {
                if (!inJustTrackPoints || !_dataPoints[i].isWaypoint()) {
                    double mSqDist, xSqDist, ySqDist;
                    xDist = Math.abs(_xValues[i] - inX);
                    yDist = Math.abs(_yValues[i] - inY);
                    if ((xDist < inMaxDist) && (yDist < inMaxDist)) {
                        xSqDist = xDist * xDist;
                        ySqDist = yDist * yDist;
                        mSqDist = xSqDist + ySqDist;
                        if ((mSqDist < nearestSqDist) || (nearestSqDist < 0.0)) {
                            nearestPoint = i;
                            nearestSqDist = mSqDist;
                        }
                    }
                } else if ((xDist > inMaxDist) && (yDist > inMaxDist) && (nearestSqDist > 0.0)) {
                    break;
                }
            }
        } catch (ArrayIndexOutOfBoundsException obe) {
            return -1; // probably moving the mouse while data is changing
        }
        // Check whether it's within required distance
        if ((nearestSqDist > maxSqDist) && (inMaxDist > 0.0)) {
            return -1;
        }
        if (nearestPoint < 0)
            return -1;

        return nearestPoint;
    }

    /**
     * @param inX x value of point
     * @return minimum wrapped value
     */
    private static double getMinXDist(double inX) {
        return Math.min(Math.min(Math.abs(inX), Math.abs(inX - 1.0)), Math.abs(inX + 1.0));
    }

    /**
     * Get the next track point starting from the given index
     *
     * @param inStartIndex index to start looking from
     * @return next track point, or null if end of data reached
     */
    public DataPoint getNextTrackPoint(int inStartIndex) {
        return getNextTrackPoint(inStartIndex, _numPoints, true);
    }

    /**
     * Get the next track point starting from the given index
     *
     * @param inStartIndex index to start looking from
     * @param inEndIndex   index to stop looking (inclusive)
     * @return next track point, or null if end of data reached
     */
    public DataPoint getFirstTrackPointBetween(int inStartIndex, int inEndIndex) {
        return getNextTrackPoint(inStartIndex, inEndIndex, true);
    }

    /**
     * Get the previous track point starting from the given index
     *
     * @param inStartIndex index to start looking from
     * @return next track point, or null if end of data reached
     */
    public DataPoint getPreviousTrackPoint(int inStartIndex) {
        // end index is given as _numPoints but actually it just counts down to -1
        return getNextTrackPoint(inStartIndex, _numPoints, false);
    }

    /**
     * Get the next track point starting from the given index
     *
     * @param inStartIndex index to start looking from
     * @param inEndIndex   index to stop looking (inclusive)
     * @param inCountUp    true for next, false for previous
     * @return next track point, or null if end of data reached
     */
    private DataPoint getNextTrackPoint(int inStartIndex, int inEndIndex, boolean inCountUp) {
        int increment = inCountUp ? 1 : -1;
        for (int i = inStartIndex; i <= inEndIndex; i += increment) {
            DataPoint point = getPoint(i);
            // Exit if end of data reached - there wasn't a track point
            if (point == null) {
                return null;
            }
            if (point.isValid() && !point.isWaypoint()) {
                // next track point found
                return point;
            }
        }
        return null;
    }

    /**
     * @param inStartIndex start index of range
     * @param inEndIndex   end index of range
     * @return true if there are any track points in this range
     */
    public boolean isTrackPointWithin(int inStartIndex, int inEndIndex) {
        for (int i = inStartIndex; i <= inEndIndex; i++) {
            if (!getPoint(i).isWaypoint()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param inStartIndex start index of range
     * @param inEndIndex   end index of range
     * @return true if there are any segment breaks in this range
     */
    public boolean isSegmentBreakWithin(int inStartIndex, int inEndIndex) {
        for (int i = inStartIndex; i <= inEndIndex; i++) {
            DataPoint point = getPoint(i);
            if (!point.isWaypoint() && point.getSegmentStart()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Append the given point to the end of the track
     *
     * @param inPoint point to append
     * @return true if successful
     */
    public boolean appendPoint(DataPoint inPoint) {
        return insertPoint(inPoint, _numPoints);
    }

    /**
     * Re-insert the specified point at the given index
     *
     * @param inPoint point to insert
     * @param inIndex index at which to insert the point
     * @return true if it worked, false otherwise
     */
    public boolean insertPoint(DataPoint inPoint, int inIndex) {
        if (inIndex > _numPoints || inPoint == null) {
            return false;
        }
        // Make new array to copy points over to
        DataPoint[] newPointArray = new DataPoint[_numPoints + 1];
        if (inIndex > 0) {
            System.arraycopy(_dataPoints, 0, newPointArray, 0, inIndex);
        }
        newPointArray[inIndex] = inPoint;
        if (inIndex < _numPoints) {
            System.arraycopy(_dataPoints, inIndex, newPointArray, inIndex + 1, _numPoints - inIndex);
        }
        // Change over to new array
        _dataPoints = newPointArray;
        _numPoints++;
        extendFieldList(inPoint.getFieldList());
        // needs to be scaled again
        _scaled = false;
        UpdateMessageBroker.informSubscribers();
        return true;
    }

    /**
     * Append the specified point range to the end of the track
     *
     * @param inPoints list of points to append
     * @return true if it worked, false otherwise
     */
    public boolean appendRange(List<DataPoint> inPoints) {
        if (inPoints == null || inPoints.isEmpty()) {
            return false;
        }
        // Make new array to copy points over to
        DataPoint[] newPointArray = new DataPoint[_numPoints + inPoints.size()];
        System.arraycopy(_dataPoints, 0, newPointArray, 0, _numPoints);
        int index = _numPoints;
        for (DataPoint point : inPoints) {
            newPointArray[index] = point;
            extendFieldList(point.getFieldList());
            index++;
        }
        // Change over to new array
        _dataPoints = newPointArray;
        _numPoints = _dataPoints.length;
        _scaled = false;
        return true;
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
        if (inEnd >= getNumPoints() - 1) inEnd = getNumPoints() - 1;

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

        if (DEBUG) {
//            int d = (int) (_nearestDist * 1000);
//            Log.d(TAG, "getNearestTrackpointIndex inStart = " + inStart + ", inEnd = " + inEnd + ": nearestPoint = " + nearestPoint + "; nearestDist = " + d + "m");
        }
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
        if (inEnd >= getNumPoints()) return DataPoint.INVALID_INDEX;

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
                hasSegmentStart |= _dataPoints[i].getSegmentStart();
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
    public void load(Track inOther) {
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
        UpdateMessageBroker.informSubscribers();
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
        UpdateMessageBroker.informSubscribers();
        return true;
    }

    static DataPoint[] _waypoints;
    static int _numWaypoints;
    static int[] _pointIndices;

    /**
     * Interleave all waypoints by each nearest track point
     *
     * @return true if successful, false if no change
     * @implNote major changes
     * @author BiselliW
     * @since 20.2.006
     */
    public boolean interleaveWaypoints() {
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
        if ( /* _numWaypoints == 0 || */ _numWaypoints == _numPoints)
            return false;

        // Loop round points copying to correct order
        reorderPoints();

        // make last trackpoint to end point
        makeEndPoint();

        // find all nearest track points for all way points
        findNearestTrackPoints();

        // needs to be scaled again to recalc x, y
        _scaled = false;
        UpdateMessageBroker.informSubscribers();
        return true;
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
        DataPoint[] clone = new DataPoint[getNumPoints()];
        System.arraycopy(_dataPoints, 0, clone, 0, getNumPoints());
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

    /**
     * @return true if track contains trackpoints with altitude
     * @author BiselliW
     */
    public boolean hasAltitudes() {
        if (!_scaled) {
            scalePoints();
        }
        return _hasAltitude;
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
        // name first track point as "Start"
        boolean setStart = true;
        String strStart = App.resources.getString(R.string.start);

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
                        point.setWaypointName(strStart);
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
//									point.makeRoutePoint(_waypoints[j].getWaypointName(), copyIndex);
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
            scalePoints();
        }
        return _hasNamedTrackpoint;
    }

    /**
     * Find the nearest point to the specified x and y coordinates
     * or DataPoint.INVALID_INDEX if no point is within the specified max distance
     *
     * @param inX               x             coordinate (0,...,1)
     * @param inY               y             coordinate (0,...,1)
     * @param inMaxDist         maximum distance from selected coordinates (0,...,1)
     * @param inJustTrackPoints true if waypoints should be ignored
     * @return index of nearest point or negative index if not found
     * @author BiselliW
     * @implNote BiselliW - all coordinates are relative (0,1)
     * - nearestDist -> nearestSquDist
     */
    public int getNearestPointIndex2(double inX, double inY, double inMaxDist, boolean inJustTrackPoints) {
        int nearestPoint = 0;
        double nearestSquDist = -1.0;
        double distX, distY, currSquDist;
        for (int i = 0; i < getNumPoints(); i++) {
            if (!inJustTrackPoints || !_dataPoints[i].isWaypoint()) {
// TODO WB square distance as square sum
                distX = _xValues[i] - inX;
                distY = _yValues[i] - inY;
                currSquDist = distX * distX + distY * distY;
                if (
                        (currSquDist < nearestSquDist)
                                ||
                                (nearestSquDist < 0.0)
                ) {
                    nearestPoint = i;
                    nearestSquDist = currSquDist;

                    if (currSquDist == 0) {
                        break;
                    }

                }
            }
        }
        // Check whether it's within required distance
        if (
                (nearestSquDist > inMaxDist * inMaxDist)
                        &&
                        (inMaxDist > 0.0)
        ) {
            return -nearestPoint;
        }

        return nearestPoint;
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

/**
 * @param ptIndex index of a point
 * @return latitude of the point
 * @author BiselliW
 * /
public double getLatitude(int ptIndex) {
if (ptIndex < getNumPoints()) {
return getPoint(ptIndex).getLatitude().getDouble();
}
return 0.0;
}

/**
 * @param ptIndex index of a point
 * @return longitude of the point
 * @author BiselliW
 * /
public double getLongitude(int ptIndex) {
if (ptIndex < getNumPoints()) {
return getPoint(ptIndex).getLongitude().getDouble();
}
return 0.0;
}
*/

}
