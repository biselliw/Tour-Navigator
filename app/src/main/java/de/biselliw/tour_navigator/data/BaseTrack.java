package de.biselliw.tour_navigator.data;

/* @since WB */

import static java.lang.Math.sqrt;

import tim.prune.data.Coordinate;
import tim.prune.data.DataPoint;
import tim.prune.data.Distance;
import tim.prune.data.DoubleRange;
import tim.prune.data.Field;
import tim.prune.data.FieldList;
import tim.prune.data.PointCreateOptions;
import tim.prune.UpdateMessageBroker;

import de.biselliw.tools.debug.Log;

import de.biselliw.tour_navigator.helpers.I18nManager;

import de.biselliw.tour_navigator.helpers.MapUtils;

/**
 * Class to hold all track information,
 * including track points and waypoints
 * @since WB
 * 
 */
public class BaseTrack {
	/**
	 * TAG for log messages.
	 */
	static final String TAG = "BaseTrack";
	private static final boolean DEBUG = false;

	// Master field list
	protected FieldList _masterFieldList;
	// Data points
	protected DataPoint[] _dataPoints;
	// Scaled x, y values
	protected double[] _xValues = null;
	protected double[] _yValues = null;
	protected boolean _scaled;

	// variable ranges
	private DoubleRange _latRange = null, _longRange = null;
	private DoubleRange _xRange = null, _yRange = null;

	protected int _numPoints;

	private boolean _hasTrackpoint = false;
	private boolean _hasWaypoint = false;
	/** Nearest distance of a track point to the specified Latitude and Longitude coordinates */
	private double _nearestDist = -1.0;

	/**
	 * Constructor for empty track
	 */
	public BaseTrack()
	{
		// create field list
		_masterFieldList = new FieldList();
		// make empty DataPoint array
		_dataPoints = new DataPoint[0];
		_numPoints = 0;
		// needs to be scaled
		_scaled = false;
	}

	/**
	 * Constructor using fields and points from another Track
	 * @param inFieldList Field list from another Track object
	 * @param inPoints (edited) point array
	 */
	public BaseTrack(FieldList inFieldList, DataPoint[] inPoints)
	{
		_masterFieldList = inFieldList;
		_dataPoints = inPoints;
		if (_dataPoints == null) _dataPoints = new DataPoint[0];
		_numPoints = _dataPoints.length;
		_scaled = false;
	}

	/**
	 * Load method, for initialising and reinitialising data
	 * @param inFieldArray array of Field objects describing fields
	 * @param inPointArray 2d object array containing data
	 * @param inOptions load options such as units
	 * @since WB
	 * - add "Ziel"
	 */
	public void load(Field[] inFieldArray, Object[][] inPointArray, PointCreateOptions inOptions)
	{
		if (DEBUG) Log.d(TAG, "Loaded");
		if (inFieldArray == null || inPointArray == null)
		{
			_numPoints = 0;
			return;
		}
		// copy field list
		_masterFieldList = new FieldList(inFieldArray);
		// make DataPoint object from each point in inPointList
		_dataPoints = new DataPoint[inPointArray.length];
		String[] dataArray;
		int pointIndex = 0;
		String _pointName;
		for (Object[] objects : inPointArray) {
			dataArray = (String[]) objects;
			// Convert to DataPoint objects
			DataPoint point = new DataPoint(dataArray, _masterFieldList, inOptions);
			if (point.isValid()) {
				// bugfix of outdooractive GPX tracks with GPX coordinates as track point names
				_pointName = point.getWaypointName();
				if (_pointName.length() > 6) {
					_pointName = _pointName.substring(0, 6);
					String _pointLat = point.getLatitude().output(Coordinate.FORMAT_NONE).substring(0, 6);
					if (_pointName.equals(_pointLat)) {
						// remove same name as in previous track point
						point.setWaypointName("");
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
	 * Reverse the route
	 */
	public boolean reverseRoute()
	{
		return reverseRange(0,_numPoints-1);
	}

	/**
	 * Reverse the specified range of points
	 * @param inStart start index
	 * @param inEnd end index
	 * @return true if successful, false otherwise
	 */
	public boolean reverseRange(int inStart, int inEnd)
	{
		if (inStart < 0 || inEnd < 0 || inStart >= inEnd || inEnd >= _numPoints)
		{
			return false;
		}
		// calculate how many point swaps are required
		int numPointsToReverse = (inEnd - inStart + 1) / 2;
		DataPoint p;
		for (int i=0; i<numPointsToReverse; i++)
		{
			// swap pairs of points
			p = _dataPoints[inStart + i];
			_dataPoints[inStart + i] = _dataPoints[inEnd - i];
			_dataPoints[inEnd - i] = p;
		}
		// adjust segment starts
		shiftSegmentStarts(inStart, inEnd);
		// Find first track point and following track point, and set segment starts to true
		DataPoint firstTrackPoint = getNextTrackPoint(inStart);
		if (firstTrackPoint != null) {firstTrackPoint.setSegmentStart(true);}
		DataPoint nextTrackPoint = getNextTrackPoint(inEnd+1);
		if (nextTrackPoint != null) {nextTrackPoint.setSegmentStart(true);}
		// needs to be scaled again
		_scaled = false;
		UpdateMessageBroker.informSubscribers();
		return true;
	}

	/**
	 * Request that a rescale be done to recalculate derived values
	 */
	public void requestRescale()
	{
		_scaled = false;
	}

	/**
	 * @return true if track needs rescaling
	 */
	public boolean needsRescaling ()
	{
		return !_scaled;
	}

	/**
	 * Interleave all waypoints by each nearest track point
	 * @return true if successful, false if no change
	 * @since WB
	 */
	public boolean interleaveWaypoints()
	{
		// Separate way points and find nearest track point
		if (DEBUG) {
			Log.d(TAG, "interleaveWaypoints(): Separate waypoints and find nearest trackpoint");
			Log.d(TAG, "                       ----------------------------------------------");
			Log.d(TAG, "- needsRescaling: "+ !_scaled);
		}

		if (!_scaled) 
		{
			scalePoints();
			if (DEBUG) {
				Log.d(TAG, "- needsRescaling: "+ !_scaled);
			}
		}
		int numWaypoints = 0;

		DataPoint[] waypoints = new DataPoint[_numPoints];
		int[] pointIndices = new int[_numPoints];
		DataPoint point;
		int i;
		if (DEBUG) {
			Log.d(TAG, "- find nearest track points for all way points");
			Log.d(TAG, "- --------------------------------------------");
		}
		for (i=0; i<_numPoints; i++)
		{
			point = _dataPoints[i];
			// remove link from track point to way point
			point.clearWayPointLink();

			// if point is a way point outside the track
			if (point.isWayPoint())
			{
				// find nearest track point
				waypoints[numWaypoints] = point;
				waypoints[numWaypoints].clearWayPointLink();
				pointIndices[numWaypoints] = getNearestPointIndex(_xValues[i], _yValues[i], 0.000005, true);
				if (DEBUG) {
					Log.d(TAG, "  - _dataPoints["+i+"] is WP["+numWaypoints+"] (" + waypoints[numWaypoints].getWaypointName()+")");
					if (pointIndices[numWaypoints] >= 0)
						Log.d(TAG, "   - NearestPointIndex: " + pointIndices[numWaypoints]);
					else
					    Log.d(TAG, "   - out of reach ");
				}
				numWaypoints++;
			}
		}
		// Exit if data not mixed
		if (numWaypoints == 0 || numWaypoints == _numPoints)
			return false;

		if (DEBUG) {
			Log.d(TAG, "  ----------------------------------------------------------------------- ");
			Log.d(TAG, "  found "+numWaypoints+" way points");
			Log.d(TAG, "- rearrange track into: (track points, linked way points), other way points");
		}
		// Loop round points copying to correct order
		DataPoint[] dataCopy = new DataPoint[_numPoints];
		int copyIndex = 0, trackPoints = 0, linkedPoints = 0, nonlinkedPoints = 0;
		// name first track point as "Start"
		boolean setStart = true;
		String strStart = I18nManager.getText("fieldname.waypointstart");

		for (i=0; i<_numPoints; i++)
		{
			point = _dataPoints[i];
			// if it's a track point, copy it
			if (!point.isWayPoint())
			{
				// if (DEBUG) {Log.d(TAG, "  - _dataPoints["+i+"] is track point #"+trackPoints+" -> dataCopy["+copyIndex+"]");
				dataCopy[copyIndex] = point;
				copyIndex++;
				trackPoints++;

				if (setStart)
				{
					if (point.isRoutePoint())
					{
						setStart = false;
						if (DEBUG) {Log.d(TAG,
								"  - _dataPoints["+i+"] is already a named track point (" + point.getRoutePointName()+") -> dataCopy["+(copyIndex-1)+"]");
						}
					}
					else
					{
						point.setWaypointName(strStart);
						point.setLinkIndex(0);
//						point.makeRoutePoint(strStart, -1);
						setStart = false;
						if (DEBUG) {Log.d(TAG,
								"  - _dataPoints["+i+"] named to \"" + strStart +"\") -> dataCopy["+(copyIndex-1)+"]");
						}
					}
				}
				else
				{
					// check for way points with this index
					boolean foundWP = false;
					int linkedTP = DataPoint.INVALID_INDEX;
					for (int j=0; j<numWaypoints; j++)
					{
						if ((pointIndices[j] >= 0) && (pointIndices[j] <= i))
						{
							/*
							 *  is this way point the nearest to the track point?
							 *  - link the track point to this way point
							 */
							if (!foundWP)
							{
								foundWP = true;
								linkedTP = copyIndex-1;
								if (DEBUG) {Log.d(TAG,
										"  - _dataPoints["+i+"] is track point #"+trackPoints+" -> dataCopy["+linkedTP+"]");
								}
								if (DEBUG) {Log.d(TAG,
										"    - make it a Route Point linked to WP("+waypoints[j].getWaypointName()+"), index="+linkedTP);
								}
								point.makeRoutePoint(waypoints[j].getWaypointName(), copyIndex);
							}
							waypoints[j].setLinkIndex(linkedTP);
							// else link the following track point to this way point
							pointIndices[j] = DataPoint.INVALID_INDEX;

							if (DEBUG) {Log.d(TAG,
									"    - add WP["+j+"] ("+waypoints[j].getWaypointName()+") -> dataCopy["+copyIndex+"]");
							}
							dataCopy[copyIndex] = waypoints[j];
							copyIndex++;
							linkedPoints++;
						}
					}
				}
			}
		}
		if (DEBUG) {
			Log.d(TAG, "  ----------------------------------------------------------------------- ");
			Log.d(TAG, "  found "+copyIndex+" points within the track: "+trackPoints+" track points, "+linkedPoints+" nearby way points");
			Log.d(TAG, "- check for remaining way points outside the track");
		}
		// check for way points without index
		for (int j=0; j<numWaypoints; j++)
		{
			if (pointIndices[j] != DataPoint.INVALID_INDEX)
			{
				if (DEBUG) {
						Log.d(TAG, 
						"  - add WP["+j+"] ("+waypoints[j].getWaypointName()+") -> dataCopy["+copyIndex+"]");
				}
				dataCopy[copyIndex] = waypoints[j];
				nonlinkedPoints++;
				copyIndex++;
			}
		}
		if (DEBUG) {
			Log.d(TAG, "  ------------------------------------------------------------------ ");
			Log.d(TAG, "  copied " + copyIndex + "/" + _numPoints + " points:");
			Log.d(TAG, "  " + trackPoints + " track points, " + linkedPoints + " linked points, " + nonlinkedPoints + " non linked points, ");
		}

		// Copy data back to track
		_dataPoints = dataCopy;
 		int endPoint = 0;
 
		for (i=0; i<_numPoints; i++)
		{
			point = _dataPoints[i];
			if (!point.isWayPoint())
			{
				endPoint = i;
			}
		}
	
 		point = _dataPoints[endPoint];
		if (point.getWaypointName().isEmpty())
		{
			if (DEBUG) {
				Log.d(TAG, "  set TP[" + endPoint + "] as end point");
			}
			point.setWaypointName(I18nManager.getText("fieldname.waypointend"));
		}
		else {
			if (DEBUG) {
				Log.d(TAG, "  leave TP[" + endPoint + "] (" + point.getWaypointName() + ") as end point");
			}
		}
		
		if (DEBUG) {
			Log.d(TAG, "- find all nearest track points for all way points");
			Log.d(TAG, "- ------------------------------------------------");
		}
		for (int j=0; j<numWaypoints; j++)
		{
			int linkedTP, linkedWP;
			point = waypoints[j];
			linkedWP = getPointIndex(point);
			double lat = point.getLatitude().getDouble();
			double lon = point.getLongitude().getDouble();
			// get the index to the currently linked track point
			linkedTP = point.getLinkIndex();
			if (DEBUG) {
				Log.d(TAG,
				"  - WP["+j+"] ("+waypoints[j].getWaypointName()+") -> _dataPoints["+linkedTP+"]");
			}
			if (DEBUG) {
				Log.d(TAG,
				"    - WP["+j+"] inside track -> _dataPoints["+linkedTP+"]");
			}
			while ((linkedTP >= 0) && (linkedTP < _numPoints))
			{
				// find the next track point which is considered as outside of the track
				linkedTP = getOutsidePointIndex2(linkedTP+1, _numPoints-1, lat, lon, 0.2, true);
				if (DEBUG) {
					Log.d(TAG,
					"    - WP["+j+"] out of track -> _dataPoints["+linkedTP+"]");
				}

				// find the next track point after this one which is considered as inside the track again
				if (linkedTP >= 0)
				{
					linkedTP = getNearestTrackpointIndex(linkedTP+1, lat, lon, 0.020, 0.0);
					if (DEBUG) {
						Log.d(TAG,
						"    - WP["+j+"] inside track -> _dataPoints["+linkedTP+"]");
					}
					while ((linkedTP >= 0) && (linkedTP < _numPoints-1))
					{
						point = _dataPoints[linkedTP];
						if (point != null) {
							if ((point._isTrackpoint()) && (point.getLinkIndex() <= 0))
							{
								if (DEBUG) {Log.d(TAG,
										"    - make _dataPoints["+linkedTP+("] to a Route Point linked to WP("+waypoints[j].getWaypointName()+")"));
								}
								point.makeRoutePoint(waypoints[j].getWaypointName(), linkedWP);
								break;
							}
						}
						linkedTP++;
					}
				}
				else
					break;
			}
		}

		// needs to be scaled again to recalc x, y
		_scaled = false;
		UpdateMessageBroker.informSubscribers();
		return true;
	}
  
	//////// information methods /////////////

	/**
	 * @return true if track has altitude data
	 */
	public boolean hasAltitudeData()
	{
		for (int i=0; i<_numPoints; i++) {
			if (_dataPoints[i].hasAltitude()) {return true;}
		}
		return false;
	}

	/**
	 * @return true if track contains at least one trackpoint
	 */
	public boolean hasTrackPoints()
	{
		if (!_scaled) {scalePoints();}
		return _hasTrackpoint;
	}

	/**
	 * @return true if track contains waypoints
	 */
	public boolean hasWaypoints()
	{
		if (!_scaled) {scalePoints();}
		return _hasWaypoint;
	}


	///////// Internal processing methods ////////////////

	/**
	 * Scale all the points in the track to gain x and y values
	 * ready for plotting
	 */
	private synchronized void scalePoints()
	{
		// Loop through all points in track, to see limits of lat, long
		_longRange = new DoubleRange();
		_latRange = new DoubleRange();
		int p;
		_hasWaypoint = false; _hasTrackpoint = false;
		for (p=0; p < getNumPoints(); p++)
		{
			DataPoint point = getPoint(p);
			if (point != null && point.isValid())
			{
				_longRange.addValue(point.getLongitude().getDouble());
				_latRange.addValue(point.getLatitude().getDouble());
				if (point.isWaypoint())
					_hasWaypoint = true;
				else
					_hasTrackpoint = true;
			}
		}

		// Loop over points and calculate scales
		_xValues = new double[getNumPoints()];
		_yValues = new double[getNumPoints()];
		_xRange = new DoubleRange();
		_yRange = new DoubleRange();
		for (p=0; p < getNumPoints(); p++)
		{
			DataPoint point = getPoint(p);
			if (point != null)
			{
				_xValues[p] = MapUtils.getXFromLongitude(point.getLongitude().getDouble());
				_xRange.addValue(_xValues[p]);
				_yValues[p] = MapUtils.getYFromLatitude(point.getLatitude().getDouble());
				_yRange.addValue(_yValues[p]);
			}
		}
		_scaled = true;
	}

	/**
	 * Get the next track point starting from the given index
	 * @param inStartIndex index to start looking from
	 * @param inEndIndex index to stop looking (inclusive)
	 * @param inCountUp true for next, false for previous
	 * @return next track point, or null if end of data reached
	 */
	private DataPoint getNextTrackPoint(int inStartIndex, int inEndIndex, boolean inCountUp)
	{
		// Loop forever over points
		int increment = inCountUp?1:-1;
		for (int i=inStartIndex; i<=inEndIndex; i+=increment)
		{
			DataPoint point = getPoint(i);
			// Exit if end of data reached - there wasn't a track point
			if (point == null) {return null;}
			if (point.isValid() && !point.isWaypoint()) {
				// next track point found
				return point;
			}
		}
		return null;
	}



//////// information methods /////////////

	/**
	 * Find the nearest point to the specified x and y coordinates
	 * or DataPoint.INVALID_INDEX if no point is within the specified max distance
	 *
	 * @param inX x             coordinate (0,...,1)
	 * @param inY y             coordinate (0,...,1)
	 * @param inMaxDist         maximum distance from selected coordinates (0,...,1)
	 * @param inJustTrackPoints true if waypoints should be ignored
	 * @return index of nearest point or negative index if not found
	 *
	 * @since WB
	 * - all coordinates are relative (0,1)
	 * - nearestDist -> nearestSquDist
	 */

	public int getNearestPointIndex(double inX, double inY, double inMaxDist, boolean inJustTrackPoints)
	{
		int nearestPoint = 0;
		double nearestSquDist = -1.0;
		double distX, distY, currSquDist;
		for (int i=0; i < getNumPoints(); i++)
		{
			if (!inJustTrackPoints || !_dataPoints[i].isWaypoint())
			{
				// TODO WB square distance as square sum
				distX = _xValues[i] - inX;
				distY = _yValues[i] - inY;
				currSquDist = distX*distX + distY*distY;
				if (
						(currSquDist < nearestSquDist)
						||
						(nearestSquDist < 0.0)
				)
				{
					nearestPoint = i;
					nearestSquDist = currSquDist;

					if (currSquDist == 0)
					{
						break;
					}

				}
			}
		}
		// Check whether it's within required distance
		if (
				(nearestSquDist > inMaxDist*inMaxDist)
				&& 
				(inMaxDist > 0.0)
		)
		{
			return -nearestPoint;
		}

		return nearestPoint;
	}


	/**
	 * Find the nearest track point to the specified x and y coordinates
	 * or DataPoint.INVALID_INDEX if no
	 *
	 * @param inStart start index
	 * @param inX x coordinate [km]
	 * @param inY y coordinate [km]
	 * @param inMaxDist maximum distance from selected coordinates [km] to point
	 * @param inMaxDistDest maximum distance along the track [km] towards the destination
	 * @param inJustTrackPoints true if waypoints should be ignored
	 * @return index of nearest track point or <= if no point is within the specified max distanceS
	 *
	 * @since WB
	 * - all coordinates in [km]
	 */
	public int getNearestPointIndex(int inStart, double inX, double inY, double inMaxDist, double inMaxDistDest, boolean inJustTrackPoints) {
		int nearestPoint = 0;
		double startTrack = -1;
		if (inStart < 0) inStart = 0;
		if (inStart >= getNumPoints() - 1) {
			return DataPoint.INVALID_INDEX;
		}

		double nearestSquDist = -1.0;
		_nearestDist = -1.0;
		double sqMaxDist = inMaxDist * inMaxDist;
		double distX, distY, currSquDist;
		for (int i=inStart; i < getNumPoints(); i++)
		{
			if (!inJustTrackPoints || !_dataPoints[i].isWaypoint())
			{
				// TODO WB square distance as square sum
				distX = _xValues[i] - inX;
				distY = _yValues[i] - inY;
				currSquDist = distX*distX + distY*distY;
				if ( (currSquDist < nearestSquDist) || (nearestSquDist < 0.0) )
				{
					nearestPoint = i;
					nearestSquDist = currSquDist;

					if (currSquDist == 0)
					{
						break;
					}
				}

				if (inMaxDistDest > 0)
				{
					if (startTrack < 0)
						startTrack = _dataPoints[i].getDistance() + inMaxDistDest;
					else if (startTrack < _dataPoints[i].getDistance())
						break;
				}
			}
		}

		if (DEBUG) {
			double dist = sqrt(nearestSquDist);
			int d = (int)(dist*1000);
			String s = "nearestDist: " + d + "m / nearestDistToStart: ";

			double radians = DataPoint.calculateRadiansBetween(_dataPoints[nearestPoint], _dataPoints[inStart]);
			dist = Distance.convertRadiansToDistance(radians);
			d = (int)(dist*1000);
			s = s + d + "m";
			Log.d(TAG, s);
		}
		// Check whether it's within required distance
		// todo covert to km
		_nearestDist = nearestSquDist;
		if (
				(nearestSquDist > sqMaxDist)
						&&
						(inMaxDist > 0.0)
		)
		{
			return -nearestPoint;
		}
		return nearestPoint;
	}

	/**
	 * Find the nearest track point to the specified Latitude and Longitude coordinates
	 * or DataPoint.INVALID_INDEX if no
	 *
	 * @param inStart 		start index
 	 * @param inLatitude 	Latitude in degrees
	 * @param inLongitude 	Longitude in degrees
	 * @param inMaxDist 	maximum distance from selected coordinates [km] to point
	 * @param inMaxDistDest maximum distance along the track [km] towards the destination
	 * @return 				>= 0: index of nearest track point within the specified max distance
	 * 						<  0: index of nearest track point outside the specified max distance
	 * 						DataPoint.INVALID_INDEX if no point is within the specified max distance
	 */
	public int getNearestTrackpointIndex(int inStart, double inLatitude, double inLongitude, double inMaxDist, double inMaxDistDest) {
		/* init index of the nearest track point to the specified Latitude and Longitude coordinates */
		int nearestPoint = DataPoint.INVALID_INDEX;
		double startTrack = -1;
		_nearestDist = -1.0;

		if (inStart < 0) inStart = 0;
		if (inStart >= getNumPoints() - 1) {
			return DataPoint.INVALID_INDEX;
		}

		double currDist;
		for (int i=inStart; i < getNumPoints(); i++)
		{
			DataPoint point = _dataPoints[i];
			if (point._isTrackpoint())
			{
				double radians = point.calculateRadiansBetween(inLatitude, inLongitude);
				currDist = Distance.convertRadiansToDistance(radians);
				if ( (currDist < _nearestDist) || (_nearestDist < 0.0) )
				{
					nearestPoint = i;
					_nearestDist = currDist;
					if (currDist < 0.005) break;
				}

				if (inMaxDistDest > 0)
				{
					if (startTrack < 0)
						startTrack = _dataPoints[i].getDistance() + inMaxDistDest;
					else if (startTrack < _dataPoints[i].getDistance())
						break;
				}
			}
		}

		if (DEBUG) {
			int d = (int)(_nearestDist*1000);
			Log.d(TAG, "getNearestTrackpointIndex("+inStart+") : index: "+nearestPoint+"; nearestDist = "+ d +"m");
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
	 * @return distance of nearest track point [km], negated if not within the specified max distance
	 * @since WB
	 * - all coordinates in [km]
	 */
	public double getNearestDistance() {
		return _nearestDist;
	}

	/**
	 * Find the next track point which is considered as outside of the track 
	 * or DataPoint.INVALID_INDEX if no
	 *
	 * @param inStart 		start index
	 * @param inEnd 		end index
	 * @param inX 			x coordinate [km]
	 * @param inY 			y coordinate [km]
	 * @param inMinDist 	minimum distance from selected coordinates [km] to point
	 * @param inJustTrackPoints true if waypoints should be ignored
	 * @return index of nearest track point or <= if no point is within the specified max distanceS
	 *
	 * @since WB
	 * - all coordinates in [km]
	 */
	public int getOutsidePointIndex(int inStart, int inEnd, double inX, double inY, double inMinDist, boolean inJustTrackPoints) {
		if (inStart < 0) inStart = 0;
		if (inStart > inEnd) return DataPoint.INVALID_INDEX;
		if (inEnd >= getNumPoints()) return DataPoint.INVALID_INDEX;

		int nearestPoint = DataPoint.INVALID_INDEX;
		double nearestSquDist = -1.0;
		_nearestDist = -1.0;
		double sqMinDist = inMinDist * inMinDist;
		double distX, distY, currSquDist;
		for (int i=inStart; i <= inEnd; i++)
		{
			if (!inJustTrackPoints || !_dataPoints[i].isWaypoint())
			{
				// TODO WB square distance as square sum
				distX = _xValues[i] - inX;
				distY = _yValues[i] - inY;
				currSquDist = distX*distX + distY*distY;
				if (currSquDist > sqMinDist)
				{
					nearestPoint = i;
					nearestSquDist = currSquDist;
					
					if (DEBUG) {
						double dist = sqrt(nearestSquDist);
						int d = (int)(dist*1000);
						String s = "nearestDist: " + d + "m / nearestDistToStart: ";

						double radians = DataPoint.calculateRadiansBetween(_dataPoints[nearestPoint], _dataPoints[inStart]);
						dist = Distance.convertRadiansToDistance(radians);
						d = (int)(dist*1000);
						s = s + d + "m";
						Log.d(TAG, s);
					}
					break;
				}
			}
		}
		return nearestPoint;
	}



	/**
	 * Find the next track point which is considered as outside of the track 
	 * or DataPoint.INVALID_INDEX if no
	 *
	 * @param inStart 		start index
	 * @param inEnd 		end index
	 * @param inLatitude 	Latitude in degrees
	 * @param inLongitude 	Longitude in degrees
	 * @param inMinDist 	minimum distance from selected coordinates [km] to point
	 * @param inJustTrackPoints true if waypoints should be ignored
	 * @return 				index of the next track point which is considered as outside of the track 
	 *
	 * @since WB
	 * - all coordinates in [km]
	 */
	public int getOutsidePointIndex2(int inStart, int inEnd, double inLatitude, double inLongitude, double inMinDist, boolean inJustTrackPoints) {

		if (inStart < 0) inStart = 0;
		if (inStart > inEnd) return DataPoint.INVALID_INDEX;
		if (inEnd >= getNumPoints()) return DataPoint.INVALID_INDEX;

		for (int i=inStart; i < inEnd; i++)
		{
			DataPoint point = _dataPoints[i];
			if (point != null) {
				if (!inJustTrackPoints || !point.isWaypoint())
				{
					double radians = point.calculateRadiansBetween(inLatitude, inLongitude);
					double currDist = Distance.convertRadiansToDistance(radians);
					if (currDist > inMinDist )
					{
						if (DEBUG) {
							int d = (int)(currDist*1000);
							Log.d(TAG, "getOutsidePointIndex() Dist = "+ d +"m");
						}
						return i;
					}
				}
			}
		}

		return DataPoint.INVALID_INDEX;
	}

	/**
	 * Search for the given Point in the track and return the index
	 * @param inPoint Point to look for
	 * @return index of Point, if any or DataPoint.INVALID_INDEX if not found
	 */
	public int getPointIndex(DataPoint inPoint)
	{
		if (inPoint != null)
		{
			// Loop over points in track
			for (int i=0; i<=_numPoints-1; i++)
			{
				if (_dataPoints[i] == inPoint)
				{
					return i;
				}
			}
		}
		// not found
		return DataPoint.INVALID_INDEX;
	}

	/**
	 * Find the nearest way point to the specified x and y coordinates
	 * or DataPoint.INVALID_INDEX if no point is within the specified max distance
	 *
	 * @param inX x coordinate (0,...,1)
	 * @param inY y coordinate (0,...,1)
	 * @param inMaxDist maximum distance from selected coordinates [m]
	 * @return index of nearest way point or DataPoint.INVALID_INDEX if not found
	 *
	 * @since WB
	 * - all coordinates are relative (0,1)
	 */

	public int getNearestWaypointIndex(double inX, double inY, double inMaxDist)
	{
		int nearestPoint = DataPoint.INVALID_INDEX;
		double nearestSquDist = -1.0;
		double distX, distY, currSquDist;
		final double X = 38000000;
		final double inMinDist = 10/X;
		inMaxDist = inMaxDist / X;

		for (int i=0; i < getNumPoints(); i++)
		{
			if (_dataPoints[i].isWaypoint())
			{
				// calc. square distance
				distX = _xValues[i] - inX;
				distY = _yValues[i] - inY;
				currSquDist = distX*distX + distY*distY;
				if (
						(currSquDist < nearestSquDist)
						|| 
						(nearestSquDist < 0.0)
				)
				{
					nearestPoint = i;
					nearestSquDist = currSquDist;
					if (currSquDist < inMinDist)
					{
						break;
					}
				}
			}
		}

		// Check whether it's within required distance
		if (
				(nearestSquDist > inMaxDist*inMaxDist)
				&& 
				(inMaxDist > 0.0)
		)
		{
			return DataPoint.INVALID_INDEX;
		}
		return nearestPoint;
	}


	/**
	 * Get the point at the given index
	 * @param inPointNum index number, starting at 0
	 * @return DataPoint object, or null if out of range
	 */
	public DataPoint getPoint(int inPointNum)
	{
		if (inPointNum > -1 && inPointNum < getNumPoints())
		{
			return _dataPoints[inPointNum];
		}
		return null;
	}

	/**
	 * @return waypoint comment or description, if any
	 * @author Walter Biselli
	 * @since WB
	 */
	public String getWaypointCommentOrDescription(int inPointNum)
	{
		if (inPointNum < 0 || inPointNum >= getNumPoints()) return "";

		return getWaypointCommentOrDescription(_dataPoints[inPointNum]);
	}

	/**
	 * @return waypoint comment or description, if any
	 * @author Walter Biselli
	 * @since WB
	 */
	public String getWaypointCommentOrDescription(DataPoint inPoint)
	{
		if (inPoint == null) return "";

		String comment = inPoint.getWaypointCommentOrDescription();
		if (comment.equals(""))
		{
			int linkIndex = inPoint.getLinkIndex();
			if (linkIndex >= 0)
			{
				DataPoint point = _dataPoints[linkIndex];
				if (point == null) return "";
				comment = point.getWaypointCommentOrDescription();
			}
		}

		return comment;
	}

	/**
	 * Get the next track point starting from the given index
	 * @param inStartIndex index to start looking from
	 * @return next track point, or null if end of data reached
	 */
	public DataPoint getNextTrackPoint(int inStartIndex)
	{
		return getNextTrackPoint(inStartIndex, _numPoints, true);
	}

	/**
	 * Get the next track point in the given range
	 * @param inStartIndex index to start looking from
	 * @param inEndIndex index to stop looking
	 * @return next track point, or null if end of data reached
	 */
	public DataPoint getNextTrackPoint(int inStartIndex, int inEndIndex)
	{
		return getNextTrackPoint(inStartIndex, inEndIndex, true);
	}


	/**
	 * Get the previous track point starting from the given index
	 * @param inStartIndex index to start looking from
	 * @return next track point, or null if end of data reached
	 */
	public DataPoint getPreviousTrackPoint(int inStartIndex)
	{
		// end index is given as _numPoints but actually it just counts down to -1
		return getNextTrackPoint(inStartIndex, _numPoints, false);
	}


	/**
	 * Clear all distances and times since start of the track
	 * @since WB
	 */
	public void clearRealtimeData()
	{
		for (int i=0; i<_numPoints; i++)
		{
			_dataPoints[i].clearRealtimeData();
		}
	}


	public double getLatitude(int ptIndex) {
		if (ptIndex < getNumPoints()) {
			return getPoint(ptIndex).getLatitude().getDouble();
		}
		return 0.0;
	}

	public double getLongitude(int ptIndex) {
		if (ptIndex < getNumPoints()) {
			return getPoint(ptIndex).getLongitude().getDouble();
		}
		return 0.0;
	}

	/**
	 * @return the number of (valid) points in the track
	 */
	public int getNumPoints()
	{
		return _numPoints;
	}

	/**
	 * @return The range of x values as a DoubleRange object
	 */
	public DoubleRange getXRange()
	{
		if (!_scaled) {scalePoints();}
		return _xRange;
	}

	/**
	 * @return The range of y values as a DoubleRange object
	 */
	public DoubleRange getYRange()
	{
		if (!_scaled) {scalePoints();}
		return _yRange;
	}

	/**
	 * @return The range of lat values as a DoubleRange object
	 */
	public DoubleRange getLatRange()
	{
		if (!_scaled) {scalePoints();}
		return _latRange;
	}

	/**
	 * @return The range of lon values as a DoubleRange object
	 */
	public DoubleRange getLonRange()
	{
		if (!_scaled) {scalePoints();}
		return _longRange;
	}

	/**
	 * @param inX x value of point
	 * @return minimum wrapped value
	 */
	private static double getMinXDist(double inX)
	{
		// TODO: Should be abs(mod(inX-0.5,1)-0.5) - means two adds, one mod, one abs instead of two adds, 3 abss and two compares
		return Math.min(Math.min(Math.abs(inX), Math.abs(inX-1.0)), Math.abs(inX+1.0));
	}

	/**
	 * @param inPointNum point index, starting at 0
	 * @return scaled x value of specified point
	 */
	public double getX(int inPointNum)
	{
		if (!_scaled) {scalePoints();}
		return _xValues[inPointNum];
	}

	/**
	 * @param inPointNum point index, starting at 0
	 * @return scaled y value of specified point
	 */
	public double getY(int inPointNum)
	{
		if (!_scaled) {scalePoints();}
		return _yValues[inPointNum];
	}

	/**
	 * @return the master field list
	 */
	public FieldList getFieldList()
	{
		return _masterFieldList;
	}

	///////// Internal processing methods ////////////////

	/**
	 * Shift all the segment start flags in the given range by 1
	 * Method used by reverse range and its undo
	 * @param inStartIndex start of range, inclusive
	 * @param inEndIndex end of range, inclusive
	 */
	public void shiftSegmentStarts(int inStartIndex, int inEndIndex)
	{
		boolean prevFlag = true;
		boolean currFlag;
		for (int i=inStartIndex; i<= inEndIndex; i++)
		{
			DataPoint point = getPoint(i);
			if (point != null && !point.isWaypoint())
			{
				// remember flag
				currFlag = point.getSegmentStart();
				// shift flag by 1
				point.setSegmentStart(prevFlag);
				prevFlag = currFlag;
			}
		}
	}


}