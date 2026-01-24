package de.biselliw.tour_navigator.dialogs;

import android.content.Context;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.GetWikipediaFunction;

/**
 * Search dialog to add Wikipedia articles to the track
 */
public class WikipediaDialog extends SearchResultDialog  {

    public WikipediaDialog(Context context, DataPoint inPoint) {
        super(context, context.getString(R.string.wikipedia_title), inPoint);

        GetWikipediaFunction getWikipediaFunction = new GetWikipediaFunction(App.app, trackListModel);
        getWikipediaFunction.getWikipedia(inPoint, lang);
        searchFunction = getWikipediaFunction;
    }

    /**
     * @param inColNum index of column, 0 or 1
     * @return key for this column
     */
    protected String getColumnKey(int inColNum)
    {
        if (inColNum == 0) return "dialog.wikipedia.column.name";
        return "dialog.wikipedia.column.distance";
    }

};

