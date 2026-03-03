package de.biselliw.tour_navigator.functions;

import java.util.*;

/**
 * Segmented Least Squares for 1D profiles (distance -> elevation):
 * Computes an optimal piecewise-linear approximation with L2 error.
 */
public class SegmentedLeastSquares {

    /** Output segment */
    public static class Segment {
        public final int startIndex, endIndex;   // index range [i0, i1]
        public final double a, b;  // y = a*x + b
        public final double error; // sum of squared errors

        public Segment(int startIndex, int endIndex, double a, double b, double error) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.a = a;
            this.b = b;
            this.error = error;
        }
    }

    /**
     * Main entry point.
     * @param pts input points, sorted by x
     * @param lambda penalty per segment (controls number of segments)
     */
    public static List<Segment> fit(List<TrackPoint> pts, double lambda) {
        int n = pts.size();

        // Prefix sums for fast least-squares
        double[] Sx = new double[n + 1];
        double[] Sy = new double[n + 1];
        double[] Sxx = new double[n + 1];
        double[] Sxy = new double[n + 1];
        double[] Syy = new double[n + 1];

        for (int i = 1; i <= n; i++) {
            TrackPoint p = pts.get(i - 1);
            Sx[i]  = Sx[i - 1]  + p.distance;
            Sy[i]  = Sy[i - 1]  + p.elevation;
            Sxx[i] = Sxx[i - 1] + p.distance * p.distance;
            Sxy[i] = Sxy[i - 1] + p.distance * p.elevation;
            Syy[i] = Syy[i - 1] + p.elevation * p.elevation;
        }

        // Precompute error and line parameters for all intervals
        double[][] err = new double[n][n];
        double[][] a = new double[n][n];
        double[][] b = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                int m = j - i + 1;
                double sx  = Sx[j + 1]  - Sx[i];
                double sy  = Sy[j + 1]  - Sy[i];
                double sxx = Sxx[j + 1] - Sxx[i];
                double sxy = Sxy[j + 1] - Sxy[i];
                double syy = Syy[j + 1] - Syy[i];

                double denom = m * sxx - sx * sx;
                double ai, bi;

                if (Math.abs(denom) < 1e-12) {
                    // Degenerate: vertical or identical x
                    ai = 0.0;
                    bi = sy / m;
                } else {
                    ai = (m * sxy - sx * sy) / denom;
                    bi = (sy - ai * sx) / m;
                }

                // SSE = sum (y - ax - b)^2
                double e = syy
                        + ai * ai * sxx
                        + m * bi * bi
                        - 2 * ai * sxy
                        - 2 * bi * sy
                        + 2 * ai * bi * sx;

                a[i][j] = ai;
                b[i][j] = bi;
                err[i][j] = e;
            }
        }

        // Dynamic Programming
        double[] dp = new double[n + 1];
        int[] prev = new int[n + 1];

        dp[0] = 0.0;
        prev[0] = -1;

        for (int j = 1; j <= n; j++) {
            dp[j] = Double.POSITIVE_INFINITY;
            for (int i = 1; i <= j; i++) {
                double cost = dp[i - 1] + err[i - 1][j - 1] + lambda;
                if (cost < dp[j]) {
                    dp[j] = cost;
                    prev[j] = i - 1;
                }
            }
        }

        // Backtracking
        LinkedList<Segment> segments = new LinkedList<>();
        int j = n;
        while (j > 0) {
            int i = prev[j];
            segments.addFirst(new Segment(
                    i,
                    j - 1,
                    a[i][j - 1],
                    b[i][j - 1],
                    err[i][j - 1]
            ));
            j = i;
        }

        return segments;
    }
}
