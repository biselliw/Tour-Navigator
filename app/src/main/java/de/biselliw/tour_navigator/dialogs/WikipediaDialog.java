package de.biselliw.tour_navigator.dialogs;

import android.content.Context;

import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.GetWikipediaFunction;


public class WikipediaDialog extends SearchResultDialog  {
    private GetWikipediaFunction getWikipediaFunction = null;

    public WikipediaDialog(Context context, DataPoint inPoint) {
        super(context, inPoint);

        getWikipediaFunction = new GetWikipediaFunction((MainActivity)context, _trackListModel);
        getWikipediaFunction.getWikipedia(inPoint, lang);

    }

    /**
     * @return name key
     */
    public String getNameKey() {
        return "function.getwikipedia";
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



}
