package de.biselliw.tour_navigator.dialogs;

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

    replaced by WaypointsDialogFragment
*/

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.function.search.GetWaypointsFunction;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.ui.ControlElements;

/**
 * Search dialog to add waypoints provided by the GPX file which are out of track
 */
public class WaypointsDialog extends SearchResultDialog  {

    public WaypointsDialog(ControlElements inActivity, DataPoint inPoint) {
        super(inActivity, inActivity.getString(R.string.wpt_title),inPoint);

        GetWaypointsFunction getWaypointsFunction = new GetWaypointsFunction(inActivity, trackListModel);
        prefixWaypointType = getWaypointsFunction.getWaypoints(inPoint, lang);
        searchFunction = getWaypointsFunction;
    }

    /**
     * Get keys for column titles
     * @param inColNum index of column, 0 or 1
     * @return key for this column
     * @implNote: not used
     */
    @Override
    protected String getColumnKey(int inColNum)
    {
        return "";
    }
};
