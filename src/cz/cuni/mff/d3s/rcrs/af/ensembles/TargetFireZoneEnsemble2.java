package cz.cuni.mff.d3s.rcrs.af.ensembles;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.FIRE_MAX_HELP_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELP_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_MODE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.FireStation;
import cz.cuni.mff.d3s.rcrs.af.Log;
import cz.cuni.mff.d3s.rcrs.af.comm.Msg;
import cz.cuni.mff.d3s.rcrs.af.comm.TargetMsg;
import cz.cuni.mff.d3s.rcrs.af.components.IComponent;
import cz.cuni.mff.d3s.rcrs.af.modes.Mode;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class TargetFireZoneEnsemble2 extends Ensemble {

	private enum msgClass {
		Ensemble;
	}
	
	private static final int FIRE_SIZE_FOR_HELP = 3;

	private static TargetFireZoneEnsemble2 INSTANCE = null;

	private static final Log log = new Log("TargetFireZoneEnsemble2");

	public static TargetFireZoneEnsemble2 getInstance(StandardWorldModel model, FireStation fireStation) {
		if (INSTANCE == null) {
			INSTANCE = new TargetFireZoneEnsemble2(model, fireStation);
		}

		return INSTANCE;
	}

	private TargetFireZoneEnsemble2(StandardWorldModel model, FireStation fireStation) {
		super(new Predicate<Map<String,Object>>(){
			@Override
			public boolean test(Map<String, Object> t) {
				EntityID memberPosition = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_POSITION));
				EntityID coordPosition = (EntityID) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_POSITION));
				if(memberPosition == null || coordPosition == null) {
					return false;
				}
				
				int coordId = (int) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_ID));
				int memberId = (int) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_ID));
				EntityID memberHelpTarget = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_HELP_TARGET));
				
				@SuppressWarnings("unchecked")
				Set<EntityID> coordBurningBuildings = (Set<EntityID>) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_BURNING_BUILDINGS));
				int coordCloseBurningBuildingsCnt = 0;
				for(EntityID building : coordBurningBuildings) {
					if(model.getDistance(coordPosition, building) < fireStation.getMaxDistance()) {
						coordCloseBurningBuildingsCnt += 1;
					}
				}
				
				boolean memberIsFree = memberHelpTarget == null;
				boolean memberSearching = t.get(Ensemble.getMemberFieldName(KNOWLEDGE_MODE)) == Mode.Search;
				boolean memberIsClose = model.getDistance(memberPosition, coordPosition) < FIRE_MAX_HELP_DISTANCE;
								
				boolean satisfied = (coordCloseBurningBuildingsCnt >= FIRE_SIZE_FOR_HELP)
						&& memberIsFree && memberSearching && memberIsClose;

				log.i(0, msgClass.Ensemble,
						"\n\tcId: FF%d\n"
						+ "\tmId: FF%d\n"
						+ "\tmemberSearching: %s\n"
						+ "\tmemberIsFree: %s\n"
						+ "\tcoordBuildingCnt: %d\n"
						+ "\tSAT: %s",
						coordId, memberId, memberSearching, memberIsFree,
						coordCloseBurningBuildingsCnt, satisfied);
				
				return satisfied;
			}
		});
	}

	@Override
	public String getMediatedKnowledge() {
		return KNOWLEDGE_HELP_TARGET;
	}

	@Override
	public Set<String> getAssumedCoordKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] { KNOWLEDGE_ID,
				KNOWLEDGE_POSITION, KNOWLEDGE_BURNING_BUILDINGS }));
	}

	@Override
	public Set<String> getAssumedMemberKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] { KNOWLEDGE_ID, KNOWLEDGE_HELP_TARGET,
				KNOWLEDGE_POSITION, KNOWLEDGE_MODE }));
	}

	@Override
	public Msg getMessage(IComponent coordinator, IComponent member) {
		return new TargetMsg((int) member.getKnowledge().get(KNOWLEDGE_ID),
				(int) coordinator.getKnowledge().get(KNOWLEDGE_ID),
				(EntityID) coordinator.getKnowledge().get(KNOWLEDGE_POSITION), 0);
	}

}
