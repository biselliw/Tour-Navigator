package de.biselliw.tour_navigator.tim_prune.function.search;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.stubs.Config;
import tim.prune.data.Distance;
import tim.prune.data.Latitude;
import tim.prune.data.Longitude;
import tim.prune.data.Unit;

import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

/**
 * Function to load nearby point information from OSM
 */
public class SearchOsmPoisFunction extends GenericDownloaderFunction
{
	/** Maximum distance from point in m */
	private static final int MAX_DISTANCE = 500;
	/** Coordinates to search for */
	private double _searchLatitude = 0.0, _searchLongitude = 0.0;

    private DataPoint dataPoint = null;

    BaseActivity _activity;

	/**
	 * Constructor
	 * @param activity
	 */
	public SearchOsmPoisFunction(BaseActivity activity, TrackListModel inTrackListModel) {
		super(null, inTrackListModel);
        _activity = activity;
	}

    public void getOSM(DataPoint inPoint, String inLang)
    {
        dataPoint = inPoint;
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
        if(dataPoint == null) return;
        // Get coordinates from current point (if any) or from centre of screen

        _searchLatitude  = dataPoint.getLatitude().getDouble();
        _searchLongitude = dataPoint.getLongitude().getDouble();

        // Submit search (language not an issue here)
        submitSearch(_searchLatitude, _searchLongitude);

        // Set status label according to error or "none found", leave blank if ok
        if (_errorMessage == null && _trackListModel.isEmpty()) {
            _errorMessage = _activity.getString(R.string.osm_pois_nonefound);
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
				_errorMessage = _activity.getString(R.string.osm_timeout); // e.getClass().getName() + " - " + e.getMessage();
                _trackListModel.changed = true;
                return;
			}
		} catch (MalformedURLException | SAXException | ParserConfigurationException ignored) {
		}

		// Calculate distances for each returned point
		DataPoint searchPoint = new DataPoint(Latitude.make(_searchLatitude),
			Longitude.make(_searchLongitude));
		Unit distUnit = Config.getUnitSet().getDistanceUnit();
		for (SearchResult result : xmlHandler.getPointList())
		{
			DataPoint foundPoint = new DataPoint(Latitude.make(result.getLatitude()),
				Longitude.make(result.getLongitude()));
			double dist = DataPoint.calculateRadiansBetween(searchPoint, foundPoint);
			result.setLength(Distance.convertRadiansToDistance(dist, distUnit));
		}

		// TODO: maybe limit number of results using MAX_RESULTS
		// Add track list to model
		ArrayList<SearchResult> pointList = xmlHandler.getPointList();
		_trackListModel.addTracks(pointList, true);
		_trackListModel.setShowPointTypes(true);
	}
}
