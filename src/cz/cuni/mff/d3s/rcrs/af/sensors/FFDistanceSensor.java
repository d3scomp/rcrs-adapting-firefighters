package cz.cuni.mff.d3s.rcrs.af.sensors;

import cz.cuni.mff.d3s.rcrs.af.FireStation;
import rescuecore2.worldmodel.EntityID;

public class FFDistanceSensor extends Sensor {

	private final FireStation fireStation;
	public final EntityID fireFighter1;
	public final EntityID fireFighter2;
	
	
	public FFDistanceSensor(FireStation fireStation, EntityID fireFighter1, EntityID fireFighter2) {
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
