package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.FIRE_PROBABILITY_THRESHOLD;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.WATER_THRESHOLD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.modes.Mode;
import cz.cuni.mff.d3s.rcrs.af.sensors.FireSensor;
import cz.cuni.mff.d3s.rcrs.af.sensors.Sensor.Quantity;
import cz.cuni.mff.d3s.rcrs.af.sensors.WaterSensor;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;

public class FireFighter extends AbstractSampleAgent<FireBrigade> {
	
	
	private final String id;
	
	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
	
	private int maxWater;
	private int maxDistance;
	private int maxPower;

	private Mode mode = Mode.Search;

	private EntityID searchTarget = null;
	private EntityID fireTarget = null;
	private EntityID refillTarget = null;
	
	private Map<Building, FireSensor> fireSensor;
	private WaterSensor waterSensor;

	StandardEntity[] roads;
	Set<EntityID> refillStations = new HashSet<>();
	
	// ########################################################################

	public FireFighter(int id) {
		this.id = String.format("FF%d", id);
		maxWater = -1;
		fireSensor = new HashMap<>();
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE, StandardEntityURN.HYDRANT,
				StandardEntityURN.GAS_STATION);
		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);
		Logger.info(toString() + " connected: max extinguish distance = " + maxDistance + ", max power = " + maxPower
				+ ", max tank = " + maxWater);
		
		Collection<StandardEntity> entities = model.getEntitiesOfType(StandardEntityURN.ROAD);
		roads = entities.toArray(new StandardEntity[entities.size()]);
		
		// Store refill stations and create fire sensors
		for (StandardEntity entity : model) {
            if (entity instanceof Hydrant || entity instanceof Refuge) {
            	refillStations.add(entity.getID());
            }
            if (entity instanceof Building) {
            	fireSensor.put((Building) entity, new FireSensor(this, (Building) entity));
            }
        }
		
		waterSensor = new WaterSensor(this);
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	@Override
	protected void think(int time, ChangeSet changes, Collection<Command> heard) {
		long startTime = System.nanoTime();
		
		waterSensor.sense(time);
		for(Building building : fireSensor.keySet()) {
			fireSensor.get(building).sense(time);
		}
			
		switchMode(time);

		// Act
		switch (mode) {
		case Search:
			Logger.info(formatLog(time, "searching"));
			if (searchTarget == null) {
				searchTarget = randomTarget();
			} else if (searchTarget.equals(location().getID())) {
				searchTarget = randomTarget();
			}
			Logger.info(formatLog(time, "moving towards search target: " + searchTarget.getValue()));
			sendMove(time, planShortestRoute(searchTarget));
			break;
		case MoveToFire:
			if (fireTarget != null) {
				Logger.info(formatLog(time, "moving towards fire target: " + fireTarget.getValue()));
				sendMove(time, planShortestRoute(fireTarget));
			} else {
				Logger.error(formatLog(time, " in MoveToFireMode missing fireTarget"));
			}
			break;
		case MoveToRefill:
			if (refillTarget != null) {
				Logger.info(formatLog(time, "moving towards refill target: " + refillTarget.getValue()));
				sendMove(time, planShortestRoute(refillTarget));
			} else {
				Logger.error(formatLog(time, "in MoveToRefillMode missing refillTarget"));
			}
			break;
		case Extinguish:
			EntityID building = findCloseBurningBuilding();
			if (building != null) {
				Logger.info(formatLog(time, "extinguishing(" + building.getValue() + ")[" + getWater() + "]"));
				sendExtinguish(time, building, maxPower);
			} else {
				Logger.warn(formatLog(time, "Trying to extinguish null."));
			}
			break;
		case Refill:
			if(!atRefill()) {
				Logger.error(formatLog(time, "Trying to refill at wrong place " + location().getID().getValue()));
			} else {
				Logger.info(formatLog(time, "refilling(" + getWater() + ")"));
			}
			sendRest(time);
			break;
		
		default:
			Logger.error(id + " in unknown mode " + mode);
			break;

		}
		
		long duration = System.nanoTime() - startTime;
		Logger.info(formatLog(time, "thinking took " + duration/1000000 + " ms"));
	}

	private void switchMode(int time) {
		boolean modeSwitched = true;

		switch (mode) {
		case Extinguish:
			// Are we out of water?
			if (waterSensor.isLevel(Quantity.LESS_OR_EQUAL, WATER_THRESHOLD)) {
				// Plan for refill
				List<EntityID> path = planShortestRoute(refillStations.toArray(new EntityID[refillStations.size()]));
				if(path != null) {
					refillTarget = getTarget(path);
					mode = Mode.MoveToRefill;
				} else {
					Logger.error(formatLog(time, "no refill station reachable"));
				}
				break;
			}
			// Not next a building on fire?
			if(findCloseBurningBuilding() == null) {
				// Plan to search fire
				searchTarget = randomTarget();
				mode = Mode.Search;
				break;
			}
			// Keep extinguishing
			modeSwitched = false;
			break;
		case MoveToFire:
			// Target reached?
			if(fireTarget == null || fireTarget.equals(location().getID())) { 
				mode = Mode.Extinguish;
				break;
			}
			// Otherwise keep moving
			modeSwitched = false;
			break;
		case MoveToRefill:
			// Target reached?
			if(refillTarget == null || refillTarget.equals(location().getID())) {
				mode = Mode.Refill;
				break;
			}
			// Otherwise keep moving
			modeSwitched = false;
			break;
		case Refill:
			// Still not full?
			if (waterSensor.isLevel(Quantity.LESS_THAN, maxWater - WATER_THRESHOLD)) {
				modeSwitched = false;
				break;
			}
			// Water full
			EntityID burningBuilding = findCloseBurningBuilding();
			if (burningBuilding != null) {
				// Any known burning buildings?
				List<EntityID> path = planShortestRoute(burningBuilding);
				if(path != null) {
					fireTarget = getTarget(path);
					mode = Mode.MoveToFire;
				} else {
					Logger.error(formatLog(time, "no burning building reachable"));
				}
			} else {
				// Search burning buildings
				searchTarget = randomTarget();
				mode = Mode.Search;
			}
			break;
		case Search: {
			// Found burning building?
			if (findCloseBurningBuilding() != null) {
				mode = Mode.Extinguish;
				break;
			}
			modeSwitched = false;
			break;
		}
		default:
			Logger.error(formatLog(time, "in unknown mode " + mode));
			mode = Mode.Search;
			break;
		}

		if (modeSwitched) {
			Logger.info(formatLog(time, "switching to " + mode));
		}

	}
	
	// Sensor readings ////////////////////////////////////////////////////////
	
	public double getMaxWater() {
		if(maxWater == -1) {
			throw new RuntimeException("The maxWater variable is not initialized. Calling getFFMaxWater() before postConnect()?");
		}
		
		return maxWater;
	}
	
	public double getWater() {
		return me().getWater();
	}
	
	public int getBuildingDistance(Building building) {
		return model.getDistance(building, location());
	}
	
	public boolean isBuildingOnFire(Building building) {
		return building.isOnFire();
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	
	private EntityID randomTarget() {
		return roads[random.nextInt(roads.length)].getID();
	}

	public EntityID findCloseBurningBuilding() {
		ArrayList<EntityID> closeBuildings = new ArrayList<>();
		// Can we extinguish any right now?
		for (Building building : fireSensor.keySet()) {
			if (model.getDistance(location(), building) < maxDistance*0.8
					&& fireSensor.get(building).isLevel(Quantity.GREATER_OR_EQUAL, FIRE_PROBABILITY_THRESHOLD)) {
				closeBuildings.add(building.getID());
			}
		}
		if(closeBuildings.size() == 0) {
			return null;
		}
		return closeBuildings.get(random.nextInt(closeBuildings.size()));
	}

	private List<EntityID> planShortestRoute(EntityID... targets) {
		List<EntityID> path = search.breadthFirstSearch(me().getPosition(), targets);
		if (path != null) {
			Logger.info(formatLog(0, "planed route to " + path.get(path.size() - 1)));
			return path;
		} else {
			for (EntityID target : targets) {
				Logger.warn(formatLog(0, "couldn't plan a path to " + target));
				;
			}
			return null;
		}
	}
	
	private EntityID getTarget(List<EntityID> path) {
		return path.get(path.size() - 1);
	}
	
	public boolean atRefill() {
		StandardEntity here = location();
		return here instanceof Hydrant || here instanceof Refuge;
	}

	private String formatLog(int time, String msg) {
		return String.format("T[%d] L[%s] %s %s", time, location(), id, msg);
	}
	
	@Override
	public String toString() {
		return id;
	}
}
