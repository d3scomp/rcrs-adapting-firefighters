package cz.cuni.mff.d3s.rcrs.af;

import java.util.Map;
import java.util.Set;

public interface IComponent {

	Map<String, Object> getKnowledge();
	Set<String> getExposedKnowledge();
}
