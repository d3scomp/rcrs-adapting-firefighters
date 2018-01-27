package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.WATER_NOISE_VARIANCE;

import cz.cuni.mff.d3s.rcrs.af.FireFighter;
import cz.cuni.mff.d3s.rcrs.af.NoiseFilter;

public class WaterSensor extends Sensor {

	private FireFighter fireFighter;
	
	
	public WaterSensor(FireFighter fireFighter) {
		super(new NoiseFilter(WATER_NOISE_VARIANCE * fireFighter.getMaxWater()));
		this.fireFighter = fireFighter;
	}
	
	@Override
	protected double getValue() {
		return fireFighter.getWater();
	}
	
	
}