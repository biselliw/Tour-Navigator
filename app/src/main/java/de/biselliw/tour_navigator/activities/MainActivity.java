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

    Copyright 2025 Walter Biselli (BiselliW)
*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.dialogs.AcceptGoogleMapsPolicyDialog;
import de.biselliw.tour_navigator.dialogs.PauseTimeDialog;
import de.biselliw.tour_navigator.dialogs.StartTimeDialog;
import de.biselliw.tour_navigator.files.FileUtils;
import de.biselliw.tour_navigator.files.HTML_File;
import de.biselliw.tour_navigator.helpers.GlobalExceptionHandler;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.helpers.ProfileAdapter;
import de.biselliw.tour_navigator.tim.prune.load.FileToBeLoaded;
import de.biselliw.tour_navigator.tim_prune.data.Track;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.load.xml.XmlFileLoader;
import de.biselliw.tour_navigator.tim_prune.save.GpxExporter;

import static android.app.Service.START_FLAG_REDELIVERY;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static de.biselliw.tour_navigator.activities.SettingsActivity._app;
import static de.biselliw.tour_navigator.activities.SettingsActivity.getConsentGoogleMaps;

/**
 * @since 26.1
 */
public class MainActivity extends LocationActivity  implements
        NavigationView.OnNavigationItemSelectedListener {

    boolean intentFromOtherApp = false;
    public static HTML_File htmlFile;
    private String gpxFileName = "";
    GpxExporter gpxExporter;
    private boolean gpxFileCached = false;

    /**
     * TAG for log messages.
     */
    static final String TAG = "MainActivity";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    int REQUEST_OPEN_GPX = 222;

    SharedPreferences sharedPref = null;
    boolean firstStart = false;

    @SuppressLint({"ResourceType", "MissingSuperCall"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SettingsActivity.setSharedPreferences(sharedPref);
        firstStart = SettingsActivity.isFirstTimeLaunch();

        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());

        super.app = new App(this);
        super.pa = new ProfileAdapter(this, super.app);

        SettingsActivity.getPreferences(sharedPref, super.app);

        gpxExporter = new GpxExporter(super.app);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        super.main = this;
        mNavigationView.setNavigationItemSelectedListener(this);
        selectNavigationItem(R.id.nav_main);

        htmlFile = new HTML_File(this, recordAdapter);

        String instanceState = "";
        if (savedInstanceState != null) instanceState = savedInstanceState.toString();
        Log.d (TAG, "onPostCreate (" + instanceState +")");

        // Handle a received intent from another app
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals("android.intent.action.VIEW")) {
            intentFromOtherApp = true;
            onActivityResult(REQUEST_OPEN_GPX, RESULT_OK, intent);
        }

        if (firstStart)
        {
            Intent mainIntent = new Intent(this, TutorialActivity.class);
            startActivity(mainIntent);
        }

        if (savedInstanceState != null)
        {
            gpxFileCached = savedInstanceState.getBoolean("gpxFileCached");
        }
        else
            // Load a GPX file from cache
            gpxFileCached = SettingsActivity.isGpxFileLoaded();

        if (gpxFileCached) {
            OpenCachedFileGPX();
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
        outState.putBoolean("gpxFileCached",gpxFileCached);
        Log.d(TAG,"onSaveInstanceState("+outState.toString()+")");
        Log.Close();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Log.getMemoryInfo();
    }


    @Override
    public void onRestart() {
        super.onRestart();
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    /*
     * This method is called when the app is no longer in the foreground and is partially visible.
     * This can happen when the user switches to another app or when the screen is turned off.
     * onPause() is a good place to save any unsaved data or state changes before the app is paused.
     */
    public void onPause() {
        // Save the GPX file in the cache?
        if (control.updateGpxFile) {
            gpxFileCached = SaveFileGPX();
            SettingsActivity.setGpxFileLoaded(gpxFileCached);
            control.updateGpxFile = false;
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        recordAdapter.notifyDataSetChanged();
        super.onResume();
    }


    @Override
    public void onLowMemory() {}

    @Override
    /*
     * Prevent closing the App here when user pressed the Back key
     * @see https://developer.android.com/guide/components/activities/tasks-and-back-stack
     */
    public void onBackPressed()
    {
        if (!getExpandViewStatus())
            if (intentFromOtherApp)
                finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
     * Handles menu options messages
     * @param item menu item clicked by user
     * @return true if message was handled, otherwise pass along other messages from the UI
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        control.setTrackingStatus(false);

        if (id == R.id.itm_pause_time)
            /* Change the pause time of the current waypoint */
            changePauseTime();
        else if (id == R.id.itm_comment_waypoint)
            /* comment the current waypoint */
            commentRoutePoint();
        else if (id == R.id.itm_delete_waypoint)
            /* Delete the current waypoint */
            deleteRoutePoint();
        else if (id == R.id.itm_delete_trackpoints)
            /* Delete all following trackpoints */
            deleteTrackPoints();
        else if (id == R.id.itm_set_new_start) {
            setNewStart();
            super.app.Update();
        }
        else if (id == R.id.itm_nav_waypoint)
            /* Navigate to the waypoint */
            navigateToRoutePoint();
        else if (id == R.id.itm_nav_google)
            /* Navigate with Google */
            navigateWithGoogle();
        else if (id == android.R.id.home) {
            /* Respond to the action bar's Up/Home button */
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else
            return super.onOptionsItemSelected(item);
        clearErrorMessage();

        return true;
    }

    /*
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
     * Handles Navigation menu options messages
     * @param item menu item clicked by user
     * @return true if message was handled, otherwise pass along other messages from the UI
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();

        mDrawerLayout.closeDrawer(GravityCompat.START);

        // return if we are not going to another page
        if (id == R.id.nav_main) {
            return true;
        } else if (id == R.id.nav_file_info) {
            control.showFileInfo();
            return true;
        } else if (id == R.id.nav_start_time) {
            /* Change the start time of the tour */
            changeStartTime();
            super.app.Update();
            return true;
        } else if (id == R.id.nav_reverse_route) {
            super.app.reverseRoute();
            return true;
        }
        // delay transition so the drawer can close
        mHandler.postDelayed(() -> {
            try {
                goToNavigationItem(id);
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }, NAVDRAWER_LAUNCH_DELAY);

        // fade out the active activity
        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
        }

        return true;
    }

    /**
     *  Register an activity to load the GPX file
     *  @see <a href="https://developer.android.com/training/basics/intents/result#java">developer.android.com</a>
     */
    public void registerActivityResultLauncherOpenDocument() {

        // Define the MIME type you want to filter by
//        final String mimeType = "application/gpx+xml";
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
        //        .putExtra(Intent.EXTRA_MIME_TYPES, new String[] { mimeType });
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
                // todo Field requires API level 26 (current min is 23): `android.provider.DocumentsContract#EXTRA_INITIAL_URI`
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
                // todo Field requires API level 26 (current min is 23): `android.provider.DocumentsContract#EXTRA_INITIAL_URI`
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
     *  Change the start time of the tour
     */
    void changeStartTime() {
        if (recordAdapter.getCount() > 0) {
            StartTimeDialog startTimeDialog = new StartTimeDialog(MainActivity.this, recordAdapter);
            startTimeDialog.show();
        }
    }

    /**
     * Change the pause time of the current waypoint
     */
    void changePauseTime() {
        if (recordAdapter.getPlace() > 0) {
            PauseTimeDialog pauseTimeDialog = new PauseTimeDialog(MainActivity.this, recordAdapter, super.app);
            pauseTimeDialog.show();
        }
    }

    /**
     * Edit the comment of the current waypoint
     */
    public void commentRoutePoint()
    {
        Intent intent = new Intent(this, CommentActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
    }

    /**
     * Delete the current route point from the track
     */
    public void deleteRoutePoint() {
        int selected = recordAdapter.getPlace();
        if (selected >= 0) {
            // Clear pause time in advance
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record != null) {
                DataPoint dataPoint = record.getTrackPoint();
                dataPoint.setWaypointDuration(0);

                recordAdapter.recordList.remove(selected);
                recordAdapter.notifyDataSetChanged();

                Track track = App.getTrack();
                if (track != null) {
                    if ((record.trackPointIndex >= 0) && (record.trackPointIndex < track.getNumPoints())) {
                        track.deletePoint(record.trackPointIndex);
                        _app.Update();
                    }
                }
            }
        }
    }

    /**
     * Delete all following trackpoints
     */
    public void deleteTrackPoints() {
        int selected = recordAdapter.getPlace();
        if (selected >= 0) {
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record != null) {
                Track track = App.getTrack();
                if (track != null) {
                    track.deleteRange(record.trackPointIndex+1,track.getNumPoints()-1);
                    _app.Update();
                    super.pa.initPlot();
                }
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
                DataPoint point = record.getTrackPoint();
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
     * Navigate to the route point with Google Maps
     *
     */
    void navigateWithGoogle() {
        if (recordAdapter.getCount() > 0) {
            RecordAdapter.Record record = recordAdapter.getItem(recordAdapter.getPlace());
            if (record != null) {
                if (getConsentGoogleMaps())
                    runGoogleMaps();
                else {
                    AcceptGoogleMapsPolicyDialog acceptDialog = new AcceptGoogleMapsPolicyDialog(MainActivity.this, this);
                    acceptDialog.show();
                }
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
                DataPoint point = record.getTrackPoint();
                if (point != null) {
                    String queryParameter = formatLatitude(point) + "," + formatLongitude(point);
                    /* Show the map at the given latitude and longitude */
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
     * @param itemId index of the route point within the record list
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
        else if (id == R.id.nav_start_time) {
            /* Change the start time of the tour */
            changeStartTime();
        } else if (id == R.id.nav_reverse_route)
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
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (id == R.id.nav_help) {
            // open help page
            intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (id == R.id.nav_timetable) {
            htmlFile.formatTimetableToHTML(false);
            // open Time Table page
            intent = new Intent(this, TimeTableActivity.class);
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
        registerActivityResultLauncherOpenDocument();
    }

    /**
     * Open GPX file via intent from another app
     * @param data Intent data
     */
    void OpenFileGPX(final Intent data) {
        // delete any previously used cache
        FileUtils.deleteDocumentCacheDir(this);
        clearErrorMessage();

        Uri uriFile = data.getData(); //The uri with the location of the file
        // Get the File path from the Uri for Storage Access Framework Documents
        String uriFilePath = FileUtils.getPath(this, uriFile);
        String ext = FileUtils.getExtension(uriFilePath);
        if (DEBUG) {
            Log.d(TAG, "GPX file URI: " + uriFile);
            Log.d(TAG, "GPX file name: " + uriFilePath);
            assert uriFile != null;
            Log.d(TAG, "isExternalStorageDocument: " + FileUtils.isExternalStorageDocument(uriFile));
        }
        if (ext.startsWith(".gpx")) {
            gpxFileName = FileUtils.getFileName(this,uriFile);
            if (DEBUG) Log.d(TAG, "Load GPX file: " + uriFile);
            XmlFileLoader _xmlFileLoader = new XmlFileLoader(super.app);

            try {
                InputStream _xmlStream = null;
                try {
                    assert uriFile != null;
                    _xmlStream = this.getContentResolver().openInputStream(uriFile);
                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
                }
                if (DEBUG) Log.d(TAG, "Open GPX stream");
                File file = FileUtils.getFile(this,uriFile);
                FileToBeLoaded gpx_file = new FileToBeLoaded(file,null);
                _xmlFileLoader.openStream(_xmlStream);
//                _xmlFileLoader.openStream(_xmlStream);
                if (DEBUG) Log.d(TAG, "GPX file loaded?");
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
        else
            control.showErrorMessage(getString(R.string.error_invalid_gpx_file));
    }

    /**
     * Save GPX file
     * @param data Intent data
     */
    void SaveFileGPX(final Intent data) {
        try {
            if (data != null)
            {
                Uri uri = data.getData(); //The uri with the location of the file
                ContentResolver cr = this.getContentResolver();
                assert uri != null;
                OutputStream _xmlStream = cr.openOutputStream(uri, "w");
                OutputStreamWriter writer = new OutputStreamWriter(_xmlStream, StandardCharsets.UTF_8);
                GpxExporter.downloadData(writer);

                // exchange wrong file extension provided by Android: *.gpx (x) > * (x).gpx
                try {
                    String path = uri.getPath();
                    assert path != null;
                    int dot = path.lastIndexOf("/");
                    if (dot > 0) {
                        String filename = path.substring(dot+1);
                        dot = filename.lastIndexOf(".");
                        if (dot > 0) {
//                            String ext = filename.substring(dot);
                            filename = filename.substring(0,dot);
                            DocumentsContract.renameDocument(cr, uri, filename+".gpx");
                        }
                    }
                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
                }
            }

        } catch (FileNotFoundException e) {
            if (DEBUG) {
                Log.d(TAG, "Download failed");
            }
//            e.printStackTrace();
        } catch (IOException e) {
//            e.printStackTrace();
        }

        if (DEBUG) Log.d(TAG, "Download was successfully");
    }

    /**
     * Load a GPX file from cache
     */
    boolean OpenCachedFileGPX() {
        android.content.Context context = getApplicationContext();
        File cacheDir = FileUtils.getDocumentCacheDir(context);

        try {
            Log.d (TAG, "OpenCachedFileGPX()");
            // Create a new file in the internal directory
/*
            FileToBeLoaded file = new FileToBeLoaded(cacheDir, "TourNavigator.gpx");
            if (DEBUG) Log.d(TAG, "Load GPX file from cache");
            XmlFileLoader xmlFileLoader = new XmlFileLoader(super.app);
            xmlFileLoader.openFile(file,true);
            if (DEBUG) Log.d(TAG, "GPX file loaded?");

 */
            return true;
        } catch (Exception e) {
            Log.e(TAG,"OpenCachedFileGPX()", e);
        }
        return false;
    }

    /*
     * Save the GPX file in the cache
     */
    boolean SaveFileGPX() {
        android.content.Context context = getApplicationContext();
        File cacheDir = FileUtils.getDocumentCacheDir(context);

        try {
            Log.d (TAG, "SaveFileGPX()");

            // Create a new file in the internal directory
            File file = new File(cacheDir, "TourNavigator.gpx");
            if (file.exists())
                file.delete();
            // Open a FileOutputStream to write to the file
            FileOutputStream xmlStream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(xmlStream); // StandardCharsets.UTF_8);
            GpxExporter.downloadData(writer);
            return true;
        } catch (IOException e) {
            Log.e(TAG,"SaveFileGPX()", e);
        }
        return false;
    }

    /*
     *  Save HTML file
     */
    void SaveFileHTML(final Intent data) {
        Uri uriFile = data.getData(); //The uri with the location of the file
        try {
            assert uriFile != null;
            htmlFile.SaveFileHTML(this.getContentResolver().openOutputStream(uriFile, "w"));
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
        clearErrorMessage();
        control.setTrackingStatus(false);
    }

    /**
     * Continue Tracking
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void continueTracking(View view) {
        clearErrorMessage();
        control.setTrackingStatus(true);
    }

    /**
     * Show route details
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void expand_less(View view) {
        clearErrorMessage();
        control.setExpandViewStatus(false);
    }

    /**
     * Show extended tour description
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void expand_more(View view) {
        clearErrorMessage();
        control.setExpandViewStatus(true);
    }

    /**
     * Hide the altitude profile
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void hide_profile(View view) {
        clearErrorMessage();
        control.activateProfile(View.INVISIBLE);
    }

    /**
     * Show the altitude profile
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void show_profile(View view) {
        clearErrorMessage();
        control.activateProfile(View.VISIBLE);
    }

}