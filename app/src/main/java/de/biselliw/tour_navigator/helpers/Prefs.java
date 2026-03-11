package de.biselliw.tour_navigator.helpers;
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
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import java.util.ArrayList;

import de.biselliw.tour_navigator.App;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.activities.SettingsActivity;
import de.biselliw.tour_navigator.data.TrackSegments;

import static de.biselliw.tour_navigator.ui.ControlElements.setAlarmPreference;

/**
 * central class to access preferences
 */
public class Prefs {

    /**
     * Preference for initial introduction into the app
     */
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private static boolean _isFirstTimeLaunch = true;

    /**
     * hiking parameters used for time calculation
     */
    public static ArrayList<Parameter> hikingParameters;

    /**
     * Preferences for use of Internet
     */
    public static final String PREF_CONSENT_INTERNET = "pref_consent_internet";
    private static boolean _consentInternet = false;

    /**
     * Preference for use of Google Maps
     */
    public static final String PREF_CONSENT_GOOGLE_MAPS = "pref_consent_google_maps";

    /**
     * Preference for use of Schwarzwaldverein Tourenportal (German only)
     */
    public static final String PREF_CONSENT_SWV = "pref_consent_swv";

    public static final String PREF_HIKING_PAR_ALARM = "pref_hiking_par_alarm";

    /**
     * Preference for debugging (in DEBUG variant only)
     */
    public static final String PREF_DEBUG = "pref_debug";

    /**
     * Get SharedPreferences instance
     * @param inContext context
     * @return SharedPreferences instance
     */
    public static SharedPreferences get(Context inContext) {
        return inContext.getSharedPreferences(
                inContext.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    public static SharedPreferences getDefault() {
        return App.getContext().getSharedPreferences(
                App.getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    /**
     * Get preferences
     * @param inPref handle to preferences
     */
    public static void getPreferences(SharedPreferences inPref)
    {
        if (inPref != null) {
            _isFirstTimeLaunch = inPref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
            _consentInternet = inPref.getBoolean(PREF_CONSENT_INTERNET, false);
            SettingsActivity.consentGoogleMaps(inPref.getBoolean(PREF_CONSENT_GOOGLE_MAPS, false));
            SettingsActivity.consentSwvTourenportal(inPref.getBoolean(PREF_CONSENT_SWV, false));
            setAlarmPreference(inPref.getBoolean(PREF_HIKING_PAR_ALARM, false));

            if (BuildConfig.DEBUG)
                Log.setWritingEnabled (inPref.getBoolean(PREF_DEBUG, false), "Tour Navigator ");
        }
    }

    /**
     * Initial introduction into the app
     * @return true if app has not been invoked before
     */
    public static boolean isFirstTimeLaunch() {
        return _isFirstTimeLaunch;
    }

    public static void setFirstTimeLaunch(Context inContext, boolean isFirstTime) {
        _isFirstTimeLaunch = isFirstTime;
        get(inContext).edit().putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime).apply();
    }

    /**
     * class for hiking parameters used for time calculation
     */
    public static class Parameter {
        /** key for preference setting */
        public final String key;
        /** minimum, maximum, default values [m/h] */
        public final int minValue, mavValue, defaultValue;

        public Parameter (String key, int minValue, int mavValue, int defaultValue) {
            this.key = key;
            this.minValue = minValue;
            this.mavValue = mavValue;
            this.defaultValue = defaultValue;
        }
    }

    /**
     * Define hiking parameters for time calculation
     */
    public static void defineHikingParameters () {
        hikingParameters = new ArrayList<>();
        hikingParameters.add(new Parameter("pref_hiking_par_horSpeed", 1000, 10000, 4200));
        hikingParameters.add(new Parameter("pref_hiking_par_speedClimb", 100, 1000, 350));
        hikingParameters.add(new Parameter("pref_hiking_par_speedDescent", 100, 2000, 500));
    }

    /**
     * Set default values for hiking parameters on first time launch
     * @param inPref handle to preferences
     */
    public static void setDefaultHikingParameters (SharedPreferences inPref) {
        if (inPref != null && hikingParameters != null) {
            String def = inPref.getString(hikingParameters.get(0).key, "");
            if (def.isEmpty()) {
                // set default preferences for hiking times calculation
                SharedPreferences.Editor editor = inPref.edit();
                for (int i = 0; i < hikingParameters.size(); i++) {
                    editor.putString(hikingParameters.get(i).key,
                            String.valueOf(hikingParameters.get(i).defaultValue));
                }
                editor.apply();
            }
        }
    }

    /**
     * get preferences for hiking times calculation
     * @param inSegments target for parameters
     */
    public static void getHikingParameters(TrackSegments inSegments)
    {
        SharedPreferences pref = Prefs.getDefault();
        if (hikingParameters != null && pref != null) {
            /* hiking speed parameters */
            // horizontal part in [km/h]
            int horSpeed = getIntFromPref(pref, hikingParameters.get(0).key, hikingParameters.get(0).defaultValue);
            // ascending part in [km/h]
            int vertSpeedClimb = getIntFromPref(pref, hikingParameters.get(1).key, hikingParameters.get(1).defaultValue);
            // descending part in [km/h]
            int vertSpeedDescent = getIntFromPref(pref, hikingParameters.get(2).key, hikingParameters.get(2).defaultValue);

            // set hiking parameters
            inSegments.setHikingParameters(horSpeed / 1000.0, vertSpeedClimb / 1000.0,
                    vertSpeedDescent / 1000.0);
        }
    }

    /**
     * Get the value of a preference
     * @param inPref handle to preferences
     * @param inKey key for preference
     * @param inDefault default value
     * @return value of the preference
     */
    private static int getIntFromPref(SharedPreferences inPref, String inKey, int inDefault) {
        int value = inDefault;
        if (inPref != null) {
            try {
                value = Integer.parseInt(inPref.getString(inKey, ""));
            } catch (Exception ignored) {
            }
        }
        return value;
    }

    /**
     * Get the persisted start time of the tour
     * @return start time in [min] since midnight
     */
    public static int getStartTime(Context inContext) {
        return get(inContext).getInt("StartTime", -1);
    }

    /**
     * Persist the start time of the tour
     * @param inValue start time in [min] since midnight
     */
    public static void setStartTime(Context inContext, int inValue) {
        get(inContext).edit().putInt("StartTime", inValue).apply();
    }

    /**
     * Preferences for use of Internet
     */
    public static boolean getConsentInternet() {
        return _consentInternet;
    }

    /**
     * Preferences for use of Google Maps
     */
    public static boolean getConsentGoogleMaps() {
        return SettingsActivity.getConsentGoogleMaps();
    }

    /**
     * Preferences for use of Schwarzwaldverein Tourenportal
     */
    public static boolean getConsentSwvTourenportal() {
        return SettingsActivity.getConsentSwvTourenportal();
    }

    /**
     * Shared Preferences to restore application after shut down
     */
    public static int getProfileViewVisibility(Context inContext) {
        return get(inContext).getInt("ProfileViewVisibility", View.VISIBLE);
    }

    public static void setProfileViewVisibility(Context inContext, int inValue) {
        get(inContext).edit().putInt("ProfileViewVisibility", inValue).apply();
    }

    /**
     * Preferences for writing Debug infos to Download dir
     */
    public static boolean getConsentDebug(SharedPreferences sharedPref) {
        return sharedPref.getBoolean(PREF_DEBUG, false);
    }

}