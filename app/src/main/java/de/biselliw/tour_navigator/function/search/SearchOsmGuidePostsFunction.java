package de.biselliw.tour_navigator.function.search;

import org.xml.sax.SAXException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.stubs.Config;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.GenericDownloaderFunction;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchOsmPoisXmlHandler;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import tim.prune.data.Distance;
import tim.prune.data.DoubleRange;
import tim.prune.data.Latitude;
import tim.prune.data.Longitude;
import tim.prune.data.Unit;

import static de.biselliw.tour_navigator.tim_prune.function.search.SearchOsmFunction.translateTag;

/**
 * Function to load nearby point information from OSM using the <a href="overpass-api.de">Overpass-API</a>
 */
public class SearchOsmGuidePostsFunction extends GenericDownloaderFunction
{
    /**
     * TAG for log messages.
     */
    static final String TAG = "SearchOsmGuidePostsFunction";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public static final String WAYPOINT_TYPE = "OSM";

    /** Maximum distance between track and point in m */
    private static final int MAX_DISTANCE = 50;

    /**
	 * Constructor
     * @param inApp App object
	 */
	public SearchOsmGuidePostsFunction(App inApp, TrackListModel inTrackListModel) {
		super(inApp, inTrackListModel);
	}

    public String getOsmGuidePosts()
    {
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
        // Submit search (language not an issue here)
        submitSearch();

        // Set status label according to error or "none found", leave blank if ok
        if (_errorMessage == null && _trackListModel.isEmpty()) {
            _errorMessage = resources.getString(R.string.osm_pois_nonefound);
        }
	}

    public static String formatDoubleUS (double inValue) {
        return String.format(Locale.US, "%.6f", inValue);
    }

	/**
	 * Submit the search for the given parameters
	 */
	private void submitSearch() {
        // determine bounding box for all track points
        DoubleRange latRange = track.getLatRange();
        DoubleRange lonRange = track.getLonRange();

        String boundingBox =  formatDoubleUS(latRange.getMinimum()) + "," + formatDoubleUS(lonRange.getMinimum())
                + "," + formatDoubleUS(latRange.getMaximum()) + "," + formatDoubleUS(lonRange.getMaximum());
        String urlString = "https://overpass-api.de/api/interpreter?data="
            + "node"
            + "[\"information\"=\"guidepost\"][\"hiking\"=\"yes\"]"
            + "(" + boundingBox + ");"
            + "out qt;";
        // Parse the returned XML with a special handler
        SearchOsmPoisXmlHandler xmlHandler = new SearchOsmPoisXmlHandler();
        _errorMessage = "";

        if (!_DEBUG)
            try {
                URL url = new URL(urlString);
                SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
                try (InputStream inStream = url.openStream()) {
                    saxParser.parse(inStream, xmlHandler);
                } catch (Exception e) {
                    _errorMessage = resources.getString(R.string.osm_timeout); // e.getClass().getName() + " - " + e.getMessage();
                    _trackListModel.changed = true;
                    return;
                }
            } catch (MalformedURLException | SAXException | ParserConfigurationException ignored) {
            }

        ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

        if (_DEBUG) {
            SearchResult searchResult = new SearchResult();
            searchResult.setTrackName("Demo");
            searchResult.setLatitude("48.2488458");
            searchResult.setLongitude("8.2112455");
            searchResult.setPointType("guidepost");
            searchResult.update();
            reducedTrackList.add(searchResult);
        }
        else {
            for (SearchResult searchResult : xmlHandler.getPointList())
            {
                // Update single search result
                searchResult.update();

                // find nearest track point
                DataPoint searchPoint = searchResult.getDataPoint();
                Unit distUnit = Config.getUnitSet().getDistanceUnit();
                if (searchPoint != null && !searchResult.isDuplicate()) {
                    for (int i = 0; i < track.getNumPoints(); i++) {
                        DataPoint trackPoint = track.getPoint(i);
                        if (trackPoint != null && !trackPoint.isWaypoint()) {
                            double dist = DataPoint.calculateRadiansBetween(searchPoint, trackPoint);
                            double distance = Distance.convertRadiansToDistance(dist, distUnit);
                            if (distance < MAX_DISTANCE) {
                                searchResult.setPointType(translateTag(searchResult.getPointType()));
                                reducedTrackList.add(searchResult);
                                break;
                            }
                        }
                    }
                }
            }

            // Add track list to model
            if (reducedTrackList.isEmpty())
                _errorMessage = resources.getString(R.string.osm_pois_nonefound);
        }

		_trackListModel.addTracks(reducedTrackList, true);
		_trackListModel.setShowPointTypes(true);
	}

}
