package de.biselliw.tour_navigator.function.search;


import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.data.Resources;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.stubs.Config;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchOsmPoisXmlHandler;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import de.biselliw.tour_navigator.ui.ControlElements;
import tim.prune.data.Distance;
import tim.prune.data.DoubleRange;
import tim.prune.data.Unit;

import static de.biselliw.tour_navigator.tim_prune.function.search.SearchOsmFunction.translateTag;

/**
 * Function to load nearby point information from OSM using the <a href="overpass-api.de">Overpass-API</a>
 */
public class SearchOsmGuidePostsFunction extends GenericSearchFunction
{
    /**
     * TAG for log messages.
     */
    static final String TAG = "SearchOsmGuidePostsFunction";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /** Enable simulation of overpass-api query by loading a test file instead */
    private static final boolean SIMULATE_QUERY = false;

    public static final String WAYPOINT_TYPE = "OSM";

    /** Maximum distance between track and point in km */
    private static final double MAX_DISTANCE = 0.05;

    /**
	 * Constructor
     * @param inActivity parent activity
	 */
	public SearchOsmGuidePostsFunction(ControlElements inActivity, TrackListModel inTrackListModel) {
		super(inActivity, inTrackListModel);
	}

    public String getOsmGuidePosts()
    {
        // background work
        new Thread(this).start();
        return WAYPOINT_TYPE;
    }

	/**
	 * Run method to get the nearby points in a separate thread
	 */
	public void run()
	{
        // Submit search (language not an issue here)
        submitSearch();

        // Set status label according to error or "none found", leave blank if ok
        if (_errorMessage == null && _trackListModel.isEmpty()) {
            _errorMessage = Resources.getString(R.string.osm_pois_none_found);
        }
	}

    public static String formatDoubleUS (double inValue) {
        return String.format(Locale.US, "%.6f", inValue);
    }

	/**
	 * Submit the search for the given parameters
	 */
	private void submitSearch() {
        InputStream inStream = null;
        SAXParser saxParser = null;

        // Parse the returned XML with a special handler
        SearchOsmPoisXmlHandler xmlHandler = new SearchOsmPoisXmlHandler();
        _errorMessage = "";

        try {
            saxParser = SAXParserFactory.newInstance().newSAXParser();
            if (!SIMULATE_QUERY) {
                // determine bounding box for all track points
                DoubleRange latRange = track.getLatRange();
                DoubleRange lonRange = track.getLonRange();

                String boundingBox = formatDoubleUS(latRange.getMinimum()) + "," + formatDoubleUS(lonRange.getMinimum())
                        + "," + formatDoubleUS(latRange.getMaximum()) + "," + formatDoubleUS(lonRange.getMaximum());
                String urlString = "https://overpass-api.de/api/interpreter?data="
                        + "node"
                        + "[\"information\"=\"guidepost\"][\"hiking\"=\"yes\"]"
                        + "(" + boundingBox + ");"
                        + "out qt;";
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
                        inStream = assetManager.open("overpass_api_results_guideposts.txt");
                } catch (IOException e) {
                    Log.e(TAG,"submitSearch():" + e.getClass().getName() + " - " + e.getMessage());
                    _errorMessage = "assets/overpass_api_results_guideposts.txt could not be opened";
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

        ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

        if (xmlHandler.getPointList() != null)
            for (SearchResult searchResult : xmlHandler.getPointList())
            {
                // Update single search result
                searchResult.update();

                // find nearest track point
                if (findNearestTrackpoint(searchResult, MAX_DISTANCE)) {
                    searchResult.setPointType(translateTag(searchResult.getPointType()));
                    reducedTrackList.add(searchResult);
                }
            }

        // Add track list to model
        if (reducedTrackList.isEmpty())
            _errorMessage = Resources.getString(R.string.osm_pois_none_found);

		_trackListModel.addTracks(reducedTrackList, true);
	}

}
