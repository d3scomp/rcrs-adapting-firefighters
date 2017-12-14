package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import cz.cuni.mff.d3s.metaadaptation.correlation.ComponentPort;
import cz.cuni.mff.d3s.metaadaptation.correlation.CorrelationMetadataWrapper;
import cz.cuni.mff.d3s.rcrs.af.comm.BuildingsMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.comm.TargetMsg;
import cz.cuni.mff.d3s.rcrs.af.correlation.ComponentImpl;
import cz.cuni.mff.d3s.rcrs.af.correlation.ComponentPortImpl;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

public abstract class Ensemble {

	private static final String COORDINATOR_KEY = "coordinator";
	private static final String MEMBER_KEY = "member";
	
	private final Predicate<Map<String, Object>> filter;
	
	public Ensemble(Predicate<Map<String, Object>> filter) {
		this.filter = filter;
	}
	
	public boolean isSatisfied(ComponentImpl coordinator, ComponentImpl member) {
		for(ComponentPort coordinatorPort : coordinator.getPorts()) {
			for(ComponentPort memberPort : member.getPorts()) {
				Set<String> coordinatorExposedKnowledge = ((ComponentPortImpl)coordinatorPort).getExposedKnowledge();
				Set<String> memberExposedKnowledge = ((ComponentPortImpl)memberPort).getExposedKnowledge();
				
				if(!coordinatorExposedKnowledge.containsAll(getAssumedCoordKnowledge())){
					continue;
				}
				
				if(!memberExposedKnowledge.containsAll(getAssumedMemberKnowledge())) {
					continue;
				}
				
				// limit messages with burning buildings to broken agents only
				if(getMediatedKnowledge().equals(KNOWLEDGE_BURNING_BUILDINGS)) {
					int id = (int) member.getKnowledge().get(KNOWLEDGE_ID);
					int[] fid = new int[] {3};
					if(IntStream.of(fid).anyMatch(x -> x == id)) { //TODO: variable id
						return false;
					}
				}
				
				Map<String, Object> knowledge = new HashMap<>();
				Map<String, Object> exposedKnowledge = coordinator.getKnowledge();
				for(String key : coordinatorExposedKnowledge) {
					knowledge.put(getCoordinatorFieldName(key), exposedKnowledge.get(key));
				}
				exposedKnowledge = member.getKnowledge();
				for(String key : memberExposedKnowledge) {
					knowledge.put(getMemberFieldName(key), exposedKnowledge.get(key));
				}
								
				return filter.test(knowledge);		
			}
		}
		
		// Exposed knowledge not subset of assumed knowledge
		return false;
	}
	
	public Msg getMessage(ComponentImpl coordinator, ComponentImpl member) {
		String mediatedKnowledge = getMediatedKnowledge();
		if(mediatedKnowledge.equals(KNOWLEDGE_FIRE_TARGET)) {
			return new TargetMsg((int) member.getKnowledge().get(KNOWLEDGE_ID),
					((CorrelationMetadataWrapper<EntityID>) coordinator.getKnowledge().get(KNOWLEDGE_POSITION)).getValue());
		} else if(mediatedKnowledge.equals(KNOWLEDGE_BURNING_BUILDINGS)) {
			CorrelationMetadataWrapper<List<EntityID>> wrapper =
					(CorrelationMetadataWrapper<List<EntityID>>) coordinator.getKnowledge().get(KNOWLEDGE_BURNING_BUILDINGS);
			return new BuildingsMsg((int) member.getKnowledge().get(KNOWLEDGE_ID), wrapper.getValue());
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
