package de.biselliw.tour_navigator.dialogs;

import android.content.Context;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.GetWikipediaFunction;
import de.biselliw.tour_navigator.ui.ControlElements;

/**
 * Search dialog to add Wikipedia articles to the track
 */
public class WikipediaDialog extends SearchResultDialog  {

    public WikipediaDialog(ControlElements inActivity, DataPoint inPoint) {
        super(inActivity, inActivity.getString(R.string.wikipedia_title), inPoint);

        GetWikipediaFunction getWikipediaFunction = new GetWikipediaFunction(inActivity, trackListModel);
        prefixWaypointType = getWikipediaFunction.getWikipedia(inPoint, lang);
        searchFunction = getWikipediaFunction;
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

