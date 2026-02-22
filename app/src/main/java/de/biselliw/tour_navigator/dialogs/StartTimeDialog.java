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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.MainActivity;

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

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int time = 0;
        if (sharedPref != null) {
            time = sharedPref.getInt("StartTime", 0);
        }

        /* load time picker with current start time */
        TimePicker timePicker = findViewById(R.id.timePickerStart);
        timePicker.setIs24HourView(true);
        timePicker.setMinute(time % 60);
        timePicker.setHour(time / 60);

        /* define OnClick event for changing the start time */
        Button buttonOkay = findViewById(R.id.bt_start_ok);
        buttonOkay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* load current start time from time picker */
                TimePicker timePicker = findViewById(R.id.timePickerStart);

                int time = timePicker.getHour() * 60 + timePicker.getMinute();

                if (sharedPref != null) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("StartTime", time);
                    editor.apply(); // editor.commit();
                }

                ((MainActivity)context).notifyStartTimeChanged(time);
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
