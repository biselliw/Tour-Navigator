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

import android.content.Intent;
import android.os.Bundle;

import de.biselliw.tour_navigator.activities.helper.WebViewActivity;

/**
 * Show the time table of the tour
 */
public class TimeTableActivity extends WebViewActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle a received intent
        Intent intent = getIntent();
        if (intent != null) {
            String html = intent.getStringExtra("contents");
            loadData(html);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}