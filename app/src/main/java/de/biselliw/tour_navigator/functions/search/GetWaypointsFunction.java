package de.biselliw.tour_navigator.functions.search;

/*
    This file is part of Tour Navigator

    Tour Navigator is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    Tour Navigator is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/
import java.util.ArrayList;

import de.biselliw.tour_navigator.stubs.Config;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;
import de.biselliw.tour_navigator.ui.ControlElements;
import tim.prune.data.Distance;
import tim.prune.data.Unit;

/**
 * Function to load waypoints provided by the GPX file which are out of track
 */
public class GetWaypointsFunction extends GenericSearchFunction
{
    /** new waypoint type required for GPX file */
    public static final String WAYPOINT_TYPE = "WPT";

    /**
     * Constructor
     * @param inActivity parent activity
     */
    public GetWaypointsFunction(ControlElements inActivity) {
        super(inActivity);
    }

    public String getWaypoints() {
        if (track != null) {
            // background work
            new Thread(this).start();
        }
        return WAYPOINT_TYPE;
    }

    /**
     * Get all waypoints which are not linked to the track
     */
    @Override
    public void run() {
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
                    double minDistance = 9999.9, foundDistance = 0.0;;
                    for (int i = 0; i < track.getNumPoints(); i++) {
                        DataPoint trackPoint = track.getPoint(i);
                        if (trackPoint != null && !trackPoint.isWaypoint()) {
                            double dist = DataPoint.calculateRadiansBetween(searchPoint, trackPoint);
                            if (dist >= 0) {
                                double distance = Distance.convertRadiansToDistance(dist, distUnit);
                                if (distance < minDistance) {
                                    minDistance = distance;
                                    foundDistance = trackPoint.getDistance();
                                    if (distance < 0.01)
                                        break;
                                }
                            }
                        }
                    }
                    if (minDistance >= 0) {
                        SearchResult result = new SearchResult();
                        result.setDataPoint(searchPoint);
                        result.setTrackName(searchPoint.getWaypointName());
                        // set the distance of the found point with regard to the start of the track
                        result.setDistance(foundDistance);
                        result.setWebUrl(searchPoint.getWebLink());
                        result.setDescription(searchPoint.getDescription());
                        result.setPointType(searchPoint.getWaypointType());
                        trackList.add(result);
                    }
                }
            }
        }
        // Add track list to model
        if (trackListModel != null) {
            trackListModel.changed = true;
            trackListModel.addTracks(trackList, true);
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
