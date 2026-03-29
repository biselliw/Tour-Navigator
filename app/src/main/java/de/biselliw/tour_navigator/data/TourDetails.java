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

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.content.Context;
import android.content.res.Resources;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.adapter.RecordAdapter;
import de.biselliw.tour_navigator.functions.search.GetOpenStreetMapFunction;
import de.biselliw.tour_navigator.functions.search.GetWaypointsFunction;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.SourceInfo;
import de.biselliw.tour_navigator.functions.search.GetWikipediaFunction;
import tim.prune.data.Distance;

public class TourDetails {

    // FIXME potential memory leak
    private final App _app;
    private final RecordAdapter _recordAdapter;
    private final Resources _res;

    public TourDetails(Context inContext, App app, RecordAdapter recordAdapter)
    {
        this._res = inContext.getResources();
        this._app = app;
        this._recordAdapter = recordAdapter;
    }

    /**
     * provide the comment and description linked to a place
     *
     * @param logErrors log system errors instead of tour information
     * @param inPlace row index of the table
     */
    public AdditionalInfo getAdditionalInfo(boolean logErrors, int inPlace) {
        if (inPlace >= 0) {
            return getWaypointInfo(inPlace);
        }
        else {
            if (logErrors) {
                return getErrorInfo();
            } else {
                return getFileInfo();
            }
        }
    }

    public int getWptCount () {
        if (_recordAdapter != null)
            return _recordAdapter.getCount();
        else
            return 0;
    }

    /**
     * Interpret the Waypoint symbol provided by outdooractive
     * @param symbol specific waypoint symbol
     * @return translated string
     */
    public String interpretWaypointSymbol(String symbol)
    {
        if (_res != null) {
            switch (symbol) {
                /* outdooractive types */
                case "waypointDirRightComb":
                    symbol = _res.getString(R.string.waypointDirRightComb);
                    break;
                case "waypointDirLeftComb":
                    symbol = _res.getString(R.string.waypointDirLeftComb);
                    break;
                case "waypointUpComb":
                    symbol = _res.getString(R.string.waypointUpComb);
                    break;
                case "waypointFlagComb":
                    symbol = _res.getString(R.string.waypointFlagComb);
                    break;
            }
        }
        return symbol;
    }

    /**
     * Interpret the Waypoint type provided by the GPX file and OSM
     * @param type specific waypoint type
     * @return translated string
     */
    private String interpretWaypointType(String type)
    {
        if (type.equals(GetWikipediaFunction.WAYPOINT_TYPE)) {
            if (_res != null)
                return _res.getString(R.string.wpt_wikipedia);
        }
        else if (type.startsWith(GetWaypointsFunction.WAYPOINT_TYPE))
            return GetWaypointsFunction.interpretWaypointType(type);
        else if (type.startsWith(GetOpenStreetMapFunction.WAYPOINT_TYPE))
            return GetOpenStreetMapFunction.interpretWaypointSymbol(type);
        return GetOpenStreetMapFunction.translateTag(type);
    }

    /**
     * provide file information
     *
     */
    public AdditionalInfo getFileInfo() {
        AdditionalInfo info = new AdditionalInfo();

        /* provide info from GPX file */
        info.comment = "";
        info.title = "";
        info.description = "";
        info.link = "";
        SourceInfo sourceInfo = App.getSourceInfo();
        if (sourceInfo != null) {
            info.title = sourceInfo.getFileTitle();
            info.description = sourceInfo.getTrackDescription();
            if (info.description.isEmpty())
                info.description = sourceInfo.getFileDescription();
            info.author = sourceInfo.getAuthor();
            info.link = sourceInfo.getMetaLink();
        }
        return info;
    }

    /**
     * @return Error information
     */
    public AdditionalInfo getErrorInfo() {
        AdditionalInfo info = new AdditionalInfo();
        /* provide info from HTML error log*/
        info.comment     = "";
        info.title       = "Error Log";
        info.description = Log.getDebugHTML();
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
            if (inPlace < _recordAdapter.getCount()) {
                RecordAdapter.Record record = _recordAdapter.getItem(inPlace);
                if (record == null) return null;
                DataPoint point = record.trackPoint;
                if (point == null) return null;

                info.title = point.getRoutePointName();
                info.comment = point.getComment();
                info.type = point.getWaypointType();
                info.symbol = point.getWaypointSymbol();
                info.description = point.getDescription();

                if (info.description.isEmpty()) {
                    if (point.getLinkIndex() >= 0) {
                        DataPoint linkedPoint = _app.getPoint(point.getLinkIndex());
                        if (linkedPoint != null) {
                            info.type = linkedPoint.getWaypointType();
                            if (linkedPoint.isProtectedWayPoint()) {
                                // translate type
                                info.type = interpretWaypointType(info.type);
                                // calculate distance between track and waypoint
                                double radians = DataPoint.calculateRadiansBetween(point,linkedPoint);
                                if (radians >= 0) {
                                    double distance_km = Distance.convertRadiansToDistance(radians);
                                    // add distance to type
                                    if (distance_km > 0.1)
                                        info.type += " (" + (int) (distance_km * 1000.0) + " m " + _res.getString(R.string.distance_from_track) + ")";
                                }
                            }
                            info.description = linkedPoint.getDescription();
                            info.link = linkedPoint.getWebLink();
                        }
                    }
                }
                if (!info.type.isEmpty()) {
                    if (info.comment.isEmpty()) {
                        info.comment = info.type;
                    }
                    else {
                        info.comment = info.type + ": " + info.comment;
                    }
                } else if (!info.symbol.isEmpty()) {
                    /* Handle outdooractive GPX infos */
                    info.symbol = interpretWaypointSymbol(info.symbol);

                    if (!info.comment.isEmpty()) {
                        info.comment = info.symbol + ": " + info.comment;
                    }
                    else {
                        info.comment = info.symbol;
                    }
                } else {
                    info.comment = "";
                }
            }
        }
        return info;
    }

    /**
     * @param inPlace table row
     * @return the DataPoint for a selected row
     */
    public DataPoint getDataPoint(int inPlace) {
        if (_recordAdapter == null) return null;
        RecordAdapter.Record record = _recordAdapter.getItem(inPlace);
        if (record == null) return null;
        return record.trackPoint;
    }

    public String getPlannedArriveTime(int inPlace) {
        if (_recordAdapter == null) return "";
        return _recordAdapter.getPlannedArriveTime(inPlace);
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
