package de.biselliw.tour_navigator.tim_prune.load.xml;

// Basic class required for Android app
// tim.prune.load.xml.XmlFileLoader
// @since WB

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.tim_prune.data.SourceInfo;
import de.biselliw.tour_navigator.tim.prune.load.FileToBeLoaded;

import static de.biselliw.tour_navigator.ui.ControlElements.control;

/**
 * Class for handling loading of Xml files, and passing the
 * loaded data back to the App object
 * @since 26.1
 */
public class XmlFileLoader extends DefaultHandler implements Runnable
{
    private InputStream _XML_stream = null;

	private final App _app;
	private FileToBeLoaded _fileLock = null;
	private boolean _autoAppend = false;
	private XmlHandler _handler = null;
	private boolean _parsedXmlStream = false;
	private String _unknownType = null;


	/** TAG for log messages. */
	static final String TAG = "XmlFileLoader";
	private static final boolean _DEBUG = false; // Set to true to enable logging
	private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

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
	 * @param inFileLock File to open
	 * @param inAutoAppend true to auto-append
     * @implNote does not work with Android devices
	 */
	public void openFile(FileToBeLoaded inFileLock, boolean inAutoAppend)
	{
		_fileLock = inFileLock;
		_fileLock.takeOwnership();	// we keep ownership for separate thread
		_autoAppend = inAutoAppend;
		reset();
		// start new thread in case xml parsing is time-consuming
		new Thread(this).start();
	}

    /**
     * Open the selected stream
     * @param inStream   stream to open
     * @author BiselliW
     */
    public void openStream(InputStream inStream)  {
        _fileLock = new FileToBeLoaded(null,null);
        _XML_stream = inStream;
        reset();
        if (DEBUG) {
            Log.d(TAG, "Start new thread in case xml parsing is time-consuming");
        }
        new Thread(this).start();
    }

	/**
	 * Run method, to parse the file
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		FileInputStream inStream = null;
		boolean success = false;
		try
		{
			if (DEBUG) Log.d(TAG,"parse XML Stream");
            if (_XML_stream != null)
                success = parseXmlStream(_XML_stream);
            else {
                inStream = new FileInputStream(_fileLock.getFile());
                success = parseXmlStream(inStream);
            }
		}
		catch (FileNotFoundException fnfe) {
			if (DEBUG) Log.e(TAG,"FileNotFound");
		}

		if (DEBUG) Log.d(TAG,"result: " + success);
		// Clean up the stream, don't need it any more
		if (inStream != null) {
			try {inStream.close();} catch (IOException e2) {}
		}

		if (success)
		{
			// Check whether handler was properly instantiated
			if (_handler == null)
			{
				// Wasn't either kml or gpx
                Log.e(TAG,"error.load.unknownxml" + " " + _unknownType);
			}
			else
			{
				SourceInfo sourceInfo = new SourceInfo(_fileLock.getFile(), _handler.getFileType(),
					_handler.getFileVersion());
				sourceInfo.setFileTitle(_handler.getFileTitle());
				sourceInfo.setFileDescription(_handler.getFileDescription());
				sourceInfo.setExtensionInfo(_handler.getExtensionInfo());
                sourceInfo.setLink(_handler.getLink());

				/* Pass information back to app
				new FileTypeLoader(_app).loadData(_handler, sourceInfo, _autoAppend,
					new MediaLinkInfo(_handler.getLinkArray()));

				 */
				_app.informDataLoaded(_handler.getFieldArray(), _handler.getDataArray(),
					null, sourceInfo, null /* _handler.getTrackNameList() */ );
			}
		}
		_fileLock.release();
	}

	/**
	 * Try both Xerces and the built-in java classes to parse the given xml stream
	 * @param inStream input stream from file / zip / gzip
	 * @return true on success, false if both xerces and built-in parser failed
	 */
	public boolean parseXmlStream(InputStream inStream)
	{
		boolean success = false;
		if (DEBUG) 	Log.d(TAG,"Firstly, try to use xerces to parse the xml ");
		// Firstly, try to use xerces to parse the xml (will throw an exception if not available)
		try
		{
			SAXParser saxParser = SAXParserFactory.newInstance("org.apache.xerces.parsers.SAXParser", null).newSAXParser();
			saxParser.parse(inStream, this);
			success = true; // worked
		}
		catch (Throwable e) {} // don't care too much if it didn't work, there's a backup

		// If that didn't work, try the built-in classes (which work for xml1.0 but handling for 1.1 contains bugs)
		if (!success)
		{
			try
			{
                if (DEBUG) Log.d(TAG, "Parse with SAXParser");
				// Construct a SAXParser and use this as a default handler
				SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
				saxParser.parse(inStream, this);
				success = true;
				if (DEBUG) Log.d(TAG, "Parsing with SAXParser successfull");
			}
			catch (Exception e)
			{
               if (DEBUG) {
                  if (_parsedXmlStream) {
                        Log.d(TAG, "Parsing with SAXParser finished - exception ignored");
                        success = true;
                    } else {
//                      Log.e(TAG, "SAXParser Exception: " + e.getMessage()););
					  
                        // BiselliW: accept "org.apache.harmony.xml.ExpatParser$ParseException: "
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
                // Show error message
                control.showErrorMessage(e.toString()); // .getMessage());
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
			if  (qName.equals("gpx")) {
				_handler = new GpxHandler();
				}
			else if (_unknownType == null && !qName.equals(""))
			{
				_unknownType = qName;
			}
		}
		if (_handler != null) {
			_handler.startElement(uri, localName, qName, attributes);
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

			if (qName.equals("gpx"))
				_parsedXmlStream = true;
		}
		super.endElement(uri, localName, qName);
	}

	/**
	 * @return The Xml handler used for the parsing
	 */
	public XmlHandler getHandler() {
		return _handler;
	}

}
