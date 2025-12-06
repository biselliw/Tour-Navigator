package de.biselliw.tour_navigator.activities.adapter;

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

    You should have received a copy of the GNU General Public License. If not, see
            <http://www.gnu.org/licenses/>.

    Copyright 2025 Walter Biselli (BiselliW)
*/

import android.app.Activity;
import android.content.Context;
import android.text.format.Time;
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
import de.biselliw.tour_navigator.activities.SettingsActivity;
import de.biselliw.tour_navigator.tim_prune.data.Field;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import static de.biselliw.tour_navigator.tim_prune.config.TimezoneHelper.getSelectedTimezone;
import static de.biselliw.tour_navigator.tim_prune.config.TimezoneHelper.getSelectedTimezoneStr;

/**
 * class to handle all records of the timetable
 */
public class RecordAdapter extends BaseAdapter {

    /** max. tolerated delay [min] */
    public static final int DELAY_MIN       = 5;
    public static final int DELAY_MAX       = 8;

    /** Color Codes */
    private static final int COLOR_BG_SELECTED = 0xFFB3DBFB;
    private static final int COLOR_BG_RECORD = 0xFFFFFFFF;

    /* don't show the presumable arrival time */
    private static final int COLOR_NONE       = 0xAA000000;
    /* text colors depending on the current delay */
    private static final int COLOR_DELAY_NONE = 0xAA0000ff;
    public static final int COLOR_DELAY_MIN  = 0xAA008000;
    public static final int COLOR_DELAY_MAX  = 0xAAB71C1C;

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
        public double Sclimb;
        public double Sdescent;
        public long Sseconds;

        public Record (DataPoint _trackPoint, int _trackPointIndex, double _Sdistance, double _Sclimb, double _Sdescent, long _Sseconds)
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
        if ((i >= 0) && (i < getCount())) {
            return recordList.get(i);
        } else {
            return null;
        }
    }

    public int indexOf (int inIndex)
    {
        int place = 0, result = -1;
        while (place < getCount()) {
            RecordAdapter.Record record = getItem(place);
            assert (record != null);
            if (inIndex <= record.trackPointIndex) {
                result = place;
                break;
            }
            place++;
        }
        return result;
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
        RecordViewHolder holder;
        if (view == null) {
            LayoutInflater recordInflater = (LayoutInflater) recordContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            // todo Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout's root element)
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
            if (point != null)
            {
                /* Mark selected row */
                if (i == _selected)
                    view.setBackgroundColor(COLOR_BG_SELECTED);
                else
                    view.setBackgroundColor(COLOR_BG_RECORD);

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
            }
        }
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
    public void setStartTime(long inTime) {
        _startTime.set(inTime);
    }

    /**
     * Set the start time of the tour
     * @param inTime start time of the tour
     */
    public void setStartTime(Time inTime) {
        _startTime.set(inTime);
        SettingsActivity.setStartTime(inTime.toMillis(true));
        Record record = getItem(0);
        if (record != null) {
            DataPoint point = record.trackPoint;
            if (point != null) {
                String time2445 = inTime.format2445();
                point.setFieldValue(Field.TIMESTAMP,time2445,false);
            }
        }
    }

    /**
     *
     * @return the selected record / null
     */
    public Record getCurrentItem() {
        if (_selected >= 0)
            return recordList.get(_selected);
        else
            return null;
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
     * Get the selected item in the list of places
     *
     * @return Index (starting at 0) / -1 if nothing is selected
     */
    public int getPlace() {
        return _selected;
    }

    /**
     * Get the item in the list of places
     * @param inPointIndex index of the data point
     *
     * @return Index (starting at 0) / -1 if nothing is selected
     */
    public int findPlace(int inPointIndex) {
        return _selected;
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
     * @param inDelay delay [min]
     */
    public void setDelay(int inDelay) {
        _delay_min = inDelay;
    }

    /**
     * @return the current delay [min]
     */
    public int getDelay() {
        return _delay_min;
    }

    /**
     * Show/hide real time data in places list view
     * @param inRealtime true to show
     */
    public void setRealtime (boolean inRealtime)
    {
        _realtime = inRealtime;
    }

    /**
     * @param inPlace table row
     * @return the DataPoint for a selected row
     */
    public DataPoint getDataPoint(int inPlace) {
        RecordAdapter.Record record = getItem(inPlace);
        if (record == null) return null;
        return record.getTrackPoint();
    }

    /**
     * Format the planned arrival time for a waypoint
     *
     * @param inPoint waypoint data
     * @return formatted time string
     */
    public String getPlannedArriveTime(DataPoint inPoint) {
        long _t = _startTime.toMillis(true) + inPoint.getTime() * 1000L;
        calender.setTimeInMillis(_t);
        return timeFormat.format(calender.getTime());
    }

    /**
     * Format the planned arrival time for a waypoint
     *
     * @param inPlace waypoint index of the record
     * @return formatted time string
     */
    public String getPlannedArriveTime(int inPlace) {
        String res = "";
        Record record = getItem(inPlace);
        if (record != null) {
            DataPoint point = record.trackPoint;
            if (point != null) res = getPlannedArriveTime(point);
        }
        return res;
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
            inView.setTextColor(getTextColorDelay());
        }
        else {
            /* don't show the presumable arrival time */
            inView.setTextColor(COLOR_NONE);
            if (inShowPlanned)
            {
                /* get the formatted planned arrival time */
                sTime = getPlannedArriveTime(inPoint);
            }
        }
        inView.setText(sTime);
    }

    /**
     * @return the background color code depending on the current delay
     */
    public int getBackgroundColorDelay() {
        if (_delay_min >= DELAY_MAX) {
            return COLOR_DELAY_MAX;
        } else if (_delay_min > 0) {
            return COLOR_DELAY_MIN;
        } else {
            return COLOR_DELAY_NONE;
        }
    }

    /**
     * @return the text color code depending on the current delay
     */
    public int getTextColorDelay() {
        if (_delay_min >= DELAY_MAX) {
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

}

