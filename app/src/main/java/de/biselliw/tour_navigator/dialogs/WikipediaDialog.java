package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.GetWikipediaFunction;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;


public class WikipediaDialog extends SearchResultDialog  {

    public WikipediaDialog(Context context, DataPoint inPoint) {
        super(context, inPoint);

        GetWikipediaFunction getWikipediaFunction = new GetWikipediaFunction((MainActivity) context, _trackListModel);
        getWikipediaFunction.getWikipedia(inPoint, lang);
        _searchFunction = getWikipediaFunction;
        _waypointType = "Wikipedia";
        _protectWaypoint = true;
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

    void showSelected(int selected) {
        SearchResult searchResult = _trackListModel.getTrack(selected);
        if (searchResult != null) {

            String url = "https://de.wikipedia.org/wiki/" + searchResult.getTrackName();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            _context.startActivity(Intent.createChooser(intent, "Open with"));
        }
    }
};

