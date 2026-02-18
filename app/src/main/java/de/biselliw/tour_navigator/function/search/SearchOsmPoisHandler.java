package de.biselliw.tour_navigator.function.search;
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

import java.util.Arrays;

import de.biselliw.tour_navigator.tim_prune.function.search.SearchOsmPoisXmlHandler;

/**
 * XML handler for dealing with XML returned from the OSM Overpass api,
 * specially for the OSM Poi service
 */
public class SearchOsmPoisHandler extends SearchOsmPoisXmlHandler
{
    private boolean _matchAll = false;

    /**
     * Check if key / value match
     * @param inKey key from startElement attributes
     * @param inValue value from startElement attributes
     * @return true if OSM item shall be used
     */
    private boolean match (String inKey, String inValue) {
        /* lists of values */
        String[] values_amenity = new String[]{"bbq", "bench", "biergarten", "cafe", "drinking_water",
                "fountain", "ice_cream", "lounger", "place_of_worship", "restaurant", "shelter", "toilets", "water_point"};
        String[] values_information = new String[]{"board", "guidepost", "map"};
        String[] values_tourism = new String[]{"museum","viewpoint",
            "artwork" // todo translate
        };
        String[] values_natural = new String[]{"spring"};

        /* list of keys */
        String[] keys = new String[]{"amenity", "information", "tourism", "natural"};
        /* list of value lists */
        String[][] values = new String[][]{values_amenity,values_information, values_tourism, values_natural};

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
        String[] keys = new String[]{"highway","hiking",

            "historic", // todo translate "historic", "monument", "memorial", "wayside_shrine", "wayside_cross"
            "man_made", // todo translate "street_cabinet"
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
        super.startElement(inUri, inLocalName, inTagName, inAttributes);
        if (inTagName.equals("node")) {
            String link = "https://www.openstreetmap.org/" + inTagName + "/" + inAttributes.getValue("id");
            _currPoint.setWebUrl(link);
            _useCurrPoint = false;
        }
    }

	/**
	 * @param inAttributes attributes to process
	 */
	protected void processTag(Attributes inAttributes) {
        String key = inAttributes.getValue("k");
        if (key != null) {
            String value = inAttributes.getValue("v");

            if (match(key) || match(key, value)) {
                if (_currPoint.getPointType().isEmpty())
                    _currPoint.setPointType(value);
                _useCurrPoint = true;
            }
            if (_matchAll)
                _useCurrPoint = true;

            if (_useCurrPoint) {
                _currPoint.isGuidePost = value.equals("guidepost");
            }

            if (key.equals("wikimedia_commons")) {
                value = "https://commons.wikimedia.org/wiki/" + value.replace(" ", "_");
            } else if (key.startsWith("wikipedia")) {
                value = "https://de.wikipedia.org/wiki/" + value.replace(" ", "_");
            } else if (key.equals("website")) {
                _currPoint.setDownloadLink(value);
            } else if (key.equals("ref")) {
                _currPoint.setRef(value);
            }

            if (!key.equals("ele")) {
                _currPoint.setDescription(_currPoint.getDescription() + "<p>" + key + ": " + value + "</p>");
            }
        }
        super.processTag(inAttributes);
    }
}
