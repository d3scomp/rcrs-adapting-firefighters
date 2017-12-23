package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_FIREFIGHTER;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_DETECT_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_MOVE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_EXTINGUISHING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILL_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_WATER;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import cz.cuni.mff.d3s.rcrs.af.modes.TransitionImpl;
import rescuecore2.worldmodel.EntityID;

public class Component implements IComponent {
	
	protected int id = -1;
	protected int time = -1;
	protected FireStation fireStation;
	
	protected Map<String, Object> knowledge;
	protected Set<String> exposedKnowledge;
	protected Set<String> faultyKnowledge;
	
	public Component(FireStation fireStation) {
		knowledge = new HashMap<>();
		exposedKnowledge = new HashSet<>();
		faultyKnowledge = new HashSet<>();
		
		this.fireStation = fireStation;
	}

	public void loadKnowledge(KnowledgeMsg msg, int time) {
		// Load with time and wrap
		// Convert time from s to ms
		this.id = msg.id;
		this.time = time;
		
		knowledge.put(KNOWLEDGE_ID, msg.id);
		knowledge.put(KNOWLEDGE_FIRE_TARGET, msg.fireTarget);
		knowledge.put(KNOWLEDGE_REFILL_TARGET, msg.refillTarget);
		knowledge.put(KNOWLEDGE_POSITION, msg.position);
		knowledge.put(KNOWLEDGE_WATER, msg.water);
		knowledge.put(KNOWLEDGE_EXTINGUISHING, msg.extinguishing);
		knowledge.put(KNOWLEDGE_CAN_MOVE, msg.canMove);
		knowledge.put(KNOWLEDGE_BURNING_BUILDINGS, msg.burningBuildings);
		knowledge.put(KNOWLEDGE_CAN_DETECT_BUILDINGS, msg.canDetectBuildings);
		knowledge.put(KNOWLEDGE_HELPING_FIREFIGHTER, msg.helpingFireFighter);
		knowledge.put(KNOWLEDGE_HELPING_DISTANCE, msg.helpingDistance);
		
		if(exposedKnowledge.isEmpty()) {
			exposedKnowledge.addAll(knowledge.keySet());
		}
	}
	
	public int getId() {
		return id;
	}
	
	public int getTime() {
		return time;
	}
	
	public Map<String, Object> getKnowledge(){
		return knowledge;
	}
	
	public Set<String> getExposedKnowledge(){
		return exposedKnowledge;
	}
	
	public Set<String> getFaultyKnowledge(){
		return faultyKnowledge;
	}

	@Override
	public int getWater() {
		return (int) knowledge.get(KNOWLEDGE_WATER);
	}

	@Override
	public int getMaxWater() {
		return fireStation.getMaxWater();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<EntityID> getBurningBuildings() {
		return (List<EntityID>) knowledge.get(KNOWLEDGE_BURNING_BUILDINGS);
	}

	@Override
	public EntityID getFireTarget() {
		return (EntityID) knowledge.get(KNOWLEDGE_FIRE_TARGET);
	}

	@Override
	public EntityID getRefillTarget() {
		return (EntityID) knowledge.get(KNOWLEDGE_REFILL_TARGET);
	}

	@Override
	public EntityID getLocation() {
		return (EntityID) knowledge.get(KNOWLEDGE_POSITION);
	}

	@Override
	public int getHelpingFireFighter() {
		return (int) knowledge.get(KNOWLEDGE_HELPING_FIREFIGHTER);
	}

	@Override
	public EntityID findCloseBurningBuilding() {
		// Relevant only for fire fighter object
		return null;
	}

	@Override
	public void setFireTarget(boolean set) {
		// Relevant only for fire fighter object
	}

	@Override
	public void setRefillTarget(boolean set) {
		// Relevant only for fire fighter object
	}

	@Override
	public void addTransitionCallback(TransitionImpl transition) {
		fireStation.addTransitionCallback(transition, id);
	}

	@Override
	public void removeTransitionCallback(TransitionImpl transition) {
		fireStation.removeTransitionCallback(transition, id);
	}

	@Override
	public void setGuardParamCallback(TransitionImpl transition, String name, double value) {
		fireStation.setGuardParamCallback(transition, name, value, id);
	}
}
