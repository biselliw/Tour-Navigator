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
*/

import android.content.Context;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.function.search.GetWaypointsFunction;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

/**
 * Search dialog to add waypoints provided by the GPX file which are out of track
 */
public class WaypointsDialog extends SearchResultDialog  {

    public WaypointsDialog(Context context, DataPoint inPoint) { super(context,
            context.getString(R.string.wpt_title),
            inPoint);

        GetWaypointsFunction getWaypointsFunction = new GetWaypointsFunction(App.app, trackListModel);
        getWaypointsFunction.getWaypoints(inPoint, lang);
        searchFunction = getWaypointsFunction;
    }

    /**
     * @param inColNum index of column, 0 or 1
     * @return key for this column
     */
    protected String getColumnKey(int inColNum)
    {
        return "";
    }
};
