package cz.cuni.mff.d3s.rcrs.af.componentisolation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.componentisolation.Port;
import cz.cuni.mff.d3s.rcrs.af.IComponent;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

public class ComponentImpl implements cz.cuni.mff.d3s.metaadaptation.componentisolation.Component, IComponent {

	public static final String KNOWLEDGE_CAN_REFILL = "can_refill";
	public static final String KNOWLEDGE_REFILL_ID = "refillId";
	
	
	private final EntityID refillStation;
	private Set<Port> ports;
	private Map<String, Object> knowledge;
	
	public ComponentImpl(EntityID refillStation) {
		this.refillStation = refillStation;
		knowledge = new HashMap<>();
		knowledge.put(KNOWLEDGE_CAN_REFILL, true);
		knowledge.put(KNOWLEDGE_REFILL_ID, refillStation);
		
		ports = new HashSet<>();
		ports.add(new Port() {
			@Override
			public Set<String> getExposedKnowledge() {
				Set<String> ek = new HashSet<>();
				ek.add(KNOWLEDGE_CAN_REFILL);
				return ek;
			}});
	}
	
	public void malfunction() {
		knowledge.put(KNOWLEDGE_CAN_REFILL, false);
	}
	
	@Override
	public Map<String, Object> getKnowledge() {
		return knowledge;
	}

	@Override
	public Set<String> getExposedKnowledge() {
		return knowledge.keySet();
	}
	
	@Override
	public Set<String> getFaultyKnowledge() {		
		if(knowledge.containsKey(KNOWLEDGE_CAN_REFILL)
				&& !(boolean)knowledge.get(KNOWLEDGE_CAN_REFILL)) {
			// refill not working at this refill station
			Set<String> faultyKnowledge = new HashSet<>();
			faultyKnowledge.add(KNOWLEDGE_CAN_REFILL);
			return faultyKnowledge;
		}
		return Collections.emptySet();
	}


	@Override
	public Set<Port> getPorts() {
		return ports;
	}
	
	public EntityID getId() {
		return refillStation;
	}

	@Override
	public void removePort(Port port) {
		ports.remove(port);
		knowledge.remove(KNOWLEDGE_CAN_REFILL);
		Logger.info("Removing port " + KNOWLEDGE_CAN_REFILL + " from station " + refillStation.getValue());
	}
	
	@Override
	public String toString() {
		return String.format("RS %d", refillStation.getValue());
	}

}
