package de.biselliw.tour_navigator.data;

import androidx.annotation.NonNull;

public class Segment {

    enum segment_type {
        SEG_FLAT,
        SEG_UP_MODERATE,
        SEG_UP_STEEP,
        SEG_DOWN_MODERATE,
        SEG_DOWN_STEEP
    }

    segment_type segmentType;
    int startIndex, endIndex;
    double startDistance_km, startClimb_m, startDescent_m;
    long startSeconds;
    double distance_km,
    /** Total climb in metres */
    climb_m,
    /** Total descent in metres */
    descent_m;
    long horSeconds, vertSeconds, totalSeconds, totalBreakTime_s;


    @NonNull
    public String toString() {
        return "Type: " + segmentType.ordinal();
    }
}
