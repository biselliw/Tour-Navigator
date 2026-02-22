package de.biselliw.tour_navigator.adapter;

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

    Copyright 2026 Walter Biselli (BiselliW)
*/

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.biselliw.tour_navigator.BuildConfig;
import de.biselliw.tour_navigator.R;
import de.biselliw.tour_navigator.activities.LocationActivity;
import de.biselliw.tour_navigator.activities.MainActivity;
import de.biselliw.tour_navigator.data.Resources;
import de.biselliw.tour_navigator.helpers.Log;
import de.biselliw.tour_navigator.helpers.ProfileAdapter;
import de.biselliw.tour_navigator.tim_prune.data.DataPoint;

import static de.biselliw.tour_navigator.App.app;

/**
 * class to handle all records of the timetable
 */
public class RecordAdapter extends BaseAdapter {

    /**
     * TAG for log messages.
     */
    static final String TAG = "RecordAdapter";
    private static final boolean _DEBUG = true; // Set to true to enable logging
    private static final boolean DEBUG = _DEBUG && BuildConfig.DEBUG;

    /**
     * max. tolerated delay [min]
     */
    public static final int DELAY_MIN = 3;
    public static final int DELAY_MAX = 8;

    /**
     * resource IDs for colors
     */
    private static final int COLOR_BG_SELECTED = R.color.COLOR_BG_SELECTED;
    private static final int COLOR_BG_RECORD = R.color.COLOR_BG_RECORD;

    /* don't show the presumable arrival time */
    private static final int COLOR_NONE = R.color.COLOR_NONE;
    /* text colors depending on the current delay */
    private static final int COLOR_DELAY_NONE = R.color.COLOR_DELAY_NONE;
    public static final int COLOR_DELAY_MIN = R.color.COLOR_DELAY_MIN;
    public static final int COLOR_DELAY_MAX = R.color.COLOR_DELAY_MAX;

    private ProfileAdapter _profileAdapter;

    LayoutInflater recordInflater;

    private ListView recordsView;
    public List<Record> recordList;
    private List<Record> _updatedRecordList = null;

    private Calendar _calendar;

    private int _selected = -1;
    private int _initialPlace = -1;
    private int _lastPlace = -1;
    private String _debugLastPlace = "";
    private int _endPlace = 0;

    /** start time of the tour in [min] since midnight */
    private int _startTime_min;
    /** start time of the tour in [UTC ms] since midnight */
    private long _startTime_UTC;

    private boolean _realtime = false;
    /**
     * current distance since start
     */
    private double _distance = 0.0;
    /**
     * current delay [min]
     */
    private int _delay_min = 0;
    final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.GERMAN);
    final DecimalFormat decFormat = new DecimalFormat("  #0.0");

    private static class RecordViewHolder {
        public TextView timeView;
        public TextView delayView;
        public TextView distanceView;
        public TextView placeView;
    }

    TextView track_place, track_arrive;

    /**
     * Constructor
     *
     * @param activity context
     * @param records list of all records
     */
    public RecordAdapter(LocationActivity activity, ProfileAdapter inProfileAdapter, List<Record> records) {
        recordList = records;

        recordInflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        _profileAdapter = inProfileAdapter;

        track_place = activity.findViewById(R.id.track_place);
        track_arrive = activity.findViewById(R.id.track_arrive);

        recordsView = activity.findViewById(R.id.records_view);
        recordsView.setAdapter(this);

        // Create a Listener for this list view of places
        recordsView.setOnItemClickListener((adapter, v, inPlace, arg3) ->
                setPlace(activity, inPlace, true));

        _calendar = Calendar.getInstance();
    }

    public static class Record {
        public final int trackPointIndex;
        public final DataPoint trackPoint;
        public final double Sdistance;
        public final double Sclimb;
        public final double Sdescent;
        public final long Sseconds;

        public Record(DataPoint _trackPoint, int _trackPointIndex, double _Sdistance, double _Sclimb, double _Sdescent, long _Sseconds) {
            trackPointIndex = _trackPointIndex;
            trackPoint = _trackPoint;
            Sdistance = _Sdistance;
            Sclimb = _Sclimb;
            Sdescent = _Sdescent;
            Sseconds = _Sseconds;
        }
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

    /**
     * Get the index of the record containing a trackpoint to search for
     * @param inIndex index of the trackpoint within the list of trackpoints
     * @return index of the record
     */
    public int indexOf(int inIndex) {
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
     * @return view
     */
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        RecordViewHolder holder;

        if (view == null) {
            // Avoid passing `null` as the view root (needed to resolve layout parameters on the inflated layout's root element)
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
        if (record != null) {
            // use local calendar
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(_startTime_UTC);

            DataPoint point = record.trackPoint;
            if (point != null) {
                /* Mark selected row */
                int bgColor = (i == _selected) ? COLOR_BG_SELECTED : COLOR_BG_RECORD;
                view.setBackgroundColor(Resources.get_Color(bgColor)); // 0x14d0d0d0); // todo get_Color(bgColor));

                /* get the formatted arrival time */
                calendar.setTimeInMillis(_startTime_UTC + point.getTime() * 1000L);

                String plannedTime_Str = timeFormat.format(calendar.getTime());
                if (point.getWaypointDuration() > 0) {
                    /* add end of break time */
                    calendar.add(Calendar.MINUTE, point.getWaypointDuration());
                    plannedTime_Str = plannedTime_Str + " " + timeFormat.format(calendar.getTime());
                }
                holder.timeView.setText(plannedTime_Str);

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

    public void onDataSetChanged() {
        if (_updatedRecordList != null) {
            RemoveRecords();
            for (Record record : _updatedRecordList) {
                add(record);
            }
            _updatedRecordList.clear();
            _updatedRecordList = null;
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (DEBUG) Log.d(TAG,"notifyDataSetChanged()");
        super.notifyDataSetChanged();
    }

    public void notifyDataSetChanged(List<Record> records) {
        _updatedRecordList = records;
        if (records == null) {
            RemoveRecords();
        }
        if (DEBUG) Log.d(TAG,"notifyDataSetChanged(records)");
// todo  notifyDataSetChanged();
    }

    /**
     * Remove all records
     */
    public void RemoveRecords() {
        while (getCount() > 0) {
            recordList.remove(0);
        }
        _selected = -1;
    }

    /**
     * Set the start time of the tour
     *
     * @param inStartTime time in [min] since midnight
     */
    public void setStartTime(int inStartTime) {
        _startTime_min = inStartTime;

        /* get the corresponding UTC time in ms once */
        _calendar.set(0, 0, 0, 0, 0);
        _calendar.setTimeInMillis(_calendar.getTimeInMillis() + inStartTime * 60000L);
        _startTime_UTC = _calendar.getTimeInMillis();

/*
        Record record = getItem(0);
        if (record != null) {
            DataPoint point = record.trackPoint;
            if (point != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.MINUTE, inStartTime % 60);
                calendar.set(Calendar.HOUR, inStartTime / 60);
                // replace inTime.format2445(); eg. "2024-09-07T07:39:27.290Z"
                String time2445 = ISO_8601_FORMAT.format(calendar.getTime());
                // todo check result
                point.setFieldValue(Field.TIMESTAMP, time2445, false);
            }
        }
 */
        track_arrive.setText("");
        notifyDataSetChanged();
    }

    /**
     * Sets an item in the list of places
     *
     * @param inPlace Index (starting at 0) of the data item to be selected or -1 if nothing
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
     * Set the current distance since start
     *
     * @param inDistance inDistance
     */
    public void setDistance(double inDistance) {
        _distance = inDistance;
    }

    /**
     * Set the current delay
     *
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
     *
     * @param inRealtime true to show
     */
    public void setRealtime(boolean inRealtime) {
        _realtime = inRealtime;
    }

    /**
     * Format the planned arrival time for a route point
     *
     * @param inPlace route point index of the record
     * @return formatted time string
     */
    public String getPlannedArriveTime(int inPlace) {
        String res = "";
        Record record = getItem(inPlace);
        if (record != null) {
            DataPoint point = record.trackPoint;
            if (point != null) {
                _calendar.setTimeInMillis(_startTime_UTC + point.getTime() * 1000L);
                res = timeFormat.format(_calendar.getTime());
            }
        }
        return res;
    }

    /**
     * Show the presumable arrival time depending on the delay
     *
     * @param inShowDelay   if true: show the planned time plus delay
     * @param inShowPlanned show planned time if presumable is not available
     * @param inView        TextView for display
     * @param inPoint       point data
     * @return formatted planned arrival time
     */
    public synchronized String showPresumableArriveTime(boolean inShowDelay, boolean inShowPlanned, TextView inView, DataPoint inPoint) {
        // use local calendar
        Calendar calendar = Calendar.getInstance();

        /* get the formatted planned arrival time */
        calendar.setTimeInMillis(_startTime_UTC + inPoint.getTime() * 1000L);
        String plannedTime_Str = "";

        if (_realtime && (_startTime_min > 0) && (inPoint.getDistance() >= _distance)) {
            // todo check result
            if (inShowDelay) {
                // get the formatted planned arrival time
                plannedTime_Str = timeFormat.format(calendar.getTime());

                calendar.set(0, 0, 0, 0, 0);
                if (_delay_min > 0) {
                    calendar.add(Calendar.MINUTE, _delay_min);
                    String delay_Str = timeFormat.format(calendar.getTime());
                    plannedTime_Str = plannedTime_Str + " +" + delay_Str;
                } else if (_delay_min < 0) {
                    calendar.add(Calendar.MINUTE, -_delay_min);
                    String delay_Str = timeFormat.format(calendar.getTime());
                    plannedTime_Str = plannedTime_Str + " -" + delay_Str;
                }
            } else {
                // show delay
                calendar.add(Calendar.MINUTE, _delay_min);
                plannedTime_Str = timeFormat.format(calendar.getTime());
            }
            inView.setTextColor(getTextColorDelay());
        } else {
            /* don't show the presumable arrival time */
            inView.setTextColor(Resources.get_Color(COLOR_NONE));
            if (inShowPlanned) {
                /* get the formatted planned arrival time */
                plannedTime_Str = timeFormat.format(calendar.getTime());
            }
        }
        inView.setText(plannedTime_Str);
        return plannedTime_Str;
    }

    /**
     * @return the text color code depending on the current delay
     */
    public int getTextColorDelay() {
        if (_delay_min >= DELAY_MAX)
            return Resources.get_Color(COLOR_DELAY_MAX);
        else if (_delay_min > 0)
            return Resources.get_Color(COLOR_DELAY_MIN);
        else
            return Resources.get_Color(COLOR_DELAY_NONE);
    }

    /**
     * Check if the list already contains a record with a data point
     * @param inPoint object to compare
     * @return true if the track already contains the point
     * @author BiselliW
     */
    public boolean contains(DataPoint inPoint) {
        for (int i = 0; i < recordList.size(); i++) {
            DataPoint point = getItem(i).trackPoint;
            if (point != null)
                if (point.getLinkIndex() >= 0)
                    point = app.getPoint(point.getLinkIndex());
            if (point != null)
                if (point.isDuplicate(inPoint))
                    return true;
        }
        return false;
    }

    /**
     * add a record without updating the list view
     *
     * @param record record
     */
    public void add(Record record) {
        recordList.add(record);
    }

    /**
     * Set next place to arrive:
     * Put he marker to the row within the table of places which shows the next place to arrive
     * after the given distance
     *
     * @param inDistance current distance since start
     * @param inUser     true if invoked by the user
     * @return index of the place within the list of places
     */
    public int setNextPlace(LocationActivity inActivity, double inDistance, boolean inUser) {
        int place = 0;
        do {
            DataPoint recPoint = getItem(place).trackPoint;
            if (recPoint != null) {
                double dist = recPoint.getDistance();
                if (inDistance > dist) {
                    place++;
                }
                else
                {
                    setPlace(inActivity, place, inUser);
                    break;
                }
            }
        }
        while (place < getCount());
        return place;
    }

    /**
     * Set the position in list of places and update information
     *
     * @param inPlace row index of the table
     * @param inUser  true if invoked by the user
     * @see RecordAdapter#setPlace(int)
     */
    public boolean setPlace(LocationActivity inActivity, int inPlace, boolean inUser) {
        // if (DEBUG) Log.d(TAG,"setPlace "+ inPlace);
        double distanceToPlace = 0.0;
        if (inPlace < 0) inPlace = 0;
        _initialPlace = inPlace;

        _endPlace = inPlace;
        if (inUser)
            inActivity.clearErrorMessage();

        if (showPlace(inPlace)) {
            /* Set relative position in the seek bar */
            RecordAdapter.Record record = getItem(inPlace);
            if (record == null) return false;
            DataPoint point = record.trackPoint;
            if (point == null) return false;
            double distance = point.getDistance();
            distanceToPlace = distance - _distance; // dist_from_start;

            /* Scroll to the place in the list */
            setPlace(inPlace);

            if (!inActivity.isViewExpanded()) {
                if (!inUser)
                    recordsView.smoothScrollToPosition(inPlace);
                else
                    inActivity.updateTrackingStatus();
            }

            if (inUser) {
                inActivity.setStartGpsIndex(point.getIndex());
                if (inPlace == 0) {
                    inActivity.resetLocationStatus();
                }

                record = getItem(inPlace + 1);
                double nextDistance = distance + 1.0;
                if (record != null) {
                    point = record.trackPoint;
                    if (point != null)
                        nextDistance = point.getDistance();
                }
                _profileAdapter.setXRange(distance, nextDistance);
            } else {
                if (inPlace > 0) {
                    record = getItem(inPlace - 1);
                    double prevDistance = distance - 1.0;
                    if (record != null) {
                        point = record.trackPoint;
                        if (point != null)
                            prevDistance = point.getDistance();
                    }
                    _profileAdapter.setXRange(prevDistance, distance);
                } else
                    _profileAdapter.clearXRange();
            }
        } else {
            _profileAdapter.setCursor(-1);
            _profileAdapter.clearXRange();
            setPlace(inPlace);
        }

        // Set the distance to the next place
        inActivity.setDistanceToPlace(distanceToPlace);

        if (inPlace != _lastPlace)
        {
            inActivity.showExpandViewStatus(inPlace, inActivity.isViewExpanded());
            inActivity.showAdditionalInfo(inPlace);
            notifyDataSetChanged();
            recordsView.smoothScrollToPosition(inPlace);
        }

        _lastPlace = inPlace;
        return true;
    }


    /**
     * Put the marker to a selected row within the table of places
     *
     * @param inPlace row index of the table
     * @return if shown
     */
    public boolean showPlace(int inPlace) {
        String place = "", plannedTime_Str = "";
        track_arrive.setText("");
        _endPlace = inPlace;

        if (inPlace >= 0) {
            if (inPlace < getCount()) {
                /* Show the place on the board */
                RecordAdapter.Record record = getItem(inPlace);
                if (record == null) return false;
                DataPoint point = record.trackPoint;
                if (point == null) return false;
                place = point.getRoutePointName();

                /* Show time of arrival at next place */
                if (_startTime_min > 0) {
                    // show the presumable arrival time depending on the delay
                    plannedTime_Str = showPresumableArriveTime(true, true, track_arrive, point);
                }
            }
        }
        track_place.setText(place);
        if (DEBUG) {
            if (!place.equals(_debugLastPlace)) Log.i(TAG,"showPlace: " + place +
                    "; planned arrival: " + plannedTime_Str);
            _debugLastPlace = place;
        }
        return (inPlace >= 0);
    }

    public void scrollToListPosition() {
        notifyDataSetChanged();
        int inPlace = getPlace();
        if (inPlace >= 0)
            recordsView.smoothScrollToPosition(inPlace);
    }

    public int getInitialPlace () { return _initialPlace; }

    /* limit search to end of a record */
    public int getEndIndex (int prevEndIndex) {
        int endIndex = prevEndIndex;

        int startPlace = getPlace();
        if (startPlace > _endPlace) _endPlace = startPlace;
        for (int place = startPlace; place < getCount() - 1; place++) {
            if (place <= _endPlace) {
                RecordAdapter.Record nextRecord = getItem(place + 1);
                int _endIndex = nextRecord.trackPointIndex - 1;
                if (endIndex > _endIndex)
                    _endPlace++;
                if (_endIndex > endIndex)
                    endIndex = _endIndex;
                break;
            }
        }
        return endIndex;
    }

    public void resetEndPlace() {  _endPlace = 0; }

    public void destroy () {
        _profileAdapter = null;
        recordInflater = null;
        recordsView = null;;
        if (recordList != null) {
            recordList.clear();
            recordList = null;
        }
        if (_updatedRecordList != null) {
            _updatedRecordList.clear();
            _updatedRecordList = null;
        }
        _calendar = null;;
    }



}