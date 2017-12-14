package cz.cuni.mff.d3s.rcrs.af.modes;

import java.util.function.Predicate;
import java.util.function.Function;

import cz.cuni.mff.d3s.metaadaptation.modeswitch.Mode;
import cz.cuni.mff.d3s.metaadaptation.modeswitch.Transition;

public class TransitionImpl implements Transition {

	private final Mode from;
	private final Mode to;
	private final Predicate<Void> guard;
	private final Function<Void, Void> action;
	private int priority;
	
	public TransitionImpl(Mode from, Mode to, Predicate<Void> guard, Function<Void, Void> action) {
		this.from = from;
		this.to= to;
		this.guard = guard;
		this.action = action;
		priority = 1;
	}
	
	public TransitionImpl(Mode from, Mode to, Predicate<Void> guard) {
		this(from, to, guard, null);
	}
	
	@Override
	public Mode getFrom() {
		return from;
	}

	@Override
	public Mode getTo() {
		return to;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public Predicate<Void> getGuard() {
		return guard;
	}
	
	public void invokeAction() {
		if(action != null) {
			action.apply(null);
		}
	}

}
