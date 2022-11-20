package de.biselliw.tour_navigator.tim_prune.data;

import de.biselliw.tour_navigator.tim_prune.config.Config;
import de.biselliw.tour_navigator.stubs.AudioClip;
import de.biselliw.tour_navigator.stubs.MediaObject;
import de.biselliw.tour_navigator.stubs.Photo;
import de.biselliw.tour_navigator.tim.prune.data.Altitude;
import de.biselliw.tour_navigator.tim.prune.data.Coordinate;
import de.biselliw.tour_navigator.tim.prune.data.FieldList;
import de.biselliw.tour_navigator.tim.prune.data.Latitude;
import de.biselliw.tour_navigator.tim.prune.data.Longitude;
import de.biselliw.tour_navigator.tim.prune.data.PointCreateOptions;
import de.biselliw.tour_navigator.tim.prune.data.Timestamp;
import de.biselliw.tour_navigator.tim.prune.data.TimestampUtc;
import de.biselliw.tour_navigator.tim.prune.data.Unit;


/**
 * Class to represent a single data point in the series
 * including all its fields
 * Can be either a track point or a waypoint
 * @author tim.prune
 * @author BiselliW
 * @implNote added _waypointDuration, _isWaypoint, _waypointComment, _waypointDescription; extension: pause time [min] at this waypoint
 */

public class DataPoint
{
	/** Array of Strings holding raw values */
	private String[] _fieldValues = null;
	/** list of field definitions */
	private FieldList _fieldList = null;
	/** Special fields for coordinates */
	private Coordinate _latitude = null, _longitude = null;
	private Altitude _altitude = null;
	private Timestamp _timestamp = null;
	/** Attached photo */
	private Photo _photo = null;
	/** Attached audio clip */
	private AudioClip _audio = null;
	private String _waypointName = null;
	private String _routePointName = null;
	private boolean _startOfSegment = false;
	private boolean _markedForDeletion = false;
	private int _modifyCount = 0;

	public static final int INVALID_INDEX = -32000;

/** @since BiselliW */
	// hiking extension
	private boolean _isWaypoint;
	/** Distance since start [km] */
	private double _distance_km;
	/** Time since start [s] */
	private long _time_s;
	/** pause time [min] at this waypoint */
	private int _waypointDuration = 0;
	private String _waypointType = null;
	private String _waypointSym = null;
	private String _waypointComment = null;
	private String _waypointDescription = null;
	private String _waypointLink = null;
	/** index of the track point linked to the way point */
	private  int _linkIndex = INVALID_INDEX;
/* BiselliW -> */


	/**
	 * Constructor
	 * @param inValueArray array of String values
	 * @param inFieldList list of fields
	 * @param inOptions creation options such as units
	 */
	public DataPoint(String[] inValueArray, FieldList inFieldList, PointCreateOptions inOptions)
	{
		// save data
		_fieldValues = inValueArray;
		// save list of fields
		_fieldList = inFieldList;
		// Remove double quotes around values
		removeQuotes(_fieldValues);
		// parse fields into objects
           		parseFields(null, inOptions);
	}


	/**
	 * Parse the string values into objects eg Coordinates
	 * @param inField field which has changed, or null for all
	 * @param inOptions creation options such as units
	 * @since BiselliW
	 */
	private void parseFields(Field inField, PointCreateOptions inOptions)
	{
		if (inOptions == null) inOptions = new PointCreateOptions();
		if (inField == null || inField == Field.LATITUDE) {
			_latitude = new Latitude(getFieldValue(Field.LATITUDE));
		}
		if (inField == null || inField == Field.LONGITUDE) {
			_longitude = new Longitude(getFieldValue(Field.LONGITUDE));
		}
		if (inField == null || inField == Field.ALTITUDE)
		{
			Unit altUnit = inOptions.getAltitudeUnits();
			if (_altitude != null && _altitude.getUnit() != null) {
				altUnit = _altitude.getUnit();
			}
			_altitude = new Altitude(getFieldValue(Field.ALTITUDE), altUnit);
		}
		if (inField == null || inField == Field.TIMESTAMP) {
			_timestamp = new TimestampUtc(getFieldValue(Field.TIMESTAMP));
		}
		if (inField == null || inField == Field.WAYPT_NAME) {
			_waypointName = getFieldValue(Field.WAYPT_NAME);
		}
		if (inField == null || inField == Field.NEW_SEGMENT)
		{
			String segmentStr = getFieldValue(Field.NEW_SEGMENT);
			if (segmentStr != null) {segmentStr = segmentStr.trim();}
			_startOfSegment = (segmentStr != null && (segmentStr.equals("1") || segmentStr.toUpperCase().equals("Y")));
		}
/* @since BiselliW */
		if (inField == null || inField == Field.WAYPT_TYPE) {
			_waypointType = getFieldValue(Field.WAYPT_TYPE);
		}
		if (inField == null || inField == Field.WAYPT_SYM) {
			_waypointSym = getFieldValue(Field.WAYPT_SYM);
		}
 		if (inField == null || inField == Field.COMMENT) {
			_waypointComment = getFieldValue(Field.COMMENT);
		}
		if (inField == null || inField == Field.DESCRIPTION) {
			_waypointDescription = getFieldValue(Field.DESCRIPTION);
		}
		if (inField == null || inField == Field.WAYPT_DUR) {
			try {
				String dur = getFieldValue(Field.WAYPT_DUR);
				_waypointDuration = new Integer(dur);
			}
			catch (NumberFormatException e) {
				_waypointDuration = 0;
			}
		}
		if (inField == null || inField == Field.WAYPT_FLAG) {
			String value = getFieldValue(Field.WAYPT_FLAG);
			_isWaypoint = (value != null) && value.equals("1");
		}
		if (inField == null || inField == Field.WAYPT_LINK) {
			_waypointLink = getFieldValue(Field.WAYPT_LINK);
		}
/* BiselliW -> */
	}


	/**
	 * Constructor for additional points (eg interpolated, photos)
	 * @param inLatitude latitude
	 * @param inLongitude longitude
	 * @param inAltitude altitude
	 */
	public DataPoint(Coordinate inLatitude, Coordinate inLongitude, Altitude inAltitude)
	{
		// Only these three fields are available
		_fieldValues = new String[3];
		Field[] fields = {Field.LATITUDE, Field.LONGITUDE, Field.ALTITUDE};
		_fieldList = new FieldList(fields);
		_latitude = inLatitude;
		_fieldValues[0] = inLatitude.output(Coordinate.FORMAT_NONE);
		_longitude = inLongitude;
		_fieldValues[1] = inLongitude.output(Coordinate.FORMAT_NONE);
		if (inAltitude == null) {
			_altitude = Altitude.NONE;
		}
		else {
			_altitude = inAltitude;
			_fieldValues[2] = "" + inAltitude.getValue();
		}
		_timestamp = new TimestampUtc(null);
	}


	/**
	 * Get the value for the given field
	 * @param inField field to interrogate
	 * @return value of field
	 */
	public String getFieldValue(Field inField)
	{
		return getFieldValue(_fieldList.getFieldIndex(inField));
	}


	/**
	 * Get the value at the given index
	 * @param inIndex index number starting at zero
	 * @return field value, or null if not found
	 */
	private String getFieldValue(int inIndex)
	{
		if (_fieldValues == null || inIndex < 0 || inIndex >= _fieldValues.length)
			return null;
		return _fieldValues[inIndex];
	}


	/**
	 * Set (or edit) the specified field value
	 * @param inField Field to set
	 * @param inValue value to set
	 * @param inUndo true if undo operation, false otherwise
	 */
	public void setFieldValue(Field inField, String inValue, boolean inUndo)
	{
		// See if this data point already has this field
		int fieldIndex = _fieldList.getFieldIndex(inField);
		// Add to field list if necessary
		if (fieldIndex < 0)
		{
			// If value is empty & field doesn't exist then do nothing
			if (inValue == null || inValue.equals(""))
			{
				return;
			}
			// value isn't empty so extend field list
			fieldIndex = _fieldList.extendList(inField);
		}
		// Extend array of field values if necessary
		if (fieldIndex >= _fieldValues.length)
		{
			resizeValueArray(fieldIndex);
		}
		// Set field value in array
		_fieldValues[fieldIndex] = inValue;
		// Increment edit count on all field edits except segment
		if (inField != Field.NEW_SEGMENT) {
			setModified(inUndo);
		}
		// Change Coordinate, Altitude, Name or Timestamp fields after edit
		if (_altitude != null && _altitude.getUnit() != null) {
			// Altitude already present so reuse format
			parseFields(inField, null); // current units will be used
		}
		else {
			// use default altitude format from config
			parseFields(inField, Config.getUnitSet().getDefaultOptions());
		}
	}

	/**
	 * Either increment or decrement the modify count, depending on whether it's an undo or not
	 * @param inUndo true for undo, false otherwise
	 */
	public void setModified(boolean inUndo)
	{
		if (!inUndo) {
			_modifyCount++;
		}
		else {
			_modifyCount--;
		}
	}

	/**
	 * @return field list for this point
	 */
	public FieldList getFieldList()
	{
		return _fieldList;
	}

	/** @param inFlag true for start of track segment */
	public void setSegmentStart(boolean inFlag)
	{
		setFieldValue(Field.NEW_SEGMENT, inFlag?"1":null, false);
	}

	/**
	 * Mark the point for deletion
	 * @param inFlag true to delete, false to keep
	 */
	public void setMarkedForDeletion(boolean inFlag) {
		_markedForDeletion = inFlag;
	}

	/** @return latitude */
	public Coordinate getLatitude()
	{
		return _latitude;
	}
	/** @return longitude */
	public Coordinate getLongitude()
	{
		return _longitude;
	}
	/** @return true if point has altitude */
	public boolean hasAltitude()
	{
		return _altitude != null && _altitude.isValid();
	}
	/** @return altitude */
	public Altitude getAltitude()
	{
		return _altitude;
	}
	/** @return true if point has timestamp */
	public boolean hasTimestamp()
	{
		return _timestamp.isValid();
	}
	/** @return timestamp */
	public Timestamp getTimestamp()
	{
		return _timestamp;
	}

	/**
	 * @return waypoint name, if any
	 * @implNote check for null
	*/
	public String getWaypointName()
	{
		if (_waypointName == null) return "";
		return _waypointName;
	}

	/** @return true if start of new track segment */
	public boolean getSegmentStart()
	{
		return _startOfSegment;
	}

	/** @return true if point marked for deletion */
	public boolean getDeleteFlag()
	{
		return _markedForDeletion;
	}

	/**
	 * @return true if point has a waypoint name
	 */
	public boolean isWaypoint()
	{
		return (_waypointName != null && !_waypointName.equals(""));
	}

	/**
	 * @return true if point is a waypoint 
	 * @implNote check for null
	 */
	public boolean isWayPoint()
	{
		return (_waypointName != null) && !_waypointName.equals("") && _isWaypoint;
	}


	/**
	 * @return waypoint hyperlink
	 * @implNote check for null
	*/
	public String getWaypointLink()
	{
		if (_waypointLink == null) return "";
		return _waypointLink;
	}

	/**
	 * @return true if point has been modified since loading
	 */
	public boolean isModified()
	{
		return _modifyCount > 0;
	}

	/**
	 * Add an altitude offset to this point, and keep the point's string value in sync
	 * @param inOffset offset as double
	 * @param inUnit unit of offset, feet or metres
	 * @param inDecimals number of decimal places
	 */
	public void addAltitudeOffset(double inOffset, Unit inUnit, int inDecimals)
	{
		if (hasAltitude())
		{
			_altitude.addOffset(inOffset, inUnit, inDecimals);
			_fieldValues[_fieldList.getFieldIndex(Field.ALTITUDE)] = _altitude.getStringValue(null);
			setModified(false);
		}
	}

	/**
	 * Add a time offset to this point
	 * @param inOffset offset to add (-ve to subtract)
	 */
	public void addTimeOffsetSeconds(long inOffset)
	{
		if (hasTimestamp())
		{
			_timestamp.addOffsetSeconds(inOffset);
			_fieldValues[_fieldList.getFieldIndex(Field.TIMESTAMP)] = _timestamp.getText(null);
			setModified(false);
		}
	}

	/**
	 * Set the photo for this data point
	 * @param inPhoto Photo object
	 */
	public void setPhoto(Photo inPhoto) {
		_photo = inPhoto;
		_modifyCount++;
	}

	/**
	 * @return associated Photo object
	 */
	public Photo getPhoto() {
		return _photo;
	}

	/**
	 * Set the audio clip for this point
	 * @param inAudio audio object
	 */
	public void setAudio(AudioClip inAudio) {
		_audio = inAudio;
		_modifyCount++;
	}

	/**
	 * @return associated audio object
	 */
	public AudioClip getAudio() {
		return _audio;
	}

	/**
	 * Attach the given media object according to type
	 * @param inMedia either a photo or an audio clip
	 */
	public void attachMedia(MediaObject inMedia)
	{
		if (inMedia != null) {
			if (inMedia instanceof Photo) {
				setPhoto((Photo) inMedia);
				inMedia.setDataPoint(this);
			}
			else if (inMedia instanceof AudioClip) {
				setAudio((AudioClip) inMedia);
				inMedia.setDataPoint(this);
			}
		}
	}

	/**
	 * @return true if the point is valid
	 */
	public boolean isValid()
	{
		return _latitude.isValid() && _longitude.isValid();
	}

	/**
	 * @return true if the point has either a photo or audio attached
	 */
	public boolean hasMedia() {
		return _photo != null || _audio != null;
	}

	/**
	 * Calculate the number of radians between two points (for distance calculation)
	 * @param inPoint1 first point
	 * @param inPoint2 second point
	 * @return angular distance between points in radians
	 */
	public static double calculateRadiansBetween(DataPoint inPoint1, DataPoint inPoint2)
	{
		if (inPoint1 == null || inPoint2 == null)
			return 0.0;
		final double TO_RADIANS = Math.PI / 180.0;
		// Get lat and long from points
		double lat1 = inPoint1.getLatitude().getDouble() * TO_RADIANS;
		double lat2 = inPoint2.getLatitude().getDouble() * TO_RADIANS;
		double lon1 = inPoint1.getLongitude().getDouble() * TO_RADIANS;
		double lon2 = inPoint2.getLongitude().getDouble() * TO_RADIANS;
		// Formula given by Wikipedia:Great-circle_distance as follows:
		// angle = 2 arcsin( sqrt( (sin ((lat2-lat1)/2))^^2 + cos(lat1)cos(lat2)(sin((lon2-lon1)/2))^^2))
		double firstSine = Math.sin((lat2-lat1) / 2.0);
		double secondSine = Math.sin((lon2-lon1) / 2.0);
		double term2 = Math.cos(lat1) * Math.cos(lat2) * secondSine * secondSine;
		double answer = 2 * Math.asin(Math.sqrt(firstSine*firstSine + term2));
		// phew
		return answer;
	}


	/**
	 * Resize the value array
	 * @param inNewIndex new index to allow
	 */
	private void resizeValueArray(int inNewIndex)
	{
		int newSize = inNewIndex + 1;
		if (newSize > _fieldValues.length)
		{
			String[] newArray = new String[newSize];
			System.arraycopy(_fieldValues, 0, newArray, 0, _fieldValues.length);
			_fieldValues = newArray;
		}
	}


	/**
	 * Remove all single and double quotes surrounding each value
	 * @param inValues array of values
	 */
	private static void removeQuotes(String[] inValues)
	{
		if (inValues == null) {return;}
		for (int i=0; i<inValues.length; i++)
		{
			inValues[i] = removeQuotes(inValues[i]);
		}
	}

	/**
	 * Remove any single or double quotes surrounding a value
	 * @param inValue value to modify
	 * @return modified String
	 */
	private static String removeQuotes(String inValue)
	{
		if (inValue == null) {return "";}
		final int len = inValue.length();
		if (len <= 1) {return inValue;}
		// get the first and last characters
		final char firstChar = inValue.charAt(0);
		final char lastChar  = inValue.charAt(len-1);
		if (firstChar == lastChar)
		{
			if (firstChar == '\"' || firstChar == '\'') {
				return inValue.substring(1, len-1);
			}
		}
		return inValue;
	}

	/**
	 * Get string for debug
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "[Lat=" + getLatitude().toString() + ", Lon=" + getLongitude().toString() + "]";
	}

	/**
	 * Set the waypoint name
	 * @implNote only internal use for tour navigator
	 * @param inWaypointName waypoint name
	 * @author BiselliW
	*/
	public void setWaypointName(String inWaypointName)
	{
		_waypointName = inWaypointName;
// todo setFieldValue(Field.WAYPT_NAME, inWaypointName, false);
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @return waypoint comment, if any
	 * @author BiselliW
	*/
	public String getWaypointComment()
	{
		if (_waypointComment == null) return "";
		return _waypointComment;
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @param comment waypoint comment
	 * @author BiselliW
	 */
	public void setWaypointComment(String comment)
	{
		_waypointComment = comment;
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @return waypoint description, if any
	 * @author BiselliW
	*/
	public String getWaypointDescription()
	{
		if (_waypointDescription == null) return "";
		return _waypointDescription;
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @return waypoint symbol, if any
	 * @author BiselliW
	*/
	public String getWaypointSymbol()
	{
		if (_waypointSym == null) return "";
		return _waypointSym;
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @return waypoint type, if any
	 * @author BiselliW
	 */
	public String getWaypointType()
	{
		if (_waypointType == null) return "";
		return _waypointType;
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @return waypoint comment or description, if any
	 * @author BiselliW
	*/
	public String getWaypointCommentOrDescription()
	{
		String str = getWaypointComment();
		if (str.equals("")) str = getWaypointDescription();
		return str;
	}

	/**
	 * Set waypoint duration [min]
	 * @implNote only internal use for tour navigator
	 * @param waypointDuration waypoint duration [min]
	 * @author BiselliW
	*/
	public void setWaypointDuration(int waypointDuration)
	{
		_waypointDuration = waypointDuration;
	}

	public int getWaypointDuration()
	{
		return _waypointDuration;
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @return true if point is a track point
	 * @author BiselliW
	 */
	public boolean _isTrackpoint() { return !_isWaypoint; }

	/**
	 *
	 * @return true if the trackpoint is named
	 */
	public boolean isNamedTrackpoint()
	{
		return !_isWaypoint && ((_waypointName != null) && !_waypointName.equals(""));
	}
	
	/**
	 * unmark a point as Route Point
	 * @implNote only internal use for Tour Navigator
	 *
	 * @author BiselliW
	 */
	public void clearWayPointLink()
	{
		if (_linkIndex > 0)
		{
			_routePointName = "";
		}
		_linkIndex = INVALID_INDEX;
	}

	/**
	 * mark a point as Route Point
	 * @implNote only internal use for tour navigator
	 *
	 * @param  inName name to be assigned to the route point
	 * @param  inLinkIndex index of the linked way point within the track
	 * @author BiselliW
	 */
	public void makeRoutePoint(String inName, int inLinkIndex)
	{
		_isWaypoint = false;
		_routePointName = inName;
		_linkIndex = inLinkIndex;
	}

	/**
	 * @implNote only internal use for tour navigator
	 *
	 * @return index of a point to which the current point is linked
	 * @since BiselliW
	 */
	public int getLinkIndex ()
	{
		return _linkIndex;
	}

	/**
	 * set the link index of a point
	 * @implNote only internal use for tour navigator
	 *
	 * @param inIndex index of a waypoint to which the current point shall be linked
	 * @author BiselliW
	 */
	public void setLinkIndex (int inIndex) { _linkIndex = inIndex; }

	/**
	 * checks if a point is a Route Point:
	 * - a track point with name
	 * - a track point linked to a waypoint
	 * @implNote only internal use for tour navigator
	 *
	 * @return true if the point is a Route Point
	 * @author BiselliW
	 */
	public boolean isRoutePoint()
	{
		return !getRoutePointName().equals("");
	}
	
	/**
	 * @implNote only internal use for tour navigator
	 *
	 * @return Route Point name
	 * @author BiselliW
	*/
	public String getRoutePointName()
	{
		if (_isWaypoint) return "";
		
		if ((_routePointName != null) && !_routePointName.equals("")) return _routePointName;

		if ((_waypointName != null) && !_waypointName.equals("")) return _waypointName;
		
		return "";
	}

	/**
	 * Clear distance and time since start of the track
	 * @implNote only internal use for tour navigator
	 * @author BiselliW
	 */
	public void clearRealtimeData()
	{
		if (_distance_km > 0) {
			_distance_km = 0;
		}
		_time_s = 0;
	}

	/**
	 * Set distance since start
	 * @implNote only internal use for tour navigator
	 *
	 * @param distance_km distance since start [km]
	 * @author BiselliW
	 */
	public void setRealtimeDataDist(double distance_km)	{ _distance_km = distance_km; }

	/**
	 * Set Time since start [s]
	 *
	 * @param time_s Time since start [s]
	 * @implNote only internal use for tour navigator
	 * @author BiselliW
	 */
	public void setRealtimeDataTime(long time_s) { _time_s = time_s; }

	/**
	 * @implNote only internal use for tour navigator
	 * @author BiselliW
	 *
	 * @return Time since start [s]
	 */
	public long getTime() { return _time_s; }

	/**
	 * @implNote only internal use for tour navigator
	 * @author BiselliW
	 *
	 * @return distance since start [km]
	 */
	public double getDistance() { return _distance_km;	}

	/**
	 * Calculate the number of radians between two points (for distance calculation)
	 * @implNote only internal use for tour navigator
	 *
	 * @param inLatitude Latitude in degrees
	 * @param inLongitude Longitude in degrees
	 * @return angular distance between points in radians
	 * @author BiselliW
	 */
	public double calculateRadiansBetween(double inLatitude, double inLongitude)
	{
		final double TO_RADIANS = Math.PI / 180.0;
		// Get lat and long from points
		double lat1 = _latitude.getDouble() * TO_RADIANS;
		double lat2 = inLatitude * TO_RADIANS;
		double lon1 = _longitude.getDouble() * TO_RADIANS;
		double lon2 = inLongitude * TO_RADIANS;
		// Formula given by Wikipedia:Great-circle_distance as follows:
		// angle = 2 arcsin( sqrt( (sin ((lat2-lat1)/2))^^2 + cos(lat1)cos(lat2)(sin((lon2-lon1)/2))^^2))
		double firstSine = Math.sin((lat2-lat1) / 2.0);
		double secondSine = Math.sin((lon2-lon1) / 2.0);
		double term2 = Math.cos(lat1) * Math.cos(lat2) * secondSine * secondSine;
		double answer = 2 * Math.asin(Math.sqrt(firstSine*firstSine + term2));
		// phew
		return answer;
	}

}
