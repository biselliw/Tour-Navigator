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
    If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;

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
import de.biselliw.tour_navigator.dialogs.AcceptGoogleMapsPolicyDialog;
import de.biselliw.tour_navigator.dialogs.AcceptSwvTourenportalPolicyDialog;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.helpers.Prefs;

import static de.biselliw.tour_navigator.helpers.Prefs.PREF_CONSENT_GOOGLE_MAPS;
import static de.biselliw.tour_navigator.helpers.Prefs.PREF_CONSENT_INTERNET;
import static de.biselliw.tour_navigator.helpers.Prefs.PREF_CONSENT_SWV;
import static de.biselliw.tour_navigator.helpers.Prefs.PREF_DEBUG;
import static de.biselliw.tour_navigator.helpers.Prefs.hikingParameters;

/**
 * Application settings
 * @see <a href="https://developer.android.com/reference/androidx/preference/package-summary">AndroidX Preference Library</a>
 */
public class SettingsActivity extends BaseActivity {
    // SettingsActivity
    // └─ activity_settings.xml
    //     └─ FrameLayout (settings_container)
    //         └─ SettingsFragment (PreferenceFragmentCompat)
    //             └─ Prefs, preferences.xml

    private SettingsFragment _settingsFragment = null;

    /**
     * hiking parameters used for walking time calculation
     */
    static boolean hikingParametersChanged = false;

    static String lang = "";

    /**
     * Preferences for use of Google Maps
     */
    private static boolean _consentGoogleMaps = false;
    private static boolean _updateGooglePrefs = false;

    /**
     * Preferences for use of Schwarzwaldverein Tourenportal
     */
    private static boolean _consentSwvTourenportal = false;
    private static boolean _updateSwvTourenportal = false;


    public static class SettingsFragment extends PreferenceFragmentCompat {
        SwitchPreferenceCompat prefConsentGoogleMaps = null;
        SwitchPreferenceCompat prefConsentSwv = null;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

            setupInternetPreferences(prefs);

            for (Prefs.Parameter parameter : hikingParameters) {
                setupNumericPreference(parameter.key, parameter.minValue, parameter.mavValue,
                        parameter.defaultValue);
            }

            setupDebugPreference();
        }

        private void setupInternetPreferences(SharedPreferences prefs) {

            SwitchPreferenceCompat prefInternet = findPreference(PREF_CONSENT_INTERNET);
            prefConsentGoogleMaps = findPreference(PREF_CONSENT_GOOGLE_MAPS);
            prefConsentSwv = findPreference(PREF_CONSENT_SWV);

            if (prefInternet == null)
                return;
            boolean enableInternet = prefInternet.isChecked();

            // Accept Google Maps Policy
            if (prefConsentGoogleMaps != null) {
                prefConsentGoogleMaps.setVisible(enableInternet);
                prefConsentGoogleMaps.setOnPreferenceChangeListener(
                        (preference,newValue)->{
                            boolean value = (Boolean)newValue;
                            if(value){
                                new AcceptGoogleMapsPolicyDialog((AppCompatActivity) requireContext()).show();
                                // refuse the new value - must be confirmed in dialog
                                return false;
                            }
                            return true;
                        });
            }

            // Accept Schwarzwaldverein Tourenportal Policy
            if (prefConsentSwv != null) {
                // Das Schwarzwaldverein Tourenportal gibt es nur in deutscher Sprache!
                prefConsentSwv.setVisible(enableInternet && lang.equals("de"));
                prefConsentSwv.setOnPreferenceChangeListener(
                        (preference,newValue)->{
                            boolean value = (Boolean)newValue;
                            if(value){
                                new AcceptSwvTourenportalPolicyDialog((AppCompatActivity) requireContext()).show();
                                // refuse the new value - must be confirmed in dialog
                                return false;
                            }
                            return true;
                        });
            }

            prefInternet.setOnPreferenceChangeListener(
                    (preference,newValue)->{
                        boolean value = (Boolean)newValue;

                        if(prefConsentGoogleMaps!=null){
                            prefConsentGoogleMaps.setVisible(value);
                            if(!value)
                                prefConsentGoogleMaps.setChecked(false);
                        }

                        if(prefConsentSwv!=null){
                            prefConsentSwv.setVisible(value);
                            if(!value)
                                prefConsentSwv.setChecked(false);
                        }
                        return true;
                    });
        }

        private void setupNumericPreference(
                String key,
                int min,
                int max,
                int defaultValue) {

            EditTextPreference pref = findPreference(key);

            if(pref==null)
                return;

            pref.setOnBindEditTextListener(editText->{

                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setSingleLine(true);
            });

            pref.setOnPreferenceChangeListener(
                    (preference,newValue)->{
                        int value;

                        try{
                            hikingParametersChanged = true;
                            value=Integer.parseInt((String)newValue);
                        }
                        catch(Exception e){
                            return false;
                        }

                        if(value<min || value>max){
                            pref.setText(String.valueOf(defaultValue));
                            return false;
                        }
                        return true;
                    });

            pref.setSummaryProvider(preference->{
                String text = ((EditTextPreference)preference).getText();

                if(text==null)
                    return "";

                try{
                    int value=Integer.parseInt(text);
                    if(value==defaultValue)
                        return "Default: "+defaultValue;
                }catch(Exception ignored){}

                return text;
            });
        }

        private void setupDebugPreference() {
            SwitchPreferenceCompat debugPref = findPreference(PREF_DEBUG);
            if (debugPref != null)
                debugPref.setVisible(BuildConfig.DEBUG);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            _settingsFragment = new SettingsFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, _settingsFragment)
                    .commit();
        }

        lang = getString(R.string.lang);
    }

    public void onStart() {
        super.onStart();
        startTimer(100);
    }

    /**
     * callback function to periodically update the user interface
     */
    @Override
    protected void updateUI() {
        if (_updateGooglePrefs) {
            _updateGooglePrefs = false;
            if (_settingsFragment != null)
                _settingsFragment.prefConsentGoogleMaps.setChecked(_consentGoogleMaps);
        }
        if (_updateSwvTourenportal) {
            _updateSwvTourenportal = false;
            if (_settingsFragment != null)
                _settingsFragment.prefConsentSwv.setChecked(_consentSwvTourenportal);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            if (_settingsFragment != null)
                updatePreferences(_settingsFragment.getPreferenceManager().getSharedPreferences());
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Public methods
     * --------------------------------------------------------------------------------------------
     */
    /**
     * Preferences for use of Google Maps
     */
    public static boolean getConsentGoogleMaps() {
        return _consentGoogleMaps;
    }

    public static void consentGoogleMaps(boolean inValue) {
        _consentGoogleMaps = inValue;
        _updateGooglePrefs = true;
    }

    /**
     * Preferences for use of Schwarzwaldverein Tourenportal
     */
    public static boolean getConsentSwvTourenportal() {
        return _consentSwvTourenportal;
    }
    public static void consentSwvTourenportal(boolean inValue) {
        _consentSwvTourenportal = inValue;
        _updateSwvTourenportal = true;
    }

    /*
     * --------------------------------------------------------------------------------------------
     * Private methods
     * --------------------------------------------------------------------------------------------
     */

    /**
     * update preferences for hiking times calculation if they were changed
     */
    static private void updatePreferences(SharedPreferences inPref) {
        Prefs.getPreferences(inPref);
        if (hikingParametersChanged) {
            hikingParametersChanged = false;
            // FIXME create intent
            App.app.Update();
        }
        if (BuildConfig.DEBUG)
            if (!Prefs.getConsentDebug(inPref))
                Log.clearDebugHTML();
    }
}
