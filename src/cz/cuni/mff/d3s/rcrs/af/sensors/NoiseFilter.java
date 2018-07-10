package cz.cuni.mff.d3s.rcrs.af.sensors;

import java.util.Random;

public class NoiseFilter {

	private double variance;
	private Random random;
	
	
	public NoiseFilter(double variance) {
		this.variance = variance;
		random = new Random();
	}
	
	public double generateNoise(double mean) {
		return random.nextGaussian() * variance + mean;
	}
}
