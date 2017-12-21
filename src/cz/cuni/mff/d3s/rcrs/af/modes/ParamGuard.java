package cz.cuni.mff.d3s.rcrs.af.modes;

import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.IComponent;

public abstract class ParamGuard extends Guard {

	public ParamGuard(IComponent component) {
		super(component);
	}
	
	public Predicate<Void> setPredicate(){
		return new Predicate<Void>(){
			@Override
			public boolean test(Void t) {
				return isSatisfied();
			}
		};
	}

}
