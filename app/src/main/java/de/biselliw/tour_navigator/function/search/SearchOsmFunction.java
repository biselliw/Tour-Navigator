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


import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.data.Resources;
import de.biselliw.tour_navigator.function.OpenStreetMapXmlHandler;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.stubs.Config;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import de.biselliw.tour_navigator.ui.ControlElements;
import tim.prune.data.Distance;
import tim.prune.data.Latitude;
import tim.prune.data.Longitude;
import tim.prune.data.Unit;

import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

/**
 * Function to load nearby point information from OpenStreetMap (OSM) using the <a href="overpass-api.de">Overpass-API</a>
 */
public class SearchOsmFunction extends GenericSearchFunction
{
    /**
     * TAG for log messages.
     */
    static final String TAG = "SearchOsmFunction";
    private static final boolean _DEBUG = false; // Set to true to enable debugging / logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /** new waypoint type required for GPX file */
    public static final String WAYPOINT_TYPE = "OSM";
    /** Enable simulation of overpass-api query by loading a test file instead */
    private static final boolean SIMULATE_QUERY = false;


	/** Maximum distance from point in m */
	private static final int MAX_DISTANCE = 500;

    /** Translation table fpr OSM POIs */
    static String[] symbols =   {"restaurant",       "bbq",        "bench", "lounger", "guidepost",
            "bus_stop", "stop", "station", "turning_circle",
    "map", "cafe", "drinking_water", "toilets", "place_of_worship", "shelter", "office",
    "board", "ice_cream", "viewpoint", "museum"};
    static int[] ids = {R.string.restaurant, R.string.bbq, R.string.bench,R.string.lounger, R.string.guidepost,
            R.string.bus_stop, R.string.stop, R.string.station, R.string.turning_circle,
    R.string.map, R.string.cafe, R.string.drinking_water, R.string.toilets, R.string.place_of_worship,
    R.string.shelter, R.string.office, R.string.board, R.string.ice_cream, R.string.viewpoint, R.string.museum};

    /**
	 * Constructor
     * @param inActivity parent activity
	 */
	public SearchOsmFunction(ControlElements inActivity, TrackListModel inTrackListModel) {
		super(inActivity);
	}

    /**
     * Add a query
     * @param _inAround around
     * @param inKey key
     * @param inValue value
     */
    private String addQuery (String _inAround, String inKey, String inValue) {
        return _inAround + "[\"" + inKey + "\"=\"" + inValue + "\"];";
    }
    private String addQuery (String _inAround, String inKey) {
        return _inAround + "[\"" + inKey + "\"];";
    }

    /**
     * Create an overpass api query
     * @return query URI
     */
    private String getQuery() {
        String around = "node(around:" + MAX_DISTANCE + "," + _searchLatitude + "," + _searchLongitude  + ")";
        String urlString = "https://overpass-api.de/api/interpreter?data=("
                + addQuery(around, "amenity")
                + addQuery(around, "building")
                + addQuery(around, "information")
                /*
                + addQuery(around, "amenity", "place_of_worship")
                + addQuery(around, "amenity", "restaurant")
                + addQuery(around, "amenity", "cafe")
                + addQuery(around, "amenity", "biergarten")
                + addQuery(around, "amenity", "ice_cream")
                + addQuery(around, "amenity", "bbq")
                + addQuery(around, "amenity", "bench")
                + addQuery(around, "amenity", "lounger")
                + addQuery(around, "amenity", "drinking_water")
                + addQuery(around, "amenity", "water_point")
                + addQuery(around, "amenity", "shelter")
                + addQuery(around, "amenity", "toilets")
                + addQuery(around, "building", "toilets")

                + addQuery(around, "information","guidepost\"][\"hiking\"=\"yes\"")
                + addQuery(around, "railway","name")
                + addQuery(around, "highway","name")
                 */
                + addQuery(around, "tourism")
                + ");out%20qt;";
        return urlString;
    }

    /**
     * @return simulation file name of OSM data
     */
    private String getSimulationFileName() {
        return "overpass_api_results_pois.txt";
    }

    /**
     * Submit a query to the overpass-api server
     * @return stream with results from overpass API
     */
    private InputStream queryOverpassApi () {
        InputStream inputStream = null;
        // get the query to the overpass-api server
        String urlString = getQuery();
        try {
            URL url = new URL(urlString);
            inputStream = url.openStream();
        } catch (IOException e) {
            Log.e(TAG, "run():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = Resources.getString(R.string.server_not_found);
            trackListModel.changed = true;
        }

        return inputStream;
    }

    /**
     * Fetch overpass api OSM data from simulation file
     * @return stream with overpass API data
     */
    private InputStream getSimulatedOsmQuery() {
        InputStream inputStream = null;
        try {
            if (assetManager != null) {
                inputStream = assetManager.open(getSimulationFileName());
                trackListModel.message = "OSM Data from file assets/" + getSimulationFileName();
            }
        } catch (IOException e) {
            Log.e(TAG, "run():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = "file assets/" + getSimulationFileName() + " could not be opened";
            trackListModel.changed = true;
        }
        return inputStream;
    }

    /**
	 * Run method to get the nearby points in a separate thread
	 */
	public void run()
	{
        if (trackListModel == null) return;

        trackListModel.changed = false;
        InputStream inputStream = null;
        _errorMessage = "";

        // Parse the returned XML with a special handler
        SAXParser saxParser = null;
        OpenStreetMapXmlHandler xmlHandler = new OpenStreetMapXmlHandler();
        xmlHandler.matchAll();

        try {
            saxParser = SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            Log.e(TAG, "run():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = "error in SAXParser";
            trackListModel.changed = true;
        }

        /* load the stream
           =========================================================================================
         */
        if (saxParser != null) {
            if (!SIMULATE_QUERY) {
                // Submit a query to the overpass-api server
                inputStream = queryOverpassApi ();
            } else {
                // fetch overpass api OSM data from file in assets directory
                inputStream = getSimulatedOsmQuery();
            }
        }

        /* parse the stream
           =========================================================================================
         */
        try {
            if (saxParser != null && inputStream != null && !trackListModel.changed)
                saxParser.parse(inputStream, xmlHandler);
        } catch (SAXException | IOException e) {
            Log.e(TAG, "run(): " + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = Resources.getString(R.string.server_not_found);
            trackListModel.changed = true;
        }

        // Close stream and ignore errors
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignored) {
            }
        }

        /* Add track list to model
           =========================================================================================
         */
        ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

        // was parsing successful ?
        if (xmlHandler.getPointList() != null) {
            // Calculate distances for each returned point
            DataPoint searchPoint = new DataPoint(Latitude.make(this._searchLatitude),
                    Longitude.make(this._searchLongitude));
            Unit distUnit = Config.getUnitSet().getDistanceUnit();
            for (SearchResult searchResult : xmlHandler.getPointList()) {
                // Update single search result
                searchResult.update();
                double dist = DataPoint.calculateRadiansBetween(searchPoint, searchResult.getDataPoint());
                searchResult.setDistance(Distance.convertRadiansToDistance(dist, distUnit));

                // translate waypoint types
                searchResult.setPointType(translateTag(searchResult.getPointType()));
                if (searchResult.getTrackName().isEmpty()) {
                    searchResult.setTrackName(searchResult.getPointType());
                    if (searchResult.getDataPoint() != null)
                        searchResult.getDataPoint().setWaypointName(searchResult.getTrackName());
                }
                if (!searchResultIsDuplicate(searchResult))
                    reducedTrackList.add(searchResult);
            }
        }
        // Add track list to model
        if (reducedTrackList.isEmpty())
            _errorMessage = Resources.getString(R.string.osm_pois_none_found);

        trackListModel.addTracks(reducedTrackList, true);
	}

    /**
     * Interpret the Waypoint symbol provided by OSM
     * @param symbol specific waypoint symbol
     * @return translated string
     */
    public static String interpretWaypointSymbol(String symbol) {
        try {
            symbol = symbol.substring(WAYPOINT_TYPE.length() + 2);
            return translateTag(symbol);
        }
        catch (Exception e) {
            return symbol;
        }
    }

    /**
     * Translate waypoint types
     * @param symbol waypoint symbol
     * @return Translated waypoint symbol
     */
    public static String translateTag(String symbol) {
        for (int i = 0; i < symbols.length; i++)
            if (symbols[i].equals(symbol)) {
                return Resources.getString(ids[i]);
            }
        return symbol;
    }
}
