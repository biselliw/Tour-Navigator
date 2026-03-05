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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.LocationService;
import de.biselliw.tour_navigator.data.AppState;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.data.TrackSegments;
import de.biselliw.tour_navigator.functions.LocationHandler;
import de.biselliw.tour_navigator.helpers.Log;
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
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_APPROACHING;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_BREAK;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_GOTO_START;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_IDLE;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_OUT_OF_TRACK;
import static de.biselliw.tour_navigator.functions.LocationHandler.LOC_TRACKING;

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

    private static Location _location = null;

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
    private static final int COLOR_NO_GPX               = COLOR_RED;
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

//    private int _delay_min = 0;
//    private long _endBreakTime = 0;
    private boolean _setNextPlace = true;

    /** timeout counter to detect GPS location timeout */
    private int _timerGps_ms = 0;
    final static int _timeoutGps_ms = 10000;

    /** timeout counter to warn "out of track" */
    private int _timerOutOfTrack_ms = 0;
    final static int MAX_ALARMS_OUT_OF_TRACK = 10;
    final static int TIMEOUT_OUT_OF_TRACK_ALARM_MS = 10000;
    final static int INTERVAL_OUT_OF_TRACK_ALARM_MS = 10000;
    private int _counterOutOfTrackAlarms = 0;

    /** index of the lst place to search for the nearest GPS location */
    private int endIndex = 0;

    private int _arrivedPlace = -1;


    /**
     * start time of the tour in [min] since midnight
     */
    private int _startTime_min = 0;

//    private double _distanceAtBreak = 0.0;

    private boolean _updateLogTimerGPS = false;
    private boolean _updateRecordAdapter = false;
    /** Request to update the status of the navigation */
    private boolean _updateStatus = false;

    /** Request to update the status of the GPS */
    private static boolean _updateGpsStatus = false;

    private static gpsStatus _newGpsStatus = gpsStatus.NOT_REGISTERED;
    private static gpsStatus _GpsStatus = gpsStatus.NOT_REGISTERED;
    private static boolean _destinationReached = false;

    public static final int TASK_COMPLETE = 4;

    // An object that manages Messages in a Thread
    private Handler mainHandler;

    private BroadcastReceiver locationReceiver = null;
//    private BroadcastReceiver locationHandler = null;

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
//                setLocation(intent.getParcelableExtra("location")); // todo

                handleGpsData(location.getAccuracy());

/*
                Location location = intent.getParcelableExtra("location");
                Intent handler = new Intent(ACTION_LOCATION_RECEIVE);
                handler.setPackage(getPackageName());   // important for implicit broadcasts
                handler.putExtra("location", location);
                sendBroadcast(handler);
 */
            }
        };
/*
        locationHandler = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // handle the intent ACTION_LOCATION_RECEIVE
                Log.i(TAG,"Location received via intent");

                setLocation(intent.getParcelableExtra("location"));
            }
        };
 */
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
                // The decoding is done
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
            Log.e("MEMORY", msg + String.valueOf(level));
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
        /* register the GPS location receiver
        filter = new IntentFilter(ACTION_LOCATION_RECEIVE);
        registerReceiver(
                locationHandler,
                filter,
                Context.RECEIVER_NOT_EXPORTED
        );
         */
    }

    @Override
    protected void onStop() {
        // unregister the GPS location receiver
        unregisterReceiver(locationReceiver);
//        unregisterReceiver(locationHandler);
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
        //  if (DEBUG) Log.d(TAG,"setStartGPSindex("+inIndex+")");
        LocationHandler.setStartGpsIndex(inIndex);
        if (inIndex == 0) {
            // recordAdapter.resetEndPlace();
            // endIndex = 1;
            // _delay_min = 0;
            _arrivedPlace = -1;
            _destinationReached = false;
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
            setupUserInterface();
        }
        else
            // In all other cases, pass along the message without any other action.
            mainHandler.obtainMessage(state, gpsTask).sendToTarget();
    }

    /**
     * Handle received GPS location data
     * @param inAccuracy horizonal accuracy of the location provider
     */
    public void handleGpsData(float inAccuracy) {
        int locStatus = LocationHandler.getStatus();

        recordAdapter.setRealtime(true);
        if (inAccuracy < 15.0)
            setGpsStatus(gpsStatus.GPS_FIX);
        else
            setGpsStatus(gpsStatus.WAIT_FOR_GPS_FIX);
        _timerGps_ms = 0;

        switch (_locationStatus) {
            case NO_GPX_FILE_LOADED:
            case GPX_FILE_LOADED:
            case WAIT_USER_START:
                if (gpsSimulation != null)
                    gpsSimulation.Reset();
                break;
        }

        // show activity
        showActivity(locStatus);

        /* Show distances */
        double dist_from_start = LocationHandler.getDistance();
        double dist_to_destin = TrackSegments.summary.totalDistance_km - dist_from_start;
        double dist_to_next_place = LocationHandler.getDistanceToNextPlace();
        showDistances(dist_from_start, dist_to_destin, dist_to_next_place);
        recordAdapter.setDistance(dist_from_start);
        int place = LocationHandler.getPlace();

        if (locStatus != LOC_BREAK) {
            int nextPlace = LocationHandler.getNextPlace();
            if (nextPlace > recordAdapter.getPlace()) {
                recordAdapter.setPlace(this, nextPlace, false);
                recordAdapter.scrollToListPosition();
            }
        }

        int index = LocationHandler.getIndex();
        profileAdapter.setCursor(index);
    }


    /**
     * Show activity
     */
    private void showActivity(int locStatus) {
        String activity ="";
        int bgColor = COLOR_RED;
        switch (locStatus) {
            case LOC_GOTO_START: {
                activity = getString(R.string.goto_start_pos);
                activity = activity + getNearestDistance();
                bgColor = BG_COLOR_MESSAGE;
                break;
            }
            case LOC_TRACKING: {
                int delay_min = LocationHandler.getDelay();
                recordAdapter.setDelay(delay_min);
                activity = getString(R.string.on_track);
                if (delay_min >= DELAY_MAX) {
                    bgColor = COLOR_DELAY_MAX;
                } else if (delay_min > DELAY_MIN) {
                    bgColor = COLOR_DELAY_MIN;
                } else {
                    bgColor = COLOR_TRACKING;
                }
                break;
            }
            case LOC_OUT_OF_TRACK: {
                activity = getString(R.string.return_to_track);
                activity = activity + getNearestDistance();
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
        setTitleText(activity, bgColor);
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
        _startTime_min = SettingsActivity.getStartTime(sharedPref);
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

    public void setLocation(Location inLocation) {
        if (gpsSimulation == null)
            _location = inLocation;
        updateUI();
    }

    /**
     * callback function to periodically update the user interface
     */
    @Override
    protected void updateUI() {
        if (DEBUG) if (_updateLogTimerGPS) {
            Log.d(TAG,"runner: _timerGps_ms = " + _timerGps_ms);
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
*/
        gpsStatus prevGpsStatus = _GpsStatus;
        if (isTracking()) {
            long now = 0;
            if (gpsSimulation != null) {
                /** Handle GPS simulation */
                LocationHandler.handleLocation(gpsSimulation.getLocation());
                handleGpsData(15.0F);
            }

            if (_location != null) {
                now = _location.getTime();
                handleGpsData(now, _location.getLatitude(), _location.getLongitude(), _location.getAccuracy());
                requestStatusUpdate();
                _location = null;
            }

        } else if (prevGpsStatus != gpsStatus.GPS_FIX) {
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
        int perm2 = ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION);
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
           == PackageManager.PERMISSION_GRANTED) {
        }
        else {
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

    static locationStatus _prevLocationStatus = locationStatus.INITIAL;

    /**
     * Update the status of the navigation
     */
    private void onUpdateStatus() {
        updateTrackingStatus();

        String activity ="";
        int bgColor = COLOR_RED;
        String time;

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
            switch (_locationStatus) {
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
                    if ((_initialLocationStatus != INITIAL)
                            && (_initialLocationStatus != WAIT_USER_START)
                    ) {
                        if (DEBUG) Log.d(TAG, " set initial location status:");
                        setLocationStatus(_initialLocationStatus);
                        _initialLocationStatus = INITIAL;
                        if (_initialTrackingStatus) {
                            if (DEBUG) Log.d(TAG, " set initial tracking status: active");
                            setTrackingStatus(true);
                            _initialTrackingStatus = false;
                        }
                    }
                    else
                        setTrackingStatus(false);
                    if (app.getTrack().isValid()) // !app.getTrack().isValidRecordedTrackFile())
                        setLocationStatus(WAIT_USER_START);
                    break;
                }
                case WAIT_USER_START: {
                    _destinationReached = false;
                    resetStartGpsIndex(LocationHandler.getStartGpsIndex());
                    // don't show real time data in places list view
                    recordAdapter.setRealtime(false);
                    break;
                }
            }
        }
        else if (isTracking()) {
            switch (_locationStatus) {
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
                case GOTO_START_POS: {
                    activity = getString(R.string.goto_start_pos);
                    if (_GpsStatus == gpsStatus.GPS_FIX) {
                    } else {
                        activity = getString(R.string.gps_fix_lost);
                        // todo check: Variable is already assigned to this value
                        bgColor = COLOR_NO_GPX;
                    }
                    break;
                }
                case OUT_OF_TRACK: {
                    /* if (_GpsStatus == gpsStatus.GPS_FIX)
                    else */ if (!_destinationReached)
                        activity = getString(R.string.gps_fix_lost);

                    break;
                }
                case APPROACHING: {
                    /* if (_GpsStatus == gpsStatus.GPS_FIX)
                    else */ if (!_destinationReached)
                        activity = getString(R.string.gps_fix_lost);
                    break;
                }
                case BREAK:
                case TRACKING: { /*
                    } */
                    if (_timerOutOfTrack_ms > TIMEOUT_OUT_OF_TRACK_ALARM_MS)
                        tts.speak(getString(R.string.on_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                    _timerOutOfTrack_ms = 0;
                    _counterOutOfTrackAlarms = 0;
                    break;
                }
                case DESTINATION_REACHED: {
                    _destinationReached = true;
                    activity = getString(R.string.dest_reached);
                    bgColor = COLOR_DESTINATION_REACHED;
                    break;
                }
                case DESTINATION_FAILED: {
                    _destinationReached = true;
                    activity = getString(R.string.dest_failed);
                    // todo check: Variable is already assigned to this value
                    bgColor = COLOR_DESTINATION_FAILED;
                    break;
                }
                default: {
                    activity = "";
                    if (DEBUG)
                        Log.e(TAG, "onUpdateStatus(): invalid value of _locationStatus = " + _locationStatus);
                }
            }
        }
        else {
            switch (_locationStatus) {
                case WAIT_USER_START: {
                    if (_startTime_min == 0)
                        activity = getString(R.string.set_start_time);
                    else if (!getPermissionsGranted()) {
                        activity = getString(R.string.permit_location);
                        requestLocationPermissionIfNeeded();
                    }
                    else
                        activity = getString(R.string.wait_user_start);
                    bgColor = BG_COLOR_MESSAGE;
                    break;
                }
                default: {

                }
            }
        }

        if (!getExpandViewStatus()) {
            if (!isErrorMessage()) {
                if (!activity.isEmpty()) {
                    // show current system time
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss ");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    time = sdf.format(calendar.getTime());

                    /** add real time, ex. 00:00:00 Die Tour kann starten! */
                    if (_GpsStatus == gpsStatus.GPS_FIX)
                        activity = time + activity;

                    //    setTitleText(activity, bgColor);
                }
            }
// todo             if (isTracking())
// todo                 recordAdapter.scrollToListPosition();
// todo            recordAdapter.notifyDataSetChanged();
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
            requestStatusUpdate();
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
            String s = locationStatusStr[inStatus.ordinal()];
            return s;
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
     * @param inDistanceToPlace current distance to place
     */
    private void showDistances(double inDistance, double inDistanceToDestination, double inDistanceToPlace) {
        TextView distanceView = findViewById(R.id.track_distance);
        distanceView.setText(new DecimalFormat("#0.00 km").format(inDistance));

        TextView dist_to_destinView = findViewById(R.id.track_dist_to_dest);
        dist_to_destinView.setText(new DecimalFormat("#0.0 km").format(inDistanceToDestination));

        String _distanceToPlace;
        if (inDistanceToPlace >= 1.0)
            _distanceToPlace = new DecimalFormat("in #0.0 km: ").format(inDistanceToPlace);
        else
            _distanceToPlace = new DecimalFormat("in #0 m: ").format((int)(inDistanceToPlace*100.0)*10);

        String comment = _distanceToPlace;
        if (additionalInfo != null)
            comment = comment + additionalInfo.comment;

        TextView commentView = findViewById(R.id.comment_view);
        commentView.setText(comment);
    }

    /**
     * Refresh the timetable with actual position information from GPS location provider
     *
     * @param inGPStime   time stamp [ms] from system or GPS simulation file
     * @param inLatitude  GPS latitude
     * @param inLongitude GPS longitude
     * @param inAccuracy  horizontal accuracy [m] / 0
     */
    public void handleGpsData(long inGPStime, double inLatitude, double inLongitude, float inAccuracy) {
        handleGpsData(inAccuracy);

        if (recordAdapter.getCount() > 0) {
            /*
            switch (_locationStatus) {
                case GOTO_START_POS:
                case TRACKING:
                case APPROACHING:
                case BREAK:
                case OUT_OF_TRACK: {
                    /* limit search to end of a record
                    endIndex = recordAdapter.getEndIndex(endIndex);

                    /* Find nearest track point to the received GPS location * /
                    int nearestGPSindex = app.getNearestTrackpointIndex(_startGpsIndex, endIndex, inLatitude, inLongitude, maxOffset_km);
                    /* Return the nearest distance of this track point to the received GPS location * /
                    nearestDistance = app.getNearestDistance();

                    // nearby GPS location found ?
                    if (nearestGPSindex >= 0) {
                        while (_arrivedPlace < recordAdapter.getCount()) {
                            if (_startGpsIndex >= recordAdapter.getItem(_arrivedPlace+1).trackPointIndex) {
                                _arrivedPlace++;
                                DataPoint place = recordAdapter.getItem(_arrivedPlace).trackPoint;
                                if (DEBUG)
                                    Log.i(TAG, "handleGPSdata(): arrived at: " +
                                            place.getRoutePointName()
                                        + " @ " + formatHourMinSecs(inGPStime / 1000L));
                                // handle break if place is reached
                                // if (dist_from_start >= record_dist_from_start) {

                                    int breakTime_min = place.getWaypointDuration(); // recordAdapter.getBreakTime();
                                    // don't leave the place in case of break
                                    if (breakTime_min > 0) {
                                        _distanceAtBreak = app.getPoint(nearestGPSindex).getDistance(); // dist_from_start;

                                        // are we within our timetable?
                                        _remainBreakTime_min = breakTime_min - _delay_min;
                                        if (DEBUG)
                                            Log.i(TAG,"BREAK: remainBreakTime_min = " + _remainBreakTime_min);
                                        if (gpsSimulation != null)
                                            setTrackingStatus(false);
                                        if (_remainBreakTime_min > 0)
                                        {
                                            // calc time stamp of end of break
                                            _endBreakTime = inGPStime + (long) _remainBreakTime_min * 60000L;
                                            _setNextPlace = false;
                                            setLocationStatus(BREAK);
                                        }
                                    }
                                    else {
                                        _remainBreakTime_min = 0;
                                    }
                                // }
                            }
                            else
                                break;
                        }
                        String simGPSindex = (gpsSimulation != null) ? ", simGPSindex = " + gpsSimulation.getGpsIndex() : "";
                        Log.d(TAG, "handleGPSdata(): _locationStatus = " + getLocationStatus(_locationStatus) +
                                simGPSindex +
                                ", startGPSindex = " + _startGpsIndex + ", endIndex = " + endIndex +
                                ": nearestGPSindex = " + nearestGPSindex +
                                ", nearestDist = " + (int) (nearestDistance * 1000.0) + " m");

                    // nearby GPS location found ?
//                    if (nearestGPSindex >= 0) {
                        switch (_locationStatus) {
                            case GOTO_START_POS:
                            case APPROACHING:
                            case OUT_OF_TRACK: {
                                if (nearestDistance < maxOffsetTrack_km)
                                    setLocationStatus(TRACKING);
                                break;
                            }
                        }
                        // handle it
                        handlePosition(nearestGPSindex, inGPStime);
                    }
                    else if (nearestGPSindex == DataPoint.INVALID_INDEX)
                        // extend the search by one location
                        // todo check end of record
                        endIndex++;
                    else {
                        // no valid GPS location found: extend the search by one location
                        // todo check end of record
                        endIndex++;
                        // depending on the location status:
                        switch (_locationStatus) {
                            case OUT_OF_TRACK:
                            case GOTO_START_POS:
                            case DESTINATION_REACHED:
                            case BREAK:
                                requestStatusUpdate();
                                break;
                            default:
                                setLocationStatus(OUT_OF_TRACK);
                        }
                    }
                }
                break;
            }
             */
        }
        else
            // invalid!
            setLocationStatus(INITIAL);
    }

    /**
     * Handle a nearby found GPS location
     *
     * @param inPosition index of the nearest track point
     * @param inTime     local time from the GPS receiver [ms]
     * /
    private void handlePosition(int inPosition, long inTime) {
        /* use the position to start navigation
        setStartGpsIndex(inPosition); // todo + 1);

        DataPoint point = app.getPoint(inPosition);
        if (recordAdapter != null && point != null) {
            double record_dist_from_start = recordAdapter.getDistance();
            /* Show current distance since start and the remaining distance to destination * /
            double dist_from_start = point.getDistance();
            double dist_to_destin = TrackSegments.summary.totalDistance_km - dist_from_start;
            showDistances(dist_from_start, dist_to_destin, record_dist_from_start - dist_from_start);
            recordAdapter.setDistance(dist_from_start);

            /* Calculate the time shift between GPS and expected arrival time of the current point * /
            long destTime_s = point.getTime();
            int place = recordAdapter.getPlace();
//            _setNextPlace = true;
            if ((_startTime_min > 0) && ((inPosition == 0) || (destTime_s > 0))) {

                // calculate delay
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(inTime);

                // ignore the day in simulation
                long gpsTime_s = calendar.get(Calendar.SECOND) + 60 * (calendar.get(Calendar.MINUTE) + 60L * calendar.get(Calendar.HOUR_OF_DAY));
                _delay_min = (int)(gpsTime_s - (_startTime_min * 60L + destTime_s))/60;

                if (DEBUG)
                    Log.d(TAG, "handlePosition(): gpsTime_s = " + gpsTime_s
                        + "; destTime_s = " + destTime_s
                        + "; distance = " + dist_from_start
                        + "; delay_min = " + _delay_min);

                if (DEBUG) {
                    if (point.isRoutePoint())
                        Log.i(TAG,"reached: " + point.getRoutePointName());
                }

                if (_remainBreakTime_min <= 0)
                {
                    recordAdapter.setDelay(_delay_min);

                    /* handle break if place is reached
                    if (dist_from_start >= record_dist_from_start) {

                        int breakTime_min = recordAdapter.getBreakTime();
                        // don't leave the place in case of break
                        if (breakTime_min > 0) {
                            _distanceAtBreak = dist_from_start;

                            // are we within our timetable?
                            _remainBreakTime_min = breakTime_min - (int) _delay_min;
                            if (DEBUG)
                                Log.i(TAG,"BREAK: remainBreakTime_min = " + _remainBreakTime_min);
                            if (gpsSimulation != null)
                                setTrackingStatus(false);
                            if (_remainBreakTime_min > 0)
                            {
                                // calc time stamp of end of break
                                _endBreakTime = inTime + (long) _remainBreakTime_min * 60000L;
                                setNextPlace = false;
                                setLocationStatus(locationStatus.BREAK);
                            }
                        }
                    }
                     * /
                }
                else
                {
                    /* calc remaining time of break * /
                    long remainBreakTime_ms = _endBreakTime - inTime;
                    if (remainBreakTime_ms > 0) {
                        _remainBreakTime_min = (int) (remainBreakTime_ms / 60000L);
                        // don't leave the place in case of break
                        // are we nearby?
                        if (dist_from_start < _distanceAtBreak + maxOffsetStart_km)
                            _setNextPlace = false;
                    }
                    else{
                        if (gpsSimulation != null)
                            setTrackingStatus(false);
                        _remainBreakTime_min = 0;
                    }
                }
            }
* /

            /* Set next place to arrive after current distance * /
            if (_setNextPlace)
            {
                _distanceAtBreak = 0.0;
                _remainBreakTime_min = 0;
                place = recordAdapter.setNextPlace(this, dist_from_start);
            }
            if (place < recordAdapter.getCount()) {
// todo                recordAdapter.notifyDataSetChanged(); // FIXME
                // destination reached?
                if (dist_to_destin <= maxOffsetTrack_km)
                    setLocationStatus(DESTINATION_REACHED);
                else
                    switch (_locationStatus) {
                        case GOTO_START_POS:
                        case TRACKING:
                        case APPROACHING:
                            setLocationStatus(TRACKING);
                            break;
                        case OUT_OF_TRACK:
                            setLocationStatus(APPROACHING);
                            break;
                        case DESTINATION_REACHED:
                        default:
//                            requestStatusUpdate();
                    }
            } else {
                setLocationStatus(DESTINATION_REACHED);
            }
        }

    } */
}