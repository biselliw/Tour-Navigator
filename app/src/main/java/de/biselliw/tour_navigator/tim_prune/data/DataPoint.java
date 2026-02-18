package de.biselliw.tour_navigator.tim_prune.data;

import java.util.TimeZone;

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.stubs.AudioClip;
import de.biselliw.tour_navigator.stubs.Photo;
import tim.prune.data.Altitude;
import tim.prune.data.Coordinate;
import tim.prune.data.Distance;
import tim.prune.data.FieldList;
import tim.prune.data.Latitude;
import tim.prune.data.Longitude;
import tim.prune.data.PointCreateOptions;
import tim.prune.data.Timestamp;
import tim.prune.data.TimestampUtc;
import tim.prune.data.Unit;
import tim.prune.data.UnitSet;

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
    /** Altitude */
	private Altitude _altitude = null;

    // @todo use Speed
//	private Speed _hSpeed = null, _vSpeed = null;

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
	 */
	public static final int INVALID_INDEX = -32000, OUT_OF_TRACK = -1;
    public static final double INVALID_VALUE = -9999.9999;
    /** index of the track point within the array of track points */
    private int _index = INVALID_INDEX;
    /** index of a track point linked to a waypoint:<br>
     * - for trackpoints: index of the waypoint<br>
     * - for waypoints: index of the first trackpoint which links to this waypoint */
    private  int _linkIndex = INVALID_INDEX;
    /** index of the next trackpoint which links to the same waypoint */
    private  int _linkIndexNext = INVALID_INDEX;
    /** for waypoints: counter for the number of trackpoints which link to this waypoint */
    private int _linkCount = 0;
	private String _routePointName = null;
	private boolean _isWaypoint;
    private boolean _isProtectedWaypoint;

    /** Distance since start [km] */
	private double _distance_km;
	/** Time since start [s] */
	private long _time_s;
	/** break time [min] at this trackpoint */
	private int _duration = 0;
	private String _wptType = null;
	private String _symbol = null;
	/** waypoint or trackpoint comment */
	private String _comment = null;
	/** waypoint or trackpoint description */
	private String _description = null;
	/** waypoint or trackpoint link */
	private String _webLink = null;


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
	 * @return true if point has been modified since loading
	 */
	public boolean isModified() {
		return _modifyCount > 0;
	}

	/**
	 * Compare two DataPoint objects to see if they are duplicates
	 * @param inOther other object to compare
     * @implNote biselliw: compare longitude/latitude using min. allowed difference
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
		 *
		if (_photo != null) return false;
		*/
        // Compare latitude and longitude
        double diffLong = _longitude.getDouble() - inOther.getLongitude().getDouble();
        if (Math.abs(diffLong) > 0.0001)
            return false;
        double diffLat = _latitude.getDouble() - inOther.getLatitude().getDouble();
        if (Math.abs(diffLat) > 0.0001)
            return false;

//		if (!_longitude.equals(inOther._longitude) || !_latitude.equals(inOther._latitude)) return false;

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

	/*
	 * @return true if the point has either a photo or audio attached
     * @ todo use media
	 * /
	public boolean hasMedia() {
//		return _photo != null || _audio != null;
        return false;
	}

	/*
	 * @ return name of attached photo and/or audio
     * @ todo use media
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
*/

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

    public double distanceTo(double inLatitude, double inLongitude) {
        return Distance.convertRadiansToDistance(calculateRadiansBetween(inLatitude, inLongitude));
    }

    public double distanceTo(DataPoint inPoint) {
        return inPoint._distance_km - _distance_km;
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

    public void setIndex(int inIndex) {
        _index = inIndex;
    }

    public int getIndex() {
        return _index;
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
     * @implNote BiselliW check null
	 */
	@NonNull
    public String toString() {
        String type = _isWaypoint ? "WP " : "TP ";
        if (_isProtectedWaypoint) type = "p" + type;
        String name = !getWaypointName().isEmpty() ? getWaypointName() :
                (_routePointName != null) ? _routePointName : "";
        String lat = getLatitude() != null ? getLatitude().toString() : "null";
        String lon = getLongitude() != null ? getLongitude().toString() : "null";
        String res = "[Lat=" + lat + ", Lon=" + lon + "]";
        if (name != null) res = name + ": " + res;
		return type + res;
	}
	
	/** 
	 * @return  or trackpoint comment, if any
	 * @author BiselliW
	 * @since 22.2.005
	*/
    @NonNull
	public String getComment()
	{
		if (_comment == null) return "";
		return _comment;
	}

    /**
     * @param inDescription waypoint or trackpoint description
     * @author BiselliW
     * @since 22.2.005
     */
    public void setDescription(String inDescription)
    {
        _description = inDescription;
    }
	/** 
	 * @return waypoint or trackpoint description, if any
	 * @author BiselliW
	 * @since 22.2.005
	*/
    @NonNull
	public String getDescription()
	{
		if (_description == null) return "";
		return _description;
	}

    /**
     * @param inWebLink waypoint or trackpoint weblink
     * @author BiselliW
     * @since 22.2.005
     */
    public void setWebLink(String inWebLink)
    {
        _webLink = inWebLink;
    }

    /**
	 * @return waypoint or trackpoint weblink, if any
	 * @author BiselliW
	 * @since 22.2.005
	*/
    @NonNull
	public String getWebLink()
	{
		if (_webLink == null) return "";
		return _webLink;
	}

	/**
	 * unmark a point as Route Point
	 * @author BiselliW
	 */
	public void clearWayPointLink()
	{
		if (_linkIndex > 0)
			_routePointName = "";
		_linkIndex = _linkIndexNext = INVALID_INDEX;
	}

	/**
	 * set the link index of a point
	 * @param inIndex for trackpoints: index of the waypoint<br>
     *                - for waypoints: index of the first trackpoint which links to this waypoint
	 * @author BiselliW
	 */
	public void setLinkIndex(int inIndex)
	{
		_linkIndex = inIndex;
        _linkCount = 1;
	}

    /**
     * set the link index of a point
     * @param inIndex index of the next trackpoint which links to the same waypoint
     * @author BiselliW
     */
    public void setLinkIndexNext (int inIndex)
    {
        _linkIndexNext = inIndex;
        _linkCount++;
    }

    /**
     * get the link index of a point
     * @return - for trackpoints: index of the waypoint<br>
     *         - for waypoints: index of the first trackpoint which links to this waypoint
     * @author BiselliW
     */
    public int getLinkIndex()
    {
        return _linkIndex;
    }

    /**
     * get the link index of a point
     * @return index of the next trackpoint which links to the same waypoint
     * @author BiselliW
     */
    public int getLinkIndexNext()
    {
        return _linkIndexNext;
    }

	/**
	 * @return true if point is a track point
	 * @author BiselliW
	 */
	public boolean isTrackPoint()
	{
		return !_isWaypoint;
	}

	/**
	 * @return true if the trackpoint has a name
	 * @author BiselliW
	 */
	public boolean isNamedTrackpoint()
	{
		return !_isWaypoint && ((_waypointName != null) && !_waypointName.isEmpty());
	}

    /**
     * @return true if point has a waypoint name
     */
    public boolean isWaypoint() {
        if (_waypointName == null) return false;
        return !_waypointName.isEmpty();
    }

    /**
	 * @return true if point is a waypoint 
	 * @author BiselliW
	 */
	public boolean isWayPoint()
	{
		return (_waypointName != null) && !_waypointName.equals("") && _isWaypoint;
	}

    /**
     * @return true if point is a protected waypoint
     * @author BiselliW
     */
    public boolean isProtectedWayPoint()
    {
        return isWayPoint() && _isProtectedWaypoint;
    }

	/**
	 * checks if a point is a Route Point:<br>
	 * - a track point with name<br>
	 * - a track point linked to a waypoint
	 * @return true if the point is a Route Point
	 * @author BiselliW
	 */
	public boolean isRoutePoint()
	{
		return !getRoutePointName().isEmpty();
	}
	
	/** 
	 * @return Route Point name, either the internal name or the name of the track point (no waypoint!)
	 * @author BiselliW
	 * @see #makeRoutePoint(String, int)
	*/
    @NonNull
	public String getRoutePointName()
	{
		if (_isWaypoint) return "";
		
		if ((_routePointName != null) && !_routePointName.isEmpty()) return _routePointName;

		if ((_waypointName != null) && !_waypointName.isEmpty()) return _waypointName;
		
		return "";
	}

	/**
	 * mark a point as Route Point
	 * @param  inName name to be assigned to the route point
	 * @param  inLinkIndex index of the linked waypoint within the track
	 * @author BiselliW
	 */
	public void makeRoutePoint(String inName, int inLinkIndex)
	{
		_isWaypoint = false;
		_routePointName = inName;
		_linkIndex = inLinkIndex;
        _linkIndexNext = INVALID_INDEX;
	}

    /**
     * mark a point as Route Point with data from another point
     * @param  fromOther source point
     * @author BiselliW
     */
    public void makeRoutePointFrom(DataPoint fromOther)
    {
        _isWaypoint = false;
        _routePointName = fromOther._waypointName;
        if (_routePointName.isBlank())
            _routePointName = fromOther._routePointName;
        setFieldValue(Field.WAYPT_NAME,_routePointName,false);
        _linkIndex = INVALID_INDEX;
        _linkIndexNext = INVALID_INDEX;
        setFieldValue(Field.WAYPT_TYPE, fromOther._wptType, false);
        _wptType = fromOther._wptType;
        setFieldValue(Field.SYMBOL, fromOther._symbol == null ? fromOther._wptType : fromOther._symbol, false);
        setFieldValue(Field.COMMENT, fromOther._comment, false);
        _comment = fromOther._comment;
        setFieldValue(Field.DESCRIPTION, fromOther._description, false);
        _description = fromOther._description;
        setFieldValue(Field.WAYPT_LINK, fromOther._webLink, false);
        _webLink = fromOther._webLink;
    }


    /**
     * downgrade a route point to a simple trackpoint
     * @author BiselliW
     */
    public void removeRoutePoint() {
        setWaypointName("");
        _waypointName = _routePointName = null;
        _linkIndex = _linkIndexNext = INVALID_INDEX;
        _isWaypoint = _isProtectedWaypoint = false;
    }

    /**
     * todo mark a point as invalid (for deletion)
     * @author BiselliW
     */
    public void makePointInvalid() {
        _latitude = _longitude = null;
        _waypointName = _routePointName = null;
//        _linkIndex = INVALID_INDEX;
        _isWaypoint = _isProtectedWaypoint = false;
        _duration = 0;
    }

    /**
     * @implNote only internal use for tour navigator
     * @param inSymbol waypoint symbol
     * @author BiselliW
     * @todo delete?
     */
    public void setWaypointSymbol(String inSymbol)
    {
        _symbol = inSymbol;
    }

	/**
	 * @implNote only internal use for Tour Navigator
	 * @return waypoint symbol, if any
	 * @author BiselliW
	*/
    @NonNull
    public String getWaypointSymbol()
	{
		if (_symbol == null) return "";
		return _symbol;
	}

    /**
     * @implNote only internal use for tour navigator
     * @param inType waypoint type
     * @author BiselliW
     * @todo delete?
     */
    public void setWaypointType(String inType)
    {
        _wptType = inType;
    }

    /**
	 * @implNote only internal use for tour navigator
	 * @return waypoint type, if any
	 * @author BiselliW
	 */
    @NonNull
	public String getWaypointType()
	{
		if (_wptType == null) return "";
		return _wptType;
	}

	/**
     * Get the break time at a route point
	 * @return waypoint duration (break) [min]
	 * @author BiselliW
	*/
	public int getWaypointDuration()
	{
		return _duration;
	}

	/**
     * Set the break time at a route point
	 * @param inDuration break [min]
	 * @author BiselliW
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
	 */
	public void setDistance(double distance_km)
	{
		_distance_km = distance_km;
	}
	
	/**
	 * @return distance since start [km] 
	 * @author BiselliW
	 */
	public double getDistance() { return _distance_km; }

	/**
	 * Set Time since start
	 * @param time_s Time since start [s]
	 * @author BiselliW
	 */
	public void setTime(long time_s)
	{
		_time_s = time_s;
	}
	
	/**
	 * @return Time since start [s]
	 * @author BiselliW
	 */
	public long getTime() { return _time_s; }

	/**
	 * Calculate the number of radians between two points (for distance calculation)
	 * @param inLatitude Latitude in degrees
	 * @param inLongitude Longitude in degrees
	 * @return angular distance between points in radians
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
     * mark a point as protected waypoint
     * @author BiselliW
     */
    public void makeProtectedWaypoint()
    {
        _isWaypoint = true;
        _isProtectedWaypoint = true;
    }

}
