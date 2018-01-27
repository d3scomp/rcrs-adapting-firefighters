package cz.cuni.mff.d3s.rcrs.af;

import java.util.Random;

public class NoiseFilter {

	private double variance;
	private Random random;
	
	
	public NoiseFilter(double variance) {
		this.variance = variance;
		random = new Random();
	}
	
	public double generateNoise(double mean) {
		int sign = random.nextBoolean() ? 1 : -1;
		return sign * random.nextGaussian() * Math.sqrt(variance) + mean;
	}
}
