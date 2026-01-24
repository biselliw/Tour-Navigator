package de.biselliw.tour_navigator.tim_prune.load.xml;

/*
    Tour Navigator is a GPX file based app. It creates and observes the
    timetable of a tour in realtime

    Copyright (C) 2025 Walter Biselli (BiselliW)

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
 * @since 26.1
 * @todo create parent class for project
*/

import java.util.ArrayList;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import tim.prune.data.ExtensionInfo;
import tim.prune.data.FieldGpx;
import tim.prune.data.FieldXml;
import tim.prune.data.FileType;
import tim.prune.load.xml.GpxTag;
import de.biselliw.tour_navigator.tim_prune.data.Field;

/**
 * Class for handling specifics of parsing Gpx files
 *
 * @see de.biselliw.tour_navigator.data
 * @implSpec used package org.xml.sax has been replaced by the SAX2 Attributes interface, which includes Namespace support.
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/org/xml/sax/package-summary.html">Package org.xml.sax</a>
 * @author tim.prune
 * @implNote BiselliW: new GPX tags (WAYPT_CMT), new order of point fields in class Field
 * @todo replace package org.xml.sax by the SAX2 Attributes interface
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
 * * - 7 ()  : WAYPT_DUR (break)
 * * - 8 (6) : WAYPT_TYPE (_type)
 * * - 9 ()  : WAYPT_SYM symbol name
 * * -10 (5) : NEW_SEGMENT 1 if _startSegment && !_insideWaypoint
 * * -11 ()  : WAYPT_FLAG !_isTrackPoint ? "1" : "0";
 * * -12 ()  : WAYPT_LINK
 */

public class GpxHandler extends XmlHandler {
    /**
     * TAG for log messages.
     */
    static final String TAG = "GpxHandler";
    private static final boolean DEBUG = false; // Set to true to enable logging

    private boolean _insidePoint = false;
    private boolean _insideWaypoint = false;
    private boolean _insideExtensions = false;
    private boolean _startSegment = true;

    // Extensions by BiselliW
    private final boolean _storeExtensions = false;
    private boolean _insideMetaData = false;
    private boolean _metaAuthorSet = false;
    private boolean _isTrackPoint = false;

    private int _trackNum = -1;
    private final GpxTag _fileTitle = new GpxTag(), _fileDescription = new GpxTag();
    private final GpxTag _pointName = new GpxTag(), _trackName = new GpxTag();
    private final GpxTag _elevation = new GpxTag(), _time = new GpxTag();
    private final GpxTag _type = new GpxTag(), _description = new GpxTag();
    private final GpxTag _link = new GpxTag(), _comment = new GpxTag();
    private final GpxTag _sym = new GpxTag();


    // Extensions by BiselliW
    private final GpxTag _fileAuthor = new GpxTag();
    private final GpxTag _duration = new GpxTag();

    private GpxTag _currentTag = null;
    private final ExtensionInfo _extensionInfo = new ExtensionInfo();
    private final ArrayList<String[]> _pointList = new ArrayList<>();
    private final ArrayList<String> _linkList = new ArrayList<>();

    private boolean _isOutdooractive = false;

    private Stack<String> _extensionTags = null;
    private FieldGpx _gpxField = null;

    private final GpxTag _source = new GpxTag();
    private final String _OutdooractiveSrcPrefix = "outdooractive.21430.";
    private String _OutdooractiveSrcPostfix = ".21430.";
    private final String _OutdooractivePoiLink = "https://www.schwarzwaldverein-tourenportal.de/poi/";
    private final String _toubizPrefix = "toubiz-"; // "toubiz-tta-poi.21430.";
    private final String _toubizLink = "https://www.schwarzwald-tourismus.info/attraktionen/";
    private boolean _isFirstTrackPoint = true;

    /**
     * Constructor, setting up the fields
     */
    public GpxHandler() {
        final Field[] fields = {Field.WAYPT_NAME,
                Field.LATITUDE, Field.LONGITUDE, Field.ALTITUDE,
                Field.TIMESTAMP,
                Field.COMMENT,
                Field.DESCRIPTION, Field.WAYPT_DUR, Field.WAYPT_TYPE, Field.SYMBOL,
                Field.NEW_SEGMENT,
                Field.WAYPT_FLAG,
                Field.WAYPT_LINK};

        for (Field field : fields) {
            addField(field);
        }
    }

    /**
     * Receive the start of a tag
     *
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/org/xml/sax/ContentHandler.html#startElement(java.lang.String,%20java.lang.String,%20java.lang.String,%20org.xml.sax.Attributes)">docs.oracle.com: startElement</a>
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        // Read the parameters for metadata, waypoints and track points
        String tag = qName.toLowerCase();
        // if (DEBUG) Log.d(TAG, "startElement() tag = " + tag);
        _gpxField = null;
        if (tag.equals("metadata")) {
            _insideMetaData = true;
            _metaAuthorSet = false;
            resetCurrentValues();
            _fileTitle.setValue(null);
            _fileAuthor.setValue(null);
            _source.setValue(null);
            _link.setValue(null);
            _description.setValue(null);
            _comment.setValue(null);
        }
        else if (tag.equals("wpt") || tag.equals("trkpt") || tag.equals("rtept"))
		{
            _insideMetaData = false;
			_insidePoint = true;
			_insideWaypoint = tag.equals("wpt");
            _isTrackPoint = tag.equals("trkpt");
			resetCurrentValues();
			addCurrentValue(Field.LATITUDE, getAttribute(attributes, "lat"));
			addCurrentValue(Field.LONGITUDE, getAttribute(attributes, "lon"));
			_elevation.setValue(null);
			_pointName.setValue(null);
			_time.setValue(null);
			_type.setValue(null);
            _link.setValue(null);
            _description.setValue(null);
            _comment.setValue(null);
			_sym.setValue(null);
            // additional values
            _source.setValue(null);
            _duration.setValue(null);
        }
		else if (tag.equals("ele")) {
			_currentTag = _elevation;
		}
		else if (tag.equals("name"))
		{
            if (_insideMetaData) {
                if (!_metaAuthorSet) {
                    _currentTag = _fileTitle;
                } else {
                    _currentTag = _fileAuthor;
                }
            }
			else if (_insidePoint) {
				_currentTag = _pointName;
			}
			else if (_trackNum < 0) {
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
		else if (tag.equals("description") || tag.equals("desc"))
		{
			if (_insidePoint) {
				_currentTag = _description;
			}
			else {
				_currentTag = _fileDescription;
			}
		}
		else if (tag.equals("cmt")) {
			_currentTag = _comment;
		}
		else if (tag.equals("sym")) {
			_currentTag = _sym;
        } else if (tag.equals("oa:extension")) {
            _isOutdooractive = true;
        } else if (tag.equals("src")) {
            _currentTag = _source;
        } else if (tag.equals("link")) {
			_link.setValue(attributes.getValue("href"));
		}

        // @implNote BiselliW: private extension to handle break times
        else if (tag.equals("break")) {
			_currentTag = _duration;
		}		
		else if (tag.equals("trkseg")) {
			_startSegment = true;
		}
		else if (tag.equals("trk"))
		{
			_trackNum++;
			_trackName.setValue(null);
		}
		else if (tag.equals("extensions") && _insidePoint)
		{
			_insideExtensions = true;
            if (_storeExtensions) {
                _currentTag = new GpxTag();
                _extensionTags = new Stack<>();
            }
		}
		else if (_insideExtensions)
		{
            if (_storeExtensions)
                _extensionTags.add(qName);
            if (_currentTag != null) {
                _currentTag.clear();
            }
		}
		else if (tag.equals("gpx"))
		{
			setFileType(FileType.GPX);
			processGpxAttributes(attributes);
        } else if (tag.equals("author")) {
            _metaAuthorSet = true;
        } else {
            // Maybe it's a recognised gpx field like hdop
            _gpxField = FieldGpx.getField(tag);
            _currentTag = new GpxTag();
        }

        /* @implNote BiselliW
         * check initialisation of current tag */
        if (_currentTag != null) {
            _currentTag.clear();
        }

        super.startElement(uri, localName, qName, attributes);
    }


	/** Process the attributes from the main gpx tag including extensions */
	private void processGpxAttributes(Attributes attributes)
	{
        // System.out.println("Start gpx element: " + qName);
        final int numAttributes = attributes.getLength();
        for (int i = 0; i < numAttributes; i++) {
            String attributeName = attributes.getQName(i).toLowerCase();
            String attrValue = attributes.getValue(i);
            // System.out.println("   Attribute '" + attributeName + "' - '" + attributes.getValue(i) + "'");
            if (attributeName.equals("version")) {
                setFileVersion(attributes.getValue(i));
            } else if (attributeName.contentEquals("xmlns")) {
                _extensionInfo.setNamespace(attrValue);
            } else if (attributeName.equals("xmlns:xsi")) {
                _extensionInfo.setXsi(attrValue);
            } else if (attributeName.equals("xsi:schemalocation")) {
                String[] schemas = attrValue.split(" ");
            for (String schema : schemas) {
                _extensionInfo.addXsiAttribute(schema);
            }
            } else if (attributeName.startsWith("xmlns:")) {
                String prefix = attributeName.substring(6);
                _extensionInfo.addNamespace(prefix, attrValue);
            }
        }
    }

    /**
     * Process end tag
     *
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/org/xml/sax/ContentHandler.html#endElement(java.lang.String,%20java.lang.String,%20java.lang.String)">docs.oracle.com: startElement</a>
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        String tag = qName.toLowerCase();
        // if (DEBUG) Log.d(TAG, "endElement() tag = " + tag);
        if (tag.equals("metadata")) {
            // process meta data
            processMetaData();
            _insideMetaData = false;
        }
        else if (tag.equals("wpt") || tag.equals("trkpt") || tag.equals("rtept"))
		{
            // Create alternative link to a POI superseding the web link
            if (tag.equals("wpt")
//                    && (_link.getValue().isEmpty())
            ) {
                String source = _source.getValue();
                if (!source.isEmpty()) {
                    // Create Outdooractive link to a POI if no web link is provided
                    if (source.startsWith(_OutdooractiveSrcPrefix)) {
                        source = source.substring(_OutdooractiveSrcPrefix.length());
                        // link is only allowed to web site - no app!
                        _link.setValue(_OutdooractivePoiLink.concat(source));
                    }
                    // Create Toubiz link to a POI if no web link is provided
                    else if (source.startsWith(_toubizPrefix)) {
                        int index = source.indexOf(_OutdooractiveSrcPostfix);
                        if (index >= 0) {
                            source = source.substring(index+_OutdooractiveSrcPostfix.length());
                            if (source.length() > 10)
                                // link is only allowed to web site - no app!
                                _link.setValue(_toubizLink.concat(source));
                        }
                    }
                }
            }

            // don't load Outdooractive trackpoints which are only for routing
            if (_isTrackPoint) {
                if (_sym.getValue() == null)
                    _pointName.setValue(null);
                _isFirstTrackPoint = false;
            }
           //  if (_insidePoint)
                processPoint();
            _insideWaypoint = false;
            _insidePoint = false;
		}
		else if (tag.equals("extensions")) {
			_insideExtensions = false;
		}
		else if (_insideExtensions && _storeExtensions)
		{
            if (_currentTag != null)
            {
                // if (_storeExtensions)
                {
                    String value = _currentTag.getValue();
                    _extensionTags.pop();
                    if (!value.isEmpty())
                    {
                        FieldXml field = new FieldXml(FileType.GPX, tag, _extensionTags);
                        if (!hasField(field)) {
                            addField(field);
                        }
                        addCurrentValue(field, value);
                    }
                }
                _currentTag.clear();
            }
		}
		else if (_gpxField != null)
		{
            if (_currentTag != null)
            {
                String value = _currentTag.getValue();
                if (!value.isEmpty())
                {
                    if (!hasField(_gpxField)) {
                        addField(_gpxField);
                    }
                    addCurrentValue(_gpxField, value);
                }
                _currentTag.clear();
            }
            _gpxField = null;
		}
		else if (_insidePoint && _currentTag != null && getFileVersion().equals("1.0"))
		{
			String value = _currentTag.getValue();
			String tagNamespace = getNamespace(tag);
			if (tagNamespace != null && !value.isEmpty())
			{
				String id = this.getExtensionInfo().getNamespaceName(tagNamespace);
				if (id != null)
				{
					FieldXml field = new FieldXml(FileType.GPX, tag, id);
					if (!hasField(field)) {
						addField(field);
					}
					addCurrentValue(field, value);
				}
			}
			_currentTag = null;
		}
		else {
			_currentTag = null;
		}

        if (tag.equals("author"))
            _metaAuthorSet = false;

        super.endElement(uri, localName, qName);
    }


	private static String getNamespace(String inTagName)
	{
		int firstColonPos = inTagName.indexOf(':');
		if (firstColonPos > 0) {
			return inTagName.substring(0, firstColonPos);
		}
		return null;
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
     * @implNote BiselliW: replace "\n" with "<br>"
	 * @return concatenation
	 */
	private static String checkCharacters(String inVariable, String inValue)
	{
		if (inVariable == null) {
			return inValue;
		}
		return inVariable + inValue.replace("\n","<br>");
	}


    /**
     * process meta data
     *
     * @author BiselliW
     * @since 22.2.006
     */
    private void processMetaData() {
		// get Name from parsed GPX tag <metadata><name> */
//		metaName        = _metaName.getValue();
        // get Description from parsed GPX tag <metadata><description> */
        metaDescription = _description.getValue();
        // get Author from parsed GPX tag <metadata><author><name>
        metaAuthor = _fileAuthor.getValue();
        // get Time from parsed GPX tag <metadata><time>
        metaTime = _time.getValue();
        // get Link from parsed GPX tag <metadata><link>
        metaLink = _link.getValue();
    }

    /**
     * Process a point, either a waypoint or track point
     *
     * @implNote new GPX tags (WAYPT_CMT), new order of point fields in class Field
     */
    private void processPoint() {
		// Values go into a String array matching the order in getFieldArray()
		addCurrentValue(Field.ALTITUDE, _elevation.getValue());
		if (_insideWaypoint) {
			addCurrentValue(Field.WAYPT_NAME, _pointName.getValue());
		}
		addCurrentValue(Field.TIMESTAMP, _time.getValue());
		if (_startSegment && !_insideWaypoint)
		{
			addCurrentValue(Field.NEW_SEGMENT, "1");
			_startSegment = false;
		}
		addCurrentValue(Field.WAYPT_TYPE, _type.getValue());
        if (!_description.getValue().isEmpty())
		    addCurrentValue(Field.DESCRIPTION, _description.getValue().replace(" / ","<br>"));
		addCurrentValue(Field.COMMENT, _comment.getValue().replace(" / ","<br>"));
		addCurrentValue(Field.SYMBOL, _sym.getValue());
        if (!_insideWaypoint && !_sym.getValue().isEmpty()) {
            addCurrentValue(Field.WAYPT_NAME, _pointName.getValue());
        }

        addCurrentValue(Field.WAYPT_DUR, _duration.getValue()); // break

        // Field.WAYPT_FLAG
        addCurrentValue(Field.WAYPT_FLAG, !_isTrackPoint ? "1" : "0");
        addCurrentValue(Field.WAYPT_LINK, _link.getValue());

		_pointList.add(getCurrentValues());
		_linkList.add(_link.getValue());
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
		for (int i=0; i<numPoints; i++) {
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
		if (!hasLink) {
			result = null;
		}
		return result;
	}

	/**
	 * @return file title
	 */
	public String getFileTitle() {
		return _fileTitle.getValue();
	}

	/**
	 * @return file description
     * @implNote BiselliW remove <br> inserted by checkCharacters()
	 */
	public String getFileDescription() {
		return _fileDescription.getValue().replace("<br>","\r");
	}

	@Override
	public ExtensionInfo getExtensionInfo() {
		return _extensionInfo;
	}
    public void setLink(String link)
    {
        metaLink = link;
    }
    public String getLink()
    {
        return metaLink;
    }

    public String getAuthor() { return metaAuthor; }
}
