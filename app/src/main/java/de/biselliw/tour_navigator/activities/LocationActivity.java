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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.data.AppState;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.files.FileUtils;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.ui.ControlElements;

import static de.biselliw.tour_navigator.activities.SettingsActivity.sharedPref;
import static de.biselliw.tour_navigator.helpers.GpsSimulator.gpsSimulation;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

/**
 * Activity handling the timing of a tour
 * It implements a LocationListener using Google Play services
 *
 * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/location/LocationListener">
 *     LocationListener on developer.android.com</a>
 * @see <a href="https://developer.android.com/reference/android/location/LocationListener">
 *     LocationListener on developer.android.com</a>
 * @since 26.1
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
    private enum locationStatus {
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

    private String[] locationStatusStr = {
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

    private static final int COLOR_RED                 = 0xAAB71C1C;
    private static final int COLOR_WHITE               = 0xFFFFFFFF;

    private static final int COLOR_MESSAGE             = COLOR_WHITE;
    private static final int COLOR_NO_GPX              = COLOR_RED;
    private static final int COLOR_TRACKING            = 0xAA317335;
    private static final int COLOR_APPROACHING         = COLOR_RED;
    private static final int COLOR_OUT_OF_TRACK        = COLOR_RED;
    private static final int COLOR_DESTINATION_REACHED = COLOR_TRACKING;
    private static final int COLOR_DESTINATION_FAILED  = COLOR_RED;

    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1;

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
    private int startGpsIndex = 0;


    // required permissions for app
    boolean permissionLocationGranted = false;

    TextToSpeech tts;

    int remainPause_min = 0;
    long _endTimePause = 0;
    boolean raiseAlarm = true;
    boolean autoStart = true;
    int outOfTrackCount = 0;
    int alarmCount = 0;
    int alarmTtsCount = 0;

    private double dist_from_start = 0.0;
    public double TotalDistance = 0.0;
    int initialPlace = -1;
    int lastPlace = -1;

    public ListView recordsView;
    public Time CurrentTime = new Time();

    static boolean currPositionUpdated = false;
    Time currGPStime; double currLatitude, currLongitude, currAccuracy;
    final double minAccuracy = 10.0;


    /** timeout counter to detect GPS location timeout */
    int _timerGps_ms = 0;
    final static int _timeoutGps_ms = 3000;
    int _timerPeriod_ms;


    /** index of the lst place to search for the nearest GPS location */
    private int endPlace = 0, endIndex = 0;

    /** max. allowed offset between a track point and the GPS location */
    private double maxOffset_km = maxOffsetStart_km;
    /** nearest distance of the track point to the received GPS location */
    private double nearestDistance = 0.0;

    /** start time of the tour in UTC milliseconds since the epoch.*/
    private long startTime = 0;

    private double _distanceAtPause = 0.0;
    private long _Pause_ms = 0L;

    //    Runnable timerRunnable;
    Handler timerHandler = new Handler();
    /** Request to update the status of the navigation */
    private boolean _updateStatus = false;
    private boolean _trackingEnabled = false;

    /** Request to update the status of the GPS */
    private  boolean _updateGpsStatus = false;

    private locationStatus _locationStatus = locationStatus.INITIAL;
    private locationStatus _prevLocationStatus = locationStatus.DESTINATION_FAILED;
    private gpsStatus _newGpsStatus = gpsStatus.NOT_REGISTERED;
    private gpsStatus _GpsStatus = gpsStatus.NOT_REGISTERED;

    public static final int TASK_COMPLETE = 4;

    // An object that manages Messages in a Thread
    private Handler mainHandler;
    private SeekBar seekBar;

    private FusedLocationProviderClient locationClient;

    static int run_counter2 = 0;

    /**
     * Constructor of the class
     */
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /* create a table of waypoints */
        recordAdapter = new RecordAdapter(this, new ArrayList<>());
        recordsView = findViewById(R.id.records_view);
        recordsView.setAdapter(recordAdapter);

        // Create a Listener for this list view of places
        recordsView.setOnItemClickListener((adapter, v, inPlace, arg3) ->
                setPlace(inPlace, true));

        /* Create a seek bar to show the current place in relation to its distance */
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (recordAdapter.getCount() > 0) {
                    if (fromUser) {
                        /* Put he marker to the row within the table of places which shows the
                        next place to arrive after the given distance */
                        double distance = TotalDistance * progress / 100.0;
                        setNextPlace(distance,true);
                    }
                }
            }

            /* nonused but mandatory functions */
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {  }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {  }
        });

        // use Fused as GPS LocationProvider
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        /* Install a timer to handle all activities */
        Runnable timerRunnable = new Runnable() {
            int run_counter = 0;
            @Override
            public void run() {
                run_counter2++;
                run_counter++;

                if (run_counter > 2) {
                    if (DEBUG) Log.d(TAG, "run counter = " + run_counter);
                }

                runner ();

                _timerGps_ms += _timerPeriod_ms;
                timerHandler.postDelayed(this, _timerPeriod_ms);

                if (run_counter != 0)
                    run_counter--;
                run_counter2--;
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);

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

    private synchronized void runner () {
        if (!AppState.getPaused()) {
            getCurrentLocation(gpsSimulation != null);
            if (currPositionUpdated)
                _timerGps_ms = 0;

            // Request to update the status of the navigation ?
            if (_updateStatus)
                onUpdateStatus();

            checkGpsStatus();

// Request to update the status of the GPS location provider ?
            if (_updateGpsStatus)
                onUpdateGpsStatus();

            gpsStatus prevGpsStatus = _GpsStatus;
            if (control.isTracking()) {
                if (currPositionUpdated) {
                    currPositionUpdated = false;
                    handleGpsData(currGPStime, currLatitude, currLongitude);
                } else {
                    requestStatusUpdate();
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
                    requestStatusUpdate();
                    break;
                default:
            }
            _timerPeriod_ms = 100 * (gpsSimulation != null ? 1 : 10);

            /* handle remaining pause time
            if (remainPause_min == 0)
                _Pause_ms = 0L;
            else {
// implement 1 Minute Timer
                _Pause_ms += _timerPeriod_ms;
                if (_Pause_ms > 60000) {
                    _Pause_ms -= 60000;
                    remainPause_min -= 1;
                    if (remainPause_min < 0)
                        remainPause_min = 0;
                }
            }
             */
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        super.details = new TourDetails(this, super.app, recordAdapter);

        /* Check if the ACCESS_FINE_LOCATION permission has been granted */
        requestPermissionsIfNeeded();

        setLocationStatus(locationStatus.NO_GPX_FILE_LOADED);

        // forced recreation by system ?
        if (savedInstanceState != null) {
            AppState.getValues(savedInstanceState);
            initialPlace = Math.max(AppState.getGpxPlace()-1,0);
        } else {  // normal start:
            // Clear the app states
            AppState.clearState();

            // delete any previously used cache
            FileUtils.clearAppCache(this);
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
        // remember current place
        AppState.setGpxPlace(recordAdapter.getPlace());
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

    /*
     * Callback function for Check required permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = ((grantResults.length == 1) && (grantResults[0] == PackageManager.PERMISSION_GRANTED));
        if (requestCode == PERMISSION_REQUEST_ACCESS_FINE_LOCATION) {
            permissionLocationGranted = granted;
        }
    }


    @Override
    /*
     * This method is called when the app is no longer in the foreground and is partially visible.
     * This can happen when the user switches to another app or when the screen is turned off.
     * onPause() is a good place to save any unsaved data or state changes before the app is paused.
     */
    public void onPause() {
        super.onPause();
        if (gpsSimulation != null) {
            AppState.setGpxSimulationIndex(gpsSimulation.getGpsIndex());
        }
        AppState.setStartGpsIndex(startGpsIndex);

    //        SettingsActivity.setPlace(recordAdapter.getPlace());
    }

    @Override
    public void onResume() {
        super.onResume();
        raiseAlarm = sharedPref.getBoolean("pref_hiking_par_alarm",true);
        autoStart = sharedPref.getBoolean("pref_hiking_par_autoStart",true);
        showAlarmPreference ();
        if (gpsSimulation != null) {
            gpsSimulation.Reset(AppState.getGpxSimulationIndex());
        }

    }

    @Override
    /*
     * Prevent closing the App here when user pressed the Back key
     * @see https://developer.android.com/guide/components/activities/tasks-and-back-stack
     */
    public void onBackPressed()
    {
        control.setExpandViewStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Private methods
     * --------------------------------------------------------------------------------------------
     */

    private boolean getPermissionsGranted() {
        /* Check if the ACCESS_FINE_LOCATION permission has been granted */
        return  (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermissionsIfNeeded() {
        /* Check if the ACCESS_FINE_LOCATION permission has been granted */
        if (getPermissionsGranted())
            setGpsStatus(gpsStatus.PERMISSION_GRANTED);
        else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            setGpsStatus(gpsStatus.PERMISSION_DENIED);
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastKnownLocation() {
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Log.e(TAG,"No last known location. Try fetching the current location first.");
            } else
            {
                Log.e(TAG,"Last known location:\n" +
                        "lat : " + location.getLatitude() + "\n" +
                        "long : " + location.getLongitude() + "\n" +
                        "fetched at " + System.currentTimeMillis()
                );
            }
        });
    }

    /**
     * Get the current GPS location
     * @param inUseLocationSimulation true if location data are simulated
     */
    @SuppressLint("MissingPermission")
    private void getCurrentLocation(boolean inUseLocationSimulation) {


        // when GPS simulation is active:
        if (inUseLocationSimulation) {
            if (control.isTracking()) {
                Location location = gpsSimulation.getLocation();
                if (location != null) {
                    /* use time from GPS simulation data */
                    CurrentTime.set(location.getTime());
                    currGPStime = CurrentTime;

                    /* use geo location from GPS simulation data */
                    currLatitude = location.getLatitude();
                    currLongitude = location.getLongitude();

                    // simulate min. required accuracy for GPS fix
                    currAccuracy = minAccuracy;
                    // requet to update location data
                    currPositionUpdated = true;
                }
                else
                    setGpsStatus(gpsStatus.GPS_TIMEOUT);
/*
                else {
                    if (_locationStatus != locationStatus.DESTINATION_REACHED)
                        setLocationStatus(locationStatus.DESTINATION_FAILED);
                }
 */
            }
        }
        else
        // when real GPS data are used:
        {
            CancellationTokenSource cts = new CancellationTokenSource();

            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        currPositionUpdated = false;
                        if (location != null) {
                            /* use time from system (the GPS provider deliveres UTC time stamps) */
                            CurrentTime.setToNow();
                            currGPStime = CurrentTime;

                            /* use geo location from GPS Provider */
                            currLatitude = location.getLatitude();
                            currLongitude = location.getLongitude();
                            currAccuracy = location.getAccuracy();

                            // request to update location data
                            currPositionUpdated = true;
                        }
                    });
        }

        if (currPositionUpdated)
            // check required accuracy for GPS fix
            if (currAccuracy <= minAccuracy)
                setGpsStatus(gpsStatus.GPS_FIX);
            else
                setGpsStatus(gpsStatus.WAIT_FOR_GPS_FIX);

    }

    /**
     * Update the status of the navigation
     */
    private void onUpdateStatus() {
        control.updateTrackingStatus();

        String time;
        String activity ="";
        int bgColor = COLOR_RED;
        boolean delayed = (recordAdapter.getDelay() > 5);

        switch (_locationStatus) {
            case NO_GPX_FILE_LOADED: {
                if (_prevLocationStatus != _locationStatus) {
                    activity = getString(R.string.load_gpx_file);
                    bgColor = COLOR_MESSAGE;
                    seekBar.setVisibility(View.INVISIBLE);
                    _prevLocationStatus = _locationStatus;
                    _trackingEnabled = false;
                    break;
                } else
                    return;
            }

            case GPX_FILE_LOADED: {
                setStartGpsIndex(AppState.getStartGpsIndex());
                seekBar.setVisibility(View.VISIBLE);
                // don't show real time data in places list view
                recordAdapter.setRealtime(false);
                if (startTime == 0) {
                    activity = getString(R.string.set_start_time);
                    bgColor = COLOR_MESSAGE;
                    break;
                }
                setLocationStatus(locationStatus.WAIT_USER_START);
            }

            case WAIT_USER_START: {
                _trackingEnabled = true;
                activity = getString(R.string.wait_user_start);
                bgColor = COLOR_MESSAGE;
                if (!control.isTracking())
                    break;
                else {
                    // show first place
                    int startPlace = initialPlace;
                    setPlace(startPlace, false);
                    /* start tracking from first track point */
                    if (recordAdapter.getCount() > startPlace)
                        setStartGpsIndex(super.app.getPointIndex(recordAdapter.getItem(startPlace).getTrackPoint()));
                    setLocationStatus(locationStatus.GOTO_START_POS);
                }
            }
            case GOTO_START_POS: {
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
                _trackingEnabled = true;
                break;
            }
            case OUT_OF_TRACK: {
                if (!control.isTracking())
                    break;
                activity = getString(R.string.returnto_track);
                bgColor = COLOR_OUT_OF_TRACK;
                if (_GpsStatus == gpsStatus.GPS_FIX)
                    activity = activity + getNearestDistance();
                else
                    activity = getString(R.string.gps_fix_lost);

                if (raiseAlarm) {
                    outOfTrackCount++;
                    if ((outOfTrackCount > outOfTrackCountAlarm) && (alarmCount < maxAlarms)) {
                        // play alarm?
                        if (alarmTtsCount == 0) {
                            tts.speak(getString(R.string.returnto_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                            alarmCount++;
                        }
                        if (alarmTtsCount++ > alarmInterval)
                            alarmTtsCount = 0;
                    }
                }
                break;
            }

            case APPROACHING: {
                if (!control.isTracking())
                    break;
                activity = getString(R.string.returnto_track);
                bgColor = COLOR_APPROACHING;
                if (_GpsStatus == gpsStatus.GPS_FIX)
                    activity = activity + getNearestDistance();
                else
                    activity = getString(R.string.gps_fix_lost);
                break;
            }

            case TRACKING: {
                if (!control.isTracking())
                    break;
                if (remainPause_min > 0) {
/** @todo tanslate "PAUSE: Weiter in " */
                    activity = "PAUSE: Weiter in " + remainPause_min + " min.";
                    bgColor = COLOR_TRACKING;
                }
                else {
                    if (_GpsStatus == gpsStatus.GPS_FIX) {
                        activity = getString(R.string.on_track);
                        bgColor = recordAdapter.getDelayColor(); // delayed ? COLOR_NO_GPX : COLOR_TRACKING;
                        break;
                    } else {
                        activity = getString(R.string.gps_fix_lost);
                        bgColor = COLOR_NO_GPX;
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
                activity = getString(R.string.dest_reached);
                bgColor = COLOR_DESTINATION_REACHED;
                break;
            }

            case DESTINATION_FAILED: {
                activity = getString(R.string.dest_failed);
                bgColor = COLOR_DESTINATION_FAILED;
                break;
            }

            default: {
                activity = "Nothing to do?";
                if (DEBUG) Log.e(TAG,"onUpdateStatus(): invalid value of _locationStatus = " + _locationStatus);
                bgColor = COLOR_MESSAGE;
            }
        }

        if (!control.getExpandViewStatus()) {
            if (!isErrorMessage()) {
                if (!activity.isEmpty()) {
                    TextView view = findViewById(R.id.main_text_title);
                    view.setBackgroundColor(bgColor);

                    // show current system time
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss ");
                    Calendar calender = Calendar.getInstance();
                    calender.setTimeInMillis(CurrentTime.toMillis(true));
                    time = sdf.format(calender.getTime());

                    /** @todo 00:00:00 Die Tour kann starten!
                    if (_GpsStatus == gpsStatus.GPS_FIX)
                    activity = time + activity;
                     */
                    view.setText(activity);
                }
            }

            recordAdapter.notifyDataSetChanged();
        }
        _updateStatus = false;
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
    private void setGpsStatus(gpsStatus inGpsStatus) {
        if (inGpsStatus != _GpsStatus) {
            _newGpsStatus = inGpsStatus;
            _updateGpsStatus = true;
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
        requestStatusUpdate();
    }

    /**
     * Get the status of the navigation
     * @return status string
     */
    private String getLocationStatus() {
        if (_locationStatus.ordinal() <= locationStatus.DESTINATION_FAILED.ordinal() )
            return locationStatusStr[_locationStatus.ordinal()];
        else
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
            double dist_to_destin = TotalDistance - dist_from_start;
            showDistances(dist_from_start);
            recordAdapter.setDistance(dist_from_start);

            /* Calculate the time shift between GPS and expected arrival time of the current point */
            long destTime_s = point.getTime();
            int place = recordAdapter.getPlace();
            boolean setNextPlace = true;
            if ((startTime > 0) && ((inPosition == 0) || (destTime_s > 0))) {

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
                    delay_s = (gpsTime_ms - startTime) / 1000;
                    delay_s = (delay_s - destTime_s);
                }

                if (remainPause_min <= 0)
                {
                    recordAdapter.setDelay((int) (delay_s / 60));

                    // handle pause
                    int pause_min = point.getWaypointDuration();
                    // don't leave the place in case of pause
                    if (pause_min > 0) {
                        _distanceAtPause = dist_from_start;

                        // are we within our timetable?
                        remainPause_min = pause_min - (int)delay_s / 60;

                        if (remainPause_min > 0)
                        {
                            // calc time stamp of end of pause
                            _endTimePause = inTime.toMillis(true) + (long)remainPause_min * 60000L;
                            setNextPlace = false;
                        }
                    }
                }
                else
                {
                    /* calc remaining time of pause */
                    long remainPause_ms = _endTimePause - inTime.toMillis(true);
                    if (remainPause_ms > 0)
                        remainPause_min = (int)(remainPause_ms / 60000L);
                    else{
                        remainPause_min = 0;
                        setNextPlace = true;
                    }

                    // don't leave the place in case of pause
                    // are we nearby?
                    if (dist_from_start < _distanceAtPause + maxOffsetStart_km)
                        setNextPlace = false;
                }
            }

            /* Set next place to arrive after current distance */
            if (setNextPlace)
                place = setNextPlace(dist_from_start,false);
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
                        case DESTINATION_REACHED:
                            setLocationStatus(locationStatus.TRACKING);
                            break;
                        case OUT_OF_TRACK:
                            setLocationStatus(locationStatus.APPROACHING);
                            break;
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
            image_alarm_on.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Set the first track point used for navigation
     * @param inIndex index of the track point
     */
    private void setStartGpsIndex(int inIndex)
    {
        if (inIndex != startGpsIndex) {
            //  if (DEBUG) Log.d(TAG,"setStartGPSindex("+inIndex+")");
            startGpsIndex = inIndex;
            if (inIndex == 0) {
                //                startPlace = 0;
                endPlace = 0;
                endIndex = 0;
            }
            /* update profile */
            super.pa.setCursor (startGpsIndex);
        }
    }

    /*
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

    /**
     * Put the marker to a selected row within the table of places
     *
     * @param inPlace row index of the table
     * @return if shown
     */
    public boolean showPlace(int inPlace) {
        String place = "";
        TextView track_place = findViewById(R.id.track_place);
        TextView timeView    = findViewById(R.id.track_arrive);

        timeView.setText("");

        if (inPlace >= 0) {
            if (inPlace < recordAdapter.getCount())
            {
                /* Show the place on the board */
                RecordAdapter.Record record = recordAdapter.getItem(inPlace);
                if (record == null) return false;
                DataPoint point = record.getTrackPoint();
                if (point == null) return false;
                place = point.getRoutePointName();

                /* Show time of arrival at next place */
                if (startTime > 0) {
                    // show the presumable arrival time depending on the delay
                    recordAdapter.showPresumableArriveTime(true, true, timeView, point);
                }
            }
        }

        track_place.setText(place);

        return (inPlace >= 0);
    }

    /** Show the current distance since start and the remaining distance to destination
     *
     * @param inDistance current distance
     */
    private void showDistances(double inDistance) {
        TextView distanceView = findViewById(R.id.track_distance);
        distanceView.setText(new DecimalFormat("#0.00 km").format(inDistance));

        double dist_to_destin = TotalDistance - inDistance;
        TextView dist_to_destinView = findViewById(R.id.track_dist_to_dest);
        dist_to_destinView.setText(new DecimalFormat("#0.0 km").format(dist_to_destin));
    }

    public boolean isGpsFix()
    {
        return (_GpsStatus == gpsStatus.GPS_FIX);
    }

    /**
     * Refresh the timetable with actual position information from GPS location provider
     *
     * @param inGPStime   time stamp [ms] from system or GPS simulation file
     * @param inLatitude  GPS latitude
     * @param inLongitude GPS longitude
     */
    private void handleGpsData(Time inGPStime, double inLatitude, double inLongitude) {
        recordAdapter.setRealtime(true);

        if (recordAdapter.getCount() > 0) {
            switch (_locationStatus) {
                case NO_GPX_FILE_LOADED:
                    if (gpsSimulation != null)
                        gpsSimulation.Reset();
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
                    DataPoint point = super.app.getPoint(startGpsIndex);
                    int startIndex = 0, startPlace = 0;
                    if (recordAdapter != null) {
                        RecordAdapter.Record currentRecord = recordAdapter.getCurrentItem();
                        startPlace = recordAdapter.getPlace();
                        if (startPlace > endPlace) endPlace = startPlace;
                        for (int place = startPlace; place < recordAdapter.getCount() - 1; place++) {
                            if (place <= endPlace) {
                                if (startIndex < 0)
                                    startIndex = currentRecord.trackPointIndex;

                                RecordAdapter.Record nextRecord = recordAdapter.getItem(place + 1);
                                int _endIndex = nextRecord.trackPointIndex - 1;
                                if (endIndex > _endIndex)
                                    endPlace++;
                                if (_endIndex > endIndex)
                                    endIndex = _endIndex;
                                break;
                            }
                        }
                    }

                    /* Find nearest track point to the received GPS location */
                    int nearestGPSindex = super.app.getNearestTrackpointIndex(startGpsIndex, endIndex, inLatitude, inLongitude, maxOffset_km);
                    /* Return the nearest distance of this track point to the received GPS location */
                    nearestDistance = super.app.getNearestDistance();

                    if (DEBUG) {
                        String simGPSindex = (gpsSimulation != null) ? ", simGPSindex = " + gpsSimulation.getGpsIndex() : "";
                        Log.d(TAG, "HandleGPSdata(): _locationStatus = " + getLocationStatus() +
                                ", place = " + startPlace + simGPSindex +
                                ", startGPSindex = " + startGpsIndex + ", endIndex = " + endIndex +
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
     * Set next place to arrive:
     * Put he marker to the row within the table of places which shows the next place to arrive
     * after the given distance
     *
     * @param inDistance current distance since start
     * @param inUser     true if invoked by the user
     * @return index of the place within the list of places
     */
    private int setNextPlace(double inDistance, boolean inUser) {
        int place = 0;
//        remainPause_min = 0;
        _distanceAtPause = 0.0;
        do {
            DataPoint recPoint = recordAdapter.getItem(place).getTrackPoint();
            if (recPoint != null) {
//                int pause = recPoint.getWaypointDuration();
                double dist = recPoint.getDistance();
                if (inDistance > dist)
                    place++;
                else
                {
                    // remainPause_min = 0;
                    setPlace(place, inUser);
                    break;
                }
            }
        }
        while (place < recordAdapter.getCount());
        return place;
    }

    /**
     * Set the position in list of places and update information
     *
     * @param inPlace       row index of the table
     * @param inUser        true if invoked by the user
     * @see RecordAdapter#setPlace(int)
     */
    private boolean setPlace(int inPlace, boolean inUser) {
        // if (DEBUG) Log.d(TAG,"setPlace "+ inPlace);
        double progress = 0.0;
        double distanceToPlace = 0.0;
/*
        if (inPlace < lastPlace)
            Log.e(TAG, "setPlace() inPlace(" + inPlace + " < lastPlace (" + lastPlace + ")");
        else
            lastPlace = inPlace;
*/
        if (inPlace < 0) inPlace = 0;
        initialPlace = inPlace;
        AppState.setGpxPlace(inPlace);
        //        startPlace = inPlace;
        if (inPlace == 0) endIndex = 1;
        endPlace = inPlace;

        if (showPlace(inPlace))
        {
            /* Set relative position in the seek bar */
            RecordAdapter.Record record = recordAdapter.getItem(inPlace);
            if (record == null) return false;
            DataPoint point = record.getTrackPoint();
            if (point == null) return false;
            double distance = point.getDistance();
            progress = distance * 100.0 / TotalDistance;
            distanceToPlace = distance - dist_from_start;

            /* Scroll to the place in the list */
            recordAdapter.setPlace(inPlace);

            if (!expandView)
            {
                if (!inUser)
                    recordsView.smoothScrollToPosition(inPlace);
                else
                    control.updateTrackingStatus();
            }

            if (inUser)
            {
                setStartGpsIndex(super.app.getPointIndex(point));
                if (inPlace == 0) {
                    setLocationStatus(locationStatus.GOTO_START_POS);
                    if (gpsSimulation != null) {
//                    setLocationStatus(locationStatus.WAIT_GPS_PROVIDER);
                        gpsSimulation.Reset();
                    }
                }

                record = recordAdapter.getItem(inPlace+1);
                double nextDistance = distance + 1.0;
                if (record != null)
                {
                    point = record.getTrackPoint();
                    if (point != null)
                        nextDistance = point.getDistance();
                }

                super.pa.setXRange(distance, nextDistance);
            }
            else
            {
                if (inPlace > 0)
                {
                    record = recordAdapter.getItem(inPlace-1);
                    double prevDistance = distance - 1.0;
                    if (record != null)
                    {
                        point = record.getTrackPoint();
                        if (point != null)
                            prevDistance = point.getDistance();
                    }

                    super.pa.setXRange(prevDistance, distance);
                }
                else
                    super.pa.clearXRange();
            }
        }
        else
        {
            super.pa.setCursor (-1);
            super.pa.clearXRange();
            recordAdapter.setPlace(inPlace);
        }

        seekBar.setProgress((int) progress);

        // Set the distance to the next place
        control.setDistanceToPlace(distanceToPlace);

        if (inPlace != lastPlace)
        {
            control.showExpandViewStatus(inPlace,expandView);
            control.showAddInfo(inPlace);
        }
        recordAdapter.notifyDataSetChanged();
        recordsView.smoothScrollToPosition(inPlace);

        lastPlace = inPlace;
        return true;
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Public methods
     * --------------------------------------------------------------------------------------------
     */

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

            pa.initPlot();
            control.setupUserInterface();
        }
        else
            // In all other cases, pass along the message without any other action.
            mainHandler.obtainMessage(state, gpsTask).sendToTarget();
    }

    /**
     * Notify the application of the changed start time of the tour
     */
    public void notifyStartTimeChanged() {
        startTime = recordAdapter.getStartTime().toMillis(true);
        requestStatusUpdate();
    }

    public boolean isTrackingEnabled() {
        return _trackingEnabled;
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
        startTime = SettingsActivity.getStartTime();
        if (startTime > 0) recordAdapter.setStartTime(startTime);

        setPlace(AppState.getGpxPlace(),false);
         recordAdapter.notifyDataSetChanged();
        //if (gpsSimulation == null)
        {
            setLocationStatus(locationStatus.GPX_FILE_LOADED);

            // try to expand the view if description is available
//            control.setExpandViewStatus(true); // SettingsActivity.getExpandView() );
        }
    }

    public void scrollToListPosition()
    {
        recordAdapter.notifyDataSetChanged();
        int inPlace = recordAdapter.getPlace();
        if (inPlace >= 0)
            recordsView.smoothScrollToPosition(inPlace);
    }


}