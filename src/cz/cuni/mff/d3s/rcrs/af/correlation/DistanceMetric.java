package cz.cuni.mff.d3s.rcrs.af.correlation;

import cz.cuni.mff.d3s.metaadaptation.correlation.metric.Metric;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/**
 * Measures how far apart two components are.
 * 
 *
 */
public class DistanceMetric implements Metric {

	final StandardWorldModel model;
	
	public DistanceMetric(StandardWorldModel model) {
		this.model = model;
	}
	
	@Override
	public double distance(Object value1, Object value2) {

		EntityID component1 = (EntityID) value1;
		EntityID component2 = (EntityID) value2;

		return model.getDistance(component1, component2);

	}

}
