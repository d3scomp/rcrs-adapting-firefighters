package cz.cuni.mff.d3s.rcrs.af;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
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

	private final String sid;
	private final int CHANNEL_IN = 1;
	private final int CHANNEL_OUT = 2;

	private final Set<Ensemble> ensembles = new HashSet<>();
	private final Map<String, IComponent> components = new HashMap<>();

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
		
		// TODO: check vacant hydrants

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
								ensemble.getClass(), coordinator.getSid(), member.getSid()));
						Msg msg = ensemble.getMessage(coordinator, member);
						sendSpeak(time, CHANNEL_OUT, msg.getBytes());
						Logger.info(String.format("at %d %s sending msg %s", time, sid, msg));
					}
				}
			}
		}

		sendRest(time);
	}

	@Override
	public String toString() {
		return sid;
	}
}
