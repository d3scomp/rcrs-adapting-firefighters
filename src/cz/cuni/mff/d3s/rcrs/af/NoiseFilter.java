package cz.cuni.mff.d3s.rcrs.af;

import java.util.Random;

public class NoiseFilter {

	private double mean;
	private double variance;
	private Random random;
	
	
	public NoiseFilter(double mean, double variance) {
		this.mean = mean;
		this.variance = variance;
		random = new Random();
	}
	
	public double generateNoise() {
		return random.nextGaussian() * Math.sqrt(variance) + mean;
	}
}
