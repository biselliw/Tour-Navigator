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
import de.biselliw.tour_navigator.functions.search.GetWikipediaFunction;
import de.biselliw.tour_navigator.ui.ControlElements;

import static android.view.View.GONE;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.KEY_DISTANCE_FROM_START_KM;
import static de.biselliw.tour_navigator.tim_prune.function.search.TrackListModel.KEY_NAME;

/**
 * Search dialog to add Wikipedia articles
 */
public class WikipediaDialogFragment extends SearchResultDialogFragment {

    /**
     * Basic dialog for searching POIs
     *
     * @param inActivity context of the class
     */
    public static WikipediaDialogFragment newInstance(ControlElements inActivity) {
        searchFunction = new GetWikipediaFunction(inActivity);
        return new WikipediaDialogFragment();
    }

    @Override
    public void onAttach( @NonNull Context context ) {
        super.onAttach(context);
        GetWikipediaFunction search = (GetWikipediaFunction)searchFunction;
        if (search != null) {
            if (search.queryAround)
                // Find Wikipedia articles nearby the given way point
                prefixWaypointType = search.queryAround();
            else
                // Find Wikipedia articles within a bounding box covering the track
                prefixWaypointType = search.wikipediaBoundingBox();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadButtonAll.setVisibility(GONE);
        showButton.setVisibility(View.VISIBLE);
    }

    /**
     * @return column count
     */
    @Override
    public int getColumnCount()
    {
        if (searchFunction != null) {
            if (searchFunction.queryAround)
                return 2;
            else
                return 1;
        }
        return 0;
    }

    /**
     * Get column titles
     * @param inColNum index of column
     * @return title for this column
     * @implNote all three columns are always in use!
     */
    @Override
    protected String getColumnTitle(int inColNum)
    {
        if (searchFunction != null) {
            if (!searchFunction.queryAround || inColNum == 1)
                return getString(R.string.wikipedia_article_name);
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
        if (searchFunction != null) {
            if (!searchFunction.queryAround || inColNum == 1)
                return KEY_NAME;
            else
                return KEY_DISTANCE_FROM_START_KM;
        }
        return 0;
    }
}