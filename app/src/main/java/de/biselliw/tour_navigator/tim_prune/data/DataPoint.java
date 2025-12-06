package de.biselliw.tour_navigator.tim_prune.data;

import java.util.TimeZone;

import de.biselliw.tour_navigator.stubs.AudioClip;
import de.biselliw.tour_navigator.stubs.Photo;
import de.biselliw.tour_navigator.tim.prune.data.Altitude;
import de.biselliw.tour_navigator.tim.prune.data.Coordinate;
import de.biselliw.tour_navigator.tim.prune.data.Distance;
import de.biselliw.tour_navigator.tim.prune.data.FieldList;
import de.biselliw.tour_navigator.tim.prune.data.Latitude;
import de.biselliw.tour_navigator.tim.prune.data.Longitude;
import de.biselliw.tour_navigator.tim.prune.data.PointCreateOptions;
import de.biselliw.tour_navigator.tim.prune.data.Timestamp;
import de.biselliw.tour_navigator.tim.prune.data.TimestampUtc;
import de.biselliw.tour_navigator.tim.prune.data.Unit;
import de.biselliw.tour_navigator.tim.prune.data.UnitSet;

/**
 * Class to represent a single data point in the series
 * including all its fields
 * Can be either a track point or a waypoint
 * @author tim.prune
 * @author BiselliW
 * @implNote added _waypointDuration, _isWaypoint, _waypointComment, _waypointDescription; extension: pause time [min] at this waypoint
 * @since 26.1
 */
public class DataPoint
{
	/** Array of Strings holding raw values */
	private String[] _fieldValues;
	/** list of field definitions */
	private final FieldList _fieldList;
	/** Special fields for coordinates */
	private Coordinate _latitude = null, _longitude = null;
	private Altitude _altitude = null;

    /**
     * @todo use Speed
     * * /
	private Speed _hSpeed = null, _vSpeed = null;
*/
	private Timestamp _timestamp = null;
	private SourceInfo _sourceInfo = null;
	private int _originalIndex = -1;

	/** Attached photo */
	private Photo _photo = null;
    /**
     * @todo use media
	/** Attached audio clip */
    /**
     * @todo use media
     * */
	private AudioClip _audio = null;

	private String _waypointName = null;
	private boolean _startOfSegment = false;
	private int _modifyCount = 0;

	private static FieldList _sharedLatLonAltFieldList = null;

	/** extensions:
	 * @since 22.2.005
	 */
	public static final int INVALID_INDEX = -32000;
	private String _routePointName = null;
	private boolean _isWaypoint;
	/** Distance since start [km] */
	private double _distance_km;
	/** Time since start [s] */
	private long _time_s;
	/** pause time [min] at this trackpoint */
	private int _duration = 0;
	// todo _wptType
	private String _wptType = null;
	private String _symbol = null;
	/** way point or trackpoint comment */ 
	private String _comment = null;
	/** way point or trackpoint description */
	private String _description = null;
	/** way point or trackpoint link */
	private String _webLink = null;
	/** index of the track point linked to the way point */
	private  int _linkIndex = INVALID_INDEX;


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
	 * @implNote: add values of fields WAYPT_TYPE, WAYPT_SYM, COMMENT, DESCRIPTION, WAYPT_DUR, WAYPT_FLAG, WAYPT_LINK
	 * @since 22.2.005
	 */
	private void parseFields(Field inField, PointCreateOptions inOptions)
	{
		if (inOptions == null) inOptions = new PointCreateOptions();
		if (inField == null || inField == Field.LATITUDE) {
			_latitude = Latitude.make(getFieldValue(Field.LATITUDE));
		}
		if (inField == null || inField == Field.LONGITUDE) {
			_longitude = Longitude.make(getFieldValue(Field.LONGITUDE));
		}
		if (inField == null || inField == Field.ALTITUDE)
		{
			final Unit altUnit;
			if (_altitude != null && _altitude.getUnit() != null) {
				altUnit = _altitude.getUnit();
			}
			else {
				altUnit = inOptions.getAltitudeUnits();
			}
			_altitude = new Altitude(getFieldValue(Field.ALTITUDE), altUnit);
		}
        /**
         * @todo use speed
		if (inField == null || inField == Field.SPEED)
		{
			final Unit speedUnit;
			if (_hSpeed != null && _hSpeed.getUnit() != null) {
				speedUnit = _hSpeed.getUnit();
			}
			else {
				speedUnit = inOptions.getSpeedUnits();
			}
			_hSpeed = Speed.createOrNull(getFieldValue(Field.SPEED), speedUnit);
		}
		if (inField == null || inField == Field.VERTICAL_SPEED)
		{
			final Unit vspeedUnit;
			final boolean isInverted;
			if (_vSpeed != null && _vSpeed.getUnit() != null)
			{
				vspeedUnit = _vSpeed.getUnit();
				isInverted = _vSpeed.isInverted();
			}
			else
			{
				vspeedUnit = inOptions.getVerticalSpeedUnits();
				isInverted = !inOptions.getVerticalSpeedsUpwards();
			}
			_vSpeed = Speed.createOrNull(getFieldValue(Field.VERTICAL_SPEED), vspeedUnit, isInverted);
		}
         */

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
			_startOfSegment = (segmentStr != null && (segmentStr.equals("1") || segmentStr.equalsIgnoreCase("Y")));
		}
		
		
		if (inField == null || inField == Field.WAYPT_TYPE) {
			_wptType = getFieldValue(Field.WAYPT_TYPE);
		}
		if (inField == null || inField == Field.SYMBOL) {
			_symbol = getFieldValue(Field.SYMBOL);
		}
 		if (inField == null || inField == Field.COMMENT) {
			_comment = getFieldValue(Field.COMMENT);
		}
		if (inField == null || inField == Field.DESCRIPTION) {
			_description = getFieldValue(Field.DESCRIPTION);
		}
		if (inField == null || inField == Field.WAYPT_DUR) {
			try {
				String dur = getFieldValue(Field.WAYPT_DUR);
				_duration = new Integer(dur);
			}
			catch (NumberFormatException e) {
				_duration = 0;
			}
		}
		if (inField == null || inField == Field.WAYPT_FLAG) {
			String value = getFieldValue(Field.WAYPT_FLAG);
			_isWaypoint = (value != null) && value.equals("1");
		}
		if (inField == null || inField == Field.WAYPT_LINK) {
			_webLink = getFieldValue(Field.WAYPT_LINK);
		}
	}

	/**
	 * Constructor for additional points without altitude
	 * @param inLatitude latitude
	 * @param inLongitude longitude
	 */
	public DataPoint(Coordinate inLatitude, Coordinate inLongitude) {
		this(inLatitude, inLongitude, null);
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
		_fieldList = getSharedLatLonFieldList();
		// TODO: Check if either latitude or longitude is null - in which case do what?
		_latitude = inLatitude;
		_fieldValues[0] = inLatitude.toString();
		_longitude = inLongitude;
		_fieldValues[1] = inLongitude.toString();
		if (inAltitude == null) {
			_altitude = Altitude.NONE;
		}
		else {
			_altitude = inAltitude;
			_fieldValues[2] = "" + inAltitude.getValue();
		}
		_timestamp = new TimestampUtc(null);
	}

	/** Simplified constructor just giving raw values for latitude and longitude */
	public DataPoint(double inLatitude, double inLongitude) {
		this(Latitude.make(inLatitude), Longitude.make(inLongitude), null);
	}

	/** @return a shared field list just containing latitude longitude and altitude */
	private static FieldList getSharedLatLonFieldList()
	{
		if (_sharedLatLonAltFieldList == null)
		{
			Field[] fields = {Field.LATITUDE, Field.LONGITUDE, Field.ALTITUDE};
			_sharedLatLonAltFieldList = new FieldList(fields);
		}
		return _sharedLatLonAltFieldList;
	}

	/**
	 * Get the value for the given field
	 * @param inField field to interrogate
	 * @return value of field
	 */
	public String getFieldValue(Field inField) {
		return getFieldValue(_fieldList.getFieldIndex(inField));
	}

	/**
	 * Get the value at the given index
	 * @param inIndex index number starting at zero
	 * @return field value, or null if not found
	 */
	private String getFieldValue(int inIndex)
	{
		if (_fieldValues == null || inIndex < 0 || inIndex >= _fieldValues.length) {
			return null;
		}
		return _fieldValues[inIndex];
	}

	/**
	 * Set (or edit) the waypoint name field
	 * @param inValue name to set
	 */
	public void setWaypointName(String inValue)
	{
		setRawFieldValue(Field.WAYPT_NAME, inValue);
		setModified(false);
		_waypointName = inValue;
	}

	/**
	 * Set (or edit) the specified field value, without specifying a unit set
	 * @param inField Field to set
	 * @param inValue value to set
	 * @param inUndo true if undo operation, false otherwise
	 */
	public void setFieldValue(Field inField, String inValue, boolean inUndo) {
		setFieldValue(inField, inValue, null, inUndo);
	}

	/**
	 * Set (or edit) the specified field value
	 * @param inField Field to set
	 * @param inValue value to set
	 * @param inUnitSet unit set to use
	 * @param inUndo true if undo operation, false otherwise
	 */
	public void setFieldValue(Field inField, String inValue, UnitSet inUnitSet, boolean inUndo)
	{
		setRawFieldValue(inField, inValue);
		// Increment edit count on all field edits except segment
		if (inField != Field.NEW_SEGMENT) {
			setModified(inUndo);
		}

		final boolean needUnits;
		if (inField == Field.ALTITUDE) {
			needUnits = _altitude == null || _altitude.getUnit() == null;
		}
        /**
         * @todo use speed
		else if (inField == Field.SPEED) {
			needUnits = _hSpeed == null || _hSpeed.getUnit() == null;
		}
		else if (inField == Field.VERTICAL_SPEED) {
			needUnits = _vSpeed == null || _vSpeed.getUnit() == null;
		}
         */
		else {
			needUnits = false;
		}
		if (needUnits && inUnitSet == null && inValue != null && !inValue.equals("")) {
			throw new IllegalArgumentException("Units required to set field " + inField.getName());
		}

		parseFields(inField, inUnitSet == null ? null : inUnitSet.getDefaultOptions());
	}

	/**
	 * Extend the field list if necessary and set the raw field value
	 * @param inField field to set
	 * @param inValue value to set
	 */
	private void setRawFieldValue(Field inField, String inValue)
	{
		if (inField == null) {
			return;
		}
		// See if this data point already has this field
		int fieldIndex = _fieldList.getFieldIndex(inField);
		// Add to field list if necessary
		if (fieldIndex < 0)
		{
			// If value is empty & field doesn't exist then do nothing
			if (inValue == null || inValue.equals("")) {
				return;
			}
			// value isn't empty so extend field list
			fieldIndex = _fieldList.addField(inField);
		}
		// Extend array of field values if necessary
		if (fieldIndex >= _fieldValues.length) {
			resizeValueArray(fieldIndex);
		}
		// Set field value in array
		_fieldValues[fieldIndex] = inValue;
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
	public FieldList getFieldList() {
		return _fieldList;
	}

	/** @param inFlag true for start of track segment */
	public void setSegmentStart(boolean inFlag) {
		setFieldValue(Field.NEW_SEGMENT, inFlag ? "1" : null, false);
	}

	/** @return latitude */
	public Coordinate getLatitude() {
		return _latitude;
	}

	/** @return longitude */
	public Coordinate getLongitude() {
		return _longitude;
	}

	/** @return true if point has altitude */
	public boolean hasAltitude() {
		return _altitude != null && _altitude.isValid();
	}

	/** @return altitude */
	public Altitude getAltitude() {
		return _altitude;
	}

    /** @return true if point has horizontal speed (loaded as field)
     * @todo use speed
    * /
	public boolean hasHSpeed() {
		return _hSpeed != null && _hSpeed.isValid();
	}

	/** @return horizontal speed * /
	public Speed getHSpeed() {
		return _hSpeed;
	}

	/** @return true if point has vertical speed (loaded as field) * /
	public boolean hasVSpeed() {
		return _vSpeed != null && _vSpeed.isValid();
	}

	/** @return vertical speed * /
	public Speed getVSpeed() {
		return _vSpeed;
	}

    /**
     * @return true if the point is valid
     */

    /** @return true if point has timestamp */
	public boolean hasTimestamp() {
		return _timestamp != null && _timestamp.isValid();
	}

	/** @return timestamp */
	public Timestamp getTimestamp() {
		return _timestamp;
	}

	/** @return waypoint name, if any */
	public String getWaypointName() {
		return (_waypointName == null ? "" : _waypointName);
	}

	/** @return true if start of new track segment */
	public boolean getSegmentStart() {
		return _startOfSegment;
	}

	/**
	 * @return true if point has a waypoint name
	 */
	public boolean isWaypoint() {
		return _waypointName != null && !_waypointName.equals("");
	}

	/**
	 * @return true if point is a waypoint 
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public boolean isWayPoint()
	{
		return (_waypointName != null) && !_waypointName.equals("") && _isWaypoint;
	}


	/**
	 * @return true if point has been modified since loading
	 */
	public boolean isModified() {
		return _modifyCount > 0;
	}

	/**
	 * Compare two DataPoint objects to see if they are duplicates
	 * @param inOther other object to compare
	 * @return true if the points are equivalent
	 */
	public boolean isDuplicate(DataPoint inOther)
	{
		if (inOther == null) return false;
		if (_longitude == null || _latitude == null
			|| inOther._longitude == null || inOther._latitude == null)
		{
			return false;
		}
		/* Make sure photo points aren't specified as duplicates
		 * @todo use media
		 * /
		if (_photo != null) return false;
		*/
		// Compare latitude and longitude
		if (!_longitude.equals(inOther._longitude) || !_latitude.equals(inOther._latitude))
		{
			return false;
		}
		// Note that conversion from decimal to dms can make non-identical points into duplicates
		// Compare waypoint name (if any)
		if (!isWaypoint()) {
			return !inOther.isWaypoint();
		}
		return (inOther._waypointName != null && inOther._waypointName.equals(_waypointName));
	}

	/**
	 * Set the altitude including units
	 * @param inValue value as string
	 * @param inUnit units
	 * @param inIsUndo true if it's an undo operation
	 */
	public void setAltitude(String inValue, Unit inUnit, boolean inIsUndo)
	{
		_altitude = new Altitude(inValue, inUnit);
		setFieldValue(Field.ALTITUDE, inValue, inIsUndo);
		setModified(inIsUndo);
	}

	/**
	 * Set the altitude from another altitude value
	 */
	public void setAltitude(Altitude inAltitude)
	{
		_altitude = new Altitude(inAltitude);
		setFieldValue(Field.ALTITUDE, _altitude.getStringValue(null), false);
		setModified(false);
	}


	/**
	 * Set the photo for this data point
	 * @param inPhoto Photo object
     * @todo use media
	 */
	public void setPhoto(Photo inPhoto) {
		_photo = inPhoto;
		_modifyCount++;
	}

	/**
	 * @return associated Photo object
     * @todo use media
	 */
	public Photo getPhoto() {
		return _photo;
	}

	/**
	 * Set the audio clip for this point
	 * @param inAudio audio object
     * @todo use media
	 * /
	public void setAudio(AudioClip inAudio) {
		_audio = inAudio;
		_modifyCount++;
	}
    */

	/**
	 * @return associated audio object
     * @todo use media
	 */
	public AudioClip getAudio() {
		return _audio;
	}

	/**
	 * @return true if the point is valid
	 */
	public boolean isValid() {
		return _latitude != null && _longitude != null;
	}

	/**
	 * @return true if the point has either a photo or audio attached
     * @todo use media
	 * /
	public boolean hasMedia() {
//		return _photo != null || _audio != null;
        return false;
	}

	/**
	 * @return name of attached photo and/or audio
     * @todo use media
	 * /
	public String getMediaName()
	{
		String mediaName = null;
 		if (_photo != null) {
			mediaName = _photo.getName();
		}
		if (_audio != null)
		{
			if (mediaName == null) {
				mediaName = _audio.getName();
			}
			else {
				mediaName = mediaName + ", " + _audio.getName();
			}
		}
		return mediaName;
	}

	/**
	 * Calculate the number of radians between two points (for distance calculation)
	 * @param inPoint1 first point
	 * @param inPoint2 second point
	 * @return angular distance between points in radians
	 */
	public static double calculateRadiansBetween(DataPoint inPoint1, DataPoint inPoint2)
	{
		if (inPoint1 == null || inPoint2 == null) {
			return 0.0;
		}
		// Get lat and long from points, in degrees
		double lat1 = inPoint1.getLatitude().getDouble();
		double lat2 = inPoint2.getLatitude().getDouble();
		double lon1 = inPoint1.getLongitude().getDouble();
		double lon2 = inPoint2.getLongitude().getDouble();
		return Distance.calculateRadiansBetween(lat1, lon1, lat2, lon2);
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
	 * @return a clone object with copied data
     * @todo use clonePoint
	 * /
	public DataPoint clonePoint()
	{
		// Copy all values (note that photo is not copied)
		String[] valuesCopy = new String[_fieldValues.length];
		System.arraycopy(_fieldValues, 0, valuesCopy, 0, _fieldValues.length);

		PointCreateOptions options = new PointCreateOptions();
		if (_altitude != null) {
			options.setAltitudeUnits(_altitude.getUnit());
		}
		// Make new object to hold cloned data
		DataPoint point = new DataPoint(valuesCopy, _fieldList, options);
		// Copy the speed information
		if (hasHSpeed()) {
			point.getHSpeed().copyFrom(_hSpeed);
		}
		if (hasVSpeed()) {
			point.getVSpeed().copyFrom(_vSpeed);
		}
		return point;
	}
*/

	public void setSourceInfo(SourceInfo inInfo) {
		_sourceInfo = inInfo;
	}

	public SourceInfo getSourceInfo() {
		return _sourceInfo;
	}

	public void setOriginalIndex(int inIndex) {
		_originalIndex = inIndex;
	}

	public int getOriginalIndex() {
		return _originalIndex;
	}

	/**
	 * Remove all single and double quotes surrounding each value
	 * @param inValues array of values
	 */
	private static void removeQuotes(String[] inValues)
	{
		if (inValues != null)
		{
			for (int i=0; i<inValues.length; i++) {
				inValues[i] = removeQuotes(inValues[i]);
			}
		}
	}

	/**
	 * Remove any single or double quotes surrounding a value
	 * @param inValue value to modify
	 * @return modified String
	 */
	private static String removeQuotes(String inValue)
	{
		if (inValue == null) {
			return null;
		}
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
	 */
	public String toString() {
		return "[Lat=" + getLatitude().toString() + ", Lon=" + getLongitude().toString() + "]";
	}
	
	/** 
	 * @return way point or trackpoint comment, if any 
	 * @author BiselliW
	 * @since 22.2.005
	*/
	public String getComment()
	{
		if (_comment == null) return "";
		return _comment;
	}

	/** 
	 * @return way point or trackpoint description, if any 
	 * @author BiselliW
	 * @since 22.2.005
	*/
	public String getDescription()
	{
		if (_description == null) return "";
		return _description;
	}

	/**
	 * @return way point or trackpoint weblink, if any
	 * @author BiselliW
	 * @since 22.2.005
	*/
	public String getWebLink()
	{
		if (_webLink == null) return "";
		return _webLink;
	}

	/**
	 * unmark a point as Route Point
	 * @author BiselliW
	 * @since 22.2.006
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
	 * set the link index of a point
	 * @param inIndex index of a waypoint to which the current point shall be linked
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public void setLinkIndex (int inIndex)
	{
		_linkIndex = inIndex;
	}

	/**
	 * @return true if point is a track point
	 * @author Walter Biselli
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public boolean isTrackPoint()
	{
		return !_isWaypoint;
	}

	/**
	 * @return true if the trackpoint has a name
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public boolean isNamedTrackpoint()
	{
		return !_isWaypoint && ((_waypointName != null) && !_waypointName.equals(""));
	}

	/**
	 * checks if a point is a Route Point:
	 * - a track point with name
	 * - a track point linked to a waypoint
	 * @return true if the point is a Route Point
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public boolean isRoutePoint()
	{
		return !getRoutePointName().equals("");
	}
	
	/** 
	 * @return Route Point name, either the internal name or the name of the track point (no way point!)  
	 * @author BiselliW
	 * @see #makeRoutePoint(String, int)
	 * @since 22.2.006
	*/
	public String getRoutePointName()
	{
		if (_isWaypoint) return "";
		
		if ((_routePointName != null) && !_routePointName.equals("")) return _routePointName;

		if ((_waypointName != null) && !_waypointName.equals("")) return _waypointName;
		
		return "";
	}

	/**
	 * mark a point as Route Point
	 * @param  inName name to be assigned to the route point
	 * @param  inLinkIndex index of the linked way point within the track
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public void makeRoutePoint(String inName, int inLinkIndex)
	{
		_isWaypoint = false;
		_routePointName = inName;
		_linkIndex = inLinkIndex;
	}

	/**
	 * Set the waypoint name
	 * @ param inWaypointName waypoint name
	 * @author BiselliW
	 * @since 22.2.006
     * @todo setWaypointName
	* /
	public void setWaypointName(String inWaypointName)
	{
		setFieldValue(Field.WAYPT_NAME, inWaypointName, false);
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @return waypoint symbol, if any
	 * @author BiselliW
	*/
	public String getWaypointSymbol()
	{
		if (_symbol == null) return "";
		return _symbol;
	}

	/**
	 * @implNote only internal use for tour navigator
	 * @return waypoint type, if any
	 * @author BiselliW
	 */
	public String getWaypointType()
	{
		if (_wptType == null) return "";
		return _wptType;
	}

	/**
	 * @return index of a point to which the current point is linked
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public int getLinkIndex ()
	{
		return _linkIndex;
	}

	/** 
	 * @return waypoint duration (pause) [min]
	 * @author BiselliW
	 * @since 22.2.006
	*/
	public int getWaypointDuration()
	{
		return _duration;
	}

	/**
	 * Set waypoint duration
	 * @param inDuration pause) [min]
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public void setWaypointDuration(int inDuration)
	{
		_duration = inDuration;
	}

	/**
	 * set timestamp 
	 * @param inTimestamp Time stamp
	 * @param inTimezone  Time zone 
	 * @author BiselliW
	 * @since 22.2.006
	*/
	public void setTimestamp(Timestamp inTimestamp, TimeZone inTimezone)
	{
		setFieldValue(Field.TIMESTAMP, inTimestamp.getText(inTimezone), false);
		_timestamp = inTimestamp;
	}

	/**
	 * Set distance since start
	 * @param distance_km distance since start [km]
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public void setDistance(double distance_km)
	{
		_distance_km = distance_km;
	}
	
	/**
	 * @return distance since start [km] 
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public double getDistance() { return _distance_km; }

	/**
	 * Set Time since start
	 * @param time_s Time since start [s]
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public void setTime(long time_s)
	{
		_time_s = time_s;
	}
	
	/**
	 * @return Time since start [s]
	 * @author BiselliW
	 * @since 22.2.006
	 */ 
	public long getTime() { return _time_s; }

	/**
	 * Calculate the number of radians between two points (for distance calculation)
	 * @param inLatitude Latitude in degrees
	 * @param inLongitude Longitude in degrees
	 * @return angular distance between points in radians
	 * @since 22.2.006
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

// ###############################################

	/**
	 * Clear distance and time since start of the track
	 * @implNote only internal use for tour navigator
	 * @author BiselliW
	 */
	public void _clearRealtimeData()
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
	public void _setRealtimeDataDist(double distance_km)	{ _distance_km = distance_km; }

	/**
	 * Set Time since start [s]
	 *
	 * @param time_s Time since start [s]
	 * @implNote only internal use for tour navigator
	 * @author BiselliW
	 */
	public void _setRealtimeDataTime(long time_s) { _time_s = time_s; }

}
