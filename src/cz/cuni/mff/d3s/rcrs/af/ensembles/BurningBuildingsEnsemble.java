package cz.cuni.mff.d3s.rcrs.af.ensembles;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.comm.BuildingsMsg;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.components.IComponent;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

public class BurningBuildingsEnsemble extends Ensemble {
	
	private static BurningBuildingsEnsemble INSTANCE = null;
	
	public static BurningBuildingsEnsemble getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new BurningBuildingsEnsemble();
		}
		
		return INSTANCE;
	}
	
	private BurningBuildingsEnsemble() {
		super(new Predicate<Map<String,Object>>(){
			@Override
			public boolean test(Map<String, Object> t) {
				int coordId = (int) t.get(getCoordinatorFieldName(KNOWLEDGE_ID));
				int memberId = (int) t.get(getMemberFieldName(KNOWLEDGE_ID));
				@SuppressWarnings("unchecked")
				Set<EntityID> burningBuildings = (Set<EntityID>) t.get(getCoordinatorFieldName(KNOWLEDGE_BURNING_BUILDINGS));

				boolean satisfied = coordId != memberId;
				
				if(satisfied) {
					StringBuilder builder = new StringBuilder();
					for(EntityID building : burningBuildings) {
						builder.append(building.getValue());
					}
					Logger.debug(String.format("BBE:\n"
							+ "\tcId: FF%d\n"
							+ "\tmId: FF%d\n"
							+ "\tbb: %s\n", coordId, memberId, builder.toString()));
				}
				
				return satisfied;
			}
		});
	}

	@Override
	public String getMediatedKnowledge() {
		return KNOWLEDGE_BURNING_BUILDINGS;
	}

	@Override
	public Set<String> getAssumedCoordKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {
				KNOWLEDGE_ID,
				KNOWLEDGE_BURNING_BUILDINGS}));
	}

	@Override
	public Set<String> getAssumedMemberKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] {
				KNOWLEDGE_ID,
				KNOWLEDGE_BURNING_BUILDINGS}));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Msg getMessage(IComponent coordinator, IComponent member) {
		return new BuildingsMsg((int) coordinator.getKnowledge().get(KNOWLEDGE_ID),
				(Set<EntityID>) coordinator.getKnowledge().get(KNOWLEDGE_BURNING_BUILDINGS));
	}

}
