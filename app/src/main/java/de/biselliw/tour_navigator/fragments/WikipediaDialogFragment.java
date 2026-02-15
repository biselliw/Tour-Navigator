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
import de.biselliw.tour_navigator.function.search.SearchOsmGuidePostsFunction;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.GetWikipediaFunction;
import de.biselliw.tour_navigator.ui.ControlElements;

import static android.view.View.GONE;
import static androidx.constraintlayout.widget.ConstraintSet.VISIBLE;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_DISTANCE_KM;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_DISTANCE_M;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_NAME;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.COL_KEY_TYPE;

/**
 * Search dialog to add Wikipedia articles
 */
public class WikipediaDialogFragment extends SearchResultDialogFragment {

    /**
     * Basic dialog for searching POIs
     *
     * @param inActivity context of the class
     */
    public static WikipediaDialogFragment newInstance(ControlElements inActivity, DataPoint inPoint) {
        dataPoint = inPoint;
        WikipediaDialogFragment fragment = new WikipediaDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", inActivity.getString(R.string.wikipedia_title));
        fragment.setArguments(args);
        return  fragment;
    }

    @Override
    public void onAttach( @NonNull Context context ) {
        super.onAttach(context);
        GetWikipediaFunction getWikipediaFunction = new GetWikipediaFunction((ControlElements)context, trackListModel);
        prefixWaypointType = getWikipediaFunction.getWikipedia(dataPoint, lang);
        searchFunction = getWikipediaFunction;
        getWikipediaFunction.assetManager = this.requireContext().getAssets();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadButtonAll.setVisibility(GONE);
        showButton.setVisibility(VISIBLE);
    }


    /** Set a notification routine to be called after adding route points
     * @param inNotification Runnable as callback function
     */
    public WikipediaDialogFragment setNotification (Runnable inNotification)
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
        return 2;
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
                return "";
            case 1:
            default:
                return getString(R.string.wikipedia_article_name);
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
            default:
                return COL_KEY_NAME;
        }
    }
}