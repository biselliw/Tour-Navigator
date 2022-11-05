/*
 * Copyright (C) 2022 Walter Biselli
 *
 * Hiking Navigator App (the "Software") is free software:
 *
 * Licensed under the GNU General Public License along with this (the "License")
 * either version 3 of the License, or any later version.
 * GNU General Public License is published by the Free Software Foundation,
 *
 * The Software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * You can redistribute the Software and/or modify it under the terms of the License
 *
 * See the GNU General Public License for more details.
 * You should have received a copy of the License along with the Software.
 * If not, you may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.biselliw.tour_navigator.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.navigation.NavigationView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.GravityCompat;
import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.dialogs.PauseTimeDialog;
import de.biselliw.tour_navigator.dialogs.StartTimeDialog;
import de.biselliw.tour_navigator.files.FileUtils;
import de.biselliw.tour_navigator.files.GpxExporter;
import de.biselliw.tour_navigator.files.HTML_File;
import de.biselliw.tour_navigator.helpers.ProfileAdapter;
import tim.prune.data.DataPoint;
import tim.prune.load.xml.XmlFileLoader;

public class MainActivity extends LocationActivity  implements
        NavigationView.OnNavigationItemSelectedListener {

    private static HTML_File htmlFile;

    /**
     * TAG for log messages.
     */
    static final String TAG = "MainActivity";
    private static final boolean DEBUG = false; // Set to true to enable logging

    int REQUEST_OPEN_GPX = 222;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.app = new App(this);
        super.pa = new ProfileAdapter(this, super.app);

        // Load preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SettingsActivity.getPreferences(sharedPref, super.app);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.main = this;
        super.onPostCreate(savedInstanceState);
        mNavigationView.setNavigationItemSelectedListener(this);
        selectNavigationItem(R.id.nav_main);

        htmlFile = new HTML_File(this, recordAdapter);

        // Handle a received intent from another app
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals("android.intent.action.VIEW")) {
            onActivityResult(REQUEST_OPEN_GPX, RESULT_OK, intent);
        }
    }

    @Override
    public void onResume() {
        recordAdapter.notifyDataSetChanged();
        super.onResume();
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


    public boolean	onMenuItemClick(MenuItem item)
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
        else if (id == R.id.itm_delete_waypoint)
            /* Delete the current waypoint */
            recordAdapter.remove();
        else if (id == R.id.itm_copy_timetable)
            /* Copy the timetable to the clipboard */
            copyTimetableToClipboard();
        else if (id == R.id.itm_nav_waypoint)
            /* Navigate to the waypoint */
            navigateToWaypoint();
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

    /*
     *  Register an activity to load the GPX file
     *  @see https://developer.android.com/training/basics/intents/result#java
     */
    public void registerActivityResultLauncherOpenDocument() {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/*");
        // @link https://www.b4x.com/android/forum/threads/list-of-known-android-intents-to-do-stuff.9823/
        // @link https://stackoverflow.com/questions/13065838/what-are-the-possible-intent-types-for-intent-settypetype
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

    /*
     *  Register an activity to store the GPX file
     *  @see https://developer.android.com/training/basics/intents/result#java
     */
    public void registerActivityResultLauncherCreateDocumentGPX() {
        // Intent creation
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
        // will trigger exception if no  appropriate category passed
        intent.addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/gpx")
// todo Field requires API level 26 (current min is 21): `android.provider.DocumentsContract#EXTRA_INITIAL_URI`
//                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS)
                .putExtra(Intent.EXTRA_TITLE, "Tour.gpx");
        CreateDocumentGPXActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> CreateDocumentGPXActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    SaveFileGPX(result.getData());
                }
            });

    /*
     *  Register an activity to store the HTML file
     *  @see https://developer.android.com/training/basics/intents/result#java
     */
    public void registerActivityResultLauncherCreateDocumentHTML() {
        // Intent creation
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        // will trigger exception if no  appropriate category passed
        intent.addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/html")
// todo Field requires API level 26 (current min is 21): `android.provider.DocumentsContract#EXTRA_INITIAL_URI`
//                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS)
                .putExtra(Intent.EXTRA_TITLE, "Tour.html");
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

    /*
     *  Change the start time of the tour
     */
    void changeStartTime() {
        if (recordAdapter.getCount() > 0) {
            StartTimeDialog startTimeDialog = new StartTimeDialog(MainActivity.this, recordAdapter);
            startTimeDialog.show();
        }
    }

    /*
     * Change the pause time of the current waypoint
     */
    void changePauseTime() {
        if (recordAdapter.getPlace() > 0) {
            PauseTimeDialog pauseTimeDialog = new PauseTimeDialog(MainActivity.this, recordAdapter, super.app);
            pauseTimeDialog.show();
        }
    }

    /*
     * Copy the timetable to the clipboard
     */
    void copyTimetableToClipboard() {
        StringBuffer html = htmlFile.getTimetableAsHTML();
        ClipData myClipData = ClipData.newHtmlText("text", html.toString(), html.toString());
        ClipboardManager myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        assert myClipboard != null;
        myClipboard.setPrimaryClip(myClipData);
    }

    /*
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

    /*
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

    /*
     *  Navigate to the waypoint with another app
     */
    void navigateToWaypoint() {
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
     * Navigate with Google Maps
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

    // set active navigation item
    private void selectNavigationItem(int itemId) {
        for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
            boolean b;
            b = (itemId == mNavigationView.getMenu().getItem(i).getItemId());
            mNavigationView.getMenu().getItem(i).setChecked(b);
        }
    }

    /*
     * Manager for main navigation
     * @id identifier for the menu item.
     */
    private void goToNavigationItem(int id) throws IOException {
        Intent intent;

        if (id == R.id.nav_tutorial) {
            intent = new Intent(this, TutorialActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
        // -------------------------------------------------
        else if (id == R.id.nav_open_gpx)
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
        }
    }

    /*
     *  Open GPX file
     */
    public void OpenFileGPX(View view) {
        OpenFileGPX();
    }

    /*
     *  Open GPX file
     */
    public void OpenFileGPX() {
        registerActivityResultLauncherOpenDocument();
    }

    /*
     * Open GPX file
     * @param data Intent data
     * @link https://stackoverflow.com/questions/43199564/uri-with-com-android-externalstorage-documents-located-on-non-primary-volume
     */
    void OpenFileGPX(final Intent data) {
        Uri uriFile = data.getData(); //The uri with the location of the file
        // Get the File path from the Uri
        String uriFilePath = FileUtils.getPath(this, uriFile);
        String ext = FileUtils.getExtension(uriFilePath);
        if (DEBUG) {
            Log.d(TAG, "GPX file URI: " + uriFile);
            Log.d(TAG, "GPX file name: " + uriFilePath);
            Log.d(TAG, "isExternalStorageDocument: " + FileUtils.isExternalStorageDocument(uriFile));
        }
        if (ext.equals(".gpx")) {
            Log.d(TAG, "Load GPX file: " + uriFile);
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
    }

    /*
     *  Save GPX file
     */
    void SaveFileGPX(final Intent data) {
        try {
            if (data != null)
            {
                Uri uriFile = data.getData(); //The uri with the location of the file
                OutputStream _xmlStream = this.getContentResolver().openOutputStream(uriFile, "w");
                GpxExporter.downloadData(_xmlStream);
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


    public void pauseTracking(View view) {
        control.setTrackingStatus(false);
    }

    public void continueTracking(View view) {
        control.setTrackingStatus(true);
    }

    public void expand_less(View view) {
        control.setExpandViewStatus(false);
    }

    public void expand_more(View view) {
        control.setExpandViewStatus(true);
    }

    public void hide_profile(View view) {
        control.activateProfile(false);
    }

    public void show_profile(View view) {
        control.activateProfile(true);
    }

}
