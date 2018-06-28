package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_DETECT_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_EXTINGUISHING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_FIREFIGHTER;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELP_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILLING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILL_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_WATER;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;

/**
 * Mirror component for {@link FireFighter} on the {@link FireStation} side.
 * @author iridin
 *
 */
public class FFComponent implements IComponent {
	protected int id = -1;
	protected int time = -1;
	
	protected Map<String, Object> knowledge;
	
	public FFComponent() {
		knowledge = new HashMap<>();
	}
	
	@Override
	public void loadKnowledge(KnowledgeMsg msg, int time) {
		// Load with time and wrap
		// Convert time from s to ms
		this.id = msg.id;
		this.time = time;
		
		knowledge.put(KNOWLEDGE_ID, msg.id);
		knowledge.put(KNOWLEDGE_FIRE_TARGET, msg.fireTarget);
		knowledge.put(KNOWLEDGE_HELP_TARGET, msg.helpTarget);
		knowledge.put(KNOWLEDGE_REFILL_TARGET, msg.refillTarget);
		knowledge.put(KNOWLEDGE_POSITION, msg.position);
		knowledge.put(KNOWLEDGE_WATER, msg.water);
		knowledge.put(KNOWLEDGE_EXTINGUISHING, msg.extinguishing);
		knowledge.put(KNOWLEDGE_REFILLING, msg.refilling);
		knowledge.put(KNOWLEDGE_BURNING_BUILDINGS, msg.burningBuildings);
		knowledge.put(KNOWLEDGE_CAN_DETECT_BUILDINGS, msg.canDetectBuildings);
		knowledge.put(KNOWLEDGE_HELPING_FIREFIGHTER, msg.helpingFireFighter);
		knowledge.put(KNOWLEDGE_HELPING_DISTANCE, msg.helpingDistance);
	}
	
	@Override
	public Map<String, Object> getKnowledge(){
		return knowledge;
	}
	
	@Override
	public Set<String> getExposedKnowledge(){
		return knowledge.keySet();
	}
	
	@Override
	public String getSid() {
		return String.format("FF%d", id);
	}
	
	@Override
	public String toString() {
		return String.format("FF%d", id);
	}
}
