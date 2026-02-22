package de.biselliw.tour_navigator.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Filters {
    public static List<TrackPoint> medianFilter(List<TrackPoint> in, int window) {
        List<TrackPoint> out = new ArrayList<>();
        int w = window / 2;

        for (int i = 0; i < in.size(); i++) {
            List<Double> vals = new ArrayList<>();
            for (int j = Math.max(0, i - w); j <= Math.min(in.size() - 1, i + w); j++) {
                vals.add(in.get(j).elevation);
            }
            Collections.sort(vals);
            double median = vals.get(vals.size() / 2);
            out.add(new TrackPoint(in.get(i).distance, median, in.get(i).isRoutePoint));
        }
        return out;
    }
}
