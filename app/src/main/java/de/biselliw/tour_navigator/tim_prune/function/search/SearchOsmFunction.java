package de.biselliw.tour_navigator.tim_prune.function.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.data.Resources;
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

    /** Enable simulation of overpass-api query by loading a test file instead */
    private static final boolean SIMULATE_QUERY = false;

    public static final String WAYPOINT_TYPE = "OSM";

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
		super(inActivity, inTrackListModel);
	}

    public String getOSM(DataPoint inPoint)
    {
        // Get coordinates from current point (if any)
        getSearchCoordinates(inPoint);
        // background work
        new Thread(this::run).start();
        return WAYPOINT_TYPE;
    }

	/**
	 * Run method to get the nearby points in a separate thread
	 */
	public void run()
	{
        // Submit search (language not an issue here)
        InputStream inStream = null;
        SAXParser saxParser = null;

        // Parse the returned XML with a special handler
        SearchOsmPoisXmlHandler xmlHandler = new SearchOsmPoisXmlHandler();
        _trackListModel.changed = false;
        _errorMessage = "";

        try {
            saxParser = SAXParserFactory.newInstance().newSAXParser();
            if (!SIMULATE_QUERY) {
                /* fetch OSM data from overpass-api
                   =================================================================================
                 */
                String coords = "around:" + MAX_DISTANCE + "," + _searchLatitude + "," + _searchLongitude;
                String urlString = "https://overpass-api.de/api/interpreter?data=("
                        + "node(" + coords + ")[\"amenity\"=\"place_of_worship\"];"
                        + "node(" + coords + ")[\"amenity\"=\"restaurant\"];"
                        + "node(" + coords + ")[\"amenity\"=\"cafe\"];"
                        + "node(" + coords + ")[\"amenity\"=\"biergarten\"];"
                        + "node(" + coords + ")[\"amenity\"=\"ice_cream\"];"
                        + "node(" + coords + ")[\"amenity\"=\"bbq\"];"
                        + "node(" + coords + ")[\"amenity\"=\"bench\"];"
                        + "node(" + coords + ")[\"amenity\"=\"lounger\"];"
                        + "node(" + coords + ")[\"amenity\"=\"drinking_water\"];"
                        + "node(" + coords + ")[\"amenity\"=\"water_point\"];"
                        + "node(" + coords + ")[\"amenity\"=\"shelter\"];"
                        + "node(" + coords + ")[\"amenity\"=\"toilets\"];"
                        + "node(" + coords + ")[\"information\"=\"guidepost\"][\"hiking\"=\"yes\"];"
                        + "node(" + coords + ")[\"railway\"][\"name\"];"
                        + "node(" + coords + ")[\"highway\"][\"name\"];"
                        + "node(" + coords + ")[\"tourism\"];"
                        + ");out%20qt;";
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
                /* fetch OSM data from former file retrieved from overpass-api
                   =================================================================================
                 */
                try {
                    if (assetManager != null)
                        inStream = assetManager.open("overpass_api_results_pois.txt");
                } catch (IOException e) {
                    Log.e(TAG,"submitSearch():" + e.getClass().getName() + " - " + e.getMessage());
                    _errorMessage = "assets/overpass_api_results_pois.txt could not be opened";
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

        if (!_trackListModel.changed) {
            // Calculate distances for each returned point
            DataPoint searchPoint = new DataPoint(Latitude.make(this._searchLatitude),
                    Longitude.make(this._searchLongitude));
            Unit distUnit = Config.getUnitSet().getDistanceUnit();
            ArrayList<SearchResult> reducedTrackList = new ArrayList<>();

            if (xmlHandler.getPointList() != null)
                for (SearchResult searchResult : xmlHandler.getPointList()) {
                    // Update single search result
                    searchResult.update();
                    double dist = DataPoint.calculateRadiansBetween(searchPoint, searchResult.getDataPoint());
                    searchResult.setDistance(Distance.convertRadiansToDistance(dist, distUnit));

                    searchResult.setPointType(translateTag(searchResult.getPointType()));
                    if (!searchResultIsDuplicate(searchResult))
                        reducedTrackList.add(searchResult);
                }

            // Add track list to model
            if (reducedTrackList.isEmpty())
                _errorMessage = Resources.getString(R.string.osm_pois_none_found);

            _trackListModel.addTracks(reducedTrackList, true);
        }
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
                return Resources.getString(ids[i]);
            }
        return symbol;
    }
}
