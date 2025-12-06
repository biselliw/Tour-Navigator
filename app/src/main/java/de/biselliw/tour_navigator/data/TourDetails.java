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
    If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2025 Walter Biselli (BiselliW)
*/

import android.content.Context;
import android.content.res.Resources;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.SourceInfo;

public class TourDetails {

    public static TourDetails details = null;
    App app;
    RecordAdapter recordAdapter;
    private final Resources res;

    public TourDetails(Context inContext, App app, RecordAdapter recordAdapter)
    {
        details = this;
        this.res = inContext.getResources();
        this.app = app;
        this.recordAdapter = recordAdapter;
    }

    // todo move ?
    /**
     * provide the comment and description linked to a place
     *
     * @param logErrors log system errors instead of tour information
     * @param inPlace row index of the table
     */
    public AdditionalInfo getAdditionalInfo(boolean logErrors, int inPlace) {

        if (inPlace >= 0)
            return  getWaypointInfo(inPlace);
        else
            if (logErrors)
                return getErrorInfo();
            else
                return getFileInfo();
    }

    public int getWptCount () {
        if (recordAdapter != null)
            return recordAdapter.getCount();
        else
            return 0;
    }

    /**
     * Check if File Info is available
     * @return true if File Info is available
     * /
    public boolean isFileInfoAvailable() {
        String description = "";
        SourceInfo sourceInfo = App.getSourceInfo();
        if (sourceInfo != null)
            description = sourceInfo.getFileDescription();

        return (!description.isEmpty());
    }


    /**
     * @param inPlace row index of the table
     * @return true description of the route point or its linked one if available
     * /
    public String getRoutePointDescription(int inPlace) {
        String description = "";

        if ((inPlace >= 0) && (inPlace < recordAdapter.getCount())) {
            RecordAdapter.Record record = recordAdapter.getItem(inPlace);
            DataPoint point = record.getTrackPoint();
            if (point != null) {
                description = point.getDescription();
                if (description.isEmpty()) {
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
    */

    /**
     * Interpret the Waypoint symbol provided by outdooractive GPX files
     * @param symbol outdooractive specific waypoint symbol
     * @return interpreted string
     */
    public String interpretWaypointSymbol(String symbol)
    {
        if (res == null) return "";
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
     * provide file information
     *
     */
    public AdditionalInfo getFileInfo() {
        AdditionalInfo info = new AdditionalInfo();

        /* provide info from GPX file */
        info.comment = app.trackName;
        info.title = app.trackName;
        info.description = "";
        info.link = "";
        SourceInfo sourceInfo = App.getSourceInfo();
        if (sourceInfo != null) {
            info.title = sourceInfo.getFileTitle();
            info.description = sourceInfo.getFileDescription();
            info.author = sourceInfo.getAuthor();
            info.link = sourceInfo.getMetaLink();
        }
        return info;
    }

    /**
     * provide Error information
     *
     */
    public AdditionalInfo getErrorInfo() {
        AdditionalInfo info = new AdditionalInfo();
        /* provide info from HTML error log*/
        info.comment     = "";
        info.title       = "Error Log";
        info.description = Log.getHTML();
        info.link        = "";
        return info;
    }

    /**
     * provide the comment and description linked to a place
     *
     * @param inPlace row index of the table
     */
    public AdditionalInfo getWaypointInfo(int inPlace) {
        AdditionalInfo info = new AdditionalInfo();
        /* place given? */
        if (inPlace >= 0) {
            /* provide place info */
            if (inPlace < recordAdapter.getCount()) {
                RecordAdapter.Record record = recordAdapter.getItem(inPlace);
                if (record == null) return null;
                DataPoint point = record.getTrackPoint();
                if (point == null) return null;

                info.title = point.getRoutePointName();
                info.comment = point.getComment();
                info.type = point.getWaypointType();
                info.symbol = point.getWaypointSymbol();
                info.description = point.getDescription();

                if (info.description.isEmpty()) {
                    if (point.getLinkIndex() >= 0) {
                        point = app.getPoint(point.getLinkIndex());
                        if (point != null) {
                            info.type = point.getWaypointType();
                            info.description = point.getDescription();
                            info.link = point.getWebLink();
                        }
                    }
                }
                if (!info.type.isEmpty()) {
                    if (info.comment.isEmpty())
                        info.comment = info.type;
                    else
                        info.comment = info.type + ": " + info.comment;
                } else if (!info.symbol.isEmpty()) {
                    /* Handle outdooractive GPX infos */
                    info.symbol = interpretWaypointSymbol(info.symbol);

                    if (!info.comment.isEmpty())
                        info.comment = info.symbol + ": " + info.comment;
                        /*
                        else if (!info.description.equals(""))
                            info.comment = info.symbol + ": " + info.description;

                         */
                    else
                        info.comment = info.symbol;
                } else
                    info.comment = "";
            }
        }
        return info;
    }

    /**
     * @param inPlace table row
     * @return the DataPoint for a selected row
     */
    public DataPoint getDataPoint(int inPlace) {
        if (recordAdapter == null) return null;
        RecordAdapter.Record record = recordAdapter.getItem(inPlace);
        if (record == null) return null;
        return record.getTrackPoint();
    }

    public String getPlannedArriveTime(int inPlace) {
        if (recordAdapter == null) return "";
        return recordAdapter.getPlannedArriveTime(inPlace);
    }


    static public class AdditionalInfo
    {
        public String title = "";
        public String comment = "";
        public String description = "";
        public String type;
        public String author = "";
        public String symbol;
        public String sourceLink = "";
        public String link = "";
    }

}
