package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_DETECT_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_MOVE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_EXTINGUISHING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_WATER;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;

public class Component {
	
	protected int id = -1;
	protected int time = -1;
	
	protected Map<String, Object> knowledge;
	protected Set<String> exposedKnowledge;
	protected Set<String> faultyKnowledge;
	
	public Component() {
		knowledge = new HashMap<>();
		exposedKnowledge = new HashSet<>();
		faultyKnowledge = new HashSet<>();
	}

	public void loadKnowledge(KnowledgeMsg msg, int time) {
		// Load with time and wrap
		// Convert time from s to ms
		this.id = msg.id;
		this.time = time;
		
		knowledge.put(KNOWLEDGE_ID, msg.id);
		knowledge.put(KNOWLEDGE_FIRE_TARGET, msg.target);
		knowledge.put(KNOWLEDGE_POSITION, msg.position);
		knowledge.put(KNOWLEDGE_WATER, msg.water);
		knowledge.put(KNOWLEDGE_EXTINGUISHING, msg.extinguishing);
		knowledge.put(KNOWLEDGE_CAN_MOVE, msg.canMove);
		knowledge.put(KNOWLEDGE_BURNING_BUILDINGS, msg.burningBuildings);
		knowledge.put(KNOWLEDGE_CAN_DETECT_BUILDINGS, msg.canDetectBuildings);	
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
}
