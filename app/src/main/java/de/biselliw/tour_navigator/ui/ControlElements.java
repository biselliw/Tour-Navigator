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
    along with FairEmail. If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2024 Walter Biselli (BiselliW)
*/

import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.LocationActivity;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.helpers.ProfileAdapter;

import static de.biselliw.tour_navigator.activities.SettingsActivity.getExpandView;
import static de.biselliw.tour_navigator.activities.SettingsActivity.getProfileViewVisibility;
import static de.biselliw.tour_navigator.activities.SettingsActivity.setExpandView;
import static de.biselliw.tour_navigator.activities.SettingsActivity.setProfileViewVisibility;

public class ControlElements extends BaseActivity {

    public static ControlElements control = null;
    public App app = null;
    public MainActivity main = null;
    public ProfileAdapter pa = null;

    private LocationActivity la = null;
    protected TourDetails details = null;

    public RecordAdapter recordAdapter = null;

    protected static boolean expandView = false;
    private static boolean fileInfoAvailable = false;

    private static final int COLOR_MESSAGE = 0xFFFFFFFF;
    private static final int COLOR_RED = 0xAAB71C1C;

    /**
     * if true: the GPS provider controls the tracking
     */
    private static boolean _isTracking;

    Handler timerHandler = new Handler();

    boolean _rescalePlaceView = true;
    boolean _initUserInterface = false;
    boolean _setupUserInterface = false;
    boolean _updateTrackingStatus = false;
    LocationActivity.gpsStatus _gpsStatus;
    boolean _updateGpsStatus = false;
    private int _place = -1;
    int _expandViewVisibility = View.GONE;
    boolean _updateExpandViewStatus = false;
    TourDetails.AdditionalInfo additionalInfo = null;
    String errorMessage = "";
    boolean _updateErrorMessage = false;
    boolean _updateExpandView = false;
    boolean _updateAdditionalInfo = false;
    String _comment = "";
    int _profileViewVisibility = View.GONE;
    boolean _updateProfile = false;

    public boolean updateGpxFile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        la = (LocationActivity) this;

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
                if (_setupUserInterface) {
                    // Enable a group of navigation items
                    onPrepareNavigationMenu(mNavigationView.getMenu());
                    onSetupUserInterface();
                }
                if (_updateTrackingStatus)
                    onShowTrackingStatus();
                if (_updateGpsStatus)
                    onShowGpsStatus();
                if (_updateExpandViewStatus)
                    onShowExpandViewStatus();
                if (_updateErrorMessage)
                    onShowErrorMessage();
                if (_updateAdditionalInfo)
                    onShowAdditionalInfo2();
                if (_updateExpandView)
                    onShowAdditionalInfo();
                if (_updateProfile)
                    onShowProfile();
                if (_rescalePlaceView)
                    onRescalePlaceView();
                //    enableNavigationItem(R.id.nav_file_info, fileInfoAvailable);

                timerHandler.postDelayed(this, 100);
            }

        };
        timerHandler.postDelayed(timerRunnable, 100);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        control = this;
        super.onPostCreate(savedInstanceState);
        // Initialize the user interface before loading a new GPX track
        initUserInterface();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            if (
                    (id == R.id.itm_pause_time) ||
                            (id == R.id.itm_comment_waypoint) ||
                            (id == R.id.itm_nav_waypoint) ||
                            (id == R.id.itm_nav_google) ||
                            (id == R.id.itm_split) ||
                            (id == R.id.itm_set_new_start) ||
                            (id == R.id.itm_delete_waypoint) ||
                            (id == R.id.itm_delete_trackpoints))
                item.setEnabled(!expandView && (_place >= 0));
            else
                item.setVisible(false);
        }

        return true;
    }

    /**
     * Enable/disable a group of navigation items
     *
     * @param menu NavigationView menu
     */

    void onPrepareMenu(@NonNull Menu menu) {

    }

    public boolean onPrepareNavigationMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            if (id == R.id.nav_file_info)
                item.setEnabled(!_initUserInterface && fileInfoAvailable);
            else if (
                    (id == R.id.nav_reverse_route) ||
                            (id == R.id.nav_start_time) ||
                            (id == R.id.nav_download_gpx) ||
                            (id == R.id.nav_download_html) ||
                            (id == R.id.nav_timetable))
                item.setEnabled(!_initUserInterface);
            else
                item.setVisible(true);
        }

        return true;
    }

    /**
     * Set the optimum width to show the place
     */
    private void onRescalePlaceView() {
        TextView placeView = findViewById(R.id.track_place);
        TableLayout table_layout1 = findViewById(R.id.table_layout1);
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
        setTrackingStatus(false);
        _setupUserInterface = false;
    }

    /**
     * Show the tracking status
     */
    private void onShowTrackingStatus() {
        _updateTrackingStatus = false;
        if (main != null) {
            ImageView image_tracking_pause = main.findViewById(R.id.image_tracking_pause);
            ImageView image_tracking = main.findViewById(R.id.image_tracking);

            if (la.isGPS_Fix()) {
                if (_isTracking) {
                    image_tracking_pause.setVisibility(View.VISIBLE);
                    image_tracking.setVisibility(View.GONE);
                } else {
                    image_tracking_pause.setVisibility(View.GONE);
                    image_tracking.setVisibility(View.VISIBLE);
                }
            } else {
                image_tracking_pause.setVisibility(View.GONE);
                image_tracking.setVisibility(View.INVISIBLE);
            }
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

        switch (_gpsStatus) {
            case NO_PERMISSION:
                image_location_disabled.setVisibility(View.VISIBLE);
                image_location_home.setVisibility(View.GONE);
                image_location_off.setVisibility(View.GONE);
                image_location_on.setVisibility(View.INVISIBLE);
                image_location_wait.setVisibility(View.INVISIBLE);
                break;
            case PROVIDER_ENABLED:
                image_location_disabled.setVisibility(View.GONE);
                image_location_home.setVisibility(View.VISIBLE);
                image_location_off.setVisibility(View.INVISIBLE);
                image_location_on.setVisibility(View.INVISIBLE);
                image_location_wait.setVisibility(View.INVISIBLE);
                break;
            case PROVIDER_DISABLED:
                image_location_disabled.setVisibility(View.GONE);
                image_location_home.setVisibility(View.VISIBLE);
                image_location_off.setVisibility(View.GONE);
                image_location_on.setVisibility(View.INVISIBLE);
                image_location_wait.setVisibility(View.INVISIBLE);
                break;
            case WAIT_FOR_GPS_FIX:
                image_location_disabled.setVisibility(View.GONE);
                image_location_home.setVisibility(View.GONE);
                image_location_off.setVisibility(View.GONE);
                image_location_on.setVisibility(View.INVISIBLE);
                image_location_wait.setVisibility(View.VISIBLE);
                break;
            case GPS_FIX:
                image_location_disabled.setVisibility(View.GONE);
                image_location_home.setVisibility(View.GONE);
                image_location_off.setVisibility(View.GONE);
                image_location_on.setVisibility(View.VISIBLE);
                image_location_wait.setVisibility(View.INVISIBLE);
                break;
            case GPS_TIMEOUT:
                image_location_disabled.setVisibility(View.GONE);
                image_location_home.setVisibility(View.GONE);
                image_location_off.setVisibility(View.VISIBLE);
                image_location_on.setVisibility(View.GONE);
                image_location_wait.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Show one of the images expand more/less depending on the state of expandView
     */
    private void onShowExpandViewStatus() {
        ImageView image_expand_more = main.findViewById(R.id.image_expand_more);
        ImageView image_expand_less = main.findViewById(R.id.image_expand_less);

        _updateExpandViewStatus = false;
        switch (_expandViewVisibility) {
            case View.INVISIBLE:
                image_expand_more.setVisibility(View.GONE);
                image_expand_less.setVisibility(View.VISIBLE);
                break;
            case View.VISIBLE:
                image_expand_more.setVisibility(View.VISIBLE);
                image_expand_less.setVisibility(View.GONE);
                break;
            default:
                image_expand_more.setVisibility(View.INVISIBLE);
                image_expand_less.setVisibility(View.GONE);
        }
    }

    /**
     * Show Error message
     */
    private void onShowErrorMessage() {
        _updateExpandView = false;

        TextView main_text_title = findViewById(R.id.main_text_title);
        main_text_title.setBackgroundColor(COLOR_RED);
        main_text_title.setText(errorMessage);
        _updateErrorMessage = false;
    }

    /**
     * Show additional info
     */
    private void onShowAdditionalInfo() {
        _updateExpandView = false;

        if (additionalInfo != null) {
            TextView main_text_title = findViewById(R.id.main_text_title);
            main_text_title.setBackgroundColor(COLOR_MESSAGE);
            main_text_title.setText(additionalInfo.title);

//                TextView commentView = main.findViewById(R.id.comment_view);
//        commentView.setText(additionalInfo.comment);

            TextView descriptionView = main.findViewById(R.id.description_view);
            String html = additionalInfo.description;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                descriptionView.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
            } else {
                descriptionView.setText(Html.fromHtml(html));
            }
            descriptionView.scrollTo(0, 0);

            TextView linkView = main.findViewById(R.id.link_view);
            linkView.setText(additionalInfo.link);
        }

        LinearLayout location_content = main.findViewById(R.id.location_content);
        LinearLayout description_content = main.findViewById(R.id.description_content);

        location_content.setVisibility(expandView ? View.GONE : View.VISIBLE);
        description_content.setVisibility(expandView ? View.VISIBLE : View.GONE);

        if (!expandView) {
            la.updateStatus();
            la.scrollToListPosition();
        }
    }

    /**
     * Show additional info
     */
    private void onShowAdditionalInfo2() {
        _updateAdditionalInfo = false;

        String comment = _comment;
        if (additionalInfo != null) {
            comment = comment + additionalInfo.comment;
        }
        TextView commentView = main.findViewById(R.id.comment_view);
        commentView.setText(comment);
    }

    /**
     * Show/hide the profile
     */
    private void onShowProfile() {
        _updateProfile = false;
        LinearLayout plot = main.findViewById(R.id.profile_plot);
        plot.setVisibility(_profileViewVisibility == View.VISIBLE ? View.VISIBLE : View.GONE);
        switch (_profileViewVisibility) {
            case View.INVISIBLE:
                setViewVisibility(R.id.image_show_profile, View.VISIBLE);
                setViewVisibility(R.id.image_hide_profile, View.INVISIBLE);
                break;
            case View.VISIBLE:
                setViewVisibility(R.id.image_show_profile, View.GONE);
                setViewVisibility(R.id.image_hide_profile, View.VISIBLE);
                break;
            default:
                setViewVisibility(R.id.image_show_profile, View.INVISIBLE);
                setViewVisibility(R.id.image_hide_profile, View.GONE);
        }

        if (_profileViewVisibility == View.VISIBLE) {
            la.updateStatus();
            la.scrollToListPosition();
        }
    }

    private int getViewWidth(int id) {
        TextView view = findViewById(id);
        return view.getWidth();
    }

    private void setViewVisibility(int id, int visibility) {
        if (main != null) {
            ImageView view = main.findViewById(id);
            if (view != null)
                view.setVisibility(visibility);
        }
    }

    /**
     * Initialize the user interface before loading a new GPX track
     */
    private void initUserInterface() {
        fileInfoAvailable = false;

        setTrackingStatus(false);

        activateProfile(View.GONE);
        setExpandViewStatus(getExpandView());
        setViewVisibility(R.id.image_expand_more, View.GONE);
        _initUserInterface = true;
    }

    /**
     * Setup the user interface after loading a new GPX track
     */
    public void setupUserInterface() {
        fileInfoAvailable = details.isFileInfoAvailable();
        _comment = "";
        showAddInfo(-1);

//        activateProfile(View.VISIBLE);
        activateProfile(getProfileViewVisibility());
        _setupUserInterface = true;
    }


    /**
     * Get the tracking status
     *
     * @return true if the GPS provider controls the tracking
     */
    public boolean isTracking() {
        return _isTracking;
    }

    /**
     * Set the tracking status
     *
     * @param inTracking true if the GPS provider shall controls the tracking
     */
    public void setTrackingStatus(boolean inTracking) {
        _isTracking = inTracking;
        if (inTracking)
            setExpandViewStatus(false);
        updateTrackingStatus();
    }

    /**
     * Update the controls showing the tracking status
     */
    public void updateTrackingStatus() {
        _updateTrackingStatus = true;
    }


    /**
     * Show the GPS status
     */
    public void showGPS_Status(LocationActivity.gpsStatus gpsStatus) {
        _gpsStatus = gpsStatus;
        _updateGpsStatus = true;
        updateTrackingStatus();
    }

    /**
     * Show one of the images expand more/less depending on the state of expandView
     *
     * @param inPlace row index of the table or -1
     * @return true if view is expanded
     */
    public boolean showExpandViewStatus(int inPlace, boolean inExpand) {
        boolean isExpandableView = fileInfoAvailable;
        if (details != null) {
            _place = inPlace;
            if (inPlace >= 0)
                isExpandableView = !details.getRoutePointDescription(inPlace).equals("");
            if (isExpandableView)
                if (inExpand)
                    _expandViewVisibility = View.INVISIBLE;
                else {
                    _expandViewVisibility = View.VISIBLE;
                    isExpandableView = false;
                }
            else
                _expandViewVisibility = View.GONE;

            _updateExpandViewStatus = true;
            _updateProfile = true;
        } else
            isExpandableView = false;

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
        expandView = showExpandViewStatus(place, inExpand);
        setExpandView(inExpand);
        showAddInfo(place);
    }

    public boolean getExpandViewStatus() {
        return expandView;
    }

    /**
     * Show the file info if available
     */
    public void showFileInfo() {
        // update the expansion mode
        expandView = showExpandViewStatus(-1, true);
        showAddInfo(-1);
    }

    /**
     * Show additional info if available
     */
    public void showAddInfo(int inPlace) {
        if (details != null) {
            additionalInfo = details.getAdditionalInfo(inPlace);
            _updateExpandView = true;
            _updateAdditionalInfo = true;
        } else
            additionalInfo = null;
    }

    /**
     * Set the distance to the next place
     */
    public void setDistanceToPlace(double distanceToPlace) {
        _comment = new DecimalFormat("in #0.0 km: ").format(distanceToPlace);
        _updateAdditionalInfo = true;
    }

    /**
     * Show/hide the profile
     *
     * @param state view state
     */
    public void activateProfile(int state) {
        _profileViewVisibility = state;
        if (state != View.GONE)
            setProfileViewVisibility(state);
        _updateProfile = true;
    }

    public void showErrorMessage(String message)
    {
        errorMessage = message;
        _updateErrorMessage = true;

    }
}