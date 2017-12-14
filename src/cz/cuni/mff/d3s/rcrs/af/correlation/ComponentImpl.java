package cz.cuni.mff.d3s.rcrs.af.correlation;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_DETECT_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_MOVE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_EXTINGUISHING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_WATER;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.correlation.Component;
import cz.cuni.mff.d3s.metaadaptation.correlation.ComponentPort;
import cz.cuni.mff.d3s.metaadaptation.correlation.CorrelationMetadataWrapper;
import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import rescuecore2.worldmodel.EntityID;

public class ComponentImpl implements Component {
	
	private int id = -1;
	private Set<String> faultyKnowledge = new HashSet<>();
	
	private Set<ComponentPort> ports;
	private Map<String, Object> knowledge;
	
	public ComponentImpl() {
		knowledge = new HashMap<>();
		ports = new HashSet<>();
	}

	public void loadKnowledge(KnowledgeMsg msg, int time) {
		// Load with time and wrap
		// Convert time from s to ms
		this.id = msg.id;
		knowledge.put(KNOWLEDGE_ID, msg.id);
		knowledge.put(KNOWLEDGE_FIRE_TARGET, msg.target);
		CorrelationMetadataWrapper<EntityID> position = new CorrelationMetadataWrapper<>(msg.position, KNOWLEDGE_POSITION, time*1000);
		knowledge.put(KNOWLEDGE_POSITION, position);
		knowledge.put(KNOWLEDGE_WATER, msg.water);
		knowledge.put(KNOWLEDGE_EXTINGUISHING, msg.extinguishing);
		knowledge.put(KNOWLEDGE_CAN_MOVE, msg.canMove);
		CorrelationMetadataWrapper<List<EntityID>> buildings = new CorrelationMetadataWrapper<>(msg.burningBuildings, KNOWLEDGE_BURNING_BUILDINGS, time*1000);
		if(!msg.canDetectBuildings) {
			buildings.malfunction();
		}
		knowledge.put(KNOWLEDGE_BURNING_BUILDINGS, buildings);
		knowledge.put(KNOWLEDGE_CAN_DETECT_BUILDINGS, msg.canDetectBuildings);
		
	}

	@Override
	public Map<String, Object> getKnowledge() {
		return knowledge;
	}

	@Override
	public Set<String> getFaultyKnowledge() {		
		if(knowledge.containsKey(KNOWLEDGE_CAN_DETECT_BUILDINGS)
				&& !(boolean)knowledge.get(KNOWLEDGE_CAN_DETECT_BUILDINGS)) {
			// Search for burning buildings no longer works due to malfunction
			if(!faultyKnowledge.contains(KNOWLEDGE_BURNING_BUILDINGS)) {
				faultyKnowledge.add(KNOWLEDGE_BURNING_BUILDINGS);
			}
		}
		return faultyKnowledge;
	}

	@Override
	public Set<ComponentPort> getPorts() {
		return ports;
	}

	@Override
	public ComponentPort addPort(Set<String> exposedKnowledge) {
		ComponentPort port = new ComponentPortImpl(exposedKnowledge);
		ports.add(port);
		
		return port;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("FF" + id + " [");
		
		for(EntityID building : ((CorrelationMetadataWrapper<List<EntityID>>)knowledge.get(KNOWLEDGE_BURNING_BUILDINGS)).getValue()) {
			buffer.append(building + ", ");
		}
		buffer.append("]");
		return buffer.toString();
	}
	
}
