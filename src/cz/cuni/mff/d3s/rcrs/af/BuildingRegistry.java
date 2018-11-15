package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.AVG_PEOPLE_PER_FLOOR;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.VAR_PEOPLE_PER_FLOOR;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.worldmodel.EntityID;

public class BuildingRegistry {
	
	
	private final Map<EntityID, Building> buildings;
	private final Map<EntityID, Integer> people;
	private final Random random;
	
	private int survivors = 0;
	private int casualties = 0;
	
	public BuildingRegistry() {
		buildings = new HashMap<>();
		people = new HashMap<>();
		random = new Random();
	}
	
	public synchronized void updateBuilding(Building building) {
		if(!buildings.containsKey(building.getID())) {
			int peopleInBuilding = getRandomPeopleOnFloor();
			
			if(building.isFloorsDefined()) {
				for(int i = 1; i < building.getFloors(); i++) {
					peopleInBuilding += getRandomPeopleOnFloor();
				}
			}
			
			people.put(building.getID(), peopleInBuilding);
		}
		
		buildings.put(building.getID(), building);
	}
	
	public int getPeopleInBuilding(Building building) {
		if(people.containsKey(building.getID())) {
			return people.get(building.getID()); 
		}
		
		return 0;
	}
	
	public synchronized void calculate() {
		int all = 0;
		survivors = 0;
		for(Building building : buildings.values()) {
			double multiplier = 1;
			Fieryness fieryness = building.getFierynessEnum();
			if(fieryness == Fieryness.BURNT_OUT) {
				multiplier = 0;
			}
			if(fieryness == Fieryness.INFERNO || fieryness == Fieryness.SEVERE_DAMAGE) {
				multiplier = 0.5;
			}
			
			survivors += multiplier * people.get(building.getID());
			all += people.get(building.getID());
		}
		
		casualties = all - survivors;
	}
	
	public int getSurvivors() {
		return survivors;
	}
	
	public int getCasualties() {
		return casualties;
	}
	
	private int getRandomPeopleOnFloor() {
		return AVG_PEOPLE_PER_FLOOR + (
				(random.nextInt(2*VAR_PEOPLE_PER_FLOOR) - VAR_PEOPLE_PER_FLOOR));
	}

}
