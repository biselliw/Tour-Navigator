package de.biselliw.tour_navigator.files;

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

    You should have received a copy of the GNU General Public License. If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2025 Walter Biselli (BiselliW)
*/

import android.content.Context;
import android.content.res.Resources;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.MessageFormat;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;


public class HTML_File {

    public static StringBuffer html;

    private static Resources res;
    // todo Do not place Android context classes in static fields (static reference to `RecordAdapter` which has field `recordContext` pointing to `Context`); this is a memory leak
    private final RecordAdapter recordAdapter;
    private final TourDetails details;
    final DecimalFormat decFormat = new DecimalFormat("#0.0");

    final int COL_NR = 0;
    final int COL_ARRIVE = 1;
    final int COL_DISTANCE = 2;
    final int COL_WPT_NAME = 3;
    final int COL_HEIGHT = 4;
    final int COL_DIST = 5;
    final int COL_CLIMB = 6;
    final int COL_DESCENT = 7;
    final int COL_DURATION = 8;
    final int COL_PAUSE = 9;
    final int COL_COMMENT = 10;
    final int _colCount = COL_COMMENT + 1;

    private StringBuffer html_buffer;
    private StringBuffer desc_buffer;
    private int descItems = 0;

    TourDetails.AdditionalInfo addInfo;


    public HTML_File(Context inContext, RecordAdapter inRecordAdapter) {
        res = inContext.getResources();
        recordAdapter = inRecordAdapter;
        details = new TourDetails(inContext,App.app,inRecordAdapter);
        addInfo = details.getFileInfo();
    }

    /**
     * Copy the time table to the clipboard
     */
    public StringBuffer formatTimetableToHTML(boolean addLinks) {
        html_buffer = new StringBuffer();
        desc_buffer = new StringBuffer();
        descItems = 0;

        writeHtmlHeader();
        writeTimeTableHeader();
        writeTimeTableRows(addLinks);
        writeTimeTableFooter(addLinks);
        if (addLinks) {
            writeDescriptionHeader();
            writeDescriptionTable();
        }

        html = html_buffer;
        return html_buffer;
    }

    /*
     *  Save HTML file
     */
    public void SaveFileHTML(OutputStream _xmlStream) {
        formatTimetableToHTML(true);
        OutputStreamWriter writer;
        try {
            writer = new OutputStreamWriter(_xmlStream, StandardCharsets.UTF_8);
            // write file
            writer.write(html.toString());

            // close file
            writer.close();
            _xmlStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the HTML header
     */
    private void writeHtmlHeader() {
        addInfo = details.getFileInfo();
        String title = (addInfo == null) ? "" : addInfo.title;

        html_buffer.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n")
                .append("<head>\n")
                .append("<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />\n")
                .append("<title>")
                .append(res.getString(R.string.timetable));
        if (!title.isEmpty())
            html_buffer.append(": ").append(title);
        html_buffer.append("</title>")
                .append("<style type=\"text/css\">\n")
                .append(".text {font-size: medium;}\n")
                .append(".cell {font-size: medium;}\n")
                .append("</style>\n")
                .append("</head><body>\n");
    }

    /**
     * Write the Time Table header
     */
    private void writeTimeTableHeader() {
        html_buffer.append("<table width=\"100%\" border=\"1\" cellpadding=\"0\" cellspacing=\"2\" summary=\"\">\n");

        html_buffer.append("<caption>");
        html_buffer.append(res.getString(R.string.timetable));
        String title = (addInfo == null) ? "" : addInfo.title;
        if (!title.isEmpty())
            html_buffer.append(": ").append(title);
        html_buffer.append("</caption>");

        // write table header
        html_buffer.append("<tr>");
        for (int col = 0; col < _colCount; col++) {
            switch (col) {
                case COL_NR:
                    html_buffer.append("<th>").append(res.getString(R.string.nr)).append("</th>");
                    break;
                case COL_ARRIVE:
                    html_buffer.append("<th>").append(res.getString(R.string.time)).append("</th>");
                    break;
                case COL_DISTANCE:
                    html_buffer.append("<th>km</th>");
                    break;
                case COL_WPT_NAME:
                    html_buffer.append("<th>").append(res.getString(R.string.place)).append("</th>");
                    break;
                case COL_HEIGHT:
                    html_buffer.append("<th>").append(res.getString(R.string.altitude_m)).append("</th>");
                    break;
                case COL_DIST:
                    html_buffer.append("<th>").append(res.getString(R.string.dist_km)).append("</th>");
                    break;
                case COL_CLIMB:
                    html_buffer.append("<th>").append(res.getString(R.string.climb_m)).append("</th>");
                    break;
                case COL_DESCENT:
                    html_buffer.append("<th>").append(res.getString(R.string.descent_m)).append("</th>");
                    break;
                case COL_DURATION:
                    html_buffer.append("<th>").append(res.getString(R.string.duration)).append("</th>");
                    break;
                case COL_PAUSE:
                    html_buffer.append("<th>").append(res.getString(R.string.pause)).append("</th>");
                    break;
                case COL_COMMENT:
                    html_buffer.append("<th>").append(res.getString(R.string.comment)).append("</th>");
                    break;
            }
        }
        html_buffer.append("</tr>\n");
    }

    /**
     * Create additional infos to a route point and link them forward/back to the time table
     * @param inPlace row index of the table
     * @return true if description is available
     */
    public boolean makeDescriptionAvailable(int inPlace) {
        boolean descAvailable = false;
        if (details == null) return false;

        TourDetails.AdditionalInfo wptInfo = details.getWaypointInfo(inPlace);
        if (wptInfo == null) return false;

        StringBuilder tmp_buffer = new StringBuilder();

        // prepare table row assuming that there is anything to link to the time table
        // row "Nr":
        tmp_buffer.append("<tr><td class=\"cell\" align=\"right\"><a name=\"poi").append(descItems + 1)
                .append("\"><a href=\"#wp").append(descItems + 1).append("\">")
                .append(inPlace + 1).append("</a></a></td><td class=\"cell\" class=\"cell\">");

        // row "Type":
        String val = wptInfo.symbol;
        if (!val.isEmpty())
            tmp_buffer.append(details.interpretWaypointSymbol(val));

        // row "Description":
        tmp_buffer.append("</td><td class=\"cell\" class=\"cell\">");
        val = wptInfo.description;
        if (!val.isEmpty()) {
            tmp_buffer.append(val);
            descAvailable = true;
        }

        val = wptInfo.link;
        if (!val.isEmpty()) {
                tmp_buffer.append("<p><a href=\"")
                        .append(val ).append("\" target=_blank>").append(val).append("</a></p>\n");
                descAvailable = true;
            }

        tmp_buffer.append("</td></tr>\n");

        if (descAvailable) {
            if (descItems == 0) {
                // start with description header
                desc_buffer.append("<table width=\"100%\" border=\"1\" cellpadding=\"2\" cellspacing=\"2\" summary=\"\">\n")
                        .append("<tr><th>").append(res.getString(R.string.nr)).append("</th><th>")
                        .append(res.getString(R.string.wpt_type)).append("</th><th>").append(res.getString(R.string.wpt_desc))
                        .append("</th></tr>\n");
            }
            // add new row
            desc_buffer.append(tmp_buffer);
            descItems++;
        }
        return descAvailable;
    }


    /**
     * write Time Table rows
     */
    private void writeTimeTableRows(boolean addLinks) {
        // write table rows
        if (details == null) return;

        for (int row = 0; row < details.getWptCount(); row++) {
            TourDetails.AdditionalInfo wptInfo = details.getWaypointInfo(row);
            if (wptInfo != null) {
                RecordAdapter.Record record = recordAdapter.getItem(row);
                DataPoint recPoint = details.getDataPoint(row);

                html_buffer.append("<tr>");
                for (int col = 0; col < _colCount; col++) {
                    /* text alignment */
                    switch (col) {
                        case COL_WPT_NAME:
                        case COL_COMMENT:
                            html_buffer.append("<td class=\"cell\">");
                            break;
                        default:
                            html_buffer.append("<td class=\"cell\" align=\"right\">");
                            break;
                    }
                    switch (col) {
                        case COL_NR:
                            html_buffer.append(row + 1);
                            break;
                        case COL_ARRIVE:
                            html_buffer.append(details.getPlannedArriveTime(row));
                            break;
                        case COL_DISTANCE:
                            html_buffer.append(decFormat.format(recPoint.getDistance()));
                            break;
                        case COL_WPT_NAME:
                            // is extended description available?
                            if (makeDescriptionAvailable(row) && addLinks) {
                                html_buffer.append("<a name=\"wp").append(descItems)
                                        .append("\"><a href=\"#poi").append(descItems).append("\">")
                                        .append(recPoint.getRoutePointName()).append("</a></a>");
                            } else
                                html_buffer.append(recPoint.getRoutePointName());
                            break;
                        case COL_HEIGHT:
                            html_buffer.append((int) recPoint.getAltitude().getValue());
                            break;
                        case COL_DIST:
                            if (row > 0) {
                                final DecimalFormat formatter = new DecimalFormat("  #0.0");
                                html_buffer.append(formatter.format(record.Sdistance));
                            }
                            break;
                        case COL_CLIMB:
                            if (row > 0)
                                html_buffer.append((int) record.Sclimb);
                            break;
                        case COL_DESCENT:
                            if (row > 0)
                                html_buffer.append((int) record.Sdescent);
                            break;
                        case COL_DURATION:
                            if (row > 0)
                                html_buffer.append(formatIntToTime((int) record.Sseconds / 60));
                            break;
                        case COL_PAUSE: {
                            int dur = recPoint.getWaypointDuration();
                            if (dur > 0)
                                html_buffer.append(formatIntToTime(dur));
                            break;
                        }
                        case COL_COMMENT:
                            html_buffer.append(recPoint.getComment());
                            break;
                        default:
                            break;
                    }
                    html_buffer.append("</td>");
                }
            }
            html_buffer.append("</tr>\n");
        }
    }

    /**
     * write Time table footer
     */
    private void writeTimeTableFooter(boolean addLinks) {
        html_buffer.append("<tr>");
        for (int col = 0; col < _colCount; col++) {
            /* text alignment */
            switch (col) {
                case COL_WPT_NAME:
                case COL_COMMENT:
                    html_buffer.append("<td class=\"cell\" class=\"cell\"><b>");
                    break;
                default:
                    html_buffer.append("<td class=\"cell\" align=\"right\"><b>");
                    break;
            }
            switch (col) {
                case COL_WPT_NAME:
                    html_buffer.append(res.getString(R.string.summary));
                    break;
                case COL_CLIMB:
                    html_buffer.append((int) App.getClimb());
                    break;
                case COL_DESCENT:
                    html_buffer.append((int) App.getDescent());
                    break;
                case COL_DURATION:
                    html_buffer.append(formatIntToTime((int) (App.getTotalSeconds() / 60L
                    - App.getTotalPauseInMins())));
                    break;
                case COL_PAUSE:
                    html_buffer.append(formatIntToTime((int) App.getTotalPauseInMins()));
                    break;
                default:
                    html_buffer.append("&nbsp;");
            }
            html_buffer.append("</b></td>");
        }
        html_buffer.append("</tr>\n");

        html_buffer.append("</table>");

        html_buffer.append("<caption>");
        html_buffer.append(res.getString(R.string.app_donate));
        html_buffer.append("</caption>");

        if (!addLinks)
            html_buffer.append("</body></html>");
    }

    String formatIntToTime(int inMinutes) {
        // separate minutes and hours
        int minute = inMinutes % 60;
        int hour = (inMinutes / 60) % 60;

        // create output format
        Object[] arguments = {
                hour,
                minute
        };

        // First format the cell value as required
        return MessageFormat.format(
                "  {0,number,00}:{1,number,00}",
                arguments);
    }


    /**
     * write Description header
     */
    private void writeDescriptionHeader() {
        html_buffer.append("<p><b>").append(res.getString(R.string.tour_add_info)).append("</b></p>");

        html_buffer.append("<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"2\" summary=\"\">\n");

        String author = "", link = "", time = "", desc = "";

        if (addInfo != null) {
            author = addInfo.author;
            link = addInfo.link;
            // time = sourceInfo.getMetaTime();
            desc = addInfo.description;
        }
        if (!author.isEmpty()) {
            html_buffer.append("<tr><td class=\"cell\" width=\"20%\" >").append(res.getString(R.string.tour_author)).append(":</td><td class=\"cell\" >")
                    .append(addInfo.author).append("</td></tr>\n");
        }

        if (!link.isEmpty()) {
            html_buffer.append("<tr><td class=\"cell\" class=\"cell\">").append(res.getString(R.string.tour_link)).append(":</td><td class=\"cell\" class=\"cell\"><a href=\"")
                    .append(link).append("\" target=_blank>").append(link).append("</a></td></tr>\n");
        }

        if (!time.isEmpty()) {
            html_buffer.append("<tr><td class=\"cell\" class=\"cell\">").append(res.getString(R.string.tour_updated)).append(":</td><td class=\"cell\" class=\"cell\">")
                    .append(time).append("</td></tr>\n");
        }

        html_buffer.append("</table>\n");

        if (!desc.isEmpty()) {
            html_buffer.append("<p>").append(desc).append("</p>\n")
                    .append("<p> </p>\n");
        }
    }

    /**
     * write Description table if any
     */
    private void writeDescriptionTable() {
        if (descItems > 0) {
            html_buffer.append(desc_buffer)
                    .append("</table>\n");
        }

    }
}