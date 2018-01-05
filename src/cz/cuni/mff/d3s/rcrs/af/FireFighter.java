package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_FAILURE_IDS;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_FAILURE_TIME;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_INTRODUCE_FAILURE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_FAILURE_TIME;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_INTRODUCE_FAILURE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.failedRefillStations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.BuildingsMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.comm.PropertyMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.RefillMsg;
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
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import sample.AbstractSampleAgent;

public class FireFighter extends AbstractSampleAgent<FireBrigade> implements IFFComponent {

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

	private EntityID helpTarget; // updated by mode switch
	public final static String KNOWLEDGE_HELP_TARGET = "helpTarget";

	private EntityID refillTarget; // updated by ensemble
	public final static String KNOWLEDGE_REFILL_TARGET = "refillTarget";

	private EntityID searchTarget;

	// position; location()
	public final static String KNOWLEDGE_POSITION = "position";

	// water; me().getWater()
	public final static String KNOWLEDGE_WATER = "water";

	// extinguishing: mode == Extinguish
	public final static String KNOWLEDGE_EXTINGUISHING = "extinguishing";
	
	// extinguishing: mode == Refill || mode == MoveToRefill
	public final static String KNOWLEDGE_REFILLING = "refilling";
	
	private List<EntityID> burningBuildings = Collections.emptyList();
	public final static String KNOWLEDGE_BURNING_BUILDINGS = "burningBuildings";

	private boolean canDetectBuildings = true;
	public final static String KNOWLEDGE_CAN_DETECT_BUILDINGS = "canDetectBuildings";
	
	private int helpingFireFighter;
	public final static String KNOWLEDGE_HELPING_FIREFIGHTER = "helpingFireFighter";
	
	private int helpingDistance;
	public final static String KNOWLEDGE_HELPING_DISTANCE = "helpingDistance";

	// ########################################################################

	private ModeChartImpl modeChart;
	StandardEntity[] roads;

	public FireFighter(int id) {
		this.id = id;
		sid = String.format("FF%d", id);
		modeChart = new ModeChartImpl(this);
		helpingFireFighter = -1;
		helpingDistance = Integer.MAX_VALUE;
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
		
		// Specify mode chart parameters after maxWater is defined
		for(TransitionImpl t : modeChart.getTransitions()) {
			t.getInnerGuard().specifyParameters();
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
		long startTime = System.nanoTime();
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			sendSubscribe(time, CHANNEL_IN);
			Logger.info(sid + " subscribed to channel " + CHANNEL_IN);
		}

		// update knowledge
		checkFailures(time);
		Logger.info(formatLog(time, "can detect: " + canDetectBuildings + ", water: " + getWater()));
		findBurningBuildings();
		Logger.info(formatLog(time, "found burning buildings: " + stringBurningBuildings()));
		
		// Anulate helping firefighter to cancel ensemble if no longer satisfied
		helpTarget = null;
		helpingFireFighter = -1;
		helpingDistance = Integer.MAX_VALUE;

		// changes.getChangedEntities(); // TODO: extract buildings check burned
		// status
		// changes.getChangedProperty(building, StandardEntityConstants.Fieryness)

		// If fire station issued a command follow it.
		for (Command nextCom : heard) {
			if (nextCom instanceof AKSpeak) {
				AKSpeak message = (AKSpeak) nextCom;
				Msg command = Msg.fromBytes(message.getContent());
				Logger.debug(formatLog(time, "heard " + message));
				
				if (command instanceof TargetMsg) {
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
				}
				if (command instanceof RefillMsg) {
					RefillMsg refillMsg = (RefillMsg) command;
					if (refillMsg.memberId == id) {
						Logger.info(formatLog(time, "received " + refillMsg));
						refillTarget = refillMsg.coordId;
					}
				}
				if (command instanceof BuildingsMsg) {
					BuildingsMsg buildingsMsg = (BuildingsMsg) command;
					if (buildingsMsg.id == id && !canDetectBuildings) {
						Logger.info(formatLog(time, "received " + buildingsMsg));
						burningBuildings = buildingsMsg.burningBuildings;
						Logger.info(formatLog(time, "injected with burning buildings"));
					}
				}
				if(command instanceof TransitionMsg) {
					TransitionMsg transitionMsg = (TransitionMsg) command;
					if(transitionMsg.id == id) {
						Logger.info(formatLog(time, "received " + transitionMsg));
						TransitionImpl transition = transitionMsg.transition;
						switch(transitionMsg.action) {
						case ADD:
							addTransitionCallback(transition);
							break;
						case REMOVE:
							removeTransitionCallback(transition);
							break;
						default:
							Logger.error(formatLog(time, "The operation \"" + transitionMsg.action +"\" is not supported"));
							break;
						}
						
						Logger.info(formatLog(time, "injected with transition " + transition));
					}
				}
				if(command instanceof PropertyMsg) {
					PropertyMsg propertyMsg = (PropertyMsg) command;
					if(propertyMsg.id == id) {
						Logger.info(formatLog(time, "received " + propertyMsg));
						TransitionImpl transition = propertyMsg.transition;
						setGuardParamCallback(transition, propertyMsg.property, propertyMsg.value);
						
						Logger.info(formatLog(time, "parameter " + propertyMsg.property + " adjusted to " + propertyMsg.value));
					}
				}
			}
		}

		// Switch mode
		modeChart.decideModeSwitch();

		// Switch behavior according to current mode

		// Search
		if (modeChart.getCurrentMode() instanceof SearchMode) {
			Logger.info(formatLog(time, "searching"));
			if(helpTarget != null) {
				searchTarget = helpTarget;
			} else if (searchTarget == null) {
				searchTarget = randomTarget();
			} else if (searchTarget.equals(location().getID())) {
				searchTarget = randomTarget();
			}
			Logger.info(formatLog(time, "moving towards search target: " + searchTarget.getValue()));
			sendMove(time, planShortestRoute(searchTarget));
		}

		// Move to fire
		else if (modeChart.getCurrentMode() instanceof MoveToFireMode) {
			Logger.info(formatLog(time, "moving to fire " + fireTarget));
			if (fireTarget != null) {
				Logger.info(formatLog(time, "moving towards fire target: " + fireTarget.getValue()));
				sendMove(time, planShortestRoute(fireTarget));
			} else {
				Logger.error(formatLog(time, " in MoveToFireMode missing fireTarget"));
			}
		}

		// Move to Refill
		else if (modeChart.getCurrentMode() instanceof MoveToRefillMode) {
			if (refillTarget != null) {
				if(H2_INTRODUCE_FAILURE && time >= H2_FAILURE_TIME && failedRefillStations.contains(refillTarget)) {
					Logger.info(formatLog(time, "Stucked moving towards broken refill station " + refillTarget.getValue()));
				} else {
					Logger.info(formatLog(time, "moving towards refill target: " + refillTarget.getValue()));
					sendMove(time, planShortestRoute(refillTarget));
				}
			} else {
				Logger.error(formatLog(time, "in MoveToRefillMode missing refillTarget"));
			}
		}

		// Extinguish
		else if (modeChart.getCurrentMode() instanceof ExtinguishMode) {
			EntityID building = findCloseBurningBuilding();
			if (building != null) {
				Logger.info(formatLog(time, "extinguishing(" + building.getValue() + ")[" + getWater() + "]"));
				sendExtinguish(time, building, maxPower);
			} else {
				Logger.warn(formatLog(time, "Trying to extinguish null."));
			}
		}

		// Refill;
		else if (modeChart.getCurrentMode() instanceof RefillMode) {
			if(!atRefill()) {
				Logger.warn(formatLog(time, "Trying to refill at wrong place " + getLocation().getValue()));
			} else {
				Logger.info(formatLog(time, "refilling(" + getWater() + ")"));
			}
			sendRest(time);
		}

		// Send knowledge
		Msg msg = new KnowledgeMsg(id, location().getID(), fireTarget, helpTarget, refillTarget,
				getWater(), extinguishing(), refilling(), burningBuildings, canDetectBuildings,
				helpingFireFighter, helpingDistance);
		sendSpeak(time, CHANNEL_OUT, msg.getBytes());
		
		long duration = System.nanoTime() - startTime;
		Logger.info(formatLog(time, "thinking took " + duration/1000000 + " ms"));

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

	private boolean refilling() {
		return modeChart.getCurrentMode() instanceof RefillMode
				|| modeChart.getCurrentMode() instanceof MoveToRefillMode;
	}
	
	public void setFireTarget(boolean set) {
		if (set) {
			List<EntityID> path = search.breadthFirstSearch(me().getPosition(), burningBuildings);
			if (path == null) {
				Logger.error(formatLog(0, "Couldn't plan a path to a fire."));
			} else {
				if(path.size() > 2) {
					fireTarget = path.get(path.size() - 2); // -2 to avoid entering building on fire
				} else {
					fireTarget = location().getID();
				}
			}
		} else {
			fireTarget = null;
		}
	}

	public EntityID getLocation() {
		return location().getID();
	}
	
	public int getHelpingFireFighter() {
		return helpingFireFighter;
	}

	public List<EntityID> getBurningBuildings() {
		return burningBuildings;
	}

	private void checkFailures(int time) {
		// H1 failure
		canDetectBuildings = !(H1_INTRODUCE_FAILURE && time >= H1_FAILURE_TIME && H1_FAILURE_IDS.indexOf(sid) != -1);
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

	private String stringBurningBuildings() {
		StringBuilder builder = new StringBuilder();
		for(EntityID buildingId : burningBuildings) {
			Building building = (Building) model.getEntity(buildingId);
			builder.append(buildingId.getValue());
			try {
				builder.append("(temp: ");
				builder.append(building.getTemperature());
				builder.append(")(on fire: ");
				builder.append(building.isOnFire());
				builder.append(")(fire: ");
				builder.append(building.getFieryness());
				builder.append(")(ignition: ");
				builder.append(building.getIgnition());
				builder.append(")");
			} catch (NullPointerException e) {
				builder.append(" null ");
			}
		}
		
		return builder.toString();
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

	private String formatLog(int time, String msg) {
		return String.format("T[%d] L[%s] %s %s", time, location(), sid, msg);
	}

	@Override
	public String toString() {
		return sid;
	}

	@Override
	public void addTransitionCallback(TransitionImpl transition) {
		modeChart.addTransition(transition.getFrom(), transition.getTo(), transition.getGuard());
	}

	@Override
	public void removeTransitionCallback(TransitionImpl transition) {
		modeChart.removeTransition(transition);
	}

	@Override
	public void setGuardParamCallback(TransitionImpl outerTransition, String name, double value) {
		TransitionImpl targetTransition = null;
		for(TransitionImpl innerTransition : modeChart.getTransitions()) {
			if(innerTransition.equals(outerTransition)) {
				targetTransition = innerTransition;
				break;
			}
		}
		
		if(targetTransition != null) {
			targetTransition.setGuardParam(name, value);
		} else {
			Logger.error(String.format("Target transition \"%s\"not found while setting guard parameter \"%s=%f\"",
					outerTransition, name, value));
		}
	}

	@Override
	public Map<String, Object> getKnowledge() {
		return null;
	}

	@Override
	public Set<String> getExposedKnowledge() {
		return null;
	}

	@Override
	public boolean atRefill() {
		StandardEntity here = location();
		return here instanceof Hydrant || here instanceof Refuge;
	}
}
