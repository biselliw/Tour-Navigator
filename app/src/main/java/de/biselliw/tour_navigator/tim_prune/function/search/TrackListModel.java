package de.biselliw.tour_navigator.tim_prune.function.search;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

import de.biselliw.tour_navigator.tim_prune.data.DataPoint;


/**
 * Model for list of tracks from a search result (eg geonames, overpass)
 */
public class TrackListModel // extends AbstractTableModel
{
    /**
     * column keys for interpretation of contents
     * */
    public static final int KEY_NAME = 1;
    public static final int KEY_TYPE = 2;
    public static final int COL_KEY_DISTANCE = 3;
    public static final int KEY_DISTANCE_TO_TRACK_M = 4;
    public static final int KEY_DISTANCE_FROM_START_KM = 5;
    public static final int COL_KEY_REF = 6;

    /** status message */
    public String message = "";

    /** notification for change of data */
    public boolean changed = false;

    /** List of tracks */
    private ArrayList<SearchResult> _trackList = null;

    /** Number of columns */
    private int _numColumns = 2;

    /**
	 * Constructor
	 * @param inColumnCount total number of columns
	 */
	public TrackListModel(int inColumnCount)
	{
		_numColumns = inColumnCount;
        /** Formatter for distances */
        NumberFormat _distanceFormatter = NumberFormat.getInstance();
        _distanceFormatter.setMaximumFractionDigits(1);
	}

	/**
	 * @return column count
	 */
	public int getColumnCount()
	{
		return _numColumns;
	}

	/**
	 * @return number of rows
	 */
	public int getRowCount()
	{
		if (_trackList == null) return 0;
		return _trackList.size();
	}

	/** @return true if there are no rows */
	public boolean isEmpty()
	{
		return getRowCount() == 0;
	}

	/**
	 * @param inRowNum row number
	 * @param inKey column key
	 * @return cell entry at given row and column
	 */
	public String getValueAt(int inRowNum, int inKey)
	{
        SearchResult track = _trackList.get(inRowNum);
        if (track != null) {
            switch (inKey) {
                case KEY_NAME:
                    return track.getTrackName();
                case KEY_TYPE:
                    return track.getPointType();
                case COL_KEY_REF:
                    if (track.getRef() != null)
                        return track.getRef();
                    break;
                case KEY_DISTANCE_TO_TRACK_M: {
                    return new DecimalFormat("#").format(track.getDistance() * 1000.0) + " m";
                }
                case KEY_DISTANCE_FROM_START_KM: {
                    return new DecimalFormat("#0.0").format(track.getDistance()) + " km";
/*
                    DataPoint dataPoint = track.getDataPoint();
                    if (dataPoint != null)
                        return new DecimalFormat("#0.0").format(dataPoint.getDistance()) + " km";
                    break;
 */
                }
            }
        }

        return "";
	}

	/**
	 * Add a list of tracks to this model
	 * @param inList list of tracks to add
	 */
	public void addTracks(ArrayList<SearchResult> inList)
	{
		addTracks(inList, false);
	}

	/**
	 * Add a list of tracks to this model and optionally sort them
	 * @param inList list of tracks to add
	 * @param inSort true to sort results after adding
	 */
	public void addTracks(ArrayList<SearchResult> inList, boolean inSort)
	{
		if (_trackList == null) {_trackList = new ArrayList<SearchResult>();}
		final int prevCount = _trackList.size();
		if (inList != null && inList.size() > 0)
		{
			_trackList.addAll(inList);
			if (inSort) {
				Collections.sort(_trackList);
			}
		}
        changed = true;
	}

	/**
	 * @param inRowNum row number from 0
	 * @return track object for this row
	 */
	public SearchResult getTrack(int inRowNum)
	{
		return _trackList.get(inRowNum);
	}

	/**
	 * Clear the list of tracks
	 */
	public void clear()
	{
		_trackList = null;
	}
}
