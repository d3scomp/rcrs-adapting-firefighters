package cz.cuni.mff.d3s.rcrs.af.buildings;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.PEOPLE_ARIMA_ORDER_D;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.PEOPLE_ARIMA_ORDER_P;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.PEOPLE_ARIMA_ORDER_Q;
import static cz.cuni.mff.d3s.rcrs.af.buildings.OccupancyData.OBSERVATION_LENGTH;
import static cz.cuni.mff.d3s.rcrs.af.buildings.OccupancyData.PREDICTION_LENGTH;

import java.util.HashMap;
import java.util.Map;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.worldmodel.EntityID;

public class BuildingRegistry {
	
	public enum BuildingKind {
		Residential, Commercial;
	}
	
	private class BuildingBundle {
		public Building building;
		public final int[] people;
		public final BuildingKind kind;
		
		public BuildingBundle(Building building, BuildingKind kind, int[] people) {
			this.building = building;
			this.kind = kind;
			this.people = people;
		}
		
		@Override
		public int hashCode() {
			return building.getID().hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof BuildingBundle)) {
				return false;
			}
			
			BuildingBundle other = (BuildingBundle) obj;
			return this.building.equals(other.building);
		}
		
	}
	
	private final Map<EntityID, BuildingBundle> buildings;
	private final OccupancyData occupancyData;
	private final OccupancyModel residentialOccupancyModel;
	private final Map<Integer, OccupancyModel> businessOccupancyModels;
	private final int hour;
	
	private int survivors = 0;
	private int casualties = 0;
	
	public BuildingRegistry(int hour) {
		buildings = new HashMap<>();
		occupancyData = new OccupancyData();
		residentialOccupancyModel = new OccupancyModel();
		businessOccupancyModels = new HashMap<>();
		this.hour = hour;
	}
	
	public void registerBuilding(Building building) {
		int floors = getFloors(building);
		BuildingKind kind = getBuildingKind(building);
		int[] people = occupancyData.generateOccupancy(kind, floors);
		
		if(kind == BuildingKind.Residential) {
			residentialOccupancyModel.addTrainingData(people);
		} else {
			if(!businessOccupancyModels.containsKey(floors)) {
				businessOccupancyModels.put(floors, new OccupancyModel());
			}
			businessOccupancyModels.get(floors).addTrainingData(people);
		}
		
		buildings.put(building.getID(), new BuildingBundle(building, kind, people));
	}
	
	public synchronized void updateBuilding(Building building) {
		buildings.get(building.getID()).building = building;
	}
	
	public void trainModels() {
		residentialOccupancyModel.trainModel(PEOPLE_ARIMA_ORDER_P, PEOPLE_ARIMA_ORDER_D, PEOPLE_ARIMA_ORDER_Q);
		for(int key : businessOccupancyModels.keySet()) {
			businessOccupancyModels.get(key).trainModel(PEOPLE_ARIMA_ORDER_P, PEOPLE_ARIMA_ORDER_D, PEOPLE_ARIMA_ORDER_Q);
		}
	}
	
	public int predictPeopleInBuilding(Building building) {
		BuildingBundle bundle = buildings.get(building.getID());
		if(bundle.kind == BuildingKind.Residential) {
			return (int) residentialOccupancyModel.getModel().getForecastValue(hour);
		}
		
		int floors = getFloors(building);
		if(!businessOccupancyModels.containsKey(floors)) {
			throw new UnsupportedOperationException("Model missing for " + floors + " floor buildings.");
		}
		
		return (int) businessOccupancyModels.get(floors).getModel().getForecastValue(hour);
	}
	
	public void calculate() {
		int all = 0;
		survivors = 0;
		for(BuildingBundle bundle : buildings.values()) {
			double multiplier = 1;
			Fieryness fieryness = bundle.building.getFierynessEnum();
			if(fieryness == Fieryness.BURNT_OUT) {
				multiplier = 0;
			}
			if(fieryness == Fieryness.INFERNO || fieryness == Fieryness.SEVERE_DAMAGE) {
				multiplier = 0.5;
			}
			
			survivors += multiplier * bundle.people[hour];
			all += bundle.people[hour];
		}
		
		casualties = all - survivors;
	}
	
	public int getSurvivors() {
		return survivors;
	}
	
	public int getCasualties() {
		return casualties;
	}
	
	public String dumpBuildings() {
		StringBuilder builder = new StringBuilder();
		builder.append("Id\tkind\tpeople\tfieryness\n");
		
		for(BuildingBundle bundle : buildings.values()) {
			builder.append(bundle.building.getID().getValue())
				   .append("\t")
				   .append(bundle.kind)
				   .append("\t")
				   .append(bundle.people[hour])
				   .append("\t")
				   .append(bundle.building.getFierynessEnum())
				   .append("\n");
		}
		
		builder.append("\nhour: " + hour + "\n");
		
		return builder.toString();
	}

	public String dumpModels() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("point estimates\n");
		
		double[] residentialForecast = residentialOccupancyModel.getModel().getForecast(PREDICTION_LENGTH);
		Map<Integer, double[]> businessForecast = new HashMap<>();
		for(int floor : businessOccupancyModels.keySet()) {
			businessForecast.put(floor, businessOccupancyModels.get(floor).getModel().getForecast(PREDICTION_LENGTH));
		}
		builder.append(dumpModel(residentialForecast, businessForecast));

		builder.append("lower bound\n");
		
		residentialForecast = residentialOccupancyModel.getModel().getForecastLowerBound(PREDICTION_LENGTH);
		businessForecast = new HashMap<>();
		for(int floor : businessOccupancyModels.keySet()) {
			businessForecast.put(floor, businessOccupancyModels.get(floor).getModel().getForecastLowerBound(PREDICTION_LENGTH));
		}
		builder.append(dumpModel(residentialForecast, businessForecast));

		builder.append("upper bound\n");
		
		residentialForecast = residentialOccupancyModel.getModel().getForecastUpperBound(PREDICTION_LENGTH);
		businessForecast = new HashMap<>();
		for(int floor : businessOccupancyModels.keySet()) {
			businessForecast.put(floor, businessOccupancyModels.get(floor).getModel().getForecastUpperBound(PREDICTION_LENGTH));
		}
		builder.append(dumpModel(residentialForecast, businessForecast));
		
		return builder.toString();
	}
	
	private String dumpModel(double[] residentialForecast, Map<Integer, double[]> businessForecast) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Time\tResidential");
		for(int floor : businessForecast.keySet()) {
			builder.append("\tCommercial_" + floor);
		}
		builder.append("\n");
		
		for(int i = 0; i < PREDICTION_LENGTH; i++) {
			builder.append(i).append("\t")
				   .append(residentialForecast[i]);
			for(int key : businessForecast.keySet()) {
				builder.append("\t").append(businessForecast.get(key)[i]);
			}
			builder.append("\n");
		}
		
		return builder.toString();
	}
	
	public String dumpPeople() {
		StringBuilder builder = new StringBuilder();
		builder.append("Time\tResidential");
		for(int floor : businessOccupancyModels.keySet()) {
			builder.append("\tCommercial_" + floor);
		}
		builder.append("\n");
		
		double[] residentialAvg = residentialOccupancyModel.getAveragedTrainingData();
		Map<Integer, double[]> businessAvg = new HashMap<>();
		for(int floor : businessOccupancyModels.keySet()) {
			businessAvg.put(floor, businessOccupancyModels.get(floor).getAveragedTrainingData());
		}
		
		for(int i = 0; i < OBSERVATION_LENGTH; i++) {
			builder.append(i).append("\t")
				   .append(residentialAvg[i]);
			for(int key : businessAvg.keySet()) {
				builder.append("\t").append(businessAvg.get(key)[i]);
			}
			builder.append("\n");
		}
		
		return builder.toString();
	}
	
	/*private BuildingKind getRandomBuildingKind() {
		if(random.nextBoolean()) {
			return BuildingKind.Residential;
		}
		
		return BuildingKind.Commercial;
	}*/
	
	private BuildingKind getBuildingKind(Building building) {
		if(getFloors(building) > 1) {
			return BuildingKind.Commercial;
		}
		
		return BuildingKind.Residential;
	}

	private int getFloors(Building building) {
		if(building.isFloorsDefined()) {
			return building.getFloors();
		}
		return 1;
	}
}
