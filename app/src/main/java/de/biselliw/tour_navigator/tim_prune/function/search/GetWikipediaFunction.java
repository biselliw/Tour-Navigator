package de.biselliw.tour_navigator.tim_prune.function.search;

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
import de.biselliw.tour_navigator.function.search.GenericSearchFunction;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.GetWikipediaXmlHandler;
import de.biselliw.tour_navigator.ui.ControlElements;

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


    String lang = "";

	/**
	 * Constructor
     * @param inActivity parent activity
	 */
	public GetWikipediaFunction(ControlElements inActivity, TrackListModel inTrackListModel) {
		super(inActivity, inTrackListModel);
	}

    /**
     * Load nearby Wikipedia articles
     * @param inPoint point for which surrounding data shall be searched
     * @param inLang language code for search
     */
    public String getWikipedia(DataPoint inPoint, String inLang)
    {
        // Get coordinates from current point
        getSearchCoordinates(inPoint);
        lang = inLang;
        // background work
        new Thread(this).start();
        return WAYPOINT_TYPE;
    }

	/**
	 * Run method to get the nearby points in a separate thread
	 */
	public void run()
	{
        InputStream inStream = null;
        SAXParser saxParser = null;
        if (_trackListModel == null) return;

        // Parse the returned XML with a special handler
        GetWikipediaXmlHandler xmlHandler = new GetWikipediaXmlHandler();
        _trackListModel.changed = false;
        _errorMessage = "";

        try {
            saxParser = SAXParserFactory.newInstance().newSAXParser();
            if (!SIMULATE_QUERY) {
                /* fetch Wikipedia data from geonames.org
                   =================================================================================
                 */
                String urlString = "https://secure.geonames.org/findNearbyWikipedia?lat=" +
                        _searchLatitude + "&lng=" + _searchLongitude + "&maxRows=" + MAX_RESULTS
                        + "&radius=" + MAX_DISTANCE + "&lang=" + lang
                        + "&username=" + GEONAMES_USERNAME;
                try {
                    URL url = new URL(urlString);
                    inStream = url.openStream();
                }
                catch (IOException e) {
                    Log.e(TAG,"submitSearch():" + e.getClass().getName() + " - " + e.getMessage());
                    _errorMessage = Resources.getString(R.string.server_not_found);
                    _trackListModel.changed = true;
                }
            } else {
                /* fetch Wikipedia data from former file retrieved from overpass-api
                   =================================================================================
                 */
                try {
                    if (assetManager != null)
                        inStream = assetManager.open("geonames_findNearbyWikipedia.txt");
                } catch (IOException e) {
                    Log.e(TAG,"submitSearch():" + e.getClass().getName() + " - " + e.getMessage());
                    _errorMessage = "assets/geonames_findNearbyWikipedia.txt could not be opened";
                    _trackListModel.changed = true;
                }
            }
        } catch (ParserConfigurationException | SAXException e) {
            Log.e(TAG,"submitSearch():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = "error in SAXParser";
            _trackListModel.changed = true;
        }

        try {
            if (saxParser != null && inStream != null && !_trackListModel.changed)
                saxParser.parse(inStream, xmlHandler);
        } catch (SAXException | IOException e) {
            Log.e(TAG,"submitSearch():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = Resources.getString(R.string.server_not_found);
            _trackListModel.changed = true;
        }

        if (inStream != null && !_trackListModel.changed) {
            // Close stream and ignore errors
            try {
                inStream.close();
            } catch (Exception ignored) {}

            // Add track list to model
            ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

            // Show error message from parsing if any
            String error = xmlHandler.getErrorMessage();
            if (error != null && !error.isEmpty()) {
                _errorMessage = error;
            }
            else {
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
            }
            // Add new articles to model
            _trackListModel.addTracks(reducedTrackList, true);
        }
    }
}
