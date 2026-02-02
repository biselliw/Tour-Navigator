package de.biselliw.tour_navigator.tim_prune.function.search;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
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
	/** Maximum number of results to get */
	private static final int MAX_RESULTS = 20;
	/** Maximum distance from point in km */
	private static final int MAX_DISTANCE = 10;
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
			_errorMessage = App.resources.getString(R.string.wikipedia_articles_none_found);
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
        String urlString = "https://secure.geonames.org/findNearbyWikipedia?lat=" +
                inLat + "&lng=" + inLon + "&maxRows=" + MAX_RESULTS
                + "&radius=" + MAX_DISTANCE + "&lang=" + inLang
                + "&username=" + GEONAMES_USERNAME;
        // Parse the returned XML with a special handler
        GetWikipediaXmlHandler xmlHandler = new GetWikipediaXmlHandler();
        InputStream inStream = null;
        _errorMessage = "";

        try
        {
            URL url = new URL(urlString);
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            inStream = url.openStream();
            saxParser.parse(inStream, xmlHandler);
        }
        catch (Exception e) {
            Log.e(TAG,"submitSearch: " + e.getClass().getName() + " - " + e.getMessage());
            _errorMessage = App.resources.getString(R.string.server_not_found);
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

            if (xmlHandler.getTrackList() != null)
                for (SearchResult searchResult : xmlHandler.getTrackList()) {
                    searchResult.update();
                    // Check if a point is already loaded
                    if (!searchResultIsDuplicate(searchResult))
                        reducedTrackList.add(searchResult);
                }
            _trackListModel.addTracks(reducedTrackList, true);
        }
    }
}
