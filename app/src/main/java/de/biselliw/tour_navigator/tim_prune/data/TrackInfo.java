package de.biselliw.tour_navigator.tim_prune.data;

import java.util.List;

import de.biselliw.tour_navigator.data.TrackDetails;
import tim.prune.data.FileInfo;

/**
 * Class to hold all track information, including data
 * and the selection information
 */
public class TrackInfo
{
	private final TrackDetails _track;
	private FileInfo _fileInfo = null;

	/**
	 * Constructor
	 * @param inTrack Track object
	 */
	public TrackInfo(TrackDetails inTrack)
	{
		_track = inTrack;
	}

	/**
	 * @return the Track object
	 */
	public TrackDetails getTrack() {
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


	/**
	 * Select the given DataPoint
	 * @param inPoint DataPoint object to select
	 */
	public void selectPoint(DataPoint inPoint) {
		selectPoint(_track.getPointIndex(inPoint));
	}


	/**
	 * Select the data point with the given index
	 * @param inPointIndex index of DataPoint to select, or -1 for none
	 */
	public void selectPoint(int inPointIndex)
	{
        // nothing to select
	}

	public boolean appendRange(List<DataPoint> inPoints)
	{
		final int currentNumPoints = getTrack().getNumPoints();
		if (getTrack().appendRange(inPoints))
		{
			// Select the first point added
			selectPoint(currentNumPoints);
			return true;
		}
		return false;
	}


	public void markPointForDeletion(int inIndex) {
		markPointForDeletion(inIndex, true);
	}

	public void markPointForDeletion(int inIndex, boolean inDelete) {
		markPointForDeletion(inIndex, inDelete, false);
	}

	public void markPointForDeletion(int inIndex, boolean inDelete, boolean inSegmentBreak)
	{

	}

}
