package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.WATER_NOISE_VARIANCE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.WATER_ARIMA_ORDER_P;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.WATER_ARIMA_ORDER_D;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.WATER_ARIMA_ORDER_Q;

import cz.cuni.mff.d3s.rcrs.af.FireFighter;

public class WaterSensor extends Sensor {

	private FireFighter fireFighter;
	
	
	public WaterSensor(FireFighter fireFighter) {
		super(new NoiseFilter(WATER_NOISE_VARIANCE * fireFighter.getMaxWater()),
				WATER_ARIMA_ORDER_P, WATER_ARIMA_ORDER_D, WATER_ARIMA_ORDER_Q);
		this.fireFighter = fireFighter;
	}
	
	@Override
	protected double getValue() {
		return fireFighter.getWater();
	}
	
	@Override
	protected double getMaxLimit() {
		return fireFighter.getMaxWater();
	}
	
	@Override
	protected double getMinLimit() {
		return 0;
	}
}
