package de.biselliw.tour_navigator.functions;
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
import java.util.HexFormat;

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
    private boolean _useCurrPoint = true;
    private boolean _isRailwayStop = false,  _isStop = false, _queryStation;

    /** set to true if parser detected to ignore items */
    private boolean _ignoreCurrPoint = false;

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
    final static String[] values_amenity = new String[]{"bbq", "bench", "biergarten", "cafe", "drinking_water",
            "fountain", "ice_cream", "lounger", "place_of_worship", "restaurant", "shelter", "toilets", "water_point"};
    final static String[] values_information = new String[]{"board", "guidepost", "map"};
    final static String[] values_man_made = new String[]{"bridge", "cairn", "cross", "obelisk", "reservoir_covered", "tower", "water_well", "watermill", "webcam", "wildlife_crossing", "windmill"};
    final static String[] values_natural = new String[]{"rock", "spring"};
    final static String[] values_shop = new String[]{"general", "supermarket"};
    final static String[] values_tourism = new String[]{"museum","viewpoint",
            "artwork" // todo translate
    };



    /* list of keys */
    final static String[] keys = new String[]{"amenity", "information", "man_made", "natural", "shop", "tourism"};
    /* list of value lists */
    final static String[][] values = new String[][]{values_amenity, values_information, values_man_made, values_natural, values_shop, values_tourism};

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

    final static String[] _keys = new String[]{ "highway",
            "hiking",
            "historic",
            "railway",
            "ref:IFOPT", "ref:ibnr"
            };

    /**
     * Check if key matches
     * @param inKey key from startElement attributes
     * @return true if the lists contain the key / value pair
     */
    private boolean match_key (String inKey) {
        for (String s : _keys)
            if (inKey.equals(s))
                return true;
        return false;
    }

    final static String[] ignoreValues = new String[]{"route_marker", // todo use way mark sign?
            "street_cabinet", "street_lamp"};

    /**
     * Check if not wanted value matches
     * @param inValue value from startElement attributes
     * @return true if the lists contain the value
     */
    private boolean ignoreValue (String inValue) {
        for (String s : ignoreValues)
            if (inValue.equals(s))
                return true;
        return false;
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
                _ignoreCurrPoint = false;
                _isRailwayStop = false;
                _isStop = false;
                _queryStation = false;
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
            else if (inTagName.equals("tag") && _currPoint != null && !_ignoreCurrPoint) {
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

            if (!ignoreValue(value)) {
                if (match_key(key)) {
                    value = checkStation(key, value);
                } else if (match(key, value)) {
                    _useCurrPoint = true;
                    _currPoint.setPointType(value);
                } else if (key.equals("operator") && value.equals("Schwarzwaldverein")) {
                    value = value + ": https://www.schwarzwaldverein.de/schwarzwald/wanderwege";
                } else if (key.contains("wikidata")) {
                    value = "https://www.wikidata.org/wiki/" + value;
                } else if (key.equals("wikimedia_commons")) {
                    value = makeWebLink("https://commons.wikimedia.org/wiki/" + value.replace(":","%3A"));
                } else if (key.startsWith("wikipedia")) {
                    value = makeWebLink("https://de.wikipedia.org/wiki/" + value);
                } else if (key.equals("website")) {
                    _currPoint.setDownloadLink(value);
                } else if (key.equals("ref")) {
                    _currPoint.setRef(value);
                } else if (key.equals("name")) {
                    _currPoint.setTrackName(value);
                } else if (value.equals("webcam")) {
                    _useCurrPoint = true;
                    _currPoint.setPointType(value);
                }

                _currPoint.setDescription(_currPoint.getDescription() + "<p>" + key + ": " + value + "</p>");
            }
            else {
                _ignoreCurrPoint = true;
                _useCurrPoint = false;
            }
        }
    }

    /**
     * check key / value for public transport stations
     * @param inKey key
     * @param inValue value
     */
    private String checkStation (String inKey, String inValue) {
        String value = inValue;
        if (inKey.equals("ref:ibnr")) {
            _useCurrPoint = true;
            _isRailwayStop = true;
            _currPoint.setPointType("station");
            value = value
                + "<ul><li>Bahnhofsinfos: https://www.bahnhof.de/bahnhof-de/id/" + inValue + "</li>"
                + addQueryStation(inValue)
                + "</ul>";
        }
        else if (inKey.equals("ref:IFOPT")) {
            if (!_currPoint.getTrackName().isEmpty()) {
                if (_isRailwayStop) {
                    _currPoint.setPointType("station");
                    _isStop = false;
                }
                else {
                    _isStop = true;
                    _currPoint.setPointType("stop");
                }
                if (value.length() > 15)
                    value = value.substring(0,14);
                value = value + "<ul><li>Fahrplanauskunft: https://www.fahrplanauskunft-mv.de/vmvsl3plus/departureMonitor?formik=origin%3D" + value + "</li>"
                        + addQueryStation(_currPoint.getTrackName())
                        + "</ul>";
            }
        }
        else if (inKey.equals("highway") && value.equals("bus_stop")) {
            _isStop = true;
            _isRailwayStop = false;
            _currPoint.setPointType("stop");
/*
            if (_currPoint.hasCoordinates()) {
                String uri = getUriPublicStops(Double.parseDouble(_currPoint.getLatitude()), Double.parseDouble(_currPoint.getLongitude()));
                value = value + "<ul><li>Fahrplanauskunft: " + uri + "</li></ul>";
            }
 */
        }
        else if (inKey.equals("railway")) {
            if (inValue.equals("station") || inValue.equals("stop")) {
                _isRailwayStop = true;
                _isStop = false;
                _currPoint.setPointType("station");
                if (!_currPoint.getTrackName().isEmpty()) {
                    _useCurrPoint = true;
                    if (!_queryStation) {
                        value = value + "<ul>"
                            + addQueryStation(_currPoint.getTrackName())
                            + "</ul>";
                    }
                }
            }
        }
        else {
            _useCurrPoint = true;
            if (!value.equals("yes") && !value.equals("no"))
                _currPoint.setPointType(value);
        }
        return value;
    }

    /**
     * Create a web link
     */
    private String makeWebLink (String inUri) {
        return inUri.replace(" ", "%20").replace(")", "%29");
    }

    /**
     * add travel query
     * @param inStation IFOPT id or station name
     */
    private String addQueryStation(String inStation) {
        String value = "";
        if (!_queryStation) {
            value = "<li>Verbindungsanfrage: "
                    + makeWebLink("https://www.bahn.de/buchung/start?zo=" + inStation)
                    + "</li>";
            _queryStation = true;
        }
        return value;
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
                    if (!_currPoint.getTrackName().isEmpty())
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
