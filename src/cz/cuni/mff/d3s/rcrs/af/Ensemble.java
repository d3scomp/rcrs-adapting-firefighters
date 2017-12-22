package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.H1_FAILURE_IDS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.comm.BuildingsMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.comm.TargetMsg;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

public abstract class Ensemble {

	private static final String COORDINATOR_KEY = "coordinator";
	private static final String MEMBER_KEY = "member";

	private final Predicate<Map<String, Object>> filter;

	public Ensemble(Predicate<Map<String, Object>> filter) {
		this.filter = filter;
	}

	public boolean isSatisfied(Component coordinator, Component member) {
		Set<String> coordinatorExposedKnowledge = coordinator.getExposedKnowledge();
		Set<String> memberExposedKnowledge = member.getExposedKnowledge();

		if (!coordinatorExposedKnowledge.containsAll(getAssumedCoordKnowledge())) {
			return false;
		}

		if (!memberExposedKnowledge.containsAll(getAssumedMemberKnowledge())) {
			return false;
		}

		// limit messages with burning buildings to broken agents only
		if (getMediatedKnowledge().equals(KNOWLEDGE_BURNING_BUILDINGS)) {
			int id = (int) member.getKnowledge().get(KNOWLEDGE_ID);
			
			if (H1_FAILURE_IDS.indexOf(Integer.toString(id)) == -1) {
				return false;
			}
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

	@SuppressWarnings("unchecked")
	public Msg getMessage(Component coordinator, Component member) {
		String mediatedKnowledge = getMediatedKnowledge();
		if (mediatedKnowledge.equals(KNOWLEDGE_FIRE_TARGET)) {
			return new TargetMsg((int) member.getKnowledge().get(KNOWLEDGE_ID),
					(int) coordinator.getKnowledge().get(KNOWLEDGE_ID),
					(EntityID) coordinator.getKnowledge().get(KNOWLEDGE_POSITION),
					(int) member.getKnowledge().get(KNOWLEDGE_HELPING_DISTANCE));
		} else if (mediatedKnowledge.equals(KNOWLEDGE_BURNING_BUILDINGS)) {
			return new BuildingsMsg((int) member.getKnowledge().get(KNOWLEDGE_ID), 
					(List<EntityID>) coordinator.getKnowledge().get(KNOWLEDGE_BURNING_BUILDINGS));
		} else {
			Logger.error("No message type implemented for mediated knowledge: " + mediatedKnowledge);
			return null;
		}
	}

	public static String getCoordinatorFieldName(String fieldName) {
		return String.format("%s.%s", COORDINATOR_KEY, fieldName);
	}

	public static String getMemberFieldName(String fieldName) {
		return String.format("%s.%s", MEMBER_KEY, fieldName);
	}

	public abstract String getMediatedKnowledge();

	public abstract Set<String> getAssumedCoordKnowledge();

	public abstract Set<String> getAssumedMemberKnowledge();

}
