package de.biselliw.tour_navigator.tim.prune.load;

import java.io.File;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.helpers.Log;

/**
 * Holds a lock on the given file and performs some action
 * when all locks are released
 * @since 26.1
 */
public class FileToBeLoaded
{
    /**
     * TAG for log messages.
     */
    static final String TAG = "FileToBeLoaded";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

	private final File _file;
	private final Runnable _afterwards;
	private int _ownerCounter;

	public FileToBeLoaded(File inFile, Runnable inAfterwards)
	{
		_file = inFile;
		_afterwards = inAfterwards;
		_ownerCounter = 1;
	}

	public File getFile() {
		return _file;
	}

	/** Accept ownership, perhaps for use in different thread
     * @todo takeOwnership()
     * */
	public synchronized void takeOwnership() {
//        Log.d(TAG, "takeOwnership file = "+_file.getAbsolutePath());
//	@todo	_ownerCounter++;
	}

	/** Release ownership */
	public synchronized void release()
	{
		_ownerCounter--;
//        Log.d(TAG, "release file = "+_file.getAbsolutePath());
		if (_ownerCounter == 0) {
            if (_afterwards != null)
			    _afterwards.run();
		}
	}
}
