package de.biselliw.tour_navigator.tim_prune.function.search;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.function.search.GenericSearchFunction;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.stubs.Config;
import de.biselliw.tour_navigator.ui.ControlElements;
import tim.prune.data.Distance;
import tim.prune.data.Latitude;
import tim.prune.data.Longitude;
import tim.prune.data.Unit;

import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

/**
 * Function to load nearby point information from OSM using the <a href="overpass-api.de">Overpass-API</a>
 */
public class SearchOsmFunction extends GenericSearchFunction
{
    /**
     * TAG for log messages.
     */
    static final String TAG = "SearchOsmFunction";
    private static final boolean _DEBUG = false; // Set to true to enable debugging / logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public static final String WAYPOINT_TYPE = "OSM";

	/** Maximum distance from point in m */
	private static final int MAX_DISTANCE = 500;

    static String[] symbols =   {"restaurant",       "bbq",        "bench",        "guidepost",        "bus_stop"};
    static int[] ids = {R.string.restaurant, R.string.bbq, R.string.bench, R.string.guidepost, R.string.bus_stop};

    /**
	 * Constructor
     * @param inActivity parent activity
	 */
	public SearchOsmFunction(ControlElements inActivity, TrackListModel inTrackListModel) {
		super(inActivity, inTrackListModel);
	}

    public String getOSM(DataPoint inPoint, String inLang)
    {
        // Get coordinates from current point (if any)
        getSearchCoordinates(inPoint);
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
        submitSearch(_searchLatitude, _searchLongitude);

        // Set status label according to error or "none found", leave blank if ok
        if (_errorMessage == null && _trackListModel.isEmpty()) {
            _errorMessage = App.resources.getString(R.string.osm_pois_none_found);
        }
	}

	/**
	 * Submit the search for the given parameters
	 * @param inLat latitude
	 * @param inLon longitude
	 */
	private void submitSearch(double inLat, double inLon) {
        String coords = "around:" + MAX_DISTANCE + "," + inLat + "," + inLon;
        String urlString = "https://overpass-api.de/api/interpreter?data=("
                + "node(" + coords + ")[\"amenity\"=\"restaurant\"];"
                + "node(" + coords + ")[\"amenity\"=\"bbq\"];"
                + "node(" + coords + ")[\"amenity\"=\"bench\"];"
                + "node(" + coords + ")[\"amenity\"=\"drinking_water\"];"
                + "node(" + coords + ")[\"amenity\"=\"water_point\"];"
                + "node(" + coords + ")[\"amenity\"=\"shelter\"];"
                + "node(" + coords + ")[\"amenity\"=\"toilets\"];"
                + "node(" + coords + ")[\"information\"=\"guidepost\"][\"hiking\"=\"yes\"];"
                + "node(" + coords + ")[\"railway\"][\"name\"];"
                + "node(" + coords + ")[\"highway\"][\"name\"];"
                + "node(" + coords + ")[\"tourism\"];"
                + ");out%20qt;";
        // Parse the returned XML with a special handler
        SearchOsmPoisXmlHandler xmlHandler = new SearchOsmPoisXmlHandler();
        _errorMessage = "";

        if (!_DEBUG)
            try {
//                String coords = "nwr(48.210416666666674,8.208166666666667,48.25688888888889,8.251416666666666)";
//                String urlString = "https://overpass-api.de/api/interpreter?data=("

                URL url = new URL(urlString);
                SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
                try (InputStream inStream = url.openStream()) {
                    saxParser.parse(inStream, xmlHandler);
                } catch (Exception e) {
                    Log.e(TAG,"submitSearch(); " + e.getClass().getName() + " - " + e.getMessage());
                    _errorMessage = App.resources.getString(R.string.server_not_found); // e.getClass().getName() + " - " + e.getMessage();
                    _trackListModel.changed = true;
                    return;
                }
            } catch (MalformedURLException | SAXException | ParserConfigurationException ignored) {
            }

		// Calculate distances for each returned point
		DataPoint searchPoint = new DataPoint(Latitude.make(_searchLatitude),
			Longitude.make(_searchLongitude));
		Unit distUnit = Config.getUnitSet().getDistanceUnit();
        ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

        if (_DEBUG) {
            SearchResult searchResult = new SearchResult();
            searchResult.setTrackName("Demo");
            searchResult.setLatitude("47.734074");
            searchResult.setLongitude("8.2112455");
            searchResult.setPointType("guidepost");
            searchResult.update();
            reducedTrackList.add(searchResult);
        }
        else {
            if (xmlHandler.getPointList() != null)
                for (SearchResult searchResult : xmlHandler.getPointList())
                {
                    // Update single search result
                    searchResult.update();
                    double dist = DataPoint.calculateRadiansBetween(searchPoint, searchResult.getDataPoint());
                    searchResult.setLength(Distance.convertRadiansToDistance(dist, distUnit));

                    searchResult.setPointType(translateTag(searchResult.getPointType() ));
                    if (!searchResultIsDuplicate(searchResult))
                        reducedTrackList.add(searchResult);
                }

            // Add track list to model
            if (reducedTrackList.isEmpty())
                _errorMessage = App.resources.getString(R.string.osm_pois_none_found);
        }

		_trackListModel.addTracks(reducedTrackList, true);
		_trackListModel.setShowPointTypes(true);
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

    public static String translateTag(String symbol) {
        for (int i = 0; i < symbols.length; i++)
            if (symbols[i].equals(symbol)) {
                return App.resources.getString(ids[i]);
            }
        return symbol;
    }
/*            + "node(" + coords + ")[\"amenity\"=\"drinking_water\"];"
                    + "node(" + coords + ")[\"amenity\"=\"water_point\"];"
                    + "node(" + coords + ")[\"amenity\"=\"shelter\"];"
                    + "node(" + coords + ")[\"amenity\"=\"toilets\"];"

 */

}
