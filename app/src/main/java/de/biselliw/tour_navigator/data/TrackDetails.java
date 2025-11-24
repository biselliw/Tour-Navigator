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

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.tim.prune.data.Altitude;
import de.biselliw.tour_navigator.tim.prune.data.Distance;

import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Track;

/**
 * class to hold all details of a track
 *
 * @author BiselliW
 * @since 26.1
 */
public class TrackDetails {

    /**
     * TAG for log messages.
     */
    static final String TAG = "TrackDetails";
	private static final boolean _DEBUG = false; // Set to true to enable logging
	private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public final static double DEF_HOR_SPEED 			= 5.0;
	public final static double DEF_VERT_SPEED_CLIMB 	= 0.35;
	public final static double DEF_VERT_SPEED_DESC 		= 0.5;
	public final static int    DEF_MIN_HEIGHT_CHANGE 	= 3;

    final static double _secondsHour 			= 3600.0;
    final static double _vertSecondsHour 		= 3.6;

	private static Track _track = null;

	private int     _ptIndex;
    private DataPoint _prevPoint = null;
    private double _altitude_m = 0, _minAltitude_m = 0, _maxAltitude_m = 0;
    private double _prevAltitude_m = -1;
    
    private double _totalDistance_km = 0.0;
    private long   _totalSeconds = 0L;
    private double  _totalClimb_m = 0;
    private double  _totalDescent_m = 0;

    /** if true: data must be recalculated in current segment of the track */
    private boolean _segRecalc = false;
    private double  _segDistance_km = 0.0;
	private int     _segStart = 0;
    private long    _segStart_s = 0L;
    private double  _segStartClimb_m = 0;
    private double  _segStartDescent_m = 0;
       
	/** Flags for whether minimum or maximum has been found */
	private boolean _gotPreviousMinimum = false, _gotPreviousMaximum = false;
	/** Integer values of previous minimum and maximum, if any */
	private double     _previousExtreme = 0;
	
	// hiking speed parameters
    /** horizontal part in [km/h] */
    private double _horSpeed = 			DEF_HOR_SPEED;
    /** climbing part in [km/h] */
    private double _vertSpeedClimb = 	DEF_VERT_SPEED_CLIMB;
    /** descending part in [km/h] */
    private double _vertSpeedDescent = 	DEF_VERT_SPEED_DESC;
    /** hysteresis value for a change of altitude */
    private int    _minHeightChange = 	DEF_MIN_HEIGHT_CHANGE;

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
    		_horSpeed = inHorSpeed;
    	if (inVertSpeedClimb > 0)
    		_vertSpeedClimb = inVertSpeedClimb;
    	if (inVertSpeedDescent > 0)
    		_vertSpeedDescent = inVertSpeedDescent;
    	if (inMinHeightChange > 0)
    		_minHeightChange = inMinHeightChange;
    }    

	/**
	 * Recalculate all selection details
	 */
    public TrackDetails(Track inTrack ) {
		_track = inTrack;
	}

	public boolean addPoint(int ptIndex)
	{
		boolean result = false;

		_ptIndex = ptIndex;
		DataPoint currPoint = _track.getPoint(ptIndex);
		result = addPoint(currPoint);

		return result;
	}

	private void resetSegment()
	{
		// update overall data at the end of the segment from section data
		_segDistance_km = 0.0;
		_segStart = _ptIndex;
		_segStart_s = _totalSeconds;
		_segStartClimb_m = _totalClimb_m;
		_segStartDescent_m = _totalDescent_m;
		_segRecalc = false;
	}

	/**
	 * Calculate distances and times of all points within the segment after analysis of the segment
	 * @param inSegSeconds calculated total time within the segment
	 */
	private void calcSegmentTimes (long inSegSeconds) {
		double segStartDistance_km = 0.0;
		int totalPause_min = 0;
		for (int ptIndex = _segStart; ptIndex <= _ptIndex; ptIndex++) {
			DataPoint currPoint = _track.getPoint(ptIndex);

			if (_segDistance_km > 0.0)
			{
				if (ptIndex == _segStart)
					segStartDistance_km = currPoint.getDistance();
				else
				{
					double segRelDistance = (currPoint.getDistance() - segStartDistance_km) / _segDistance_km;
					double segCurrTime = segRelDistance * (double)inSegSeconds + 31;
					long Time_s = _segStart_s + (int)segCurrTime + totalPause_min *60;
					long prevTime_s = currPoint.getTime();
					if (Time_s > prevTime_s)
						currPoint.setTime(Time_s);
					totalPause_min += currPoint.getWaypointDuration();
				}
			}
		}

		// update current overall data from section data
		_totalSeconds  = _segStart_s + inSegSeconds + totalPause_min*60;
	}

	/**
	 * add location data of a point to calculate its time and distance since start
	 * @param currPoint
	 * @return
	 */
	private boolean addPoint(DataPoint currPoint)
	{
		boolean result = false;
		boolean overallUp = false, overallDn = false;

		if ((currPoint == null) || currPoint.isWayPoint() /* || currPoint.hasMedia() */ )
			return false;

		// does the current point has a valid altitude?
		_altitude_m = 0;
		if (currPoint.hasAltitude())
		{
			Altitude altitude = currPoint.getAltitude();
			if (altitude.isValid())
				_altitude_m = altitude.getValue();
		}
		if (_altitude_m > 0)
		{
			// did the previous point have a valid altitude?
			if (_prevAltitude_m >= 0)
			{
				// Got an altitude value which is different from the previous one
				boolean segClimbing = (_altitude_m > _prevAltitude_m);
				overallUp = _gotPreviousMinimum && (_prevAltitude_m > _previousExtreme);
				overallDn = _gotPreviousMaximum && _prevAltitude_m < _previousExtreme;
				final boolean moreThanWiggle = Math.abs(_altitude_m - _prevAltitude_m) > _minHeightChange;

				// Do we know whether we're going up or down yet?
				if (!_gotPreviousMinimum && !_gotPreviousMaximum)
				{
					// we don't know whether we're going up or down yet - check limit
					if (moreThanWiggle)
					{
						if (segClimbing)
							_gotPreviousMinimum = true;
						else
							_gotPreviousMaximum = true;
						_previousExtreme = _prevAltitude_m;
						_prevAltitude_m = _altitude_m;
					}
				}
				else if (overallUp)
				{
					if (segClimbing)
						// we're still going up - do nothing
						_prevAltitude_m = _altitude_m;
					else if (moreThanWiggle)
						// we're going up but have dropped over a maximum
						_segRecalc = true;
				}
				else if (overallDn)
				{
					if (segClimbing)
					{
						if (moreThanWiggle)
							// we're going down but have climbed up from a minimum
							_segRecalc = true;
					}
					else
						// we're still going down - do nothing
						_prevAltitude_m = _altitude_m;
				}
			}
			else
				// we haven't got a previous value at all, so it's the start of a new segment
				_prevAltitude_m = _altitude_m;
		}

		// Calculate the distance to the previous trackpoint
		if (_prevPoint != null)
		{
			double radians = DataPoint.calculateRadiansBetween(_prevPoint, currPoint);
			double dist = Distance.convertRadiansToDistance(radians);
			_totalDistance_km += dist;
			currPoint.setDistance(_totalDistance_km);
			_segDistance_km += dist;
		}
		else
			currPoint.setDistance(0.0);
		currPoint.setTime(0L);

		// remember previous  point
		_prevPoint = currPoint;;

		if (_altitude_m > 0)
		{
			// need to calculate intermediate time?
			if (currPoint.isRoutePoint() || _segRecalc)
			{
				long segSeconds, horSeconds, vertSeconds;
				// calculate section times
				horSeconds = (long) (_segDistance_km / _horSpeed * _secondsHour);

				if (overallUp) {
					// Add the climb from _previousExtreme up to _previousValue
                    double climb_m;
					if (_segRecalc)
					{
						climb_m = _prevAltitude_m - _previousExtreme;
						_previousExtreme = _prevAltitude_m;
						_gotPreviousMinimum = false; _gotPreviousMaximum = true;
						_prevAltitude_m = _altitude_m;
					}
					else
					{
						climb_m = _altitude_m - _previousExtreme;
					}
					_totalClimb_m = _segStartClimb_m + climb_m;

					vertSeconds = (long) (climb_m / _vertSpeedClimb * _vertSecondsHour);
				} else if (overallDn) {
					// Add the descent from _previousExtreme down to _previousValue
                    double descent_m;
					if (_segRecalc)
					{
						descent_m = _previousExtreme - _prevAltitude_m;
						_previousExtreme = _prevAltitude_m;
						_gotPreviousMinimum = true; _gotPreviousMaximum = false;
						_prevAltitude_m = _altitude_m;
					}
					else
					{
						descent_m = _previousExtreme - _altitude_m;
					}
					_totalDescent_m = _segStartDescent_m + descent_m;

					vertSeconds = (long) (descent_m / _vertSpeedDescent * _vertSecondsHour);
				} else {
					vertSeconds = 0;
				}

				if (horSeconds > vertSeconds) {
					segSeconds = (long) (horSeconds + vertSeconds / 2.0);
				} else {
					segSeconds = (long) (horSeconds / 2.0 + vertSeconds);
				}

				calcSegmentTimes (segSeconds);
				result = true;
			}

			if (_segRecalc) {
				resetSegment();
			}

			if ((_altitude_m < _minAltitude_m) || (_minAltitude_m <= 0))
				_minAltitude_m = _altitude_m;
			else if (_altitude_m > _maxAltitude_m)
				_maxAltitude_m = _altitude_m;
		}

		return result;
	}


	public double getAltitude () {
		return _altitude_m;
	}

	public double getMinAltitude() { return _minAltitude_m; }

	public double getMaxAltitude() { return _maxAltitude_m; }

	public double getTotalDistance () {
		return _totalDistance_km;
	}

	public double getTotalClimb () {
		return _totalClimb_m;
	}

	public double getTotalDescent () {
		return _totalDescent_m;
	}

	public long getTotalSeconds () {
		return _totalSeconds;
	}

}



