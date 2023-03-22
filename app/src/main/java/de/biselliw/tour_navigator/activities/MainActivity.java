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

    Copyright 2023 Walter Biselli (BiselliW)
*/

import android.Manifest;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;

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
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.dialogs.PauseTimeDialog;
import de.biselliw.tour_navigator.dialogs.StartTimeDialog;
import de.biselliw.tour_navigator.files.FileUtils;
import de.biselliw.tour_navigator.files.HTML_File;
import de.biselliw.tour_navigator.helpers.ProfileAdapter;
import de.biselliw.tour_navigator.tim_prune.UpdateMessageBroker;
import de.biselliw.tour_navigator.tim_prune.data.Track;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;
import de.biselliw.tour_navigator.tim_prune.load.xml.XmlFileLoader;
import de.biselliw.tour_navigator.tim_prune.save.GpxExporter;

import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static de.biselliw.tour_navigator.activities.SettingsActivity._app;
import static de.biselliw.tour_navigator.files.FileUtils.DOCUMENTS_DIR;

public class MainActivity extends LocationActivity  implements
        NavigationView.OnNavigationItemSelectedListener {

    boolean intentFromOtherApp = false;
    public static HTML_File htmlFile;
    private String gpxFileName = "";
    GpxExporter gpxExporter;

    /**
     * TAG for log messages.
     */
    static final String TAG = "MainActivity";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    int REQUEST_OPEN_GPX = 222;

    SharedPreferences sharedPref = null;
    boolean firstStart = false;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SettingsActivity.setSharedPreferences(sharedPref);
        firstStart = SettingsActivity.isFirstTimeLaunch();

        if(firstStart) {

        }
//        else
        {
            super.app = new App(this);
            super.pa = new ProfileAdapter(this, super.app);

            SettingsActivity.getPreferences(sharedPref, super.app);

            gpxExporter = new GpxExporter(super.app);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        super.main = this;
        mNavigationView.setNavigationItemSelectedListener(this);
        selectNavigationItem(R.id.nav_main);

        htmlFile = new HTML_File(this, recordAdapter);

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
    }

    @Override
    public void onResume() {
        recordAdapter.notifyDataSetChanged();
        super.onResume();
    }

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


    /*
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
    public boolean onContextItemSelected (MenuItem item)
    {
        return false;
    }


    /*
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

    /*
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
                e.printStackTrace();
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
        intent.addCategory(Intent.CATEGORY_DEFAULT) // CATEGORY_OPENABLE)
                .setType("application/gpx+xml")
                // todo Field requires API level 26 (current min is 23): `android.provider.DocumentsContract#EXTRA_INITIAL_URI`
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
     * set new start point
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
     * @link https://developers.google.com/maps/documentation/urls/get-started#directions-action
     * @link https://bitcoden.com/answers/launching-google-maps-directions-via-an-intent-on-android
     */
    void navigateWithGoogle() {
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
     * Manager for main navigation items with own actvities
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
//            downloadFileGPX();
            registerActivityResultLauncherCreateDocumentGPX();
        else if (id == R.id.nav_download_html)
            registerActivityResultLauncherCreateDocumentHTML();
            // -------------------------------------------------
        else if (id == R.id.nav_about) {
            //open about page
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (id == R.id.nav_settings) {
            //open settings
            intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (id == R.id.nav_help) {
            //open about page
            intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (id == R.id.nav_timetable) {
            htmlFile.formatTimetableToHTML(false);
            //open Time Table page
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

        Uri uriFile = data.getData(); //The uri with the location of the file
        // Get the File path from the Uri for Storage Access Framework Documents
        String uriFilePath = FileUtils.getPath(this, uriFile);
        String ext = FileUtils.getExtension(uriFilePath);
        final String mext = MimeTypeMap.getSingleton().getExtensionFromMimeType("application/gpx+xml");
        if (DEBUG) {
            Log.d(TAG, "GPX file URI: " + uriFile);
            Log.d(TAG, "GPX file name: " + uriFilePath);
            Log.d(TAG, "isExternalStorageDocument: " + FileUtils.isExternalStorageDocument(uriFile));
        }
        if (ext.startsWith(".gpx")) {
            gpxFileName = FileUtils.getFileName(this,uriFile);
            if (DEBUG) Log.d(TAG, "Load GPX file: " + uriFile);
            XmlFileLoader _xmlFileLoader = new XmlFileLoader(super.app);

            try {
                InputStream _xmlStream = null;
                try {
                    _xmlStream = this.getContentResolver().openInputStream(uriFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (DEBUG) Log.d(TAG, "Open GPX stream");
                _xmlFileLoader.openStream(_xmlStream);
                if (DEBUG) Log.d(TAG, "GPX file loaded?");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            control.showErrorMessage(getString(R.string.error_invalid_gpx_file));
    }

   /*
     *  Save GPX file
     */
    void SaveFileGPX(final Intent data) {
        try {
            if (data != null)
            {
                Uri uri = data.getData(); //The uri with the location of the file
                ContentResolver cr = this.getContentResolver();
                OutputStream _xmlStream = cr.openOutputStream(uri, "w");
                OutputStreamWriter writer = new OutputStreamWriter(_xmlStream, StandardCharsets.UTF_8);
                GpxExporter.downloadData(writer);
            }

        } catch (FileNotFoundException e) {
            if (DEBUG) {
                Log.d(TAG, "Download failed");
            }
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (DEBUG) Log.d(TAG, "Download was successfully");
    }

    /*
     *  Save HTML file
     */
    void SaveFileHTML(final Intent data) {
        Uri uriFile = data.getData(); //The uri with the location of the file
        try {
            htmlFile.SaveFileHTML(this.getContentResolver().openOutputStream(uriFile, "w"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Pause Tracking
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void pauseTracking(View view) {
        control.setTrackingStatus(false);
    }

    /**
     * Continue Tracking
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void continueTracking(View view) {
        control.setTrackingStatus(true);
    }

    /**
     * Show route details
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void expand_less(View view) {
        control.setExpandViewStatus(false);
    }

    /**
     * Show etended tour description
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void expand_more(View view) {
        control.setExpandViewStatus(true);
    }

    /**
     * Hide the altitude profile
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void hide_profile(View view) {
        control.activateProfile(View.INVISIBLE);
    }

    /**
     * Show the altitude profile
     * @implNote call back from XML
     * @param view View provided from XML
     */
    public void show_profile(View view) {
        control.activateProfile(View.VISIBLE);
    }


    /**
     * Download GPX file
     * @see <a href="https://developer.android.com/reference/androidx/core/content/FileProvider">FileProvider</a>
     */
    public boolean downloadFileGPX3() {
        android.content.Context context = getApplicationContext();
        String fileContents = "Demo ...";
        try {
            // Create a new file object
//            File file = FileUtils.createTempImageFile(context,"demo.gpx");
            // Create an image file name
            File storageDir = new File(context.getCacheDir(), DOCUMENTS_DIR);
            File file =  File.createTempFile("demo.gpx", ".gpx", storageDir);

            FileOutputStream fos = new FileOutputStream(file);
            // Open the file for writing in internal storage
//            FileOutputStream fos = context.openFileOutput("demo.gpx", Context.MODE_PRIVATE);

            // Write data to the file
            fos.write(fileContents.getBytes());

            // Close the file
            fos.close();

//            Uri uri = FileProvider.getUriForFile(context, "de.biselliw.fileprovider", file);
            Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
            File dirDownload =  getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);


            if (DEBUG) Log.d(TAG, "Download was successfull");
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (DEBUG) Log.d(TAG, "Download failed");
        return false;
    }

    public boolean downloadFileGPX() {
        android.content.Context context = getApplicationContext();
        File dirDownload = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

 //       File dirDownload =  getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        try {
            // Create a new file in the shared download directory
        File file = new File(dirDownload, "TourNavigator.gpx");
//        Uri uri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", file);
//        Uri uri = FileProvider.getUriForFile(getApplicationContext(), getString(R.string.authorities), file);
        if (file.exists())
            file.delete();
/*
        grantUriPermission("*", uri,  Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setDataAndType(uri, context.getResources().getString(R.string.gpx_mime_type));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        ContentResolver cr = this.getContentResolver();
        OutputStream _xmlStream = cr.openOutputStream(uri, "w");
*/
        // Open a FileOutputStream to write to the file
        FileOutputStream _xmlStream = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(_xmlStream, StandardCharsets.UTF_8);
        GpxExporter.downloadData(writer);

        if (DEBUG) Log.d(TAG, "Download was successfull");
        return true;
    } catch (IOException e) {
        e.printStackTrace();
        }
        if (DEBUG) Log.d(TAG, "Download failed");
        return false;
        }

    /**
     * Download HTML file
     * @see <a href="https://developer.android.com/reference/androidx/core/content/FileProvider">FileProvider</a>
     */
    public boolean downloadFileHTML() {
        String AUTHORITY =  "androidx.core.content.FileProvider";
        File dirDownload =  // getApplicationContext().getExternalFilesDir(DIRECTORY_DOWNLOADS);
                        getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS);
        try {
            requestPermissions(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

            File file = new File(dirDownload, "TourNavigator.html");
            Uri uri = FileProvider.getUriForFile(getApplicationContext(), AUTHORITY, file);
            if (file.exists())
                file.delete();

            grantUriPermission("*", uri,  Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setDataAndType(uri, "text/html");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            ContentResolver cr = this.getContentResolver();
            OutputStream _xmlStream = cr.openOutputStream(uri, "w");
            htmlFile.SaveFileHTML(_xmlStream);

            if (DEBUG) Log.d(TAG, "Download was successfull");
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (DEBUG) Log.d(TAG, "Download failed");
        return false;
    }


}