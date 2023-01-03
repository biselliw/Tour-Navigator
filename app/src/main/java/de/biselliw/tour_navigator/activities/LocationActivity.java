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

    Copyright 2022 Walter Biselli (BiselliW)
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
    final static int alarmInterval = 100;
    final static int maxAlarms = 10;

    // required permissions for app
    boolean permissionLocationGranted = false;

    TextToSpeech tts;

    static boolean raiseAlarm = true;
    static int outOfTrackCount = 0;
    static int alarmCount = 0;
    private locStatus _navigationStatus = locStatus.NO_GPX_FILE_LOADED;
    private locStatus _prevNavStatus = locStatus.DESTINATION_FAILED;
    private gpsStatus _gpsStatus = gpsStatus.NO_PERMISSION;

    public double TotalDistance = 0.0;
//    public RecordAdapter recordAdapter;
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
    /** true if the start time of the tour has been set by the user */
    boolean startTimeSet = false;

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

        // Install a timer to inform about a GPS timeout
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
                            setGPS_Status(gpsStatus.PROVIDER_DISABLED);
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
                timerHandler.postDelayed(this, 100*steps);
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
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        raiseAlarm = sharedPref.getBoolean("pref_hiking_par_alarm",true);
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
        setPlace(-1,false);
        recordAdapter.notifyDataSetChanged();
        startTimeSet = false;
        if (gpsSimulation == null) {
            setStatus(locStatus.GPX_FILE_LOADED);

            // try to expand the view if description is available
            control.setExpandViewStatus(true);
        }
    }

    /**
     * Notify the application of the changed start time of the tour
     */
    public void notifyStartTimeChanged() {
        startTime = recordAdapter.getStartTime().toMillis(true);
        startTimeSet = true;
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
    private void handlePosition(int inPosition, Time inTime) {
        setStartGPSindex(inPosition);

        DataPoint point = super.app.getPoint(inPosition);
        if (point != null) {
            /* Show current distance since start and the remaining distance to destination */
            double distance = point.getDistance();
            double dist_to_destin = TotalDistance - distance;
            showDistances(distance);
            recordAdapter.setDistance(distance);

            /* Calculate the time shift between GPS and expected arrival time of the current point */
            long destTime = point.getTime();
            long delay;
            if (startTimeSet && ((inPosition == 0) || (destTime > 0))) {
                // ignore the day in simulation
                if (gpsSimulation != null)
                {
                    long gpsTime = inTime.minute + 60L *(inTime.hour);
                    Time start = recordAdapter.getStartTime();
                    long start_Time = start.minute + 60L *(start.hour);
                    delay = gpsTime - start_Time - destTime/60;
                }
                else
                {
                    long gpsTime = inTime.toMillis(true);
                    delay = (gpsTime - startTime) / 1000;
                    delay = (delay - destTime) / 60;
                }
                recordAdapter.setDelay((int) (delay));
            }

            /* Set next place to arrive after current distance */
            int place = setNextPlace(distance);
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
                if (!startTimeSet) {
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
                    setPlace(0, false);
                    /* start tracking from first track point */
                    if (recordAdapter.getCount() > 0)
                        setStartGPSindex(super.app.getPointIndex(recordAdapter.getItem(0).getTrackPoint()));
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
                    if ((outOfTrackCount > alarmInterval) && (alarmCount < maxAlarms)) {
                        // play alarm
                        tts.speak(getString(R.string.returnto_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                        alarmCount++;
                        outOfTrackCount = 0;
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
                activity = getString(R.string.on_track);
                if (outOfTrackCount > alarmInterval)
                    tts.speak(getString(R.string.on_track), TextToSpeech.QUEUE_FLUSH, null, getString(R.string.app_name));
                outOfTrackCount = 0;
                alarmCount = 0;

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

        if (showPlace(inPlace))
        {
            /* Set relative position in the seek bar */
            RecordAdapter.Record record = recordAdapter.getItem(inPlace);
            if (record == null) return false;
            DataPoint point = record.getTrackPoint();
            if (point == null) return false;
            double distance = point.getDistance();
            progress = distance * 100.0 / TotalDistance;

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
        do {
            DataPoint recPoint = recordAdapter.getItem(place).getTrackPoint();
            if (recPoint != null) {
                double dist = recPoint.getDistance();
                if (dist < inDistance) {
                    place++;
                } else {
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
                if (startTimeSet) {
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