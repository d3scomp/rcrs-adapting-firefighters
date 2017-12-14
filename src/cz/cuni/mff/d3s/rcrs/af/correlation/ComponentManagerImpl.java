package cz.cuni.mff.d3s.rcrs.af.correlation;

import java.util.HashSet;
import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.correlation.Component;
import cz.cuni.mff.d3s.metaadaptation.correlation.ComponentManager;

public class ComponentManagerImpl implements ComponentManager {

	private final Set<Component> components;
	
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
