package cz.cuni.mff.d3s.rcrs.af.ensembles;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILL_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.components.RefillComponent.KNOWLEDGE_REFILL_ID;
import static cz.cuni.mff.d3s.rcrs.af.components.RefillComponent.KNOWLEDGE_REFILL_VACANT;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.comm.RefillMsg;
import cz.cuni.mff.d3s.rcrs.af.components.IComponent;
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
				//int memberId = (int) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_ID));
				EntityID memberPosition = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_POSITION));
				EntityID memberRefill = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_REFILL_TARGET));
				EntityID coordId = (EntityID) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_REFILL_ID));
				boolean coordVacant = (boolean) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_REFILL_VACANT));
				
				if(memberPosition == null || coordId == null) {
					return false;
				}
				
				if(memberPosition.equals(coordId)) {
					return true;
				}
				
				if(!coordVacant) {
					return false;
				}
				
				if(memberRefill == null) {
					return true;
				}
				
				int currentDistance = model.getDistance(memberPosition, memberRefill);
				int newDistance = model.getDistance(memberPosition, coordId);
				
				if(newDistance <= currentDistance) {
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
				KNOWLEDGE_REFILL_VACANT}));
	}

	@Override
	public Set<String> getAssumedMemberKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {
				KNOWLEDGE_ID,
				KNOWLEDGE_POSITION,
				KNOWLEDGE_REFILL_TARGET}));
	}
	
	@Override
	public Msg getMessage(IComponent coordinator, IComponent member) {
		return new RefillMsg((int) member.getKnowledge().get(KNOWLEDGE_ID),
				(EntityID) coordinator.getKnowledge().get(KNOWLEDGE_REFILL_ID));
	}

}
