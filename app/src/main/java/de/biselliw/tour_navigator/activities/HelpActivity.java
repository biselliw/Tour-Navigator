/*
 This file is based on Privacy Friendly App Example:
    https://github.com/SecUSo/privacy-friendly-app-example

 Privacy Friendly App Example is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly App Example is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly App Example. If not, see <http://www.gnu.org/licenses/>.
 */

package de.biselliw.tour_navigator.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.HelpExpandableListAdapter;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;

/**
 * @author Karola Marky, Christopher Beckmann
 * @version 20171016
 * Class structure taken from tutorial at http://www.journaldev.com/9942/android-expandablelistview-example-tutorial
 * last access 27th October 2016
 */
public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        LinkedHashMap<String, List<String>> expandableListDetail = buildData();
        ExpandableListView generalExpandableListView = findViewById(R.id.generalExpandableListView);
        generalExpandableListView.setAdapter(new HelpExpandableListAdapter(this, new ArrayList<>(expandableListDetail.keySet()), expandableListDetail));

        overridePendingTransition(0, 0);
    }

    private LinkedHashMap<String, List<String>> buildData() {
        LinkedHashMap<String, List<String>> expandableListDetail = new LinkedHashMap<String, List<String>>();

        List<String> general = new ArrayList<String>();
        general.add(getString(R.string.help_overview_answer));

        expandableListDetail.put(getString(R.string.help_overview_heading), general);

        List<String> requirements = new ArrayList<String>();
        requirements.add(getString(R.string.help_requirements_answer1));
        requirements.add(getString(R.string.help_requirements_answer2));
        requirements.add(getString(R.string.help_requirements_answer3));

        expandableListDetail.put(getString(R.string.help_requirements_heading), requirements);

        List<String> time_calc = new ArrayList<String>();
        time_calc.add(getString(R.string.help_time_calc_answer1));
        time_calc.add(getString(R.string.help_time_calc_answer2));
        time_calc.add(getString(R.string.help_time_calc_answer3));

        expandableListDetail.put(getString(R.string.help_time_calc_heading), time_calc);
        
        expandableListDetail.put(getString(R.string.help_privacy), Collections.singletonList(getString(R.string.help_privacy_answer)));

        List<String> permission = new ArrayList<String>();
        permission.add(getString(R.string.help_permission_answer1));
        permission.add(getString(R.string.help_permission_answer2));
        permission.add(getString(R.string.help_permission_answer3));

        expandableListDetail.put(getString(R.string.help_permission_heading), permission);
        
        return expandableListDetail;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}