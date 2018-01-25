package cz.cuni.mff.d3s.rcrs.af;

import java.util.Collection;
import java.util.EnumSet;

import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;

public class FireStation extends StandardAgent<Building> {

	private final String id;
	
	public FireStation(int id) {
		this.id = String.format("FS%d", id);
	}
	
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_STATION);
	}

	@Override
	protected void think(int time, ChangeSet changes, Collection<Command> heard) {
		sendRest(time);
	}

	@Override
	public String toString() {
		return id;
	}
}
