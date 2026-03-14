package de.biselliw.tour_navigator.functions;

public class PublicStopsHandler {

    /**
     * Convert WGS84 coordinates into proprietary WebMercator format
     * @param inLatitude geo latitude
     * @param inLongitude geo longitude
     * @return x,y MRCV coordinates
     */
    public static double[] wgs84ToMRCV(double inLatitude, double inLongitude) {

        double latRad = Math.toRadians(inLatitude);
        double lonRad = Math.toRadians(inLongitude);

        final double R = 6378137.0;
        double x = R * lonRad;

        double y = 12000000.0 - R * Math.log(
                Math.tan(Math.PI / 4 + latRad / 2)
        );

        return new double[]{x, y};
    }

    /**
     * Get the URI to access public transport stops around a location
     * @param inLatitude geo latitude
     * @param inLongitude geo longitude
     * @return web URI
     */
    public static String getUriPublicStops(double inLatitude, double inLongitude) {
        double[] mercator = wgs84ToMRCV(inLatitude, inLongitude);
        return "https://www.fahrplanauskunft-mv.de/vmvsl3plus/departureMonitor?formik=origin%3Dcoord%253A"
                + (int) mercator[0] + "%253A" + (int) mercator[1] + "%253AMRCV";
    }
}
