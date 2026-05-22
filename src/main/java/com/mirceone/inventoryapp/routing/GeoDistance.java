package com.mirceone.inventoryapp.routing;

/**
 * Haversine distance on Earth (WGS84 sphere approximation).
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private GeoDistance() {
    }

    /**
     * @return distance in meters between two WGS84 points
     */
    public static double meters(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    /** Integer meters matrix for OR-Tools (symmetric). */
    public static long[][] metersMatrix(double[] latitudes, double[] longitudes) {
        int n = latitudes.length;
        long[][] m = new long[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    m[i][j] = 0;
                } else {
                    double d = meters(latitudes[i], longitudes[i], latitudes[j], longitudes[j]);
                    m[i][j] = Math.max(1, Math.round(d));
                }
            }
        }
        return m;
    }
}
