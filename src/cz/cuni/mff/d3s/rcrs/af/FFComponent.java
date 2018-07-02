package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_FIREFIGHTER;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELP_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ENTITY_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_MODE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILL_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_WATER;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import rescuecore2.worldmodel.EntityID;

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
		knowledge.put(KNOWLEDGE_ENTITY_ID, msg.eid);
		knowledge.put(KNOWLEDGE_FIRE_TARGET, msg.fireTarget);
		knowledge.put(KNOWLEDGE_HELP_TARGET, msg.helpTarget);
		knowledge.put(KNOWLEDGE_REFILL_TARGET, msg.refillTarget);
		knowledge.put(KNOWLEDGE_POSITION, msg.position);
		knowledge.put(KNOWLEDGE_WATER, msg.water);
		knowledge.put(KNOWLEDGE_MODE, msg.mode);
		knowledge.put(KNOWLEDGE_BURNING_BUILDINGS, msg.burningBuildings);
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
	
	public EntityID getPosition() {
		return (EntityID) knowledge.get(KNOWLEDGE_POSITION);
	}
	
	public EntityID getEId() {
		return (EntityID) knowledge.get(KNOWLEDGE_ENTITY_ID);
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
