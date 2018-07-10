package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.FIRE_PROBABILITY_THRESHOLD;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.WATER_THRESHOLD;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.WIND_DEFINED_TARGET_PROBABILITY;

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
import cz.cuni.mff.d3s.rcrs.af.comm.TargetMsg;
import cz.cuni.mff.d3s.rcrs.af.modes.Mode;
import cz.cuni.mff.d3s.rcrs.af.sensors.FireSensor;
import cz.cuni.mff.d3s.rcrs.af.sensors.Sensor.Quantity;
import cz.cuni.mff.d3s.rcrs.af.sensors.WaterSensor;
import cz.cuni.mff.d3s.rcrs.af.sensors.WindDirectionSensor;
import cz.cuni.mff.d3s.rcrs.af.sensors.WindSpeedSensor;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
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

	private enum MsgClass {
		General, Communication, Modes, Action, Wind;
	}

	private final Log log;

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
		log = new Log(sid);
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
		log.i(0, MsgClass.General, "connected: max extinguish distance = %d, max power = %d, max tank = %d",
				maxDistance, maxPower, maxWater);

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
			log.i(time, MsgClass.Communication, "subscribed to channel %d", CHANNEL_IN);
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
		log.i(time, MsgClass.General, "thinking took %d ms", duration / 1000000);
	}

	private void processCommands(int time, Collection<Command> heard) {
		// Erase fields that needs to be specified by ensembles
		refillTarget = null;
		helpTarget = null;
		helpingDistance = Integer.MAX_VALUE;

		// If fire station issued a command follow it.
		for (Command nextCom : heard) {
			if (!(nextCom instanceof AKSpeak)) {
				continue;
			}

			AKSpeak message = (AKSpeak) nextCom;
			Msg command = Msg.fromBytes(message.getContent());
			log.d(time, MsgClass.Communication, "heard %s", message);

			if (command instanceof TargetMsg) {
				TargetMsg targetMsg = (TargetMsg) command;
				if (targetMsg.memberId == id) {
					log.i(time, MsgClass.Communication, "received %s", targetMsg);
					helpTarget = targetMsg.coordTarget;
					helpingDistance = targetMsg.helpingDistance;
				}
				if (targetMsg.coordId == id) {
					log.i(time, MsgClass.Communication, "received %s", targetMsg);
					helpingFireFighter = targetMsg.memberId;
					helpingDistance = targetMsg.helpingDistance;
				}
			}
			if (command instanceof RefillMsg) {
				RefillMsg refillMsg = (RefillMsg) command;
				if (refillMsg.memberId == id) {
					log.i(time, MsgClass.Communication, "received %s", refillMsg);
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
					log.i(time, MsgClass.Communication, "received %s", buildingsMsg);
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
				List<EntityID> path = planShortestRoute(time, burningBuilding);
				if (path != null) {
					fireTarget = getTarget(path);
					mode = Mode.MoveToFire;
				} else {
					log.e(time, MsgClass.General, "no burning building reachable");
				}
			} else {
				// Search burning buildings
				searchTarget = randomTarget();
				mode = Mode.Search;
			}
			break;
		case Search: {
			// Someone needs help
			if(helpTarget != null) {
				fireTarget = helpTarget;
				mode = Mode.MoveToFire;
				break;
			}
			// Found burning building?
			if (findCloseBurningBuilding() != null) {
				mode = Mode.Extinguish;
				break;
			}
			modeSwitched = false;
			break;
		}
		default:
			log.e(time, MsgClass.General, "in unknown mode %s", mode);
			mode = Mode.Search;
			break;
		}

		if (modeSwitched) {
			log.i(time, MsgClass.Modes, "switching to %s", mode);
		}
	}

	private void act(int time) {
		// Act
		switch (mode) {
		case Search:
			log.i(time, MsgClass.Modes, "searching");
			if (searchTarget == null || searchTarget.equals(location().getID())) {
				searchTarget = newSearchTarget(time);
			}
			log.i(time, MsgClass.Action, "moving towards search target: %s", searchTarget);
			sendMove(time, planShortestRoute(time, searchTarget));
			break;
		case MoveToFire:
			log.i(time, MsgClass.Modes, "moving to fire");
			if (fireTarget != null) {
				log.i(time, MsgClass.Action, "moving towards fire target: %s", fireTarget);
				sendMove(time, planShortestRoute(time, fireTarget));
			} else {
				log.e(time, MsgClass.General, "in MoveToFireMode missing fireTarget");
			}
			break;
		case MoveToRefill:
			log.i(time, MsgClass.Modes, "moving to refill");
			if (refillTarget != null) {
				log.i(time, MsgClass.Action, "moving towards refill target: %s", refillTarget);
				sendMove(time, planShortestRoute(time, refillTarget));
			} else {
				log.w(time, MsgClass.General, "in MoveToRefillMode missing refillTarget");
			}
			break;
		case Extinguish:
			log.i(time, MsgClass.Modes, "extinguishing");
			EntityID building = findCloseBurningBuilding();
			if (building != null) {
				log.i(time, MsgClass.Action, "extinguishing(%s)[%d]", building, getWater());
				sendExtinguish(time, building, maxPower);
			} else {
				log.w(time, MsgClass.General, "Trying to extinguish null.");
			}
			break;
		case Refill:
			log.i(time, MsgClass.Modes, "refilling");
			if (!atRefill()) {
				log.e(time, MsgClass.General, "Trying to refill at wrong place %s", location().getID());
			} else {
				log.i(time, MsgClass.Action, "refilling(%d)", getWater());
			}
			// Send rest after sending knowledge
			break;

		default:
			log.e(time, MsgClass.General, "in unknown mode: %s", mode);
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
		for (StandardEntity entity : model.getEntitiesOfType(StandardEntityURN.BUILDING)) {
			Building building = (Building) entity;
			if (building.isOnFire()) {
				burningBuildings.get(id).add(building.getID());
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////

	private EntityID randomTarget() {
		return roads[random.nextInt(roads.length)].getID();
	}

	private StandardEntity randomTarget(Collection<StandardEntity> targets) {
		if (targets.size() == 0) {
			return null;
		}

		StandardEntity[] ta = targets.toArray(new StandardEntity[targets.size()]);
		return ta[random.nextInt(ta.length)];
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

	private List<EntityID> planShortestRoute(int time, EntityID... targets) {
		List<EntityID> path = search.breadthFirstSearch(me().getPosition(), targets);
		if (path != null) {
			log.i(time, MsgClass.Action, "planed route to %s", path.get(path.size() - 1));
			return path;
		} else {
			for (EntityID target : targets) {
				log.w(time, MsgClass.General, "couldn't plan a path to %s", target);
			}
			return null;
		}
	}

	private EntityID getClosestAssumedBurningBuilding() {
		int distance = Integer.MAX_VALUE;
		EntityID result = null;

		for (int ff : burningBuildings.keySet()) {
			for (EntityID building : burningBuildings.get(ff)) {
				int newDistance = model.getDistance(getID(), building);
				if (newDistance < distance) {
					result = building;
					distance = newDistance;
				}
			}
		}

		return result;
	}

	private EntityID newSearchTarget(int time) {
		EntityID building = getClosestAssumedBurningBuilding();
		if (building != null && random.nextDouble() < WIND_DEFINED_TARGET_PROBABILITY) {
			log.i(time, MsgClass.Wind, "Searching around burning building %s", building);
			double direction = windDirectionSensor.getMean();
			double speed = windSpeedSensor.getMean();
			double shiftY = Math.cos(Math.toRadians(direction)) * speed;
			double shiftX = Math.sin(Math.toRadians(direction)) * speed;
			log.i(time, MsgClass.Wind, "Wind direction %.2f speed %.2f shift x %.2f y %.2f", direction, speed, shiftX,
					shiftY);

			Pair<Integer, Integer> location = model.getEntity(building).getLocation(model);
			int targetX = location.first() + (int) shiftX;
			int targetY = location.second() + (int) shiftY;
			Collection<StandardEntity> targets = model.getObjectsInRange(targetX, targetY, (int) speed / 4);

			StandardEntity target = randomTarget(targets);
			if (target != null) {
				log.i(time, MsgClass.Wind, "Selected search target %s", target);
				return target.getID();
			}
		}

		log.i(time, MsgClass.Wind, "Random search target");
		return randomTarget();
	}

	private EntityID getTarget(List<EntityID> path) {
		return path.get(path.size() - 1);
	}

	public boolean atRefill() {
		StandardEntity here = location();
		return here instanceof Hydrant || here instanceof Refuge;
	}

	@Override
	public String toString() {
		return sid;
	}
}
