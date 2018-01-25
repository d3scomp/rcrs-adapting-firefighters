package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Run.NOISE_MEAN;
import static cz.cuni.mff.d3s.rcrs.af.Run.NOISE_VARIANCE;
import static cz.cuni.mff.d3s.rcrs.af.Run.TS_ALPHA;
import static cz.cuni.mff.d3s.rcrs.af.Run.TS_WINDOW_CNT;
import static cz.cuni.mff.d3s.rcrs.af.Run.TS_WINDOW_SIZE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.modes.Mode;
import cz.cuni.mff.d3s.tss.TimeSeries;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;

public class FireFighter extends AbstractSampleAgent<FireBrigade> {
	
	private enum Quantity {
		LESS_THAN, LESS_OR_EQUAL, GREATER_THAN, GREATER_OR_EQUAL;
	}
	
	private final String id;
	
	private final boolean useExtendedModes;

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
	
	private List<EntityID> burningBuildings = Collections.emptyList();

	private final int waterThreshold = 1000; // TODO: Move to config	
	private TimeSeries waterSeries;
	private NoiseFilter waterNoise;

	StandardEntity[] roads;
	Set<EntityID> refillStations = new HashSet<>();
	
	// ########################################################################

	public FireFighter(int id, boolean extendedModes) {
		this.id = String.format("FF%d", id);
		useExtendedModes = extendedModes; // TODO: move to config
		
		if(useExtendedModes) {
			waterSeries = new TimeSeries(TS_WINDOW_CNT, TS_WINDOW_SIZE);
		} else {
			waterSeries = null;
		}
		
		waterNoise = new NoiseFilter(NOISE_MEAN, NOISE_VARIANCE);
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
		
		// Store refill stations
		for (StandardEntity next : model) {
            if (next instanceof Hydrant || next instanceof Refuge) {
            	refillStations.add(next.getID());
            }
        }
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	@Override
	protected void think(int time, ChangeSet changes, Collection<Command> heard) {
		long startTime = System.nanoTime();
		
		findBurningBuildings();
		
		if(useExtendedModes) {
			waterSeries.addSample(getWater(), time);
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
			if (isWaterLevel(Quantity.LESS_OR_EQUAL, waterThreshold)) {
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
			if (isWaterLevel(Quantity.LESS_THAN, maxWater - waterThreshold)) {
				modeSwitched = false;
				break;
			}
			// Water full
			if (burningBuildings.size() > 0) {
				// Any known burning buildings?
				List<EntityID> path = planShortestRoute(burningBuildings.toArray(new EntityID[burningBuildings.size()]));
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
		
		return (int) (me().getWater() + waterNoise.generateNoise());
	}
	
	private EntityID randomTarget() {
		return roads[random.nextInt(roads.length)].getID();
	}
	
	private void findBurningBuildings() {
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
		List<EntityID> resultIds = new ArrayList<EntityID>();
		for (Entity next : result) {
			resultIds.add(next.getID());
		}
		burningBuildings = resultIds;
	}

	public EntityID findCloseBurningBuilding() {
		ArrayList<EntityID> closeBuildings = new ArrayList<>();
		// Can we extinguish any right now?
		for (EntityID building : burningBuildings) {
			if (model.getDistance(getID(), building) < maxDistance*0.8) {
				closeBuildings.add(building);
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
