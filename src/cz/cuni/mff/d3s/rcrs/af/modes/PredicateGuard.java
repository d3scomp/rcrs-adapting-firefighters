package cz.cuni.mff.d3s.rcrs.af.modes;

import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.IFFComponent;

public class PredicateGuard extends Guard {
	
	public PredicateGuard(IFFComponent component, Predicate<Void> predicate) {
		super(component);
		this.predicate = predicate;
	}
	
	@Override
	public void specifyParameters() {}

	@Override
	protected Predicate<Void> setPredicate() {
		// Set the predicate directly in constructor
		return null;
	}
	
	@Override
	public boolean isSatisfied() {
		return predicate.test(null);
	}

}
