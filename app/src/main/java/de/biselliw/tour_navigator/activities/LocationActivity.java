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

    Copyright 2024 Walter Biselli (BiselliW)
*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.ui.ControlElements;

import static de.biselliw.tour_navigator.activities.SettingsActivity.getExpandView;
import static de.biselliw.tour_navigator.activities.SettingsActivity.sharedPref;
import static de.biselliw.tour_navigator.helpers.GpsSimulator.gpsSimulation;

/**
 *
 * @see <a href="https://developer.android.com/reference/android/location/LocationListener">
 *     LocationListener on developer.android.com</a>
 */
public class LocationActivity extends ControlElements implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * TAG for log messages.
     */
    static final String TAG = "LocationActivity";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    public static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public enum gpsStatus {
        NO_PERMISSION,
        PROVIDER_DISABLED,
        PROVIDER_ENABLED,
        WAIT_FOR_GPS_FIX,
        GPS_FIX,
        GPS_TIMEOUT
    }

    private enum locStatus {
        NO_GPX_FILE_LOADED,
        GPX_FILE_LOADED,
        WAIT_PERMISSION_GPS,
        WAIT_GPS_PROVIDER,
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

    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1;

    final static double maxOffsetStart_km = 0.100;
    final static double maxOffsetTrack_km = 0.030;
    final static int outOfTrackCountAlarm = 10;
    final static int alarmInterval = 100;
    final static int maxAlarms = 10;

    // required permissions for app
    boolean permissionLocationGranted = false;

    TextToSpeech tts;

    static int remainPause_min = 0;
    static boolean raiseAlarm = true;
    static boolean autoStart = true;
    static int outOfTrackCount = 0;
    static int alarmCount = 0;
    static int alarmTtsCount = 0;
    private locStatus _navigationStatus = locStatus.NO_GPX_FILE_LOADED;
    private locStatus _prevNavStatus = locStatus.DESTINATION_FAILED;
    private gpsStatus _gpsStatus = gpsStatus.NO_PERMISSION;

    private double dist_from_start = 0.0;
    public double TotalDistance = 0.0;
    int initialPlace = -1;
    int lastPlace = -1;

    public ListView recordsView;
    public Time CurrentTime = new Time();

    int timeoutGps = 0;
    final static int maxTimeoutGps = 6;

    /** index of the track point from which to search for the nearest GPS location */
    private int startGPSindex = 0;
    /** max. allowed offset between a track point and the GPS location */
    private double maxOffset_km = maxOffsetStart_km;
    /** nearest distance of the track point to the received GPS location */
    private double nearestDistance = 0.0;

    /** start time of the tour in UTC milliseconds since the epoch.*/
    private long startTime = 0;

    private double _distanceAtPause = 0.0;
    private long _Pause_ms = 0L;

    Handler timerHandler = new Handler();
    Runnable timerRunnable;

    public static final int TASK_COMPLETE = 4;

    // An object that manages Messages in a Thread
    private Handler mainHandler;
    private SeekBar seekBar;

    LocationManager geolocation = null;
    LocationListener locationListener;

    /**
     * Constructor of the class
     *
     */
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// Force Portrait orientation

        recordAdapter = new RecordAdapter(this, new ArrayList<>());
        recordsView = findViewById(R.id.records_view);
        recordsView.setAdapter(recordAdapter);

         // Create LocationListener to handle received GPS data
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (gpsSimulation == null)
                {
                    gpsStatus prevGpsStatus = _gpsStatus;
                    timeoutGps = 0;
                    setGPS_Status(gpsStatus.GPS_FIX);
                    /* use time from system */
                    CurrentTime.setToNow();
                    if (control.isTracking())
                    {
                        HandleGPSdata(CurrentTime, location.getLatitude(), location.getLongitude());
                    }
                    else if (prevGpsStatus != gpsStatus.GPS_FIX) {
                        updateStatus();
                    }
                }
            }

            public void onProviderEnabled(String provider) {
                if (DEBUG) Log.d(TAG, "onProviderEnabled (" + provider + ")");
                setGPS_Status(gpsStatus.PROVIDER_ENABLED);
                recordAdapter.setRealtime(true);
                updateStatus();
            }

            public void onProviderDisabled(String provider) {
                if (DEBUG) Log.d(TAG, "onProviderDisabled (" + provider + ")");
                setGPS_Status(gpsStatus.PROVIDER_DISABLED);
                recordAdapter.setRealtime(false);
                updateStatus();
            }
        };

        // Install a timer to inform about a GPS timeout or pause
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (gpsSimulation != null)
                {
                    timeoutGps = 0;
                    if (control.isTracking())
                    {
                        Location location = gpsSimulation.getLocation();
                        if (location != null)
                        {
                            setGPS_Status(gpsStatus.GPS_FIX);
                            CurrentTime.set(location.getTime());
                            HandleGPSdata(CurrentTime, location.getLatitude(), location.getLongitude());
                        }
                        else
                        {
                            if (_navigationStatus != locStatus.DESTINATION_REACHED)
                                _navigationStatus = locStatus.DESTINATION_FAILED;
                            updateStatus();
                        }
                    }
                    else if ((_gpsStatus != gpsStatus.GPS_FIX) && (_gpsStatus != gpsStatus.PROVIDER_DISABLED))
                        setGPS_Status(gpsStatus.GPS_FIX);
                }

                int steps = gpsSimulation != null ? 1 : 10;
                switch (_gpsStatus) {
                    case NO_PERMISSION:
                    case PROVIDER_DISABLED:
                    case PROVIDER_ENABLED:
                    case WAIT_FOR_GPS_FIX:
                        updateStatus();
                        break;
                    case GPS_FIX:
                        if (_navigationStatus == locStatus.WAIT_GPS_PROVIDER)
                            updateStatus();
                        break;
                    default:
                        if (++timeoutGps > maxTimeoutGps*steps)
                            setGPS_Status(gpsStatus.GPS_TIMEOUT);
                        break;
                }
                long interval_ms = 100*steps;

                // handle remaining pause time
                if (remainPause_min == 0)
                    _Pause_ms = 0L;
                else
                {
                    // implement 1 Minute Timer
                    _Pause_ms += interval_ms;
                    if (_Pause_ms > 60000) {
                        _Pause_ms -= 60000;
                        remainPause_min -= 1;
                        if (remainPause_min < 0)
                            remainPause_min = 0;
                    }
                }
                timerHandler.postDelayed(this,  interval_ms);
            }
        };

        timerHandler.postDelayed(timerRunnable, 10000);

        // Create seek bar to show current relative distance
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (recordAdapter.getCount() > 0) {
                    if (fromUser) {
                        /* Put he marker to the row within the table of places which shows the
                        next place to arrive after the given distance */
                        double distance = TotalDistance * progress / 100.0;
                        setNextPlace(distance);
                    }
                }
            }

            @Override
            /* Notification that the user has started a touch gesture. */
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            /* Notification that the user has finished a touch gesture. */
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        // Create listeners for list view of places 
        recordsView.setOnItemClickListener((adapter, v, inPlace, arg3) ->
                setPlace(inPlace, true));

        // Define a Handler object that's attached to the UI thread
        mainHandler = new Handler(Looper.getMainLooper()) {
            /*
             * handleMessage() defines the operations to perform when the
             * Handler receives a new Message to process.
             */
            @Override
            public void handleMessage(Message inputMessage) {
                // Gets the task from the incoming Message object.
                // The decoding is done
                if (inputMessage.what == TASK_COMPLETE) {
                    notifyGpsFileLoaded();
                } else {
                    // Pass along other messages from the UI
                    super.handleMessage(inputMessage);
                }
            }
        };

        // Create TextToSpeech
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if(status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.getDefault());
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        super.details = new TourDetails(this, super.app, recordAdapter);

        // Check if the ACCESS_FINE_LOCATION permission has been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
            _gpsStatus = gpsStatus.PROVIDER_ENABLED;
        else
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);

        setStatus(locStatus.NO_GPX_FILE_LOADED);
        control.showGPS_Status(_gpsStatus);

        if (savedInstanceState != null) {
            initialPlace = savedInstanceState.getInt("initialPlace");
            if (autoStart)
                initialPlace = initialPlace - 1;
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
        outState.putInt("initialPlace",recordAdapter.getPlace());
    }

    /*
     * Cllback function for Check required permissions
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
//        SettingsActivity.setPlace(recordAdapter.getPlace());
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        raiseAlarm = sharedPref.getBoolean("pref_hiking_par_alarm",true);
        autoStart = sharedPref.getBoolean("pref_hiking_par_autoStart",true);
        showAlarmPreference ();
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
     * Notify the application of a loaded GPX file
     */
    public void notifyGpsFileLoaded() {
        startTime = SettingsActivity.getStartTime();
        if (startTime > 0) recordAdapter.setStartTime(startTime);

        setPlace(initialPlace,false);
        if (!autoStart)
            initialPlace = -1;
        recordAdapter.notifyDataSetChanged();
        if (gpsSimulation == null) {
            setStatus(locStatus.GPX_FILE_LOADED);

            // try to expand the view if description is available
            control.setExpandViewStatus(SettingsActivity.getExpandView() );
        }
    }

    /**
     * Notify the application of the changed start time of the tour
     */
    public void notifyStartTimeChanged() {
        startTime = recordAdapter.getStartTime().toMillis(true);
        updateStatus();
    }

    /**
     * Show alarm setting
     */
    void showAlarmPreference()
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
     * Set the GPS status
     */
    void setGPS_Status(gpsStatus inGpsStatus) {
        if (inGpsStatus != _gpsStatus) {
            control.showGPS_Status(inGpsStatus);
            _gpsStatus = inGpsStatus;
        }
    }

    /**
     * Refresh the timetable with actual position information from GPS receiver
     *
     * @param inGPStime   time stamp [ms] of the GPS receiver
     * @param inLatitude  GPS latitude
     * @param inLongitude GPS longitude
     */
    protected void HandleGPSdata(Time inGPStime, double inLatitude, double inLongitude) {
        recordAdapter.setRealtime(true);
        timeoutGps = 0;

        if (recordAdapter.getCount() > 0) {
            switch (_navigationStatus) {
                case NO_GPX_FILE_LOADED:
                case GPX_FILE_LOADED:
                case WAIT_PERMISSION_GPS:
                case WAIT_GPS_PROVIDER:
                case WAIT_USER_START:
                    if (gpsSimulation != null)
                        gpsSimulation.Reset();
                    updateStatus();
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

            switch (_navigationStatus) {
                case GOTO_START_POS:
                case TRACKING:
                case APPROACHING:
                case OUT_OF_TRACK:
                    // limit search to end of current record
                    DataPoint point = super.app.getPoint(startGPSindex);
                    double maxDist_km = 0.0;
                    if (point != null) {
                        double distance = point.getDistance();
                        for (int place = 0; place < recordAdapter.getCount(); place++) {
                            DataPoint next = recordAdapter.getItem(place).trackPoint;
                            if (next.getDistance() > distance)
                            {
                                maxDist_km = next.getDistance();
                                break;
                            }
                        }
                    }
                    /* Find nearest track point to the received GPS location */
                    int nearestGPSindex = super.app.getNearestTrackpointIndex(startGPSindex, inLatitude, inLongitude, maxOffset_km, maxDist_km);
                    /* Return the nearest distance of this track point to the received GPS location */
                    nearestDistance = super.app.getNearestDistance();

                    if (DEBUG) {
                        Log.d(TAG, "HandleGPSdata() - nearestGPSindex = " + nearestGPSindex);
                    }
                    // no GPS location found ?
                    if (nearestGPSindex == DataPoint.INVALID_INDEX)
                        updateStatus();
                    // nearby GPS location found ?
                    else if (nearestGPSindex >= 0)
                        handlePosition(nearestGPSindex, inGPStime);
                    else
                        switch (_navigationStatus) {
                            case GOTO_START_POS:
                            case DESTINATION_REACHED:
                            case OUT_OF_TRACK:
                                updateStatus();
                                break;
                            default:
                                setStatus(locStatus.OUT_OF_TRACK);
                        }
                    break;
            }
        }
        else
            if (_navigationStatus != locStatus.NO_GPX_FILE_LOADED)
                setStatus(locStatus.NO_GPX_FILE_LOADED);
    }

    /**
     * Handle a nearby found GPS location
     *
     * @param inPosition index of the nearest track point
     * @param inTime     local time from the GPS receiver
     */
    private int _debug_Position_stop = 0;
    private double _debug_distance_stop = 0.0;

    private void handlePosition(int inPosition, Time inTime) {

        setStartGPSindex(inPosition);

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

            if ((_DEBUG) && (_debug_Position_stop > 0.0) && (inPosition >= _debug_Position_stop) ) {
                inPosition = inPosition;
            }

            if ((_DEBUG) && (_debug_distance_stop > 0.0) && (dist_from_start >= _debug_distance_stop) ) {
                _debug_distance_stop = _debug_distance_stop;
            }

            if ((startTime > 0) && ((inPosition == 0) || (destTime_s > 0))) {

                // calculate delay
                long delay_s;
                if (gpsSimulation != null) {
                    // ignore the day in simulation
                    long gpsTime_s = inTime.second + 60 * (inTime.minute + 60L * inTime.hour);
                    Time start = recordAdapter.getStartTime();
                    long start_Time_s = (start.minute + 60L * start.hour) * 60L;
                    delay_s = gpsTime_s - (start_Time_s + destTime_s);
                    if (DEBUG) {
                        Log.d(TAG, "handlePosition() - gpsTime_s  = " + gpsTime_s);
                        Log.d(TAG, "                 - destTime_s = " + destTime_s);
                        Log.d(TAG, "                 - distance   = " + dist_from_start);
                        Log.d(TAG, "                 - delay_min  = " + delay_s / 60);
                    }
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
 //                           _endTimePause = inTime.toMillis(true) + (long)remainPause_min * 60000L;
                            setNextPlace = false;
                        }
                    }

                }
                else
                {
                    /* calc remaining time of pause
                    long remainPause_ms = _endTimePause - inTime.toMillis(true);
                    if (remainPause_ms > 0)
                        remainPause_min = (int)(remainPause_ms / 60000L);
                    else
                        setNextPlace = true;
                    */

                    // don't leave the place in case of pause
                    // are we nearby?
                    if (dist_from_start < _distanceAtPause + maxOffsetStart_km)
                        setNextPlace = false;
                }


                /* handle pause
                DataPoint recPoint = recordAdapter.getItem(place).getTrackPoint();
                if (recPoint != null) {
                    int pause_min = recPoint.getWaypointDuration();
                    // don't leave the place in case of pause
                    if (pause_min > 0) {
                        double dist = recPoint.getDistance();
                        // are we nearby?
                        if ((distance > dist) && (distance < dist + maxOffsetStart_km)) {
                            remainPause_min = 1;
                            setNextPlace = false;
                            // are we within our timetable (delay_s already includes pause time)?
                            if (delay_s < 0) {
                                remainPause_min = -(int)delay_s / 60;
                            }
                        }
                    }
                }

                 */
            }

            /* Set next place to arrive after current distance */
            if (setNextPlace)
                place = setNextPlace(dist_from_start);
            if (place < recordAdapter.getCount()) {
                recordAdapter.notifyDataSetChanged();
                // destination reached?
                if (dist_to_destin <= maxOffsetTrack_km)
                    setStatus(locStatus.DESTINATION_REACHED);
                else
                    switch (_navigationStatus) {
                        case GOTO_START_POS:
                        case TRACKING:
                        case APPROACHING:
                        case DESTINATION_REACHED:
                            setStatus(locStatus.TRACKING);
                            break;
                        case OUT_OF_TRACK:
                            setStatus(locStatus.APPROACHING);
                            break;
                        default:
                            updateStatus();
                    }
            } else {
                setStatus(locStatus.DESTINATION_REACHED);
            }
        }
    }

    /**
     * Change the status of the navigation
     * @param inStatus new status of the navigation
     */
    public void setStatus(locStatus inStatus) {
        _navigationStatus = inStatus;
        updateStatus();
    }

    /**
     * Update the status of the navigation
     */
    public void updateStatus() {
        control.updateTrackingStatus();

        String time;
        String activity;
        int bgColor;
        boolean delayed = (recordAdapter.getDelay() > 5);

        switch (_navigationStatus) {
            case NO_GPX_FILE_LOADED:
                if (_prevNavStatus != _navigationStatus)
                {
                    activity = getString(R.string.load_gpx_file);
                    bgColor = COLOR_MESSAGE;
                    seekBar.setVisibility(View.INVISIBLE);
                    _prevNavStatus = _navigationStatus;
                    break;
                }
                else
                    return;

            case GPX_FILE_LOADED:
                setStartGPSindex(0);
                seekBar.setVisibility(View.VISIBLE);
                // don't show real time data in places list view
                recordAdapter.setRealtime(false);
                if (startTime == 0) {
                    activity = getString(R.string.set_start_time);
                    bgColor = COLOR_MESSAGE;
                    break;
                }
                _navigationStatus = locStatus.WAIT_PERMISSION_GPS;
            case WAIT_PERMISSION_GPS:
                permissionLocationGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (permissionLocationGranted) {
                    if (geolocation == null) {
                        geolocation = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                        geolocation.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationListener);
                    }
                    setGPS_Status(gpsStatus.WAIT_FOR_GPS_FIX);
                }
                if (!permissionLocationGranted) {
                    setGPS_Status(gpsStatus.NO_PERMISSION);
                    activity = getString(R.string.permit_location);
                    bgColor = COLOR_NO_GPX;
                    break;
                }
                _navigationStatus = locStatus.WAIT_GPS_PROVIDER;
            case WAIT_GPS_PROVIDER:
                if (_gpsStatus == gpsStatus.WAIT_FOR_GPS_FIX) {
                    activity = getString(R.string.wait_for_gps_fix);
                    bgColor = COLOR_NO_GPX;
                    break;
                }
                else if ((_gpsStatus != gpsStatus.PROVIDER_ENABLED) && (_gpsStatus != gpsStatus.GPS_FIX))
                {
                    activity = getString(R.string.activate_gps);
                    bgColor = COLOR_NO_GPX;
                    break;
                }
                else
                {
                    if (gpsSimulation != null)
                        gpsSimulation.Reset();
                    _navigationStatus = locStatus.WAIT_USER_START;
                }
            case WAIT_USER_START:
                activity = getString(R.string.wait_user_start);
                bgColor = COLOR_MESSAGE;
                if (!control.isTracking())
                    break;
                else
                {
                    // show first place
                    int startPlace = 0;
                    if (autoStart && (initialPlace > 0))
                        startPlace = initialPlace;
                    setPlace(startPlace, false);
                    /* start tracking from first track point */
                    if (recordAdapter.getCount() > startPlace)
                        setStartGPSindex(super.app.getPointIndex(recordAdapter.getItem(startPlace).getTrackPoint()));
                    _navigationStatus = locStatus.GOTO_START_POS;
                }
            case GOTO_START_POS:
                activity = getString(R.string.goto_start_pos);
                if (_gpsStatus == gpsStatus.GPS_FIX) {
                    activity = activity + getNearestDistance();
                    bgColor = COLOR_MESSAGE;
                }
                else {
                    activity = getString(R.string.gps_fix_lost);
                    bgColor = COLOR_NO_GPX;
                }
                break;

            case OUT_OF_TRACK:
                activity = getString(R.string.returnto_track);
                bgColor = COLOR_OUT_OF_TRACK;
                if (_gpsStatus == gpsStatus.GPS_FIX)
                    activity = activity + getNearestDistance();
                else
                    activity = getString(R.string.gps_fix_lost);

                if (raiseAlarm) {
                    outOfTrackCount++;
                    if ((outOfTrackCount > outOfTrackCountAlarm) && (alarmCount < maxAlarms)) {
                        // play alarm?
                        if (alarmTtsCount == 0)
                        {
                            tts.speak(getString(R.string.returnto_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                            alarmCount++;
                        }
                        if (alarmTtsCount++ > alarmInterval)
                            alarmTtsCount = 0;
                    }
                }
                break;

            case APPROACHING:
                activity = getString(R.string.returnto_track);
                bgColor = COLOR_APPROACHING;
                if (_gpsStatus == gpsStatus.GPS_FIX)
                    activity = activity + getNearestDistance();
                else
                    activity = getString(R.string.gps_fix_lost);
                break;

            case TRACKING:
                if (remainPause_min > 0)
                    activity = "PAUSE: Weiter in " + remainPause_min + " min.";
                else
                    activity = getString(R.string.on_track);
                if (outOfTrackCount > outOfTrackCountAlarm)
                    tts.speak(getString(R.string.on_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                outOfTrackCount = 0;
                alarmCount = 0;
                alarmTtsCount = 0;

                if (_gpsStatus == gpsStatus.GPS_FIX) {
                    bgColor = delayed ? COLOR_NO_GPX : COLOR_TRACKING;
                    break;
                }
                else {
                    activity = getString(R.string.gps_fix_lost);
                    bgColor = COLOR_NO_GPX;
                }
                break;

            case DESTINATION_REACHED:
                activity = getString(R.string.dest_reached);
                bgColor = COLOR_DESTINATION_REACHED;
                break;

            case DESTINATION_FAILED:
                activity = getString(R.string.dest_failed);
                bgColor = COLOR_DESTINATION_FAILED;
                break;

            default:
                activity = "Nothing to do?";
                bgColor = COLOR_MESSAGE;
        }

        if (!control.getExpandViewStatus()) {
            TextView view = findViewById(R.id.main_text_title);
            view.setBackgroundColor(bgColor);

            // show current system time
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss ");
            Calendar calender = Calendar.getInstance();
            calender.setTimeInMillis(CurrentTime.toMillis(true));
            time = sdf.format(calender.getTime());

            if (_gpsStatus == gpsStatus.GPS_FIX)
                activity = time + activity;
            view.setText(activity);

            recordAdapter.notifyDataSetChanged();
        }
    }



    /**
     *
     */
    void setStartGPSindex(int inIndex)
    {
        if (DEBUG)
        {
            if (inIndex > startGPSindex)
                Log.d(TAG,"setStartGPSindex("+inIndex+")");
        }
        startGPSindex = inIndex;
        /* update profile */
        super.pa.setCursor (startGPSindex);
    }

    /**
     * Set the position in list of places and update information
     *
     * @param inPlace       row index of the table
     * @param inUser        true if invoked by the user
     * @see RecordAdapter#setPlace(int)
     */
    private boolean setPlace(int inPlace, boolean inUser) {
        if (DEBUG) Log.d(TAG,"setPlace");
        double progress = 0.0;
        double distanceToPlace = 0.0;

        if (inPlace < 0) inPlace = 0;

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
                setStartGPSindex(super.app.getPointIndex(point));
                if ((gpsSimulation != null) && (inPlace == 0)) {
                    setStatus(locStatus.WAIT_GPS_PROVIDER);
                    gpsSimulation.Reset();
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
        scrollToListPosition(inPlace);

        lastPlace = inPlace;
        return true;
    }

    /**
     * Set next place to arrive:
     * Put he marker to the row within the table of places which shows the next place to arrive
     * after the given distance
     *
     * @param inDistance current distance since start
     * @return index of the place within the list of places
     */
    private int setNextPlace(double inDistance) {
        int place = 0;
        remainPause_min = 0;
        _distanceAtPause = 0.0;
        do {
            DataPoint recPoint = recordAdapter.getItem(place).getTrackPoint();
            if (recPoint != null) {
                int pause = recPoint.getWaypointDuration();
                double dist = recPoint.getDistance();
                if (inDistance > dist)
                    place++;
                else
                {
                    // remainPause_min = 0;
                    setPlace(place, false);
                    break;
                }
            }
        }
        while (place < recordAdapter.getCount());
        return place;
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
    public void showDistances(double inDistance) {
        TextView distanceView = findViewById(R.id.track_distance);
        distanceView.setText(new DecimalFormat("#0.00 km").format(inDistance));

        double dist_to_destin = TotalDistance - inDistance;
        TextView dist_to_destinView = findViewById(R.id.track_dist_to_dest);
        dist_to_destinView.setText(new DecimalFormat("#0.0 km").format(dist_to_destin));
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

    public void scrollToListPosition(int inPlace)
    {
        recordAdapter.notifyDataSetChanged();
        if (inPlace >= 0)
            recordsView.smoothScrollToPosition(inPlace);
    }

    public void scrollToListPosition()
    {
        recordAdapter.notifyDataSetChanged();
        int inPlace = recordAdapter.getPlace();
        if (inPlace >= 0)
            recordsView.smoothScrollToPosition(inPlace);
    }

    public boolean isGPS_Fix ()
    {
        return (_gpsStatus == gpsStatus.GPS_FIX);
    }
}