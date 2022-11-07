package tim.prune.load.xml;

// Basic class required for Android app
// @since WB

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import de.biselliw.tour_navigator.files.GpxHandler;
import tim.prune.data.SourceInfo;
import tim.prune.load.MediaLinkInfo;

import android.util.Log;

import de.biselliw.tour_navigator.App;

/**
 * Class for handling loading of Xml files, and passing the
 * loaded data back to the App object
 */
public class XmlFileLoader extends DefaultHandler implements Runnable
{
	public SourceInfo sourceInfo = null;

	private File _file = null;
	private FileInputStream _XML_filestream = null;
	private InputStream _XML_stream = null;

	private App _app = null;
	private XmlHandler _handler = null;
	private boolean _parsedXmlStream = false;
	private String _unknownType = null;

	private Thread _thread = null;

	/** TAG for log messages. */
	static final String TAG = "XmlFileLoader";
	private static final boolean DEBUG = false; // Set to true to enable logging

	/**
	 * Constructor
	 * @param inApp Application object to inform of track load
	 */
	public XmlFileLoader(App inApp)
	{
		_app = inApp;
	}

	/**
	 * Reset the handler to ensure data cleared
	 */
	public void reset()
	{
		_handler = null;
		_unknownType = null;
		_parsedXmlStream = false;
	}

	/**
	 * Open the selected file
	 * @param inFile File to open
	 */
	public void openFile(File inFile) throws FileNotFoundException {
		_file = inFile;
		_XML_filestream = new FileInputStream(inFile);
		_XML_stream = _XML_filestream;
		reset();
		// start new thread in case xml parsing is time-consuming
		new Thread(this).start();
	}

	/**
	 * Open the selected stream
	 * @param inStream stream to open
	 */
	public void openStream(InputStream inStream)  {
		_file = null;
		_XML_filestream = null;
		_XML_stream = inStream;
		reset();
		if (DEBUG) {
			Log.d(TAG, "Start new thread in case xml parsing is time-consuming");
		}
		_thread = new Thread(this);
		_thread.start();
	}

	/**
	 * Run method, to parse the file
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		// Moves the current Thread into the background
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

		boolean success = false;
		try
		{
			if (DEBUG) {
				Log.d(TAG,"parse XML Stream");
			}
			success = parseXmlStream(_XML_stream);
		}
		catch (Exception e) {
			if (DEBUG) {
				Log.d(TAG,"FileNotFoundException");
			}
		}

		if (DEBUG) {
			Log.d(TAG,"result: " + success);
		}

		// Clean up the stream, don't need it any more
		try {_XML_stream.close();} catch (IOException e2) {}

		if (success)
		{
			// Check whether handler was properly instantiated
			if (_handler == null)
			{
				// Wasn't either kml or gpx
				//_app.showErrorMessageNoLookup("error.load.dialogtitle",					I18nManager.getText("error.load.unknownxml") + " " + _unknownType);
			}
			else
			{
				SourceInfo.FILE_TYPE sourceType = (_handler instanceof GpxHandler ? SourceInfo.FILE_TYPE.GPX : SourceInfo.FILE_TYPE.KML);
				sourceInfo = new SourceInfo(_file, sourceType);
				sourceInfo.setFileTitle(_handler.getFileTitle());

				/* @since WB: set meta data */
				sourceInfo.setMetaData (_handler.metaName, _handler.metaDescription, _handler.metaAuthor, _handler.metaLink);
				sourceInfo.setTrackDescription(_handler.trackDescription);

				// Pass information back to app
				_app.informDataLoaded(_handler.getFieldArray(), _handler.getDataArray(),
					null, sourceInfo, _handler.getTrackNameList(),
					new MediaLinkInfo(_handler.getLinkArray()));
			}

		}
// TODO
		_XML_stream = null;
		_file = null;
		_XML_filestream = null;
	}

	/**
	 * Try both Xerces and the built-in java classes to parse the given xml stream
	 * @param inStream input stream from file / zip / gzip
	 * @return true on success, false if both xerces and built-in parser failed
	 */
	public boolean parseXmlStream(InputStream inStream)
	{
		boolean success = false;
		if (DEBUG) {
			Log.d(TAG,"Firstly, try to use xerces to parse the xml ");
		}
		// Firstly, try to use xerces to parse the xml (will throw an exception if not available)
		try
		{
			XMLReader xmlReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
			xmlReader.setContentHandler(this);
			xmlReader.parse(new InputSource(inStream));
			success = true; // worked
		}
		catch (Exception e) {} // don't care too much if it didn't work, there's a backup

		// If that didn't work, try the built-in classes (which work for xml1.0 but handling for 1.1 contains bugs)
		if (!success)
		{
			try
			{
				if (DEBUG) {
					Log.d(TAG,"Parse with SAXParser");
				}
				// Construct a SAXParser and use this as a default handler
				SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
				saxParser.parse(inStream, this);
				success = true;
				if (DEBUG) {
					Log.d(TAG,"Parsing with SAXParser successfull");
				}
			}
			catch (Exception e)
			{
				if (DEBUG) {
					if (_parsedXmlStream) {
						Log.d(TAG, "Parsing with SAXParser finished - exception ignored");
						success = true;
					}
					else {
						Log.d(TAG, "SAXParser Exception: " + e);
						// WB: accept "org.apache.harmony.xml.ExpatParser$ParseException: "
						// At line 632, column 6: junk after document element"
						if (e.getMessage().contains("junk after document element"))
							success = true;
							// WB: accept "At line 3481, column 12: not well-formed (invalid token)"
						else if (e.getMessage().contains("not well-formed (invalid token)"))
							success = true;
						else
							Log.d(TAG, "SAXParser Exception terminates XML file loading");
					}
				}
				// Show error dialog
//				_app.showErrorMessageNoLookup("error.load.dialogtitle",					I18nManager.getText("error.load.othererror") + " " + e.getMessage());
			}
		}
		return success;
	}

	/**
	 * Receive a tag
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName,
		Attributes attributes) throws SAXException
	{
		// Check for "kml" or "gpx" tags
		if (_handler == null)
		{
			if  (qName.equals("gpx")) {_handler = new GpxHandler();}
			else if (_unknownType == null && !qName.equals(""))
			{
				_unknownType = qName;
			}
		}
		else
		{
			try {
				// Handler instantiated so pass tags on to it
				_handler.startElement(uri, localName, qName, attributes);
			}

			catch (Exception e)
			{
				if (DEBUG) {
					Log.d(TAG,"Exception startElement");
				}
			}
		}
		super.startElement(uri, localName, qName, attributes);
	}


	/**
	 * Receive characters, either between or inside tags
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (_handler != null)
		{
			// Handler instantiated so pass tags on to it
			_handler.characters(ch, start, length);
		}
		super.characters(ch, start, length);
	}


	/**
	 * Receive end of element
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
		throws SAXException
	{
		if (_handler != null)
		{
			// Handler instantiated so pass tags on to it
			_handler.endElement(uri, localName, qName);

			if  (qName.equals("gpx"))
				_parsedXmlStream = true;
		}
		super.endElement(uri, localName, qName);
	}

	/**
	 * @return The Xml handler used for the parsing
	 */
	public XmlHandler getHandler()
	{
		return _handler;
	}
}
