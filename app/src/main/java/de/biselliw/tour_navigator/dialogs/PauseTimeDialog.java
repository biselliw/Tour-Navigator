/*
 This file handles the pause time of a single waypoint
 */
package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;

import de.biselliw.tour_navigator.App;

public class PauseTimeDialog extends FullScreenDialog {
    public RecordAdapter _recordAdapter;
    public int selected;
    public RecordAdapter.Record record;
    public int waypointDuration;
    public App _app;

    public PauseTimeDialog(Context context, RecordAdapter recordAdapter, App app) {
        super(context, R.layout.pausetime_dialog);

        /* load time picker with current start time */
        _recordAdapter = recordAdapter;
        _app = app;
        selected = _recordAdapter.getPlace();
        if (selected > 0) {
            record = _recordAdapter.recordList.get(selected);
            waypointDuration = record.getTrackPoint().getWaypointDuration();
            TimePicker timePicker = (TimePicker) findViewById(R.id.timePickerPause);
            timePicker.setIs24HourView(true);
            int hour = waypointDuration/60;
            int minute = waypointDuration - hour*60;
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);

            /* define OnClick event for changing the start time */
            Button buttonOkay = (Button) findViewById(R.id.bt_Pause_ok);
            buttonOkay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /* load current start time from time picker */
                    TimePicker timePicker = (TimePicker) findViewById(R.id.timePickerPause);
                    int hour = timePicker.getCurrentHour();
                    int minute =timePicker.getCurrentMinute();
                    int pause_min = hour*60 + minute;
                   record.getTrackPoint().setWaypointDuration(pause_min);

                    /* update pause time of current waypoint */
                    _recordAdapter.recordList.set(selected, record);

                    /* update time stamps of remaining waypoints */
                    _app.Update();
                    dismiss();
                }
            });
        }
        dismiss();
   }
}
