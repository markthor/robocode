package cs.mint.util;

public class FastMath {
	public static double exp(final double val) {
		final long tmp = (long) (1512775 * val + 1072632447);
		return Double.longBitsToDouble(tmp << 32);
	}
}
