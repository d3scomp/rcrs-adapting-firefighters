package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_ALPHA;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_CNT;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_SIZE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.USE_EXTENDED_MODES;

import java.util.Random;

import cz.cuni.mff.d3s.rcrs.af.NoiseFilter;
import cz.cuni.mff.d3s.tss.TimeSeries;
import rescuecore2.log.Logger;

public abstract class Sensor {
	
	public enum Quantity {
		LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL;
	}
	
	private static Random random = new Random();
	private static final double LOG_BUILDING_FRACTION = 0.02; // Fraction of building, that are not on fire, to be logged
	
	private String sensorName;
	private TimeSeries timeSeries;
	private double sample;
	private NoiseFilter noise;
	
	
	protected Sensor(String sensorName, NoiseFilter noise) {
		this.sensorName = sensorName;
		this.noise = noise;
		
		if(USE_EXTENDED_MODES) {
			timeSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE);
		} else {
			timeSeries = null;
		}
	}

	protected abstract double getValue();
	protected abstract int getDistance();
	protected abstract boolean onFire();
	protected abstract double getMaxLimit();
	protected abstract double getMinLimit();
	
	public void sense(int time) {
		double value = getValue();
		sample = noise.generateNoise(value);
		if(sample > getMaxLimit()) {
			sample = getMaxLimit();
		}
		if(sample < getMinLimit()) {
			sample = getMinLimit();
		}
		
		if(timeSeries != null) {
			timeSeries.addSample(sample, time);
		}
		
		/*if("FireSensor".equals(sensorName)) {
			int distance = getDistance();
			if(onFire()) {	
				Logger.info(String.format("%s: t: %d; d: %d; v: %f; s: %f;", 
						"onFire", time, distance, value, sample));
			} else if(random.nextDouble() < LOG_BUILDING_FRACTION){
				Logger.info(String.format("%s: t: %d; d: %d; v: %f; s: %f;", 
						"notOnFire", time, distance, value, sample));
			}
		}
		
		if("WaterSensor".equals(sensorName)) {
			Logger.info(String.format("%s: t: %d; v: %f; s: %f;", 
					"water", time, value, sample));
		}*/
	}
	
	public boolean isLevel(Quantity operation, double level) {
		
		if(timeSeries != null) {
			switch(operation) {
			case GREATER_THAN:
				return timeSeries.getMean().isGreaterThan(level, TS_ALPHA);
			case GREATER_OR_EQUAL:
				return timeSeries.getMean().isGreaterOrEqualTo(level, TS_ALPHA);
			case LESS_THAN:
				return timeSeries.getMean().isLessThan(level, TS_ALPHA);
			case LESS_OR_EQUAL:
				return timeSeries.getMean().isLessOrEqualTo(level, TS_ALPHA);
			default:
				throw new UnsupportedOperationException("Operation " + operation + " not implemented");
			}
		}
		
		switch(operation) {
		case GREATER_THAN:
			return sample > level;
		case GREATER_OR_EQUAL:
			return sample >= level;
		case LESS_THAN:
			return sample < level;
		case LESS_OR_EQUAL:
			return sample <= level;
		default:
			throw new UnsupportedOperationException("Operation " + operation + " not implemented");
		}
	}
}
