package cz.cuni.mff.d3s.rcrs.af;

import java.util.Collection;
import java.util.EnumSet;

import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;

public class FireStation extends StandardAgent<Building> {

	private final String id;
	private final int CHANNEL_IN = 1;
	private final int CHANNEL_OUT = 2;
	
	public FireStation(int id) {
		this.id = String.format("FS%d", id);
	}
	
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_STATION);
	}

	@Override
	protected void think(int time, ChangeSet changes, Collection<Command> heard) {
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            sendSubscribe(time, CHANNEL_IN, CHANNEL_OUT);
            Logger.info(id + " subscribed to channel " + CHANNEL_IN + " and " + CHANNEL_OUT);
        }
		
		// Receive messages from fire fighters
		
		// Keep list of FF positions and modes, fires, ensembles
		
		// Issue commands
		
		
		
        for (Command next : heard) {
            Logger.info(id + " Heard " + next);
            if(next instanceof AKSpeak) {
            	AKSpeak message = (AKSpeak) next;
            	Logger.info(id + " decoded " + Msg.fromBytes(message.getContent()));
            }
        }
        sendRest(time);
	}

	@Override
	public String toString() {
		return id;
	}
}
