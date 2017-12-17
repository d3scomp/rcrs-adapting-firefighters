package cz.cuni.mff.d3s.rcrs.af.correlation;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_DETECT_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.correlation.ComponentPort;
import cz.cuni.mff.d3s.metaadaptation.correlation.CorrelationMetadataWrapper;
import cz.cuni.mff.d3s.rcrs.af.Component;
import rescuecore2.worldmodel.EntityID;

public class ComponentImpl implements cz.cuni.mff.d3s.metaadaptation.correlation.Component {
	
	private final Component component;
	private Set<ComponentPort> ports;
	
	public ComponentImpl(Component component) {
		this.component = component;
		ports = new HashSet<>();
	}

	@Override
	public Map<String, Object> getKnowledge() {
		Map<String, Object> correlationKnowledge = new HashMap<>(component.getKnowledge());
		// Wrap knowledge with correlation wrapper
		correlationKnowledge.put(KNOWLEDGE_POSITION, 
				new CorrelationMetadataWrapper<>(
						component.getKnowledge().get(KNOWLEDGE_POSITION),
						KNOWLEDGE_POSITION, component.getTime()*1000) /* convert to ms */);
		CorrelationMetadataWrapper<List<EntityID>> buildings = 
				new CorrelationMetadataWrapper<>(
						(List<EntityID>) component.getKnowledge().get(KNOWLEDGE_BURNING_BUILDINGS),
						KNOWLEDGE_BURNING_BUILDINGS, component.getTime()*1000 /* convert to ms */);
		if(!((Boolean) component.getKnowledge().get(KNOWLEDGE_CAN_DETECT_BUILDINGS))) {
			buildings.malfunction();
		}
		correlationKnowledge.put(KNOWLEDGE_BURNING_BUILDINGS, buildings);
		return correlationKnowledge;
	}

	@Override
	public Set<String> getFaultyKnowledge() {		
		if(component.getKnowledge().containsKey(KNOWLEDGE_CAN_DETECT_BUILDINGS)
				&& !(boolean)component.getKnowledge().get(KNOWLEDGE_CAN_DETECT_BUILDINGS)) {
			// Search for burning buildings no longer works due to malfunction
			if(!component.getFaultyKnowledge().contains(KNOWLEDGE_BURNING_BUILDINGS)) {
				component.getFaultyKnowledge().add(KNOWLEDGE_BURNING_BUILDINGS);
			}
		}
		return component.getFaultyKnowledge();
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
		buffer.append("FF" + component.getId() + " [");
		
		for(EntityID building : ((CorrelationMetadataWrapper<List<EntityID>>)component.getKnowledge()
				.get(KNOWLEDGE_BURNING_BUILDINGS)).getValue()) {
			buffer.append(building + ", ");
		}
		buffer.append("]");
		return buffer.toString();
	}
	
}
