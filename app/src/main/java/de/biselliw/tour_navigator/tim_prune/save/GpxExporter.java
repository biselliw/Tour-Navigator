package de.biselliw.tour_navigator.tim_prune.save;
/*
    This file is part of GpsPrune and adapted to the needs of Tour Navigator

	GpsPrune is a map-based application for viewing, editing and converting coordinate data from GPS systems.

	It's a cross-platform java application, and its home page is at
	<a href="https://activityworkshop.net/software/gpsprune/">activityworkshop.net</a>

	On github you'll find all the sources from version 1 to the current version 22.2:

	<a href="https://github.com/activityworkshop/GpsPrune">github.com/activityworkshop/GpsPrune</a>

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program. If not, see <http://www.gnu.org/licenses/>.

	Copyright (C) 2022 activityworkshop.net

 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim.prune.data.Coordinate;
import de.biselliw.tour_navigator.tim.prune.data.UnitSetLibrary;
import de.biselliw.tour_navigator.tim.prune.save.xml.XmlUtils;
import de.biselliw.tour_navigator.tim_prune.data.SourceInfo;
import de.biselliw.tour_navigator.tim_prune.data.TrackInfo;

import static de.biselliw.tour_navigator.tim_prune.I18nManager.APP_NAME;

/**
 * Class to export track information
 * into a specified Gpx file
 *
 * @implNote BiselliW: added extra GPX tags (name, sym, cmt, desc, extension:pause), reduced functionality (dialogs, GpxCachers)
 */
public class GpxExporter {
	private static App _app;
	private static TrackInfo _trackInfo = null;

	/** this program name */
	private static final String GPX_CREATOR = APP_NAME;

	/**
	 * Constructor
	 * @param inApp app object
	 */
	public GpxExporter(App inApp)
	{
		_app = inApp;
		_trackInfo = inApp.getTrackInfo();
	}

	/**
	 * Download the information to the given writer
	 * @param  writer streaming object
     * @author BiselliW
	 */
	public static void downloadData(OutputStreamWriter writer) throws IOException {

		_trackInfo = _app.getTrackInfo();
		if (_trackInfo != null) {

			try {
				exportData(writer, _trackInfo);

				// close file
				writer.close();
			} catch (IOException ioe) {
				writer.close();
			}
		}
	}

	/**
	 * Export the information to the given writer
	 * @param inWriter writer object
	 * @param inInfo track info object
	 * @return number of points written
	 * @throws IOException if io errors occur on write
	 * @implNote BiselliW: reduced functionality (GpxCachers, fixed selections of exported data items, no media), add track description
	 */
	public static int exportData(OutputStreamWriter inWriter, TrackInfo inInfo) throws IOException
	{
		// Write or copy headers
		inWriter.write(getXmlHeaderString(inWriter));
		final String gpxHeader = getGpxHeaderString();
		inWriter.write(gpxHeader);
		// name and description
		SourceInfo sourceinfo = _trackInfo.getFileInfo().getSource(0);

		String inName = sourceinfo.getMetaName();
		String trackName = (inName != null && !inName.equals("")) ? XmlUtils.fixCdata(inName) : GPX_CREATOR + "Track";
		String inDesc = sourceinfo.getMetaDescription();
		String trackTitle = (inDesc != null && !inDesc.equals("")) ? XmlUtils.fixCdata(inDesc) : "Export from " + GPX_CREATOR;
		writeNameAndDescription(inWriter, trackName, trackTitle, sourceinfo.getAuthor(), sourceinfo.getMetaTime(), sourceinfo.getMetaLink());

		DataPoint point;
		// Loop over waypoints
		final int numPoints = inInfo.getTrack().getNumPoints();
		int numSaved = 0;
		for (int i=0; i<numPoints; i++)
		{
			point = inInfo.getTrack().getPoint(i);
			// Make a wpt element for each waypoint
			if (point.isWayPoint())
			{
				exportWaypoint(point, inWriter);
				numSaved++;
			}
		}

		// Export both route points and then track points
		// Output all route points (if any)
		numSaved += writeTrackPoints(inWriter, inInfo,
			true, "\t<rte><number>1</number>\n",
			null, "\t</rte>\n");

		// Output all track points, if any
		String trackStart = "\t<trk>\n\t\t<name>" + trackName + "</name>\n\t\t<trkseg>\n";
		inDesc = sourceinfo.getTrackDescription();
		String trackDesc = (inDesc != null && !inDesc.equals("")) ? XmlUtils.fixCdata(inDesc) : "";
		if (!trackDesc.equals(""))
			trackStart += "\t\t<desc>" + trackDesc + "\n\t\t</desc>\n";
		numSaved += writeTrackPoints(inWriter, inInfo,
			false, trackStart,
			"\t</trkseg>\n\t<trkseg>\n", "\t\t</trkseg>\n\t</trk>\n");


		inWriter.write("</gpx>\n");
		return numSaved;
	}


	/**
	 * Write the name, description and time according to the GPX version number
	 * @param inWriter writer object
	 * @param inName name, or null if none supplied
	 * @param inDesc description, or null if none supplied
	 * @param inAuthor author, or null if none supplied
	 * @param inTime time, or null if none supplied
	 * @param inLink link, or null if none supplied
	 * @implNote BiselliW: add extra GPX meta tags author, time, link; GPX format 1.1 only
	 */
	private static void writeNameAndDescription(OutputStreamWriter inWriter, String inName,
			String inDesc, String inAuthor, String inTime, String inLink) throws IOException
	{
		// Position of name and description fields needs to be different for GPX1.0 and GPX1.1
		// GPX 1.1 has the name and description inside a metadata tag
		inWriter.write("\t<metadata>\n");
		if (inName != null && !inName.equals(""))
		{
			inWriter.write("\t\t<name>");
			inWriter.write(inName);
			inWriter.write("</name>\n");
		}
		if (inDesc != null && !inDesc.equals(""))
		{
			inWriter.write("\t\t<desc>");
			inWriter.write(inDesc);
			inWriter.write("</desc>\n");
		}
		if (inAuthor != null && !inAuthor.equals(""))
		{
			inWriter.write("\t\t<author>\n");
			inWriter.write("\t\t\t<name>");
			inWriter.write(inAuthor);
			inWriter.write("</name>\n");
			inWriter.write("\t\t</author>\n");
		}
		if (inTime != null && !inTime.equals("")) {
			inWriter.write('\t');
			inWriter.write("\t<time>");
			inWriter.write(inTime);
			inWriter.write("</time>\n");
		}

		if (inLink != null && !inLink.equals("")) {
			inWriter.write('\t');
			inWriter.write("\t<link href=\"");
			inWriter.write(inLink);
			inWriter.write("\"/>\n");
		}

		inWriter.write("\t</metadata>\n");
	}

	/**
	 * Loop through the track outputting the relevant track points
	 * @param inWriter writer object for output
	 * @param inInfo track info object containing track
	 * @param inOnlyCopies true to only export if source can be copied
	 * @param inStartTag start tag to output
	 * @param inSegmentTag tag to output between segments (or null)
	 * @param inEndTag end tag to output
	 * @implNote BiselliW: use fix export settings, no Gpx cachers, no media
	 */
	private static int writeTrackPoints(OutputStreamWriter inWriter,
										TrackInfo inInfo,
										boolean inOnlyCopies,
										String inStartTag, String inSegmentTag, String inEndTag)
	throws IOException
	{
		// Note: Too many input parameters to this method but avoids duplication
		// of output functionality for writing track points and route points
		int numPoints = inInfo.getTrack().getNumPoints();
		int numSaved = 0;
		// Loop over track points
		for (int i=0; i<numPoints; i++)
		{
			DataPoint point = inInfo.getTrack().getPoint(i);
			// get the source from the point (if any)
			if (!inOnlyCopies && !point.isWayPoint())
			{
				// restart track segment if necessary
				if ((numSaved > 0) && point.getSegmentStart() && (inSegmentTag != null)) {
					inWriter.write(inSegmentTag);
				}
				if (numSaved == 0) {inWriter.write(inStartTag);}
				exportTrackpoint(point, inWriter);
				numSaved++;
			}
		}

		if (numSaved > 0) {
			inWriter.write(inEndTag);
		}
		return numSaved;
	}


	/**
	 * Get the header string for the xml document including encoding
	 * @param inWriter writer object
	 * @return header string defining encoding
	 */
	private static String getXmlHeaderString(OutputStreamWriter inWriter)
	{
		return "<?xml version=\"1.0\" encoding=\"" + XmlUtils.getEncoding(inWriter) + "\"?>\n";
	}


	/**
	 * Get the header string for the gpx tag
	 * @return header string as default
	 * @implNote BiselliW: use GPX version 1.1 only
	 */
	private static String getGpxHeaderString()
	{
		// Create default (1.1) header
		String gpxHeader = "<gpx version=\"1.1\" creator=\"" + GPX_CREATOR
				+ "\"\n xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ " xmlns=\"http://www.topografix.com/GPX/1/1\""
				+ " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n";

		return gpxHeader + "\n";
	}


	/**
	 * Export the specified waypoint into the file
	 * @param inPoint waypoint to export
	 * @param inWriter writer object
	 * @throws IOException on write failure
	 * @implNote BiselliW: use fix export settings (waypoints, trackpoints), no Gpx cachers, no media; add GPX tage type;
	 * mark some waypoint to not load with GpsPrune but is required for e.g. Locus Map 
	 */
	private static void exportWaypoint(DataPoint inPoint, Writer inWriter)
		throws IOException
	{
		inWriter.write("\n\t<wpt lat=\"");
		inWriter.write(inPoint.getLatitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		inWriter.write("\" lon=\"");
		inWriter.write(inPoint.getLongitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		inWriter.write("\">\n");
		// altitude if available
		if (inPoint.hasAltitude())
		{
			inWriter.write("\t\t<ele>");
			inWriter.write(inPoint.hasAltitude() ? inPoint.getAltitude().getStringValue(UnitSetLibrary.UNITS_METRES) : "0");
			inWriter.write("</ele>\n");
		}
		// write waypoint name after elevation and time
		if (!inPoint.getWaypointName().equals(""))
		{
			inWriter.write("\t\t<name>");
			inWriter.write(XmlUtils.fixCdata(inPoint.getWaypointName().trim()));
			inWriter.write("</name>\n");
		}
		// description, if any
		final String desc = XmlUtils.fixCdata(inPoint.getFieldValue(Field.DESCRIPTION));
		if (!desc.equals(""))
		{
			inWriter.write("\t\t<desc>");
			inWriter.write(desc);
			inWriter.write("</desc>\n");
		}
		// comment, if any
		final String comment = XmlUtils.fixCdata(inPoint.getFieldValue(Field.COMMENT));
		if (!comment.equals(""))
		{
			inWriter.write("\t\t<cmt>");
			inWriter.write(comment);
			inWriter.write("</cmt>\n");
		}
		// write waypoint type if any
		// mark waypoint to not load with GpsPrune but is required for e.g. Locus Map 
		String type;
		if (inPoint.isRoutePoint())
			type = "TrackPt";
		else
			type = inPoint.getFieldValue(Field.WAYPT_TYPE);
		if (type != null)
		{
			type = type.trim();
			if (!type.equals(""))
			{
				inWriter.write("\t\t<type>");
				inWriter.write(type);
				inWriter.write("</type>\n");
			}
		}
		// write link, if any
		final String link = XmlUtils.fixCdata(inPoint.getFieldValue(Field.WAYPT_LINK));
		if (!link.equals(""))
		{
			inWriter.write("\t\t<link href=\"");
			inWriter.write(link);
			inWriter.write("\"/>\n");
		}
		inWriter.write("\t</wpt>\n");
	}


	/**
	 * Export the specified trackpoint into the file
	 * @param inPoint trackpoint to export
	 * @param inWriter writer object
	 * @implNote BiselliW: always export trackpoints, GPX tags name, sym, cmt and desc, extension:pause
	 */
	private static void exportTrackpoint(DataPoint inPoint, Writer inWriter)
		throws IOException
	{
		inWriter.write("      <trkpt lat=\"");
		inWriter.write(inPoint.getLatitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		inWriter.write("\" lon=\"");
		inWriter.write(inPoint.getLongitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		inWriter.write("\">\n");
		// altitude
		if (inPoint.hasAltitude())
		{
			inWriter.write("        <ele>");
			inWriter.write(inPoint.hasAltitude() ? inPoint.getAltitude().getStringValue(UnitSetLibrary.UNITS_METRES) : "0");
			inWriter.write("</ele>\n");
		}

		// write waypoint name after elevation and time
		String name = inPoint.getWaypointName().trim();
		if (!name.equals(""))
		{
			inWriter.write("        <name>");
			inWriter.write(XmlUtils.fixCdata(name));
			inWriter.write("</name>\n");
		}
		// description, if any
		final String desc = XmlUtils.fixCdata(inPoint.getFieldValue(Field.DESCRIPTION));
		if (!desc.equals(""))
		{
			inWriter.write("        <desc>");			
			inWriter.write(desc);
			inWriter.write("</desc>\n");
		}
		// comment, if any
		final String comment = XmlUtils.fixCdata(inPoint.getFieldValue(Field.COMMENT));
		if (!comment.equals(""))
		{
			inWriter.write("        <cmt>");
			inWriter.write(comment);
			inWriter.write("</cmt>\n");
		}

		// symbol if any
		final String symbol = inPoint.getFieldValue(Field.WAYPT_SYM);
		if (symbol != null && !symbol.equals(""))
		{
			inWriter.write("        <sym>");
			inWriter.write(symbol);
			inWriter.write("</sym>\n");
		}
		// time if any
		final String time = inPoint.getFieldValue(Field.TIMESTAMP);
		if (time != null && !time.equals(""))
		{
			inWriter.write("        <time>");
			inWriter.write(time);
			inWriter.write("</time>\n");
		}
		// duration if any
		if (inPoint.getWaypointDuration() > 0) {
			inWriter.write("        <extensions>\n");
			inWriter.write("          <pause>" + inPoint.getWaypointDuration() + "</pause>\n");
			inWriter.write("        </extensions>\n");
		}
		inWriter.write("      </trkpt>\n");
	}


}
