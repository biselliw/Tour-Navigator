package de.biselliw.tour_navigator.tim_prune.data;


import java.io.File;

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.tim.prune.data.FileType;
import de.biselliw.tour_navigator.tim.prune.data.ExtensionInfo;

/**
 * Class to hold the source of the point data, including the original file
 * and file type, and references to each of the point objects
 * @since 26.1
 */
public class SourceInfo
{
	/** Source file */
	private final File _sourceFile;
	/** Name of source */
	private final String _sourceName;
	/** File type */
	private final FileType _fileType;
	/** File version */
	private final String _fileVersion;
	/** File title, if any */
	private String _fileTitle = null;
	/** File description, if any */
	private String _fileDescription = null;
	/** Extension info, if any */
	private ExtensionInfo _extensionInfo = null;
	/** Number of points */
	private int _numPoints = 0;

	/**
	 * Extensions of SourceInfo
	 * @implNote by BiselliW
	 */

	/** author parsed from tag <metadata><author><name> */
	private String _author = "";
	private String _metaTime = "";
	private String _link = "";
	private String _name = "";

	/** Constructor giving just the file and its type, without a version */
	public SourceInfo(File inFile, FileType inType) {
		this(inFile, inType, null);
	}

	/**
	 * Constructor
	 * @param inFile source file
	 * @param inType type of file
	 * @param inVersion version
	 */
	public SourceInfo(File inFile, FileType inType, String inVersion)
	{
		_sourceFile = inFile;
		_sourceName = (inFile != null ? inFile.getName() : "");
		_fileType = inType;
		_fileVersion = inVersion;
	}

	/**
	 * Constructor
	 * @param inName name of source (without file)
	 * @param inType type of file
	 */
	public SourceInfo(String inName, FileType inType)
	{
		_sourceFile = null;
		_sourceName = inName;
		_fileType = inType;
		_fileVersion = null;
	}

	/**
	 * @param inTitle title of file, eg from <name> tag in gpx
	 */
	public void setFileTitle(String inTitle) {
		_fileTitle = inTitle;
	}

	/**
	 * @param inDesc description of file, eg from <desc> tag in gpx
	 */
	public void setFileDescription(String inDesc) {
		_fileDescription = inDesc;
	}

	public void setExtensionInfo(ExtensionInfo inInfo) {
		_extensionInfo = inInfo;
	}

    public void setLink (String link ) {
       _link = link;
    }

    /**
     * @return source file
     */
	public File getFile() {
		return _sourceFile;
	}

	/**
	 * @return source name
	 */
	public String getName() {
		return _sourceName;
	}

	/**
	 * @return file type of source
	 */
	public FileType getFileType() {
		return _fileType;
	}

	/** @return version of file */
	public String getFileVersion() {
		return _fileVersion;
	}

	/**
	 * @return title of file
	 */
	public String getFileTitle() {
		return _fileTitle;
	}

	/**
	 * @return description of file
	 */
	public String getFileDescription() {
		return _fileDescription;
	}

	/**
	 * @param inNumPoints the number of points loaded from this source
	 */
	public void setNumPoints(int inNumPoints) {
		_numPoints = inNumPoints;
	}


	/**
	 * @return a string describing the extensions, or null if there aren't any
	 */
	public String getExtensions()
	{
		if (_extensionInfo == null) {
			return null;
		}
		StringBuilder builder = null;
		for (String url : _extensionInfo.getExtensions())
		{
			if (builder == null) {
				builder = new StringBuilder();
			}
			else {
				builder.append(", ");
			}
			builder.append(url);
		}
		return builder == null ? null : builder.toString();
	}

	/** @return the complete extension information, or null */
	public ExtensionInfo getExtensionInfo() {
		return _extensionInfo;
	}
	
	/**
	/**
	 * @return meta name
	 * @author BiselliW
	 * @since 22.2.006
	 */
	@NonNull
	public String getMetaName()
	{
		if (_name == null) return "";
		return _name;
	}

	/**
	 * @return meta time
	 * @author BiselliW
	 * @since 22.2.006
	 */
	@NonNull
	public String getMetaTime()
	{
		if (_metaTime == null) return "";
		return _metaTime;
	}

	/**
	 * @return meta link
	 * @author BiselliW
	 * @since 22.2.006
	 */
	@NonNull
	public String getMetaLink()
	{
		if (_link == null) return "";
		return _link;
	}

	/**
	 * @return author of the file
	 * @author BiselliW
	 * @since 22.2.006
	 */
	public String getAuthor()
	{
		return _author;
	}

    /**
     * set the name of the author of the gpx file
     * @param inAuthor author of file, eg from meta tag <author><name in gpx file
     * @author BiselliW
     * @since 22.2.006
     */
    public void setAuthor(String inAuthor)
    {
         _author = inAuthor;
    }
}
