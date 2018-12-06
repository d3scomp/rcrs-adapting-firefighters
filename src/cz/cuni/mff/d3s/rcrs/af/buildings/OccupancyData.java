package cz.cuni.mff.d3s.rcrs.af.buildings;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.AVG_RESIDENTIAL_PEOPLE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.VAR_RESIDENTIAL_PEOPLE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.AVG_COMMERCIAL_PEOPLE_PER_FLOOR;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.VAR_COMMERTIAL_PEOPLE_PER_FLOOR;

import java.util.Random;

import cz.cuni.mff.d3s.rcrs.af.buildings.BuildingRegistry.BuildingKind;

public class OccupancyData {

	public static final int BUSINESS_HOUR_START = 8;
	public static final int BUSINESS_HOUR_END = 17;
	public static final int BUSINESS_HOUR_VAR = 1;
	public static final double OFF_HOURS_COEFFICIENT = 0.05;
	public static final double BUSINESS_HOURS_RESIDENTIAL_COEFFICIENT = 0.4;
	public static final double TRANSIENT_HOURS_COEFFICIENT = 0.5;
	public static final int DAY_LENGTH = 24;
	public static final int OBSERVED_NUMBER_OF_DAYS = 7;
	public static final int OBSERVATION_LENGTH = DAY_LENGTH * OBSERVED_NUMBER_OF_DAYS;
	public static final int PREDICTION_LENGTH = DAY_LENGTH;

	private enum HourCategory {
		BUSINESS, TRANSIENT, QUIET;
	}
	
	private final Random random;
	
	
	public OccupancyData() {
		random = new Random();
	}
	
	public int[] generateOccupancy(BuildingKind kind, int floors) {
		int[] occupancy = new int[OBSERVATION_LENGTH];
		for(int i = 0; i < OBSERVATION_LENGTH; i++) {
			occupancy[i] = getRandomPeopleInBuilding(kind, floors, i);
		}
		return occupancy;
	}
	
	private int getRandomPeopleOnFloor(BuildingKind kind, int hour) {
		int avgPeople = 0;
		int varPeople = 0;
		double coefficient = 1;
		HourCategory hourCategory = getHourCategory(hour);
		
		switch(kind) {
		case Commercial:
			avgPeople = AVG_COMMERCIAL_PEOPLE_PER_FLOOR;
			varPeople = VAR_COMMERTIAL_PEOPLE_PER_FLOOR;
			if(hourCategory == HourCategory.TRANSIENT) {
				coefficient = TRANSIENT_HOURS_COEFFICIENT;
			} else if(hourCategory == HourCategory.QUIET) {
				coefficient = OFF_HOURS_COEFFICIENT;
			}
			break;
		case Residential:
			avgPeople = AVG_RESIDENTIAL_PEOPLE;
			varPeople = VAR_RESIDENTIAL_PEOPLE;
			if(hourCategory == HourCategory.TRANSIENT) {
				coefficient = TRANSIENT_HOURS_COEFFICIENT;
			} else if(hourCategory == HourCategory.BUSINESS) {
				coefficient = BUSINESS_HOURS_RESIDENTIAL_COEFFICIENT;
			}
			break;
		default:
			throw new UnsupportedOperationException("Unexpected value of BuildingKind: " + kind);
		}
		
		return (int) (coefficient * (avgPeople + ((random.nextInt(2*varPeople) - varPeople))));
	}
	
	private int getRandomPeopleInBuilding(BuildingKind kind, int floors, int time) {
		int hour = getHourInDay(time);
		int peopleInBuilding = getRandomPeopleOnFloor(kind, hour);
		
		for(int i = 1; i < floors; i++) {
				peopleInBuilding += getRandomPeopleOnFloor(kind, hour);
		}
		
		return peopleInBuilding;
	}
	
	private HourCategory getHourCategory(int hour) {
		if(hour < BUSINESS_HOUR_START - BUSINESS_HOUR_VAR
				|| hour > BUSINESS_HOUR_END + BUSINESS_HOUR_VAR) {
			return HourCategory.QUIET;
		}
		
		if(hour > BUSINESS_HOUR_START + BUSINESS_HOUR_VAR
				&& hour < BUSINESS_HOUR_END - BUSINESS_HOUR_VAR) {
			return HourCategory.BUSINESS;
		}
		
		return HourCategory.TRANSIENT;
	}
	
	private int getHourInDay(int time) {
		return time % DAY_LENGTH;
	}
}
