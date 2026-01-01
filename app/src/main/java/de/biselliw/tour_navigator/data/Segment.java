package de.biselliw.tour_navigator.data;

public class Segment {

    enum segment_type {
        SEG_FLAT,
        SEG_UP_MODERATE,
        SEG_UP_STEEP,
        SEG_DOWN_MODERATE,
        SEG_DOWN_STEEP
    }

    static segment_type segmentType;
    int startIndex, endIndex;
    double startDistance_km, startClimb_m, startDescent_m;
    long startSeconds;
    double distance_km, climb_m, descent_m;
    long horSeconds, vertSeconds, totalSeconds;

    void Segment () {
        clear();
    }

    void clear() {
        segmentType = segment_type.SEG_FLAT;
        startIndex = 0;
        distance_km = 0.0;
        climb_m = 0.0;
        descent_m = 0.0;
    }
}
