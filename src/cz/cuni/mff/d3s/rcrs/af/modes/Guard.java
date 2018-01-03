package cz.cuni.mff.d3s.rcrs.af.modes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.IFFComponent;

public abstract class Guard {
	
	protected final Map<String, Double> parameters;
	protected final IFFComponent component;
	protected Predicate<Void> predicate;

	public Guard(IFFComponent component){
		this.component = component;
		parameters = new HashMap<>();
		specifyParameters();
		predicate = setPredicate();
	}

	public Map<String, Double> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	public void setParameter(String name, double value) {
		if (parameters.containsKey(name)) {
			parameters.put(name, value);
		} else {
			throw new IllegalArgumentException(
					String.format("The %s parameter doesn't exists in %s", name, this.getClass().getName()));
		}
	}

	public Predicate<Void> getPredicate() {
		return predicate;
	}
	
	public abstract boolean isSatisfied();
	
	protected abstract Predicate<Void> setPredicate();

	public abstract void specifyParameters();
}
