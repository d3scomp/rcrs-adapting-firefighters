package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.TIME_SERIES_MODE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_ALPHA;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_CNT;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_SIZE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.ARIMA_FORECAST_LENGTH;

import cz.cuni.mff.d3s.tss.TimeSeries;

public abstract class Sensor {
	
	public enum Quantity {
		LESS_THAN, GREATER_THAN;
	}
	
	private TimeSeries timeSeries;
	private double sample;
	private NoiseFilter noise;	
	
	protected Sensor(NoiseFilter noise, int arimaP, int arimaD, int arimaQ) {
		this.noise = noise;
		
		switch(TIME_SERIES_MODE) {
		case LR:
			timeSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE, arimaP, arimaD, arimaQ);
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
				return timeSeries.afAbove(level, ARIMA_FORECAST_LENGTH, TS_ALPHA);
			case LESS_THAN:
				return timeSeries.afBelow(level, ARIMA_FORECAST_LENGTH, TS_ALPHA);
			default:
				throw new UnsupportedOperationException("Operation " + operation + " not implemented");
			}
		}
		
		switch(operation) {
		case GREATER_THAN:
			return sample > level;
		case LESS_THAN:
			return sample < level;
		default:
			throw new UnsupportedOperationException("Operation " + operation + " not implemented");
		}
	}
	
	public double getMean() {
		if(timeSeries != null) {
			return timeSeries.getMean().getMean();
		}
		
		return sample;
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
		return sample;
	}
}
