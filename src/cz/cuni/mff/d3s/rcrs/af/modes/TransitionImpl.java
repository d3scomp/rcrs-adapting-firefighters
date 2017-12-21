package cz.cuni.mff.d3s.rcrs.af.modes;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.rcrs.af.Component;
import cz.cuni.mff.d3s.rcrs.af.IComponent;

public class TransitionImpl implements cz.cuni.mff.d3s.metaadaptation.modeswitch.Transition,
		cz.cuni.mff.d3s.metaadaptation.modeswitchprops.Transition {

	private final ModeImpl from;
	private final ModeImpl to;
	private final Guard guard;
	private final Function<Void, Void> action;
	private int priority;
	private final IComponent component;

	public TransitionImpl(ModeImpl from, ModeImpl to, Guard guard, Function<Void, Void> action, IComponent component) {
		this.from = from;
		this.to = to;
		this.guard = guard;
		this.action = action;
		this.component = component;
		priority = 1;
	}

	public TransitionImpl(ModeImpl from, ModeImpl to, Guard guard, IComponent component) {
		this(from, to, guard, null, component);
	}

	@Override
	public ModeImpl getFrom() {
		return from;
	}

	@Override
	public ModeImpl getTo() {
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
		return guard.getPredicate();
	}
	
	public void invokeAction() {
		if (action != null) {
			action.apply(null);
		}
	}

	@Override
	public int hashCode() {
		return from.hashCode() + 3 * to.hashCode() + 7 * priority;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TransitionImpl) {
			TransitionImpl other = (TransitionImpl) obj;
			return this.from.equals(other.from) && this.to.equals(other.to) && this.priority == other.priority;
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("%s -> %s (priority: %d)", from.getClass().getSimpleName(), to.getClass().getSimpleName(),
				priority);
	}

	@Override
	public Map<String, Double> getGuardParams() {
		return guard.getParameters();
	}

	@Override
	public void setGuardParam(String name, double value) {
		guard.setParameter(name, value);

		if (component instanceof Component) {
			// Callback
			component.setGuardParamCallback(this, name, value);
		}
	}

}
