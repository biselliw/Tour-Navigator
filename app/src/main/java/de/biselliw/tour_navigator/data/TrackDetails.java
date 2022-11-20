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
    along with FairEmail. If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2022 Walter Biselli (BiselliW)
*/

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.tim.prune.data.Altitude;
import de.biselliw.tour_navigator.tim.prune.data.Distance;
import de.biselliw.tour_navigator.tim.prune.data.IntegerRange;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.BaseTrack;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

/**
 * class to hold all details of a track
 *
 * @author BiselliW
 */
public class TrackDetails {

    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackDetails";
	private static final boolean _DEBUG = false; // Set to true to enable logging
	private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    final static public double DEF_HOR_SPEED = 4.0;
    final static public double DEF_VERT_SPEED_CLIMB = 0.3;
    final static public double DEF_VERT_SPEED_DESC = 0.5;
    final static public int DEF_MIN_HEIGHT_CHANGE = 3;

    public double Distance_km;
    private int Altitude_m;

    public boolean calcFromSstart = true; 
    private double SumDistance;
    private int SumClimb;
    private int SumDescent;
    private long SumSeconds = 0L;
    
	private long TotalCalcSeconds = 0L;
    public boolean CalcTimes = true;
    private int Climb;
    private int Descent;
    
    // hiking speed parameters
    private double horSpeed = DEF_HOR_SPEED; // horizontal part in [km/h]
    private double vertSpeedClimb = DEF_VERT_SPEED_CLIMB; // ascending part in [km/h]
    private double vertSpeedDescent = DEF_VERT_SPEED_DESC; // descending part in [km/h]
    private int minHeightChange = DEF_MIN_HEIGHT_CHANGE; // min. required change of altitude

    private final BaseTrack _track;
    private int numPoints;

    /**
     * set hiking parameters:
     *
     * @param inHorSpeed         horizontal part in [km/h]
     * @param inVertSpeedClimb   ascending part in [km/h]
     * @param inVertSpeedDescent descending part in [km/h]
     * @param inMinHeightChange  min. required change of altitude
     */
    public void setHikingParameters(double inHorSpeed, double inVertSpeedClimb, double inVertSpeedDescent, int inMinHeightChange) {
    	if (inHorSpeed > 0)
    		horSpeed = inHorSpeed;
    	if (inVertSpeedClimb > 0)
    		vertSpeedClimb = inVertSpeedClimb;
    	if (inVertSpeedDescent > 0)
    		vertSpeedDescent = inVertSpeedDescent;
    	if (inMinHeightChange > 0)
    		minHeightChange = inMinHeightChange;
    }

    private IntegerRange _altitudeRange = null;

// todo statt Höhenwerten Steigungen filtern!
    private final int MovingAverageDiv = 1; // 4;
    final double secondsHour = 3600.0;
    final double vertSecondsHour = 3.6;

    private double ptDistance = 0.0;
    private int segStartIndex = 0;
    private double segDistance_km = 0.0, segSpeed;
    private int segClimb_m = 0, segDescent_m = 0;

    private boolean segRecalc = true, segClimbing = false, segDescending = false;
    private DataPoint lastPoint = null;
    private int lastPauseMin = 0;

    private DataPoint currPoint;
    private double[] distance_km; // absolute distance of each point from start

    private int        // current altitude [m]
            movAltValue = 0,    // moving average value of altitude
            movAltValues = 0,
            lastAltValue = 0;

    private Altitude altitude = null;
    private boolean foundAlt = false;

    public TrackDetails(BaseTrack track) {
        _track = track;
    }

    /**
     * Recalculate all selection details
     */
    public void recalculate() {
        numPoints = _track.getNumPoints();
        // calc all times by default
        CalcTimes = true;
        ptDistance = 0.0;
        segStartIndex = 0;
        segDistance_km = 0.0;
        segClimb_m = 0;
        segDescent_m = 0;

        calcFromSstart = true; 
        SumDistance = 0.0;
        SumClimb = 0;
        SumDescent = 0;
        SumSeconds = 0L;
        segRecalc = true;
        segClimbing = false;
        segDescending = false;

		Climb = 0;
		Descent = 0;
		lastPauseMin = 0;
		currPoint = null;

        if (DEBUG) Log.d(TAG, "recalculate(): numPoints: " + numPoints);

        if (numPoints > 0) {
            distance_km = new double[numPoints];
            _altitudeRange = new IntegerRange();
        }
        lastPoint = null;
        Distance_km = 0.0;
        TotalCalcSeconds = 0;

        /* Clear all distances and times since start of the track */
        _track.clearRealtimeData();

        Altitude_m = 0;        // current altitude [m]
        movAltValue = 0;    // moving average value of altitude
        movAltValues = 0;
        lastAltValue = 0;
        altitude = null;
        foundAlt = false;
    }

	/**
	 * Recalculate all track-points for timing analysis
	 *
	 * @param ptIndex index of the current track-point
	 * @return true if the current track-point is a route point
	 */
	public boolean RecalculateTrackpoint(int ptIndex) {
		boolean result = false;

		if (DEBUG) { Log.d(TAG, "RecalculateTrackpoint (" + ptIndex + ")"); }
		currPoint = _track.getPoint(ptIndex);
		if ((currPoint != null) && !currPoint.hasMedia())
		{
			if (calcFromSstart)
			{
				SumDistance = 0;
				SumClimb = 0;
				SumDescent = 0;
				SumSeconds = 0;
				if (DEBUG) Log.d(TAG, "- calcFromSstart");
			}

			if (segRecalc) {
				segClimbing = false;
				segDescending = false;
				segDistance_km = 0.0;
				segStartIndex = ptIndex;
				segRecalc = false;
				if (DEBUG) Log.d(TAG, "- segRecalc = false");
			}

			// ignore waypoints outside the track
			if (!currPoint.isWayPoint())
			{
				boolean currPointhasAltitude = false;
				if (currPoint.hasAltitude()) {
					altitude = currPoint.getAltitude();
					if (altitude.isValid())
					{
						currPointhasAltitude = true;
						Altitude_m = altitude.getValue();
						_altitudeRange.addValue(Altitude_m);
						if (foundAlt)
						{
							if (MovingAverageDiv > 1)
							{
								if (++movAltValues < MovingAverageDiv)
									movAltValue = (movAltValue * (movAltValues-1)/movAltValues)
											+ Altitude_m/movAltValues;
								else
									movAltValue = (movAltValue * (MovingAverageDiv-1)/MovingAverageDiv)
											+ Altitude_m/MovingAverageDiv;
								Altitude_m = movAltValue;
							}

							int altDiffValue = Altitude_m - lastAltValue;
							if (altDiffValue > minHeightChange)
							{
								if (segDescending)
								{
									segClimb_m  = altDiffValue;
									segRecalc = true;
									if (DEBUG) Log.d(TAG, "- segDescending: force segRecalc");
								}
								else
								{
									segClimb_m  += altDiffValue;
									segClimbing = true;
								}
								lastAltValue = Altitude_m;
//								segClimb_m -= minHeightChange / 2;
							}
							else if (altDiffValue < (-minHeightChange))
							{
								if (segClimbing)
								{
									segDescent_m  = altDiffValue;
									segRecalc = true;
									if (DEBUG) Log.d(TAG, "- segClimbing: force segRecalc");
								}
								else
								{
									segDescent_m  += altDiffValue;
									segDescending = true;
								}
								lastAltValue = Altitude_m;
							}
						}
						else
							if (DEBUG) Log.d(TAG, "- no previous altitude given: can't calculate averages");
						if (lastAltValue <= 0)
							lastAltValue = Altitude_m;
						foundAlt = true;
					}
				}
				if (!currPointhasAltitude)
					if (DEBUG) Log.d(TAG, "- currPoint has no altitude: ignore it");

				// Calculate distances, excluding way points
				if (lastPoint != null)
				{
					double radians = DataPoint.calculateRadiansBetween(lastPoint, currPoint);
					double dist = Distance.convertRadiansToDistance(radians);
					Distance_km += dist;
					segDistance_km += dist;
				}
				lastPoint = currPoint;
			}
			else
			{
				// current point is waypoint
				if (DEBUG) Log.d(TAG, "- currPoint.isWayPoint (" + currPoint.getWaypointName() + "): ignore it");
				distance_km[ptIndex] += 0;
			}

			distance_km[ptIndex] = Distance_km;
			currPoint.setRealtimeDataDist(Distance_km);

			if (
					currPoint.isRoutePoint()
					||	(ptIndex == (numPoints-1))
			)
			{
				segRecalc = true;
				if (DEBUG) Log.d(TAG, "- currPoint.isRoutePoint (" + currPoint.getRoutePointName() + "); LinkIndex=" + currPoint.getLinkIndex() + ": force segRecalc");
			}

			if (segRecalc)
			{
				// update section data
				SumDistance	+= segDistance_km;
				SumClimb 		+= segClimb_m;
				Climb 		+= segClimb_m;
				SumDescent 	+= segDescent_m;
				Descent 	+= segDescent_m;

				if (DEBUG) Log.d(TAG, "- start segRecalc : SumDistance = " + SumDistance);

				if (CalcTimes)
				{
					long segSeconds, horSeconds, vertSeconds;

					// calculate section times
					horSeconds = (long) (segDistance_km / horSpeed * secondsHour);

					if (segClimbing) {
						vertSeconds = (long) (segClimb_m / vertSpeedClimb * vertSecondsHour);
						segClimb_m = 0;
					} else if (segDescending) {
						vertSeconds = -(long) (segDescent_m / vertSpeedDescent * vertSecondsHour);
						segDescent_m = 0;
					} else
						vertSeconds = (long) (segClimb_m / vertSpeedClimb * vertSecondsHour)
								- (long) (segDescent_m / vertSpeedDescent * vertSecondsHour);

					if (horSeconds > vertSeconds)
						segSeconds = (long) (horSeconds + vertSeconds / 2.0);
					else
						segSeconds = (long) (horSeconds / 2.0 + vertSeconds);

					SumSeconds += segSeconds;

					/* relative time of each point since start */
					if (DEBUG) Log.d(TAG, "- SumSeconds = " + SumSeconds);

					// calculate time stamps of all track/way points within the current segment
					if (DEBUG) Log.d(TAG, "- calculate time stamps since segment start at point " + segStartIndex);
					for (int j = segStartIndex; j <= ptIndex; j++)
					{
						DataPoint _point = _track.getPoint(j);
						if ((j > 0) && !_point.getSegmentStart()) {
							if ((segSeconds > 0) && (segDistance_km > 0)) {
								segSpeed = segDistance_km / segSeconds;
								ptDistance = distance_km[j] - distance_km[j - 1];
								if (ptDistance > 0.0) {
									TotalCalcSeconds += ptDistance / segSpeed;
								}
							}
						}
						if (lastPauseMin > 0) {
							TotalCalcSeconds += lastPauseMin * 60L;
							lastPauseMin = 0;
						}
						_point.setRealtimeDataTime(TotalCalcSeconds);
					}
					if (DEBUG) Log.d(TAG, "- TotalCalcSeconds = " + TotalCalcSeconds);
				}
			}

			if (currPoint.isRoutePoint()) {
				lastPauseMin = currPoint.getWaypointDuration();
				if (lastPauseMin > 0) {
					if (DEBUG) Log.d(TAG, "- lastPauseMin = "+lastPauseMin);
				}
				result = true;
			}

		}

		calcFromSstart = result;
		if (DEBUG) Log.d(TAG, "- result = " + result);
		return result;
	}

    public int getAltitude () {
    	return Altitude_m;
    }
    
    public double getSumDistance () {
    	return SumDistance;
    }    
    
    
    public int getSumClimb () {
        return SumClimb;
    }

    public int getSumDescent () {
        return SumDescent;
    }

    public long getSumSeconds () {
        return SumSeconds;
    }
    
    public int getClimb () {
        return Climb;
    }
    
    public int getDescent () {
        return Descent;
    }

    public long getTotalCalcSeconds () {
        return TotalCalcSeconds;
    }

	public int getMinAltitude() {
		return _altitudeRange.getMinimum();
	}

	public int getMaxAltitude() {
		return _altitudeRange.getMaximum();
	}
}
