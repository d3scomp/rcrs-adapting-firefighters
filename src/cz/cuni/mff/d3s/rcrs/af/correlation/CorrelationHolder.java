package cz.cuni.mff.d3s.rcrs.af.correlation;

import cz.cuni.mff.d3s.metaadaptation.MetaAdaptationManager;
import cz.cuni.mff.d3s.metaadaptation.correlation.CorrelationManager;
import cz.cuni.mff.d3s.rcrs.af.FireStation;

public class CorrelationHolder {
	
	private final ComponentManagerImpl componentManager;
	private final ConnectorManagerImpl connectorManager;
	private final CorrelationManager correlationManager;
	
	public CorrelationHolder(FireStation fireStation) {
		componentManager = new ComponentManagerImpl();
		connectorManager = new ConnectorManagerImpl(fireStation);
		correlationManager = new CorrelationManager(componentManager, connectorManager);

		correlationManager.setVerbosity(true);
		correlationManager.setDumpValues(false);
	}
	
	public void registerAt(MetaAdaptationManager manager) {
		manager.addAdaptation(correlationManager);
	}
	
	public ComponentManagerImpl getComponentManager() {
		return componentManager;
	}
	
	public ConnectorManagerImpl getConnectorManager() {
		return connectorManager;
	}
	
}
