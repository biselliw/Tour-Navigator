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
    If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.LocationService;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.data.AppState;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.data.TrackSegments;
import de.biselliw.tour_navigator.functions.LocationHandler;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.helpers.Prefs;
import de.biselliw.tour_navigator.ui.ControlElements;

import static de.biselliw.tour_navigator.Notifications.ACTION_LOCATION_UPDATE;
import static de.biselliw.tour_navigator.activities.LocationActivity.locationStatus.DESTINATION_FAILED;
import static de.biselliw.tour_navigator.activities.LocationActivity.locationStatus.GOTO_START_POS;
import static de.biselliw.tour_navigator.activities.LocationActivity.locationStatus.INITIAL;
import static de.biselliw.tour_navigator.activities.LocationActivity.locationStatus.WAIT_USER_START;
import static de.biselliw.tour_navigator.adapter.RecordAdapter.COLOR_DELAY_MAX;
import static de.biselliw.tour_navigator.adapter.RecordAdapter.COLOR_DELAY_MIN;
import static de.biselliw.tour_navigator.adapter.RecordAdapter.DELAY_MAX;
import static de.biselliw.tour_navigator.adapter.RecordAdapter.DELAY_MIN;
import static de.biselliw.tour_navigator.data.AppState.gpsSimulation;
import static de.biselliw.tour_navigator.functions.LocationHandler.INVALID_DISTANCE;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_APPROACHING;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_BREAK;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_DESTINATION_REACHED;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_GOTO_START;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_IDLE;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_OUT_OF_TRACK;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_TRACKING;
import static de.biselliw.tour_navigator.functions.LocationHandler.maxOffsetPOI_km;

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
        BREAK,
        DESTINATION_REACHED,
        DESTINATION_FAILED
    }

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


    private static final int COLOR_RED                  = R.color.red;
    private static final int BG_COLOR                   = R.color.md_theme_surface;
    private static final int BG_COLOR_MESSAGE           = BG_COLOR;
    private static final int COLOR_TRACKING             = R.color.tracking;
    private static final int COLOR_APPROACHING          = R.color.approaching;
    private static final int COLOR_OUT_OF_TRACK        = COLOR_RED;
    private static final int COLOR_DESTINATION_REACHED = COLOR_TRACKING;
    private static final int COLOR_DESTINATION_FAILED  = COLOR_RED;

    /**
     * important app data for saving/restoring the application state after relaunching the app on
     * Android device
     * @see AppState
     */

    private locationStatus _locationStatus = INITIAL;
    private locationStatus _initialLocationStatus = INITIAL;
    private locationStatus _newLocationStatus = INITIAL;
    private boolean _initialTrackingStatus = false;

    /** timeout counter to detect GPS location timeout */
    private int _timerGps_ms = 0;
    final static int _timeoutGps_ms = 10000;

    /** timeout counter to warn "out of track" */
    private int _timerOutOfTrack_ms = 0;
    final static int MAX_ALARMS_OUT_OF_TRACK = 10;
    final static int TIMEOUT_OUT_OF_TRACK_ALARM_MS = 10000;
    final static int INTERVAL_OUT_OF_TRACK_ALARM_MS = 10000;
    private int _counterOutOfTrackAlarms = 0;

    /**
     * start time of the tour in [min] since midnight
     */
    private int _startTime_min = 0;


    private boolean _updateLogTimerGPS = false;
    private boolean _updateRecordAdapter = false;
    /** Request to update the status of the navigation */
    private boolean _updateStatus = false;

    /** Request to update the status of the GPS */
    private static boolean _updateGpsStatus = false;

    private static gpsStatus _newGpsStatus = gpsStatus.NOT_REGISTERED;
    private static gpsStatus _GpsStatus = gpsStatus.NOT_REGISTERED;
    public static final int TASK_COMPLETE = 4;

    // An object that manages Messages in a Thread
    private Handler mainHandler;

    private BroadcastReceiver locationReceiver = null;

    /**
     * Constructor of the class
     * @deprecated FIXME getParcelableExtra(java.lang.String)' is deprecated as of API 33 ("Tiramisu"; Android 13.0)
     */
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestForegroundPermissionIfNeeded();

        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // handle the intent ACTION_LOCATION_UPDATE
// todo               if (intent.getIdentifier().equals(ACTION_LOCATION_UPDATE))
                Log.i(TAG,"Location received via intent");

                Location location = intent.getParcelableExtra("location");
                if (location != null)
                    handleGpsData(location.getAccuracy());
            }
        };
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        super.tourDetails = new TourDetails(this, app, recordAdapter);

        /* Check if the ACCESS_FINE_LOCATION permission has been granted */
         setLocationStatus(locationStatus.NO_GPX_FILE_LOADED);

        // forced recreation by system ?
        if (savedInstanceState != null) {
            // AppState.getValues(savedInstanceState);
            setStartGpsIndex(AppState.getStartGpsIndex());
            setGpsStatus(AppState.getGpsStatus());
            _initialLocationStatus = AppState.getLocationStatus();
            _initialTrackingStatus = AppState.isTracking();
        }

        /* Create a Handler object that's attached to the UI thread */
        mainHandler = new Handler(Looper.getMainLooper()) {
            /**
             * Defines the operations to perform when the handler receives a new Message to process.
             * @param inputMessage incoming Message object
             */
            @Override
            public void handleMessage(Message inputMessage) {
                if (inputMessage.what == TASK_COMPLETE) {
                    notifyGpsFileLoaded();
                } else {
                    // Pass along other messages from the UI
                    super.handleMessage(inputMessage);
                }
            }
        };

        requestLocationPermissionIfNeeded();
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
            Log.e("MEMORY", msg + level);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // register the GPS location receiver
        IntentFilter filter = new IntentFilter(ACTION_LOCATION_UPDATE);
        registerReceiver(
                locationReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onStop() {
        // unregister the GPS location receiver
        unregisterReceiver(locationReceiver);
        super.onStop();
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
        AppState.setStartGpsIndex(LocationHandler.getStartGpsIndex());
        AppState.setTracking(isTracking());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
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
        LocationHandler.setStartGpsIndex(inIndex);
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
            setupUserInterface();
        }
        else
            // In all other cases, pass along the message without any other action.
            mainHandler.obtainMessage(state, gpsTask).sendToTarget();
    }

    /**
     * Handle received GPS location data
     * @param inAccuracy horizonal accuracy of the location provider [m]
     */
    public void handleGpsData(float inAccuracy) {
        int locStatus = LocationHandler.getStatus();

        recordAdapter.setRealtime(true);
        if (inAccuracy < maxOffsetPOI_km * 1000.0)
            setGpsStatus(gpsStatus.GPS_FIX);
        else
            setGpsStatus(gpsStatus.WAIT_FOR_GPS_FIX);
        _timerGps_ms = 0;

        int index = LocationHandler.getIndex();
        int place = LocationHandler.getPlace();
        int nextPlace = LocationHandler.getNextPlace();

        /* Show distances */
        double dist_from_start = LocationHandler.getDistance();
        double dist_to_destin = TrackSegments.summary.totalDistance_km - dist_from_start;
        double dist_to_next_place = LocationHandler.getDistanceToNextPlace();
        int delay_min = LocationHandler.getDelay();

        recordAdapter.setDistance(dist_from_start);
        if (locStatus != LOC_BREAK) {
            if (nextPlace > recordAdapter.getPlace()) {
                recordAdapter.setPlace(this, nextPlace, false);
//                recordAdapter.scrollToListPosition();
            }
        }
        recordAdapter.setDelay(delay_min);
        // show activity
        showActivity(locStatus, delay_min);
        showDistances(dist_from_start, dist_to_destin, nextPlace > place, dist_to_next_place);

        profileAdapter.setCursor(index);
    }


    /**
     * Show activity
     */
    private void showActivity(int inLocationStatus, int inDelay) {
        String activity ="";
        int txtColor = R.color.colorText;
        int bgColor = COLOR_RED;
        switch (inLocationStatus) {
            case LOC_GOTO_START: {
                activity = getString(R.string.goto_start_pos);
                activity = activity + getNearestDistance();
                bgColor = BG_COLOR_MESSAGE;
                break;
            }
            case LOC_TRACKING: {
                activity = getString(R.string.on_track);
                if (inDelay >= DELAY_MAX) {
                    bgColor = COLOR_DELAY_MAX;
                } else if (inDelay > DELAY_MIN) {
                    bgColor = COLOR_DELAY_MIN;
                } else {
                    bgColor = COLOR_TRACKING;
                }
                break;
            }
            case LOC_OUT_OF_TRACK: {
                activity = getString(R.string.return_to_track);
                activity = activity + getNearestDistance();
                txtColor = R.color.white;
                bgColor = COLOR_OUT_OF_TRACK;
                if (raiseAlarm) {
                    _timerOutOfTrack_ms += timerPeriod_ms;
                    if ((_timerOutOfTrack_ms > TIMEOUT_OUT_OF_TRACK_ALARM_MS) && (_counterOutOfTrackAlarms < MAX_ALARMS_OUT_OF_TRACK)) {
                        // play alarm?
                        if (((_timerOutOfTrack_ms - TIMEOUT_OUT_OF_TRACK_ALARM_MS) % INTERVAL_OUT_OF_TRACK_ALARM_MS) == 0) {
                            tts.speak(getString(R.string.return_to_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                            _counterOutOfTrackAlarms++;
                        }
                    }
                }
                break;
            }
            case LOC_APPROACHING: {
                activity = getString(R.string.return_to_track);
                bgColor = COLOR_APPROACHING;
                activity = activity + getNearestDistance();
                break;
            }
            case LOC_BREAK: {
                int remainBreakTime_min = LocationHandler.getRemainingBreakTime();
                if (remainBreakTime_min > 0) {
                    activity = getString(R.string.pausing) + remainBreakTime_min + " min.";
                    bgColor = COLOR_TRACKING;
                }
                break;
            }
            case LOC_DESTINATION_REACHED: {
                activity = getString(R.string.dest_reached);
                bgColor = COLOR_DESTINATION_REACHED;
                break;
            }
            default: {
                activity = "";
            }
        }

        // show current system time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss ");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String time = sdf.format(calendar.getTime());

        // add real time, ex. 00:00:00 xxx
        activity = time + activity;
        setTitleText(activity, txtColor, bgColor);
    }

    /**
     * Show activity
     */
    private void showActivity(locationStatus inLocationStatus) {
        String activity;
        int bgColor;

        switch (inLocationStatus) {
            case NO_GPX_FILE_LOADED: {
                activity = getString(R.string.load_gpx_file);
                bgColor = BG_COLOR_MESSAGE;
                break;
            }
            case GPX_FILE_LOADED: {
                if (_startTime_min == 0) {
                    activity = getString(R.string.set_start_time);
                    bgColor = BG_COLOR_MESSAGE;
                }
                else
                    activity = ""; bgColor = 0;
                break;
            }
            case WAIT_USER_START: {
                if (gpsSimulation != null)
                    gpsSimulation.Reset();
                if (_startTime_min == 0)
                    activity = getString(R.string.set_start_time);
                else if (!getPermissionsGranted()) {
                    activity = getString(R.string.permit_location);
                    requestLocationPermissionIfNeeded();
                } else
                    activity = getString(R.string.wait_user_start);
                bgColor = BG_COLOR_MESSAGE;
                break;
            }
            case DESTINATION_FAILED: {
                activity = getString(R.string.dest_failed);
                bgColor = COLOR_DESTINATION_FAILED;
                break;
            }
            default:
                activity = ""; bgColor = 0;
        }

        if (!activity.isEmpty()) {
            if (!getExpandViewStatus()) {
                if (!isErrorMessage()) {
                    setTitleText(activity, bgColor);
                }
            }
        }
    }

    /**
     * Notify the application of the changed start time of the tour
     * @param inStartTime start time in [min] since midnight
     */
    protected void onStartTimeChanged(int inStartTime) {
        _startTime_min = inStartTime;
        recordAdapter.setStartTime(inStartTime);
        LocationHandler.setStartTime(inStartTime);

        requestStatusUpdate();
    }

    /**
     *
     * @return true if tracking can start
     */
    public boolean isTrackingEnabled() {
        if (gpsSimulation == null && !getPermissionsGranted())
            return false;
        switch (_locationStatus) {
            case GPX_FILE_LOADED:
            case WAIT_USER_START:
                return (_startTime_min > 0);
            case GOTO_START_POS:
            case BREAK:
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
        _startTime_min = Prefs.getStartTime(this);
        if (_startTime_min > 0) {
            recordAdapter.setStartTime(_startTime_min);
            LocationHandler.setStartTime(_startTime_min);
        }
        setLocationStatus(locationStatus.GPX_FILE_LOADED);
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Private methods
     * --------------------------------------------------------------------------------------------
     */

    /**
     * callback function to periodically update the user interface
     */
    @Override
    protected void updateUI() {
        if (DEBUG) if (_updateLogTimerGPS) {
//            Log.d(TAG,"runner: _timerGps_ms = " + _timerGps_ms);
            _updateLogTimerGPS = false;
        }
        _timerGps_ms += timerPeriod_ms;

        recordAdapter.onDataSetChanged();
        if (_updateRecordAdapter) {
            recordAdapter.notifyDataSetChanged();
            _updateRecordAdapter = false;
        }

        // Request to update the status of the navigation ?
        if (_updateStatus) {
            onUpdateStatus();
        }

        if (isTracking())
            checkGpsStatus();

        if (_updateGpsStatus) {
            if (DEBUG) _updateLogTimerGPS = true;
            onUpdateGpsStatus();
        }
/*
        if (LocationHandler.resultsUpdated()) {
            updateLocationResults();
        }
        gpsStatus prevGpsStatus = _GpsStatus;
*/
        if (isTracking()) {
            if (gpsSimulation != null) {
                /** Handle GPS simulation */
                LocationHandler.handleLocation(gpsSimulation.getLocation());
                handleGpsData(15.0F);
            }

/*
            long now = 0;
            if (_location != null) {
                now = _location.getTime();
                handleGpsData(now, _location.getLatitude(), _location.getLongitude(), _location.getAccuracy());
                requestStatusUpdate();
                _location = null;
            }
*/

        }

        switch (_GpsStatus) {
            case PERMISSION_DENIED:
            case PROVIDER_DISABLED:
            case PROVIDER_ENABLED:
            case WAIT_FOR_GPS_FIX:
                _updateLogTimerGPS = true;
                requestStatusUpdate();
                break;
            case GPS_FIX:
                break;
            default:
        }

        super.updateUI();
    }

    /** @return true if both the ACCESS_FINE_LOCATION and FOREGROUND_SERVICE_LOCATION permissions have been granted */
    private boolean getPermissionsGranted() {
        int perm1 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
//        int perm2 = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        return (perm1 == PackageManager.PERMISSION_GRANTED) ; // todo && (perm2 == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Start/Stop the foreground Location service
     *
     * @param inStart true if the GPS provider shall controls the tracking
     */
    public void startForegroundLocationService(boolean inStart) {
        try {
            if (DEBUG)
                Log.i(TAG,"startForegroundLocationService(" + inStart + ")");
            if (!inStart)
                stopService(new Intent(this, LocationService.class));
            else {
                requestForegroundPermissionIfNeeded();

                Intent intent = new Intent(this, LocationService.class);
                ContextCompat.startForegroundService(this, intent);
            }
        } catch (Exception e) {
            Log.e(TAG,"startForegroundLocationService(" + inStart + ")", e);
        }
    }

    /** Check if the FOREGROUND_SERVICE_LOCATION permission has been granted;
     * if not : request it
     **/
    public void requestForegroundPermissionIfNeeded() {
        /* Check if the FOREGROUND_SERVICE_LOCATION permission has been granted */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
           != PackageManager.PERMISSION_GRANTED) {
            /* ⚠️ Do NOT request both at the same time or background permission will be denied. */
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    101
            );
            setGpsStatus(gpsStatus.PERMISSION_DENIED);
        }
    }

    /** Check if the ACCESS_FINE_LOCATION permission has been granted;
     * if not : request it
     **/
    public void requestLocationPermissionIfNeeded() {
        /* Check if the ACCESS_FINE_LOCATION permission has been granted */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
            setGpsStatus(gpsStatus.PERMISSION_GRANTED);
        else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            setGpsStatus(gpsStatus.PERMISSION_DENIED);
        }
    }

    static locationStatus _prevLocationStatus = INITIAL;

    /**
     * Update the status of the navigation
     */
    private void onUpdateStatus() {
        updateTrackingStatus();

        if (_newLocationStatus != _locationStatus)
        {
            _updateLogTimerGPS = true;
            _locationStatus = _newLocationStatus;
            // todo check: Variable is already assigned to this value
            _newLocationStatus = _locationStatus;
        }

        if (_prevLocationStatus != _locationStatus) {
            if (DEBUG) {
                _updateLogTimerGPS = true;
                Log.d(TAG, "onUpdateStatus() changed _locationStatus: " +
                        getLocationStatus(_prevLocationStatus) + " -> " + getLocationStatus(_locationStatus));
            }
            showActivity(_locationStatus);
            switch (_locationStatus) {
                case GPX_FILE_LOADED: {
                    if ((_initialLocationStatus != INITIAL)
                            && (_initialLocationStatus != WAIT_USER_START)
                    ) {
                        if (DEBUG) Log.d(TAG, " set initial location status:");
                        setLocationStatus(_initialLocationStatus);
                        _initialLocationStatus = INITIAL;
                        if (_initialTrackingStatus) {
                            if (DEBUG) Log.d(TAG, " set initial tracking status: active");
// todo                            setTrackingStatus(true);
                            _initialTrackingStatus = false;
                        }
                    }
                    else
                        setTrackingStatus(false);
                    if (App.getTrack().isValid()) // !app.getTrack().isValidRecordedTrackFile())
                        setLocationStatus(WAIT_USER_START);
                    break;
                }
                case WAIT_USER_START: {
                    resetStartGpsIndex(LocationHandler.getStartGpsIndex());
                    // don't show real time data in places list view
                    recordAdapter.setRealtime(false);
                    break;
                }
            }
        }
        else if (isTracking()) {
            switch (_locationStatus) {
                case GPX_FILE_LOADED:
                    break;
                case WAIT_USER_START: {
                    // show first place
                    int startPlace = recordAdapter.getInitialPlace();
                    if (startPlace < 0) startPlace = 0;
                    recordAdapter.setPlace(this, startPlace);
                    /* start tracking from first track point */
                    if (recordAdapter.getCount() > startPlace)
                        setStartGpsIndex(recordAdapter.getItem(startPlace).trackPoint.getIndex());
                    setLocationStatus(GOTO_START_POS);
                    break;
                }
                case GOTO_START_POS:
                    break;
                case BREAK:
                case TRACKING: { /*
                    } */
                    if (_timerOutOfTrack_ms > TIMEOUT_OUT_OF_TRACK_ALARM_MS)
                        tts.speak(getString(R.string.on_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                    _timerOutOfTrack_ms = 0;
                    _counterOutOfTrackAlarms = 0;
                    break;
                }
                default: {
                    if (DEBUG)
                        Log.e(TAG, "onUpdateStatus(): invalid value of _locationStatus = " + _locationStatus);
                }
            }
        }
        else {
            if (Objects.requireNonNull(_locationStatus) == WAIT_USER_START) {
                showActivity(_locationStatus);
            }
        }

        _prevLocationStatus = _locationStatus;
        _updateStatus = false;
    }

    /**
     * Update the status of the GPS location provider
     */
    private void onUpdateGpsStatus() {
        if (_newGpsStatus != _GpsStatus) {
            if (DEBUG) Log.d (TAG, "onUpdateGpsStatus() _gpsStatus = " + _GpsStatus);
            _GpsStatus = _newGpsStatus;
            showGpsStatus();
        }
        _updateGpsStatus = false;
    }

    public static gpsStatus getGpsStatus() {
        return _GpsStatus;
    }

    /**
     * Set the status of the GPS location provider
     * @param inGpsStatus status of the GPS location provider
     */
    @Override
    protected void setGpsStatus(gpsStatus inGpsStatus) {
        if (inGpsStatus != _GpsStatus) {
            switch (inGpsStatus) {
                case PERMISSION_DENIED:
                case PERMISSION_GRANTED:
                case PROVIDER_DISABLED:
                case PROVIDER_ENABLED:
                case WAIT_FOR_GPS_FIX:
                case GPS_FIX:

                case GPS_TIMEOUT:
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
            requestStatusUpdate();
            super.setGpsStatus(inGpsStatus);
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
        {
            _locationStatus = inStatus; // todo
            switch (inStatus) {
                case INITIAL:
                    LocationHandler.setLocationStatus(LOC_IDLE);
                    break;
                case GOTO_START_POS:
                    LocationHandler.setLocationStatus(LOC_GOTO_START);
                    break;
            }

            if (DEBUG) Log.d(TAG,"setLocationStatus: request "+getLocationStatus(_locationStatus)+" -> "+getLocationStatus(inStatus));
            _newLocationStatus = inStatus;
            requestStatusUpdate();
        }
    }

    public void resetLocationStatus() {
        if (gpsSimulation != null)
            gpsSimulation.Reset();
        setLocationStatus(GOTO_START_POS);
    }

    /**
     * Get the status of the navigation
     * @return status string
     */
    public static String getLocationStatus(locationStatus inStatus) {
        String[] locationStatusStr = {
                "INITIAL",
                "NO_GPX_FILE_LOADED",
                "GPX_FILE_LOADED",
                "WAIT_USER_START",
                "GOTO_START_POS",
                "TRACKING",
                "APPROACHING",
                "OUT_OF_TRACK",
                "BREAK",
                "DESTINATION_REACHED",
                "DESTINATION_FAILED" };

        if (inStatus.ordinal() <= DESTINATION_FAILED.ordinal() ) {
            return locationStatusStr[inStatus.ordinal()];
        } else
            return "INVALID";
    }


    private void resetStartGpsIndex(int inIndex) {
        _updateRecordAdapter = true;
        int place = recordAdapter.indexOf(inIndex);
        recordAdapter.setPlace(this, place);
        setStartGpsIndex(inIndex);
    }

    /**
     * @return a formatted string of the nearest distance from the current track point to the GPS location
     */
    private String getNearestDistance() {
        String distance;
        double nearestDistance = LocationHandler.getNearestDistance();
        if (nearestDistance > 1.0) {
            distance = new DecimalFormat("#0.00").format(nearestDistance) + " km";
        } else
        {
            distance = new DecimalFormat("#0").format(nearestDistance*1000.0) + " m";
        }
        return " (" + distance + ")";
    }


    /** Show all distances: since start, remaining to destination, to place
     *
     * @param inDistance current distance since start
     * @param inDistanceToDestination current distance to destination
     * @param inMoving true if current place has been left and distance to next place shall be shown
     * @param inDistanceToPlace current distance to place
     */
    private void showDistances(double inDistance, double inDistanceToDestination, boolean inMoving, double inDistanceToPlace) {
        TextView distanceView = findViewById(R.id.track_distance);
        distanceView.setText(new DecimalFormat("#0.00 km").format(inDistance));

        TextView dist_to_destinView = findViewById(R.id.track_dist_to_dest);
        dist_to_destinView.setText(new DecimalFormat("#0.0 km").format(inDistanceToDestination));

        String _distanceToPlace = "";
        if (inMoving && inDistanceToPlace < INVALID_DISTANCE) {
            if (inDistanceToPlace >= 1.0)
                _distanceToPlace = new DecimalFormat("in #0.0 km: ").format(inDistanceToPlace);
            else
                _distanceToPlace = new DecimalFormat("in #0 m: ").format((int) (inDistanceToPlace * 100.0) * 10);
        }
        String comment = _distanceToPlace;
        if (additionalInfo != null)
            comment = comment + additionalInfo.comment;

        TextView commentView = findViewById(R.id.comment_view);
        commentView.setText(comment);
    }
}