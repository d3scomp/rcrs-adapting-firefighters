package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.TIME_SERIES_MODE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_ALPHA;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_CNT;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_SIZE;

import java.util.List;

import cz.cuni.mff.d3s.rcrs.af.Configuration.TimeSeriesMode;
import cz.cuni.mff.d3s.tss.TimeSeries;
import cz.cuni.mff.d3s.tss.arima.Arima;
import cz.cuni.mff.d3s.tss.arima.ArimaOrder;
import cz.cuni.mff.d3s.tss.arima.FittingStrategy;

public abstract class Sensor {
	
	public enum Quantity {
		LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL;
	}
	
	private TimeSeries timeSeries;
	private double sample;
	private NoiseFilter noise;
	
	ArimaOrder arimaOrder;
	
	
	protected Sensor(NoiseFilter noise) {
		this.noise = noise;
		
		switch(TIME_SERIES_MODE) {
		case LR:
			timeSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE);
			break;
		case ARIMA: // TODO
			timeSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE);
			arimaOrder = ArimaOrder.order(1, 1, 1); 
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
			if(TIME_SERIES_MODE == TimeSeriesMode.ARIMA) {
				Arima model = getArimaModel();
				switch(operation) {
				case GREATER_THAN:
					return model.getMean().isGreaterThan(level, TS_ALPHA);
				case GREATER_OR_EQUAL:
					return model.getMean().isGreaterOrEqualTo(level, TS_ALPHA);
				case LESS_THAN:
					return model.getMean().isLessThan(level, TS_ALPHA);
				case LESS_OR_EQUAL:
					return model.getMean().isLessOrEqualTo(level, TS_ALPHA);
				default:
					throw new UnsupportedOperationException("Operation " + operation + " not implemented");
				}
			}
			
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
	
	public double getMean() {
		if(timeSeries != null) {
			if(TIME_SERIES_MODE == TimeSeriesMode.ARIMA) {
				Arima model = getArimaModel();
				return model.getMean().getMean();
			}
			return timeSeries.getMean().getMean();
		}
		
		return sample;
	}
	
	public double getLrb() {
		if(timeSeries != null) {
			if(TIME_SERIES_MODE == TimeSeriesMode.ARIMA) {
				Arima model = getArimaModel();
				return model.getLrb().getMean();
			}
			return timeSeries.getLrb().getMean();
		}
		
		return 0;
	}
	
	public Arima getArimaModel() {
		double[] samples = timeSeries.getSamples();
		List<Integer> times = timeSeries.getTimes();
		cz.cuni.mff.d3s.tss.arima.TimeSeries series = cz.cuni.mff.d3s.tss.arima.TimeSeries.from(samples, times);
		series.setTimePeriod(samples.length);
		
		return new Arima(series, arimaOrder, samples.length, FittingStrategy.CSS, null);
	}
	
	public double getLastSample() {
		return sample;
	}
	
	public double[] getSamples() {
		return timeSeries.getSamples();
	}
}
