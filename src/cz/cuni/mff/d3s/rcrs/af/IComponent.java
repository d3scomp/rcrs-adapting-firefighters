package cz.cuni.mff.d3s.rcrs.af;

import java.util.Map;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.comm.KnowledgeMsg;

public interface IComponent {

	void loadKnowledge(KnowledgeMsg msg, int time);
	Map<String, Object> getKnowledge();
	Set<String> getExposedKnowledge();
	String getSid();
}
