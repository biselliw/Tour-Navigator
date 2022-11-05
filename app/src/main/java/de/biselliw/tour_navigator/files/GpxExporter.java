package de.biselliw.tour_navigator.files;
/* @since WB */

import de.biselliw.tour_navigator.App;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import tim.prune.data.Coordinate;
import tim.prune.data.DataPoint;
import tim.prune.data.Field;
import tim.prune.data.MediaObject;
import tim.prune.data.SourceInfo;
import tim.prune.data.Timestamp;
import tim.prune.data.TrackInfo;
import tim.prune.data.UnitSetLibrary;
import tim.prune.save.SettingsForExport;
import tim.prune.save.xml.GpxCacherList;
import tim.prune.save.xml.XmlUtils;

/**
 * Class to export track information
 * into a specified Gpx file
 */
public class GpxExporter 
// todo extends GenericFunction implements Runnable
{
	private static App _app;
	private static TrackInfo _trackInfo = null;
	/** Remember the previous sourceInfo object to tell whether it has changed */
	private SourceInfo _previousSourceInfo = null;

	/** todo this program name */
	private static final String GPX_CREATOR = "Wanderzeitplanung";


	/**
	 * Constructor
	 * @param inApp app object
	 */
	public GpxExporter(App inApp)
	{
// todo		super(inApp);
		_app = inApp;
		_trackInfo = inApp.getTrackInfo();
	}

	/**
	 * Download the information to the given writer
	 * @param  inStream streaming object
	 */
	public static void downloadData(OutputStream inStream) throws IOException {
		OutputStreamWriter writer = null;

		// Instantiate source file cachers in case we want to copy output
		GpxCacherList gpxCachers = null;
		_trackInfo = _app.getTrackInfo();
		if (_trackInfo != null) {
			gpxCachers = new GpxCacherList(_trackInfo.getFileInfo());

			try {
				// normal writing to file - specify UTF8 encoding
				writer = new OutputStreamWriter(inStream, StandardCharsets.UTF_8);
				SettingsForExport settings = new SettingsForExport();
				settings.setExportTrackPoints(true);
				settings.setExportWaypoints(true);
				settings.setExportPhotoPoints(false);
				settings.setExportAudiopoints(false);
				settings.setExportJustSelection(false);
				settings.setExportTimestamps(true);
				// write file
				SourceInfo sourceinfo = _trackInfo.getFileInfo().getSource(0);
				final int numPoints = exportData(writer, _trackInfo, sourceinfo.getMetaName(), sourceinfo.getMetaDescription(),
						settings, gpxCachers);

				// todo close stream ?
				inStream.close();;
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
	 * @param inName name of track (optional)
	 * @param inDesc description of track (optional)
	 * @param inSettings flags for what to export and how
	 * @param inGpxCachers list of Gpx cachers containing input data
	 * @return number of points written
	 * @throws IOException if io errors occur on write
	 */
	public static int exportData(OutputStreamWriter inWriter, TrackInfo inInfo, String inName,
		String inDesc, SettingsForExport inSettings, GpxCacherList inGpxCachers) throws IOException
	{
		// Write or copy headers
		inWriter.write(getXmlHeaderString(inWriter));
		final String gpxHeader = getGpxHeaderString(inGpxCachers);
		final boolean isVersion1_1 = (gpxHeader.toUpperCase().indexOf("GPX/1/1") > 0);
		inWriter.write(gpxHeader);
		// name and description
		String trackName = (inName != null && !inName.equals("")) ? XmlUtils.fixCdata(inName) : "GpsPruneTrack";
		String desc      = (inDesc != null && !inDesc.equals("")) ? XmlUtils.fixCdata(inDesc) : "Export from GpsPrune";
		writeNameAndDescription(inWriter, trackName, desc, isVersion1_1);

		DataPoint point = null;
		final boolean exportWaypoints = inSettings.getExportWaypoints();
		final boolean exportSelection = inSettings.getExportJustSelection();
		final boolean exportTimestamps = inSettings.getExportTimestamps();
		// Examine selection
		int selStart = -1, selEnd = -1;
		if (exportSelection) {
			selStart = inInfo.getSelection().getStart();
			selEnd = inInfo.getSelection().getEnd();
		}
		// Loop over waypoints
		final int numPoints = inInfo.getTrack().getNumPoints();
		int numSaved = 0;
		for (int i=0; i<numPoints; i++)
		{
			point = inInfo.getTrack().getPoint(i);
			if (!exportSelection || (i>=selStart && i<=selEnd))
			{
				// Make a wpt element for each waypoint
				if (point.isWaypoint() && exportWaypoints)
				{
					String pointSource = (inGpxCachers == null? null : getPointSource(inGpxCachers, point));
					
					// @since WB
					if (point.isRoutePoint())
					{
						exportWaypoint(point, inWriter, inSettings);
					}
					else if ((pointSource != null) && (point.isWaypoint()) )
					{
						// If timestamp checkbox is off, strip time
						if (!exportTimestamps) {
							pointSource = stripTime(pointSource);
						}
						inWriter.write('\n');
						inWriter.write(pointSource);
					}
					else {
						exportWaypoint(point, inWriter, inSettings);
					}
					numSaved++;
				}
			}
		}
		// Export both route points and then track points
		if (inSettings.getExportTrackPoints() || inSettings.getExportPhotoPoints() || inSettings.getExportAudioPoints())
		{
			// Output all route points (if any)
			numSaved += writeTrackPoints(inWriter, inInfo, inSettings,
				true, inGpxCachers, "<rtept", "\t<rte><number>1</number>\n",
				null, "\t</rte>\n");
			// Output all track points, if any
			String trackStart = "\t<trk>\n\t\t<name>" + trackName + "</name>\n\t\t<number>1</number>\n\t\t<trkseg>\n";
			numSaved += writeTrackPoints(inWriter, inInfo, inSettings,
				false, inGpxCachers, "<trkpt", trackStart,
				"\t</trkseg>\n\t<trkseg>\n", "\t\t</trkseg>\n\t</trk>\n");
		}

		inWriter.write("</gpx>\n");
		return numSaved;
	}


	/**
	 * Write the name and description according to the GPX version number
	 * @param inWriter writer object
	 * @param inName name, or null if none supplied
	 * @param inDesc description, or null if none supplied
	 * @param inIsVersion1_1 true if gpx version 1.1, false for version 1.0
	 */
	private static void writeNameAndDescription(OutputStreamWriter inWriter,
		String inName, String inDesc, boolean inIsVersion1_1) throws IOException
	{
		// Position of name and description fields needs to be different for GPX1.0 and GPX1.1
		if (inIsVersion1_1)
		{
			// GPX 1.1 has the name and description inside a metadata tag
			inWriter.write("\t<metadata>\n");
		}
		if (inName != null && !inName.equals(""))
		{
			if (inIsVersion1_1) {inWriter.write('\t');}
			inWriter.write("\t<name>");
			inWriter.write(inName);
			inWriter.write("</name>\n");
		}
		if (inIsVersion1_1) {inWriter.write('\t');}
		inWriter.write("\t<desc>");
		inWriter.write(inDesc);
		inWriter.write("</desc>\n");
		if (inIsVersion1_1)
		{
			inWriter.write("\t</metadata>\n");
		}
	}

	/**
	 * Loop through the track outputting the relevant track points
	 * @param inWriter writer object for output
	 * @param inInfo track info object containing track
	 * @param inSettings export settings defining what should be exported
	 * @param inOnlyCopies true to only export if source can be copied
	 * @param inCachers list of GpxCachers
	 * @param inPointTag tag to match for each point
	 * @param inStartTag start tag to output
	 * @param inSegmentTag tag to output between segments (or null)
	 * @param inEndTag end tag to output
	 */
	private static int writeTrackPoints(OutputStreamWriter inWriter,
		TrackInfo inInfo, SettingsForExport inSettings,
		boolean inOnlyCopies, GpxCacherList inCachers, String inPointTag,
		String inStartTag, String inSegmentTag, String inEndTag)
	throws IOException
	{
		// Note: Too many input parameters to this method but avoids duplication
		// of output functionality for writing track points and route points
		int numPoints = inInfo.getTrack().getNumPoints();
		int selStart = inInfo.getSelection().getStart();
		int selEnd = inInfo.getSelection().getEnd();
		int numSaved = 0;
		final boolean exportSelection = inSettings.getExportJustSelection();
		final boolean exportTrackPoints = inSettings.getExportTrackPoints();
		final boolean exportPhotos = inSettings.getExportPhotoPoints();
		final boolean exportAudios = inSettings.getExportAudioPoints();
		final boolean exportTimestamps = inSettings.getExportTimestamps();
		// Loop over track points
		for (int i=0; i<numPoints; i++)
		{
			DataPoint point = inInfo.getTrack().getPoint(i);
			if ((!exportSelection || (i>=selStart && i<=selEnd)) && !point.isWayPoint())
//				if ((!exportSelection || (i>=selStart && i<=selEnd)) && !point.isWaypoint())
			{
				if ((point.getPhoto()==null && exportTrackPoints) || (point.getPhoto()!=null && exportPhotos)
					|| (point.getAudio()!=null && exportAudios))
				{
					// get the source from the point (if any)
					String pointSource = getPointSource(inCachers, point);
					// Clear point source if it's the wrong type of point (eg changed from waypoint or route point)
					if (pointSource != null && !pointSource.trim().toLowerCase().startsWith(inPointTag)) {
						pointSource = null;
					}
					if (pointSource != null || !inOnlyCopies)
					{
						// restart track segment if necessary
						if ((numSaved > 0) && point.getSegmentStart() && (inSegmentTag != null)) {
							inWriter.write(inSegmentTag);
						}
						if (numSaved == 0) {inWriter.write(inStartTag);}
						if (pointSource != null)
						{
							// If timestamps checkbox is off, strip the time
							if (!exportTimestamps) {
								pointSource = stripTime(pointSource);
							}
							inWriter.write(pointSource);
							inWriter.write('\n');
						}
						else if (!inOnlyCopies) {
							exportTrackpoint(point, inWriter, inSettings);
						}
						numSaved++;
					}
				}
			}
		}
		if (numSaved > 0) {
			inWriter.write(inEndTag);
		}
		return numSaved;
	}


	/**
	 * Get the point source for the specified point
	 * @param inCachers list of GPX cachers to ask for source
	 * @param inPoint point object
	 * @return xml source if available, or null otherwise
	 */
	private static String getPointSource(GpxCacherList inCachers, DataPoint inPoint)
	{
		if (inCachers == null || inPoint == null) {return null;}
		/* @since WB */
		if (inPoint.getWaypointDuration() > 0) {return null;}
		String source = inCachers.getSourceString(inPoint);
		if (source == null || !inPoint.isModified()) {return source;}
		// Point has been modified - maybe it's possible to modify the source
		source = replaceGpxTags(source, "lat=\"", "\"", inPoint.getLatitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		source = replaceGpxTags(source, "lon=\"", "\"", inPoint.getLongitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		source = replaceGpxTags(source, "<ele>", "</ele>", inPoint.getAltitude().getStringValue(UnitSetLibrary.UNITS_METRES));
/* @since WB: export local timestamp if the timestamp value was blank on loading */
		source = replaceGpxTags(source, "<time>", "</time>", inPoint.getTimestamp().getText(Timestamp.Format.ISO8601, null),
				inPoint.getTimestamp().getText(null));
		if (inPoint.isWaypoint())
		{
			source = replaceGpxTags(source, "<name>", "</name>", XmlUtils.fixCdata(inPoint.getWaypointName()));
			if (source != null)
			{
				source = source.replaceAll("<description>", "<desc>").replaceAll("</description>", "</desc>");
			}
			source = replaceGpxTags(source, "<desc>", "</desc>",
				XmlUtils.fixCdata(inPoint.getFieldValue(Field.DESCRIPTION)));
			source = replaceGpxTags(source, "<cmt>", "</cmt>", inPoint.getFieldValue(Field.COMMENT));
		}
		// photo / audio links
		if (source != null && (inPoint.hasMedia() || source.indexOf("</link>") > 0)) {
			source = replaceMediaLinks(source, makeMediaLink(inPoint));
		}
		return source;
	}

	/**
	 * Replace the given value into the given XML string
	 * @param inSource source XML for point
	 * @param inStartTag start tag for field
	 * @param inEndTag end tag for field
	 * @param inValue value to replace between start tag and end tag
	 * @return modified String, or null if not possible
	 */
	private static String replaceGpxTags(String inSource, String inStartTag, String inEndTag, String inValue)
	{
		return replaceGpxTags(inSource, inStartTag, inEndTag, inValue, "");
	}
	
	/**
	 * Replace the given value into the given XML string
	 * @param inSource source XML for point
	 * @param inStartTag start tag for field
	 * @param inEndTag end tag for field
	 * @param inValue value to replace between start tag and end tag
	 * @param inDefaultValue value to replace between start tag and end tag if original value is blank
	 * @return modified String, or null if not possible
	 */
	private static String replaceGpxTags(String inSource, String inStartTag, String inEndTag, String inValue,
			String inDefaultValue)
	{
		if (inSource == null) {return null;}
		// Look for start and end tags within source
		final int startPos = inSource.indexOf(inStartTag);
		final int endPos = inSource.indexOf(inEndTag, startPos+inStartTag.length());
		if (startPos > 0 && endPos > 0)
		{
			String origValue = inSource.substring(startPos + inStartTag.length(), endPos);
			if (inValue != null && origValue.equals(inValue)) {
				// Value unchanged
				return inSource;
			}
			else if (inValue == null || inValue.equals("")) {
				// Need to delete value
				return inSource.substring(0, startPos) + inSource.substring(endPos + inEndTag.length());
			}
			else if (origValue.equals("")) {
				// Need to replace value
				return inSource.substring(0, startPos+inStartTag.length()) + inDefaultValue + inSource.substring(endPos);
			}
			else {
				// Need to replace value
				return inSource.substring(0, startPos+inStartTag.length()) + inValue + inSource.substring(endPos);
			}
		}
		// Value not found for this field in original source
		if (inValue == null || inValue.equals("")) {return inSource;}
		return null;
	}


	/**
	 * Replace the media tags in the given XML string
	 * @param inSource source XML for point
	 * @param inValue value for the current point
	 * @return modified String, or null if not possible
	 */
	private static String replaceMediaLinks(String inSource, String inValue)
	{
		if (inSource == null) {return null;}
		// Note that this method is very similar to replaceGpxTags except there can be multiple link tags
		// and the tags must have attributes.  So either one heavily parameterized method or two.
		// Look for start and end tags within source
		final String STARTTEXT = "<link";
		final String ENDTEXT = "</link>";
		final int startPos = inSource.indexOf(STARTTEXT);
		final int endPos = inSource.lastIndexOf(ENDTEXT);
		if (startPos > 0 && endPos > 0)
		{
			String origValue = inSource.substring(startPos, endPos + ENDTEXT.length());
			if (inValue != null && origValue.equals(inValue)) {
				// Value unchanged
				return inSource;
			}
			else if (inValue == null || inValue.equals("")) {
				// Need to delete value
				return inSource.substring(0, startPos) + inSource.substring(endPos + ENDTEXT.length());
			}
			else {
				// Need to replace value
				return inSource.substring(0, startPos) + inValue + inSource.substring(endPos + ENDTEXT.length());
			}
		}
		// Value not found for this field in original source
		if (inValue == null || inValue.equals("")) {return inSource;}
		return null;
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
	 * @param inCachers cacher list to ask for headers, if available
	 * @return header string from cachers or as default
	 */
	private static String getGpxHeaderString(GpxCacherList inCachers)
	{
		String gpxHeader = null;
		if (inCachers != null) {gpxHeader = inCachers.getFirstHeader();}
		if (gpxHeader == null || gpxHeader.length() < 5)
		{
			// TODO: Consider changing this to default to GPX 1.1
			// Create default (1.0) header
			gpxHeader = "<gpx version=\"1.0\" creator=\"" + GPX_CREATOR
				+ "\"\n xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ " xmlns=\"http://www.topografix.com/GPX/1/0\""
				+ " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n";
		}
		return gpxHeader + "\n";
	}


	/**
	 * Export the specified waypoint into the file
	 * @param inPoint waypoint to export
	 * @param inWriter writer object
	 * @param inSettings export settings
	 * @throws IOException on write failure
	 */
	private static void exportWaypoint(DataPoint inPoint, Writer inWriter,
		SettingsForExport inSettings)
		throws IOException
	{
		inWriter.write("\n\t<wpt lat=\"");
		inWriter.write(inPoint.getLatitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		inWriter.write("\" lon=\"");
		inWriter.write(inPoint.getLongitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		inWriter.write("\">\n");
		// altitude if available
		if (inPoint.hasAltitude() || inSettings.getExportMissingAltitudesAsZero())
		{
			inWriter.write("\t\t<ele>");
			inWriter.write(inPoint.hasAltitude() ? inPoint.getAltitude().getStringValue(UnitSetLibrary.UNITS_METRES) : "0");
			inWriter.write("</ele>\n");
		}
		// timestamp if available (some waypoints have timestamps, some not)
		if (inPoint.hasTimestamp() && inSettings.getExportTimestamps())
		{
			inWriter.write("\t\t<time>");
			inWriter.write(inPoint.getTimestamp().getText(Timestamp.Format.ISO8601, null));
			inWriter.write("</time>\n");
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
		if (desc != null && !desc.equals(""))
		{
			inWriter.write("\t\t<desc>");
			inWriter.write(desc);
			inWriter.write("</desc>\n");
		}
		// comment, if any
		final String comment = XmlUtils.fixCdata(inPoint.getFieldValue(Field.COMMENT));
		if (comment != null && !comment.equals(""))
		{
			inWriter.write("\t\t<cmt>");
			inWriter.write(comment);
			inWriter.write("</cmt>\n");
		}
		// Media links, if any
		if (inSettings.getExportPhotoPoints() && inPoint.getPhoto() != null)
		{
			inWriter.write("\t\t");
			inWriter.write(makeMediaLink(inPoint.getPhoto()));
			inWriter.write('\n');
		}
		if (inSettings.getExportAudioPoints() && inPoint.getAudio() != null)
		{
			inWriter.write("\t\t");
			inWriter.write(makeMediaLink(inPoint.getAudio()));
			inWriter.write('\n');
		}
		// write waypoint type if any
		/* mark waypoint to not load with GpsPrune but is required for e.g. Locus Map 
		 * @since WB 
		 * */
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
		/* @since WB */
		// write link, if any
		final String link = XmlUtils.fixCdata(inPoint.getFieldValue(Field.WAYPT_LINK));
		if (desc != null && !link.equals(""))
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
	 * @param inSettings export settings
	 */
	private static void exportTrackpoint(DataPoint inPoint, Writer inWriter, SettingsForExport inSettings)
		throws IOException
	{
		inWriter.write("      <trkpt lat=\"");
		inWriter.write(inPoint.getLatitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		inWriter.write("\" lon=\"");
		inWriter.write(inPoint.getLongitude().output(Coordinate.FORMAT_DECIMAL_FORCE_POINT));
		inWriter.write("\">\n");
		// altitude
		if (inPoint.hasAltitude() || inSettings.getExportMissingAltitudesAsZero())
		{
			inWriter.write("        <ele>");
			inWriter.write(inPoint.hasAltitude() ? inPoint.getAltitude().getStringValue(UnitSetLibrary.UNITS_METRES) : "0");
			inWriter.write("</ele>\n");
		}
		// Maybe take timestamp from photo if the point hasn't got one
		Timestamp pointTimestamp = getPointTimestamp(inPoint, inSettings);
		// timestamp if available (and selected)
		if (pointTimestamp != null && inSettings.getExportTimestamps())
		{
			inWriter.write("        <time>");
			inWriter.write(pointTimestamp.getText(Timestamp.Format.ISO8601, null));
			inWriter.write("</time>\n");
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
		if (desc != null && !desc.equals(""))
		{
			inWriter.write("        <desc>");			
			inWriter.write(desc);
			inWriter.write("</desc>\n");
		}
		// comment, if any
		final String comment = XmlUtils.fixCdata(inPoint.getFieldValue(Field.COMMENT));
		if (comment != null && !comment.equals(""))
		{
			inWriter.write("        <cmt>");
			inWriter.write(comment);
			inWriter.write("</cmt>\n");
		}
		
		// photo, audio
		if (inPoint.getPhoto() != null && inSettings.getExportPhotoPoints())
		{
			inWriter.write("        ");
			inWriter.write(makeMediaLink(inPoint.getPhoto()));
			inWriter.write("\n");
		}
		if (inPoint.getAudio() != null && inSettings.getExportAudioPoints()) {
			inWriter.write(makeMediaLink(inPoint.getAudio()));
		}
		
		/*
		 * @since WB
		 */
		// symbol if any
		final String symbol = inPoint.getFieldValue(Field.WAYPT_SYM);
		if (symbol != null && !symbol.equals(""))
		{
			inWriter.write("        <sym>");
			inWriter.write(symbol);
			inWriter.write("</sym>\n");
		}
		// duration if any
		if (inPoint.getWaypointDuration() > 0) {
			inWriter.write("        <extensions>\n");
			inWriter.write("          <pause>" + inPoint.getWaypointDuration() + "</pause>\n");
			inWriter.write("        </extensions>\n");
		}
		inWriter.write("      </trkpt>\n");
	}


	/**
	 * Make the xml for the media link(s)
	 * @param inPoint point to generate text for
	 * @return link tags, or null if no links
	 */
	private static String makeMediaLink(DataPoint inPoint)
	{
			return null;
	}

	/**
	 * Make the media link for a single media item
	 * @param inMedia media item, either photo or audio
	 * @return link for this media
	 */
	private static String makeMediaLink(MediaObject inMedia)
	{
		if (inMedia.getFile() != null)
			// file link
			return "<link href=\"" + inMedia.getFile().getAbsolutePath() + "\"><text>" + inMedia.getName() + "</text></link>";
		if (inMedia.getUrl() != null)
			// url link
			return "<link href=\"" + inMedia.getUrl() + "\"><text>" + inMedia.getName() + "</text></link>";
		// No link available, must have been loaded from zip file - no link possible
		return "";
	}


	/**
	 * Strip the time from a GPX point source string
	 * @param inPointSource point source to copy
	 * @return point source with timestamp removed
	 */
	private static String stripTime(String inPointSource)
	{
		return inPointSource.replaceAll("[ \t]*<time>.*?</time>", "");
	}

	/**
	 * Get the timestamp from the point or its media
	 * @param inPoint point object
	 * @param inSettings export settings
	 * @return Timestamp object if available, or null
	 */
	private static Timestamp getPointTimestamp(DataPoint inPoint, SettingsForExport inSettings)
	{
		if (inPoint.hasTimestamp())
		{
			return inPoint.getTimestamp();
		}
		if (inPoint.getPhoto() != null && inSettings.getExportPhotoPoints())
		{
			if (inPoint.getPhoto().hasTimestamp())
			{
				return inPoint.getPhoto().getTimestamp();
			}
		}
		if (inPoint.getAudio() != null && inSettings.getExportAudioPoints())
		{
			if (inPoint.getAudio().hasTimestamp())
			{
				return inPoint.getAudio().getTimestamp();
			}
		}
		return null;
	}
}
