package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_MECHANISM;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H2_MECHANISM;
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
import cz.cuni.mff.d3s.rcrs.af.componentisolation.IsolationHolder;
import cz.cuni.mff.d3s.rcrs.af.correlation.CorrelationHolder;
import cz.cuni.mff.d3s.rcrs.af.correlation.DistanceMetric;
import cz.cuni.mff.d3s.rcrs.af.correlation.SurroundingMetric;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;

public class FireStation extends StandardAgent<Building> {

	private final String sid;
	private final int CHANNEL_IN = 1;
	private final int CHANNEL_OUT = 2;
	
	private final Map<Integer, Component> components = new HashMap<>();
	private final Set<Ensemble> ensembles = new HashSet<>();
	
	private final MetaAdaptationManager adaptationManager;
	private final CorrelationHolder correlationManager;
	private final IsolationHolder isolationManager;
	
	public FireStation(int id) {
		this.sid = String.format("FS%d", id);

		// start empty meta-adaptations manager
		adaptationManager = new MetaAdaptationManager();
		adaptationManager.setVerbosity(true);
		
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

	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE, StandardEntityURN.HYDRANT,
				StandardEntityURN.GAS_STATION);
		Logger.info(sid + " connected");

		ensembles.add(TargetFireZoneEnsemble.getInstance(model));

		if(H1_MECHANISM) {
			// Register metrics
			KnowledgeMetadataHolder.setBoundAndMetric(KNOWLEDGE_POSITION, 30_000, new DistanceMetric(model), 0.9);
			KnowledgeMetadataHolder.setBoundAndMetric(KNOWLEDGE_BURNING_BUILDINGS, 2, new SurroundingMetric(), 0.9);
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
		System.out.println(time);
		
		// Receive messages from fire fighters
		for (Command next : heard) {
//            Logger.info(sid + " Heard " + next);
            if(next instanceof AKSpeak) {
            	AKSpeak message = (AKSpeak) next;
            	KnowledgeMsg msg = (KnowledgeMsg) Msg.fromBytes(message.getContent());
            	if(msg == null) {
            		continue;
            	}
            	Logger.debug(String.format("at %d %s received %s", time, sid, msg));
            	Component c = components.containsKey(msg.id)
            			? components.get(msg.id)
            			: newComponent(msg, time);
            	c.loadKnowledge(msg, time);
            	components.put(msg.id, c);
            }
        }		
		
		// Evaluate ensembles
		for(int cId : components.keySet()) {
			for(int mId : components.keySet()) {
				if(cId == mId) {
					continue;
				}
				
				Component coordinator = components.get(cId);
				Component member = components.get(mId);
				for(Ensemble ensemble : ensembles) {
					if(ensemble.isSatisfied(coordinator, member)) {
						Msg msg = ensemble.getMessage(coordinator, member);
						sendSpeak(time, CHANNEL_OUT, msg.getBytes());
						Logger.debug(String.format("at %d %s sending msg %s", time, sid, msg));
					}
				}
			}
		}
		
		adaptationManager.reason();
		// TODO: check duration 
		
        sendRest(time);
	}
	
	private Component newComponent(KnowledgeMsg msg, int time) {
		Component c = new Component();
		c.loadKnowledge(msg, time);
		
		if(H1_MECHANISM) {
			correlationManager.getComponentManager().addComponent(new cz.cuni.mff.d3s.rcrs.af.correlation.ComponentImpl(c));
		}
		if(H2_MECHANISM) {
			isolationManager.getComponentManager().addComponent(new cz.cuni.mff.d3s.rcrs.af.componentisolation.ComponentImpl(c));
		}
		
		return c;
	}

	@Override
	public String toString() {
		return sid;
	}
	
	public Set<Ensemble> getEnsembles() {
		return ensembles;
	}
	
	public void addEnsemble(Ensemble ensemble) {
		ensembles.add(ensemble);
	}
}
