/**
* @since WB
* new order of point fields (old one):
* - 0 ()  : WAYPT_NAME
* - 1 (0) : LATITUDE
* - 2 (1) : LONGITUDE
* - 3 (2) : ALTITUDE (_elevation)
* - 4 ( ) : TIMESTAMP (_time)
* - 5 ()  : WAYPT_CMT (_comment)
* - 6 (7) : DESCRIPTION
* - 7 ()  : WAYPT_DUR (Pause)
* - 8 (6) : WAYPT_TYPE (_type)
* - 9 ()  : WAYPT_SYM symbol name
* -10 (5) : NEW_SEGMENT 1 if _startSegment && !_insideWaypoint
* -11 ()  : WAYPT_FLAG !_isTrackPoint ? "1" : "0";
* -12 ()  : WAYPT_LINK

load extra tags from gpx file: _metadata (name, author), _comment, _duration
*/

package de.biselliw.tour_navigator.files;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import tim.prune.load.xml.GpxTag;
import tim.prune.load.xml.XmlHandler;

import tim.prune.data.Field;
import tim.prune.load.TrackNameList;

import de.biselliw.tools.debug.Log;

/**
 * Class for handling specifics of parsing Gpx files
 */
// TODO WB added WAYPT_CMT

public class GpxHandler extends XmlHandler
{
// TODO WB added
	private boolean _insideMetaData = true;
	private boolean _MetaDataAuthorSet = false;
	private boolean _insidePoint = false;
	private boolean _insideWaypoint = false;
	private boolean _startSegment = true;
	private boolean _insideTrack = false;
	private boolean _isTrackPoint = false;
	private int _trackNum = -1;
	private GpxTag _fileTitle = new GpxTag();
	private GpxTag _pointName = new GpxTag(), _trackName = new GpxTag();
	private String _latitude = null, _longitude = null;
	private GpxTag _elevation = new GpxTag(), _time = new GpxTag();
	private GpxTag _type = new GpxTag(), _symbol = new GpxTag(), _description = new GpxTag();
	private GpxTag _link = new GpxTag(), _comment = new GpxTag();
/* TODO WB added */
	private GpxTag _metadataAuthor = new GpxTag(), _duration = new GpxTag();
	private GpxTag _trackDescription = new GpxTag();
/* WB -> */
	private GpxTag _currentTag = null;
	private ArrayList<String[]> _pointList = new ArrayList<String[]>();
	private ArrayList<String> _linkList = new ArrayList<String>();
	private TrackNameList _trackNameList = new TrackNameList();

	/**
	 * TAG for log messages.
	 */
	static final String TAG = "GpxHandler";
	private static final boolean DEBUG = false; // Set to true to enable logging

	/**
	 * Receive the start of a tag
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName,
		Attributes attributes) throws SAXException
	{
		// Read the parameters for metadata, waypoints and track points
		String tag = qName.toLowerCase();
		if (DEBUG) Log.d(TAG, "startElement() tag = "+tag);

		if (
/* @since WB */
			tag.equals("metadata") || 
			tag.equals("wpt") || tag.equals("trkpt") || tag.equals("rtept"))
		{
			if (tag.equals("metadata")) {
				_insideMetaData = true;
			} 
			else 
			{
				_insideMetaData = false;
				_insidePoint = true;
				_insideWaypoint = tag.equals("wpt");
				_isTrackPoint = tag.equals("trkpt");
			}
			
			final int numAttributes = attributes.getLength();
			for (int i=0; i<numAttributes; i++)
			{
				String att = attributes.getQName(i).toLowerCase();
				if (att.equals("lat")) {_latitude = attributes.getValue(i);}
				else if (att.equals("lon")) {_longitude = attributes.getValue(i);}
			}
			_elevation.setValue(null);
			_pointName.setValue(null);
			_time.setValue(null);
			_type.setValue(null);
			_link.setValue(null);
			_description.setValue(null);
			_symbol.setValue(null);
			_comment.setValue(null);
			_duration.setValue(null);
		}
		else if (tag.equals("ele")) {
			_currentTag = _elevation;
		}
		else if (tag.equals("name"))
		{
/* @since WB */
			if (_insideMetaData) {
				if (!_MetaDataAuthorSet) {
					_currentTag = _fileTitle; 
				}
				else {
					_currentTag = _metadataAuthor;
				}				
			}
/* @since WB */
			else if (_insidePoint) {
				_currentTag = _pointName;
			}
			else 
			{
				_currentTag = _trackName;
			}
		}
		else if (tag.equals("time")) {
			_currentTag = _time;
		}
		else if (tag.equals("type")) {
			_currentTag = _type;
		}
		else if (tag.equals("description") || tag.equals("desc")) {
			_description.setValue(null);
			_currentTag = _description;
		}
		else if (tag.equals("cmt")) {
			_currentTag = _comment;
		}
		else if (tag.equals("sym")) {
			_currentTag = _symbol;
		}		
		else if (tag.equals("link")) {
			_link.setValue(attributes.getValue("href"));
			_currentTag = null;
		}
		else if (tag.equals("pause")) {
			_currentTag = _duration;
		}		
		else if (tag.equals("trkseg")) {
			_startSegment = true;
		}
		/* @since WB */
		else if (tag.equals("trk")) {
			_insideTrack = true;
			_trackNum++;
			_trackName.setValue(null);
			_currentTag = null;
		}
		else if (tag.equals("author")) {
			_MetaDataAuthorSet = true;
		}
		
		super.startElement(uri, localName, qName, attributes);
	}


	/**
	 * Process end tag
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
		throws SAXException
	{
		String tag = qName.toLowerCase();
		if (DEBUG) Log.d(TAG, "endElement() tag = "+tag);
/* @since WB */
		if (tag.equals("metadata"))
		{
			_insideMetaData = false;
			// process meta data
			processmetaData();
		}
		else if ((tag.equals("desc")) && _insideTrack)
		{
			// only first value is used
			if (trackDescription.equals(""))
				trackDescription = _description.getValue();
		}
		else if (tag.equals("trk")) {
			_insideTrack = false;
		}
		else if (tag.equals("author"))
		{
			_MetaDataAuthorSet = false;
		}
		else if (tag.equals("wpt") || tag.equals("trkpt") || tag.equals("rtept"))
		{
			/* don't load waypoints which are simple trackpoints
			 * @since WB 
			 * */
			if (_insideWaypoint)
				if ((_type != null) && _type.getValue().equals("TrackPt"))
					_insidePoint = false;
			if (_insidePoint)
					processPoint();
			_insideWaypoint = false;
			_insidePoint = false;
		}

		_currentTag = null;
		super.endElement(uri, localName, qName);
	}


	/**
	 * Process character text (inside tags or between them)
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		String value = new String(ch, start, length);
		if (_currentTag != null) {
			_currentTag.setValue(checkCharacters(_currentTag.getValue(), value));
		}
		super.characters(ch, start, length);
	}


	/**
	 * Check to concatenate partially-received values, if necessary
	 * @param inVariable variable containing characters received until now
	 * @param inValue new value received
	 * @return concatenation
	 */
	private static String checkCharacters(String inVariable, String inValue)
	{
		if (inVariable == null) {return inValue;}
		return inVariable + inValue;
	}


	/**
	 * process meta data
	 * @since WB 
	 */
	private void processmetaData()
	{
		metaDescription = _description.getValue(); 
		metaAuthor      = _metadataAuthor.getValue();
		metaLink        = _link.getValue();
	}
	
	/**
	 * Process a point, either a waypoint or track point
     * @since WB
	 */
	private void processPoint()
	{
		// Put the values into a String array matching the order in getFieldArray()
		String[] values = new String[13];
// TODO WB modified
		values[0] = _pointName.getValue();
		values[1] = _latitude;
		values[2] = _longitude;
		values[3] = _elevation.getValue();
		values[4] = _time.getValue();
//		if (_insideWaypoint) 

		values[5] = _comment.getValue();
		values[6] = _description.getValue();
		values[7] = _duration.getValue(); // Pause

		values[8] = _type.getValue();
		values[9] = _symbol.getValue();
		if (_startSegment && !_insideWaypoint) {
			values[10] = "1";
			_startSegment = false;
		}
		// Field.WAYPT_FLAG
		values[11] = !_isTrackPoint ? "1" : "0";
		values[12] = _link.getValue();

		_pointList.add(values);
		_trackNameList.addPoint(_trackNum, _trackName.getValue(), _isTrackPoint);
		_linkList.add(_link.getValue());
		if (DEBUG)	Log.d(TAG, "processPoint "+values[0]);
	}


	/**
	 * @see tim.prune.load.xml.XmlHandler#getFieldArray()
         * @since WB
	 */
	public Field[] getFieldArray()
	{
		final Field[] fields = {
/* @since WB: new arrangements */
			Field.WAYPT_NAME, 
			Field.LATITUDE, Field.LONGITUDE, Field.ALTITUDE, 
			Field.TIMESTAMP, 
			Field.COMMENT, 
			Field.DESCRIPTION, Field.WAYPT_DUR, Field.WAYPT_TYPE, Field.WAYPT_SYM, 
			Field.NEW_SEGMENT, 
			Field.WAYPT_FLAG,
			Field.WAYPT_LINK
			};

		return fields;
	}


	/**
	 * Return the parsed information as a 2d array
	 * @see tim.prune.load.xml.XmlHandler#getDataArray()
	 */
	public String[][] getDataArray()
	{
		int numPoints = _pointList.size();
		// construct data array
		String[][] result = new String[numPoints][];
		for (int i=0; i<numPoints; i++)
		{
			result[i] = _pointList.get(i);
		}
		return result;
	}

	/**
	 * @return array of links, or null if none
	 */
	public String[] getLinkArray()
	{
		int numPoints = _linkList.size();
		boolean hasLink = false;
		String[] result = new String[numPoints];
		for (int i=0; i<numPoints; i++)
		{
			result[i] = _linkList.get(i);
			if (result[i] != null) {hasLink = true;}
		}
		if (!hasLink) {result = null;}
		return result;
	}

	/**
	 * @return track name list
	 */
	public TrackNameList getTrackNameList() {
		return _trackNameList;
	}

	/**
	 * @return file title
	 */
	public String getFileTitle() {
		return _fileTitle.getValue();
	}
}
