package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.TIME_SERIES_MODE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_ALPHA;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_CNT;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.TS_WINDOW_SIZE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.ARIMA_FORECAST_LENGTH;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.ARIMA_CONFIDENCE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.ARIMA_ORDER_P;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.ARIMA_ORDER_D;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.ARIMA_ORDER_Q;

import cz.cuni.mff.d3s.rcrs.af.Configuration.TimeSeriesMode;
import cz.cuni.mff.d3s.tss.TimeSeries;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

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
		case ARIMA:
			timeSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE);
			arimaOrder = ArimaOrder.order(ARIMA_ORDER_P, ARIMA_ORDER_D, ARIMA_ORDER_Q); 
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
				Forecast forecast = getArimaForecast();
				switch(operation) {
				case GREATER_THAN:
					return forecast.pointEstimates().mean() > level;
				case GREATER_OR_EQUAL:
					return forecast.pointEstimates().mean() >= level;
				case LESS_THAN:
					return forecast.pointEstimates().mean() < level;
				case LESS_OR_EQUAL:
					return forecast.pointEstimates().mean() <= level;
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
				Forecast forecast = getArimaForecast();
				return forecast.pointEstimates().mean();
			}
			return timeSeries.getMean().getMean();
		}
		
		return sample;
	}
	
	public double getLrb() {
		if(timeSeries != null) {
			if(TIME_SERIES_MODE == TimeSeriesMode.ARIMA) {
				Forecast forecast = getArimaForecast();
				return computeLrbMean(forecast);
			}
			return timeSeries.getLrb().getMean();
		}
		
		return 0;
	}
	
	public Forecast getArimaForecast() {
		double[] samples = timeSeries.getSamples();
		com.github.signaflo.timeseries.TimeSeries series = com.github.signaflo.timeseries.TimeSeries.from(samples);
		
		Arima model = Arima.model(series, arimaOrder);
		return model.forecast(ARIMA_FORECAST_LENGTH, ARIMA_CONFIDENCE);
	}
	
	private double computeLrbMean(Forecast forecast) {
		com.github.signaflo.timeseries.TimeSeries series = forecast.pointEstimates();
		if (series.size() <= 0) {
			return Double.NaN;
		}
		double totalTimeSum = 0;
		double totalTimeSquaresSum = 0;
		double totalSampleTimeSum = 0;
		for(int i = 0; i < series.size(); i++) {
			totalTimeSum += i;
			totalTimeSquaresSum += i*i;
			totalSampleTimeSum = i*series.at(i);
		}
				
		double x = totalTimeSum;
		double x2 = totalTimeSquaresSum;
		double y = series.sum();
		double xy = totalSampleTimeSum;

		double nom = xy - x*y / series.size();
		double denom = x2 - x*x / series.size();

		return denom != 0 ? nom / denom : Double.NaN;
	}
	
	public double getLastSample() {
		return sample;
	}
}
