package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILL_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.componentisolation.ComponentImpl.KNOWLEDGE_CAN_REFILL;
import static cz.cuni.mff.d3s.rcrs.af.componentisolation.ComponentImpl.KNOWLEDGE_REFILL_ID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import rescuecore2.log.Logger;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class RefillStationEnsemble extends Ensemble {

	
	private static RefillStationEnsemble INSTANCE = null;
	
	public static RefillStationEnsemble getInstance(StandardWorldModel model) {
		if(INSTANCE == null) {
			INSTANCE = new RefillStationEnsemble(model);
		}
		
		return INSTANCE;
	}
	
	private RefillStationEnsemble(StandardWorldModel model) {
		super(new Predicate<Map<String,Object>>(){
			@Override
			public boolean test(Map<String, Object> t) {
				EntityID memberPosition = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_POSITION));
				EntityID memberRefill = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_REFILL_TARGET));
				EntityID coordId = (EntityID) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_REFILL_ID));
				if(memberPosition == null || coordId == null) {
					return false;
				}
				
				if(memberRefill == null) {
					return true;
				}
				
				int currentDistance = model.getDistance(memberPosition, memberRefill);
				int newDistance = model.getDistance(memberPosition, coordId);
				
				if(newDistance < currentDistance) {
					Logger.info(String.format("Refill %d in ensemble with FF on %d",
							coordId.getValue(), memberPosition.getValue()));
					return true;
				}
				
				return false;
			}
		});
	}

	@Override
	public String getMediatedKnowledge() {
		return KNOWLEDGE_REFILL_ID;
	}

	@Override
	public Set<String> getAssumedCoordKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {
				KNOWLEDGE_REFILL_ID,
				KNOWLEDGE_CAN_REFILL}));
	}

	@Override
	public Set<String> getAssumedMemberKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {
				KNOWLEDGE_POSITION,
				KNOWLEDGE_REFILL_TARGET}));
	}

}
