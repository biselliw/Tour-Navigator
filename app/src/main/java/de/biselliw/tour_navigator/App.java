package de.biselliw.tour_navigator;

import android.util.Log;

import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.helpers.GPSsimulator;
import de.biselliw.tour_navigator.helpers.I18nManager;
import tim.prune.data.DataPoint;
import tim.prune.data.Field;
import tim.prune.data.PointCreateOptions;
import tim.prune.data.SourceInfo;
import tim.prune.data.Track;
import tim.prune.data.TrackInfo;
import tim.prune.load.MediaLinkInfo;
import tim.prune.load.TrackNameList;

import static de.biselliw.tour_navigator.activities.LocationActivity.TASK_COMPLETE;
import static de.biselliw.tour_navigator.helpers.GPSsimulator.gpsSim;

public class App {
    private static SourceInfo _sourceInfo = null;
    private static Track _track = null;
    private final TrackInfo _trackInfo;
    private static TrackDetails _trackDetails = null;
    private MainActivity _main;;
    private RecordAdapter _recordAdapter;

    /**
     * TAG for log messages.
     */
    static final String TAG = "App";
    private static final boolean DEBUG = false; // Set to true to enable logging

    // hiking speed parameters
    private double horSpeed = TrackDetails.DEF_HOR_SPEED; // horizontal part in [km/h]
    private double vertSpeedClimb = TrackDetails.DEF_VERT_SPEED_CLIMB; // ascending part in [km/h]
    private double vertSpeedDescent = TrackDetails.DEF_VERT_SPEED_DESC; // descending part in [km/h]
    private int minHeightChange = 10;

    private static long TotalPauseSeconds = 0L;

    public String TrackName = "";
    public String TrackDescription = "";

    public App(MainActivity main)  {
        _main = main;
        _recordAdapter = _main.recordAdapter;
        _track = new Track();
        _trackInfo = new TrackInfo(_track);

        I18nManager.init(main);
    }

    /**
     * @return the current TrackInfo
     */
    public TrackInfo getTrackInfo()
    {
        return _trackInfo;
    }

    /**
     * Receive loaded data and determine whether to filter on tracks or not
     *
     * @param inFieldArray    array of fields
     * @param inDataArray     array of data
     * @param inSourceInfo    information about the source of the data
     * @param inTrackNameList information about the track names
     */
    public void informDataLoaded(Field[] inFieldArray, Object[][] inDataArray,
                                 SourceInfo inSourceInfo, TrackNameList inTrackNameList) {
        if (DEBUG) {
            Log.d(TAG, "informDataLoaded 1");
        }
        // no link array given
        informDataLoaded(inFieldArray, inDataArray, null, inSourceInfo,
                inTrackNameList, null);
    }

    /**
     * Receive loaded data and determine whether to filter on tracks or not
     *
     * @param inFieldArray    array of fields
     * @param inDataArray     array of data
     * @param inOptions       creation options such as units
     * @param inSourceInfo    information about the source of the data
     * @param inTrackNameList information about the track names
     */
    public void informDataLoaded(Field[] inFieldArray, Object[][] inDataArray,
                                 PointCreateOptions inOptions, SourceInfo inSourceInfo, TrackNameList inTrackNameList) {
        if (DEBUG)
            Log.d(TAG, "informDataLoaded 3");

        // no link array given
        informDataLoaded(inFieldArray, inDataArray, inOptions, inSourceInfo,
                inTrackNameList, null);
    }

    /**
     * Receive loaded data and determine whether to filter on tracks or not
     *
     * @param inFieldArray    array of fields
     * @param inDataArray     array of data
     * @param inOptions       creation options such as units
     * @param inSourceInfo    information about the source of the data
     * @param inTrackNameList information about the track names
     * @param inLinkInfo      links to photo/audio clips
     */
    public void informDataLoaded(Field[] inFieldArray, Object[][] inDataArray, PointCreateOptions inOptions,
                                 SourceInfo inSourceInfo, TrackNameList inTrackNameList, MediaLinkInfo inLinkInfo) {
        if (DEBUG) {
            Log.d(TAG, "informDataLoaded 3");
        }
        // Check whether loaded array can be properly parsed into a Track
        Track loadedTrack = new Track();
        loadedTrack.load(inFieldArray, inDataArray, inOptions);
        if (loadedTrack.getNumPoints() <= 0) {
            //		showErrorMessage("error.load.dialogtitle", "error.load.nopoints");
            return;
        }

        TrackName        = inTrackNameList.getTrackName(0);
//        TrackDescription = inTrackNameList.

                // go directly to load
        informDataLoaded(loadedTrack, inSourceInfo);
    }

    /**
     * Receive loaded data and optionally merge with current Track
     *
     * @param inLoadedTrack loaded track
     * @param inSourceInfo  information about the source of the data
     */
    public void informDataLoaded(Track inLoadedTrack, SourceInfo inSourceInfo) {
		// Decide whether to load or append
        _sourceInfo = inSourceInfo;
		if (_track.getNumPoints() > 0)
		{
            // Don't append, replace data
            _track.load((Track) inLoadedTrack);
            if (inSourceInfo != null)
            {
                // set source information
                inSourceInfo.populatePointObjects(_track, _track.getNumPoints());
                _trackInfo.getFileInfo().replaceSource(inSourceInfo);
            }
		}
		else
		{
			// Currently no data held, so transfer received data
			_track.load((Track) inLoadedTrack);
			if (inSourceInfo != null)
			{
				inSourceInfo.populatePointObjects(_track, _track.getNumPoints());
				_trackInfo.getFileInfo().addSource(inSourceInfo);
			}
		}
        recalculate();
        if (!_track.hasWaypoints())
        {
            if (gpsSim == null)
            {
                gpsSim = new GPSsimulator(_track);
            }
        }
        else
        {
            if (gpsSim != null)
                gpsSim.Reset();
        }
        _main.handleState(this, TASK_COMPLETE);
	}

    /*
     * Recalculate all track points
     */
    public void recalculate() {
        RecordAdapter.Record record;
        int numPoints = _track.getNumPoints();
        _trackDetails = null;
        TotalPauseSeconds = 0L;
        long PauseSec;

        if (DEBUG) {
            Log.d(TAG, "recalculate(): " + numPoints + " Trackpoints");
        }
        if (numPoints > 0) {
            _trackDetails = new TrackDetails(_track);

            _trackDetails.setHikingParameters(horSpeed, vertSpeedClimb, vertSpeedDescent, minHeightChange);
            _track.interleaveWaypoints();
            _trackDetails.recalculate();

            if (DEBUG) {
                Log.d(TAG, "Build timetable");
            }
            if (_recordAdapter != null)
            {
                _recordAdapter.RemoveRecords();

                for (int ptIndex = 0; ptIndex <= numPoints - 1; ptIndex++) {
                    boolean addRecord = _trackDetails.RecalculateTrackpoint(ptIndex);
                    if (addRecord) {
                        DataPoint currPoint = _track.getPoint(ptIndex);
                        record = new RecordAdapter.Record(
                                currPoint,
                                ptIndex,
                                _trackDetails.getSumDistance(),
                                _trackDetails.getSumClimb(),
                                _trackDetails.getSumDescent(),
                                _trackDetails.getSumSeconds());
                        _recordAdapter.add(record);

                        if (_trackDetails.CalcTimes) {
                            int PauseMin = currPoint.getWaypointDuration();
                            PauseSec = PauseMin * 60L;
                            TotalPauseSeconds += PauseSec;
                        }
                    }
                }
            }
            if (DEBUG) {
                Log.d(TAG, "timetable built");
                Log.d(TAG, "Sclimb: " + _trackDetails.getSumClimb ());
                Log.d(TAG, "Sdescent: " + _trackDetails.getSumDescent ());
            }
            _main.TotalDistance = _trackDetails.Distance_km;
// todo FATAL EXCEPTION: Thread-2: android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
//            _main.showDistances(0);
        }
    }

    public void Update() {
        if (_trackDetails == null) return;

        int numPoints = _track.getNumPoints();

        if (DEBUG) {
            Log.d(TAG, "Update: " + numPoints + " Trackpoints");
        }
        if (numPoints > 0) {
            _trackDetails.setHikingParameters(horSpeed, vertSpeedClimb, vertSpeedDescent, minHeightChange);
            recalculate();
        }
        _recordAdapter.notifyDataSetChanged();
    }

    /**
     * set hiking parameters:
     *
     * @param inHorSpeed         horizontal part in [km/h]
     * @param inVertSpeedClimb   ascending part in [km/h]
     * @param inVertSpeedDescent descending part in [km/h]
     * @param inMinHeightChange  min. required altitude change between two trackpoints for calc.
     */
    public void setHikingParameters(double inHorSpeed, double inVertSpeedClimb, double inVertSpeedDescent, int inMinHeightChange) {
        horSpeed = inHorSpeed;
        vertSpeedClimb = inVertSpeedClimb;
        vertSpeedDescent = inVertSpeedDescent;
        minHeightChange = inMinHeightChange;

        Update();
    }
    public static SourceInfo getSourceInfo() {
        return _sourceInfo;
    }

    public DataPoint getPoint(int ptIndex) {
        return _track.getPoint(ptIndex);
    }

    public static Track getTrack()
    {
        return _track;
    }

    /**
     * Reverse the route
     */
    public boolean reverseRoute()
    {
        boolean result = false;
        if (_track != null)
        {
            result = _track.reverseRoute();
            Update();
        }
        return result;
    }

    /**
     * Search for the given Point in the track and return the index
     * @param inPoint Point to look for
     * @return index of Point, if any or -1 if not found
     */
    public int getPointIndex(DataPoint inPoint) {
        return _track.getPointIndex(inPoint);
    }

    public double getLatitude(int ptIndex) {
        return _track.getLatitude(ptIndex);
    }

    public double getLongitude(int ptIndex) {
        return _track.getLongitude(ptIndex);
    }

    public int getNearestTrackpointIndex(int inStart, double inLatitude, double inLongitude, double inMaxDist, double inMaxDistDest) {
        return _track.getNearestTrackpointIndex(inStart, inLatitude, inLongitude, inMaxDist, inMaxDistDest);
    }

    /**
     * Return the nearest distance of a track point to the specified Latitude and Longitude coordinates.
     * Index of nearest track point must have been calculated using @see "getNearestPointIndex2()"
     * @return distance of nearest track point [km], negated if not within the specified max distance
     * @since WB
     * - all coordinates in [km]
     */
    public double getNearestDistance() {
        return _track.getNearestDistance();
    }

    public static int getClimb() {
        return _trackDetails.getClimb();
    }

    public static int getDescent() {
        return _trackDetails.getDescent ();
    }

    public static long getTotalCalcSeconds() {
        return _trackDetails.getTotalCalcSeconds();
    }

    public static long getTotalPauseSeconds() { return TotalPauseSeconds; }

    public double getTotalDistance() { return _trackDetails.Distance_km; }

    public int getMinAltitude() {
        if (_trackDetails == null) return 0;
        return _trackDetails.getMinAltitude();
    }

    public int getMaxAltitude() {
        if (_trackDetails == null) return 0;
        return _trackDetails.getMaxAltitude();
    }
}

