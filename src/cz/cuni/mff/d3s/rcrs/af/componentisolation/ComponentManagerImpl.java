package cz.cuni.mff.d3s.rcrs.af.componentisolation;

import java.util.HashSet;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.componentisolation.Component;
import cz.cuni.mff.d3s.metaadaptation.componentisolation.ComponentManager;

public class ComponentManagerImpl implements ComponentManager {

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
