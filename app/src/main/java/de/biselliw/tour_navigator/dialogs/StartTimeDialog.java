package de.biselliw.tour_navigator.dialogs;

import android.content.Context;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.adapter.RecordAdapter;
import de.biselliw.tour_navigator.activities.LocationActivity;

/**
 * Class to set the start time of the tour
 * @author BiselliW
 */
public class StartTimeDialog extends FullScreenDialog {
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
        super(context, R.layout.starttime_dialog);

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

                _recordAdapter.setStartTime(time);
                _recordAdapter.notifyDataSetChanged();

                ((LocationActivity)_context).notifyStartTimeChanged();
                dismiss();
            }
        });
    }
}
