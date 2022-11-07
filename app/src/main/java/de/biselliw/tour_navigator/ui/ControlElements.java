package de.biselliw.tour_navigator.ui;

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

import androidx.annotation.NonNull;
import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.LocationActivity;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.data.TourDetails;
import de.biselliw.tour_navigator.helpers.ProfileAdapter;

public class ControlElements extends BaseActivity {

    public static ControlElements control = null;
    public App app = null;
    public static LocationActivity la = null;
    public MainActivity main = null;
    public ProfileAdapter pa = null;

    protected TourDetails details = null;

    // todo Do not place Android context classes in static fields (static reference to `RecordAdapter` which has field `recordContext` pointing to `Context`); this is a memory leak
    static RecordAdapter recordAdapter = null;

    protected static boolean expandView = false;
    private static boolean profileView = false;
    private static boolean fileInfoAvailable = false;

    private static final int COLOR_MESSAGE = 0xFFFFFFFF;

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
    boolean _updateExpandView = false;
    boolean _updateProfile = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Install a timer to update control elements */
        //runs without a timer by reposting this handler at the end of the runnable
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                if (_initUserInterface)
                {
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
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        for (int i=0; i<menu.size(); i++)
        {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            if (
                    (id == R.id.itm_pause_time) ||
                    (id == R.id.itm_delete_waypoint) ||
                    (id == R.id.itm_nav_waypoint) ||
                    (id == R.id.itm_nav_google))
                item.setEnabled(!expandView && (_place >= 0));
            else if (id == R.id.itm_copy_timetable)
                item.setVisible(false);
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

    void onPrepareMenu(@NonNull Menu menu)
    {

    }
    public boolean onPrepareNavigationMenu (Menu menu)
    {
        for (int i=0; i<menu.size(); i++)
        {
            MenuItem item = menu.getItem(i);
            int id = item.getItemId();
            if (id == R.id.nav_file_info)
                item.setEnabled(!_initUserInterface && fileInfoAvailable);
            else if (
                    (id == R.id.nav_reverse_route) ||
                            (id == R.id.nav_start_time) ||
                            (id == R.id.nav_download_gpx) ||
                            (id == R.id.nav_download_html)
            )
                item.setEnabled(!_initUserInterface);
            else
                item.setVisible(true);
        }

        return true;
    }

    /**
     * Set the optimum width to show the place
     */
    private void onRescalePlaceView ()
    {
        TextView placeView = findViewById(R.id.track_place);
        TableLayout table_layout1 = findViewById(R.id.table_layout1);
        int layoutWidth = table_layout1.getWidth();

        int h_track_arrive = getViewWidth(R.id.h_track_arrive);
        int h_track_distance = getViewWidth(R.id.h_track_distance);
        int h_track_dist_to_dest = getViewWidth(R.id.h_track_dist_to_dest);
        int sum = h_track_arrive + h_track_distance + h_track_dist_to_dest;

        int colWidth = layoutWidth - sum;
        if (colWidth > 0)
        {
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
     * Show additional info
     */
    private void onShowAdditionalInfo() {
        _updateExpandView = false;

        TextView main_text_title = findViewById(R.id.main_text_title);
        main_text_title.setBackgroundColor(COLOR_MESSAGE);
        main_text_title.setText(additionalInfo.title);

        TextView commentView = main.findViewById(R.id.comment_view);
        commentView.setText(additionalInfo.comment);
        TextView descriptionView = main.findViewById(R.id.description_view);

        String html = additionalInfo.description;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            descriptionView.setText(Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY));
        } else {
            descriptionView.setText(Html.fromHtml(html));
        }

        descriptionView.scrollTo(0,0);

        TextView linkView = main.findViewById(R.id.link_view);
        linkView.setText(additionalInfo.link);

        LinearLayout location_content    = main.findViewById(R.id.location_content);
        LinearLayout description_content = main.findViewById(R.id.description_content);

        location_content.setVisibility(expandView ? View.GONE : View.VISIBLE);
        description_content.setVisibility(expandView ? View.VISIBLE : View.GONE);

        if (!expandView)
        {
            la.updateStatus();
            la.scrollToListPosition();
        }
    }

    /**
     * Show/hide the profile
     */
    private void onShowProfile() {
        _updateProfile = false;
        LinearLayout plot = main.findViewById(R.id.profile_plot);
        plot.setVisibility((profileView && !expandView) ? View.VISIBLE : View.GONE);

        if (expandView) {
            setViewVisibility(R.id.image_show_profile, View.INVISIBLE);
            setViewVisibility(R.id.image_hide_profile, View.GONE);
        } else {
            setViewVisibility(R.id.image_show_profile, profileView ? View.GONE : View.VISIBLE);
            setViewVisibility(R.id.image_hide_profile, profileView ? View.VISIBLE : View.GONE);
        }

        if (!expandView)
        {
            la.updateStatus();
            la.scrollToListPosition();
        }
    }

    private int getViewWidth(int id)
    {
        TextView view = findViewById(id);
        return view.getWidth();
    }

    private void setViewVisibility(int id, int visibility) {
        ImageView view = main.findViewById(id);
        if (view != null)
            view.setVisibility(visibility);
    }

    /**
     * Initialize the user interface before loading a new GPX track
     */
    private void initUserInterface() {
        fileInfoAvailable = false;

        setTrackingStatus(false);

        activateProfile(false);
        setExpandViewStatus(true);
        setViewVisibility(R.id.image_expand_more, View.GONE);
        _initUserInterface = true;
    }




    public static void registerRecordAdapter(RecordAdapter adapter) {
        recordAdapter = adapter;
    }

    /**
     * Setup the user interface after loading a new GPX track
     */
    public void setupUserInterface() {
        fileInfoAvailable = details.isFileInfoAvailable();
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
        _place = inPlace;
        if (inPlace >= 0)
           isExpandableView = details.isDescriptionAvailable(inPlace);
        if (isExpandableView)
            if (inExpand)
                _expandViewVisibility = View.INVISIBLE;
            else
            {
                _expandViewVisibility = View.VISIBLE;
                isExpandableView = false;
            }
        else
            _expandViewVisibility = View.GONE;

        _updateExpandViewStatus = true;
        _updateProfile = true;

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
        showAddInfo(place);
    }

    public boolean getExpandViewStatus()
    {
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
        additionalInfo = details.getAdditionalInfo(inPlace);
        _updateExpandView = true;
    }

    /**
     * Show/hide the profile
     *
     * @param visible true to show the profile
     */
    public void activateProfile(boolean visible) {
        profileView = visible;
        _updateProfile = true;
    }


}