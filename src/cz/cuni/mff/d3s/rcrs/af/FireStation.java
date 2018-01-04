package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_MECHANISM;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_MECHANISM;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_INTRODUCE_FAILURE;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_FAILURE_IMPACT;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.failedRefillStations;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H3_MECHANISM;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H4_MECHANISM;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.MetaAdaptationManager;
import cz.cuni.mff.d3s.metaadaptation.correlation.KnowledgeMetadataHolder;
import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.comm.PropertyMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.TransitionMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.TransitionMsg.Action;
import cz.cuni.mff.d3s.rcrs.af.componentisolation.IsolationHolder;
import cz.cuni.mff.d3s.rcrs.af.correlation.CorrelationHolder;
import cz.cuni.mff.d3s.rcrs.af.correlation.DistanceMetric;
import cz.cuni.mff.d3s.rcrs.af.correlation.SurroundingMetric;
import cz.cuni.mff.d3s.rcrs.af.modes.TransitionImpl;
import cz.cuni.mff.d3s.rcrs.af.modeswitch.ModeSwitchHolder;
import cz.cuni.mff.d3s.rcrs.af.modeswitchprops.ModeSwitchPropsHolder;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;

public class FireStation extends StandardAgent<Building> {

	private static final String MAX_WATER_KEY = "fire.tank.maximum";

	private final String sid;
	private final int CHANNEL_IN = 1;
	private final int CHANNEL_OUT = 2;
	
	private final Map<Integer, FFComponent> ffComponents = new HashMap<>();
	private final Set<cz.cuni.mff.d3s.rcrs.af.componentisolation.ComponentImpl> refillComponents = new HashSet<>();
	private final Set<Ensemble> ensembles = new HashSet<>();
	
	private final MetaAdaptationManager adaptationManager;
	private final CorrelationHolder correlationManager;
	private final IsolationHolder isolationManager;
	private final ModeSwitchHolder modeSwitchManager;
	private final ModeSwitchPropsHolder modeSwitchPropsManager;
	
	private int time;
	
	public FireStation(int id) {
		this.sid = String.format("FS%d", id);

		// start empty meta-adaptations manager
		adaptationManager = new MetaAdaptationManager();
		adaptationManager.setVerbosity(true);

		System.out.println("H1_MECHANISM " + H1_MECHANISM);
		System.out.println("H2_MECHANISM " + H2_MECHANISM);
		System.out.println("H3_MECHANISM " + H3_MECHANISM);
		System.out.println("H4_MECHANISM " + H4_MECHANISM);
		
		if(H1_MECHANISM) {
			correlationManager = new CorrelationHolder(this);
			correlationManager.registerAt(adaptationManager);
					
		} else {
			correlationManager = null;
		}
		if(H2_MECHANISM) {
			isolationManager = new IsolationHolder();
			isolationManager.registerAt(adaptationManager);
		} else {
			isolationManager = null;
		}
		if(H3_MECHANISM) {
			modeSwitchManager = new ModeSwitchHolder();
		} else {
			modeSwitchManager = null;
		}
		if(H4_MECHANISM) {
			modeSwitchPropsManager = new ModeSwitchPropsHolder();
		} else {
			modeSwitchPropsManager = null;
		}

	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE, StandardEntityURN.HYDRANT,
				StandardEntityURN.GAS_STATION);
		Logger.info(sid + " connected");

		// Create refill components
		for (StandardEntity next : model) {
            if (next instanceof Hydrant || next instanceof Refuge) {
            	refillComponents.add(new cz.cuni.mff.d3s.rcrs.af.componentisolation.ComponentImpl(next.getID()));
            }
        }
		
		// ensembles.add(TargetFireZoneEnsemble.getInstance(model));

		if(H1_MECHANISM) {
			// Register metrics
			KnowledgeMetadataHolder.setBoundAndMetric(KNOWLEDGE_POSITION, 30_000, new DistanceMetric(model), 0.9);
			KnowledgeMetadataHolder.setBoundAndMetric(KNOWLEDGE_BURNING_BUILDINGS, 2, new SurroundingMetric(), 0.9);
		}
		if(H2_INTRODUCE_FAILURE) {
			for(cz.cuni.mff.d3s.rcrs.af.componentisolation.ComponentImpl refillStation : refillComponents) {
				if(random.nextDouble() <= H2_FAILURE_IMPACT) {
					refillStation.malfunction();
					failedRefillStations.add(refillStation.getId());
					Logger.info("Refill station " + refillStation.toString() + " malfunction");
				}

				if(H2_MECHANISM) {
					isolationManager.getComponentManager().addComponent(refillStation);
				}
			}
		}
	}
	
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_STATION);
	}

	@Override
	protected void think(int time, ChangeSet changes, Collection<Command> heard) {
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            sendSubscribe(time, CHANNEL_IN);
            Logger.info(sid + " subscribed to channel " + CHANNEL_IN);
        }
		this.time = time;
		System.out.println(time);
		
		// Receive messages from fire fighters
		for (Command next : heard) {
            if(next instanceof AKSpeak) {
            	AKSpeak message = (AKSpeak) next;
            	KnowledgeMsg msg = (KnowledgeMsg) Msg.fromBytes(message.getContent());
            	if(msg == null) {
            		continue;
            	}
            	Logger.info(String.format("at %d %s received %s", time, sid, msg));
            	FFComponent c = ffComponents.containsKey(msg.id)
            			? ffComponents.get(msg.id)
            			: newComponent(msg, time);
            	c.loadKnowledge(msg, time);
            	ffComponents.put(msg.id, c);
            }
        }		
		
		// Evaluate ensembles
		for(int cId : ffComponents.keySet()) {
			for(int mId : ffComponents.keySet()) {
				if(cId == mId) {
					continue;
				}
				
				FFComponent coordinator = ffComponents.get(cId);
				FFComponent member = ffComponents.get(mId);
				for(Ensemble ensemble : ensembles) {
					if(ensemble.isSatisfied(coordinator, member)) {
						Msg msg = ensemble.getMessage(coordinator, member);
						sendSpeak(time, CHANNEL_OUT, msg.getBytes());
						Logger.info(String.format("at %d %s sending msg %s", time, sid, msg));
					}
				}
			}
		}
		Ensemble refillEnsemble = RefillStationEnsemble.getInstance(model);
		for(cz.cuni.mff.d3s.rcrs.af.componentisolation.ComponentImpl coordinator : refillComponents) {
			for(int mId : ffComponents.keySet()) {
				FFComponent member = ffComponents.get(mId);
				if(refillEnsemble.isSatisfied(coordinator, member)) {
					Msg msg = refillEnsemble.getMessage(coordinator, member);
					sendSpeak(time, CHANNEL_OUT, msg.getBytes());
					Logger.info(String.format("at %d %s sending msg %s", time, sid, msg));
				}
			}
		}
		
		if(H3_MECHANISM && !modeSwitchManager.isRegistered()
				&& modeSwitchManager.getComponentManager().getComponents().size() > 0) {
			// Register after having the components
			modeSwitchManager.registerAt(adaptationManager);
		}
		if(H4_MECHANISM && !modeSwitchPropsManager.isRegistered()
				&& modeSwitchPropsManager.getComponentManager().getComponents().size() > 0) {
			// Register after having the components
			modeSwitchPropsManager.registerAt(adaptationManager);
		}
		
		long startTime = System.nanoTime();
		adaptationManager.reason();
		long duration = System.nanoTime() - startTime;
		Logger.info("Meta-adaptation took " + duration/1000000 + " ms");
		
        sendRest(time);
	}
	
	private FFComponent newComponent(KnowledgeMsg msg, int time) {
		FFComponent c = new FFComponent(this);
		c.loadKnowledge(msg, time);
		
		if(H1_MECHANISM) {
			correlationManager.getComponentManager().addComponent(new cz.cuni.mff.d3s.rcrs.af.correlation.ComponentImpl(c));
		}
		if(H3_MECHANISM) {
			modeSwitchManager.getComponentManager().addComponent(new cz.cuni.mff.d3s.rcrs.af.modeswitch.ComponentImpl(c));
		}
		if(H4_MECHANISM) {
			modeSwitchPropsManager.getComponentManager().addComponent(new cz.cuni.mff.d3s.rcrs.af.modeswitchprops.ComponentImpl(c));
		}
		
		return c;
	}

	public int getMaxWater() {
		return config.getIntValue(MAX_WATER_KEY);
	}
	
	public void addTransitionCallback(TransitionImpl transition, int id) {
		Msg msg = new TransitionMsg(id, transition, Action.ADD);
		sendSpeak(time, CHANNEL_OUT, msg.getBytes());
		Logger.info(String.format("at %d %s sending msg %s", time, sid, msg));
	}
	
	public void removeTransitionCallback(TransitionImpl transition, int id) {
		Msg msg = new TransitionMsg(id, transition, Action.REMOVE);
		sendSpeak(time, CHANNEL_OUT, msg.getBytes());
		Logger.info(String.format("at %d %s sending msg %s", time, sid, msg));
	}
	
	public void setGuardParamCallback(TransitionImpl transition, String name, double value, int id) {
		Msg msg = new PropertyMsg(id, transition, name, value);
		sendSpeak(time, CHANNEL_OUT, msg.getBytes());
		Logger.info(String.format("at %d %s sending msg %s", time, sid, msg));
	}
	
	public Set<Ensemble> getEnsembles() {
		return ensembles;
	}
	
	public void addEnsemble(Ensemble ensemble) {
		ensembles.add(ensemble);
	}

	@Override
	public String toString() {
		return sid;
	}

}
