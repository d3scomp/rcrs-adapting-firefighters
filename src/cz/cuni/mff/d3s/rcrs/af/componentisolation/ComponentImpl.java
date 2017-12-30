package cz.cuni.mff.d3s.rcrs.af.componentisolation;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_MOVE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.componentisolation.Port;
import cz.cuni.mff.d3s.rcrs.af.Component;

public class ComponentImpl implements cz.cuni.mff.d3s.metaadaptation.componentisolation.Component {

	private final Component component;
	private Set<Port> ports;
	
	public ComponentImpl(Component component) {
		this.component = component;
		ports = new HashSet<>();
		ports.add(new Port() {
			@Override
			public Set<String> getExposedKnowledge() {
				Set<String> ek = new HashSet<>();
				ek.add(KNOWLEDGE_FIRE_TARGET);
				return ek;
			}});
	}
	
	@Override
	public Map<String, Object> getKnowledge() {
		return component.getKnowledge();
	}

	@Override
	public Set<String> getFaultyKnowledge() {		
		if(component.getKnowledge().containsKey(KNOWLEDGE_CAN_MOVE)
				&& !(boolean)component.getKnowledge().get(KNOWLEDGE_CAN_MOVE)) {
			// Search for burning buildings no longer works due to malfunction
			if(!component.getFaultyKnowledge().contains(KNOWLEDGE_FIRE_TARGET)) {
				component.getFaultyKnowledge().add(KNOWLEDGE_FIRE_TARGET);
			}
		}
		return component.getFaultyKnowledge();
	}


	@Override
	public Set<Port> getPorts() {
		return ports;
	}

	@Override
	public void removePort(Port port) {
		ports.remove(port);
		if(port.getExposedKnowledge().contains(KNOWLEDGE_FIRE_TARGET)) {
			component.getExposedKnowledge().remove(KNOWLEDGE_FIRE_TARGET);
		}
		
	}

}
