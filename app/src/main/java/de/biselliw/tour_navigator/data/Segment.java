package de.biselliw.tour_navigator.data;

import androidx.annotation.NonNull;

public class Segment {

    enum type {
        SEG_INVALID,
        SEG_FLAT,
        SEG_UP,
        SEG_UP_MODERATE,
        SEG_UP_STEEP,
        SEG_DOWN,
        SEG_DOWN_MODERATE,
        SEG_DOWN_STEEP
    }

    type segmentType;
    int startIndex, endIndex;
    double startDistance_km, startClimb_m, startDescent_m;
    long startSeconds;
    double distance_km;
    /** Total climb in metres */
    double climb_m;
    /** Total descent in metres */
    double descent_m;
    int gradient;
    boolean steep;
    long totalSeconds, totalBreakTime_s;

    public Segment () {
        segmentType = type.SEG_INVALID;
    }

    public Segment (Segment fromOther) {
        segmentType = type.SEG_INVALID;
        startIndex = fromOther.endIndex;
        endIndex = startIndex;
        startDistance_km = fromOther.startDistance_km + fromOther.distance_km;
        startClimb_m   = fromOther.startClimb_m   + fromOther.climb_m;
        startDescent_m = fromOther.startDescent_m + fromOther.descent_m;
    }

    /**
     * Get the status of the navigation
     * @return status string
     */
    public String getSegmentType() {
        int segment_type= 0;
        String[] segment_typeStr = {
                "SEG_INVALID",
                "SEG_FLAT",
                "SEG_UP",
                "SEG_UP_MODERATE",
                "SEG_UP_STEEP",
                "SEG_DOWN",
                "SEG_DOWN_MODERATE",
                "SEG_DOWN_STEEP" };

        if (segmentType != null) {
            segment_type = segmentType.ordinal();
        }
        return segment_typeStr[segment_type];
    }

    @NonNull
    public String toString() {
        return "Type: " + getSegmentType();
    }

    public int getStartIndex () { return startIndex; }
    public int getEndIndex () { return endIndex; }
}
