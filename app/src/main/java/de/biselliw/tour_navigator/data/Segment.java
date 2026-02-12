package de.biselliw.tour_navigator.data;

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

import androidx.annotation.NonNull;

import static de.biselliw.tour_navigator.data.TrackSegments.formatDouble;

/**
 * linear segment of a track
 */
public class Segment {

    public enum type {
        SEG_INVALID,
        SEG_FLAT,
        SEG_UP,
        SEG_UP_MODERATE,
        SEG_UP_STEEP,
        SEG_DOWN,
        SEG_DOWN_MODERATE,
        SEG_DOWN_STEEP
    }

    /** type of the segment: flat, ascending, descending */
    public type segmentType;

    /** index range with regard to original track */
    private int startIndex, endIndex;

    /** Distance since start [km] */
    private double distance;

    /** Elevation (altitude) [m] */
    private double elevation;

    /** horizontal distance within the segment [km] */
    private double deltaX;

    /** vertical distance within the segment [m] */
    private double deltaY;

    /** active time within the segment [s] */
    private long activeTime_s;

    /** break time within the segment [s] */
    private long breakTime_s;

    /** gradient (dY/dX) [0 ... 100] */
    private int gradient;


    public Segment() {
        segmentType = type.SEG_INVALID;
    }

    public Segment(double distance, double elevation, double deltaX, double deltaY) {
        segmentType = type.SEG_INVALID;
        this.distance = distance;
        this.elevation = elevation;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public Segment(Segment fromOther) {
        segmentType = type.SEG_INVALID;
        distance = fromOther.distance + fromOther.deltaX;
        elevation = fromOther.elevation + fromOther.deltaY;
        deltaX = deltaY = 0.0;
        activeTime_s = breakTime_s = 0;
        gradient = 0;

        startIndex = fromOther.endIndex;
        endIndex = startIndex;
    }

    /**
     * @return segment type as string
     */
    public String getSegmentType() {
        int segment_type = 0;
        String[] segment_typeStr = {
                "SEG_INVALID",
                "SEG_FLAT",
                "SEG_UP",
                "SEG_UP_MODERATE",
                "SEG_UP_STEEP",
                "SEG_DOWN",
                "SEG_DOWN_MODERATE",
                "SEG_DOWN_STEEP"};

        if (segmentType != null) {
            segment_type = segmentType.ordinal();
        }
        return segment_typeStr[segment_type];
    }

    @NonNull
    public String toString() {
        return "Type: " + getSegmentType() + "; speed: " + formatDouble(deltaX / activeTime_s * 3600.0);
    }

    public void setStartIndex(int startIndex) { this.startIndex = startIndex; }
    public int getStartIndex () { return startIndex; }
    public void setEndIndex(int endIndex) { this.endIndex = endIndex; }
    public int getEndIndex () { return endIndex; }
    public void setDistance (double distance) { this.distance = distance; }
    public double getDistance() { return distance; }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
    public double getElevation() { return elevation; }

    public void setDeltaX(double deltaX) {
        this.deltaX = deltaX;
    }

    public double getDeltaX() {
        return deltaX;
    }

    public void setDeltaY(double deltaY) {
        this.deltaY = deltaY;
    }

    public double getDeltaY() {
        return deltaY;
    }

    public void setActiveTime_s(long activeTime_s) {
        this.activeTime_s = activeTime_s;
    }

    public long getActiveTime_s() {
        return activeTime_s;
    }

    public void setBreakTime_s(long breakTime_s) {
        this.breakTime_s = breakTime_s;
    }

    public long getBreakTime_s() {
        return breakTime_s;
    }

    public void setGradient(int gradient) {
        this.gradient = gradient;
    }

    public int getGradient() {
        return gradient;
    }
}
