package de.biselliw.tour_navigator.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.helper.AppCompatPreferenceActivity;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.data.TrackDetails;
import de.biselliw.tour_navigator.tim_prune.I18nManager;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    static App _app = null;
    static SharedPreferences sharedPref = null;

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    // hiking speed parameters
    final static int DEF_HOR_SPEED = (int)(1000 * TrackDetails.DEF_HOR_SPEED); // horizontal part in [km/h]
    final static int DEF_VERT_SPEED_CLIMB = (int)(1000 * TrackDetails.DEF_VERT_SPEED_CLIMB); // ascending part in [km/h]
    final static int DEF_VERT_SPEED_DESC = (int)(1000 * TrackDetails.DEF_VERT_SPEED_DESC); // descending part in [km/h]
    final static int DEF_MIN_HEIGHT_CHANGE = TrackDetails.DEF_MIN_HEIGHT_CHANGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        overridePendingTransition(0, 0);

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(BaseActivity.MAIN_CONTENT_FADEIN_DURATION);
        }
    }

    @Override
    protected void onDestroy() {
        getPreferences(sharedPref, _app);
        super.onDestroy();
    }

    /**
     * get preferences for hiking times calculation
     *
     * @param inSharedPref
     * @param inApp
     */
    static public void getPreferences (SharedPreferences inSharedPref, App inApp)
    {
        sharedPref = inSharedPref;
        _app = inApp;

        // hiking speed parameters
        int horSpeed = DEF_HOR_SPEED; // horizontal part in [km/h]
        int vertSpeedClimb = DEF_VERT_SPEED_CLIMB; // ascending part in [km/h]
        int vertSpeedDescent = DEF_VERT_SPEED_DESC; // descending part in [km/h]
        int minHeightChange = DEF_MIN_HEIGHT_CHANGE;

        String stringValue;
        try {
            stringValue = sharedPref.getString("pref_hiking_par_horSpeed", "");
            if (!stringValue.equals(""))
                horSpeed = Integer.parseInt(stringValue);
            stringValue = sharedPref.getString("pref_hiking_par_vertSpeedClimb", "");
            if (!stringValue.equals(""))
                vertSpeedClimb = Integer.parseInt(stringValue);
            stringValue = sharedPref.getString("pref_hiking_par_vertSpeedDescent", "");
            if (!stringValue.equals(""))
                vertSpeedDescent = Integer.parseInt(stringValue);
        }
        catch (Exception e) { }
        _app.setHikingParameters(horSpeed / 1000.0,vertSpeedClimb / 1000.0, vertSpeedDescent / 1000.0, minHeightChange);
    }


    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                finish();
                //NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
 //       loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // Set the summary to reflect the new value.
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            int val = -1;
            try {
                val = Integer.valueOf(stringValue);
            }
            catch (Exception e) {  }
            String key = preference.getKey();
            int minValue=0, defValue=0, maxValue=0;
            EditTextPreference pref = (EditTextPreference)preference;

            switch (key) {
                case "pref_hiking_par_horSpeed":
                    minValue = 2000;
                    defValue = DEF_HOR_SPEED;
                    maxValue = 130000;
                    break;
                case "pref_hiking_par_vertSpeedClimb":
                    minValue = 100;
                    defValue = DEF_VERT_SPEED_CLIMB;
                    maxValue = 80000;
                    break;
                case "pref_hiking_par_vertSpeedDescent":
                    minValue = 200;
                    defValue = DEF_VERT_SPEED_DESC;
                    maxValue = 80000;
                    break;
            }
            if ((val < minValue) || (val > maxValue)){
                stringValue = String.valueOf(defValue);
                pref.setText(stringValue);
                String set_to_default = I18nManager.getText("pref_hiking_par_set_to_default");
                stringValue = set_to_default + stringValue;
            }
            preference.setSummary(stringValue);
        }
        return true;
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     * The commented method bindPrefenceSummaryToValue should be added for all preferences
     * with a summary that is depended from the current value of the preference
     */
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("pref_hiking_par_horSpeed"));
            bindPreferenceSummaryToValue(findPreference("pref_hiking_par_vertSpeedClimb"));
            bindPreferenceSummaryToValue(findPreference("pref_hiking_par_vertSpeedDescent"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static void setSharedPreferences(SharedPreferences inSharedPref)
    {
        sharedPref = inSharedPref;
    }

    public static boolean isFirstTimeLaunch() {
        return sharedPref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public static void setFirstTimeLaunch(boolean isFirstTime) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

}
