package de.biselliw.tour_navigator.stubs;

import java.io.File;

/**
 * Stub class to represent a photo and link to DataPoint
 *
 * @implSpec Photos are not handled within this app
 * @author BiselliW
 */
public abstract class Photo extends MediaObject
{
	/**
	 * Dummy Constructor
	 * @param inFile File object for photo
	 */
	public Photo(File inFile)
	{
		super(inFile, null);
	}

}
