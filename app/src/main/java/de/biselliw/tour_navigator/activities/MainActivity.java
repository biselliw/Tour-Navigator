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

    You should have received a copy of the GNU General Public LicenseIf not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.adapter.RecordAdapter;
import de.biselliw.tour_navigator.data.AppState;
import de.biselliw.tour_navigator.data.Resources;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.dialogs.BreakTimeDialog;
import de.biselliw.tour_navigator.dialogs.CommentDialog;
import de.biselliw.tour_navigator.dialogs.StartTimeDialog;
import de.biselliw.tour_navigator.files.FileUtils;
import de.biselliw.tour_navigator.files.HTML_File;
import de.biselliw.tour_navigator.fragments.OpenStreetMapDialogFragment;
import de.biselliw.tour_navigator.fragments.WaypointsDialogFragment;
import de.biselliw.tour_navigator.fragments.WikipediaDialogFragment;
import de.biselliw.tour_navigator.helpers.GlobalExceptionHandler;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.helpers.Prefs;
import de.biselliw.tour_navigator.helpers.ProfileAdapter;
import de.biselliw.tour_navigator.tim_prune.data.Track;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.load.xml.XmlFileLoader;
import de.biselliw.tour_navigator.tim_prune.save.GpxExporter;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static de.biselliw.tour_navigator.helpers.Prefs.defineHikingParameters;
import static de.biselliw.tour_navigator.tim_prune.data.DataPoint.INVALID_INDEX;

/**
 *
 */
public class MainActivity extends LocationActivity  implements
        NavigationView.OnNavigationItemSelectedListener {

    /**
     * TAG for log messages.
     */
    static final String TAG = "MainActivity";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    int REQUEST_OPEN_GPX = 222;

    private boolean _destroyed = false;
    boolean intentFromOtherApp = false;

    public HTML_File htmlFile;
    private final boolean _autoAppend = false;
    private String gpxFileName = "";


    /**
     * if >= 0; change start time of the tour in [min] since midnight
     */
    int _changeStartTime = -1;

    private boolean _updateRecords = false;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    /*
     * One-time initialization
     */
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (DEBUG)
            AppState.MainActivityInstanceCount++;

        Resources.resources = getResources();
        Resources.getResources();

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        overridePendingTransition(0, 0);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        app = new App(this);

        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());

        profileAdapter = new ProfileAdapter(this);
        /* create a table of route points */
        recordAdapter = new RecordAdapter(this, profileAdapter, new ArrayList<>());

        // Load preferences
        Prefs.getPreferences(Prefs.get(this));

        defineHikingParameters();
        Prefs.setDefaultHikingParameters (Prefs.get(this));
    }

    @Override
    /*
     * Within the Android Activity lifecycle, onStart() is invoked
     * - within One-time initialization after onCreate() and before onPostCreate()
     * - between onRestart() and onResume(),
     * at the point where the Activity is becoming visible to the user but is not yet in the foreground for interaction.
     * Primary Responsibility: Acquire resources needed while visible
     */
    public void onStart() {
        if (DEBUG) Log.i(TAG,"onStart");
        super.onStart();
        startTimer(100);
    }

    @Override
    /*
     * Within the Android Activity lifecycle, onPostCreate() is invoked after onCreate() and onStart()
     * Primary Responsibility:
     */
    protected void onPostCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG,"onPostCreate");
        super.onPostCreate(savedInstanceState);

        mNavigationView.setNavigationItemSelectedListener(this);
        selectNavigationItem(R.id.nav_open_gpx);

        htmlFile = new HTML_File(this);

        // Handle a received intent from another app
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals("android.intent.action.VIEW")) {
            intentFromOtherApp = true;
            onActivityResult(REQUEST_OPEN_GPX, RESULT_OK, intent);
        }

        if (Prefs.isFirstTimeLaunch())
        {
            Intent mainIntent = new Intent(this, TutorialActivity.class);
            startActivity(mainIntent);
        }
        // app restarted by Android?
        else {
            if (AppState.destroyed || (savedInstanceState != null)) {
                if (AppState.getGpxSimulationUri() != null) {
                    if (DEBUG) Log.d(TAG, "OpenFileGPX(AppState.getGpxSimulationUri())");
                    OpenFileGPX(AppState.getGpxSimulationUri());
                } else if (AppState.isGpxFileCached()) {
                    OpenCachedFileGPX();
                }
            } else {  // normal start:
                if (DEBUG) Log.d(TAG, "normal start");
                // Clear the app states
                AppState.clearState();
                FileUtils.clearAppCache(this);
            }
        }
    }

    @Override
    /*
     * Within the Android Activity lifecycle, onRestart() is invoked
     * after onStop()
     * Primary Responsibility:
     */
    public void onRestart() {
        if (DEBUG) Log.i(TAG,"onRestart");
        super.onRestart();
        AppState.restarted = true;
    }

    @Override
    /*
     * Within the Android Activity lifecycle, onResume() is invoked
     * after onPostCreate()
     * after onStart()
     * Primary Responsibility:
     */
    public void onResume() {
        if (DEBUG) Log.i(TAG,"onResume");
        if (stopped) {
            stopped = false;
        }
        recordAdapter.notifyDataSetChanged();
        super.onResume();
        AppState.setPaused(false);
    }

    @Override
    /*
     * This method is called when the app is no longer in the foreground and is partially visible.
     * This can happen when the user switches to another app or when the screen is turned off.
     * onPause() is a good place to save any unsaved data or state changes before the app is paused.
     * Primary Responsibility: Release interaction-related resources
     */
    public void onPause() {
        if (DEBUG) Log.i(TAG,"onPause");
        // Save the GPX file in the cache?
        if (updateGpxFile) {
            boolean savedFileGPX = SaveFileGPX();
            AppState.setGpxFileCached(savedFileGPX);
            if (DEBUG) Log.d(TAG,"updated GPX File " + (savedFileGPX ? "saved" : "NOT saved"));
            // SettingsActivity.setGpxFileLoaded(gpxFileCached);
            updateGpxFile = !savedFileGPX;
        }
        super.onPause();
        AppState.setPaused(true);
    }

    @Override
    /*
     * This method is called after onPause()
     * Primary Responsibility: Release visibility-related resources
     */
    public void onStop() {
        if (DEBUG) Log.i(TAG,"onStop");
        super.onStop();
        stopTimer();
        startTimer(500);

        AppState.stopped = true;
    }

    @Override
    public void onLowMemory() {
        AppState.lowMemory = true;
    }

    @Override
    /*
     * Within the Android Activity lifecycle, onSaveInstanceState() is invoked
     * after onStop()
     * before the system may drop the activity from memory by simply killing its process, making it destroyed.
     * Primary Responsibility: save the state of the activity to completely restore it to its previous state after a restart
     * @see <a href="https://developer.android.com/reference/android/app/Activity#onSaveInstanceState(android.os.Bundle)">
     *      Activity Lifecycle</a> on developer.android.com
     */
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG) Log.i(TAG,"onSaveInstanceState(): close Log file");
        Log.Close();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"onDestroy");

        htmlFile.destroy(); htmlFile = null;

        recordAdapter.destroy ();

        if (DEBUG) {
            _destroyed = true;
            AppState.MainActivityInstanceCount--;
        }
        app.destroy();
        AppState.destroyed = true;

        super.onDestroy();
    }

    @Override
    /*
     * Prevent closing the App here when user pressed the Back key
     * @see https://developer.android.com/guide/components/activities/tasks-and-back-stack
     */
    public void onBackPressed()
    {
        if (!getExpandViewStatus()) {
            if (intentFromOtherApp)
                finish();
        }
        super.onBackPressed();
    }

    /**
     * Initialize the contents of the Activity's standard options menu
     * @param menu options menu
     * @return Pass the result along other messages from the UI
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onContextItemSelected (@NonNull MenuItem item)
    {
        return false;
    }

    /**
     * Handles Navigation menu options messages
     * @param item menu item clicked by user
     * @return true if message was handled, otherwise pass along other messages from the UI
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(GravityCompat.START);

        // return if we are not going to another page
        if (id == R.id.nav_file_info) {
            showFileInfo();
            return true;
        }
        else if (id == R.id.nav_remote_waypoints)
        {
            // add waypoints along the track which are provided by the GPX file but are out of track
            WaypointsDialogFragment dialog = WaypointsDialogFragment.newInstance(this);
            dialog.setNotification(this::updateRecords);
            dialog.show(getSupportFragmentManager(),"WaypointsDialogFragment");
            return true;
        }
        else if (id == R.id.nav_osm_guideposts)
        {
            // find OSM guideposts along the track
            OpenStreetMapDialogFragment dialog = OpenStreetMapDialogFragment.newInstance(this);
            dialog.setTitle (getString(R.string.osm_guide_posts_title));
            dialog.queryBoundingBox(0.01);
            dialog.findGuideposts();
            dialog.setNotification(this::updateRecords);
            dialog.show(getSupportFragmentManager(),"OpenStreetMapDialogFragment");
            return true;
        }
        else if (id == R.id.nav_osm_pois)
        {
            // find OSM POIs along the track
            OpenStreetMapDialogFragment dialog = OpenStreetMapDialogFragment.newInstance(this);
            dialog.setTitle (getString(R.string.osm_pois_title));
            dialog.queryBoundingBox(0.01);
            dialog.findPOIs();
            dialog.setNotification(this::updateRecords);
            dialog.show(getSupportFragmentManager(),"OpenStreetMapDialogFragment");
            return true;
        }
        else if (id == R.id.nav_wikipedia) {
            WikipediaDialogFragment dialog = WikipediaDialogFragment.newInstance(this);
            dialog.setTitle(getString(R.string.wikipedia_title));
            dialog.queryBoundingBox(0.02);
            dialog.setNotification(this::updateRecords);
            dialog.show(getSupportFragmentManager(), "WikipediaDialogFragment");
            return true;
        }
        else if (id == R.id.nav_start_time) {
            /* Change the start time of the tour */
            changeStartTime();
            return true;
        } else if (id == R.id.nav_reverse_route) {
            super.app.reverseRoute();
            return true;
        }
        try {
            goToNavigationItem(id);
        } catch (IOException ignored) {
        }
        return true;
    }

    /**
     * Handles menu options messages
     * @param item menu item clicked by user
     * @return true if message was handled, otherwise pass along other messages from the UI
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        setTrackingStatus(false);

        // Resource IDs will be non-final by default in Android Gradle Plugin version 8.0, avoid using them in switch case statements
        if (id == R.id.itm_break_time)
            /* Change the pause time of the current waypoint */
            changeBreakTime();
        else if (id == R.id.itm_comment_waypoint)
            /* comment the current waypoint */
            commentRoutePoint();
        else if (id == R.id.itm_nav_waypoint)
            /* Navigate to the waypoint */
            navigateToRoutePoint();
        else if (id == R.id.itm_nav_swv_tourenportal)
            /* Navigate to Schwarzwaldverein Tourenportal */
            navigateToSwvTourenportal();
        else if (id == R.id.itm_nav_google)
            /* Navigate with Google */
            navigateWithGoogle();
        else if (id == R.id.itm_find_nearby_wikipedia)
            /* find nearby Wikipedia articles */
            findNearbyWikipedia();
        else if (id == R.id.itm_find_nearby_osm)
            /* find nearby OSM POIs */
            getNearbyOSM();
        else if (id == R.id.itm_delete_routepoint)
            /* Delete the current waypoint */
            deleteRoutePoint();
        else if (id == R.id.itm_delete_trackpoints)
            /* Delete all following trackpoints */
            deleteTrackPoints();
        else if (id == R.id.itm_set_new_start) {
            setNewStart();
            // todo create intent ?
            app.Update();
        }
        else if (id == android.R.id.home) {
            /* Respond to the action bar's Up/Home button */
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        else
            return super.onOptionsItemSelected(item);

        clearErrorMessage();

        return true;
    }

    /**
     *  Call back function to handle activity results
     */
    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_OPEN_GPX) {
                OpenFileGPX(data);
            }
        }
    }

    /**
     * update all places in the records view
     */
    public synchronized void updateRecords() {
        _updateRecords = true;
    }

    /**
     *  Register an activity to load the GPX file
     *  @see <a href="https://developer.android.com/training/basics/intents/result#java">developer.android.com</a>
     */
    public void registerActivityResultLauncherOpenDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/octet-stream").addCategory(Intent.CATEGORY_OPENABLE);
        OpenDocumentActivityResultLauncher.launch(intent);
    }

    //Instead of onActivityResult() method use this one
    ActivityResultLauncher<Intent> OpenDocumentActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null)
                        OpenFileGPX(data);
                }
            });

    /**
     * Register an activity to store the GPX file
     * @see <a href="https://developer.android.com/training/basics/intents/result#java">developer.android.com</a>
     */
    public void registerActivityResultLauncherCreateDocumentGPX() {
        // Intent creation
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
        // will trigger exception if no  appropriate category passed
        intent.addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/gpx+xml")
                .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                .putExtra(Intent.EXTRA_TITLE, gpxFileName);
        CreateDocumentGPXActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> CreateDocumentGPXActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    SaveFileGPX(result.getData());
                }
            });

    /**
     *  Register an activity to store the HTML file
     *  @see <a href="https://developer.android.com/training/basics/intents/result#java">developer.android.com</a>
     */
    public void registerActivityResultLauncherCreateDocumentHTML() {
        // Intent creation
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        // will trigger exception if no  appropriate category passed
        intent.addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/html")
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS)
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, DIRECTORY_DOWNLOADS)
                .putExtra(Intent.EXTRA_TITLE, gpxFileName.replace("gpx", "html"));
        CreateDocumentHTMLActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> CreateDocumentHTMLActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Here, no request code
                    Intent data = result.getData();
                    if (data != null)
                        SaveFileHTML(data);
                }
            });

    /**
     * Change the start time of the tour
     */
    private void changeStartTime() {
        if (recordAdapter.getCount() > 0) {
            StartTimeDialog startTimeDialog = new StartTimeDialog(this);
            startTimeDialog.show();
        }
    }

    /**
     * Notify the application of the changed start time of the tour
     */
    public void notifyStartTimeChanged(int inStartTime) {
        _changeStartTime = inStartTime;
    }

    protected void onStartTimeChanged(int inStartTime) {
        super.onStartTimeChanged(_changeStartTime);
    }

    /**
     * Change the pause time of the current waypoint
     */
    void changeBreakTime() {
        if (recordAdapter.getPlace() > 0) {
            BreakTimeDialog breakTimeDialog = new BreakTimeDialog(this, recordAdapter, super.app);
            breakTimeDialog.show();
        }
    }

    /**
     * Edit the comment of the current waypoint
     */
    public void commentRoutePoint()
    {
        CommentDialog commentDialog = new CommentDialog(this, recordAdapter);
        commentDialog.show();
    }

    /**
     * Delete the waypoint linked to the current route point
     */
    public void deleteRoutePoint() {
        try {
            // Get the selected record
            if (recordAdapter == null) return;
            int selected = recordAdapter.getPlace();
            if (selected < 0) return;
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record == null) return;
            DataPoint dataPoint = record.trackPoint;
            if (dataPoint == null) return;

            // is a waypoint linked to the routepoint?
            if (dataPoint.getLinkIndex() >= 0) {
                Track track = App.getTrack();
                if (track == null) return;

                /* check the waypoint */
                DataPoint linkedWaypoint = track.getPoint(dataPoint.getLinkIndex());
                if (linkedWaypoint == null) return;
                int indexWaypoint = linkedWaypoint.getIndex();
                if (indexWaypoint < 0) return;

                /* walk through all trackpoints linking to this waypoint */
                int linkIndexFirst = linkedWaypoint.getLinkIndex();
                if (linkIndexFirst < 0) return;

                int linkIndex = linkIndexFirst;
                do {
                    DataPoint trackPoint = track.getPoint(linkIndex);
                    if (trackPoint == null) return;
                    // plausibility check: are corresponding link indices identical?
                    if (trackPoint.getLinkIndex() != indexWaypoint) return;

                    /* preserve all affected route points which shall not be deleted */
                    linkIndex = trackPoint.getLinkIndexNext();
                    if (trackPoint != dataPoint)
                        // make a route point out of the track point using attributes from the waypoint
                        trackPoint.makeRoutePointFrom(linkedWaypoint);
                    else
                        dataPoint.removeRoutePoint();

                } while (linkIndex != INVALID_INDEX);

                // make the waypoint invalid - thus it can remain in the tracklist without
                // the need to update indices
                linkedWaypoint.makePointInvalid();
                dataPoint.clearWayPointLink();
            } else {
                // no waypoint is linked to the routepoint: remove all attributes of the trackpoint
                dataPoint.removeRoutePoint();
            }

            // do not remove the routepoint - reduce it to a simple trackpoint without name and description
            recordAdapter.recordList.remove(selected);
            recordAdapter.notifyDataSetChanged();
        }
        catch (Exception ignored) {
        }
    }

    /**
     * Delete all following trackpoints
     */
    private void deleteTrackPoints() {
        int selected = recordAdapter.getPlace();
        if (selected >= 0) {
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record != null) {
                TrackDetails track = App.getTrack();
                if (track != null) {
                    track.deleteRange(record.trackPointIndex+1,track.getNumTrackPoints()-1);
                    // todo create intent ?
                    App.app.Update();
                    super.profileAdapter.initPlot(track);
                }
            }
        }
    }

    /**
     * find nearby Wikipedia articles
     */
    public void findNearbyWikipedia() {
        int selected = recordAdapter.getPlace();
        if (selected >= 0) {
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record != null) {
                WikipediaDialogFragment dialog = WikipediaDialogFragment.newInstance(this);
                dialog.setTitle(getString(R.string.wikipedia_title));
                dialog.queryAround(record.trackPoint);
                dialog.setNotification(this::updateRecords);
                dialog.show(getSupportFragmentManager(),"WikipediaDialogFragment");
            }
        }
    }

    /**
     * find nearby OSM POIs
     */
    public void getNearbyOSM() {
        int selected = recordAdapter.getPlace();
        if (selected >= 0) {
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record != null) {
                // find OSM POIs
                OpenStreetMapDialogFragment dialog = OpenStreetMapDialogFragment.newInstance(this);
                dialog.setTitle (getString(R.string.osm_nearby_pois_title));
                dialog.queryAround(record.trackPoint);
                dialog.findPOIs();
                dialog.setNotification(this::updateRecords);
                dialog.show(getSupportFragmentManager(),"OpenStreetMapDialogFragment");
            }
        }
    }

    /**
     * Format geo coordinates for internal use
     * @return formatted Latitude
     * @param point the date point object
     */
    String formatLatitude(DataPoint point) {
        if (point != null) {
            double lat = point.getLatitude().getDouble();
            return new DecimalFormat("#0.00000").format(lat).replace(',', '.');
        } else
            return "";
    }

    /**
     * Format geo coordinates for internal use
     * @return formatted Longitude
     * @param point the date point object
     */
    String formatLongitude(DataPoint point) {
        if (point != null) {
            double lon = point.getLongitude().getDouble();
            return new DecimalFormat("#0.00000").format(lon).replace(',', '.');
        } else
            return "";
    }

    /**
     * set new start point of the tour on user demand
     */
    void setNewStart()
    {
        if (recordAdapter.getCount() > 0) {
            /* Get destination coordinates */
            RecordAdapter.Record record = recordAdapter.getItem(recordAdapter.getPlace());
            if (record != null) {
                App.getTrack().setNewStart(record.trackPointIndex);
            }
        }
    }


    /**
     *  Navigate to the route point with another app
     */
    void navigateToRoutePoint() {
        if (recordAdapter.getCount() > 0) {
            /* Get destination coordinates */
            RecordAdapter.Record record = recordAdapter.getItem(recordAdapter.getPlace());
            if (record != null) {
                DataPoint point = record.trackPoint;
                if (point != null)
                    // is the track point linked to a way point?
                    if (point.getLinkIndex() >= 0)
                        // take it
                        point = App.getTrack().getPoint(point.getLinkIndex());
                if (point != null) {
                    String queryParameter = "geo:" + formatLatitude(point) + "," + formatLongitude(point);
                    /* Navigate e.g. with DB Navigator */
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse(queryParameter));
                    startActivity(intent);
                }
            }
        }
    }

    /**
     * Navigate to the route point in external web browser with Schwarzwaldverein Tourenportal
     * @implNote final web view depends on browser (working in Firefox, not in Brave!)
     */
    private void navigateToSwvTourenportal() {
        if (recordAdapter != null && recordAdapter.getCount() > 0) {
            /* Get destination coordinates */
            RecordAdapter.Record record = recordAdapter.getItem(recordAdapter.getPlace());
            if (record != null) {
                DataPoint point = record.trackPoint;
                if (point != null) {
                    /* Show the map at the given latitude and longitude */
                    String area = "*";
                    String filter = "r-fullyTranslatedLangus-,r-openState-,sb-sortedBy-0";
                    String zc = "16.," + formatLongitude(point) + "," + formatLatitude(point);
            /*
                Option 1: build full fragment
                   String fragment = buildFragment(area, filter, zc);
                    Uri uri = Uri.parse("https://www.schwarzwaldverein-tourenportal.de/mobile/de/touren#" + fragment);
            */
            /*
                Option 2: use Uri.Builder() - encodes uri - thus it cannot be used in outdooractive:
                    Uri uri = new Uri.Builder()
                            .scheme("https")
                            .authority("www.schwarzwaldverein-tourenportal.de")
                            .appendPath("mobile")
                            .appendPath("de")
                            .appendPath("touren")
                            .appendPath("")
                            .fragment(fragment) //.fragment(Uri.encode(fragment, "&=*,.-"))
                            .build();
            */
            /*
                Option 3: ignore fragment
            */
                    Uri uri = Uri.parse("https://www.schwarzwaldverein-tourenportal.de/mobile/de/touren#zc=" + zc);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
            }
        }
    }

    /**
     * Navigate to the route point with Google Maps
     */
    void navigateWithGoogle() {
        if (recordAdapter.getCount() > 0) {
            RecordAdapter.Record record = recordAdapter.getItem(recordAdapter.getPlace());
            if (record != null) {
                if (Prefs.getConsentGoogleMaps())
                    runGoogleMaps();
            }
        }
    }

    /**
     * Navigate to the route point with Google Maps
     *
     * @link <a href="https://developers.google.com/maps/documentation/urls/get-started#directions-action">developers.google.com</a>
     * @link <a href="https://bitcoden.com/answers/launching-google-maps-directions-via-an-intent-on-android">bitcoden.com</a>
     */
    public void runGoogleMaps() {
        if (recordAdapter.getCount() > 0) {
            /* Get destination coordinates */
            RecordAdapter.Record record = recordAdapter.getItem(recordAdapter.getPlace());
            if (record != null) {
                DataPoint point = record.trackPoint;
                if (point != null) {
                    String queryParameter = formatLatitude(point) + "," + formatLongitude(point);
                    /* Show the map at the given latitude and longitude, eg.: https://www.google.com/maps/dir/?api=1&destination=48.40475,8.01058 */
                    Uri.Builder directionsBuilder = new Uri.Builder()
                            .scheme("https")
                            .authority("www.google.com")
                            .appendPath("maps")
                            .appendPath("dir")
                            .appendPath("")
                            .appendQueryParameter("api", "1")
                            .appendQueryParameter("destination", queryParameter);
                    Intent intent = new Intent(Intent.ACTION_VIEW, directionsBuilder.build());
                    startActivity(intent);
                }
            }
        }
    }

    /**
     * set active navigation item
     * @param itemId index of the menu item
     */
    private void selectNavigationItem(int itemId) {
        for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
            boolean b;
            b = (itemId == mNavigationView.getMenu().getItem(i).getItemId());
            mNavigationView.getMenu().getItem(i).setChecked(b);
        }
    }

    /**
     * Manager for main navigation items with own activities
     * @param id identifier for the menu item.
     */
    private void goToNavigationItem(int id) throws IOException {
        Intent intent;

        if (id == R.id.nav_open_gpx)
            OpenFileGPX();
        else if (id == R.id.nav_reverse_route)
            App.getTrack().reverseRoute();
        else if (id == R.id.nav_download_gpx)
            registerActivityResultLauncherCreateDocumentGPX();
        else if (id == R.id.nav_download_html)
            registerActivityResultLauncherCreateDocumentHTML();
        else if (id == R.id.nav_about) {
            // open about page
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (id == R.id.nav_settings) {
            // open settings page
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (id == R.id.nav_help) {
            // open help page
            intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (id == R.id.nav_timetable) {
            StringBuffer html = htmlFile.formatTimetableToHTML(recordAdapter,tourDetails,false);
            // open Time Table page
            intent = new Intent(this, TimeTableActivity.class);
            intent.putExtra("contents",html.toString());
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    /**
     * Open GPX file
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void OpenFileGPX(View view) {
        OpenFileGPX();
    }

    /**
     * Open GPX file
     */
    public void OpenFileGPX() {
        AppState.clearNavigationState();
        Log.clearDebugHTML();
        clearErrorMessage();
        registerActivityResultLauncherOpenDocument();
    }

    /**
     * Open GPX file via intent from another app
     * @param data Intent data
     */
    void OpenFileGPX(final Intent data) {
        AppState.clearNavigationState();
        resetGpsIndex();
        Log.clearDebugHTML();
        clearErrorMessage();

        Uri uriFile = data.getData(); //The uri with the location of the file
        // Get the File path from the Uri for Storage Access Framework Documents
        String uriFilePath = FileUtils.getPath(this, uriFile);
        String ext = FileUtils.getExtension(uriFilePath);
        if (DEBUG) {
            Log.d(TAG, "OpenFileGPX(): uri = " + uriFile);
            // Log.d(TAG, "GPX file name: " + uriFilePath);
            assert uriFile != null;
            // Log.d(TAG, "isExternalStorageDocument: " + FileUtils.isExternalStorageDocument(uriFile));
        }
        if (ext.startsWith(".gpx")) {
            gpxFileName = FileUtils.getFileName(this,uriFile);
            // if (DEBUG) Log.d(TAG, "Load GPX file: " + uriFile);

            try {
                InputStream _xmlStream = null;
                try {
                    assert uriFile != null;
                    _xmlStream = this.getContentResolver().openInputStream(uriFile);
                    App.gpxUri = uriFile;
                } catch (FileNotFoundException ignored) { }
                // if (DEBUG) Log.d(TAG, "Open GPX stream");
                XmlFileLoader xmlFileLoader = new XmlFileLoader(super.app);
                xmlFileLoader.openStream(_xmlStream, _autoAppend, app::informDataLoadComplete);
                if (DEBUG) Log.d(TAG, "-> XmlFileLoader(app::informDataLoadComplete)");
            } catch (Exception ignored) { }
        }
        else {
            showErrorMessage(getString(R.string.error_invalid_gpx_file));
        }
    }

    /**
     * Save GPX file
     * @param data Intent data
     */
    void SaveFileGPX(final Intent data) {
        try {
            if (DEBUG) Log.d(TAG, "SaveFileGPX(intent)");
            if (data != null)
            {
                Uri uri = data.getData(); //The uri with the location of the file
                ContentResolver cr = this.getContentResolver();
                assert uri != null;
                OutputStream _xmlStream = cr.openOutputStream(uri, "w");
                OutputStreamWriter writer = new OutputStreamWriter(_xmlStream, StandardCharsets.UTF_8);
                GpxExporter.downloadData(writer,app.getTrackInfo());

                // exchange wrong file extension provided by Android: *.gpx (x) > * (x).gpx
                try {
                    String path = uri.getPath();
                    assert path != null;
                    int dot = path.lastIndexOf("/");
                    if (dot > 0) {
                        String filename = path.substring(dot+1);
                        dot = filename.lastIndexOf(".");
                        if (dot > 0) {
                            filename = filename.substring(0,dot);
                            DocumentsContract.renameDocument(cr, uri, filename+".gpx");
                        }
                    }
                }
                catch (FileNotFoundException ignored) {  }
            }

        } catch (FileNotFoundException e) {
            if (DEBUG) Log.e(TAG, "Download failed");
        } catch (IOException ignored) {  }

        if (DEBUG) Log.d(TAG, "Download was successfully");
    }

    /**
     * Reload a GPX file via URI
     *
     * @param uriFile URI of the file
     */
    public void OpenFileGPX(Uri uriFile ) {
        try {
            InputStream _xmlStream;
            try {
                assert uriFile != null;
                _xmlStream = this.getContentResolver().openInputStream(uriFile);
            } catch (FileNotFoundException e) { return; }
            if (DEBUG) Log.d(TAG, "OpenFileGPX(Uri) uri =" + uriFile.getPath());
            XmlFileLoader xmlFileLoader = new XmlFileLoader(super.app);
            xmlFileLoader.openStream(_xmlStream, _autoAppend, app::informUriFileLoadComplete);
            if (DEBUG) Log.d(TAG, "GPX file loaded?");
        } catch (Exception ignored) {  }
    }

    /**
     * Load a GPX file from cache
     */
    public void OpenCachedFileGPX() {
        try {
            android.content.Context context = getApplicationContext();
            File cacheDir = FileUtils.getDocumentCacheDir(context); // FileUtils.getDownloadsDir();
            File file = new File (cacheDir, "TourNavigator.gpx");

            if(DEBUG) Log.d (TAG, "OpenCachedFileGPX():");

            XmlFileLoader xmlFileLoader = new XmlFileLoader(super.app);
            xmlFileLoader.openFile(file, _autoAppend, app::informDataLoadComplete);
            if (DEBUG) Log.d(TAG, "- GPX file loaded?");
        } catch (Exception e) {
            Log.e(TAG,"- cached GPX file not loaded: ", e);
        }
    }

    /*
     * Save the GPX file in the cache
     */
    boolean SaveFileGPX() {
        android.content.Context context = getApplicationContext();
        File cacheDir = FileUtils.getDocumentCacheDir(context);
        boolean res = true;

        try {
            if (DEBUG) Log.d (TAG, "SaveFileGPX()");

            // Create a new file in the internal directory
            File file = new File(cacheDir, "TourNavigator.gpx");
            if (file.exists())
                res = file.delete();
            if (res) {
                // Open a FileOutputStream to write to the file
                FileOutputStream xmlStream = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(xmlStream); // StandardCharsets.UTF_8);
                return (GpxExporter.downloadData(writer, app.getTrackInfo()) > 0);
            }
        } catch (IOException e) {
            Log.e(TAG,"SaveFileGPX(): failed to write to cache", e);
        }

        return false;
    }

    /**
     *  Save HTML file
     */
    void SaveFileHTML(final Intent data) {
        Uri uriFile = data.getData(); //The uri with the location of the file
        try {
            assert uriFile != null;
            OutputStream _xmlStream = this.getContentResolver().openOutputStream(uriFile, "w");
            StringBuffer html = htmlFile.formatTimetableToHTML(recordAdapter, tourDetails, true);
            OutputStreamWriter writer;
            try {
                writer = new OutputStreamWriter(_xmlStream, StandardCharsets.UTF_8);
                // write file
                writer.write(html.toString());

                // close file
                writer.close();
                assert _xmlStream != null;
                _xmlStream.close();
            } catch (IOException ignored) { }

        } catch (FileNotFoundException e) {
            Log.e(TAG,"SaveFileHTML()", e);
        }
    }

    /**
     * Pause Tracking
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void pauseTracking(View view) {
        setTrackingStatus(false);
    }

    /**
     * Continue Tracking
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void continueTracking(View view) {
        setTrackingStatus(true);
    }

    /**
     * Show route details
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void expand_less(View view) {
        clearErrorMessage();
        setExpandViewStatus(false);
    }

    /**
     * Show extended tour description
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void expand_more(View view) {
        clearErrorMessage();
        setExpandViewStatus(true);
    }

    /**
     * Speaks the description text
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void text_to_speech(View view) {
        speakAddInfo();
    }

    public void voice_selection_off(View v) {
        stopSpeaking();
    }

    /**
     * Hide the altitude profile
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void hide_profile(View view) {
        clearErrorMessage();
        activateProfile(View.INVISIBLE);
    }

    /**
     * Show the altitude profile
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void show_profile(View view) {
        clearErrorMessage();
        activateProfile(View.VISIBLE);
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Private methods
     * --------------------------------------------------------------------------------------------
     *
     */

    /**
     * callback function to periodically update the user interface
     */
    @Override
    protected void updateUI() {
        if (DEBUG) {
            if (_destroyed) {
                Log.e(TAG,"leakage in timerRunnable");
                return;
            }
        }

        if (_changeStartTime >= 0) {
            onStartTimeChanged(_changeStartTime);
            _changeStartTime = -1;
        }

        if (App.getTrack() != null) {
            super.updateUI();

            if (_updateRecords) {
                if (App.getTrack() != null)
                    notifyDataSetChanged(App.getTrack().updateRecords());
                recordAdapter.notifyDataSetChanged();
                _updateRecords = false;
            }
        }
    }
}