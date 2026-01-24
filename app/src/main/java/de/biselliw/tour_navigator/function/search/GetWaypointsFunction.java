package de.biselliw.tour_navigator.function.search;

import java.util.ArrayList;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.stubs.Config;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.GenericDownloaderFunction;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel;
import tim.prune.data.Distance;
import tim.prune.data.Unit;

/**
 * Function to load waypoints provided by the GPX file which are out of track
 */
public class GetWaypointsFunction extends GenericDownloaderFunction
{
    public static final String WAYPOINT_TYPE = "WPT";

    /**
     * Constructor
     * @param inApp App object
     */
    public GetWaypointsFunction(App inApp, TrackListModel inTrackListModel) {
        super(inApp, inTrackListModel);
    }

    public void getWaypoints(DataPoint inPoint, String inLang) {
        if (track != null) {
            // background work
            new Thread(this).start();
        }
    }

    @Override
    public void run() {
        // Submit search
        submitSearch();
    }

    private void submitSearch() {
        // get all waypoints which are not linked to the track
        ArrayList<DataPoint> wayPoints = track.getWayPointsOutOfTrack();

        ArrayList<SearchResult> trackList = new ArrayList<>();
        _errorMessage = "";

        // are waypoints out of track remaining ?
        // condition is always true as menu item is invisible otherwise
        if (!wayPoints.isEmpty()) {
            Unit distUnit = Config.getUnitSet().getDistanceUnit();
            for (DataPoint searchPoint : wayPoints) {
                // show only way points outside the track which are not yet linked to the track
                if (searchPoint.getLinkIndex() < 0) {
                    // Calculate distances to track for each way point
                    double minDistance = 9999.9;
                    for (int i = 0; i < track.getNumPoints(); i++) {
                        DataPoint trackPoint = track.getPoint(i);
                        if (trackPoint != null && !trackPoint.isWaypoint()) {
                            double dist = DataPoint.calculateRadiansBetween(searchPoint, trackPoint);
                            double distance = Distance.convertRadiansToDistance(dist, distUnit);
                            if (distance < minDistance) {
                                minDistance = distance;
                                if (distance < 0.01)
                                    break;
                            }
                        }
                    }
                    if (minDistance >= 0) {
                        SearchResult result = new SearchResult();
                        result.setDataPoint(searchPoint);
                        result.setTrackName(searchPoint.getWaypointName());
                        result.setLength(Distance.convertRadiansToDistance(minDistance, distUnit));
                        result.setWebUrl(searchPoint.getWebLink());
                        result.setDescription(searchPoint.getDescription());
                        result.setPointType(searchPoint.getWaypointType());
                        trackList.add(result);
                    }
                }
            }
        }
        // Add track list to model
        if (_trackListModel != null) {
            _trackListModel.changed = true;
            _trackListModel.addTracks(trackList, true);
        }
    }

    /**
     * Interpret the Waypoint symbol provided by GPX file
     * @param type specific waypoint type
     * @return translated string
     */
    public static String interpretWaypointType(String type)
    {
        return type;
    }

}
