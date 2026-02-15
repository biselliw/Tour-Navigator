package de.biselliw.tour_navigator.tim_prune.function.search;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.biselliw.tour_navigator.App;
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
 * according to the currently viewed area
 */
public class GetWikipediaFunction extends GenericSearchFunction
{
    /**
     * TAG for log messages.
     */
    static final String TAG = "GetWikipediaFunction";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /** Enable simulation of overpass-api query by loading a test file instead */
    private static final boolean SIMULATE_QUERY = false;

	/** Maximum number of results to get */
	private static final int MAX_RESULTS = 30;
	/** Maximum distance from point in km */
	private static final int MAX_DISTANCE = 20;
	private static final String GEONAMES_USERNAME = "tournavigator";

    public static final String WAYPOINT_TYPE = "Wikipedia";

    String lang = "";

	/**
	 * Constructor
     * @param inActivity parent activity
	 */
	public GetWikipediaFunction(ControlElements inActivity, TrackListModel inTrackListModel) {
		super(inActivity, inTrackListModel);
	}

	public String getWikipedia(DataPoint inPoint, String inLang)
    {
        // Get coordinates from current point (if any)
        getSearchCoordinates(inPoint);
        lang = inLang;
        new Thread(() -> {
            // background work
            run();
        }).start();
        return WAYPOINT_TYPE;
    }

	/**
	 * Run method to get the nearby points in a separate thread
	 */
	public void run()
	{
		// For geonames, firstly try the local language
        submitSearch(_searchLatitude, _searchLongitude, lang);

		// Set status label according to error or "none found", leave blank if ok
		if (_errorMessage.isEmpty() && _trackListModel.isEmpty()) {
			_errorMessage = Resources.getString(R.string.wikipedia_articles_none_found);
		}
	}

	/**
	 * Submit the search for the given parameters
	 * @param inLat latitude
	 * @param inLon longitude
	 * @param inLang language code to use, such as en or de
	 */
	private void submitSearch(double inLat, double inLon, String inLang)
	{
        // Parse the returned XML with a special handler
        GetWikipediaXmlHandler xmlHandler = new GetWikipediaXmlHandler();
        InputStream inStream = null;
        SAXParser saxParser = null;
        _errorMessage = "";

        try {
            saxParser = SAXParserFactory.newInstance().newSAXParser();
            if (!SIMULATE_QUERY) {
                String urlString = "https://secure.geonames.org/findNearbyWikipedia?lat=" +
                        inLat + "&lng=" + inLon + "&maxRows=" + MAX_RESULTS
                        + "&radius=" + MAX_DISTANCE + "&lang=" + inLang
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
            if (saxParser != null && inStream != null)
                saxParser.parse(inStream, xmlHandler);
        } catch (SAXException | IOException e) {
            Log.e(TAG,"submitSearch():" + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = Resources.getString(R.string.server_not_found);
            _trackListModel.changed = true;
        }

        // Close stream and ignore errors
        try {
            inStream.close();
        } catch (Exception ignored) {}

        // Add track list to model
        ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

        if (_trackListModel != null) {
            // Show error message from parsing if any
            String error = xmlHandler.getErrorMessage();
            if (error != null && !error.isEmpty()) {
                //	_app.showErrorMessageNoLookup(getNameKey(), error);
                _errorMessage = error;
            }

            if (xmlHandler.getTrackList() != null) {
                for (SearchResult searchResult : xmlHandler.getTrackList()) {
                    searchResult.update();
                    // Check if a point is already loaded
                    if (!searchResultIsDuplicate(searchResult))
                        reducedTrackList.add(searchResult);
                }
                xmlHandler.getTrackList().clear();
            }
            _trackListModel.addTracks(reducedTrackList, true);
        }
    }
}
