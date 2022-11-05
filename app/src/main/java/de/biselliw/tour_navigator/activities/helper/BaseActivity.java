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

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import de.biselliw.tour_navigator.R;
// import de.biselliw.tour_navigator.databinding.ActivityMainBinding;


public class BaseActivity extends AppCompatActivity {

    public final int NAVDRAWER_LAUNCH_DELAY = 250;
    public final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    public static final int MAIN_CONTENT_FADEIN_DURATION = 250;

    protected Toolbar mToolbar;

    // Navigation drawer:
    protected DrawerLayout mDrawerLayout;
    protected NavigationView mNavigationView;
    protected Handler mHandler;

//    protected ActivityMainBinding binding;

    private boolean use_databinding = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }

    // protected abstract int getNavigationDrawerID();

    @Override
    public void onResume() {
        super.onResume();

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        View mainContent;
/*
        if (use_databinding)
        {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            mToolbar = binding.layoutToolbar.toolbar;
            if (getSupportActionBar() == null)
                setSupportActionBar(mToolbar);

            mDrawerLayout = binding.drawerLayout;
            mainContent = binding.mainContent.mainContent;
            mNavigationView = binding.navView;
        }
        else
 */
        {
            mToolbar = findViewById(R.id.toolbar);
            if (getSupportActionBar() == null)
                setSupportActionBar(mToolbar);
            mDrawerLayout = findViewById(R.id.drawer_layout);
            mainContent = findViewById(R.id.main_content);
            mNavigationView = findViewById(R.id.nav_view);
        }

        if (mDrawerLayout != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            mDrawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }


        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        }
    }
}
