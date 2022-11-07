package de.biselliw.tour_navigator.activities.adapter;

import android.app.Activity;
import android.content.Context;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.data.DataPoint;

import static de.biselliw.tour_navigator.helpers.TimezoneHelper.getSelectedTimezone;
import static de.biselliw.tour_navigator.helpers.TimezoneHelper.getSelectedTimezoneStr;

/**
 * class to handle all records of the timetable
 * @author BiselliW
 */
public class RecordAdapter extends BaseAdapter {

    /**
     * TAG for log messages.
     */
    static final String TAG = "RecordAdapter";
    private static final boolean DEBUG = false; // Set to true to enable logging

    private static final int COLOR_NOBE       = 0xAA000000;
    private static final int COLOR_DELAY_NONE = 0xAA0000ff;
    private static final int COLOR_DELAY_MIN  = 0xAA008000;
    private static final int COLOR_DELAY_MAX  = 0xAAB71C1C;

    Context recordContext;
    public List<Record> recordList;
    private final Calendar calender;
    private int _selected = -1;
    /** start time of the tour */
    private final Time _startTime;
    private boolean _realtime = false;
    /**  current distance since start */
    private double _distance = 0.0;
    /** current delay [min] */
    private int _delay_min = 0;
    final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    final DecimalFormat decFormat = new DecimalFormat("  #0.0");

    private static class RecordViewHolder {
        public TextView timeView;
        public TextView delayView;
        public TextView distanceView;
        public TextView placeView;
    }

    /**
     * Constructor
     * @param context context
     * @param records list of all records
     */
    public RecordAdapter(Context context, List<Record> records) {
        recordList = records;
        recordContext = context;
        calender = Calendar.getInstance();
        // todo check timezone
        calender.setTimeZone(getSelectedTimezone());

        _startTime = new Time(getSelectedTimezoneStr());
        _startTime.setToNow();
        _startTime.hour =  0;
        _startTime.minute =0;
    }

    public static class Record {
        public int trackPointIndex;
        public DataPoint trackPoint;
        public double Sdistance;
        public int Sclimb;
        public int Sdescent;
        public long Sseconds;

        public Record (DataPoint _trackPoint, int _trackPointIndex, double _Sdistance, int _Sclimb, int _Sdescent, long _Sseconds)
        {
            trackPointIndex = _trackPointIndex;
            trackPoint = _trackPoint;
            Sdistance  = _Sdistance;
            Sclimb     = _Sclimb;
            Sdescent   = _Sdescent;
            Sseconds   = _Sseconds;
        }

        public DataPoint getTrackPoint() { return trackPoint;}
    }

    @Override
    public int getCount() {
        return recordList.size();
    }

    @Override
    public Record getItem(int i) {
        if (i < getCount()) {
            return recordList.get(i);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


    /**
     * update the list view of places
     *
     * @param i         index of the place
     * @param view      returned view
     * @param viewGroup not used
     * @return          view
     */
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (DEBUG) Log.d(TAG, "getView (" + i +")");
        RecordViewHolder holder;
        if (view == null) {
            LayoutInflater recordInflater = (LayoutInflater) recordContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = recordInflater.inflate(R.layout.record, null);
            holder = new RecordViewHolder();
            holder.timeView = view.findViewById(R.id.record_time);
            holder.delayView = view.findViewById(R.id.record_delay);
            holder.distanceView = view.findViewById(R.id.record_distance);
            holder.placeView = view.findViewById(R.id.record_place);
            view.setTag(holder);
        } else {
            holder = (RecordViewHolder) view.getTag();
        }

        Record record = getItem(i);
        if (record != null)
        {
            DataPoint point = record.trackPoint;

            /* Mark selected row */
            if (i == _selected)
                view.setBackgroundColor(0xFFB3DBFB);
            else
                view.setBackgroundColor(0xFFFFFFFF);

            /* get the formatted arrival time */
            String time = getPlannedArriveTime(point);
            if (point.getWaypointDuration() > 0) {
                /* add end of pause time */
                calender.add(Calendar.MINUTE, point.getWaypointDuration());
                time = time + " " + timeFormat.format(calender.getTime());
            }
            holder.timeView.setText(time);

            /* show the presumable arrival time depending on the delay */
            showPresumableArriveTime(false, false, holder.delayView, point);

            /* show distance since start */
            holder.distanceView.setText(decFormat.format(point.getDistance()));

            /* show waypoint name */
            holder.placeView.setText(point.getRoutePointName());

            if (DEBUG) Log.d(TAG, "  WaypointName: " + point.getRoutePointName());
        }
        else
            if (DEBUG) Log.d(TAG, "  FATAL: record not available");

        return view;
    }

    /**
     * Remove all records
     */
    public void RemoveRecords()
    {
        while (getCount() > 0)
        {
            recordList.remove(0);
        }
    }

    /**
     * @return start time of the tour
     */
    public Time getStartTime() {
        return _startTime;
    }

    /**
     * Set the start time of the tour
     * @param inTime start time of the tour
     */
    public void setStartTime(Time inTime) {
        _startTime.set(inTime);
    }

    /**
     * Get the selected item in the list of places
     *
     * @return Index (starting at 0) / -1 if nothing is selected
     */
    public int getPlace() {
        return _selected;
    }

    /**
     * Get the selected item within the track
     *
     * @return Index of the track point within the track (starting at 0) / -1 if nothing is selected
     */
    public int getItemIndex() {
        if (_selected >= 0)
        {
            Record record = getItem(_selected);
            return record.trackPointIndex;
        }
        else
            return -1;
    }

    /**
     * Sets an item in the list of places
     *
     * @param inPlace Index (starting at 0) of the data item to be selected or -1 if nothing
     * @see de.biselliw.tour_navigator.activities.LocationActivity#showPlace(int)
     */
    public void setPlace(int inPlace) {
        _selected = Math.min(inPlace, recordList.size());
        notifyDataSetChanged();
    }

    /**
     * Set the current distance since start
     * @param inDistance inDistance
     */
    public void setDistance (double inDistance) {
        _distance = inDistance;
    }

    /**
     * Set the current delay
     * @param inDelay [min]
     */
    public void setDelay(int inDelay) {
        _delay_min = inDelay;
    }
    /**
     * Get the current delay
     * @return current delay [min]
     */
    public int getDelay() {
        return _delay_min;
    }

    public void setRealtime (boolean inRealtime)
    {
        _realtime = inRealtime;
    }

    /**
     * get the formatted planned arrival time
     *
     * @param inPoint waypoint data
     * @return formatted arrival time
     */
    public String getPlannedArriveTime(DataPoint inPoint) {
        long _t = _startTime.toMillis(true) + inPoint.getTime() * 1000L;
        calender.setTimeInMillis(_t);
        return timeFormat.format(calender.getTime());
    }

    /**
     * Show the presumable arrival time depending on the delay
     *
     * @param inShowDelay if true: show the planned time plus delay
     * @param inShowPlanned show planned time if presumable is not available
     * @param inView TextView for display
     * @param inPoint point data
     * */
    public void showPresumableArriveTime(boolean inShowDelay, boolean inShowPlanned, TextView inView, DataPoint inPoint) {
        String sTime = "";
        long _t = _startTime.toMillis(true);
        if (_realtime && (_t > 0) && (inPoint.getDistance() >= _distance)) {
            _t += inPoint.getTime() * 1000;
            calender.setTimeInMillis(_t);
            if (inShowDelay) {
                // get the formatted planned arrival time
                sTime = getPlannedArriveTime(inPoint);

                calender.set(0, 0, 0, 0, 0);
                if (_delay_min > 0) {
                    calender.add(Calendar.MINUTE, _delay_min);
                    String sDelay = timeFormat.format(calender.getTime());
                    sTime = sTime + " +" + sDelay;
                } else if (_delay_min < 0) {
                    calender.add(Calendar.MINUTE, -_delay_min);
                    String sDelay = timeFormat.format(calender.getTime());
                    sTime = sTime + " -" + sDelay;
                }
            }
            else
            {
                calender.add(Calendar.MINUTE, _delay_min);
                sTime = timeFormat.format(calender.getTime());
            }

            inView.setTextColor(getDelayColor());
        }
        else {
            /* don't show the presumable arrival time */
            inView.setTextColor(COLOR_NOBE);
            if (inShowPlanned)
            {
                /* get the formatted planned arrival time */
                sTime = getPlannedArriveTime(inPoint);
            }
        }

        inView.setText(sTime);
    }

    public int getDelayColor() {
        if (_delay_min >= 8) {
            return COLOR_DELAY_MAX;
        } else if (_delay_min > 0) {
            return COLOR_DELAY_MIN;
        } else {
            return COLOR_DELAY_NONE;
        }
    }

    /**
     * add a record without updating the list view
     * @param record record
     */
    public void add(Record record) {
        recordList.add(record);
    }

    /*
     * Remove the current waypoint from the list - not from the track
     */
    public void remove() {
        if (_selected > 0) {
            /* Clear pause time in advance */
            Record record = getItem(_selected);
            if (record != null)
            {
                DataPoint dataPoint = record.getTrackPoint();
                dataPoint.setWaypointDuration(0);
            }

            recordList.remove(_selected);
            notifyDataSetChanged();
        }
    }


}

