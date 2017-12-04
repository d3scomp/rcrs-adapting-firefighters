package cz.cuni.mff.d3s.rcrs.af;

import static rescuecore2.misc.Handy.objectsToIDs;
import static cz.cuni.mff.d3s.rcrs.af.Run.TS_WINDOW_CNT;
import static cz.cuni.mff.d3s.rcrs.af.Run.TS_WINDOW_SIZE;
import static cz.cuni.mff.d3s.rcrs.af.Run.TS_ALPHA;
import static cz.cuni.mff.d3s.rcrs.af.Run.NOISE_MEAN;
import static cz.cuni.mff.d3s.rcrs.af.Run.NOISE_VARIANCE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import cz.cuni.mff.d3s.rcrs.af.modes.Mode;
import cz.cuni.mff.d3s.tss.*;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;
import sample.DistanceSorter;

public class FireFighter extends AbstractSampleAgent<FireBrigade> {
	
	private enum Quantity {
		LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL;
	}
	
	private final String id;
	
	private final boolean useExtendedModes;

	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";

	private final int waterThreshold = 1000;
	
	private int maxWater;
	private int maxDistance;
	private int maxPower;

	private Mode mode = Mode.Search;
	private EntityID target;
	private EntityID closeBurningBuilding;
	
	private TimeSeries waterSeries;
	private NoiseFilter noise;

	public FireFighter(int id, boolean extendedModes) {
		this.id = String.format("FF%d", id);
		useExtendedModes = extendedModes;
		
		if(useExtendedModes) {
			waterSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE);
		} else {
			waterSeries = null;
		}
		
		noise = new NoiseFilter(NOISE_MEAN, NOISE_VARIANCE);
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
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	@Override
	protected void think(int time, ChangeSet changes, Collection<Command> heard) {

		if(useExtendedModes) {
			waterSeries.addSample(getWater(), time);
		}
		
		findCloseBurningBuilding();		
		switchMode();

		// Act
		switch (mode) {
		case Extinguish:
			Logger.info(id + " extinguishing at " + location());
			sendExtinguish(time, closeBurningBuilding, maxPower);
			break;
		case Move:
			Logger.info(id + " moving to target " + target);
			sendMove(time, planShortestRoute(target));
			break;
		case Refill:
			Logger.info(id + " filling with water at " + location());
			sendRest(time);
			break;
		case Search:
			Logger.info(id + " at " + location().getID() + " search towards target " + target);
			sendMove(time, planShortestRoute(target));
			break;
		default:
			Logger.error(id + " in unknown mode " + mode);
			break;

		}

	}

	private void switchMode() {
		boolean modeSwitched = true;

		switch (mode) {
		case Extinguish:
			// Are we out of water?
			if (isWaterLevel(Quantity.LESS_OR_EQUAL, waterThreshold)) {
				// Plan for refill
				List<EntityID> path = planShortestRoute(refugeIDs.toArray(new EntityID[refugeIDs.size()]));
				if(path != null) {
					target = getTarget(path);
					mode = Mode.Move;
				} else {
					target = getTarget(randomWalk());
					mode = Mode.Search;
				}
				break;
			}
			// Not next a building on fire?
			if(closeBurningBuilding == null) {
				// Plan to search fire
				target = getTarget(randomWalk());
				mode = Mode.Search;	
				break;
			}
			// Keep extinguishing
			modeSwitched = false;
			break;
		case Move:
			// Target reached?
			if (location().getID().equals(target)) {
				if (location() instanceof Refuge) {
					// If we are at refuge
					mode = Mode.Refill;
					break;
				}
				// If we reached target different than refuge 
				mode = Mode.Extinguish;
				break;
			}
			// Otherwise keep moving
			modeSwitched = false;
			break;
		case Refill:
			// Are we currently filling with water?
			if (!(location() instanceof Refuge)) {
				Logger.error(id + " misplaced refill at " + location());
				Logger.info(id + " mode fallback.");
				mode = Mode.Search;
				break;
			}
			// Still not full?
			if (isWaterLevel(Quantity.LESS_OR_EQUAL, maxWater - waterThreshold)) {
				modeSwitched = false;
				break;
			}
			// Water full
			Collection<EntityID> buildings = getBurningBuildings();
			List<EntityID> path = planShortestRoute(buildings.toArray(new EntityID[buildings.size()]));
			if (path != null) {
				// Any known burning buildings?
				target = getTarget(path);
				mode = Mode.Move;
			} else {
				// Search burning buildings
				target = getTarget(randomWalk());
				mode = Mode.Search;
			}
			break;
		case Search: {
			// Found burning building?
			if (closeBurningBuilding != null) {
				mode = Mode.Extinguish;
				break;
			}
			// Reached target?
			if (target == null || location().getID().equals(target)) {
				// Plan for new one
				target = getTarget(randomWalk());
			}
			modeSwitched = false;
			break;
		}
		default:
			Logger.error(id + " in unknown mode " + mode);
			mode = Mode.Search;
			break;
		}

		if (modeSwitched) {
			Logger.info(id + " switching to " + mode);
		}

	}
	
	private boolean isWaterLevel(Quantity operation, int level) {
		
		if(useExtendedModes) {
			switch(operation) {
			case GREATER_THAN:
				return waterSeries.getMean().isGreaterThan(level, TS_ALPHA);
			case GREATER_OR_EQUAL:
				return waterSeries.getMean().isGreaterOrEqualTo(level, TS_ALPHA);
			case LESS_THAN:
				return waterSeries.getMean().isLessThan(level, TS_ALPHA);
			case LESS_OR_EQUAL:
				return waterSeries.getMean().isLessOrEqualTo(level, TS_ALPHA);
			default:
				throw new UnsupportedOperationException("Operation " + operation + " not implemented");
			}
		}
		
		switch(operation) {
		case GREATER_THAN:
			return getWater() > level;
		case GREATER_OR_EQUAL:
			return getWater() >= level;
		case LESS_THAN:
			return getWater() < level;
		case LESS_OR_EQUAL:
			return getWater() <= level;
		default:
			throw new UnsupportedOperationException("Operation " + operation + " not implemented");
		}
	}
	
	private int getWater() {
		if(!me().isWaterDefined()) {
			throw new UnsupportedOperationException("Operation getWater() not supported on " + id);
		}
		
		return (int) (me().getWater() + noise.generateNoise());
	}

	private void findCloseBurningBuilding() {
		closeBurningBuilding = null;
		
		// Find all buildings that are on fire
		Collection<EntityID> buildings = getBurningBuildings();
		// Can we extinguish any right now?
		for (EntityID building : buildings) {
			if (model.getDistance(getID(), building) <= maxDistance) {
				closeBurningBuilding = building;
				return;
			}
		}
	}

	private List<EntityID> planShortestRoute(EntityID ... targets) {
		List<EntityID> path = search.breadthFirstSearch(me().getPosition(), targets);
		if (path != null) {
			Logger.info(id + " planed route to " + path.get(path.size() - 1));
			return path;
		} else {
			for (EntityID target : targets) {
				Logger.info(id + " couldn't plan a path to " + target);
			}
			return null;
		}
	}
	
	private EntityID getTarget(List<EntityID> path) {
		return path.get(path.size()-1);
	}
	
	private Collection<EntityID> getBurningBuildings() {
		Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
		List<Building> result = new ArrayList<Building>();
		for (StandardEntity next : e) {
			if (next instanceof Building) {
				Building b = (Building) next;
				if (b.isOnFire()) {
					result.add(b);
				}
			}
		}
		// Sort by distance
		Collections.sort(result, new DistanceSorter(location(), model));
		return objectsToIDs(result);
	}

	private List<EntityID> planPathToFire(EntityID target) {
		// Try to get to anything within maxDistance of the target
		Collection<StandardEntity> targets = model.getObjectsInRange(target, maxDistance);
		if (targets.isEmpty()) {
			return null;
		}
		return search.breadthFirstSearch(me().getPosition(), objectsToIDs(targets));
	}

	@Override
	public String toString() {
		return id;
	}
}
