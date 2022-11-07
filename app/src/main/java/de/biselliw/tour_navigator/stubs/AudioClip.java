package de.biselliw.tour_navigator.stubs;

import java.io.File;

/**
 * Stub class to represent an audio clip for correlation
 *
 * @implSpec audio clips are not handled within this app
 * @author BiselliW
 */
public class AudioClip extends MediaObject
{
	/**
	 * Dummy Constructor
	 *
	 * @param inFile file object
	 */
	public AudioClip(File inFile)
	{
		super(inFile, null);
	}
}
