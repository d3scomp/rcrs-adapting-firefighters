package cz.cuni.mff.d3s.rcrs.af.sensors;

import cz.cuni.mff.d3s.rcrs.af.FireStation;
import cz.cuni.mff.d3s.rcrs.af.components.FFComponent;

public class FFDistanceSensor extends Sensor {

	private final FireStation fireStation;
	public final FFComponent fireFighter1;
	public final FFComponent fireFighter2;
	
	
	public FFDistanceSensor(FireStation fireStation, FFComponent fireFighter1, FFComponent fireFighter2) {
		super(new NoiseFilter(0));
		this.fireStation = fireStation;
		this.fireFighter1 = fireFighter1;
		this.fireFighter2 = fireFighter2;
	}
	
	@Override
	protected double getValue() {
		double distance = fireStation.getFFDistance(fireFighter1, fireFighter2);
		
		return distance;
	}
	
	@Override
	protected double getMaxLimit() {
		return Double.POSITIVE_INFINITY;
	}
	
	@Override
	protected double getMinLimit() {
		return 0;
	}
}
