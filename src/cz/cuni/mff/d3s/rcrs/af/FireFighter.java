package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_FAILURE_IDS;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_FAILURE_TIME;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_INTRODUCE_FAILURE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_FAILURE_IDS;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_FAILURE_TIME;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_INTRODUCE_FAILURE;
import static rescuecore2.standard.entities.StandardEntityURN.HYDRANT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import cz.cuni.mff.d3s.rcrs.af.comm.BuildingsMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.comm.TargetMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.TransitionMsg;
import cz.cuni.mff.d3s.rcrs.af.modes.ExtinguishMode;
import cz.cuni.mff.d3s.rcrs.af.modes.ModeChartImpl;
import cz.cuni.mff.d3s.rcrs.af.modes.MoveToFireMode;
import cz.cuni.mff.d3s.rcrs.af.modes.MoveToRefillMode;
import cz.cuni.mff.d3s.rcrs.af.modes.RefillMode;
import cz.cuni.mff.d3s.rcrs.af.modes.SearchMode;
import cz.cuni.mff.d3s.rcrs.af.modes.TransitionImpl;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;

public class FireFighter extends AbstractSampleAgent<FireBrigade> implements IComponent {

	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";

	private int maxWater;
	private int maxDistance;
	private int maxPower;

	private final int CHANNEL_IN = 2;
	private final int CHANNEL_OUT = 1;

	// Knowledge ##############################################################

	private final int id;
	private final String sid;
	public final static String KNOWLEDGE_ID = "id";

	private EntityID fireTarget; // updated by mode switch
	public final static String KNOWLEDGE_FIRE_TARGET = "fireTarget";

	private EntityID refillTarget; // updated by mode switch
	public final static String KNOWLEDGE_REFILL_TARGET = "refillTarget";

	private EntityID searchTarget;

	// position; location()
	public final static String KNOWLEDGE_POSITION = "position";

	// water; me().getWater()
	public final static String KNOWLEDGE_WATER = "water";

	private boolean canMove;
	public final static String KNOWLEDGE_CAN_MOVE = "canMove";

	// extinguishing: mode == Extinguish
	public final static String KNOWLEDGE_EXTINGUISHING = "extinguishing";

	private List<EntityID> burningBuildings = Collections.emptyList();
	public final static String KNOWLEDGE_BURNING_BUILDINGS = "burningBuildings";

	private boolean canDetectBuildings = true;
	public final static String KNOWLEDGE_CAN_DETECT_BUILDINGS = "canDetectBuildings";

	// ########################################################################

	private ModeChartImpl modeChart;
	private List<EntityID> refillStations;
	StandardEntity[] roads;

	public FireFighter(int id) {
		this.id = id;
		sid = String.format("FF%d", id);
		modeChart = new ModeChartImpl(this);
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE, StandardEntityURN.HYDRANT,
				StandardEntityURN.GAS_STATION);
		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);
		Logger.info(sid + " connected: max extinguish distance = " + maxDistance + ", max power = " + maxPower
				+ ", max tank = " + maxWater);

		refillStations = new ArrayList<>();
		refillStations.addAll(refugeIDs);
		for (StandardEntity hydrant : model.getEntitiesOfType(HYDRANT)) {
			refillStations.add(hydrant.getID());
		}

		Collection<StandardEntity> entities = model.getEntitiesOfType(StandardEntityURN.ROAD);
		roads = entities.toArray(new StandardEntity[entities.size()]);
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	@Override
	protected void think(int time, ChangeSet changes, Collection<Command> heard) {
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			sendSubscribe(time, CHANNEL_IN);
			Logger.info(sid + " subscribed to channel " + CHANNEL_IN);
		}

		// update knowledge
		checkFailures(time);
		findBurningBuildings();

		// l changes.getChangedEntities(); // TODO: extract buildings check burned
		// status
		// changes.getChangedProperty(building, StandardEntityConstants.Fieryness)

		boolean modeOverridden = false;
		// If fire station issued a command follow it.
		for (Command nextCom : heard) {
			if (nextCom instanceof AKSpeak) {
				AKSpeak message = (AKSpeak) nextCom;
				Msg command = Msg.fromBytes(message.getContent());
				Logger.debug(formatLog(time, "heard " + message));
				
				if (command instanceof TargetMsg) {
					TargetMsg targetMsg = (TargetMsg) command;
					if (targetMsg.id == id) {
						Logger.debug(formatLog(time, "received " + targetMsg));
						fireTarget = targetMsg.target;
						modeChart.setCurrentMode(MoveToFireMode.class);
						modeOverridden = true;
						Logger.info(formatLog(time, "targeted towards " + fireTarget));
					}
				}
				if (command instanceof BuildingsMsg) {
					BuildingsMsg buildingsMsg = (BuildingsMsg) command;
					if (buildingsMsg.id == id && !canDetectBuildings) {
						Logger.debug(formatLog(time, "received " + buildingsMsg));
						burningBuildings = buildingsMsg.burningBuildings;
						Logger.info(formatLog(time, "injected with burning buildings"));
					}
				}
				if(command instanceof TransitionMsg) {
					TransitionMsg transitionMsg = (TransitionMsg) command;
					if(transitionMsg.id == id) {
						Logger.debug(formatLog(time, "received " + transitionMsg));
						TransitionImpl transition = transitionMsg.transition;
						switch(transitionMsg.action) {
						case ADD:
							modeChart.addTransition(transition.getFrom(), transition.getTo(), transition.getGuard());
							break;
						case REMOVE:
							modeChart.removeTransition(transition);
							break;
						default:
							Logger.error(formatLog(time, "The operation \"" + transitionMsg.action +"\" is not supported"));
							break;
						}
						
						Logger.info(formatLog(time, "injected with transition " + transition));
					}
				}
			}
		}

		// Switch mode if necessary
		if (!modeOverridden) {
			modeChart.decideModeSwitch();
		}

		// Switch behavior according to current mode

		// Search
		if (modeChart.getCurrentMode() instanceof SearchMode) {
			Logger.info(formatLog(time, "searching"));
			if (searchTarget == null) {
				searchTarget = randomTarget();
			} else if (searchTarget.equals(location().getID())) {
				searchTarget = randomTarget();
			}
			// sendMove(time, randomWalk());
			if (canMove) {
				sendMove(time, planShortestRoute(searchTarget));
			} else {
				Logger.info(formatLog(time, "can't move."));
			}
		}

		// Move to fire
		else if (modeChart.getCurrentMode() instanceof MoveToFireMode) {
			if (canMove) {
				Logger.info(formatLog(time, "moving to fire " + fireTarget));
				if (fireTarget != null) {
					sendMove(time, planShortestRoute(fireTarget));
				} else {
					Logger.error(formatLog(time, " in MoveToFireMode missing fireTarget"));
				}
			} else {
				Logger.info(formatLog(time, "can't move."));
			}
		}

		// Move to Refill
		else if (modeChart.getCurrentMode() instanceof MoveToRefillMode) {
			if (canMove) {
				Logger.info(formatLog(time, "moving to refill " + refillTarget));
				if (refillTarget != null) {
					sendMove(time, planShortestRoute(refillTarget));
				} else {
					Logger.error(formatLog(time, "in MoveToRefillMode missing refillTarget"));
				}
			} else {
				Logger.info(formatLog(time, "can't move."));
			}
		}

		// Extinguish
		else if (modeChart.getCurrentMode() instanceof ExtinguishMode) {
			EntityID building = findCloseBurningBuilding();
			if (building != null) {
				Logger.info(formatLog(time, "extinguishing(" + building + ")[" + getWater() + "]"));
				// Logger.info(model.getEntity(building).getFullDescription());
				sendExtinguish(time, building, maxPower);
			} else {
				Logger.warn(formatLog(time, "Trying to extinguish null."));
			}
		}

		// Refill;
		else if (modeChart.getCurrentMode() instanceof RefillMode) {
			Logger.info(formatLog(time, "refilling(" + getWater() + ")"));
			sendRest(time);
		}

		// Send knowledge
		Msg msg = new KnowledgeMsg(id, location().getID(), fireTarget, refillTarget,
				getWater(), extinguishing(), canMove, burningBuildings, canDetectBuildings);
		sendSpeak(time, CHANNEL_OUT, msg.getBytes());

	}
	
	@Override
	public int getId() {
		return id;
	}

	public int getWater() {
		if (!me().isWaterDefined()) {
			throw new UnsupportedOperationException("Operation getWater() not supported on " + id);
		}

		return me().getWater();
	}

	private EntityID randomTarget() {
		return roads[random.nextInt(roads.length)].getID();
	}

	public int getMaxWater() {
		return maxWater;
	}

	public EntityID getFireTarget() {
		return fireTarget;
	}

	public EntityID getRefillTarget() {
		return refillTarget;
	}

	private boolean extinguishing() {
		return modeChart.getCurrentMode() instanceof ExtinguishMode;
	}

	public void setFireTarget(boolean set) {
		if (set) {
			List<EntityID> path = search.breadthFirstSearch(me().getPosition(), burningBuildings);
			if (path == null) {
				Logger.error(formatLog(0, "Couldn't plan a path to a fire."));
			} else {
				fireTarget = getTarget(path);
			}
		} else {
			fireTarget = null;
		}
	}

	public void setRefillTarget(boolean set) {
		if (set) {
			List<EntityID> path = null;
			// With small probability refill at hydrant
			if (random.nextDouble() < 0.3) {
				path = search.breadthFirstSearch(me().getPosition(), refillStations);
			} else {
				path = search.breadthFirstSearch(me().getPosition(), refugeIDs);
			}
			if (path == null) {
				Logger.error(formatLog(0, "Couldn't plan a path to a refuge."));
			} else {
				refillTarget = getTarget(path);
			}
		} else {
			refillTarget = null;
		}
	}

	private EntityID getTarget(List<EntityID> path) {
		return path.get(path.size() - 1);
	}

	public EntityID getLocation() {
		return location().getID();
	}

	public boolean atRefuge() {
		return location() instanceof Refuge;
	}

	public List<EntityID> getBurningBuildings() {
		return burningBuildings;
	}

	private void checkFailures(int time) {
		// H1 failure
		canDetectBuildings = !(H1_INTRODUCE_FAILURE && time >= H1_FAILURE_TIME && H1_FAILURE_IDS.indexOf(sid) != -1);

		// H2 failure
		canMove = !(H2_INTRODUCE_FAILURE && time >= H2_FAILURE_TIME && H2_FAILURE_IDS.indexOf(sid) != -1);
	}

	private void findBurningBuildings() {
		if (!canDetectBuildings) {
			// Search for burning buildings no longer works due to malfunction
			Logger.info(formatLog(0, "Sensing buildings failed."));
			return;
		}

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
		// Can we extinguish any right now?
		for (EntityID building : burningBuildings) {
			if (model.getDistance(getID(), building) < maxDistance) {
				return building;
			}
		}
		return null;
	}

	private List<EntityID> planShortestRoute(EntityID... targets) {
		List<EntityID> path = search.breadthFirstSearch(me().getPosition(), targets);
		if (path != null) {
			Logger.debug(formatLog(0, "planed route to " + path.get(path.size() - 1)));
			return path;
		} else {
			for (EntityID target : targets) {
				Logger.warn(formatLog(0, "couldn't plan a path to " + target));
				;
			}
			return null;
		}
	}

	private String formatLog(int time, String msg) {
		return String.format("T[%d] L[%s] %s %s", time, location(), sid, msg);
	}

	@Override
	public String toString() {
		return sid;
	}

	@Override
	public void addTransition(TransitionImpl transition) {
		// This is relevant only on the fire station side
	}

	@Override
	public void removeTransition(TransitionImpl transition) {
		// This is relevant only on the fire station side
		
	}

}
