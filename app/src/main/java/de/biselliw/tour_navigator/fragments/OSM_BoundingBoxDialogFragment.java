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
import de.biselliw.tour_navigator.function.search.SearchOsmBoundingBoxFunction;
import de.biselliw.tour_navigator.ui.ControlElements;

import static android.view.View.GONE;
import static androidx.constraintlayout.widget.ConstraintSet.VISIBLE;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_DISTANCE_KM;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_NAME;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_REF;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_TYPE;

/**
 * Search dialog to add OSM guideposts along the track
 */
public class OSM_BoundingBoxDialogFragment extends SearchResultDialogFragment {

    private boolean queryGuideposts = false;
    private boolean queryPOIs = false;

    /**
     * Basic dialog for searching POIs
     *
     * @param inActivity context of the class
     */
    public static OSM_BoundingBoxDialogFragment newInstance(ControlElements inActivity) {
        return new OSM_BoundingBoxDialogFragment();
    }

    /**
     * Request a query for Guideposts
     * @return this fragment
     */
    public OSM_BoundingBoxDialogFragment queryGuideposts ()
    {
        queryGuideposts = true;
        return this;
    }

    /**
     * Request a query for POIs
     * @return this fragment
     */
    public OSM_BoundingBoxDialogFragment queryPOIs()
    {
        queryPOIs = true;
        return this;
    }

    /**
     * Set a notification routine to be called after adding route points
     * @param inNotification Runnable as callback function
     */
    public OSM_BoundingBoxDialogFragment setNotification (Runnable inNotification)
    {
        notification = inNotification;
        return this;
    }

    @Override
    public void onAttach( @NonNull Context context ) {
        super.onAttach(context);
        SearchOsmBoundingBoxFunction searchOsm = new SearchOsmBoundingBoxFunction((ControlElements)context, trackListModel);
        searchFunction = searchOsm;
        searchOsm.assetManager = this.requireContext().getAssets();

        if (queryGuideposts)
            prefixWaypointType = searchOsm.getOsmGuideposts();
        else if (queryPOIs)
            prefixWaypointType = searchOsm.getOsmPOIs();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadButtonAll.setVisibility(GONE);
        showButton.setVisibility(GONE);
        if (queryGuideposts)
            loadButtonAll.setVisibility(View.VISIBLE);
        if (queryPOIs)
            showButton.setVisibility(View.VISIBLE);
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
        if (queryGuideposts) {
            switch (inColNum) {
                case 0:
                    return getString(R.string.distance);
                case 1:
                    return getString(R.string.guide_post_name);
                default:
                    return getString(R.string.wpt_ref);
            }
        }
        if (queryPOIs) {
            switch (inColNum) {
                case 0:
                    return getString(R.string.distance);
                case 1:
                    return getString(R.string.poi_name);
                default:
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
     * @implNote: not used
     */
    protected String getColumnKey(int inColNum) {
        if (queryGuideposts)
            switch (inColNum) {
                case 0:
                    return COL_KEY_DISTANCE_KM;
                case 1:
                    return COL_KEY_NAME;
                default:
                    return COL_KEY_REF;
            }
        if (queryPOIs)
            switch (inColNum) {
                case 0:
                    return COL_KEY_DISTANCE_KM;
                case 1:
                    return COL_KEY_NAME;
                default:
                    return COL_KEY_TYPE;
            }
        return "";
    }
}