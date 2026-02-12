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

import android.app.Dialog;
import android.content.Context;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.adapter.RecordAdapter;

/**
 * Class to set the start time of the tour
 * @author BiselliW
 */
public class StartTimeDialog extends Dialog {
    public Time time;
    RecordAdapter _recordAdapter;
    Context _context;

    /**
     * Constructor
     *
     * @param context context
     * @param recordAdapter adapter for the timetable records
     */
    public StartTimeDialog(Context context, RecordAdapter recordAdapter) {
        super(context);
        setContentView(R.layout.dialog_starttime);

        /* load time picker with current start time */
        _recordAdapter = recordAdapter;
        _context = context;

        time = recordAdapter.getStartTime();
        TimePicker timePicker = (TimePicker) findViewById(R.id.timePickerStart);
        timePicker.setIs24HourView(true);
        timePicker.setHour(time.hour);
        timePicker.setMinute(time.minute);

        /* define OnClick event for changing the start time */
        Button buttonOkay = (Button) findViewById(R.id.bt_start_ok);
        buttonOkay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* load current start time from time picker */
                TimePicker timePicker = (TimePicker) findViewById(R.id.timePickerStart);

                time.setToNow();
                time.hour =  timePicker.getHour();
                time.minute =timePicker.getMinute();

                ((MainActivity)_context).notifyStartTimeChanged(time);
                dismiss();
            }
        });
        /* define OnClick event to cancel the dialog */
        Button cancelButton = findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(v -> {
            dismiss();
        });
    }
}
