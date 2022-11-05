package de.biselliw.tour_navigator.files;

import android.content.Context;
import android.content.res.Resources;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.MessageFormat;

import tim.prune.data.DataPoint;
import tim.prune.data.SourceInfo;

public class HTML_File  {

    private static Resources res;
    private static RecordAdapter recordAdapter;
    final DecimalFormat decFormat = new DecimalFormat("  #0.0");


    public HTML_File(Context inContext, RecordAdapter inRecordAdapter) {
        res = inContext.getResources();
        recordAdapter = inRecordAdapter;
    }

    /**
     * Copy the time table to the clipboard
     */
    public StringBuffer getTimetableAsHTML() {
        final int COL_NR                    = 0;
        final int COL_ARRIVE                = 1;
        final int COL_DISTANCE              = 2;
        final int COL_WPT_NAME              = 3;
        final int COL_HEIGHT                = 4;
        final int COL_DIST		            = 5;
        final int COL_CLIMB 		        = 6;
        final int COL_DESCENT		        = 7;
        final int COL_DURATION		        = 8;
        final int COL_PAUSE                 = 9;
        final int COL_COMMENT               = 10;
        final int _colCount = COL_COMMENT + 1;

        int col, row;
        String author = res.getString(R.string.about_author_names);
        String description="";

        StringBuffer html_buffer = new StringBuffer();

        SourceInfo sourceInfo = App.getSourceInfo();
        if (sourceInfo != null) {
            author = sourceInfo.getAuthor();
            description = sourceInfo.getMetaName();
            if (description.equals(""))
                description = sourceInfo.getFileTitle();
        }

        html_buffer.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
        html_buffer.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        html_buffer.append("<head>\n");
        html_buffer.append("<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />\n");
        html_buffer.append("<title>");
        html_buffer.append(res.getString(R.string.timetable));
        if (!description.equals(""))
            html_buffer.append(": ").append(description);
        html_buffer.append("</title>");

        html_buffer.append("</head><body><table width=\"100%\" border=\"1\" cellpadding=\"0\" cellspacing=\"2\" summary=\"\">\n");

        html_buffer.append("<caption>");
        html_buffer.append(res.getString(R.string.timetable));
        if (!description.equals(""))
            html_buffer.append(": ").append(description);
        html_buffer.append("</caption>");

        // write table header
        html_buffer.append("<tr>");
        for (col = 0; col < _colCount; col++) {
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

        // write table rows
        int rowCount = recordAdapter.getCount();
        for (row = 0; row < rowCount; row++) {
            html_buffer.append("<tr>");
            RecordAdapter.Record record = recordAdapter.getItem(row);
            if (record == null) break;
            DataPoint recPoint = record.getTrackPoint();
            if (recPoint != null) {
                for (col = 0; col < _colCount; col++) {
                    /* text alignment */
                    switch (col) {
                        case COL_WPT_NAME:
                        case COL_COMMENT:
                            html_buffer.append("<td>");
                            break;
                        default:
                            html_buffer.append("<td align=\"right\">");
                            break;
                    }
                    switch (col) {
                        case COL_NR:
                            html_buffer.append(row + 1);
                            break;
                        case COL_ARRIVE:
                            html_buffer.append(recordAdapter.getPlannedArriveTime(recPoint));
                            break;
                        case COL_DISTANCE:
                            html_buffer.append(decFormat.format(recPoint.getDistance()));
                            break;
                        case COL_WPT_NAME:
                            html_buffer.append(recPoint.getRoutePointName());
                            break;
                        case COL_HEIGHT:
                            html_buffer.append(recPoint.getAltitude().getValue());
                            break;
                        case COL_DIST:
                            final DecimalFormat formatter = new DecimalFormat( "  #0.0" );
                            html_buffer.append(formatter.format(record.Sdistance));
                            break;
                        case COL_CLIMB :
                            html_buffer.append(record.Sclimb);
                            break;
                        case COL_DESCENT :
                            html_buffer.append(record.Sdescent);
                            break;
                        case COL_DURATION:
                            html_buffer.append(formatIntToTime((int)record.Sseconds / 60));
                            break;
                        case COL_PAUSE: {
                            int dur = (int)recPoint.getWaypointDuration();
                            if (dur > 0)
                                html_buffer.append(formatIntToTime(dur));
                            break;
                        }
                        case COL_COMMENT:
                            html_buffer.append(recPoint.getWaypointComment());
//                            html_buffer.append(App.getTrack().getWaypointCommentOrDescription(recPoint));
                            break;
                        default:
                            break;
                    }
                    html_buffer.append("</td>");
                }
            }
            html_buffer.append("</tr>\n");
        }

        // write table footer
        html_buffer.append("<tr>");
        for (col = 0; col < _colCount; col++) {
            /* text alignment */
            switch (col) {
                case COL_WPT_NAME:
                case COL_COMMENT:
                    html_buffer.append("<td><b>");
                    break;
                default:
                    html_buffer.append("<td align=\"right\"><b>");
                    break;
            }
            switch (col) {
                case COL_NR:
                case COL_ARRIVE:
                case COL_DISTANCE:
                case COL_DIST:
                case COL_HEIGHT:
                case COL_COMMENT:
                default:
                    html_buffer.append("&nbsp");
                    break;
                case COL_WPT_NAME:
                    html_buffer.append(res.getString(R.string.summary));
                    break;
                case COL_CLIMB:
                    html_buffer.append(App.getClimb());
                    break;
                case COL_DESCENT:
                    html_buffer.append(App.getDescent());
                    break;
                case COL_DURATION:
                    html_buffer.append(formatIntToTime((int)App.getTotalCalcSeconds() / 60));
                    break;
                case COL_PAUSE:
                    html_buffer.append(formatIntToTime((int)App.getTotalPauseSeconds() / 60));
                    break;
            }
            html_buffer.append("</b></td>");
        }
        html_buffer.append("</tr>\n");

        html_buffer.append("</table>");

        html_buffer.append("<caption>");
        html_buffer.append(res.getString(R.string.app_donate));
        html_buffer.append("</caption>");

        html_buffer.append("</body></html>");
        return html_buffer;
    }

    String formatIntToTime (int inMinutes)
    {
        // separate minutes and hours
        int minute = inMinutes % 60;
        int hour = (inMinutes / 60) % 60;

        // create output format
        Object[] arguments = {
                new Integer(hour),
                new Integer(minute)
        };

        // First format the cell value as required
        String duration = MessageFormat.format(
                "  {0,number,00}:{1,number,00}",
                arguments);

        return duration;
    }

    /*
     *  Save HTML file
     */
    public void SaveFileHTML(OutputStream _xmlStream) {
        StringBuffer html = getTimetableAsHTML();
        OutputStreamWriter writer;
        try {
            writer = new OutputStreamWriter(_xmlStream, "UTF-8");
            // write file
            writer.write(html.toString());

            // close file
            writer.close();
            _xmlStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
