package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.TIME_SERIES_MODE;

import java.util.Comparator;

import cz.cuni.mff.d3s.rcrs.af.Configuration.TimeSeriesMode;
import cz.cuni.mff.d3s.rcrs.af.buildings.BuildingRegistry;
import rescuecore2.standard.entities.Building;


public class PeopleSensor implements Comparator<Building> {

	private final BuildingRegistry buildingRegistry;
	
	public PeopleSensor(BuildingRegistry buildingRegistry) {
		this.buildingRegistry = buildingRegistry;
	}
	
	public void updateBuilding(Building building) {
		buildingRegistry.updateBuilding(building);
	}

	public int getPredictedPeople(Building building) {
		if(TIME_SERIES_MODE == TimeSeriesMode.On) {
			return buildingRegistry.predictPeopleInBuilding(building);
		}
		return 0;
	}
	
	@Override
	public int compare(Building b1, Building b2) {
		if(TIME_SERIES_MODE == TimeSeriesMode.On) {
			int p1 = buildingRegistry.predictPeopleInBuilding(b1);
			int p2 = buildingRegistry.predictPeopleInBuilding(b2);
			
			return p1 - p2;
		}
		
		return 0;
	}
}
