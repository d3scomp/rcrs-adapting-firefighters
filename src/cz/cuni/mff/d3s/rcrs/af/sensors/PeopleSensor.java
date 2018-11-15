package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.PEOPLE_NOISE_VARIANCE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.AVG_PEOPLE_PER_FLOOR;

import cz.cuni.mff.d3s.rcrs.af.BuildingRegistry;
import rescuecore2.standard.entities.Building;

public class PeopleSensor extends Sensor {

	private BuildingRegistry buildings;
	public final Building building;
	
	public PeopleSensor(BuildingRegistry buildingRegistry, Building building) {
		super(new NoiseFilter(PEOPLE_NOISE_VARIANCE * AVG_PEOPLE_PER_FLOOR),
				0, 0, 0);
		this.buildings = buildingRegistry;
		this.building = building;
	}
	
	@Override
	protected double getValue() {
		return buildings.getPeopleInBuilding(building);
	}

	@Override
	protected double getMaxLimit() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected double getMinLimit() {
		return 0;
	}
	
	

}
