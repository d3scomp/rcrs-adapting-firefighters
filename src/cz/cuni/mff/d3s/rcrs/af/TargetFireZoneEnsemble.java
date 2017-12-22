package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_EXTINGUISHING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_FIREFIGHTER;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import rescuecore2.log.Logger;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class TargetFireZoneEnsemble extends Ensemble {

	private static final int MAX_SEPARATION_DISTANCE = 70_000;
	
	private static TargetFireZoneEnsemble INSTANCE = null;
	
	public static TargetFireZoneEnsemble getInstance(StandardWorldModel model) {
		if(INSTANCE == null) {
			INSTANCE = new TargetFireZoneEnsemble(model);
		}
		
		return INSTANCE;
	}
	
	private TargetFireZoneEnsemble(StandardWorldModel model) {
		super(new Predicate<Map<String,Object>>(){
			@Override
			public boolean test(Map<String, Object> t) {
				EntityID memberPosition = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_POSITION));
				EntityID coordPosition = (EntityID) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_POSITION));
				if(memberPosition == null || coordPosition == null) {
					return false;
				}
				
				int memberId = (int) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_ID));
				boolean coordExtinguishing = (boolean) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_EXTINGUISHING));
				EntityID memberFireTarget = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_FIRE_TARGET));
				int helpingFireFighter = (int) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_HELPING_FIREFIGHTER));
				int helpingDistance = (int) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_HELPING_DISTANCE));
				boolean memberIsFree = memberFireTarget == null || memberFireTarget.equals(coordPosition);
				boolean coordGotDifferentHelp = helpingFireFighter != -1 && helpingFireFighter != memberId;
				
				int newDistance = model.getDistance(memberPosition, coordPosition);
				
				boolean newIsCloser = newDistance < helpingDistance
						&& newDistance < MAX_SEPARATION_DISTANCE;
				
				boolean satisfied = !coordGotDifferentHelp && memberIsFree && coordExtinguishing && newIsCloser;
				if(satisfied) {
					int coordId = (int) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_ID));
					Logger.debug("TargetFireZoneEnsemble satisfied for " + "FF" + memberId + " and FF" + coordId);
				}
				
				return satisfied;
			}
		});
	}

	@Override
	public String getMediatedKnowledge() {
		return KNOWLEDGE_FIRE_TARGET;
	}

	@Override
	public Set<String> getAssumedCoordKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {
				KNOWLEDGE_FIRE_TARGET,
				KNOWLEDGE_POSITION,
				KNOWLEDGE_EXTINGUISHING,
				KNOWLEDGE_HELPING_FIREFIGHTER}));
	}

	@Override
	public Set<String> getAssumedMemberKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {
				KNOWLEDGE_FIRE_TARGET,
				KNOWLEDGE_POSITION,
				KNOWLEDGE_EXTINGUISHING}));
	}

}
