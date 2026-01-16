package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchOsmPoisFunction;
import de.biselliw.tour_navigator.tim_prune.function.search.SearchResult;

public class OSM_Dialog extends SearchResultDialog {

    public OSM_Dialog(Context context, DataPoint inPoint) {
        super(context, inPoint);

        TextView view = findViewById(R.id.search_title);
        view.setText(context.getString(R.string.find_nearby_osm));
        view = findViewById(R.id.bt_show);
        view.setText(context.getString(R.string.osm_poi_show));

        SearchOsmPoisFunction searchOsmPoisFunction = new SearchOsmPoisFunction((MainActivity) context, _trackListModel);
        searchOsmPoisFunction.getOSM(inPoint, lang);
        _searchFunction = searchOsmPoisFunction;
        _waypointType = "OSM";
        _protectWaypoint = true;
    }

    @Override
    protected String getColumnKey(int inColNum) {
        return "";
    }

    @Override
    void showSelected(int selected) {
        SearchResult searchResult = _trackListModel.getTrack(selected);
        if (searchResult != null) {
            String url = searchResult.getWebUrl();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            _context.startActivity(Intent.createChooser(intent, "Open with"));
        }
    }
}
