package de.biselliw.tour_navigator.tim_prune.function.search;

import android.content.res.Resources;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.stubs.Config;
import tim.prune.data.Distance;
import tim.prune.data.Latitude;
import tim.prune.data.Longitude;
import tim.prune.data.Unit;

import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

/**
 * Function to load nearby point information from OSM
 */
public class SearchOsmFunction extends GenericDownloaderFunction
{
    public static final String WAYPOINT_TYPE = "OSM";

	/** Maximum distance from point in m */
	private static final int MAX_DISTANCE = 500;

    static String[] symbols =   {"restaurant",       "bbq",        "bench",        "guidepost",        "bus_stop"};
    static int[] ids = {R.string.restaurant, R.string.bbq, R.string.bench, R.string.guidepost, R.string.bus_stop};

    /**
	 * Constructor
     * @param inApp App object
	 */
	public SearchOsmFunction(App inApp, TrackListModel inTrackListModel) {
		super(inApp, inTrackListModel);
	}

    public void getOSM(DataPoint inPoint, String inLang)
    {
        // Get coordinates from current point (if any)
        getSearchCoordinates(inPoint);
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
        // Submit search (language not an issue here)
        submitSearch(_searchLatitude, _searchLongitude);

        // Set status label according to error or "none found", leave blank if ok
        if (_errorMessage == null && _trackListModel.isEmpty()) {
            _errorMessage = resources.getString(R.string.osm_pois_nonefound);
        }
	}

	/**
	 * Submit the search for the given parameters
	 * @param inLat latitude
	 * @param inLon longitude
	 */
	private void submitSearch(double inLat, double inLon)
	{
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
			+ ");out%20qt;";
		// Parse the returned XML with a special handler
		SearchOsmPoisXmlHandler xmlHandler = new SearchOsmPoisXmlHandler();
        _errorMessage = "";

        try
		{
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

		// Calculate distances for each returned point
		DataPoint searchPoint = new DataPoint(Latitude.make(_searchLatitude),
			Longitude.make(_searchLongitude));
		Unit distUnit = Config.getUnitSet().getDistanceUnit();
		for (SearchResult searchResult : xmlHandler.getPointList())
		{
            // Update single search result
            searchResult.update();
/*
			DataPoint foundPoint = new DataPoint(Latitude.make(searchResult.getLatitude()),
				Longitude.make(searchResult.getLongitude()));
 */
			double dist = DataPoint.calculateRadiansBetween(searchPoint, searchResult.getDataPoint());
			searchResult.setLength(Distance.convertRadiansToDistance(dist, distUnit));

            searchResult.setPointType(translateTag(searchResult.getPointType()));
		}

		// TODO: maybe limit number of results using MAX_RESULTS
		// Add track list to model
		ArrayList<SearchResult> pointList = xmlHandler.getPointList();
		_trackListModel.addTracks(pointList, true);
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

    private static String translateTag(String symbol) {
        for (int i = 0; i < symbols.length; i++)
            if (symbols[i].equals(symbol)) {
                return resources.getString(ids[i]);
            }
        return symbol;
    }
/*            + "node(" + coords + ")[\"amenity\"=\"drinking_water\"];"
                    + "node(" + coords + ")[\"amenity\"=\"water_point\"];"
                    + "node(" + coords + ")[\"amenity\"=\"shelter\"];"
                    + "node(" + coords + ")[\"amenity\"=\"toilets\"];"

 */

}
