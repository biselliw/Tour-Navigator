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
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.function.WikipediaXmlHandler;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.ui.ControlElements;
import tim.prune.data.DoubleRange;

/**
 * Function to load nearby point information from Wikipedia
*/
public class GetWikipediaFunction extends GenericSearchFunction
{
    /**
     * TAG for log messages.
     */
    static final String TAG = "GetWikipediaFunction";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /** new waypoint type required for GPX file */
    public static final String WAYPOINT_TYPE = "Wikipedia";

    /** Enable simulation of geonames-api query by loading a test file instead */
    private static final boolean SIMULATE_QUERY = false;

	/** Maximum number of results to get */
	private static final int MAX_RESULTS = 30;
	/** Maximum distance from point in km */
	private static final int MAX_DISTANCE = 20;
    /** user name required for geonames api */
	private static final String GEONAMES_USERNAME = "tournavigator";

    /** find Nearby Wikipedia articles around the current point */
    private boolean findNearbyWikipedia = false;

	/**
	 * Constructor
     * @param inActivity parent activity
	 */
	public GetWikipediaFunction(ControlElements inActivity) {
		super(inActivity);
	}

    /**
     * Find nearby Wikipedia articles
     */
    public String queryAround()
    {
        // Get coordinates from current point
        findNearbyWikipedia = getSearchCoordinates(dataPoint);
        // background work
        new Thread(this).start();
        return WAYPOINT_TYPE;
    }

    /**
     * Find Wikipedia articles within a bounding box covering the track
     */
    public String wikipediaBoundingBox()
    {
        boundingBox = getBoundingBox(extendBoundingBox);

        // background work
        new Thread(this).start();
        return WAYPOINT_TYPE;
    }

    /**
     * Create a bounding box for queries within a rectangular region covered by the track
     * @param inExtendDegrees extend all sides of the rectangular by a fixed offset in degrees
     * @return bounding box
     */
    private String getBoundingBox(double inExtendDegrees) {
        // determine bounding box for all track points
        DoubleRange latRange = track.getLatRange();
        DoubleRange lonRange = track.getLonRange();

        return "north=" + formatDoubleUS(latRange.getMaximum() + inExtendDegrees)
      + "&south=" + formatDoubleUS(latRange.getMinimum() - inExtendDegrees)
      + "&east="  + formatDoubleUS(lonRange.getMaximum() + inExtendDegrees)
      + "&west="  + formatDoubleUS(lonRange.getMinimum() - inExtendDegrees);
    }

    /**
     * Create a query depending on the request
     * @return query URI
     */
    private String getQuery() {
        String urlString = "https://secure.geonames.org/";

        // findNearbyWikipedia?
        if (findNearbyWikipedia)
            urlString = urlString + "findNearbyWikipedia?"
                + "lat=" + _searchLatitude + "&lng=" + _searchLongitude + "&maxRows=" + MAX_RESULTS
                + "&radius=" + MAX_DISTANCE;
        else
            // use wikipediaBoundingBox
            urlString = urlString + "wikipediaBoundingBox?"
                + boundingBox;

        urlString = urlString + "&lang=" + lang  + "&username=" + GEONAMES_USERNAME;

        return urlString;
    }

    /**
     * @return simulation file name of Wikipedia data
     */
    private String getSimulationFileName() {
        if (findNearbyWikipedia)
            return "findNearbyWikipedia.txt";
        else
            return "wikipediaBoundingBox.txt";
    }

    /**
     * Submit a query to the geonames server
     * @return stream with results from geonames
     */
    private InputStream submitQuery() {
        InputStream inputStream = null;
        // get the query for the geonames server
        String urlString = getQuery();
        try {
            URL url = new URL(urlString);
            inputStream = url.openStream();
        }
        catch (IOException e) {
            Log.e(TAG,"run():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = Resources.getString(R.string.server_not_found);
            trackListModel.changed = true;
        }
        return inputStream;
    }

    /**
     * Fetch geonames data from simulation file
     * @implNote only used for debugging
     * @return stream with geonames data
     */
    private InputStream getSimulatedQuery() {
        InputStream inputStream = null;
        if (DEBUG) {
            try {
                if (assetManager != null) {
                    inputStream = assetManager.open(getSimulationFileName());
                    trackListModel.message = "Simulation data from file assets/" + getSimulationFileName();
                }
            } catch (IOException e) {
                Log.e(TAG, "run():" + e.getClass().getName() + " - " + e.getMessage());
                _errorMessage = "file assets/" + getSimulationFileName() + " could not be opened";
                trackListModel.changed = true;
            }
        }
        return inputStream;
    }

    /**
	 * Run method to get the points in a separate thread
	 */
	public void run() {
        if (trackListModel == null) return;

        trackListModel.changed = false;
        InputStream inputStream = null;
        _errorMessage = "";

        // Parse the returned XML with a special handler
        SAXParser saxParser = null;
        WikipediaXmlHandler xmlHandler = new WikipediaXmlHandler();

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
                // Submit a query to the geonames server
                inputStream = submitQuery();
            } else {
                if (DEBUG)
                    // fetch geonames data from file in assets directory
                    inputStream = getSimulatedQuery();
            }
        }

        /* parse the stream
           =========================================================================================
         */
        try {
            if (saxParser != null && inputStream != null && !trackListModel.changed)
                saxParser.parse(inputStream, xmlHandler);
        } catch (SAXException | IOException e) {
            Log.e(TAG,"submitSearch():" + e.getClass().getName() + " - " + e.getMessage());
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
        // Add track list to model
        ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

        // was parsing successful ?
        if (xmlHandler.getTrackList() != null) {
            // for all records ...
            for (SearchResult searchResult : xmlHandler.getTrackList()) {
                // Update single search result
                searchResult.update();
                // Check if a point is already loaded
                if (!searchResultIsDuplicate(searchResult))
                    reducedTrackList.add(searchResult);
            }
            // free memory
            xmlHandler.getTrackList().clear();
        }

        // No new articles found ?
        if (reducedTrackList.isEmpty())
            _errorMessage = Resources.getString(R.string.wikipedia_articles_none_found);

        // Add new articles to model
        trackListModel.addTracks(reducedTrackList, true);
    }
}
