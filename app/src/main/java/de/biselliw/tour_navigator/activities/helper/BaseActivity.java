/*
 * Copyright (C) 2022 Walter Biselli
 *
 * Hiking Navigator App (the "Software") is free software:
 *
 * Licensed under the GNU General Public License along with this (the "License")
 * either version 3 of the License, or any later version.
 * GNU General Public License is published by the Free Software Foundation,
 *
 * The Software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * You can redistribute the Software and/or modify it under the terms of the License
 *
 * See the GNU General Public License for more details.
 * You should have received a copy of the License along with the Software.
 * If not, you may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.biselliw.tour_navigator.activities.helper;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.helpers.Log;

public class BaseActivity extends AppCompatActivity {

    /**
     * TAG for log messages.
     */
    static final String TAG = "BaseActivity";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    public final int NAVDRAWER_LAUNCH_DELAY = 250;
    public final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    public static final int MAIN_CONTENT_FADEIN_DURATION = 250;

    protected MaterialToolbar mToolbar;

    // Navigation drawer:
    protected DrawerLayout mDrawerLayout;
    protected NavigationView mNavigationView;
    protected Handler mHandler;

    protected boolean isDark = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        if (_DEBUG) {
            // query night mode
            int nightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    @Override
    /*
     * Override the standard behavior when Back button is pressed
     * @see https://developer.android.com/guide/components/activities/tasks-and-back-stack
     */
    public void onBackPressed() {
        if (mDrawerLayout != null)
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
                return;
            }
        super.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // first: check for toolbar without drawer
        MaterialToolbar mToolbar_min = findViewById(R.id.toolbar_min);
        if (mToolbar_min != null) {
            mToolbar = mToolbar_min;
        }
        else {
            // alternative: check for toolbar with drawer
            mToolbar = findViewById(R.id.toolbar);
        }
        if (getSupportActionBar() == null)
            // show back arrow in the toolbar
            setSupportActionBar(mToolbar);

        if (mToolbar_min != null) {
            // show back arrow in the toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
        else {
            mDrawerLayout = findViewById(R.id.drawer_layout);
            if (mDrawerLayout != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                mDrawerLayout.addDrawerListener(toggle);
                toggle.syncState();
            }
        }

        View mainContent = findViewById(R.id.main_content);
        mNavigationView = findViewById(R.id.nav_view);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        overridePendingTransition(0, 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (DEBUG) {
            String resultCodeStr = resultCode == RESULT_OK ? " [OK]" : " [NOK]";
            String debugStr = "onActivityResult(requestCode=" + requestCode +
                    ", resultCode=" + resultCode + resultCodeStr +
                    ", data=";
            if (data != null)
                debugStr +=
                        " [Action= " + data.getAction() +
                                " ,Data=" + data.getData() +
                                ")";
            else
                debugStr += "none";
            debugStr += ")";

            Log.d(TAG, debugStr);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
