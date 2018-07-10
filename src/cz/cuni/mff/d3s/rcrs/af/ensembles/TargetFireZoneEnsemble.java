package cz.cuni.mff.d3s.rcrs.af.ensembles;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.MAX_SEPARATION_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ENTITY_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_FIREFIGHTER;
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

public class TargetFireZoneEnsemble extends Ensemble {

	private enum msgClass {
		Ensemble;
	}

	static int computedDistance;

	private static TargetFireZoneEnsemble INSTANCE = null;

	private static final Log log = new Log("TargetFireZoneEnsemble");

	public static TargetFireZoneEnsemble getInstance(StandardWorldModel model, FireStation fireStation) {
		if (INSTANCE == null) {
			INSTANCE = new TargetFireZoneEnsemble(model, fireStation);
		}

		return INSTANCE;
	}

	private TargetFireZoneEnsemble(StandardWorldModel model, FireStation fireStation) {
		super(new Predicate<Map<String,Object>>(){
			@Override
			public boolean test(Map<String, Object> t) {
				EntityID memberPosition = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_POSITION));
				EntityID coordPosition = (EntityID) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_POSITION));
				if(memberPosition == null || coordPosition == null) {
					return false;
				}

				int coordId = (int) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_ID));
				EntityID coordEid = (EntityID) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_ENTITY_ID));
				int memberId = (int) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_ID));
				EntityID memberEid = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_ENTITY_ID));
				boolean coordNotMember = t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_HELP_TARGET)) == null;
				boolean coordExtinguishing = t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_MODE)) == Mode.Extinguish;
				EntityID memberHelpTarget = (EntityID) t.get(Ensemble.getMemberFieldName(KNOWLEDGE_HELP_TARGET));
				int helpingFireFighter = (int) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_HELPING_FIREFIGHTER));
				int helpingDistance = (int) t.get(Ensemble.getCoordinatorFieldName(KNOWLEDGE_HELPING_DISTANCE));
				boolean memberIsFree = memberHelpTarget == null;
				boolean alreadyAMember = !memberIsFree && memberId == helpingFireFighter && memberHelpTarget.equals(coordPosition);
				boolean memberSearching = t.get(Ensemble.getMemberFieldName(KNOWLEDGE_MODE)) == Mode.Search;
				
				int newDistance = model.getDistance(memberPosition, coordPosition);
				boolean newDistanceDecreasing = fireStation.getFFDistanceLrb(coordEid, memberEid) < 0;
				
				boolean newIsCloser = newDistance < helpingDistance
						&& newDistance < MAX_SEPARATION_DISTANCE;
				
				boolean satisfied = coordNotMember && coordExtinguishing && memberSearching
						&& ((memberIsFree && newIsCloser && newDistanceDecreasing) || alreadyAMember);

				log.i(0, msgClass.Ensemble,
						"\n\tcId: FF%d\n"
						+ "\tmId: FF%d\n"
						+ "\tcoordNotMember: %s\n"
						+ "\tcoordExtinguishing: %s\n"
						+ "\tmemberSearching: %s\n"
						+ "\tmemberIsFree: %s\n"
						+ "\tnewIsCloser: %s\n"
						+ "\tnewDistanceDecreasing: %s\n"
						+ "\tLrb: %f\n"
						+ "\talreadyAMember: %s\n"
						+ "\tSAT: %s",
						coordId, memberId, coordNotMember, coordExtinguishing,
						memberSearching, memberIsFree, newIsCloser, newDistanceDecreasing,
						fireStation.getFFDistanceLrb(coordEid, memberEid),
						alreadyAMember, satisfied);
					
				computedDistance = newDistance;
				
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
		return new HashSet<>(Arrays.asList(new String[] { KNOWLEDGE_ID, KNOWLEDGE_ENTITY_ID, KNOWLEDGE_HELP_TARGET,
				KNOWLEDGE_HELPING_DISTANCE, KNOWLEDGE_POSITION, KNOWLEDGE_MODE, KNOWLEDGE_BURNING_BUILDINGS,
				KNOWLEDGE_HELPING_FIREFIGHTER }));
	}

	@Override
	public Set<String> getAssumedMemberKnowledge() {
		return new HashSet<>(Arrays.asList(new String[] { KNOWLEDGE_ID, KNOWLEDGE_ENTITY_ID, KNOWLEDGE_HELP_TARGET,
				KNOWLEDGE_POSITION, KNOWLEDGE_MODE }));
	}

	@Override
	public Msg getMessage(IComponent coordinator, IComponent member) {
		return new TargetMsg((int) member.getKnowledge().get(KNOWLEDGE_ID),
				(int) coordinator.getKnowledge().get(KNOWLEDGE_ID),
				(EntityID) coordinator.getKnowledge().get(KNOWLEDGE_POSITION), computedDistance);
	}

}
