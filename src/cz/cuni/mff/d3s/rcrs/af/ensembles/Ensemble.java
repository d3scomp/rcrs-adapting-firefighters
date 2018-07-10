package cz.cuni.mff.d3s.rcrs.af.ensembles;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.components.IComponent;

public abstract class Ensemble {

	private static final String COORDINATOR_KEY = "coordinator";
	private static final String MEMBER_KEY = "member";

	private final Predicate<Map<String, Object>> filter;

	public Ensemble(Predicate<Map<String, Object>> filter) {
		this.filter = filter;
	}

	public boolean isSatisfied(IComponent coordinator, IComponent member) {
		Set<String> coordinatorExposedKnowledge = coordinator.getExposedKnowledge();
		Set<String> memberExposedKnowledge = member.getExposedKnowledge();

		if (!coordinatorExposedKnowledge.containsAll(getAssumedCoordKnowledge())) {
			return false;
		}

		if (!memberExposedKnowledge.containsAll(getAssumedMemberKnowledge())) {
			return false;
		}

		Map<String, Object> knowledge = new HashMap<>();
		Map<String, Object> exposedKnowledge = coordinator.getKnowledge();
		for (String key : coordinatorExposedKnowledge) {
			knowledge.put(getCoordinatorFieldName(key), exposedKnowledge.get(key));
		}
		exposedKnowledge = member.getKnowledge();
		for (String key : memberExposedKnowledge) {
			knowledge.put(getMemberFieldName(key), exposedKnowledge.get(key));
		}

		return filter.test(knowledge);
	}

	public static String getCoordinatorFieldName(String fieldName) {
		return String.format("%s.%s", COORDINATOR_KEY, fieldName);
	}

	public static String getMemberFieldName(String fieldName) {
		return String.format("%s.%s", MEMBER_KEY, fieldName);
	}

	public abstract Msg getMessage(IComponent coordinator, IComponent member);
	
	public abstract String getMediatedKnowledge();

	public abstract Set<String> getAssumedCoordKnowledge();

	public abstract Set<String> getAssumedMemberKnowledge();

}
