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

import android.os.Build;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.data.Resources;
import de.biselliw.tour_navigator.files.FileUtils;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import de.biselliw.tour_navigator.ui.ControlElements;
import tim.prune.data.DoubleRange;

import static de.biselliw.tour_navigator.data.AppState.isGpxFileGuidePostsCached;
import static de.biselliw.tour_navigator.data.AppState.isGpxFilePOIsCached;
import static de.biselliw.tour_navigator.data.AppState.setGpxFileGuidePostsCached;
import static de.biselliw.tour_navigator.data.AppState.setGpxFilePOIsCached;
import static de.biselliw.tour_navigator.function.search.SearchOsmFunction.translateTag;

/**
 * Function to load nearby point information from OpenStreetMap (OSM) using the <a href="overpass-api.de">Overpass-API</a>.
 * OpenStreetMap data are queried in a bounding box covering the whole track and looking for hiking relevant nodes.
 * The filtered resulting points are sorted by their distance from start of the track.  .
 */
public class SearchOsmBoundingBoxFunction extends GenericSearchFunction {
    /**
     * TAG for log messages.
     */
    static final String TAG = "SearchOsmBoundingBoxFunction";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /**
     * new waypoint type required for GPX file
     */
    public static final String WAYPOINT_TYPE = "OSM";

    /**
     * Enable simulation of overpass-api query by loading a test file instead
     */
    private static final boolean SIMULATE_QUERY = false;


    /**
     * Maximum distance between track and point in km
     */
    private static final double MAX_DISTANCE = 0.050;

    private boolean queryGuideposts = false;
    private boolean queryPOIs = false;


    /**
     * Constructor
     *
     * @param inActivity parent activity
     *
     * @implNote Needs one of these additional calls:
     * @see #getOsmGuideposts: Query guideposts only
     * @see #getOsmPOIs: Query all hiking relevant POIs except of guideposts
     */
    public SearchOsmBoundingBoxFunction(ControlElements inActivity, TrackListModel inTrackListModel) {
        super(inActivity, inTrackListModel);
    }

    /**
     * Query guideposts only
     *
     * @return WAYPOINT_TYPE
     */
    public String getOsmGuideposts() {
        queryGuideposts = true;

        // background work
        new Thread(this).start();
        return WAYPOINT_TYPE;
    }

    /**
     * Query all hiking relevant POIs except of guideposts
     *
     * @return WAYPOINT_TYPE
     */
    public String getOsmPOIs() {
        queryPOIs = true;

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

        return "(" + formatDoubleUS(latRange.getMinimum()) + inExtendDegrees + ","
                + formatDoubleUS(lonRange.getMinimum()) + inExtendDegrees
                + "," + formatDoubleUS(latRange.getMaximum()) + inExtendDegrees
                + "," + formatDoubleUS(lonRange.getMaximum()) + inExtendDegrees + ");";
    }

    /**
     * Create an overpass api query depending on the request
     * @return query URI
     */
    private String getQuery() {
        String boundingBox = getBoundingBox(0.0);
        String urlString = "https://overpass-api.de/api/interpreter?data=(";

        if (queryGuideposts)
            urlString = urlString
                    + "node[information=guidepost][hiking=yes]" + boundingBox;
        if (queryPOIs)
            urlString = urlString
                    + "node[amenity]" + boundingBox
                    + "node[tourism][museum]" + boundingBox
                    + "node[tourism][viewpoint]" + boundingBox;
        if (!queryGuideposts)
            urlString = urlString
                    // excluding [information!=guidepost] does not work if including all [tourism]
                    // + "node[tourism=information][information!=guidepost]" + boundingBox
                    + "node[amenity]" + boundingBox
                    + "node[natural]" + boundingBox
                    + "node[man_made]" + boundingBox
                    // + "node[wikipedia]" + boundingBox
                    + "node[wikimedia_commons]" + boundingBox
                    + "node[wikidata]" + boundingBox
                    + "node[website]" + boundingBox

                    + "node[historic]" + boundingBox
                    + "node[historic=memorial]" + boundingBox
                    + "node[historic=wayside_shrine]" + boundingBox
                    + "node[historic=wayside_cross]" + boundingBox

                    + "node[man_made]" + boundingBox
         + ");out qt;";

        return urlString;
    }

    /**
     * @return true if OSM file has been cached before
     */
    private boolean isDataFileCached() {
        if (queryGuideposts)
            return isGpxFileGuidePostsCached();
        if (queryPOIs)
            return isGpxFilePOIsCached();
        return false;
    }

    /**
     * @param inValue set to true if OSM file has been successfully cached
     */
    private void setDataFileCached(boolean inValue) {
        if (queryGuideposts)
            setGpxFileGuidePostsCached(inValue);
        if (queryPOIs)
            setGpxFilePOIsCached(inValue);
    }

    /**
     * @return file name of cached OSM data
     */
    private String getCachedDataFileName() {
        if (queryGuideposts)
            return "osm_guideposts.txt";
        if (queryPOIs)
            return "osm_pois.txt";
        return "";
    }

    /**
     * @return simulation file name of OSM data
     */
    private String getSimulationFileName() {
        if (queryGuideposts)
            return "overpass_api_guideposts.txt";
        if (queryPOIs)
            return "overpass_api_pois.txt";
        return "";
    }

    /**
     * Submit a query to the overpass-api server
     * @return stream with results from overpass API
     */
    private InputStream submitQuery() {
        InputStream inputStream = null;
        // get the query for the overpass-api server
        String urlString = getQuery();
        try {
            URL url = new URL(urlString);
            inputStream = url.openStream();
        } catch (IOException e) {
            Log.e(TAG, "run():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = Resources.getString(R.string.server_not_found);
            _trackListModel.changed = true;
        }

        return inputStream;
    }

    /**
     * Cache the stream for future reloading
     * @param inputStream data stream
     * @apiNote Call requires API level 33
     * @return true if OSM data were cached
    */
    private boolean cacheQuery(InputStream inputStream)
    {
        boolean osmFileCached = false;

        android.content.Context context = _activity.getBaseContext();
        File cacheDir = FileUtils.getDocumentCacheDir(context);
        // Create a new file in the internal directory
        String cachedFileName = getCachedDataFileName();
        try {
            File file = new File(cacheDir, cachedFileName);
            if (file.exists())
                file.delete();
            // Open a FileOutputStream to write to the file
            FileOutputStream outputStream = new FileOutputStream(file, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                inputStream.transferTo(outputStream);
                // remember cached file
                osmFileCached = true;
                setDataFileCached(true);
            }
            else
            {
                // FIXME inputStream.transferTo(outputStream); for SDK < TIRAMISU Call requires API level 33 (current min is 23): java.io.InputStream#transferT
            }
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "run(): failed to write " + cachedFileName + " to cache", e);
        }
        // Close stream and ignore errors
        try {
            inputStream.close();
        } catch (Exception ignored) {
        }
        return osmFileCached;
    }

    /**
     * Fetch overpass api OSM data from cached file
     * @return stream with overpass API data
     */
    private InputStream getCachedQuery()
    {
        String cachedFileName = getCachedDataFileName();
        InputStream inputStream = null;
        try {
            android.content.Context context = _activity.getBaseContext();
            File cacheDir = FileUtils.getDocumentCacheDir(context);

            // Create a new file in the internal directory
            File file = new File(cacheDir, cachedFileName);
            if (file.exists()) {
                // Open a FileInputStream to read from the file
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    inputStream = Files.newInputStream(file.toPath());
                }
                else {
                    inputStream = new FileInputStream(file);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "run(): cached file " + cachedFileName + " could not be opened" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = Resources.getString(R.string.osm_data_cache_error);
            setDataFileCached(false);
            _trackListModel.changed = true;
        }
        return inputStream;
    }

    /**
     * Fetch overpass api OSM data from simulation file
     * @return stream with overpass API data
     */
    private InputStream getSimulatedQuery() {
        InputStream inputStream = null;
        try {
            if (assetManager != null) {
                inputStream = assetManager.open(getSimulationFileName());
                _trackListModel.message = "Simulation data from file assets/" + getSimulationFileName();
            }
        } catch (IOException e) {
            Log.e(TAG, "run():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = "file assets/" + getSimulationFileName() + " could not be opened";
            _trackListModel.changed = true;
        }
        return inputStream;
    }

    /**
     * Run method to get the points in a separate thread
     */
    public void run() {
        if (_trackListModel == null) return;

        _trackListModel.changed = false;
        InputStream inputStream = null;
        _errorMessage = "";
        boolean osmFileCached = isDataFileCached();

        // Parse the returned XML with a special handler
        SAXParser saxParser = null;
        SearchOsmPoisHandler xmlHandler = new SearchOsmPoisHandler();

        try {
            saxParser = SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            Log.e(TAG, "run():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = "error in SAXParser";
            _trackListModel.changed = true;
        }

        /* load the stream
           =========================================================================================
         */
        if (saxParser != null) {
            if (!SIMULATE_QUERY) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        // are cached OSM data not available?
                        !osmFileCached) {
                    // Submit a query to the overpass-api server
                    inputStream = submitQuery();

                    // cache the stream for future reloading
                    if (inputStream != null)
                        cacheQuery(inputStream);
                }

                // are cached OSM data available?
                if (isDataFileCached()) {
                    // fetch overpass api OSM data from cached file
                    inputStream = getCachedQuery();
                    // Notify the user
                    if (osmFileCached)
                        _trackListModel.message = Resources.getString(R.string.osm_data_from_cache);
                }
            } else {
                // fetch overpass api OSM data from file in assets directory
                inputStream = getSimulatedQuery();
            }
        }

        /* parse the stream
           =========================================================================================
         */
        try {
            if (saxParser != null && inputStream != null && !_trackListModel.changed)
                saxParser.parse(inputStream, xmlHandler);
        } catch (SAXException | IOException e) {
            Log.e(TAG, "run(): cached file could not be interpreted - " + e.getClass().getName() + " - " + e.getMessage());
            if (isDataFileCached()) {
                setDataFileCached(false);
                _errorMessage = Resources.getString(R.string.osm_data_cache_error);
            } else
                _errorMessage = Resources.getString(R.string.server_not_found);
            _trackListModel.changed = true;
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
            for (SearchResult searchResult : xmlHandler.getPointList()) {
                // Update single search result
                searchResult.update();

                if (queryGuideposts || !searchResult.isGuidePost)
                    // Check if a point is already loaded
                    if (!searchResultIsDuplicate(searchResult))
                        // find nearest track point
                        if (findNearestTrackpoint(searchResult, MAX_DISTANCE)) {
                            if (queryGuideposts || !searchResult.isGuidePost) {
                                // translate waypoint types
                                searchResult.setPointType(translateTag(searchResult.getPointType()));
                                if (searchResult.getTrackName().isEmpty()) {
                                    searchResult.setTrackName(searchResult.getPointType());
                                    if (searchResult.getDataPoint() != null)
                                        searchResult.getDataPoint().setWaypointName(searchResult.getTrackName());
                                }
                                reducedTrackList.add(searchResult);
                            }
                        }
            }
        }

        // Add track list to model
        if (reducedTrackList.isEmpty() && _errorMessage.isEmpty())
            _errorMessage = Resources.getString(R.string.osm_pois_none_found);

        _trackListModel.addTracks(reducedTrackList, true);
    }
}
