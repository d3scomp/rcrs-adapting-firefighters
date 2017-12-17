package cz.cuni.mff.d3s.rcrs.af.componentisolation;

import cz.cuni.mff.d3s.metaadaptation.MetaAdaptationManager;
import cz.cuni.mff.d3s.metaadaptation.componentisolation.ComponentIsolationManager;

public class IsolationHolder {
	private final ComponentManagerImpl componentManager;
	private final ComponentIsolationManager isolationManager;
	
	public IsolationHolder() {
		componentManager = new ComponentManagerImpl();
		isolationManager = new ComponentIsolationManager(componentManager);

		isolationManager.setVerbosity(true);
	}
	
	public void registerAt(MetaAdaptationManager manager) {
		manager.addAdaptation(isolationManager);
	}
	
	public ComponentManagerImpl getComponentManager() {
		return componentManager;
	}
}
