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
import de.biselliw.tour_navigator.function.search.GetOpenStreetMapFunction;
import de.biselliw.tour_navigator.ui.ControlElements;

import static android.view.View.GONE;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.KEY_DISTANCE_FROM_START_KM;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.KEY_NAME;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_REF;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.KEY_TYPE;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.KEY_DISTANCE_TO_TRACK_M;

/**
 * Search dialog to add OSM guideposts along the track
 */
public class OpenStreetMapDialogFragment extends SearchResultDialogFragment {

    /**
     * Basic dialog for searching POIs
     *
     * @param inActivity context of the class
     * @implNote Needs one of these additional calls:
     * @see #findGuideposts : Query guideposts only
     * @see #findPOIs : Query all hiking relevant POIs except of guideposts
     */
    public static OpenStreetMapDialogFragment newInstance(ControlElements inActivity) {
        searchFunction = new GetOpenStreetMapFunction(inActivity);
        return new OpenStreetMapDialogFragment();
    }

    /**
     * Request a query for guideposts
     * @return this fragment
     */
    public OpenStreetMapDialogFragment findGuideposts()
    {
        GetOpenStreetMapFunction searchOSM = (GetOpenStreetMapFunction)searchFunction;
        if (searchOSM != null)
            searchOSM.findGuideposts = true;
        return this;
    }

    /**
     * Request a query for POIs
     * @return this fragment
     */
    public OpenStreetMapDialogFragment findPOIs()
    {
        GetOpenStreetMapFunction searchOSM = (GetOpenStreetMapFunction)searchFunction;
        if (searchOSM != null)
            searchOSM.findPOIs = true;
        return this;
    }

    @Override
    public void onAttach( @NonNull Context context ) {
        super.onAttach(context);
        GetOpenStreetMapFunction searchOSM = (GetOpenStreetMapFunction)searchFunction;
        if (searchOSM != null) {
            if (searchOSM.queryAround)
                // Find POIs nearby the given way point
                prefixWaypointType = searchOSM.queryAround();
            if (searchFunction.queryBoundingBox)
                // Find POIs within a bounding box covering the track
                prefixWaypointType = searchOSM.queryBoundingBox();
        }
//        if (dataPoint != null) ...

//        if (queryBoundingBox)

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadButtonAll.setVisibility(GONE);
        showButton.setVisibility(GONE);
        GetOpenStreetMapFunction searchOSM = (GetOpenStreetMapFunction) searchFunction;
        if (searchOSM != null) {
            if (searchOSM.findGuideposts)
                loadButtonAll.setVisibility(View.VISIBLE);
            if (searchOSM.findPOIs)
                showButton.setVisibility(View.VISIBLE);
        }
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
        GetOpenStreetMapFunction searchOSM = (GetOpenStreetMapFunction)searchFunction;
        if (searchOSM != null) {
            switch (inColNum) {
                case 0:
                    return getString(R.string.distance);
                case 1:
                    if (searchOSM.findGuideposts)
                        return getString(R.string.guide_post_name);
                    else
                        return getString(R.string.poi_name);
                default:
                    if (searchOSM.findGuideposts)
                        return getString(R.string.wpt_ref);
                    else
                        return getString(R.string.wpt_type);
            }
        }
        return "";
    }

    /**
     * Get column key for interpretation of contents
     *
     * @param inColNum index of column (1,2,3)
     * @return key for this column
     */
    protected int getColumnKey(int inColNum) {
        GetOpenStreetMapFunction searchOSM = (GetOpenStreetMapFunction)searchFunction;
        if (searchOSM != null) {
            if (searchOSM.queryAround) {
                switch (inColNum) {
                    case 0:
                        return KEY_DISTANCE_TO_TRACK_M;
                    case 1:
                        return KEY_NAME;
                    default:
                        return KEY_TYPE;
                }
            }
            else if (searchOSM.queryBoundingBox) {
                switch (inColNum) {
                    case 0:
                        return KEY_DISTANCE_FROM_START_KM;
                    case 1:
                        return KEY_NAME;
                    default:
                        if (searchOSM.findGuideposts)
                            return COL_KEY_REF;
                        else
                            return KEY_TYPE;
                }
            }
        }
        return 0;
    }
}