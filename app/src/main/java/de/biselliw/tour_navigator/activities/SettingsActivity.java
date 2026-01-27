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

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.data.TrackSegments;
import de.biselliw.tour_navigator.dialogs.AcceptGoogleMapsPolicyDialog;
import de.biselliw.tour_navigator.helpers.Log;

import static de.biselliw.tour_navigator.ui.ControlElements.setAlarmPreference;

/**
 * Application settings
 * @see <a href="https://developer.android.com/reference/androidx/preference/package-summary">AndroidX Preference Library</a>
 */
public class SettingsActivity extends BaseActivity {
    // SettingsActivity
    // └─ activity_settings.xml
    //     └─ FrameLayout (settings_container)
    //         └─ SettingsFragment (PreferenceFragmentCompat)
    //             └─ preferences.xml
    static Resources _resources = null;
    static AppCompatActivity activity;
    static SharedPreferences sharedPref = null;
    private static SettingsFragment _settingsFragment = null;
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    static boolean hikingParametersChanged = false;


    public static class SettingsFragment extends PreferenceFragmentCompat {

        SwitchPreferenceCompat pref_consent_internet = null;
        SwitchPreferenceCompat pref_consent_google_maps = null;

        private class IntPreference {
            EditTextPreference textPref;
            public String key = "";
            public int defaultValue, minValue, maxValue;

            public IntPreference(MainActivity.Parameter inParameter) {
                this.key = inParameter.key;
                this.defaultValue = inParameter.defaultValue;
                this.minValue = inParameter.minValue;
                this.maxValue = inParameter.mavValue;
                intPreference();
            }

            public void intPreference() {
                textPref = findPreference(key);
                if (textPref != null) {
                    textPref.setOnBindEditTextListener(editText -> {
                        editText.setInputType(
                                InputType.TYPE_CLASS_NUMBER
                        );
                        editText.setSingleLine(true);
                    });

                    textPref.setOnPreferenceChangeListener((preference, newValue) -> {
                        String stringValue = (String) newValue;

                        int value = 0;
                        try {
                            assert stringValue != null;
                            value = Integer.parseInt(stringValue);
                        } catch (Exception ignored) {
                        }
                        hikingParametersChanged = true;
                        if ((value < minValue) || (value > maxValue)) {
                            /* Apply default value */
                            stringValue = String.valueOf(defaultValue);
                            textPref.setText(stringValue);
                            return false; // Reject empty input
                        }
                        return true; // Accept and persist value
                    });

                    textPref.setSummaryProvider(preference -> {
                        String stringValue = ((EditTextPreference) preference).getText();
                        int value = 0;
                        try {
                            assert stringValue != null;
                            value = Integer.parseInt(stringValue);
                        } catch (Exception ignored) {
                        }
                        //preference.setDefaultValue(defValue);
                        if (value == defaultValue)
                            /* Apply default value */
                            stringValue = _resources.getString(R.string.set_to_default) + ": " + defaultValue;
                        return stringValue;
                    });
                }
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // hide Debug setting in releases
            if (!BuildConfig.DEBUG) {
                SwitchPreferenceCompat switchPref = findPreference("pref_debug");
                if (switchPref != null)
                    switchPref.setVisible(false);
            }

            // Bind the summaries of EditText preferences to their values. When their values change,
            // their summaries are updated to reflect the new value, per the Android Design guidelines.
            if (MainActivity.hikingParameters != null)
                for (int i = 0; i < MainActivity.hikingParameters.size(); i++)
                    new IntPreference(MainActivity.hikingParameters.get(i));

            pref_consent_internet =    findPreference("pref_consent_internet");
            pref_consent_google_maps = findPreference("pref_consent_google_maps");
            if (pref_consent_google_maps != null) {
                pref_consent_google_maps.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        if (pref_consent_internet != null && pref_consent_internet.isChecked()) {
                            AcceptGoogleMapsPolicyDialog acceptDialog = new AcceptGoogleMapsPolicyDialog(activity);
                            acceptDialog.show();
                        }
                        else
                            // refuse the new value
                            return false;
                    }
                    // Handle change (e.g., enable/disable notifications)
                    return true; // true = save the new value
                });
            }

            if (pref_consent_internet != null) {
                pref_consent_internet.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (!enabled) {
                        if (pref_consent_google_maps != null)
                            pref_consent_google_maps.setChecked(false);
                    }
                    // save the new value
                    return true;
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        activity = this;
        _resources = getResources();

        if (savedInstanceState == null) {
            _settingsFragment = new SettingsFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, _settingsFragment)
                    .commit();
        }

        // set default value on first time launch
        ArrayList<MainActivity.Parameter> hikingParameters = MainActivity.hikingParameters;
        if (sharedPref != null && hikingParameters != null) {
            String def = sharedPref.getString(hikingParameters.get(0).key, "");
            if (def.isEmpty()) {
                // set default preferences for hiking times calculation
                SharedPreferences.Editor editor = sharedPref.edit();
                for (int i = 0; i < hikingParameters.size(); i++) {
                    editor.putString(hikingParameters.get(i).key,
                            String.valueOf(hikingParameters.get(i).defaultValue));
                }
                editor.apply();
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // entspricht systemkonformer Back-Navigation
        return true;
    }

    @Override

    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

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

    /**
     * Get a boolean value from settings
     * @param inKey key name
     * @param inDefault default value
     */
     public static boolean getBooleanPref(String inKey, boolean inDefault) {
        if (sharedPref != null)
            return sharedPref.getBoolean(inKey,inDefault);
        else
            return inDefault;
    }

    /**
     * store an integer value in the settings
     * @param inKey key name
     * @param inValue value
     */
    private static void setIntPref(String inKey, int inValue) {
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(inKey, inValue);
            editor.apply();
        }
    }

/*
 * --------------------------------------------------------------------------------------------
 * Public methods
 * --------------------------------------------------------------------------------------------
 */

    public static void setSharedPreferences(SharedPreferences inSharedPref)
    {
        sharedPref = inSharedPref;
    }

    /**
     * get preferences
     */
    public static void getPreferences()
    {
        if (sharedPref != null) {
            setWritingEnabled(sharedPref.getBoolean("pref_debug", false));
            setAlarmPreference(sharedPref.getBoolean("pref_hiking_par_alarm", true));
        }
    }

    /**
     * get preferences for hiking times calculation
     * @param inSegments
     */
    public static void getHikingParameters(TrackSegments inSegments)
    {
        ArrayList<MainActivity.Parameter> hikingParameters = MainActivity.hikingParameters;
        if (hikingParameters != null) {
            // hiking speed parameters

            // horizontal part in [km/h]
            int horSpeed = getIntFromPref(hikingParameters.get(0).key, hikingParameters.get(0).defaultValue);
            // ascending part in [km/h]
            int vertSpeedClimb = getIntFromPref(hikingParameters.get(1).key, hikingParameters.get(1).defaultValue);
            // descending part in [km/h]
            int vertSpeedDescent = getIntFromPref(hikingParameters.get(2).key, hikingParameters.get(2).defaultValue);

            // set hiking parameters
            inSegments.setHikingParameters(horSpeed / 1000.0, vertSpeedClimb / 1000.0,
                    vertSpeedDescent / 1000.0);
        }
    }

    public static boolean isFirstTimeLaunch() {
        return getBooleanPref(IS_FIRST_TIME_LAUNCH, true);
    }

    public static void setFirstTimeLaunch(boolean isFirstTime) {
        setBooleanPref(IS_FIRST_TIME_LAUNCH, isFirstTime);
    }

    public static long getStartTime() {
        Time time = new Time();
        if (sharedPref != null)
            time.set(sharedPref.getLong("StartTime", 0L));
        int hour = time.hour;
        int min = time.minute;
        time.setToNow();
        time.set(0,min,hour,time.monthDay,time.month,time.year);
        return time.toMillis(true);
    }

    public static void setStartTime(long inValue) {
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong("StartTime", inValue);
            editor.apply(); // editor.commit();
        }
    }

    /**
     * Preferences for use of Internet
     */
    public static boolean getConsentInternet() {
        return getBooleanPref("pref_consent_internet", false);
    }

    /**
     * Preferences for use of Google Maps
     */
    public static boolean getConsentGoogleMaps() {
        return getBooleanPref("pref_consent_google_maps", false);
    }

    public static void consentGoogleMaps(boolean inValue) {
        if (_settingsFragment == null)
            setBooleanPref("pref_consent_google_maps", inValue);
        else
            _settingsFragment.pref_consent_google_maps.setChecked(inValue);
    }

    /**
     * Preferences for writing Debug infos to Download dir
     */
    public static boolean getConsentDebug() {
        return sharedPref.getBoolean("pref_debug", false);
    }

    /**
     * Shared Preferences to restore application after shut down
     */
    public static int getProfileViewVisibility() {
        int value = sharedPref.getInt("ProfileViewVisibility", View.VISIBLE);
        if (value == View.GONE) value = View.VISIBLE;
        return value;
    }

    public static void setProfileViewVisibility(int inValue) {
        setIntPref("ProfileViewVisibility", inValue);
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Private methods
     * --------------------------------------------------------------------------------------------
     */

    /**
     * update preferences for hiking times calculation if they were changed
     */
    static private boolean updatePreferences() {
        if (hikingParametersChanged) {
            getPreferences();
            hikingParametersChanged = false;
            App.app.recalculate();
            App.app.Update();
            return true;
        }
        if (!getConsentDebug())
            Log.clearHTML();

        return false;
    }

    /**
     * store a boolean value in the settings
     * @param inKey key name
     * @param inValue value
     */
    private static void setBooleanPref(String inKey, boolean inValue) {
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(inKey, inValue);
            editor.apply();
        }
    }

    private static int getIntFromPref(String inKey, int inDefault) {
        int value = inDefault;
        if (sharedPref != null) {
            try {
                value = Integer.parseInt(sharedPref.getString(inKey, ""));
            } catch (Exception ignored) {
            }
        }
        return value;
    }

    private static void setWritingEnabled (boolean isEnabled) {
        Log.setWritingEnabled (isEnabled, "Tour Navigator ");
    }

}
