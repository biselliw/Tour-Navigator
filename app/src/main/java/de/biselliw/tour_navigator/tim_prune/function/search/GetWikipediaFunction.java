package de.biselliw.tour_navigator.tim_prune.function.search;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.GetWikipediaXmlHandler;

/**
 * Function to load nearby point information from Wikipedia
 * according to the currently viewed area
 */
public class GetWikipediaFunction extends GenericDownloaderFunction
{
	/** Maximum number of results to get */
	private static final int MAX_RESULTS = 20;
	/** Maximum distance from point in km */
	private static final int MAX_DISTANCE = 10;
	private static final String GEONAMES_USERNAME = "tournavigator";

    public static final String WAYPOINT_TYPE = "Wikipedia";

    String lang = "";

	/**
	 * Constructor
     * @param inApp App object
	 */
	public GetWikipediaFunction(App inApp, TrackListModel inTrackListModel) {
		super(inApp, inTrackListModel);
	}

	public void getWikipedia(DataPoint inPoint, String inLang)
    {
        // Get coordinates from current point (if any)
        getSearchCoordinates(inPoint);
        lang = inLang;
        new Thread(() -> {
            // background work
            run();
        }).start();
    }

	/**
	 * Run method to get the nearby points in a separate thread
	 */
	public void run()
	{
		// For geonames, firstly try the local language
        submitSearch(_searchLatitude, _searchLongitude, lang);

        // prohibit recursive search
//        dataPoint = null;

		// Set status label according to error or "none found", leave blank if ok
		if (_errorMessage.isEmpty() && _trackListModel.isEmpty()) {
			_errorMessage = resources.getString(R.string.wikipedia_articles_nonefound);
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
        // Example http://api.geonames.org/findNearbyWikipedia?lat=47&lng=9
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
            _errorMessage = e.getClass().getName() + " - " + e.getMessage();
        }
        // Close stream and ignore errors
        try {
            assert inStream != null;
            inStream.close();
        } catch (Exception ignored) {}

        // Add track list to model
//        ArrayList<SearchResult> trackList = xmlHandler.getTrackList();
        ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

        if (_trackListModel != null) {
            for (SearchResult searchResult : xmlHandler.getTrackList()) {
                // Check if a point is already loaded
                searchResult.setPointType(WAYPOINT_TYPE);
                searchResult.update();
//                if (!searchResult.isDuplicate())
                reducedTrackList.add(searchResult);
            }
            _trackListModel.addTracks(reducedTrackList, true);

            // Show error message if any
            if (_trackListModel.isEmpty()) {
                String error = xmlHandler.getErrorMessage();
                if (error != null && !error.equals("")) {
                    //	_app.showErrorMessageNoLookup(getNameKey(), error);
                    _errorMessage = error;
                }
            }
        }
    }
}
