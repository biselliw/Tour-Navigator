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
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchOsmFunction;

/**
 * Search dialog to add waypoints provided by OSM
 */
public class OSM_Dialog extends SearchResultDialog {

    public OSM_Dialog(Context context, DataPoint inPoint) {
        super(context, context.getString(R.string.osm_title), inPoint);

        SearchOsmFunction searchOsmPoisFunction = new SearchOsmFunction(App.app, trackListModel);
        prefixWaypointType = searchOsmPoisFunction.getOSM(inPoint, lang);
        searchFunction = searchOsmPoisFunction;
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
}
