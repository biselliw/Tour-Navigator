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

import android.os.Bundle;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.helper.WebViewActivity;

/**
 * Show help file in a WebView
 */
public class HelpActivity extends WebViewActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* load HTML help file into WebView */
        String file = "help";
        String lang = this.getString(R.string.lang);

        if (lang.equals("de"))
            file = file + "-" + lang;
        loadUrl("file:///android_asset/" + file + ".html");
    }
}