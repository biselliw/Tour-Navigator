package de.biselliw.tour_navigator.tim_prune.data;

import de.biselliw.tour_navigator.tim.prune.data.FileInfo;

/**
 * Class to hold all track information, including data
 * and the selection information
 */
public class TrackInfo
{
	private final BaseTrack _track;
	private FileInfo _fileInfo = null;

	/**
	 * Constructor
	 * @param inTrack Track object
	 */
	public TrackInfo(BaseTrack inTrack)
	{
		_track = inTrack;
		_fileInfo = new FileInfo();
	}

	/**
	 * @return the Track object
	 */
	public BaseTrack getTrack() {
		return _track;
	}

	/**
	 * @return the FileInfo object
	 */
	public FileInfo getFileInfo() {
		return _fileInfo;
	}

	/**
	 * Replace the file info with a previously made clone
	 * @param inInfo cloned file info
	 */
	public void setFileInfo(FileInfo inInfo) {
		_fileInfo = inInfo;
	}

}
