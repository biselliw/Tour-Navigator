package tim.prune.data;
	/* @since WB */

import java.io.File;

import de.biselliw.tour_navigator.data.DataPoint;

/**
 * Class to hold the source of the point data, including the original file
 * and file type, and references to each of the point objects
 */
public class SourceInfo
{
	/** File type of source file */
	public enum FILE_TYPE {TEXT, GPX, KML, NMEA, GPSBABEL, GPSIES, JSON};

	/** Source file */
	private File _sourceFile = null;
	/** Name of source */
	private String _sourceName = null;
	/** File type */
	private FILE_TYPE _fileType = null;
	/** File title, if any */
	private String _fileTitle = null;

	/** Array of datapoints */
	private DataPoint[] _points = null;
	/** Number of points */
	private int _numPoints = 0;
	/** Array of point indices (if necessary) */
	private int[] _pointIndices = null;

	/* @since WB */
	private String _name = "";
	private String _author = "";
	private String _metaDescription = "";
	private String _link = "";
	private String _trackDescription = "";

	/**
	 * Constructor
	 * @param inFile source file
	 * @param inType type of file
	 */
	public SourceInfo(File inFile, FILE_TYPE inType)
	{
		_sourceFile = inFile;
		_sourceName = (inFile != null) ? inFile.getName() : "";
		_fileType = inType;
	}

	/**
	 * Constructor
	 * @param inName name of source (without file)
	 * @param inType type of file
	 */
	public SourceInfo(String inName, FILE_TYPE inType)
	{
		_sourceFile = null;
		_sourceName = inName;
		_fileType = inType;
	}

	/**
	 * @param inTitle title of file, eg from <name> tag in gpx
	 */
	public void setFileTitle(String inTitle)
	{
		_fileTitle = inTitle;
	}

	/**
	 * @return source file
	 */
	public File getFile()
	{
		return _sourceFile;
	}

	/**
	 * @return source name
	 */
	public String getName()
	{
		if (_sourceName == null) return "";
		return _sourceName;
	}

	/**
	 * @return meta name
	 */
	public String getMetaName()
	{
		if (_name == null) return "";
		return _name;
	}

	/**
	 * @return meta link
	 */
	public String getMetaLink()
	{
		if (_link == null) return "";
		return _link;
	}

	/**
	 * @return track description
	 * @since WB
	 */
	public String getTrackDescription()
	{
		if (_trackDescription == null) return "";
		return _trackDescription;
	}

	/**
	 * Set the description of the track
	 * @param inDescription Description of the track
	 * @since WB
	 */
	public void setTrackDescription(String inDescription)
	{
		_trackDescription = inDescription;
	}

	/**
	 * @return meta description of the file
	 * @since WB
	 */
	public String getMetaDescription()
	{
		return _metaDescription;
	}

	/**
	 * @return author of the file
	 * @since WB
	 */
	public String getAuthor() 
	{
		return _author;
	}

		
	/**
	 * @return file type of source
	 */
	public FILE_TYPE getFileType()
	{
		return _fileType;
	}

	/**
	 * set meta data
	 * @param name        short description of the route
	 * @param description long  description of the route
	 * @param author      author's name
	 * @param link        web link
	 * @since WB
	 */
	public void setMetaData (String name, String description, String author, String link)
	{
		_name = name;
		_metaDescription = description;
		_author = author;
		_link = link;
	}

	/**
	 * @return title of file
	 */
	public String getFileTitle()
	{
		if (_fileTitle == null)
			return "";
		else
			return _fileTitle;
	}

	/**
	 * @return number of points from this source
	 */
	public int getNumPoints()
	{
		return _numPoints;
	}

	/**
	 * Set the indices of the points selected out of a loaded track
	 * @param inSelectedFlags array of booleans showing whether each point in the original data was loaded or not
	 */
	public void setPointIndices(boolean[] inSelectedFlags)
	{
		_numPoints = inSelectedFlags.length;
		_pointIndices = new int[_numPoints];
		int p=0;
		for (int i=0; i<_numPoints; i++) {
			if (inSelectedFlags[i]) {_pointIndices[p++] = i;}
		}
		// Now the point indices array holds the index of each of the selected points
	}

	/**
	 * Take the points from the given track and store
	 * @param inTrack track object containing points
	 * @param inNumPoints number of points loaded
	 */
	public void populatePointObjects(Track inTrack, int inNumPoints)
	{
		if (_numPoints == 0) {_numPoints = inNumPoints;}
		if (inNumPoints > 0)
		{
			_points = new DataPoint[inNumPoints];
			int trackLen = inTrack.getNumPoints();
			System.arraycopy(inTrack.cloneContents(), trackLen-inNumPoints, _points, 0, inNumPoints);
			// Note data copied twice here but still more efficient than looping
		}
	}

	/**
	 * Look for the given point in the array
	 * @param inPoint point to look for
	 * @return index, or -1 if not found
	 */
	public int getIndex(DataPoint inPoint)
	{
		int idx = -1;
		for (int i=0; i<_points.length; i++)
		{
			if (_points[i] == inPoint) {
				idx = i;
				break;
			}
		}
		if (idx == -1) {return idx;}             // point not found
		if (_pointIndices == null) {return idx;} // All points loaded
		return _pointIndices[idx]; // use point index mapping
	}
}
