package de.biselliw.tour_navigator.fragments;
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
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.function.search.GetWaypointsFunction;
import de.biselliw.tour_navigator.ui.ControlElements;

import static android.view.View.GONE;
import static androidx.constraintlayout.widget.ConstraintSet.VISIBLE;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_DISTANCE_KM;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_NAME;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_TYPE;

/**
 * Search dialog to add waypoints along the track which are provided by the GPX file but are out of track
 */
public class WaypointsDialogFragment extends SearchResultDialogFragment {

    /**
     * Basic dialog for searching POIs
     *
     * @param inActivity context of the class
     */
    public static WaypointsDialogFragment newInstance(ControlElements inActivity) {
        WaypointsDialogFragment fragment = new WaypointsDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", inActivity.getString(R.string.wpts_out_of_track));
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach( @NonNull Context context ) {
        super.onAttach(context);
        GetWaypointsFunction getWaypointsFunction = new GetWaypointsFunction((ControlElements)context, trackListModel);
        prefixWaypointType = getWaypointsFunction.getWaypoints(null, lang);
        searchFunction = getWaypointsFunction;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadButtonAll.setVisibility(VISIBLE);
        showButton.setVisibility(GONE);
    }

    /** Set a notification routine to be called after adding route points
     * @param inNotification Runnable as callback function
     */
    public WaypointsDialogFragment setNotification (Runnable inNotification)
    {
        notification = inNotification;
        return this;
    }

    /**
     * @return column count
     */
    @Override
    public int getColumnCount()
    {
        return 3;
    }

    /**
     * Get column titles
     * @param inColNum index of column
     * @return key for this column
     */
    @Override
    protected String getColumnTitle(int inColNum)
    {
        switch (inColNum) {
            case 0:
                return getString(R.string.distance);
            case 1:
                return getString(R.string.waypoint_name);
            default:
                return getString(R.string.wpt_type);
        }
    }

    /**
     * Get column key for interpretation of contents
     *
     * @param inColNum index of column (1,2,3)
     * @return key for this column
     * @implNote: not used
     */
    protected String getColumnKey(int inColNum) {
        switch (inColNum) {
            case 0:
                return COL_KEY_DISTANCE_KM;
            case 1:
                return COL_KEY_NAME;
            default:
                return COL_KEY_TYPE;
        }
    }

}