package cz.cuni.mff.d3s.rcrs.af.correlation;

import java.util.ArrayList;
import java.util.List;

import cz.cuni.mff.d3s.metaadaptation.correlation.metric.Metric;
import rescuecore2.standard.entities.StandardEntity;

/**
 Measures whether components see the same surroundings according to buildings around them. 
 */
public class SurroundingMetric implements Metric {

	@Override
	public double distance(Object value1, Object value2) {

		@SuppressWarnings("unchecked")
		List<StandardEntity> buildings1 = (List<StandardEntity>) value1;
		@SuppressWarnings("unchecked")
		List<StandardEntity> buildings2 = (List<StandardEntity>) value2;
		
		List<StandardEntity> commonBuildings = new ArrayList<>(buildings1);
		commonBuildings.retainAll(buildings2);
		
		int b1Size = buildings1.size();
		int b2Size = buildings2.size();
		int cbSize = commonBuildings.size();
		
		if(b1Size == 0 || b2Size == 0) {
			// Dont compare empty sets
			return Double.NaN;
		}
		
		return (b1Size + b2Size) - 2*cbSize;
	}

}
