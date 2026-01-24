package de.biselliw.tour_navigator.function;

import org.apache.commons.math3.linear.*;

/**
 * <p>solution for estimating parameters a,b,c in  t = ax + by + cz
 * using Apache Commons Math with a numerically stable QR decomposition, followed by validation and residual analysis
 * Validation and residual analysis Metrics included</p>
 *
 * Residuals:<ul>
 * <li>RMSE (Root Mean Squared Error): measures absolute fit quality: RMSE ≈ 0 → excellent fit</li>
 * <li>R² (Coefficient of Determination): variance explained by the model: R² close to 1 → strong explanatory power</li>
 * </ul>
 *
 * Systematic patterns in residuals → model misspecification; Large RMSE with high R² → scale issues or outliers
 */
public class LinearFit3D {

    public static class Result {
        public final double a;
        public final double b;
        public final double c;
        public final double rmse;
        public final double r2;

        public Result(double a, double b, double c, double rmse, double r2) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.rmse = rmse;
            this.r2 = r2;
        }
    }

    public static Result estimate(
            double[] x, double[] y, double[] z, double[] t) {

        int size = x.length;
        if (y.length != size || z.length != size || t.length != size) {
            throw new IllegalArgumentException("All arrays must have same length");
        }

        // Design matrix X
        double[][] data = new double[size][3];
        for (int i = 0; i < size; i++) {
            data[i][0] = x[i];
            data[i][1] = y[i];
            data[i][2] = z[i];
        }

        RealMatrix X = MatrixUtils.createRealMatrix(data);
        RealVector T = MatrixUtils.createRealVector(t);

        // QR decomposition (numerically stable)
        DecompositionSolver solver = new QRDecomposition(X).getSolver();
        RealVector beta = solver.solve(T);

        double a = beta.getEntry(0);
        double b = beta.getEntry(1);
        double c = beta.getEntry(2);

        // Predictions
        RealVector tHat = X.operate(beta);

        // Residuals
        RealVector residuals = T.subtract(tHat);

        double rss = residuals.dotProduct(residuals);

        double meanT = T.getL1Norm() / size;
        double tss = 0.0;
        for (int i = 0; i < size; i++) {
            double d = t[i] - meanT;
            tss += d * d;
        }

        double rmse = Math.sqrt(rss / size);
        double r2 = 1.0 - rss / tss;

        return new Result(a, b, c, rmse, r2);
    }
}
