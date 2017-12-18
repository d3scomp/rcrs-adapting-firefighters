package cz.cuni.mff.d3s.rcrs.af.modeswitch;

import java.util.HashSet;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.modeswitch.Component;

public class ComponentManagerImpl implements cz.cuni.mff.d3s.metaadaptation.modeswitch.ComponentManager {

	Set<Component> components;
	
	public ComponentManagerImpl() {
		components = new HashSet<>();
	}
	
	public void addComponent(Component component) {
		components.add(component);
	}
	
	@Override
	public Set<Component> getComponents() {
		return components;
	}

}
