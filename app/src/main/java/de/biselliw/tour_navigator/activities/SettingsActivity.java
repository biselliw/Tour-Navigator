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

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import android.text.InputType;
import android.text.format.Time;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.data.BaseSegments;
import de.biselliw.tour_navigator.helpers.Log;

import static de.biselliw.tour_navigator.ui.ControlElements.setAlarmPreference;

/**
 * Application settings
 * @see <a href="https://developer.android.com/reference/androidx/preference/package-summary">AndroidX Preference Library</a>
 */
public class SettingsActivity extends AppCompatActivity {
    static Resources _resources = null;
    static SharedPreferences sharedPref = null;

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    /** default hiking speed parameter: horizontal part in [m/h] */
    final static int DEF_HOR_SPEED = (int)(1000 * BaseSegments.DEF_HOR_SPEED);
    /** default hiking speed parameter: ascending part in [m/h] */
    final static int DEF_SPEED_CLIMB = (int)(1000 * BaseSegments.DEF_SPEED_CLIMB);
    /** default hiking speed parameter: descending part in [m/h]; */
    final static int DEF_SPEED_DESCENT = (int)(1000 * BaseSegments.DEF_SPEED_DESCENT);

    static String[] keys = new String[]{"pref_hiking_par_horSpeed", "pref_hiking_par_speedClimb", "pref_hiking_par_speedDescent"};
    static int[] defaults = new int[]{DEF_HOR_SPEED, DEF_SPEED_CLIMB, DEF_SPEED_DESCENT};
    static boolean setDefaults = false;

    static boolean hikingParametersChanged = false;

    public static class SettingsFragment extends PreferenceFragmentCompat {

        void bindPreferenceToValue(int ID) {
            EditTextPreference ref = findPreference(keys[ID]);
            if (ref != null) {
                ref.setSummaryProvider(
                        EditTextPreference.SimpleSummaryProvider.getInstance()
                );
                ref.setOnBindEditTextListener(editText -> {
                    editText.setInputType(
                            InputType.TYPE_CLASS_NUMBER
                    );
                    editText.setSingleLine(true);
                });

                ref.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (preference instanceof EditTextPreference) {
                        // For all other preferences, set the summary to the value's
                        // simple string representation.
                        String stringValue = newValue.toString();
                        int val = 0;
                        try {
                            val = Integer.parseInt(stringValue);
                        }
                        catch (Exception ignored) {  }
                        String key = preference.getKey();
                        int minValue=0, defValue=0, maxValue=0;

                        if (key.equals(keys[0])) {
                            minValue = 1000;
                            defValue = defaults[0];
                            maxValue = 130000;
                        }
                        else if (key.equals(keys[1])) {
                            minValue = 100;
                            defValue = defaults[1];
                            maxValue = 80000;
                        }
                        else if (key.equals(keys[2])) {
                            minValue = 100;
                            defValue = defaults[2];
                            maxValue = 80000;
                        }
                        //preference.setDefaultValue(defValue);
                        if ((val < minValue) || (val > maxValue)){
                            /* Apply default value */
                            stringValue = String.valueOf(defValue);
                            EditTextPreference pref = (EditTextPreference)preference;
                            pref.setText(stringValue);
                            return false;
                        }
                        hikingParametersChanged = true;
                    }
                    else if (preference instanceof SwitchPreference) {
                        boolean val = (boolean)newValue;
                        String key = preference.getKey();
                        if (key.equals("pref_debug")) {
                            if (!val)
                                Log.clearHTML();
                        }
                    }
                    return true;
                });
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceToValue(0);
            bindPreferenceToValue(1);
            bindPreferenceToValue(2);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        overridePendingTransition(0, 0);

        _resources = getResources();

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }

        String def = sharedPref.getString(keys[0],"");
        if (def.isEmpty()) {
            // set default preferences for hiking times calculation
            SharedPreferences.Editor editor = sharedPref.edit();
            for (int i = 0; i <= 2; i++)
            {
                def = String.valueOf(defaults[i]);
                editor.putString(keys[i], def);
            }
            editor.apply();
            setDefaults = true;
        }
    }


    @Override
    public void onBackPressed() {
        updatePreferences();
        super.onBackPressed();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            updatePreferences();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /*
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                finish();
                return super.onMenuItemSelected(featureId, item);
            }
        }
    }
    */



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
            actionBar.show();
            //actionBar.setDisplayHomeAsUpEnabled(true);

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

    /*
     * --------------------------------------------------------------------------------------------
     * Public methods
     * --------------------------------------------------------------------------------------------
     */

    /**
     * update preferences for hiking times calculation if they were changed
     */
    static public boolean updatePreferences() {
        if (hikingParametersChanged) {
            getPreferences();
            hikingParametersChanged = false;
            App.app.recalculate();
            App.app.Update();
            return true;
        }
        if (!getConsentDebug() )
        {
            Log.clearHTML();
        }
        return false;
    }

    static int getIntFromPref(String inKey, int inDefault) {
        int value = inDefault;
        try {
            value = Integer.parseInt(sharedPref.getString(inKey,""));
        }
        catch (Exception ignored) { }
        return value;
    }

    /**
     * get preferences
     */
    static public void getPreferences()
    {
        setWritingEnabled (sharedPref.getBoolean("pref_debug", false));
        setAlarmPreference(sharedPref.getBoolean("pref_hiking_par_alarm", true));
    }

    /**
     * get preferences for hiking times calculation
     */
    static public void getHikingParameters(BaseSegments inSegments)
    {
        // hiking speed parameters

        // horizontal part in [km/h]
        int horSpeed = getIntFromPref(keys[0], DEF_HOR_SPEED);
        // ascending part in [km/h]
        int vertSpeedClimb = getIntFromPref(keys[1], DEF_SPEED_CLIMB);
        // descending part in [km/h]
        int vertSpeedDescent = getIntFromPref(keys[2], DEF_SPEED_DESCENT);

        setWritingEnabled (sharedPref.getBoolean("pref_debug", false));

        // set hiking parameters
        inSegments.setHikingParameters(horSpeed / 1000.0,vertSpeedClimb / 1000.0,
                vertSpeedDescent / 1000.0, BaseSegments.DEF_MIN_HEIGHT_CHANGE);
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
        Time time = new Time();
        time.set(sharedPref.getLong("StartTime", 0L));
        int hour = time.hour;
        int min = time.minute;
        time.setToNow();
        time.set(0,min,hour,time.monthDay,time.month,time.year);
        long startTime = time.toMillis(true);
        return startTime;
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

    private static void setWritingEnabled (boolean isEnabled) {
        Log.setWritingEnabled (isEnabled, "Tour Navigator ");
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
