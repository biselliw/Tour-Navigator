package de.biselliw.tour_navigator.function;
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

    Copyright 2026 Walter Biselli (BiselliW)
*/

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Arrays;

import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;

/**
 * XML handler for dealing with XML returned from the OSM Overpass api,
 * specially for the OSM Poi service
 */
public class OpenStreetMapXmlHandler extends DefaultHandler
{
    /** use alternative geonames.org to overpass api */
    protected boolean geonames = false;
    protected ArrayList<SearchResult> _pointList = null;
    protected SearchResult _currPoint = null;

    /** set to true if parser passed the filtered items */
    protected boolean _useCurrPoint = true;

    private boolean _matchAll = false;

    private String _value = null;
    private String _typeClass = "";

    private ArrayList<NodeRef> _nodeRefList = null;
    private boolean _hasTags, _firstNodeRef;

    class NodeRef {
        /**
         * ID of the node / way
         */
        private final String ID;
        /** Coordinates of point */
        private final String latitude, longitude;

        NodeRef (SearchResult inSearchResult) {
            ID = inSearchResult.getID();
            latitude = inSearchResult.getLatitude();
            longitude = inSearchResult.getLongitude();
        }
    }

    /* lists of values */
    String[] values_amenity = new String[]{"bbq", "bench", "biergarten", "cafe", "drinking_water",
            "fountain", "ice_cream", "lounger", "place_of_worship", "restaurant", "shelter", "toilets", "water_point"};
    String[] values_information = new String[]{"board", "guidepost", "map"};
    String[] values_tourism = new String[]{"museum","viewpoint",
            "artwork" // todo translate
    };
    String[] values_natural = new String[]{"rock", "spring"};

    /* list of keys */
    String[] keys = new String[]{"amenity", "information", "tourism", "natural"};
    /* list of value lists */
    String[][] values = new String[][]{values_amenity,values_information, values_tourism, values_natural};

    /**
     * Check if key / value match
     * @param inKey key from startElement attributes
     * @param inValue value from startElement attributes
     * @return true if OSM item shall be used
     */
    private boolean match (String inKey, String inValue) {

        return match(inKey, keys, inValue, values);
    }

    /**
     * Check if key / value match
     * @param inValues list of values
     * @param inValue value from startElement attributes
     * @return true if the list contains the value
     */
    private boolean match (String[] inValues, String inValue) {
    for (int i = 0; i < Arrays.stream(inValues).count(); i++) {
        if (inValues[i].equals(inValue))
            return true;
        }
        return false;
    }

    /**
     * Check if key / value match
     * @param inKey key from startElement attributes
     * @param inKeys list of keys
     * @param inValue value from startElement attributes
     * @param inValues list of values
     * @return true if the lists contain the key / value pair
     */
    private boolean match (String inKey, String[] inKeys, String inValue, String[][] inValues) {
        if (inKeys.length != inValues.length) return false;
        for (int key = 0; key < inKeys.length; key++)
            if (inKey.equals(inKeys[key]))
                if (match(inValues[key], inValue))
                    return true;
        return false;
    }

    /**
     * Check if key matches
     * @param inKey key from startElement attributes
     * @return true if the lists contain the key / value pair
     */
    private boolean match (String inKey) {
        String[] keys = new String[]{"highway", // todo reduce, remove street_lamp
                "hiking",

            "historic", // todo translate "historic", "monument", "memorial", "wayside_shrine", "wayside_cross"
            "man_made", // todo translate "street_cabinet", "surveillance" "webcam"
                "railway"};


        for (String s : keys)
            if (inKey.equals(s))
                return true;
        return false;
    }

    /**
     * Option to ignore ell filters
     */
    public void matchAll () {
        _matchAll = true;
    }

    /**
     * React to the start of an XML tag
     */
    public void startElement(String inUri, String inLocalName, String inTagName,
                             Attributes inAttributes) throws SAXException
    {
        if (inTagName.equals("geonames")) {
            geonames = true;
            _pointList = new ArrayList<>();
        }
        else if (inTagName.equals("osm")) {
            _pointList = new ArrayList<>();
            _nodeRefList = new ArrayList<>();
        }

        if (geonames) {
            if (inTagName.equals("poi")) {
                _currPoint = new SearchResult();
                _typeClass = "";
            }
            else {
                _value = null;
            }
        }
        else {
            if (inTagName.equals("node") || inTagName.equals("way")) {
                _currPoint = new SearchResult();
                String link = "https://www.openstreetmap.org/" + inTagName + "/" + inAttributes.getValue("id");
                _currPoint.setWebUrl(link);
                _useCurrPoint = false;
                _hasTags = false;
                if (inTagName.equals("node")) {
                    _currPoint.setID(inAttributes.getValue("id"));
                    _currPoint.setLatitude(inAttributes.getValue("lat"));
                    _currPoint.setLongitude(inAttributes.getValue("lon"));
                }
                else {
                    _firstNodeRef = true;
                }
            }
            else if (inTagName.equals("tag") && _currPoint != null) {
                processTag(inAttributes);
            }
            else if (inTagName.equals("nd") && _firstNodeRef) {
                _firstNodeRef = false;
                processNodeRef(inAttributes);
            }
        }
        super.startElement(inUri, inLocalName, inTagName, inAttributes);
    }

    /**
     * @param inAttributes attributes to process
     */
    private void processNodeRef(Attributes inAttributes) {
        String ref = inAttributes.getValue("ref");
        for (NodeRef nodeRef : _nodeRefList) {
            if (nodeRef.ID.equals(ref)) {
                _currPoint.setLatitude(nodeRef.latitude);
                _currPoint.setLongitude(nodeRef.longitude);
                break;
            }
        }
    }

	/**
	 * @param inAttributes attributes to process
	 */
	protected void processTag(Attributes inAttributes) {
        String key = inAttributes.getValue("k");
        if (key != null) {
            String value = inAttributes.getValue("v");
            _hasTags = true;

            if (_matchAll)
                _useCurrPoint = true;
            if (match(key)) {
                _useCurrPoint = true;
                _currPoint.setPointType(value);
            }
            else if (match(key, value)) {
                _useCurrPoint = true;
                _currPoint.setPointType(value);
            }
            else if (value.equals("webcam")) {
                _useCurrPoint = true;
                _currPoint.setPointType(value);
            }
                /*
                if (value.equals("guidepost")) {
// todo                    _currPoint.isGuidePost = true;
                    _currPoint.setPointType(value);
                }
*/

            if (key.equals("wikimedia_commons")) {
                value = "https://commons.wikimedia.org/wiki/" + value.replace(" ", "_");
            } else if (key.startsWith("wikipedia")) {
                value = "https://de.wikipedia.org/wiki/" + value.replace(" ", "_");
            } else if (key.equals("website")) {
                _currPoint.setDownloadLink(value);
            } else if (key.equals("ref")) {
                _currPoint.setRef(value);
            }
            else if (key.equals("name")) {
                _currPoint.setTrackName(value);
            }

            _currPoint.setDescription(_currPoint.getDescription() + "<p>" + key + ": " + value + "</p>");
        }
    }

    /**
     * React to characters received inside tags
     */
    public void characters(char[] inCh, int inStart, int inLength)
            throws SAXException
    {
        String value = new String(inCh, inStart, inLength);
        _value = (_value==null?value:_value+value);
        super.characters(inCh, inStart, inLength);
    }


    /**
     * React to the end of an XML tag
     */
    public void endElement(String inUri, String inLocalName, String inTagName)
            throws SAXException
    {
        if (geonames) {
            if (inTagName.equals("poi")) {
                // end of the entry
                _pointList.add(_currPoint);
            }
            else if (inTagName.equals("name")) {
                _currPoint.setTrackName(_value);
            }
            else if (inTagName.equals("typeClass")) {
                _typeClass = _value;
            }
            else if (inTagName.equals("typeName")) {
/*
                if (!_typeClass.isEmpty())
                    _typeClass = _typeClass + "/";
                _currPoint.setPointType(_typeClass + _value);
 */
                _currPoint.setPointType(_value);
            }
            else if (inTagName.equals("lat")) {
                _currPoint.setLatitude(_value);
            }
            else if (inTagName.equals("lng")) {
                _currPoint.setLongitude(_value);
            }
            else if (inTagName.equals("distance")) {
                try {
                    _currPoint.setDistance(Double.parseDouble(_value));
                }
                catch (NumberFormatException ignored) {}
            }
        }
        else {
            if (inTagName.equals("node") || inTagName.equals("way"))
            {
                if (_useCurrPoint) {
                    // end of the entry
                    _pointList.add(_currPoint);
                }
                else {
                    if (!_hasTags && _currPoint.hasCoordinates()) {
                        _nodeRefList.add(new NodeRef(_currPoint));
                    }
                }
            }
        }

        super.endElement(inUri, inLocalName, inTagName);
    }

    /**
     * @return the list of points
     */
    public ArrayList<SearchResult> getPointList()
    {
        return _pointList;
    }
}
