package cz.cuni.mff.d3s.rcrs.af.correlation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.metaadaptation.correlation.ConnectorManager.MediatedKnowledge;
import cz.cuni.mff.d3s.metaadaptation.correlation.ConnectorPort;
import cz.cuni.mff.d3s.metaadaptation.correlation.DynamicConnector;
import cz.cuni.mff.d3s.metaadaptation.correlation.Kind;
import cz.cuni.mff.d3s.rcrs.af.Ensemble;

public class DynamicConnectorImpl implements DynamicConnector {

	private final Set<ConnectorPort> ports;
	private Predicate<Map<String, Object>> filter;
	private MediatedKnowledge mediatedKnowledge;
	private Ensemble ensemble;
	
	public DynamicConnectorImpl(Predicate<Map<String, Object>> filter, MediatedKnowledge mediatedKnowledge){
		if (filter == null) {
			throw new IllegalArgumentException(String.format("The %s argument is null.", "filter"));
		}
		if (mediatedKnowledge == null) {
			throw new IllegalArgumentException(String.format("The %s argument is null.", "mediatedKnowledge"));
		}

		ensemble = new Ensemble(filter) {

			@Override
			public String getMediatedKnowledge() {
				return mediatedKnowledge.correlationSubject;
			}

			@Override
			public Set<String> getAssumedCoordKnowledge() {
				return new HashSet<>(Arrays.asList(new String[] {
						mediatedKnowledge.correlationFilter,
						mediatedKnowledge.correlationSubject}));
			}

			@Override
			public Set<String> getAssumedMemberKnowledge() {
				return new HashSet<>(Arrays.asList(new String[] {
						mediatedKnowledge.correlationFilter,
						mediatedKnowledge.correlationSubject}));
			}
			
		};
		
		this.filter = filter;
		this.mediatedKnowledge = mediatedKnowledge;
		ports = new HashSet<>();
	}
	
	public Ensemble getEnsemble() {
		return ensemble;
	}
	
	@Override
	public ConnectorPort addPort(Set<String> assumedKnowledge, Kind kind) {
		ConnectorPort port = new ConnectorPortImpl(assumedKnowledge, kind);
		ports.add(port);
		return port;
	}

}
