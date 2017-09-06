package larc.ctlr.tool;

public class MathTool {
	public static double dotProduct(int nDim, double[] x, double[] y) {
		double d = 0;
		for (int i = 0; i < nDim; i++) {
			d += x[i] * y[i];
			// System.out.println(String.format("%f - %f", x[i], y[i]));
		}
		// System.out.println("d = " + d);
		return d;
	}

	public static double sigmoid(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	public static double sigmoid(double kamma, double x) {
		return 1 / (1 + Math.exp(-x * kamma));
	}

	public static double normalizationFunction(double x) {
		if (x <= 0) {
			System.out.println("in normalization function: variable is negative");
			System.exit(-1);
		}
		// double s = sigmoid(x);
		double s = sigmoid(0.01, x);
		// System.out.println("s = " + s);
		return 2 * (s - 0.5);
	}

	public static double log10Sum(double[] x) {
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;
		for (int i = 0; i < x.length; i++) {
			if (x[i] > max) {
				max = x[i];
			}
			if (x[i] < min) {
				min = x[i];
			}
		}
		double mid = (max + min) / 2;
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum += Math.pow(10, x[i] - mid);
		}
		return mid + Math.log10(sum);
	}

	public static void main(String[] args) {
		double[] x = new double[] { -100, -102, -103 };
		System.out.printf("logSum = %f", log10Sum(x));
	}
}
