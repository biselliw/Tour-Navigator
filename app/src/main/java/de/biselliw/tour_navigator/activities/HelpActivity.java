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

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.ContextCompat;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.helpers.Log;

/**
 * Sho help file in a WebView
 * @see <a href="https://developer.android.com/reference/android/content/res/AssetManager">AssetManager</a>
 */

public class HelpActivity extends BaseActivity {
    /**
     * TAG for log messages.
     */
    static final String TAG = "HelpActivity";
    private static final boolean _DEBUG = false; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    WebView webView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        /* load HTML help file into WebView */
        String file = "help";
        String lang = this.getString(R.string.lang);

        if (lang.equals("de"))
            file = file + "-" + lang;

        if (_DEBUG) {
            // query night mode
            int nightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            boolean isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        }

        /* inject CSS into loaded HTML file */
        String background = String.format("%X", ContextCompat.getColor(this, R.color.colorBackground)).substring(2);
        String color = String.format("%X", ContextCompat.getColor(this, R.color.colorText)).substring(2);;

        String css = "body { background:#" + background + "; color:#" + color + "; }";
        String js = "var style = document.createElement('style');"
                + "style.innerHTML = '" + css + "';"
                + "document.head.appendChild(style);";

        webView = findViewById(R.id.help);
        if (webView != null) {
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
/*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                settings.setForceDark(WebSettings.FORCE_DARK_ON);
 */
            // load HTML file into WebView
            webView.loadUrl("file:///android_asset/" + file + ".html");

            webView.setWebViewClient(new WebViewClient() {
                /* finally inject CSS after HTML file has been loaded */
                @Override
                public void onPageFinished(WebView view, String url) {
                    view.evaluateJavascript(js,null);
                    if (_DEBUG)
                        // query HEAD of loaded HTML file
                        view.evaluateJavascript(
                            "(function(){return document.head.innerHTML;})();",
                            result -> Log.d("HEAD", result)
                    );
                }
            });
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (webView != null)
            webView.scrollTo(0,0);
    }

}