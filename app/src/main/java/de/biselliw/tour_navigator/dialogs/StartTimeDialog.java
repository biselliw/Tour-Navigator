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
import android.widget.Button;
import android.widget.TimePicker;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.activities.SettingsActivity;
import de.biselliw.tour_navigator.helpers.Prefs;

/**
 * Class to set the start time of the tour
 * @author BiselliW
 */
public class StartTimeDialog extends Dialog {

    /**
     * Constructor
     *
     * @param context context
     */
    public StartTimeDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_starttime);

        int time = Prefs.getStartTime(context);

        /* load time picker with current start time */
        TimePicker timePicker = findViewById(R.id.timePickerStart);
        timePicker.setIs24HourView(true);
        timePicker.setMinute(time % 60);
        timePicker.setHour(time / 60);

        /* define OnClick event for changing the start time */
        Button buttonOkay = findViewById(R.id.bt_start_ok);
        buttonOkay.setOnClickListener(v -> {
            /* load current start time from time picker */
            TimePicker timePicker1 = findViewById(R.id.timePickerStart);

            int time1 = timePicker1.getHour() * 60 + timePicker1.getMinute();
            Prefs.setStartTime(((MainActivity)context), time1);

            ((MainActivity)context).notifyStartTimeChanged(time1);
            dismiss();
        });

        /* define OnClick event to cancel the dialog */
        Button cancelButton = findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(v -> dismiss());
    }
}
