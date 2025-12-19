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

    Copyright 2025 Walter Biselli (BiselliW)
*/

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.helper.AppCompatPreferenceActivity;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.data.TrackTiming;
import de.biselliw.tour_navigator.helpers.Log;

/**
 * Application settings
 * @see <a href="https://developer.android.com/reference/android/preference/PreferenceActivity">PreferenceActivity</a>
 * @deprecated in API level 29
 * @todo migrate to <a href="https://developer.android.com/reference/androidx/preference/package-summary"></a>AndroidX Preference Library</a>
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    static Resources _resources = null;
    static SharedPreferences sharedPref = null;

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    /** default hiking speed parameter: horizontal part in [m/h] */
    final static int DEF_HOR_SPEED = (int)(1000 * TrackTiming.DEF_HOR_SPEED);
    /** default hiking speed parameter: ascending part in [m/h] */
    final static int DEF_VERT_SPEED_CLIMB = (int)(1000 * TrackTiming.DEF_VERT_SPEED_CLIMB);
    /** default hiking speed parameter: descending part in [m/h]; */
    final static int DEF_VERT_SPEED_DESC = (int)(1000 * TrackTiming.DEF_VERT_SPEED_DESC);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        overridePendingTransition(0, 0);

        _resources = getResources();
        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(BaseActivity.MAIN_CONTENT_FADEIN_DURATION);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                finish();
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Private methods
     * --------------------------------------------------------------------------------------------
     */

    /**
     * Setup the {@link android.app.ActionBar}
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // show the user that selecting home will return one level up
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * store a boolean value in the settings
     * @param key key name
     * @param value value
     */
    private static void setBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * store an integer value in the settings
     * @param key key name
     * @param value value
     */
    private static void setInt(String key, int value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Callback to be invoked when the value of this Preference has been changed by the user
     * @deprecated in API level 29
     * @todo migrate to <a href="https://developer.android.com/reference/androidx/preference/package-summary"></a>AndroidX Preference Library</a>
     */
    private static Preference.OnPreferenceChangeListener preferenceChangeListener =
            (preference, value) -> {
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
        }
        else if (preference instanceof SwitchPreference) {
            // not reachable
        }
        else if (preference instanceof EditTextPreference) {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            int val = -1;
            try {
                val = Integer.parseInt(stringValue);
            }
            catch (Exception ignored) {  }
            String key = preference.getKey();
            int minValue=0, defValue=0, maxValue=0;

            switch (key) {
                case "pref_hiking_par_horSpeed":
                    minValue = 1000;
                    defValue = DEF_HOR_SPEED;
                    maxValue = 130000;
                    break;
                case "pref_hiking_par_vertSpeedClimb":
                    minValue = 100;
                    defValue = DEF_VERT_SPEED_CLIMB;
                    maxValue = 80000;
                    break;
                case "pref_hiking_par_vertSpeedDescent":
                    minValue = 100;
                    defValue = DEF_VERT_SPEED_DESC;
                    maxValue = 80000;
                    break;
            }
            if ((val < minValue) || (val > maxValue)){
                /* Apply default value */
                stringValue = String.valueOf(defValue);
                EditTextPreference pref = (EditTextPreference)preference;
                pref.setText(stringValue);
                stringValue = _resources.getString(R.string.pref_hiking_par_set_to_default) + stringValue;
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
     * @see #preferenceChangeListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(preferenceChangeListener);

        // Trigger the listener immediately with the preference's
        // current value.
        preferenceChangeListener.onPreferenceChange(preference,
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

    /*
     * --------------------------------------------------------------------------------------------
     * Public methods
     * --------------------------------------------------------------------------------------------
     */

    /**
     * get preferences for hiking times calculation
     *
     * @param inSharedPref
     */
    static public void getPreferences (SharedPreferences inSharedPref)
    {
        sharedPref = inSharedPref;

        // hiking speed parameters
        int horSpeed = DEF_HOR_SPEED; // horizontal part in [km/h]
        int vertSpeedClimb = DEF_VERT_SPEED_CLIMB; // ascending part in [km/h]
        int vertSpeedDescent = DEF_VERT_SPEED_DESC; // descending part in [km/h]

        String stringValue;
        try {
            stringValue = sharedPref.getString("pref_hiking_par_horSpeed", "");
            if (!stringValue.isEmpty())
                horSpeed = Integer.parseInt(stringValue);
            stringValue = sharedPref.getString("pref_hiking_par_vertSpeedClimb", "");
            if (!stringValue.isEmpty())
                vertSpeedClimb = Integer.parseInt(stringValue);
            stringValue = sharedPref.getString("pref_hiking_par_vertSpeedDescent", "");
            if (!stringValue.isEmpty())
                vertSpeedDescent = Integer.parseInt(stringValue);

            Log.setWritingEnabled (sharedPref.getBoolean("pref_debug", false), "Tour Navigator ");
        }
        catch (Exception ignored) { }

        // set hiking parameters
        TrackTiming.setHikingParameters(horSpeed / 1000.0,vertSpeedClimb / 1000.0,
                vertSpeedDescent / 1000.0, TrackTiming.DEF_MIN_HEIGHT_CHANGE);
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
            addPreferencesFromResource(R.xml.preferences);
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
        setBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
    }

    public static long getStartTime() {
        return sharedPref.getLong("StartTime", 0L);
    }

    public static void setStartTime(long value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong("StartTime", value);
        editor.apply(); // editor.commit();
    }

    /**
     * Preferences for use of Google Maps
     */
    public static boolean getConsentGoogleMaps() {
        return sharedPref.getBoolean("pref_consent_google_maps", false);
    }

    public static void consentGoogleMaps(boolean value) {
        setBoolean("pref_consent_google_maps", value);
    }

    /**
     * Preferences for writing Debug infos to Download dir
     */
    public static boolean getConsentDebug() {
        return sharedPref.getBoolean("pref_debug", false);
    }

    /**
     * Shared Preferences to restore application after shut down
     * @todo is getProfileViewVisibility() still needed?
     */
    public static int getProfileViewVisibility() {
        int value = sharedPref.getInt("ProfileViewVisibility", View.VISIBLE);
        if (value == View.GONE) value = View.VISIBLE;
        return value;
    }

    public static void setProfileViewVisibility(int value) {
        setInt("ProfileViewVisibility", value);
    }
}
