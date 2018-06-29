package cz.cuni.mff.d3s.rcrs.af;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;
import rescuecore2.worldmodel.EntityID;

public class RefillComponent implements IComponent {

	public static final String KNOWLEDGE_REFILL_ID = "refillId";
	public static final String KNOWLEDGE_REFILL_VACANT = "refillVacant";
	
	
	private final EntityID refillStation;
	private Map<String, Object> knowledge;
	
	public RefillComponent(EntityID refillStation) {
		this.refillStation = refillStation;
		knowledge = new HashMap<>();
		knowledge.put(KNOWLEDGE_REFILL_ID, refillStation);
		knowledge.put(KNOWLEDGE_REFILL_VACANT, true);
	}
	
	@Override
	public Map<String, Object> getKnowledge() {
		return knowledge;
	}

	@Override
	public Set<String> getExposedKnowledge() {
		return knowledge.keySet();
	}
	
	public void setVacant(boolean vacant) {
		knowledge.put(KNOWLEDGE_REFILL_VACANT, vacant);
	}
	
	public EntityID getId() {
		return refillStation;
	}
	
	@Override
	public String getSid() {
		return String.format("RS%d", refillStation.getValue());
	}

	@Override
	public String toString() {
		return getSid();
	}

	@Override
	public void loadKnowledge(KnowledgeMsg msg, int time) {
		throw new UnsupportedOperationException(String.format(
				"The method \"%s\" is not supported by \"%s\"",
				"loadKnowledge", getClass()));
		
	}
}
