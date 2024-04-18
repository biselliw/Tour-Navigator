package de.biselliw.tour_navigator.data;

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
    along with FairEmail. If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2024 Walter Biselli (BiselliW)
*/

import android.content.res.Resources;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.SourceInfo;

public class TourDetails {

    public static TourDetails details = null;
    App app;
    RecordAdapter recordAdapter;
    private final Resources res;

    public TourDetails(BaseActivity activity, App app, RecordAdapter recordAdapter)
    {
        details = this;
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
     * @return true description of the route point or its linked one if available
     */
    public String getRoutePointDescription(int inPlace) {
        String description = "";

        if ((inPlace >= 0) && (inPlace < recordAdapter.getCount())) {
            RecordAdapter.Record record = recordAdapter.getItem(inPlace);
            DataPoint point = record.getTrackPoint();
            if (point != null) {
                description = point.getDescription();
                if (description.equals("")) {
                    if (point.getLinkIndex() >= 0) {
                        point = app.getPoint(point.getLinkIndex());
                        if (point != null) {
                            description = point.getDescription();
                        }
                    }
                }
            }
        }
        return description;
    }

    /**
     * Interprete the Waypoint symbol provided by outdooractive GPX files
     * @param symbol outdooractive specific waypoint symbol
     * @return interpreted string
     */
    public String interpreteWaypointSymbol(String symbol)
    {
        switch (symbol) {
            case "waypointDirRightComb":
                symbol = res.getString(R.string.waypointDirRightComb);
                break;
            case "waypointDirLeftComb":
                symbol = res.getString(R.string.waypointDirLeftComb);
                break;
            case "waypointUpComb":
                symbol = res.getString(R.string.waypointUpComb);
                break;
            case "waypointFlagComb":
                symbol = res.getString(R.string.waypointFlagComb);
                break;
        }
        return symbol;
    }

    /**
     * show the comment and description linked to a place
     *
     * @param inPlace row index of the table
     */
    // todo move ?
    public AdditionalInfo getAdditionalInfo(int inPlace) {
        AdditionalInfo info = new AdditionalInfo();

        /* place given? */
        if (inPlace >= 0)
        {
            /* provide place info */
            if (inPlace < recordAdapter.getCount()) {
                RecordAdapter.Record record = recordAdapter.getItem(inPlace);
                if (record == null) return null;
                DataPoint point = record.getTrackPoint();
                if (point == null) return null;

                info.title   = point.getRoutePointName();
                info.comment = point.getComment();
                info.type    = point.getWaypointType();
                info.symbol  = point.getWaypointSymbol();
                info.description = point.getDescription();

                if (info.description.equals(""))
                {
                    if (point.getLinkIndex() >= 0)
                    {
                        point = app.getPoint(point.getLinkIndex());
                        if (point != null) {
                            info.type        = point.getWaypointType();
                            info.description = point.getDescription();
                            info.link        = point.getWebLink();
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
                    info.symbol = interpreteWaypointSymbol(info.symbol);

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
            /* provide info from GPX file */
            info.comment     = app.trackName;
            info.title       = app.trackName;
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
