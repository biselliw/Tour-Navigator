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

    Copyright 2024 Walter Biselli (BiselliW)
*/

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.helper.BaseActivity;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import static de.biselliw.tour_navigator.ui.ControlElements.control;

public class CommentActivity extends BaseActivity {

    DataPoint dataPoint = null;
    EditText edit = null;
    int selected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        ActionBar ab = getSupportActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        overridePendingTransition(0, 0);

        RecordAdapter recordAdapter = control.recordAdapter;
        selected = recordAdapter.getPlace();
        if (selected >= 0) {
            RecordAdapter.Record record = recordAdapter.getItem(selected);
            if (record != null) {
                dataPoint = record.getTrackPoint();
                TextView view = findViewById(R.id.tvComment);
                view.setText(dataPoint.getRoutePointName());

                edit = findViewById(R.id.etComment);
                edit.setText(dataPoint.getComment());
 //               TourDetails.AdditionalInfo info = details.getAdditionalInfo(selected);
//                edit.setText(info.description);

                recordAdapter.notifyDataSetChanged();
            }
        }

        Button btnTake = findViewById(R.id.btComment);
        btnTake.setOnClickListener(view -> {
            dataPoint.setFieldValue(Field.COMMENT, edit.getText().toString(), false); // setWaypointComment(edit.getText().toString());
            control.showAdditionalInfo(selected);
            control.updateGpxFile = true;
            finish();
        });

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

}