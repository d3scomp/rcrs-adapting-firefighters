package cz.cuni.mff.d3s.rcrs.af;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.sensors.FFDistanceSensor;
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
import rescuecore2.worldmodel.EntityID;

public class FireStation extends StandardAgent<Building> {

	private final String sid;
	private final int CHANNEL_IN = 1;
	private final int CHANNEL_OUT = 2;

	private final Set<Ensemble> ensembles = new HashSet<>();
	private final Map<String, IComponent> components = new HashMap<>();
	private final List<FFDistanceSensor> fireFighterDistances = new ArrayList<>();

	public FireStation(int id) {
		this.sid = String.format("FS%d", id);
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE, StandardEntityURN.HYDRANT,
				StandardEntityURN.GAS_STATION);
		Logger.info(sid + " connected");

		// Create refill components
		for (StandardEntity refill : model) {
			if (refill instanceof Hydrant || refill instanceof Refuge) {
				IComponent c = new RefillComponent(refill.getID());
				components.put(c.getSid(), c);
			}
		}

		ensembles.add(TargetFireZoneEnsemble.getInstance(model));
		ensembles.add(RefillStationEnsemble.getInstance(model));
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
		if (time == (config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY) + 2)) {
			// All fire fighter components should be registered by now
			List<FFComponent> ff = new ArrayList<>();
			for (IComponent c : components.values()) {
				if (c instanceof FFComponent) {
					ff.add((FFComponent) c);
				}
			}
			for (FFComponent f1 : ff) {
				for (FFComponent f2 : ff) {
					if (f1 != f2) {
						fireFighterDistances.add(new FFDistanceSensor(this, f1.getEId(), f2.getEId()));
					}
				}
			}
		}
		System.out.println(time);

		// Receive messages from fire fighters
		for (Command next : heard) {
			if (next instanceof AKSpeak) {
				AKSpeak message = (AKSpeak) next;
				KnowledgeMsg msg = (KnowledgeMsg) Msg.fromBytes(message.getContent());
				if (msg == null) {
					continue;
				}
				Logger.info(String.format("at %d %s received %s", time, sid, msg));
				IComponent c = components.containsKey(msg.getSid()) ? components.get(msg.getSid()) : new FFComponent();
				c.loadKnowledge(msg, time);
				components.put(c.getSid(), c);
			}
		}
		
		// sense distances between fire fighters
		for(FFDistanceSensor sensor : fireFighterDistances) {
			System.out.println("Distance sensor sensing");
			sensor.sense(time);
		}

		// check vacant refill stations
		for (String c1 : components.keySet()) {
			if (components.get(c1) instanceof RefillComponent) {
				RefillComponent refillStation = (RefillComponent) components.get(c1);
				boolean vacant = true;

				for (String c2 : components.keySet()) {
					if (components.get(c2) instanceof FFComponent) {
						FFComponent fireFighter = (FFComponent) components.get(c2);

						EntityID ffPositionId = fireFighter.getPosition();
						EntityID rsId = refillStation.getId();
						if (ffPositionId.equals(rsId)) {
							Logger.info(String.format("%s occupied by %s", refillStation, fireFighter));
							vacant = false;
						}
					}
				}

				refillStation.setVacant(vacant);
			}
		}

		// Evaluate ensembles
		for (String cId : components.keySet()) {
			for (String mId : components.keySet()) {
				if (cId.equals(mId)) {
					continue;
				}

				IComponent coordinator = components.get(cId);
				IComponent member = components.get(mId);
				for (Ensemble ensemble : ensembles) {
					if (ensemble.isSatisfied(coordinator, member)) {
						Logger.info(String.format("Ensemble %s satisfied for %s and %s",
								ensemble.getClass().getSimpleName(), coordinator.getSid(), member.getSid()));
						Msg msg = ensemble.getMessage(coordinator, member);
						sendSpeak(time, CHANNEL_OUT, msg.getBytes());
						Logger.info(String.format("at %d %s sending msg %s", time, sid, msg));
					}
				}
			}
		}

		sendRest(time);
	}

	public double getFFDistance(EntityID fireFighter1, EntityID fireFighter2) {
		return model.getDistance(fireFighter1, fireFighter2);
	}

	@Override
	public String toString() {
		return sid;
	}
}
