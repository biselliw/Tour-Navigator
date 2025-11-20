package de.biselliw.tour_navigator.tim_prune.load.xml;

/*
    Tour Navigator is a GPX file based app. It creates and observes the
    timetable of a tour in realtime

    Copyright (C) 2022 Walter Biselli (BiselliW)

	File has been reworked from GpxHandler.java as part of GpsPrune version 22.2:
	https://github.com/activityworkshop/GpsPrune/blob/master/src/tim/prune/load/xml/GpxHandler.java

    Copyright (C) Activity Workshop

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, get it from http://www.gnu.org/licenses/


 * modified by Walter Biselli (BiselliW):
 * v. 22.2.006 - 2022-11-30
 *             - distinguish between loaded way points and trackpoints
 *             - load extra tags from gpx file: _metadata (name, author), _comment, _duration
 *             - reorder point fields

*/

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;

import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim.prune.load.TrackNameList;
import de.biselliw.tour_navigator.tim.prune.load.xml.GpxTag;
import de.biselliw.tour_navigator.tim_prune.data.Field;

/**
 * Class for handling specifics of parsing Gpx files
 *
 * @see de.biselliw.tour_navigator.data
 * @implSpec used package org.xml.sax has been replaced by the SAX2 Attributes interface, which includes Namespace support.
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/org/xml/sax/package-summary.html">Package org.xml.sax</a>
 * @author tim.prune
 * @implNote BiselliW: new GPX tags (WAYPT_CMT), new order of point fields in class Field
 *
 */
 /* new order of point fields (old one):<ul></ul>
 * <li> 0 ()  : WAYPT_NAME</li>>
 * <li> 1 (0) : LATITUDE</li>>
 * * -
 * * - 2 (1) : LONGITUDE
 * * - 3 (2) : ALTITUDE (_elevation)
 * * - 4 ( ) : TIMESTAMP (_time)
 * * - 5 ()  : WAYPT_CMT (_comment)
 * * - 6 (7) : DESCRIPTION
 * * - 7 ()  : WAYPT_DUR (Pause)
 * * - 8 (6) : WAYPT_TYPE (_type)
 * * - 9 ()  : WAYPT_SYM symbol name
 * * -10 (5) : NEW_SEGMENT 1 if _startSegment && !_insideWaypoint
 * * -11 ()  : WAYPT_FLAG !_isTrackPoint ? "1" : "0";
 * * -12 ()  : WAYPT_LINK
 */

// todo replace package org.xml.sax by the SAX2 Attributes interface
public class GpxHandler extends XmlHandler
{
    /**
     * TAG for log messages.
     */
    static final String TAG = "GpxHandler";
    private static final boolean DEBUG = false; // Set to true to enable logging

    // Extensions by BiselliW
	private boolean _insideMetaData = true;
	private boolean _metaAuthorSet = false;
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

	// Extensions by BiselliW
	private GpxTag _metaName = new GpxTag();
	private GpxTag _metaAuthor = new GpxTag(), _duration = new GpxTag();

	private GpxTag _currentTag = null;
	private ArrayList<String[]> _pointList = new ArrayList<String[]>();
	private ArrayList<String> _linkList = new ArrayList<String>();
	private TrackNameList _trackNameList = new TrackNameList();

    private boolean _isOutdooractive = false;
    private String _OutdooractiveSrcPrefix = "outdooractive.21430.";
    private GpxTag _source = new GpxTag();
    private String _OutdooractiveLink = "https://www.schwarzwaldverein-tourenportal.de/poi/";
    private boolean _isFirstTrackPoint = true;



	/**
	 * Receive the start of a tag
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/org/xml/sax/ContentHandler.html#startElement(java.lang.String,%20java.lang.String,%20java.lang.String,%20org.xml.sax.Attributes)">docs.oracle.com: startElement</a>
	 */
	public void startElement(String uri, String localName, String qName,
		Attributes attributes) throws SAXException
	{
		// Read the parameters for metadata, waypoints and track points
		String tag = qName.toLowerCase();
		if (DEBUG) Log.d(TAG, "startElement() tag = "+tag);

		if (
			tag.equals("metadata") || 
			tag.equals("wpt") || tag.equals("trkpt") || tag.equals("rtept"))
		{
			if (tag.equals("metadata")) {
				_insideMetaData = true;
				_metaAuthorSet = false;
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
            _source.setValue(null);
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
			if (_insideMetaData) {
				if (!_metaAuthorSet) {
					_currentTag = _metaName;
				}
				else {
					_currentTag = _metaAuthor;
				}				
			}
			else if (_insidePoint) {
				_currentTag = _pointName;
			}
			else if (_trackNum < 0)
			{
				_currentTag = _fileTitle;
			}
			else {
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
        else if (tag.equals("oa:Extension")) {
            _isOutdooractive = true;
        }
        else if (tag.equals("src")) {
            _currentTag = _source;
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
		else if (tag.equals("trk")) {
			_insideTrack = true;
			_trackNum++;
			_trackName.setValue(null);
			_currentTag = null;
		}
		else if (tag.equals("author")) {
			_metaAuthorSet = true;
		}
		super.startElement(uri, localName, qName, attributes);
	}


	/**
	 * Process end tag
	 * @see <a href="https://docs.oracle.com/javase/7/docs/api/org/xml/sax/ContentHandler.html#endElement(java.lang.String,%20java.lang.String,%20java.lang.String)">docs.oracle.com: startElement</a>
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
		throws SAXException
	{
		String tag = qName.toLowerCase();
		if (DEBUG) Log.d(TAG, "endElement() tag = "+tag);
		if (tag.equals("metadata"))
		{
			_insideMetaData = false;
			// process meta data
			processMetaData();
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
			_metaAuthorSet = false;
		}
		else if (tag.equals("wpt") || tag.equals("trkpt") || tag.equals("rtept"))
		{
            /* Create Outdooractive link to a POI if no web link is provided */
            if (tag.equals("wpt") && (_link != null) && (_link.getValue() == null) && (_source != null))
            {
                String source = _source.getValue();
                if (source != null) {
                    if (source.startsWith(_OutdooractiveSrcPrefix)) {
                        source = source.substring(_OutdooractiveSrcPrefix.length());
                        /* link is only allowed to web site - no app! */
                        _link.setValue(_OutdooractiveLink.concat(source));
                    }
                }
            }

            /* don't load waypoints which are simple trackpoints */
            /* no longer used by Outdooractive ?
			if (_insideWaypoint)
				if ((_type != null) && (_type.getValue() != null))
					if (_type.getValue().equals("TrackPt"))
						_insidePoint = false;
             */
            /* don't load Outdooractive trackpoints which are only for routing */
            if (_isTrackPoint) {
                if ((_symbol != null) && (_symbol.getValue() == null))
                    _pointName.setValue(null);
                _isFirstTrackPoint = false;
            }
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
		String ret;
		if (inVariable == null) 
		{
			ret = inValue;
			if (DEBUG) Log.d(TAG,"checkCharacters (null," + inValue + ") -> " + ret); 
		}
		else
		{
			ret = inVariable + inValue;
			if (DEBUG) Log.d(TAG,"checkCharacters (" + inVariable + "," + inValue + ") -> " + ret); 
		}
		
		return ret;
	}


	/**
	 * process meta data
	 * @author BiselliW
	 * @since 22.2.006
	 */
	private void processMetaData()
	{
		// get Name from parsed GPX tag <metadata><name> */
		metaName        = _metaName.getValue();
		// get Description from parsed GPX tag <metadata><description> */
		metaDescription = _description.getValue(); 
		// get Author from parsed GPX tag <metadata><author><name>
		metaAuthor      = _metaAuthor.getValue();
		// get Time from parsed GPX tag <metadata><time>
		metaTime        = _time.getValue();
		// get Link from parsed GPX tag <metadata><link>
		metaLink        = _link.getValue();
	}
	
	/**
	 * Process a point, either a waypoint or track point
	 * @implNote new GPX tags (WAYPT_CMT), new order of point fields in class Field
	 * @since 22.2.006
	 */
	private void processPoint()
	{
		// Put the values into a String array matching the order in getFieldArray()
		String[] values = new String[13];

		values[0] = _pointName.getValue();
		values[1] = _latitude;
		values[2] = _longitude;
		values[3] = _elevation.getValue();
		values[4] = _time.getValue();

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
	 * @implNote new arrangement of fields
	 * @since 22.2.006
	 */
	public Field[] getFieldArray()
	{
		final Field[] fields = {
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
	 * @see XmlHandler#getDataArray()
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
		String fileTitle = _fileTitle.getValue();
		if ((fileTitle == null) || (fileTitle.equals("")))
				fileTitle = _metaName.getValue(); 
		return fileTitle;
	}
}
