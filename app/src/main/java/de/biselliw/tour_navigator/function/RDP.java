package de.biselliw.tour_navigator.function;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>The <a href="https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm">Ramer–Douglas–Peucker (RDP) algorithm</a>
 * is an algorithm that decimates a curve composed of line segments to a similar curve with fewer points.</p>
 * <p>Brief description</p><ul>
 * <li>Start: Line between first and last point</li>
 * <li>Find the point with the maximum distance</li>
 * <li>If distance > ε: Divide recursively</li>
 * </ul>
 */
public class RDP {

    public static List<TrackPoint> simplify(List<TrackPoint> pts, double epsilon) {
        boolean[] keep = new boolean[pts.size()];
        keep[0] = true;
        keep[pts.size() - 1] = true;

        rdp(pts, 0, pts.size() - 1, epsilon, keep);

        List<TrackPoint> out = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            if (keep[i]) out.add(pts.get(i));
        }
        return out;
    }

    private static void rdp(List<TrackPoint> pts, int start, int end, double eps, boolean[] keep) {
        if (end <= start + 1) return;

        double maxDist = 0;
        int index = -1;

        TrackPoint a = pts.get(start);
        TrackPoint b = pts.get(end);

        for (int i = start + 1; i < end; i++) {
            double d = Geometry.verticalDistance(pts.get(i), a, b);
            if (d > maxDist) {
                maxDist = d;
                index = i;
            }
        }

        if (maxDist > eps) {
            keep[index] = true;
            rdp(pts, start, index, eps, keep);
            rdp(pts, index, end, eps, keep);
        }
    }

    static class Geometry {
        static double verticalDistance(TrackPoint p, TrackPoint a, TrackPoint b) {
            double t = (p.distance - a.distance) / (b.distance - a.distance);
            double yHat = a.elevation + t * (b.elevation - a.elevation);
            return Math.abs(p.elevation - yHat);
        }

        static double slopePercent(TrackPoint a, TrackPoint b) {
            return 100.0 * (b.elevation - a.elevation) / (b.distance - a.distance);
        }
    }
}



