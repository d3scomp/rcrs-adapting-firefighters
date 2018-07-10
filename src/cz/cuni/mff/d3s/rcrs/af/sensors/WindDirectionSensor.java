package cz.cuni.mff.d3s.rcrs.af.sensors;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.entities.World;

public class WindDirectionSensor extends Sensor {

	private StandardWorldModel model;
	
	
	public WindDirectionSensor(StandardWorldModel model) {
		super(new NoiseFilter(0));
		this.model = model;
	}
	
	@Override
	protected double getValue() {
		for(StandardEntity entity : model.getEntitiesOfType(StandardEntityURN.WORLD)) {
			World world = (World) entity;
			return world.getWindDirection();
		}
		
		return 0;
	}
	
	@Override
	protected double getMaxLimit() {
		return 360;
	}
	
	@Override
	protected double getMinLimit() {
		return 0;
	}
}
