package cs.utils;

public class FastMath {
	public static final double PI        = 3.1415926535897932384626433832795D;
	public static final double TWO_PI    = 6.2831853071795864769252867665590D;
	public static final double HALF_PI   = 1.5707963267948966192313216916398D;
	public static final double QUARTER_PI = 0.7853981633974483096156608458199D;
	public static final double THREE_OVER_TWO_PI = 4.7123889803846898576939650749193D;

	private static final int TRIG_DIVISIONS = 8192;//MUST be power of 2!!!
	private static final int TRIG_HIGH_DIVISIONS = 131072;//MUST be power of 2!!!
	private static final double K = TRIG_DIVISIONS / TWO_PI;
	//private static final double TAN_K = TRIG_HIGH_DIVISIONS / PI;
	private static final double ACOS_K = (TRIG_HIGH_DIVISIONS - 1)/ 2;

	private static final double[] sineTable = new double[TRIG_DIVISIONS];
	private static final double[] acosTable = new double[TRIG_HIGH_DIVISIONS];

	static {
		for (int i = 0; i < TRIG_DIVISIONS; i++) {
			sineTable[i] = Math.sin(i / K);
		}
		for(int i = 0; i < TRIG_HIGH_DIVISIONS; i++){
			acosTable[i] = Math.acos(i / ACOS_K - 1);
		}
	}

	public static final double sin(double value) {
		return sineTable[(int)(((value * K + 0.5) % TRIG_DIVISIONS + TRIG_DIVISIONS) )&(TRIG_DIVISIONS - 1)];
	}

	public static final double cos(double value) {
		return sineTable[(int)(((value * K + 0.5) % TRIG_DIVISIONS + 1.25 * TRIG_DIVISIONS) )&(TRIG_DIVISIONS - 1)];
	}

	public static final double tan(double value) {
		return sin(value)/cos(value);
	}

	public static final double asin(double value) {
		return HALF_PI - acos(value);
	}

	public static final double acos(double value) {
		return acosTable[(int)(value*ACOS_K + (ACOS_K + 0.5))];
	}

	public static final double atan(double value) {
		return (value >= 0 ? acos(1 / sqrt(value * value + 1)) : -acos(1 / sqrt(value * value + 1)));
	}

	public static final double atan2(double x, double y) {
		return (x >= 0 ? acos(y / sqrt(x*x + y*y)) : -acos(y / sqrt(x*x + y*y)));
	}

	public static final double sqrt(double x){
		return Math.sqrt(x);
	}

	public static final int signum(double d) {
		if(d == 0) return 0;
		if(d > 0) return 1;
		return -1;
	}

	public static final int sign(double d) {
		if(d < 0) return -1;
		return 1;
	}

	public static final double max(double a, double b) {
		if(a > b) return a;
		return b;
	}

	public static final double min(double a, double b) {
		if(a < b) return a;
		return b;
	}

	public static final double limit(double min, double value, double max) {
		if (value > max) return max;
		if (value < min) return min;
		return value;
	}

	public static final int max(int a, int b) {
		if(a > b) return a;
		return b;
	}

	public static final int min(int a, int b) {
		if(a < b) return a;
		return b;
	}

	public static final int limit(int min, int value, int max) {
		if (value > max) return max;
		if (value < min) return min;
		return value;
	}

	public static final double abs(double a) {
		if(a < 0) return -a;
		return a;
	}

	public static final int abs(int a) {
		if(a < 0) return -a;
		return a;
	}
}
