package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.TIME_SERIES_MODE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_ALPHA;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_CNT;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_SIZE;

import cz.cuni.mff.d3s.tss.TimeSeries;

public abstract class Sensor {
	
	public enum Quantity {
		LESS_THAN, LESS_OR_EQUAL_TO,
		GREATER_THAN, GREATER_OR_EQUAL_TO;
	}
	
	private TimeSeries timeSeries;
	//private double sample;
	private MovingAverage samples;
	private int lastSampleTime;
	private NoiseFilter noise;
	
	protected Sensor(NoiseFilter noise) {
		this.noise = noise;
		samples = new MovingAverage();
		lastSampleTime = 0;
		
		switch(TIME_SERIES_MODE) {
		case On:
			timeSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE);
			break;
		default:
			timeSeries = null;
		}
	}

	protected abstract double getValue();
	protected abstract double getMaxLimit();
	protected abstract double getMinLimit();
	
	public void sense(int time) {
		double value = getValue();
		lastSampleTime = time;
		
		double sample = noise.generateNoise(value);
		if(sample > getMaxLimit()) {
			sample = getMaxLimit();
		}
		if(sample < getMinLimit()) {
			sample = getMinLimit();
		}
		
		if(timeSeries != null) {
			timeSeries.addSample(sample, time);
		}
		
		samples.addValue(sample);
	}
	
	public boolean isLevel(Quantity operation, double level) {

		if(timeSeries != null) {		
			switch(operation) {
			case GREATER_THAN:
				return timeSeries.getLr(lastSampleTime).isGreaterThan(level, TS_ALPHA);
			case GREATER_OR_EQUAL_TO:
				return timeSeries.getLr(lastSampleTime).isGreaterOrEqualTo(level, TS_ALPHA);
			case LESS_THAN:
				return timeSeries.getLr(lastSampleTime).isLessThan(level, TS_ALPHA);
			case LESS_OR_EQUAL_TO:
				return timeSeries.getLr(lastSampleTime).isLessOrEqualTo(level, TS_ALPHA);
			default:
				throw new UnsupportedOperationException("Operation " + operation + " not implemented");
			}
		}
		
		switch(operation) {
		case GREATER_THAN:
			return samples.getAverage() > level;
		case GREATER_OR_EQUAL_TO:
			return samples.getAverage() >= level;
		case LESS_THAN:
			return samples.getAverage() < level;
		case LESS_OR_EQUAL_TO:
			return samples.getAverage() <= level;
		default:
			throw new UnsupportedOperationException("Operation " + operation + " not implemented");
		}
	}
		
	public double getMean() {
		if(timeSeries != null) {
			return timeSeries.getMean().getMean();
		}
		
		return samples.getAverage();
	}
	
	public boolean isLrbAbove(double threshold) {
		if(timeSeries != null) {
			return timeSeries.getLrb().isGreaterThan(threshold, TS_ALPHA);
		}
		
		return false;
	}
	
	public boolean isLrbBelow(double threshold) {
		if(timeSeries != null) {
			return timeSeries.getLrb().isLessThan(threshold, TS_ALPHA);
		}
		
		return false;
	}
	
	public double getLastSample() {
		return samples.getAverage();
	}
}
