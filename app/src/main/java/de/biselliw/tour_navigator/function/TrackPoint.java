package de.biselliw.tour_navigator.function;

/**
 * holds a minimum set of values for a trackpoint
 */
public class TrackPoint {
    /** distance since start of the track [km] */
    public final double distance;
    /** elevation of the trackpoint [m] */
    public final double elevation;
    /** set to true if the named trackpoint is used for routing */
    public final boolean isRoutePoint;

    public TrackPoint(double d, double e) {
        distance = d;
        elevation = e;
        isRoutePoint = false;
    }

    public TrackPoint(double d, double e, boolean isRoutePoint) {
        distance = d;
        elevation = e;
        this.isRoutePoint = isRoutePoint;
    }
}
