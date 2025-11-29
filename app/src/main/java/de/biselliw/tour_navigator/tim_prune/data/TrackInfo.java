package de.biselliw.tour_navigator.tim_prune.data;

import java.util.List;

import de.biselliw.tour_navigator.tim.prune.data.FileInfo;

/**
 * Class to hold all track information, including data
 * and the selection information
 * @since 26.1
 */
public class TrackInfo
{
	private final Track _track;
	private FileInfo _fileInfo = null;

	/**
	 * Constructor
	 * @param inTrack Track object
	 */
	public TrackInfo(Track inTrack)
	{
		_track = inTrack;
	}

	/**
	 * @return the Track object
	 */
	public Track getTrack() {
		return _track;
	}

	/**
	 * @return the FileInfo object
	 */
	public FileInfo getFileInfo()
	{
		if (_fileInfo == null)
		{
			_fileInfo = new FileInfo();
			for (int i = 0; i < _track.getNumPoints(); i++) {
				_fileInfo.addSource(_track.getPoint(i).getSourceInfo());
			}
		}
		return _fileInfo;
	}

	/** Delete the current file information so that it will be regenerated */
	public void clearFileInfo() {
		_fileInfo = null;
	}

	public boolean appendRange(List<DataPoint> inPoints)
	{
		final int currentNumPoints = getTrack().getNumPoints();
		if (getTrack().appendRange(inPoints))
		{
			// Select the first point added
//			selectPoint(currentNumPoints);
			return true;
		}
		return false;
	}

}
