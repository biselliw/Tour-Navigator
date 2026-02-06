package de.biselliw.tour_navigator.ui;

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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.LocationActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.data.AppState;
import de.biselliw.tour_navigator.data.EstimateParams;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.files.HTML_File;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.helpers.ProfileAdapter;

import static android.view.View.VISIBLE;
import static de.biselliw.tour_navigator.activities.SettingsActivity.getConsentGoogleMaps;
import static de.biselliw.tour_navigator.activities.SettingsActivity.getConsentInternet;
import static de.biselliw.tour_navigator.activities.SettingsActivity.getProfileViewVisibility;
import static de.biselliw.tour_navigator.activities.SettingsActivity.setProfileViewVisibility;

public class ControlElements extends BaseActivity {

    protected App app;

    /**
     * TAG for log messages.
     */
    static final String TAG = "ControlElements";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    protected SharedPreferences sharedPref = null;

    // todo Do not place Android context classes in static fields (static reference to ControlElements which has field webView pointing to WebView); this is a memory lea

    protected WebView webView = null;

    public ProfileAdapter profileAdapter = null;

    protected TourDetails tourDetails = null;

    protected RecordAdapter recordAdapter = null;

    public TextToSpeech tts;

    boolean _initUserInterface = false;
    boolean _setupUserInterface = false;
    static boolean _updateTrackingStatus = false;
    static boolean _updateAdditionalInfo = false;
    protected static boolean raiseAlarm = true;

    boolean _updateGpsStatus = false;
    boolean _updateExpandViewStatus = false;
    boolean _updateSpeakStatus = false;
    static boolean _updateErrorMessage = false;
    boolean _updateExpandView = false;
    static boolean _updateFileInfo = false;
    static boolean _updateWaypointType = false;
    static boolean _initProfile = false;
    boolean _updateProfile = false;
    boolean _rescalePlaceView = true;

    private boolean _isViewExpanded = false;
    private static boolean _tourInfoAvailable = false;

    private static final int COLOR_MESSAGE = 0xFFFFFFFF;
    private static final int COLOR_RED = 0xAAB71C1C;

    /**
     * if true: the GPS provider controls the tracking
     */
    private static boolean _isTracking;

    protected boolean firstStart = false;
    Handler timerHandler = new Handler();

    LocationActivity.gpsStatus _gpsStatus;
    private int _place = -1;
    int _expandViewVisibility = View.GONE;
    boolean _speakEnabled = false;
    private TourDetails.AdditionalInfo _additionalInfo = null;
    static String errorMessage = "";
    private String _distanceToPlace = "";
    int _profileViewVisibility = View.GONE;

    public static boolean updateGpxFile = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Initialize the user interface before loading a new GPX track
        initUserInterface();

        /* Install a timer to update control elements */
        //runs without a timer by reposting this handler at the end of the runnable
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (_initUserInterface) {
                    // Disable a group of navigation items
                    onPrepareNavigationMenu(mNavigationView.getMenu());
                    _initUserInterface = false;
                }
                else // todo fix onPrepareNavigationMenu(
                    onPrepareNavigationMenu(mNavigationView.getMenu());

                if (_setupUserInterface) {
                    // Enable a group of navigation items
                    onPrepareNavigationMenu(mNavigationView.getMenu());
                    onSetupUserInterface();
                }
                if (_updateFileInfo)
                    showFileInfo();
                if (_updateTrackingStatus)
                    onShowTrackingStatus();
                if (_updateGpsStatus)
                    onShowGpsStatus();
                if (_updateExpandViewStatus)
                    onShowExpandViewStatus();
                if (_updateSpeakStatus)
                    onShowSpeakStatus();
                if (_updateErrorMessage)
                    onShowErrorMessage();
                if (_updateAdditionalInfo)
                    showAdditionalInfo();
                if (_updateExpandView)
                    onShowAdditionalInfo(_additionalInfo, _isViewExpanded);
                if (_updateWaypointType)
                    onShowWaypointType(_distanceToPlace, _additionalInfo);
                if (_initProfile) {
                    profileAdapter.initPlot();
                    _initProfile = false;
                }
                if (_updateProfile)
                    onShowProfile();
                if (_rescalePlaceView)
                    onRescalePlaceView();

                timerHandler.postDelayed(this, 100);
            }
        };
        timerHandler.postDelayed(timerRunnable, 100);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppState.destroyed = true;

        timerHandler.removeCallbacksAndMessages(null);
    }


    /**
     *
     * @see #onOptionsItemSelected in #MainActivity
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            if ((id == R.id.itm_break_time) ||
                (id == R.id.itm_comment_waypoint) ||
                (id == R.id.itm_delete_routepoint) ||
                (id == R.id.itm_delete_trackpoints) ||
                (id == R.id.itm_set_new_start))
                        item.setEnabled(!_isViewExpanded && recordAdapter != null && (_place > 0) && (_place < recordAdapter.getCount()-1));
            else if (id == R.id.itm_nav_waypoint)
                        item.setEnabled(!_isViewExpanded && (_place >= 0));
            else if (id == R.id.itm_nav_google) {
                        item.setEnabled(!_isViewExpanded && (_place >= 0));
                        item.setVisible(getConsentGoogleMaps());
            } else if ((id == R.id.itm_find_nearby_wikipedia) ||
                    (id == R.id.itm_find_nearby_osm)) {
                        item.setEnabled(!_isViewExpanded && (_place >= 0));
                        item.setVisible(getConsentInternet());
            } else if (id == R.id.itm_separator) {
                        item.setEnabled(true);
                        item.setVisible(true);
            } else
                        item.setVisible(false);
        }
        return true;
    }

    public void onPrepareNavigationMenu(Menu menu) {
        boolean gpxFileValid = app.getTrack().isValid();

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            if (id == R.id.nav_file_info)
                item.setEnabled(Log.isLoggedDebugHTML() ||
                    (!_initUserInterface && _tourInfoAvailable) ||
                    app.getTrack().isValidRecordedTrackFile()
                );
            else if (
                    (id == R.id.nav_reverse_route) ||
                    (id == R.id.nav_start_time) ||
                    (id == R.id.nav_download_html)
                    )
                item.setEnabled(!_initUserInterface && gpxFileValid);
            else if (id == R.id.nav_timetable)
                item.setEnabled(!_initUserInterface && app.getTrack().hasAltitudes());
            else if (id == R.id.nav_download_gpx)
                item.setEnabled(!_initUserInterface && gpxFileValid || app.getTrack().isValidRecordedTrackFile());
            else if (id == R.id.nav_add_waypoints) {
                if (item.hasSubMenu()) {
                    SubMenu subMenu = item.getSubMenu();
                    if (subMenu != null) {
                        MenuItem mitem = subMenu.findItem(R.id.nav_remote_waypoints);
                        if (mitem != null)
                            mitem.setVisible(App.getTrack().hasWayPointsOutOfTrack());
                        mitem = subMenu.findItem(R.id.nav_osm_guideposts);
                        if (mitem != null)
                            mitem.setVisible(!_initUserInterface && app.getTrack().hasAltitudes() && getConsentInternet());
                    }
                }
            }
            else if (id == R.id.nav_osm_guideposts)
                item.setVisible(getConsentInternet());
            else
                item.setVisible(true);
        }
    }

    /**
     * Set the optimum width to show the place
     */
    private void onRescalePlaceView() {
        TextView placeView = findViewById(R.id.track_place);
        TableLayout table_layout1 = findViewById(R.id.table_head);
        int layoutWidth = table_layout1.getWidth();

        int h_track_arrive = getViewWidth(R.id.h_track_arrive);
        int h_track_distance = getViewWidth(R.id.h_track_distance);
        int h_track_dist_to_dest = getViewWidth(R.id.h_track_dist_to_dest);
        int sum = h_track_arrive + h_track_distance + h_track_dist_to_dest;

        int colWidth = layoutWidth - sum;
        if (colWidth > 0) {
            placeView.setWidth(colWidth);
            _rescalePlaceView = false;
        }
    }

    private void onSetupUserInterface() {
 //       TableLayout tableLayoutHead = findViewById(R.id.table_head);
        TableLayout tableLayoutRecords = findViewById(R.id.table_records);
        ListView recordsView = findViewById(R.id.records_view);
        TextView commentView = findViewById(R.id.comment_view);
        LinearLayout location_content = findViewById(R.id.location_content);
        LinearLayout description_content = findViewById(R.id.description_content);
        LinearLayout web_content = findViewById(R.id.web_content);
        if (App.getTrack().isValidRecordedTrackFile()) {
            setViewVisibility(R.id.table_head,View.GONE);
            tableLayoutRecords.setVisibility(View.GONE);
            recordsView.setVisibility(View.GONE);
            commentView.setVisibility(View.GONE);
            location_content.setVisibility(View.GONE);
            description_content.setVisibility(View.GONE);
            web_content.setVisibility(View.VISIBLE);
        }
        else {
            setViewVisibility(R.id.table_head,VISIBLE);
            tableLayoutRecords.setVisibility(VISIBLE);
            recordsView.setVisibility(VISIBLE);
            commentView.setVisibility(VISIBLE);
            web_content.setVisibility(View.GONE);
        }
        setTrackingStatus(false);
        _setupUserInterface = false;
    }


    /**
     * Show the tracking status
     */
    private void onShowTrackingStatus() {
        _updateTrackingStatus = false;

        if (((LocationActivity)this).isTrackingEnabled() && App.getTrack().isValid())
        {
            if (_isTracking) {
                setViewVisibility(R.id.image_tracking_pause,VISIBLE);
                setViewVisibility(R.id.image_tracking,View.GONE);
            } else {
                setViewVisibility(R.id.image_tracking_pause,View.GONE);
                setViewVisibility(R.id.image_tracking,VISIBLE);
            }
        } else {
            setViewVisibility(R.id.image_tracking_pause,View.GONE);
            setViewVisibility(R.id.image_tracking,View.INVISIBLE);
        }
    }

    /**
     * Show one of the images expand more/less depending on the state of expandView
     */
    private void onShowExpandViewStatus() {
        _updateExpandViewStatus = false;
        switch (_expandViewVisibility) {
            case VISIBLE:
                setViewVisibility(R.id.image_expand_more,View.GONE);
                setViewVisibility(R.id.image_expand_less,VISIBLE);
                break;
            case View.INVISIBLE:
                setViewVisibility(R.id.image_expand_more,VISIBLE);
                setViewVisibility(R.id.image_expand_less,View.GONE);
                break;
            default:
                setViewVisibility(R.id.image_expand_more,View.INVISIBLE);
                setViewVisibility(R.id.image_expand_less,View.GONE);
        }
    }

    /**
     * Show one of the images expand more/less depending on the state of expandView
     */
    private void onShowSpeakStatus() {
        ImageView image_text_to_speech = findViewById(R.id.image_text_to_speech);
        ImageView image_voice_selection_off = findViewById(R.id.image_voice_selection_off);
        _updateSpeakStatus = false;

        if (_speakEnabled && (_expandViewVisibility == VISIBLE) ) {
            if (tts.isSpeaking()){
                _updateSpeakStatus = true;
                image_text_to_speech.setVisibility(View.INVISIBLE);
                image_voice_selection_off.setVisibility(VISIBLE);
            }
            else
            {
                image_text_to_speech.setVisibility(VISIBLE);
                image_voice_selection_off.setVisibility(View.INVISIBLE);
            }
        }

        if (!_speakEnabled) {
            if (tts.isSpeaking())
                stopSpeaking();

            image_text_to_speech.setVisibility(View.GONE);
            image_voice_selection_off.setVisibility(View.GONE);
        }
    }

    /**
     * Show/hide the profile
     */
    private void onShowProfile() {
        _updateProfile = false;
        LinearLayout plot = findViewById(R.id.profile_plot);
        plot.setVisibility(_profileViewVisibility == VISIBLE ? VISIBLE : View.GONE);
        switch (_profileViewVisibility) {
            case View.INVISIBLE:
                setViewVisibility(R.id.image_show_profile, VISIBLE);
                setViewVisibility(R.id.image_hide_profile, View.GONE);
                break;
            case VISIBLE:
                setViewVisibility(R.id.image_show_profile, View.GONE);
                setViewVisibility(R.id.image_hide_profile, VISIBLE);
                break;
            default:
                setViewVisibility(R.id.image_show_profile, View.INVISIBLE);
                setViewVisibility(R.id.image_hide_profile, View.GONE);
        }

        if (_profileViewVisibility == VISIBLE) {
            ((LocationActivity)this).requestStatusUpdate();
            profileAdapter.setCursor(0);
        }
    }

    public static void setAlarmPreference(boolean inRaiseAlarm) {
        raiseAlarm = inRaiseAlarm;
    }
    /**
     * Show alarm setting (On/Off)
     */
    public void showAlarmPreference()
    {
        if (App.getTrack().isValidRecordedTrackFile()) {
            setViewVisibility(R.id.image_alarm_off,View.GONE);
            setViewVisibility(R.id.image_alarm_on,View.GONE);
        }
        else if (raiseAlarm)
        {
            setViewVisibility(R.id.image_alarm_off,View.GONE);
            setViewVisibility(R.id.image_alarm_on,View.VISIBLE);
        }
        else
        {
            setViewVisibility(R.id.image_alarm_off,View.VISIBLE);
            setViewVisibility(R.id.image_alarm_on,View.GONE);
        }
    }

    /**
     * Show the GPS status
     */
    private void onShowGpsStatus() {
        ImageView image_location_disabled = findViewById(R.id.image_location_disabled);
        ImageView image_location_home = findViewById(R.id.image_location_home);
        ImageView image_location_off = findViewById(R.id.image_location_off);
        ImageView image_location_on = findViewById(R.id.image_location_on);
        ImageView image_location_wait = findViewById(R.id.image_location_wait);
        _updateGpsStatus = false;

        image_location_disabled.setVisibility(View.GONE);
        image_location_home.setVisibility(View.GONE);
        image_location_off.setVisibility(View.GONE);
        image_location_on.setVisibility(View.GONE);
        image_location_wait.setVisibility(View.GONE);

        switch (_gpsStatus) {
            case PERMISSION_DENIED:
                image_location_disabled.setVisibility(VISIBLE);
                break;
            case PERMISSION_GRANTED:
            case PROVIDER_ENABLED:
                break;
            case PROVIDER_DISABLED:
                image_location_home.setVisibility(VISIBLE);
                break;
            case WAIT_FOR_GPS_FIX:
                image_location_wait.setVisibility(VISIBLE);
                break;
            case GPS_FIX:
                image_location_on.setVisibility(VISIBLE);
                break;
            case GPS_TIMEOUT:
                image_location_off.setVisibility(VISIBLE);
                break;
            default:
        }
    }

    /**
     * Show Error message
     */
    private void onShowErrorMessage() {
        _updateExpandView = false;

        setTitleText(errorMessage,COLOR_RED);
        Log.e("Error",errorMessage);
        _updateErrorMessage = false;
    }

    /**
     * Set the title text
     * @param inTitle title
     * @param inBackgroundColor background color
     */
    public void setTitleText (String inTitle, int inBackgroundColor) {
        TextView main_text_title = findViewById(R.id.main_text_title);
        main_text_title.setBackgroundColor(inBackgroundColor);
        main_text_title.setText(inTitle);
    }

    /**
     * Show additional info
     * @param inAdditionalInfo additional information to be shown in the view
     * @param inViewExpanded view for additional infos is expanded
     */
    private void onShowAdditionalInfo(TourDetails.AdditionalInfo inAdditionalInfo, boolean inViewExpanded) {
        _updateExpandView = false;

        if ((inAdditionalInfo != null)
                // && (!Log.isWritingEnabled() || !isErrorMessage())
         ){
            if (!isErrorMessage())
                setTitleText(inAdditionalInfo.title,COLOR_MESSAGE);

            TextView commentView = findViewById(R.id.comment_view);
            commentView.setText(inAdditionalInfo.comment);

            if (inViewExpanded) {
                TextView descriptionView = findViewById(R.id.description_view);
                String html = inAdditionalInfo.description;
                Spanned fromHTML;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                    fromHTML = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
                else
                    fromHTML = Html.fromHtml(html);
                descriptionView.setText(fromHTML);
                descriptionView.scrollTo(0, 0);

                TextView linkView = findViewById(R.id.link_view);
                String link = inAdditionalInfo.link;
                String swvLink = HTML_File.getSwvLink(link);
                if (!swvLink.isEmpty())
                    link = link + "\n" + swvLink;
                linkView.setText(link);
            }
        }

        LinearLayout location_content = findViewById(R.id.location_content);
        LinearLayout description_content = findViewById(R.id.description_content);

        location_content.setVisibility(inViewExpanded ? View.GONE : VISIBLE);
        description_content.setVisibility(inViewExpanded ? VISIBLE : View.GONE);

        if (!inViewExpanded)
            ((LocationActivity)this).requestStatusUpdate();
    }

    /**
     * Show the distance to the next point and its waypoint type
     * @param inDistanceToPlace formatted distance
     * @param inAdditionalInfo additional information containing the waypoint type
     */
    private void onShowWaypointType(String inDistanceToPlace, TourDetails.AdditionalInfo inAdditionalInfo) {
        _updateWaypointType = false;
        String comment = inDistanceToPlace;
        if (inAdditionalInfo != null)
            comment = comment + inAdditionalInfo.comment;

        TextView commentView = findViewById(R.id.comment_view);
        commentView.setText(comment);
    }

    private int getViewWidth(int id) {
        TextView view = findViewById(id);
        return view.getWidth();
    }

    private void setViewVisibility(int id, int visibility) {
        View view = findViewById(id);
        if (view != null)
            view.setVisibility(visibility);
    }

    /**
     * Initialize the user interface before loading a new GPX track
     */
    private void initUserInterface() {
        _tourInfoAvailable = false;

        setTrackingStatus(false);

        activateProfile(View.GONE);
        setViewVisibility(R.id.image_expand_more, View.GONE);
        showAlarmPreference();
        _initUserInterface = true;
    }

    /**
     * Setup the user interface after loading a new GPX track
     */
    public void setupUserInterface() {
        _tourInfoAvailable =
                !tourDetails.getFileInfo().description.isEmpty() ||
                !tourDetails.getFileInfo().link.isEmpty() ||
                !tourDetails.getFileInfo().sourceLink.isEmpty();
        showAdditionalInfo(-1);

        // use stored state of profile view visibility
        activateProfile(getProfileViewVisibility(sharedPref));
        _setupUserInterface = true;
    }

    /**
     * Set the tracking status
     *
     * @param inTracking true if the GPS provider shall controls the tracking
     */
    public void setTrackingStatus(boolean inTracking) {
        ((LocationActivity)this).startForegroundLocationService(inTracking);

        _isTracking = inTracking;
        setExpandViewStatus(false);
        updateTrackingStatus();
    }


    /**
     * Show the GPS location provider status
     * @param inGpsStatus status of the GPS location provider
     */
    public void showGpsStatus(LocationActivity.gpsStatus inGpsStatus) {
        _gpsStatus = inGpsStatus;
        _updateGpsStatus = true;
        updateTrackingStatus();
    }

    /**
     * Show one of the images expand more/less depending on the state of expandView
     *
     * @param inPlace row index of the table or -1
     * @param inExpand info window is expanded
     * @return true if view will be expanded
     */
    public boolean showExpandViewStatus(int inPlace, boolean inExpand) {
        boolean isExpandableView = false;;
        _speakEnabled = false;
        if (App.getTrack().isValidRecordedTrackFile()) {
            // view shall be not available
            _expandViewVisibility = View.GONE;
        }
        else if (tourDetails != null) {
            _place = inPlace;
            if (inPlace < 0) {
                isExpandableView = _tourInfoAvailable || Log.isLoggedDebugHTML();
                _speakEnabled = _tourInfoAvailable && inExpand;
            } else {
                TourDetails.AdditionalInfo info = tourDetails.getAdditionalInfo(Log.isLoggedDebugHTML(), inPlace);
                _speakEnabled = !info.description.isEmpty();
                isExpandableView = _speakEnabled ||
                        ((info.link != null) && !info.link.isEmpty());
                _speakEnabled &= inExpand;
            }
            // is the info window expandable to present text?
            if (isExpandableView)
                // shall it be expanded?
                if (inExpand)
                    // view shall be visible
                    _expandViewVisibility = VISIBLE;
                else {
                    // view shall be visible
                    _expandViewVisibility = View.INVISIBLE;
                    isExpandableView = false;
                }
            else
                // view shall be not available
                _expandViewVisibility = View.GONE;
        }
        _updateExpandViewStatus = true;
        _updateSpeakStatus = true;

        return isExpandableView;
    }

    /**
     * Show extended description view and hide the list view
     *
     * @param inExpand true if description view shall be expanded
     */
    public void setExpandViewStatus(boolean inExpand) {
        int place = recordAdapter.getPlace();
        // update the expansion mode
        _isViewExpanded = showExpandViewStatus(place, inExpand);
        showAdditionalInfo(place);
    }

    /**
     * @return true if extended description view is active
     */
    public boolean getExpandViewStatus() {
        return _isViewExpanded;
    }

    /**
     * Show the file info or HTML Log if available
     */
    public void showFileInfo() {
        if (App.getTrack().isValidRecordedTrackFile()) {
            // todo übersetzen: "Informationen zum aufgezeichneten Track"
            setTitleText("Informationen zum aufgezeichneten Track", COLOR_MESSAGE);
            webView.loadData(EstimateParams.getRecordedTrackFileInfo(),
                               "text/html","utf-8");
        } else {
            // update the expansion mode
            _isViewExpanded = showExpandViewStatus(-1, true);
            showAdditionalInfo(-1);

        }
        _updateFileInfo = false;
    }


    /**
     * Show additional info for the selected place
     */
    public void showAdditionalInfo() {
        showAdditionalInfo(recordAdapter.getPlace());
    }

    /**
     * Show additional info if available
     * @param inPlace place
     */
    public void showAdditionalInfo(int inPlace) {
        if (tourDetails != null) {
            _additionalInfo = tourDetails.getAdditionalInfo(Log.isLoggedDebugHTML(), inPlace);
            _updateExpandView = !App.getTrack().isValidRecordedTrackFile();
        } else
            _additionalInfo = null;
        _updateAdditionalInfo = false;
    }

    /**
     * Speak additional info if HTML description is available. All links will be removed
     */
    public void speakAddInfo() {
        String urlRegex =
                "(?i)\\b(" +
                        "(?:https?|ftp)://\\S+|" +      // http://, https://, ftp://
                        "//\\S+|" +                    // protocol-relative URLs
                        "www\\.\\S+|" +                // www.*
                        "[a-z0-9.-]+\\.[a-z]{2,}\\S*|" + // any-domain.anyTLD(/path)
                        "\\d{1,3}(?:\\.\\d{1,3}){3}\\S*" + // IPv4 URL-like
                        ")";
        String html = _additionalInfo.description.replaceAll("<[^>]*>", " ");

        // Remove <a>...</a> tags but keep their text
        html = html.replaceAll("<a[^>]*>(.*?)</a>", "$1");

        html = html.replaceAll(urlRegex, "");

        // Remove all attributes from all tags
        String text = html.replaceAll("<(\\w+)[^>]*>", "<$1>");

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speakAddInfo");
        _updateSpeakStatus = true;
    }

    public void stopSpeaking() {
       if (tts.isSpeaking()) {tts.stop(); }
    }

    /**
     * Set the distance to the next place
     * @param inDistanceToPlace distance in km
     */
    public void setDistanceToPlace(double inDistanceToPlace) {
        if (inDistanceToPlace >= 1.0)
            _distanceToPlace = new DecimalFormat("in #0.0 km: ").format(inDistanceToPlace);
        else
            _distanceToPlace = new DecimalFormat("in #0 m: ").format((int)(inDistanceToPlace*100.0)*10);
        _updateWaypointType = true;
    }


    /**
     * Show/hide the profile
     *
     * @param state view state
     */
    public void activateProfile(int state) {
        if (App.getTrack() != null) {
            if (!App.getTrack().hasAltitudes())
                state = View.GONE;
            _profileViewVisibility = state;
            if (state != View.GONE)
                setProfileViewVisibility(sharedPref, state);

            _updateProfile = true;
        }
    }

    /**
     * Show an error message until user confirms
     * @param message error message
     */
    public void showErrorMessage(String message)
    {
        errorMessage = message;
        _updateErrorMessage = true;
    }

    public boolean isErrorMessage() {
        return !errorMessage.isEmpty();
    }

    public  void clearErrorMessage()
    {
        errorMessage = "";
        _updateErrorMessage = false;
    }

    /**
     * Update the controls showing the tracking status
     */
    public void updateTrackingStatus() {
        _updateTrackingStatus = true;
    }

    public static void updateFileInfo() {
        _updateFileInfo = true;
    }

    /**
     * Get the tracking status
     *
     * @return true if the GPS provider controls the tracking
     */
    public static boolean isTracking() {
        return _isTracking;
    }

    public static void initProfile() {
        _initProfile = true;
    }

    public void notifyDataSetChanged(List<RecordAdapter.Record> records) {
        _updateWaypointType = false;
        if (DEBUG) Log.d(TAG,"notifyDataSetChanged(records)");
        recordAdapter.notifyDataSetChanged(records);
    }

    public void notifyDataSetChanged() {
        if (DEBUG) Log.d(TAG,"notifyDataSetChanged()");
        recordAdapter.notifyDataSetChanged();
    }
    public boolean isViewExpanded() { return _isViewExpanded; }

    public RecordAdapter getRecordAdapter () { return recordAdapter; }

    public static void updateAdditionalInfo() { _updateAdditionalInfo = true; }
}