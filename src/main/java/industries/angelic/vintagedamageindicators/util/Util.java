package industries.angelic.vintagedamageindicators.util;

public class Util {

    public static int getA(int argb) {
        return (argb >> 24) & 0xFF;
    }

    public static int getR(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int getG(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int getB(int argb) {
        return argb & 0xFF;
    }

    public static int argbtoInt(int a, int r, int g, int b) {
        return (0xFF & a) << 24 | (0xFF & r) << 16 | (0xFF & g) << 8 | (0xFF & b);
    }

    /* alpha from 0 to 1 */
    public static int applyAlpha(int argb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) Math.floor(alpha * 256.0)));
        int r = getR(argb);
        int g = getG(argb);
        int b = getB(argb);
        return argbtoInt(a, r, g, b);
    }
}
