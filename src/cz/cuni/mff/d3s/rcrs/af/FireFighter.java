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

import cz.cuni.mff.d3s.rcrs.af.comm.BuildingsMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.comm.RefillMsg;
import cz.cuni.mff.d3s.rcrs.af.modes.Mode;
import cz.cuni.mff.d3s.rcrs.af.sensors.FireSensor;
import cz.cuni.mff.d3s.rcrs.af.sensors.Sensor.Quantity;
import cz.cuni.mff.d3s.rcrs.af.sensors.WaterSensor;
import cz.cuni.mff.d3s.rcrs.af.sensors.WindDirectionSensor;
import cz.cuni.mff.d3s.rcrs.af.sensors.WindSpeedSensor;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;

public class FireFighter extends AbstractSampleAgent<FireBrigade> {

	private static final boolean LOG_FIRE_FIGHTER = true;

	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";

	private final int CHANNEL_IN = 2;
	private final int CHANNEL_OUT = 1;

	private int maxWater;
	private int maxDistance;
	private int maxPower;

	private Map<Building, FireSensor> fireSensor;
	private WaterSensor waterSensor;
	private WindDirectionSensor windDirectionSensor;
	private WindSpeedSensor windSpeedSensor;

	StandardEntity[] roads;
	Set<EntityID> refillStations = new HashSet<>();

	// Knowledge ##############################################################

	private final String sid;
	
	private final int id;
	public final static String KNOWLEDGE_ID = "id";
	// eid; getID()
	public final static String KNOWLEDGE_ENTITY_ID = "eid";

	private EntityID fireTarget;
	public final static String KNOWLEDGE_FIRE_TARGET = "fireTarget";

	private EntityID helpTarget; // updated by ensemble
	public final static String KNOWLEDGE_HELP_TARGET = "helpTarget";

	private EntityID refillTarget; // updated by ensemble
	public final static String KNOWLEDGE_REFILL_TARGET = "refillTarget";

	private EntityID searchTarget;

	// position; location()
	public final static String KNOWLEDGE_POSITION = "position";

	// water; me().getWater()
	public final static String KNOWLEDGE_WATER = "water";

	private Mode mode = Mode.Search;
	public final static String KNOWLEDGE_MODE = "mode";

	Map<Integer, Set<EntityID>> burningBuildings = new HashMap<>();
	public final static String KNOWLEDGE_BURNING_BUILDINGS = "burningBuildings";

	private int helpingFireFighter;
	public final static String KNOWLEDGE_HELPING_FIREFIGHTER = "helpingFireFighter";

	private int helpingDistance;
	public final static String KNOWLEDGE_HELPING_DISTANCE = "helpingDistance";

	// ########################################################################

	public FireFighter(int id) {
		this.id = id;
		sid = String.format("FF%d", id);
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
		windDirectionSensor = new WindDirectionSensor(model);
		windSpeedSensor = new WindSpeedSensor(model);
		burningBuildings.put(id, new HashSet<>());
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	@Override
	protected void think(int time, ChangeSet changes, Collection<Command> heard) {
		long startTime = System.nanoTime();

		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			sendSubscribe(time, CHANNEL_IN);
			logI(sid + " subscribed to channel " + CHANNEL_IN);
		}
		
		// Sense
		windDirectionSensor.sense(time);
		windSpeedSensor.sense(time);
		waterSensor.sense(time);
		for (Building building : fireSensor.keySet()) {
			fireSensor.get(building).sense(time);
		}
		senseBurningBuildings();

		processCommands(time, heard);
		switchMode(time);
		act(time);

		// Send knowledge
		Msg msg = new KnowledgeMsg(id, getID(), location().getID(), fireTarget, helpTarget, refillTarget, getWater(),
				mode, burningBuildings.get(id), helpingFireFighter, helpingDistance);
		sendSpeak(time, CHANNEL_OUT, msg.getBytes());

		// Send rest
		if (mode == Mode.Refill) {
			sendRest(time);
		}

		long duration = System.nanoTime() - startTime;
		logI(formatLog(time, "thinking took " + duration / 1000000 + " ms"));
	}

	private void processCommands(int time, Collection<Command> heard) {
		// Erase fields that needs to be specified by ensembles
		refillTarget = null;

		// If fire station issued a command follow it.
		for (Command nextCom : heard) {
			if (!(nextCom instanceof AKSpeak)) {
				continue;
			}

			AKSpeak message = (AKSpeak) nextCom;
			Msg command = Msg.fromBytes(message.getContent());
			logD(formatLog(time, "heard " + message));

			// TODO:
			/*if (command instanceof TargetMsg) {
				TargetMsg targetMsg = (TargetMsg) command;
				if (targetMsg.memberId == id) {
					Logger.info(formatLog(time, "received " + targetMsg));
					helpTarget = targetMsg.coordTarget;
					helpingDistance = targetMsg.helpingDistance;
					Logger.info(formatLog(time, "help towards " + helpTarget));
				}
				if (targetMsg.coordId == id) {
					Logger.info(formatLog(time, "received " + targetMsg));
					helpingFireFighter = targetMsg.memberId;
					helpingDistance = targetMsg.helpingDistance;
					Logger.info(formatLog(time, "help from FF" + helpingFireFighter));
				}
			}*/
			if (command instanceof RefillMsg) {
				RefillMsg refillMsg = (RefillMsg) command;
				if (refillMsg.memberId == id) {
					logI(formatLog(time, "received " + refillMsg));
					if (refillTarget == null) {
						refillTarget = refillMsg.coordId;
					} else {
						int currentDistance = model.getDistance(location().getID(), refillTarget);
						int newDistance = model.getDistance(location().getID(), refillMsg.coordId);

						if (newDistance < currentDistance) {
							refillTarget = refillMsg.coordId;
						}
					}
				}
			}
			if (command instanceof BuildingsMsg) {
				BuildingsMsg buildingsMsg = (BuildingsMsg) command;
				if (buildingsMsg.id != id) {
					logI(formatLog(time, "received " + buildingsMsg));
					burningBuildings.put(buildingsMsg.id, buildingsMsg.burningBuildings);
				}
			}
		}
	}

	private void switchMode(int time) {
		boolean modeSwitched = true;

		switch (mode) {
		case Extinguish:
			// Are we out of water?
			if (waterSensor.isLevel(Quantity.LESS_OR_EQUAL, WATER_THRESHOLD)) {
				mode = Mode.MoveToRefill;
				break;
			}
			// Not next a building on fire?
			if (findCloseBurningBuilding() == null) {
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
			if (fireTarget == null || fireTarget.equals(location().getID())) {
				mode = Mode.Extinguish;
				break;
			}
			// Otherwise keep moving
			modeSwitched = false;
			break;
		case MoveToRefill:
			// Target reached?
			if (refillTarget != null && refillTarget.equals(location().getID())) {
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
				if (path != null) {
					fireTarget = getTarget(path);
					mode = Mode.MoveToFire;
				} else {
					logE(formatLog(time, "no burning building reachable"));
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
			logE(formatLog(time, "in unknown mode " + mode));
			mode = Mode.Search;
			break;
		}

		if (modeSwitched) {
			logI(formatLog(time, "switching to " + mode));
		}
	}

	private void act(int time) {
		// Act
		switch (mode) {
		case Search:
			logI(formatLog(time, "searching"));
			// TODO: search nearest closest building on fire + wind interpolation
			if (searchTarget == null) {
				searchTarget = randomTarget();
			} else if (searchTarget.equals(location().getID())) {
				searchTarget = randomTarget();
			}
			logI(formatLog(time, "moving towards search target: " + searchTarget.getValue()));
			sendMove(time, planShortestRoute(searchTarget));
			break;
		case MoveToFire:
			if (fireTarget != null) {
				logI(formatLog(time, "moving towards fire target: " + fireTarget.getValue()));
				sendMove(time, planShortestRoute(fireTarget));
			} else {
				logE(formatLog(time, " in MoveToFireMode missing fireTarget"));
			}
			break;
		case MoveToRefill:
			if (refillTarget != null) {
				logI(formatLog(time, "moving towards refill target: " + refillTarget.getValue()));
				sendMove(time, planShortestRoute(refillTarget));
			} else {
				logI(formatLog(time, "in MoveToRefillMode missing refillTarget"));
			}
			break;
		case Extinguish:
			EntityID building = findCloseBurningBuilding();
			if (building != null) {
				logI(formatLog(time, "extinguishing(" + building.getValue() + ")[" + getWater() + "]"));
				sendExtinguish(time, building, maxPower);
			} else {
				logW(formatLog(time, "Trying to extinguish null."));
			}
			break;
		case Refill:
			if (!atRefill()) {
				logE(formatLog(time, "Trying to refill at wrong place " + location().getID().getValue()));
			} else {
				logI(formatLog(time, "refilling(" + getWater() + ")"));
			}
			// Send rest after sending knowledge
			break;

		default:
			logE(id + " in unknown mode " + mode);
			break;

		}
	}

	// Sensor readings ////////////////////////////////////////////////////////

	public double getMaxWater() {
		if (maxWater == -1) {
			throw new RuntimeException(
					"The maxWater variable is not initialized. Calling getFFMaxWater() before postConnect()?");
		}

		return maxWater;
	}

	public int getWater() {
		return me().getWater();
	}

	public int getBuildingDistance(Building building) {
		return model.getDistance(building, location());
	}

	public boolean isBuildingOnFire(Building building) {
		return building.isOnFire();
	}
	
	private void senseBurningBuildings() {
		burningBuildings.get(id).clear();
		for(StandardEntity entity : model.getEntitiesOfType(StandardEntityURN.BUILDING)) {
			Building building = (Building) entity;
			if(building.isOnFire()) {
				burningBuildings.get(id).add(building.getID());
			}
		}
	}
	
	///////////////////////////////////////////////////////////////////////////

	private EntityID randomTarget() {
		return roads[random.nextInt(roads.length)].getID();
	}

	public EntityID findCloseBurningBuilding() {
		ArrayList<EntityID> closeBuildings = new ArrayList<>();
		// Can we extinguish any right now?
		for (Building building : fireSensor.keySet()) {
			if (model.getDistance(location(), building) < maxDistance * 0.8
					&& fireSensor.get(building).isLevel(Quantity.GREATER_OR_EQUAL, FIRE_PROBABILITY_THRESHOLD)) {
				closeBuildings.add(building.getID());
			}
		}
		if (closeBuildings.size() == 0) {
			return null;
		}
		return closeBuildings.get(random.nextInt(closeBuildings.size()));
	}
	
	private List<EntityID> planShortestRoute(EntityID... targets) {
		List<EntityID> path = search.breadthFirstSearch(me().getPosition(), targets);
		if (path != null) {
			logI(formatLog(0, "planed route to " + path.get(path.size() - 1)));
			return path;
		} else {
			for (EntityID target : targets) {
				logW(formatLog(0, "couldn't plan a path to " + target));
				;
			}
			return null;
		}
	}
	
	private EntityID getClosestAssumedBurningBuilding() {
		int distance = Integer.MAX_VALUE;
		EntityID result = null;
		
		for(int ff : burningBuildings.keySet()) {
			for(EntityID building : burningBuildings.get(ff)) {
				int newDistance = model.getDistance(getID(), building);
				if(newDistance < distance) {
					result = building;
					distance = newDistance;
				}
			}
		}
		
		return result;
	}

	private EntityID getTarget(List<EntityID> path) {
		return path.get(path.size() - 1);
	}

	public boolean atRefill() {
		StandardEntity here = location();
		return here instanceof Hydrant || here instanceof Refuge;
	}
	
	private void logD(String msg) {
		if(LOG_FIRE_FIGHTER) {
			Logger.debug(msg);
		}
	}
	
	private void logI(String msg) {
		if(LOG_FIRE_FIGHTER) {
			Logger.info(msg);
		}
	}
	
	private void logW(String msg) {
		if(LOG_FIRE_FIGHTER) {
			Logger.warn(msg);
		}
	}
	
	private void logE(String msg) {
		if(LOG_FIRE_FIGHTER) {
			Logger.error(msg);
		}
	}
	
	private String formatLog(int time, String msg) {
		return String.format("T[%d] L[%s] %s %s", time, location(), sid, msg);
	}

	@Override
	public String toString() {
		return sid;
	}
}
