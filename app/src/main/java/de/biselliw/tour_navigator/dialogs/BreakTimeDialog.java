package de.biselliw.tour_navigator.dialogs;

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
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;

import de.biselliw.tour_navigator.App;

/*
 This file handles the break time of a single waypoint
 */
public class BreakTimeDialog extends FullScreenDialog {
    public RecordAdapter _recordAdapter;
    public int selected;
    public RecordAdapter.Record record;
    public int waypointDuration;
    public App _app;

    public BreakTimeDialog(Context context, RecordAdapter recordAdapter, App app) {
        super(context, R.layout.dialog_break_time);

        /* load time picker with current start time */
        _recordAdapter = recordAdapter;
        _app = app;
        selected = _recordAdapter.getPlace();
        if (selected > 0) {
            record = _recordAdapter.recordList.get(selected);
            waypointDuration = record.getTrackPoint().getWaypointDuration();
            TimePicker timePicker = (TimePicker) findViewById(R.id.timePickerBreak);
            timePicker.setIs24HourView(true);
            int hour = waypointDuration/60;
            int minute = waypointDuration - hour*60;
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);

            /* define OnClick event for changing the start time */
            Button buttonOkay = (Button) findViewById(R.id.bt_Break_ok);
            buttonOkay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /* load current start time from time picker */
                    TimePicker timePicker = (TimePicker) findViewById(R.id.timePickerBreak);
                    int hour = timePicker.getCurrentHour();
                    // prohibit negative times
                    if (hour > 12) hour = 0;
                    int minute =timePicker.getCurrentMinute();
                    int break_min = hour*60 + minute;
                    record.getTrackPoint().setWaypointDuration(break_min);

                    /* update break time of current waypoint */
                    _recordAdapter.recordList.set(selected, record);
                    _app.updateRecords();
                    _recordAdapter.notifyDataSetChanged();

                    /* update time stamps of remaining waypoints */
//                    _app.Update();
                    dismiss();
                }
            });
            /* define OnClick event to cancel the dialog */
            Button cancelButton = findViewById(R.id.btn_cancel);
            cancelButton.setOnClickListener(v -> {
                dismiss();
            });
        }
        dismiss();
   }
}
