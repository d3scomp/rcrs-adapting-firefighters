package cz.cuni.mff.d3s.rcrs.af.correlation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.metaadaptation.correlation.ConnectorManager;
import cz.cuni.mff.d3s.metaadaptation.correlation.DynamicConnector;
import cz.cuni.mff.d3s.rcrs.af.Ensemble;
import cz.cuni.mff.d3s.rcrs.af.FireStation;

public class ConnectorManagerImpl implements ConnectorManager {

	private final FireStation fireStation;
	private final Set<DynamicConnector> connectors;

	public ConnectorManagerImpl(FireStation fireStation) {
		this.fireStation = fireStation;
		connectors = new HashSet<>();
	}

	@Override
	public Set<DynamicConnector> getConnectors() {
		return connectors;
	}

	@Override
	public DynamicConnector addConnector(Predicate<Map<String, Object>> filter, MediatedKnowledge mediatedKnowledge) {
		if (filter == null) {
			// undeploy connector
			for (DynamicConnector c : connectors) {
				DynamicConnectorImpl dci = (DynamicConnectorImpl) c;
				if (dci.getEnsemble().getMediatedKnowledge().equals(mediatedKnowledge.correlationSubject)) {
					connectors.remove(c);
					for (Ensemble e : fireStation.getEnsembles()) {
						if (e.getMediatedKnowledge().equals(mediatedKnowledge.correlationSubject)) {
							fireStation.getEnsembles().remove(e);
							break;
						}
					}
					break;
				}
			}
			return null;
		}
		// deploy connector
		DynamicConnectorImpl connector = new DynamicConnectorImpl(filter, mediatedKnowledge);

		connectors.add(connector);
		fireStation.addEnsemble(connector.getEnsemble());

		return connector;
	}

}
