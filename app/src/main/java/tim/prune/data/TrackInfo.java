package tim.prune.data;

// Basic class required for Android app
// @since WB



/**
 * Class to hold all track information, including data
 * and the selection information
 */
public class TrackInfo
{
	private Track _track = null;
	private final Selection _selection;
	private FileInfo _fileInfo = null;

	/**
	 * Constructor
	 * @param inTrack Track object
	 */
	public TrackInfo(Track inTrack)
	{
		_track = inTrack;
		_selection = new Selection(_track);
		_fileInfo = new FileInfo();
	}


	/**
	 * @return the Track object
	 */
	public Track getTrack() {
		return _track;
	}


	/**
	 * @return the Selection object
	 */
	public Selection getSelection() {
		return _selection;
	}


	/**
	 * @return the FileInfo object
	 */
	public FileInfo getFileInfo() {
		return _fileInfo;
	}
}
