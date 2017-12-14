package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_EXTINGUISHING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.metaadaptation.correlation.CorrelationMetadataWrapper;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class TargetFireZoneEnsemble extends Ensemble {

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
				if(t.get(Ensemble.getMemberFieldName(KNOWLEDGE_POSITION)) == null
						|| t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_POSITION)) == null) {
					return false;
				}
				
				boolean memberIsFree = (!(boolean) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_EXTINGUISHING)))
						&& t.get(Ensemble.getMemberFieldName(KNOWLEDGE_FIRE_TARGET)) == null;
				boolean coordinatorAtFire = (boolean) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_EXTINGUISHING));
				int distance = model.getDistance(
						((CorrelationMetadataWrapper<EntityID>) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_POSITION)))
						.getValue(),
						((CorrelationMetadataWrapper<EntityID>) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_POSITION)))
						.getValue());
//				Logger.info("Distance: " + distance);
				boolean areClose = distance < 70_000; // TODO: move to constants
				
				return memberIsFree && coordinatorAtFire && areClose;
			}
		});
	}

	@Override
	public String getMediatedKnowledge() {
		return KNOWLEDGE_FIRE_TARGET;
	}

	@Override
	public Set<String> getAssumedCoordKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {KNOWLEDGE_FIRE_TARGET, KNOWLEDGE_POSITION, KNOWLEDGE_EXTINGUISHING}));
	}

	@Override
	public Set<String> getAssumedMemberKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {KNOWLEDGE_FIRE_TARGET, KNOWLEDGE_POSITION, KNOWLEDGE_EXTINGUISHING}));
	}

}
