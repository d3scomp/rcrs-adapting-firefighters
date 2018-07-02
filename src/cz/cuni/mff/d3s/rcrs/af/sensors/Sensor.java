package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_ALPHA;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_CNT;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_SIZE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.USE_EXTENDED_MODES;

import cz.cuni.mff.d3s.tss.TimeSeries;

public abstract class Sensor {
	
	public enum Quantity {
		LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL;
	}
	
	private TimeSeries timeSeries;
	private double sample;
	private NoiseFilter noise;
	
	
	protected Sensor(NoiseFilter noise) {
		this.noise = noise;
		
		if(USE_EXTENDED_MODES) {
			timeSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE);
		} else {
			timeSeries = null;
		}
	}

	protected abstract double getValue();
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
