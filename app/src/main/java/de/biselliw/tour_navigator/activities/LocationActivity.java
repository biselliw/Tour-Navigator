package de.biselliw.tour_navigator.activities;

/*
    This file is part of Tour Navigator

    Tour Navigator is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    Tour Navigator is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail. If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2025 Walter Biselli (BiselliW)
*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.util.Locale;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.LocationService;
import de.biselliw.tour_navigator.data.AppState;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.ui.ControlElements;

import static de.biselliw.tour_navigator.activities.SettingsActivity.sharedPref;
import static de.biselliw.tour_navigator.activities.adapter.RecordAdapter.COLOR_DELAY_MAX;
import static de.biselliw.tour_navigator.activities.adapter.RecordAdapter.COLOR_DELAY_MIN;
import static de.biselliw.tour_navigator.activities.adapter.RecordAdapter.DELAY_MAX;
import static de.biselliw.tour_navigator.activities.adapter.RecordAdapter.DELAY_MIN;
import static de.biselliw.tour_navigator.data.AppState.gpsSimulation;
import static de.biselliw.tour_navigator.data.TrackTiming.trackTiming;


/**
 * Activity handling the timing of a tour
 * It implements a LocationListener using Google Play services
 *
 * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/location/LocationListener">
 *     LocationListener on developer.android.com</a>
 * @see <a href="https://developer.android.com/reference/android/location/LocationListener">
 *     LocationListener on developer.android.com</a>
 */
public class LocationActivity extends ControlElements implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * TAG for log messages.
     */
    static final String TAG = "LocationActivity";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    public static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /** status of the GPS location provider */
    public enum gpsStatus {
        NOT_REGISTERED,
        PERMISSION_DENIED,
        PERMISSION_GRANTED,
        PROVIDER_DISABLED,
        PROVIDER_ENABLED,
        WAIT_FOR_GPS_FIX,
        GPS_FIX,
        GPS_TIMEOUT
    }

    /** status of the navigation */
    public enum locationStatus {
        INITIAL,
        NO_GPX_FILE_LOADED,
        GPX_FILE_LOADED,
        WAIT_USER_START,
        GOTO_START_POS,
        TRACKING,
        APPROACHING,
        OUT_OF_TRACK,
        DESTINATION_REACHED,
        DESTINATION_FAILED
    }

    private static final int COLOR_RED                 = 0xAAB71C1C;
    private static final int COLOR_WHITE               = 0xFFFFFFFF;
    private static final int COLOR_MESSAGE             = COLOR_WHITE;
    private static final int COLOR_NO_GPX              = COLOR_RED;
    private static final int COLOR_TRACKING            = 0xAA317335;
    private static final int COLOR_APPROACHING         = COLOR_RED;
    private static final int COLOR_OUT_OF_TRACK        = COLOR_RED;
    private static final int COLOR_DESTINATION_REACHED = COLOR_TRACKING;
    private static final int COLOR_DESTINATION_FAILED  = COLOR_RED;


    final static double maxOffsetStart_km = 0.100;
    final static double maxOffsetTrack_km = 0.030;
    final static int outOfTrackCountAlarm = 10;
    final static int alarmInterval = 100;
    final static int maxAlarms = 10;

    /**
     * important app data for saving/restoring the application state after relaunching the app on
     * Android device
     * @see AppState
     */

    /** index of the track point from which to search for the nearest GPS location */
    private int _startGpsIndex = 0;
    private locationStatus _locationStatus = locationStatus.INITIAL;
    private locationStatus _initualLocationStatus = locationStatus.INITIAL;
    private boolean _initualTrackingStatus = false;

    int remainBreakTime_min = 0;
    long _endBreakTime = 0;
    boolean raiseAlarm = true;
    int outOfTrackCount = 0;
    int alarmCount = 0;
    int alarmTtsCount = 0;

    private double dist_from_start = 0.0;

    public Time CurrentTime = new Time();

    /** timeout counter to detect GPS location timeout */
    private int _timerGps_ms = 0;
    final static int _timeoutGps_ms = 3000;
    int _timerPeriod_ms;


    /** index of the lst place to search for the nearest GPS location */
    private int endIndex = 0;

    /** max. allowed offset between a track point and the GPS location */
    private double maxOffset_km = maxOffsetStart_km;
    /** nearest distance of the track point to the received GPS location */
    private double nearestDistance = 0.0;

    /** start time of the tour in UTC milliseconds since the epoch.*/
    private long _startTime_ms = 0;

    private double _distanceAtBreak = 0.0;

    private boolean _updateRecordAdapter = false;
    /** Request to update the status of the navigation */
    private boolean _updateStatus = false;

    /** Request to update the status of the GPS */
    private  boolean _updateGpsStatus = false;

    private gpsStatus _newGpsStatus = gpsStatus.NOT_REGISTERED;
    private gpsStatus _GpsStatus = gpsStatus.NOT_REGISTERED;
    private static boolean _destinationReached = false;

    public static final int TASK_COMPLETE = 4;

    // An object that manages Messages in a Thread
    private Handler mainHandler;


    /**
     * Constructor of the class
     */
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocationService.locationActivity = this;

        /* Create a Handler object that's attached to the UI thread */
        mainHandler = new Handler(Looper.getMainLooper()) {
            /**
             * Defines the operations to perform when the handler receives a new Message to process.
             * @param inputMessage incoming Message object
             */
            @Override
            public void handleMessage(Message inputMessage) {
                // The decoding is done
                if (inputMessage.what == TASK_COMPLETE) {
                    notifyGpsFileLoaded();
                } else {
                    // Pass along other messages from the UI
                    super.handleMessage(inputMessage);
                }
            }
        };

        /* Create TextToSpeech */
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if(status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.getDefault());
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        super.tourDetails = new TourDetails(this, super.app, recordAdapter);

        /* Check if the ACCESS_FINE_LOCATION permission has been granted */
        requestPermissionsIfNeeded();

        setLocationStatus(locationStatus.NO_GPX_FILE_LOADED);

        // forced recreation by system ?
        if (savedInstanceState != null) {
            // AppState.getValues(savedInstanceState);
            _startGpsIndex = AppState.getStartGpsIndex();
            setGpsStatus(AppState.getGpsStatus());
            _initualLocationStatus = AppState.getLocationStatus();
            _initualTrackingStatus = AppState.isTracking();
        }
    }

    /**
     * The system can drop the activity from memory by simply killing its process, making it destroyed.
     * When it is displayed again to the user, it must be completely restarted and restored to its previous state
     * see <a href="https://developer.android.com/reference/android/app/Activity#onSaveInstanceState(android.os.Bundle)">
     Activity Lifecycle</a> on developer.android.com
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Called when the operating system has determined that it is a good time for a process to trim
     * unneeded memory from its process.
     * @param level int: The context of the trim
     * @link <a href="https://developer.android.com/reference/android/content/ComponentCallbacks2#onTrimMemory(int)">
    developer.android.com</a>
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        AppState.trimMemoryLevel = level;
        String msg = "onTrimMemory level = ";
        if (level == TRIM_MEMORY_UI_HIDDEN)
                /* the process had been showing a user interface, and is no longer doing so. Large allocations
                   with the UI should be released at this point to allow memory to be better managed. */
            Log.w("MEMORY", msg + "TRIM_MEMORY_UI_HIDDEN");
        else if (level == TRIM_MEMORY_BACKGROUND)
                /* the process has gone on to the LRU list. This is a good opportunity to clean up resources
                   that can efficiently and quickly be re-built if the user returns to the app. */
            Log.w("MEMORY", msg + "TRIM_MEMORY_BACKGROUND");
        else
            /* all other values are depreciated in API level 35 */
            Log.e("MEMORY", msg + String.valueOf(level));
    }

    @Override
    /**
     * This method is called when the app is no longer in the foreground and is partially visible.
     * This can happen when the user switches to another app or when the screen is turned off.
     * onPause() is a good place to save any unsaved data or state changes before the app is paused.
     */
    public void onPause() {
        super.onPause();
        if (gpsSimulation != null) {
            AppState.setGpxSimulationIndex(gpsSimulation.getGpsIndex());
        }
        // remember current app state
        AppState.setGpsStatus(_GpsStatus);
        AppState.setLocationStatus(_locationStatus);
        AppState.setStartGpsIndex(_startGpsIndex);
        AppState.setTracking(control.isTracking());
    }

    @Override
    public void onResume() {
        super.onResume();
        raiseAlarm = sharedPref.getBoolean("pref_hiking_par_alarm",true);
        showAlarmPreference ();
//        if (gpsSimulation != null) gpsSimulation.Reset(AppState.getGpxSimulationIndex());
    }

    @Override
    /**
     * Prevent closing the App here when user pressed the Back key
     * @see https://developer.android.com/guide/components/activities/tasks-and-back-stack
     */
    public void onBackPressed()
    {
        control.setExpandViewStatus(false);
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Public methods
     * --------------------------------------------------------------------------------------------
     */

    /**
     * Set the first track point used for navigation
     */
    public void resetGpsIndex() {
        setStartGpsIndex(0);
    }

    /**
     * Set the first track point used for navigation
     * @param inIndex index of the track point
     */
    public void setStartGpsIndex(int inIndex)
    {
        //  if (DEBUG) Log.d(TAG,"setStartGPSindex("+inIndex+")");
        _startGpsIndex = inIndex;
        if (inIndex == 0) {
            recordAdapter.resetEndPlace();
            endIndex = 1;
        }
        /* update profile */
        super.profileAdapter.setCursor (inIndex);
    }

    /**
     * Handles state messages for a particular task object
     * @param gpsTask A task object
     * @param state   The state of the task
     */
    public void handleState(App gpsTask, int state) {
        // The task finished downloading and decoding the image
        if (state == TASK_COMPLETE) {
            // Gets a Message object, stores the state in it, and sends it to the Handler
            Message completeMessage = mainHandler.obtainMessage(state, gpsTask);
            completeMessage.sendToTarget();

            profileAdapter.initPlot();
            control.setupUserInterface();
        }
        else
            // In all other cases, pass along the message without any other action.
            mainHandler.obtainMessage(state, gpsTask).sendToTarget();
    }

    /**
     * Refresh the timetable with actual position information from GPS location provider
     *
     * @param inGPStime   time stamp [ms] from system or GPS simulation file
     * @param inLatitude  GPS latitude
     * @param inLongitude GPS longitude
     * @param inAccuracy  horizontal accuracy [m] / 0
     */
    public void handleGpsData(Time inGPStime, double inLatitude, double inLongitude, float inAccuracy) {
        recordAdapter.setRealtime(true);

        _timerGps_ms = 0;
        if (inAccuracy < 30.0)
            setGpsStatus(gpsStatus.GPS_FIX);
        else
            setGpsStatus(gpsStatus.WAIT_FOR_GPS_FIX);

        if (recordAdapter.getCount() > 0) {
            switch (_locationStatus) {
                case NO_GPX_FILE_LOADED:
                case GPX_FILE_LOADED:
                case WAIT_USER_START:
                    if (gpsSimulation != null)
                        gpsSimulation.Reset();
                    requestStatusUpdate();
                    break;

                case GOTO_START_POS:
                    maxOffset_km = maxOffsetStart_km;
                    break;
                case TRACKING:
                    maxOffset_km = maxOffsetTrack_km;
                    break;
                case APPROACHING:
                    maxOffset_km = maxOffsetTrack_km;
                    break;
                case OUT_OF_TRACK:
                    maxOffset_km = maxOffsetStart_km;
                    break;

                case DESTINATION_REACHED:
                case DESTINATION_FAILED:
                default:
                    break;
            }

            switch (_locationStatus) {
                case GOTO_START_POS:
                case TRACKING:
                case APPROACHING:
                case OUT_OF_TRACK: {
                    /* limit search to end of a record */
                    endIndex = recordAdapter.getEndIndex(endIndex);

                    /* Find nearest track point to the received GPS location */
                    int nearestGPSindex = super.app.getNearestTrackpointIndex(_startGpsIndex, endIndex, inLatitude, inLongitude, maxOffset_km);
                    /* Return the nearest distance of this track point to the received GPS location */
                    nearestDistance = super.app.getNearestDistance();

                    if (DEBUG) {
                        String simGPSindex = (gpsSimulation != null) ? ", simGPSindex = " + gpsSimulation.getGpsIndex() : "";
                        Log.d(TAG, "HandleGPSdata(): _locationStatus = " + getLocationStatus() +
                                ", place =  + startPlace" + simGPSindex +
                                ", startGPSindex = " + _startGpsIndex + ", endIndex = " + endIndex +
                                ": nearestGPSindex = " + nearestGPSindex +
                                ", nearestDist = " + (int) (nearestDistance * 1000.0) + " m");
                    }
                    // no GPS location found ?
                    if (nearestGPSindex == DataPoint.INVALID_INDEX)
                        // extend the search by one location
                        endIndex++;
                        // nearby GPS location found ?
                    else if (nearestGPSindex >= 0)
                        // handle it
                        handlePosition(nearestGPSindex, inGPStime);
                    else {
                        // no valid GPS location found - depending on the location status:
                        switch (_locationStatus) {
                            case OUT_OF_TRACK:
                                // extend the search by one location
                                endIndex++;
                            case GOTO_START_POS:
                            case DESTINATION_REACHED:
                                requestStatusUpdate();
                                break;
                            default:
                                setLocationStatus(locationStatus.OUT_OF_TRACK);
                        }
                    }
                }
                break;
            }
        }
        else
            // invalid!
            setLocationStatus(locationStatus.INITIAL);
    }

    /**
     * Notify the application of the changed start time of the tour
     */
    protected void onStartTimeChanged(Time inStartTime) {
        recordAdapter.setStartTime(inStartTime);
        recordAdapter.notifyDataSetChanged();

        _startTime_ms = recordAdapter.getStartTime().toMillis(true);
        requestStatusUpdate();
    }


    public boolean isTrackingEnabled() {
        switch (_locationStatus) {
            case WAIT_USER_START:
            case GOTO_START_POS:
            case TRACKING:
            case APPROACHING:
            case OUT_OF_TRACK:
            case DESTINATION_REACHED:
                return true;
            default:
        }
        return false;
    }

    /**
     * Request to update the status of the navigation
     */
    public void requestStatusUpdate() {
        _updateStatus = true;
    }

    /**
     * Notify the application of a loaded GPX file
     */
    public void notifyGpsFileLoaded() {
        _startTime_ms = SettingsActivity.getStartTime();
        if (_startTime_ms > 0) recordAdapter.setStartTime(_startTime_ms);
        recordAdapter.notifyDataSetChanged();
        setLocationStatus(locationStatus.GPX_FILE_LOADED);
    }


    /*
     * --------------------------------------------------------------------------------------------
     * Private methods
     * --------------------------------------------------------------------------------------------
     */

    protected void runner () {
        _timerGps_ms += _timerPeriod_ms;

        if (DEBUG) Log.d(TAG,"runner");
        recordAdapter.onDataSetChanged();
        if (_updateRecordAdapter) {
            recordAdapter.notifyDataSetChanged();
            _updateRecordAdapter = false;
        }

        // Request to update the status of the navigation ?
        if (_updateStatus)
            onUpdateStatus();

        if (control.isTracking())
            checkGpsStatus();

        if (_updateGpsStatus)
            onUpdateGpsStatus();

        gpsStatus prevGpsStatus = _GpsStatus;
        if (control.isTracking() && (gpsSimulation != null)) {
            /** Handle GPS simulation */
            Location location = gpsSimulation.getLocation();
            if (location != null) {
                /* use time from GPS simulation data */
                CurrentTime.set(location.getTime());
                handleGpsData(CurrentTime, location.getLatitude(), location.getLongitude(), 5);
            }
        } else if (prevGpsStatus != gpsStatus.GPS_FIX) {
            requestStatusUpdate();
        }

        switch (_GpsStatus) {
            case PERMISSION_DENIED:
            case PROVIDER_DISABLED:
            case PROVIDER_ENABLED:
            case WAIT_FOR_GPS_FIX:
                requestStatusUpdate();
                break;
            case GPS_FIX:
//                   requestStatusUpdate();
                break;
            default:
        }
        _timerPeriod_ms = 100 * (gpsSimulation != null ? 1 : 10);
    }

    private boolean getPermissionsGranted() {
        int perm1 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int perm2 = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        return (perm1 == PackageManager.PERMISSION_GRANTED) && (perm2 == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermissionsIfNeeded() {
        /* Check if the ACCESS_FINE_LOCATION permission has been granted */
        if (getPermissionsGranted())
            setGpsStatus(gpsStatus.PERMISSION_GRANTED);
        else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);

            /* ⚠️ Do NOT request both at the same time or background permission will be denied. */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        101
                );
            }
            setGpsStatus(gpsStatus.PERMISSION_DENIED);
        }
    }

    static locationStatus _prevLocationStatus = locationStatus.DESTINATION_FAILED;

    /**
     * Update the status of the navigation
     */
    private void onUpdateStatus() {
        control.updateTrackingStatus();

        String time;
        String activity ="";
        int bgColor = COLOR_RED;

        switch (_locationStatus) {
            case NO_GPX_FILE_LOADED: {
                if (_prevLocationStatus != _locationStatus) {
                    if (DEBUG) Log.i(TAG,"onUpdateStatus() _locationStatus = NO_GPX_FILE_LOADED");
                    activity = getString(R.string.load_gpx_file);
                    bgColor = COLOR_MESSAGE;
                    break;
                } else
                    return;
            }

            case GPX_FILE_LOADED: {
                if (_prevLocationStatus != _locationStatus)
                    if (DEBUG) Log.i(TAG,"onUpdateStatus() _locationStatus = GPX_FILE_LOADED");
                _destinationReached = false;
                resetStartGpsIndex(_startGpsIndex);
                // don't show real time data in places list view
                recordAdapter.setRealtime(false);
                if (_startTime_ms == 0) {
                    activity = getString(R.string.set_start_time);
                    bgColor = COLOR_MESSAGE;
                    break;
                }
                if (_initualTrackingStatus) {
                    control.setTrackingStatus(true);
                    _initualTrackingStatus = false;
                }
                if ((_initualLocationStatus != locationStatus.INITIAL) &&
                        (_initualLocationStatus != locationStatus.WAIT_USER_START)) {
                    setLocationStatus(_initualLocationStatus);
                    _initualLocationStatus = locationStatus.INITIAL;
                    break;
                }
               // todo else
                    setLocationStatus(locationStatus.WAIT_USER_START);
                // continue ...
            }

            case WAIT_USER_START: {
                if (_prevLocationStatus != _locationStatus)
                    Log.i(TAG,"onUpdateStatus() _locationStatus = WAIT_USER_START");
                activity = getString(R.string.wait_user_start);
                bgColor = COLOR_MESSAGE;
                if (!control.isTracking())
                    break;
                else {
                    // show first place
                    int startPlace = recordAdapter.getInitialPlace();
                    recordAdapter.setPlace(startPlace, false);
                    /* start tracking from first track point */
                    if (recordAdapter.getCount() > startPlace)
                        setStartGpsIndex(super.app.getPointIndex(recordAdapter.getItem(startPlace).getTrackPoint()));
                    setLocationStatus(locationStatus.GOTO_START_POS);
                }
                // continue ...
            }
            case GOTO_START_POS: {
                if (_prevLocationStatus != _locationStatus)
                    if (DEBUG) Log.i(TAG,"onUpdateStatus() _locationStatus = GOTO_START_POS");
                _destinationReached = false;
                if (!control.isTracking())
                    break;
                activity = getString(R.string.goto_start_pos);
                if (_GpsStatus == gpsStatus.GPS_FIX) {
                    activity = activity + getNearestDistance();
                    bgColor = COLOR_MESSAGE;
                } else {
                    activity = getString(R.string.gps_fix_lost);
                    bgColor = COLOR_NO_GPX;
                }
                break;
            }
            case OUT_OF_TRACK: {
                if (_prevLocationStatus != _locationStatus)
                    if (DEBUG) Log.i(TAG,"onUpdateStatus() _locationStatus = OUT_OF_TRACK");
                if (!control.isTracking())
                    break;
                activity = getString(R.string.return_to_track);
                bgColor = COLOR_OUT_OF_TRACK;
                if (_GpsStatus == gpsStatus.GPS_FIX)
                    activity = activity + getNearestDistance();
                else
                    if (!_destinationReached)
                        activity = getString(R.string.gps_fix_lost);

                if (raiseAlarm) {
                    outOfTrackCount++;
                    if ((outOfTrackCount > outOfTrackCountAlarm) && (alarmCount < maxAlarms)) {
                        // play alarm?
                        if (alarmTtsCount == 0) {
                            tts.speak(getString(R.string.return_to_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                            alarmCount++;
                        }
                        if (alarmTtsCount++ > alarmInterval)
                            alarmTtsCount = 0;
                    }
                }
                break;
            }

            case APPROACHING: {
                if (_prevLocationStatus != _locationStatus)
                    if (DEBUG) Log.i(TAG,"onUpdateStatus() _locationStatus = APPROACHING");
                if (!control.isTracking())
                    break;
                activity = getString(R.string.return_to_track);
                bgColor = COLOR_APPROACHING;
                if (_GpsStatus == gpsStatus.GPS_FIX)
                    activity = activity + getNearestDistance();
                else
                if (!_destinationReached)
                    activity = getString(R.string.gps_fix_lost);
                break;
            }

            case TRACKING: {
                if (_prevLocationStatus != _locationStatus)
                    if (DEBUG) Log.i(TAG,"onUpdateStatus() _locationStatus = TRACKING");
                if (!control.isTracking())
                    break;
                if (remainBreakTime_min > 0) {
                    activity = getString(R.string.pausing) + remainBreakTime_min + " min.";
                    bgColor = COLOR_TRACKING;
                }
                else {
                    if (_GpsStatus == gpsStatus.GPS_FIX) {
                        int delay = recordAdapter.getDelay();
                        activity = getString(R.string.on_track);
                        if (delay >= DELAY_MAX) {
                            bgColor = COLOR_DELAY_MAX;
                        } else if (delay > DELAY_MIN) {
                            bgColor = COLOR_DELAY_MIN;
                        } else {
                            bgColor = COLOR_TRACKING;
                        }
                    } else {
                        if (!_destinationReached) {
                            activity = getString(R.string.gps_fix_lost);
                            bgColor = COLOR_NO_GPX;
                        }
                    }
                }
                if (outOfTrackCount > outOfTrackCountAlarm)
                    tts.speak(getString(R.string.on_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                outOfTrackCount = 0;
                alarmCount = 0;
                alarmTtsCount = 0;

                break;
            }

            case DESTINATION_REACHED: {
                if (_prevLocationStatus != _locationStatus)
                    if (DEBUG) Log.i(TAG,"onUpdateStatus() _locationStatus = DESTINATION_REACHED");
                _destinationReached = true;
                activity = getString(R.string.dest_reached);
                bgColor = COLOR_DESTINATION_REACHED;
                break;
            }

            case DESTINATION_FAILED: {
                if (_prevLocationStatus != _locationStatus)
                    if (DEBUG) Log.i(TAG,"onUpdateStatus() _locationStatus = DESTINATION_FAILED");
                _destinationReached = true;
                activity = getString(R.string.dest_failed);
                bgColor = COLOR_DESTINATION_FAILED;
                break;
            }

            default: {
                activity = "";
                if (DEBUG) Log.e(TAG,"onUpdateStatus(): invalid value of _locationStatus = " + _locationStatus);
            }
        }

        if (!control.getExpandViewStatus()) {
            if (!isErrorMessage()) {
                if (!activity.isEmpty()) {
/*
                    // show current system time
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss ");
                    Calendar calender = Calendar.getInstance();
                    calender.setTimeInMillis(CurrentTime.toMillis(true));
                    time = sdf.format(calender.getTime());

                    /** @todo 00:00:00 Die Tour kann starten! */
                    // if (_GpsStatus == gpsStatus.GPS_FIX)
                   // activity = time + activity;

                    setTitleText(activity,bgColor);
                }
            }
            if (control.isTracking())
                recordAdapter.scrollToListPosition();
            recordAdapter.notifyDataSetChanged();
        }
        _prevLocationStatus = _locationStatus;
        _updateStatus = false;
// todo        if (DEBUG) Log.d(TAG,"onUpdateStatus end");
    }

    /**
     * Update the status of the GPS location provider
     */
    private void onUpdateGpsStatus() {
        if (_newGpsStatus != _GpsStatus) {
            if (DEBUG) Log.d (TAG, "onUpdateGpsStatus() _gpsStatus = " + _GpsStatus);
            control.showGpsStatus(_newGpsStatus);
            _GpsStatus = _newGpsStatus;
        }
        _updateGpsStatus = false;
    }

    /**
     * Set the status of the GPS location provider
     * @param inGpsStatus status of the GPS location provider
     */
    public void setGpsStatus(gpsStatus inGpsStatus) {
        if (inGpsStatus != _GpsStatus) {
            switch (inGpsStatus) {
                case PERMISSION_DENIED:
                case PERMISSION_GRANTED:
                case PROVIDER_DISABLED:
                case PROVIDER_ENABLED:
                case WAIT_FOR_GPS_FIX:
                case GPS_FIX:
                    _newGpsStatus = inGpsStatus;
                    _updateGpsStatus = true;
                    break;
                default:
                    switch (_GpsStatus) {
                        case WAIT_FOR_GPS_FIX:
                        case GPS_FIX:
                            if (inGpsStatus == gpsStatus.GPS_TIMEOUT) {
                                _newGpsStatus = inGpsStatus;
                                _updateGpsStatus = true;
                            }
                            break;
                    }
            }

        }
    }

    private void checkGpsStatus() {
        switch (_GpsStatus) {
            case PERMISSION_DENIED:
                if (getPermissionsGranted())
                    setGpsStatus(gpsStatus.PERMISSION_GRANTED);
                break;
            case WAIT_FOR_GPS_FIX:
            case GPS_FIX:
                if (_timerGps_ms > _timeoutGps_ms){
                    _timerGps_ms = 0;
                    setGpsStatus(gpsStatus.GPS_TIMEOUT);
                }
                requestStatusUpdate();
                break;
            case PROVIDER_DISABLED:
            case PROVIDER_ENABLED:
            default:
        }
    }

    /**
     * Change the status of the navigation
     * @param inStatus new status of the navigation
     */
    private void setLocationStatus(locationStatus inStatus) {
        if (_locationStatus != inStatus)
            _locationStatus = inStatus;
        if (DEBUG) Log.d(TAG,"setLocationStatus");
        requestStatusUpdate();
    }

    public void resetLocationStatus() {
        if (gpsSimulation != null)
            gpsSimulation.Reset();
        setLocationStatus(LocationActivity.locationStatus.GOTO_START_POS);
    }

    /**
     * Get the status of the navigation
     * @return status string
     */
    private String getLocationStatus() {
        String[] locationStatusStr = {
                "INITIAL",
                "NO_GPX_FILE_LOADED",
                "GPX_FILE_LOADED",
                "WAIT_USER_START",
                "GOTO_START_POS",
                "TRACKING",
                "APPROACHING",
                "OUT_OF_TRACK",
                "DESTINATION_REACHED",
                "DESTINATION_FAILED" };

        if (_locationStatus.ordinal() <= locationStatus.DESTINATION_FAILED.ordinal() ) {
            String s = locationStatusStr[_locationStatus.ordinal()];
            return s;
        } else
            return "INVALID";
    }

    /**
     * Handle a nearby found GPS location
     *
     * @param inPosition index of the nearest track point
     * @param inTime     local time from the GPS receiver
     */
    private void handlePosition(int inPosition, Time inTime) {
        // use this position to start navigation
        setStartGpsIndex(inPosition);

        DataPoint point = super.app.getPoint(inPosition);
        if (point != null) {
            /* Show current distance since start and the remaining distance to destination */
            dist_from_start = point.getDistance();
            double dist_to_destin = trackTiming.getTotalDistance() - dist_from_start;
            showDistances(dist_from_start);
            recordAdapter.setDistance(dist_from_start);

            /* Calculate the time shift between GPS and expected arrival time of the current point */
            long destTime_s = point.getTime();
            int place = recordAdapter.getPlace();
            boolean setNextPlace = true;
            if ((_startTime_ms > 0) && ((inPosition == 0) || (destTime_s > 0))) {

                // calculate delay
                long delay_s;
                if (gpsSimulation != null) {
                    // ignore the day in simulation
                    long gpsTime_s = inTime.second + 60 * (inTime.minute + 60L * inTime.hour);
                    Time start = recordAdapter.getStartTime();
                    long start_Time_s = (start.minute + 60L * start.hour) * 60L;
                    delay_s = gpsTime_s - (start_Time_s + destTime_s);
                    /*
                    if (DEBUG) {
                        Log.d(TAG, "handlePosition() - gpsTime_s  = " + gpsTime_s);
                        Log.d(TAG, "                 - destTime_s = " + destTime_s);
                        Log.d(TAG, "                 - distance   = " + dist_from_start);
                        Log.d(TAG, "                 - delay_min  = " + delay_s / 60);
                    }
                     */
                } else {
                    long gpsTime_ms = inTime.toMillis(true);
                    delay_s = (gpsTime_ms - _startTime_ms) / 1000;
                    delay_s = (delay_s - destTime_s);
                }

                if (remainBreakTime_min <= 0)
                {
                    recordAdapter.setDelay((int) (delay_s / 60));

                    // handle break
                    int breakTime_min = point.getWaypointDuration();
                    // don't leave the place in case of break
                    if (breakTime_min > 0) {
                        _distanceAtBreak = dist_from_start;

                        // are we within our timetable?
                        remainBreakTime_min = breakTime_min - (int)delay_s / 60;

                        if (remainBreakTime_min > 0)
                        {
                            // calc time stamp of end of break
                            _endBreakTime = inTime.toMillis(true) + (long) remainBreakTime_min * 60000L;
                            setNextPlace = false;
                        }
                    }
                }
                else
                {
                    /* calc remaining time of break */
                    long remainBreakTime_ms = _endBreakTime - inTime.toMillis(true);
                    if (remainBreakTime_ms > 0)
                        remainBreakTime_min = (int)(remainBreakTime_ms / 60000L);
                    else{
                        remainBreakTime_min = 0;
                        setNextPlace = true;
                    }

                    // don't leave the place in case of break
                    // are we nearby?
                    if (dist_from_start < _distanceAtBreak + maxOffsetStart_km)
                        setNextPlace = false;
                }
            }

            /* Set next place to arrive after current distance */
            if (setNextPlace)
            {
                _distanceAtBreak = 0.0;
                remainBreakTime_min = 0;
                place = recordAdapter.setNextPlace(dist_from_start,false);
            }
            if (place < recordAdapter.getCount()) {
                recordAdapter.notifyDataSetChanged();
                // destination reached?
                if (dist_to_destin <= maxOffsetTrack_km)
                    setLocationStatus(locationStatus.DESTINATION_REACHED);
                else
                    switch (_locationStatus) {
                        case GOTO_START_POS:
                        case TRACKING:
                        case APPROACHING:
                            setLocationStatus(locationStatus.TRACKING);
                            break;
                        case OUT_OF_TRACK:
                            setLocationStatus(locationStatus.APPROACHING);
                            break;
                        case DESTINATION_REACHED:
                        default:
                            requestStatusUpdate();
                    }
            } else {
                setLocationStatus(locationStatus.DESTINATION_REACHED);
            }
        }
    }

    /**
     * Show alarm setting (On/Off)
     */
    private void showAlarmPreference()
    {
        ImageView image_alarm_off = findViewById(R.id.image_alarm_off);
        ImageView image_alarm_on  = findViewById(R.id.image_alarm_on);

        if (raiseAlarm)
        {
            image_alarm_off.setVisibility(View.GONE);
            image_alarm_on.setVisibility(View.VISIBLE);
        }
        else
        {
            image_alarm_off.setVisibility(View.VISIBLE);
            image_alarm_on.setVisibility(View.GONE);
        }
    }

    private void resetStartGpsIndex(int inIndex) {
        _updateRecordAdapter = true;
        int place = recordAdapter.indexOf(inIndex);
        recordAdapter.setPlace(place, false);
        setStartGpsIndex(inIndex);
    }

    /**
     * @return a formatted string of the nearest distance from the current track point to the GPS location
     */
    private String getNearestDistance() {
        String distance;
        if (nearestDistance > 1.0) {
            distance = new DecimalFormat("#0.00").format(nearestDistance) + " km";
        } else
        {
            distance = new DecimalFormat("#0").format(nearestDistance*1000.0) + " m";
        }
        return " (" + distance + ")";
    }


    /** Show the current distance since start and the remaining distance to destination
     *
     * @param inDistance current distance
     */
    private void showDistances(double inDistance) {
        TextView distanceView = findViewById(R.id.track_distance);
        distanceView.setText(new DecimalFormat("#0.00 km").format(inDistance));

        double dist_to_destin = trackTiming.getTotalDistance() - inDistance;
        TextView dist_to_destinView = findViewById(R.id.track_dist_to_dest);
        dist_to_destinView.setText(new DecimalFormat("#0.0 km").format(dist_to_destin));
    }
}