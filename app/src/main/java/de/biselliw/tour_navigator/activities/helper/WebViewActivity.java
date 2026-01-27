package de.biselliw.tour_navigator.activities.helper;

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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.ContextCompat;
import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.helpers.Log;

/**
 * Show HTML contents in a WebView
 * @see <a href="https://developer.android.com/reference/android/content/res/AssetManager">AssetManager</a>
 */
public class WebViewActivity extends BaseActivity {
    /**
     * TAG for log messages.
     */
    static final String TAG = "WebViewActivity";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    private WebView _webView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    /**
     * Loads HTML contents
     * @param html HTML contents to load
     */
    protected void loadData(String html) {
        loadHTML(html, false);
    }

    /**
     * Loads the given URL.
     * @param url the URL of the resource to load
     */
    protected void loadUrl(String url) {
        loadHTML(url, true);
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void loadHTML(String contents, boolean isURL) {

        /* inject CSS into loaded HTML file */
        String background = String.format("%X", ContextCompat.getColor(this, R.color.colorBackground)).substring(2);
        String color = String.format("%X", ContextCompat.getColor(this, R.color.colorText)).substring(2);;

        String css = "body { background:#" + background + "; color:#" + color + "; }";
        String js = "var style = document.createElement('style');"
                + "style.innerHTML = '" + css + "';"
                + "document.head.appendChild(style);";

        _webView = findViewById(R.id.web_view);
        if (_webView != null) {
            WebSettings settings = _webView.getSettings();
            settings.setJavaScriptEnabled(true);
/*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                settings.setForceDark(WebSettings.FORCE_DARK_ON);
 */
            // load HTML file into WebView
            if (isURL)
                _webView.loadUrl(contents);
            else
                _webView.loadData(contents,"text/html","utf-8");

            _webView.setWebViewClient(new WebViewClient() {
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
        if (_webView != null)
            _webView.scrollTo(0,0);
    }
}