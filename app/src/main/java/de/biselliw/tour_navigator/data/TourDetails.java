package de.biselliw.tour_navigator.data;

import android.content.res.Resources;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import tim.prune.data.SourceInfo;

public class TourDetails {

    App app;
    RecordAdapter recordAdapter;
    private final Resources res;

    public TourDetails(BaseActivity activity, App app, RecordAdapter recordAdapter)
    {
        this.res = activity.getResources();
        this.app = app;
        this.recordAdapter = recordAdapter;
    }

    /**
     * Check if File Info is available
     * @return true if File Info is available
     */
    public boolean isFileInfoAvailable() {
        String description = "";
        SourceInfo sourceInfo = App.getSourceInfo();
        if (sourceInfo != null)
            description = sourceInfo.getTrackDescription();

        return (!description.equals(""));
    }

    /**
     * @param inPlace row index of the table
     * @return true if description is available
     */
    public boolean isDescriptionAvailable(int inPlace) {
        String description = "";

        if ((inPlace >= 0) && (inPlace < recordAdapter.getCount())) {
            RecordAdapter.Record record = recordAdapter.getItem(inPlace);
            DataPoint point = record.getTrackPoint();
            if (point != null) {
                description = point.getWaypointDescription();
                if (description.equals("")) {
                    if (point.getLinkIndex() >= 0) {
                        point = app.getPoint(point.getLinkIndex());
                        if (point != null) {
                            description = point.getWaypointDescription();
                        }
                    }
                }
            }
        }
        return !description.equals("");
    }



    /**
     * show the comment and description linked to a place
     *
     * @param inPlace row index of the table
     */
    // todo move ?
    public AdditionalInfo getAdditionalInfo(int inPlace) {
        AdditionalInfo info = new AdditionalInfo();

        if (inPlace >= 0)
        {
            if (inPlace < recordAdapter.getCount()) {
                RecordAdapter.Record record = recordAdapter.getItem(inPlace);
                if (record == null) return null;
                DataPoint point = record.getTrackPoint();
                if (point == null) return null;

                info.title   = point.getRoutePointName();
                info.comment = point.getWaypointComment();
                info.type    = point.getWaypointType();
                info.symbol  = point.getWaypointSymbol();
                info.description = point.getWaypointDescription();

                if (info.description.equals(""))
                {
                    if (point.getLinkIndex() >= 0)
                    {
                        point = app.getPoint(point.getLinkIndex());
                        if (point != null) {
                            info.type        = point.getWaypointType();
                            info.description = point.getWaypointDescription();
                            info.link        = point.getWaypointLink();
                        }
                    }
                }
                if (!info.type.equals(""))
                {
                    if (info.comment.equals(""))
                        info.comment = info.type;
                    else
                        info.comment = info.type + ": " + info.comment;
                }
                else if (!info.symbol.equals(""))
                {
                    switch (info.symbol) {
                        case "waypointDirRightComb":
                            info.symbol = res.getString(R.string.waypointDirRightComb);
                            break;
                        case "waypointDirLeftComb":
                            info.symbol = res.getString(R.string.waypointDirLeftComb);
                            break;
                        case "waypointUpComb":
                            info.symbol = res.getString(R.string.waypointUpComb);
                            break;
                        case "waypointFlagComb":
                            info.symbol = res.getString(R.string.waypointFlagComb);
                            break;
                    }

                    if (!info.comment.equals(""))
                        info.comment = info.symbol + ": " + info.comment;
                    else if (!info.description.equals(""))
                        info.comment = info.symbol + ": " + info.description;
                    else
                        info.comment = info.symbol;
                }
            }
        }
        else
        {
            info.comment     = app.TrackName;
            info.title       = app.TrackName;
            info.description = "";
            info.link        = "";
            SourceInfo sourceInfo = App.getSourceInfo();
            if (sourceInfo != null) {
                info.description = sourceInfo.getTrackDescription();
                info.link        = sourceInfo.getMetaLink();
            }
        }

        return info;
    }

    static public class AdditionalInfo
    {
        public String title = "";
        public String comment = "";
        public String description = "";
        public String type;
        public String symbol;
        public String link = "";
    }

}
