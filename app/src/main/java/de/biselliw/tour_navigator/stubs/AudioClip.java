package de.biselliw.tour_navigator.stubs;

/*
* Dummy class required for Android app
*/

import java.io.File;

import tim.prune.data.MediaObject;

/**
 * Class to represent an audio clip for correlation
 */
public class AudioClip extends MediaObject
{
	/**
	 * Constructor
	 * @param inFile file object
	 */
	public AudioClip(File inFile)
	{
		// Timestamp is dummy
		super(inFile, null);
	}
}
