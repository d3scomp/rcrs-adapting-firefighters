package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.FIRE_NOISE_VARIANCE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.FIRE_MAX_DETECTABLE_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.FIRE_UNERRING_DETECTABLE_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.FIRE_MAX_DISTANCE_DETECTABILITY;

import cz.cuni.mff.d3s.rcrs.af.FireFighter;
import cz.cuni.mff.d3s.rcrs.af.NoiseFilter;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Building;

public class FireSensor extends Sensor {

	private FireFighter fireFighter;
	public final Building building;
	
	private double slope;
	private double offset;
	
	public FireSensor(FireFighter fireFighter, Building building) {
		super(new NoiseFilter(FIRE_NOISE_VARIANCE));
		this.fireFighter = fireFighter;
		this.building = building;
		
		// Calculate the linear detectability function
		// slope = (y2 - y1)/(x2 - x1)
		slope = (FIRE_MAX_DISTANCE_DETECTABILITY - 1)/(FIRE_MAX_DETECTABLE_DISTANCE - FIRE_UNERRING_DETECTABLE_DISTANCE);
		// offset = y - slope*x
		offset = 1 - slope*FIRE_UNERRING_DETECTABLE_DISTANCE;
	}
	
	@Override
	protected double getValue() {
		boolean isOnFire = fireFighter.isBuildingOnFire(building);
		int distance = fireFighter.getBuildingDistance(building);
		
		if(isOnFire) {
			if(distance <= FIRE_UNERRING_DETECTABLE_DISTANCE) {
				return 1;
			}
			if(distance > FIRE_MAX_DETECTABLE_DISTANCE) {
				return 0;
			}
			double probability = slope*distance + offset;
			Logger.info(String.format("Distance: %d Fire: %f", distance, probability));
			// Linear detectability with respect to the distance
			return probability;
			
		} else {
			return 0;
		}
	}
}
