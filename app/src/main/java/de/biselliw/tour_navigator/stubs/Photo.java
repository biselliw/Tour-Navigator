package de.biselliw.tour_navigator.stubs;

// Basic class required for Android app
// @since WB

import java.io.File;

import tim.prune.data.MediaObject;

/**
 * Class to represent a photo and link to DataPoint
 */
public class Photo extends MediaObject
{

	/**
	 * Constructor
	 * @param inFile File object for photo
	 */
	public Photo(File inFile)
	{
		super(inFile, null);
	}

	/**
	 * Constructor using data, eg from zip file or URL
	 * @param inData data as byte array
	 * @param inName name of file from which it came
	 * @param inUrl url from which it came (or null)
	 */
	public Photo(byte[] inData, String inName, String inUrl)
	{
		super(inData, inName, inUrl);
	}


}
