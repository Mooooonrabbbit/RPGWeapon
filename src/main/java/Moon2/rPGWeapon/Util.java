package Moon2.rPGWeapon;

public class Util {

    private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
    private static final double[] ASIN_TAB = new double[257];
    private static final double[] COS_TAB = new double[257];

    public static double fastInvSqrt(double x) {
        double xhalf = 0.5 * x;
        long i = Double.doubleToRawLongBits(x);
        i = 6910469410427058090L - (i >> 1);
        x = Double.longBitsToDouble(i);
        return x * (1.5 - xhalf * x * x);
    }

    public static double atan2(double y, double x) {
        double d2 = x * x + y * y;
        if (Double.isNaN(d2)) {
            return Double.NaN;
        } else {
            boolean negY = y < 0.0;
            if (negY) {
                y = -y;
            }

            boolean negX = x < 0.0;
            if (negX) {
                x = -x;
            }

            boolean steep = y > x;
            if (steep) {
                double t = x;
                x = y;
                y = t;
            }

            double rinv = fastInvSqrt(d2);
            x *= rinv;
            y *= rinv;
            double yp = FRAC_BIAS + y;
            int index = (int)Double.doubleToRawLongBits(yp);
            double phi = ASIN_TAB[index];
            double cPhi = COS_TAB[index];
            double sPhi = yp - FRAC_BIAS;
            double sd = y * cPhi - x * sPhi;
            double d = (6.0 + sd * sd) * sd * 0.16666666666666666;
            double theta = phi + d;
            if (steep) {
                theta = (Math.PI / 2) - theta;
            }

            if (negX) {
                theta = Math.PI - theta;
            }

            if (negY) {
                theta = -theta;
            }

            return theta;
        }
    }
}
