package cz.cuni.mff.d3s.rcrs.af.modes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.metaadaptation.modeswitch.Mode;
import cz.cuni.mff.d3s.metaadaptation.modeswitch.Transition;
import cz.cuni.mff.d3s.rcrs.af.Component;
import cz.cuni.mff.d3s.rcrs.af.IComponent;;

public class ModeChartImpl implements cz.cuni.mff.d3s.metaadaptation.modeswitch.ModeChart,
		cz.cuni.mff.d3s.metaadaptation.modeswitchprops.ModeChart {

	private ModeImpl currentMode;
	private Set<ModeImpl> modes;
	private Set<TransitionImpl> transitions;
	private IComponent component;

	public ModeChartImpl(IComponent component) {

		this.component = component;

		// INIT ###############################################################

		modes = new HashSet<>();
		transitions = new HashSet<>();

		// GUARDS #############################################################

		Guard refill2searchGuard = new ParamGuard(component) {

			private static final String FILLED_LEVEL = "FILLED_LEVEL_TO_LEAVE";

			@Override
			protected void specifyParameters() {
				parameters.put(FILLED_LEVEL, (double) component.getMaxWater());

			}

			@Override
			public boolean isSatisfied() {
				return component.getWater() == parameters.get(FILLED_LEVEL)
						&& component.getBurningBuildings().size() == 0;
			}
		};

		Guard refill2moveToFireGuard = new ParamGuard(component) {

			private static final String FILLED_LEVEL = "FILLED_LEVEL_TO_LEAVE";

			@Override
			protected void specifyParameters() {
				parameters.put(FILLED_LEVEL, (double) component.getMaxWater());
			}

			@Override
			public boolean isSatisfied() {
				return component.getWater() == parameters.get(FILLED_LEVEL)
						&& component.getBurningBuildings().size() > 0;
			}
		};

		Guard moveToRefill2refillGuard = new PredicateGuard(component, new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return component.getRefillTarget() == null
						|| component.getRefillTarget().equals(component.getLocation());
			}
		}) {
		};

		Guard moveToFire2extinguishGuard = new PredicateGuard(component, new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return (component.getFireTarget() == null || component.getFireTarget().equals(component.getLocation())
						|| component.findCloseBurningBuilding() != null); // TODO: consider removing the last statement
			}
		}) {
		};

		Guard extinguish2moveToRefillGuard = new ParamGuard(component) {

			private static final String EMPTY_LEVEL = "EMPTY_LEVEL";

			@Override
			protected void specifyParameters() {
				parameters.put(EMPTY_LEVEL, 0.0);
			}

			@Override
			public boolean isSatisfied() {
				return component.getWater() <= parameters.get(EMPTY_LEVEL);
			}
		};

		Guard extinguish2searchGuard = new ParamGuard(component) {

			private static final String FILLED_LEVEL = "FILLED_LEVEL_TO_CONTINUE";

			@Override
			protected void specifyParameters() {
				parameters.put(FILLED_LEVEL, (double) 0.0);

			}

			@Override
			public boolean isSatisfied() {
				return component.getWater() > parameters.get(FILLED_LEVEL)
						&& component.findCloseBurningBuilding() == null;
			}
		};

		Guard search2extinguishGuard = new PredicateGuard(component, new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return component.findCloseBurningBuilding() != null;
			}
		}) {
		};

		// TODO: check close burning buildings are manageable

		// ACTIONS ############################################################

		Function<Void, Void> moveToRefillAction = new Function<Void, Void>() {
			@Override
			public Void apply(Void t) {
				component.setRefillTarget(true);
				return null;
			}
		};

		Function<Void, Void> moveToFireAction = new Function<Void, Void>() {
			@Override
			public Void apply(Void t) {
				component.setFireTarget(true);
				return null;
			}
		};

		Function<Void, Void> RefillReachedAction = new Function<Void, Void>() {
			@Override
			public Void apply(Void t) {
				component.setRefillTarget(false);
				return null;
			}
		};

		Function<Void, Void> FireReachedAction = new Function<Void, Void>() {
			@Override
			public Void apply(Void t) {
				component.setFireTarget(false);
				return null;
			}
		};

		// MODES ##############################################################

		ModeImpl searchMode = new SearchMode();
		modes.add(searchMode);
		ModeImpl moveToFireMode = new MoveToFireMode();
		modes.add(moveToFireMode);
		ModeImpl moveToRefillMode = new MoveToRefillMode();
		modes.add(moveToRefillMode);
		ModeImpl extinguishMode = new ExtinguishMode();
		modes.add(extinguishMode);
		ModeImpl refillMode = new RefillMode();
		modes.add(refillMode);

		currentMode = searchMode;

		// TRANSITIONS ########################################################

		transitions.add(new TransitionImpl(searchMode, extinguishMode, search2extinguishGuard, component));
		transitions.add(new TransitionImpl(extinguishMode, searchMode, extinguish2searchGuard, component));
		transitions.add(new TransitionImpl(extinguishMode, moveToRefillMode, extinguish2moveToRefillGuard,
				moveToRefillAction, component));
		transitions.add(new TransitionImpl(moveToFireMode, extinguishMode, moveToFire2extinguishGuard,
				FireReachedAction, component));
		transitions.add(new TransitionImpl(moveToRefillMode, refillMode, moveToRefill2refillGuard, RefillReachedAction,
				component));
		transitions.add(
				new TransitionImpl(refillMode, moveToFireMode, refill2moveToFireGuard, moveToFireAction, component));
		transitions.add(new TransitionImpl(refillMode, searchMode, refill2searchGuard, component));

	}

	public Set<ModeImpl> getModes() {
		return modes;
	}

	public ModeImpl getCurrentMode() {
		return currentMode;
	}

	public void setCurrentMode(Class<? extends ModeImpl> mode) {
		for (ModeImpl m : modes) {
			if (m.getClass().equals(mode)) {
				currentMode = m;
				break;
			}
		}
	}

	public Set<TransitionImpl> getTransitions() {
		return transitions;
	}

	public Transition addTransition(Mode from, Mode to, Predicate<Void> guard) {
		Guard predicateGuard = new PredicateGuard(component, guard);
		TransitionImpl t = new TransitionImpl((ModeImpl) from, (ModeImpl) to, predicateGuard, component);
		transitions.add(t);

		if (component instanceof Component) {
			// Callback
			component.addTransitionCallback(t);
		}
		return t;
	}

	public void removeTransition(Transition transition) {
		if (component instanceof Component) {
			// Callback
			component.removeTransitionCallback((TransitionImpl) transition);
		}

		transitions.remove(transition);
	}

	public void decideModeSwitch() {
		// Switch mode only if there is a transition from it
		Set<TransitionImpl> out = getTransitionsFrom(currentMode);
		if (out.isEmpty()) {
			return;
		}

		// Sort transitions by priority
		List<TransitionImpl> sortedOut = sortByPriority(out);

		// Switch first satisfied transition
		for (TransitionImpl transition : sortedOut) {
			if (transition.getGuard().test(null)) {
				// Call the transition listeners before the mode is switched
				if (transition instanceof TransitionImpl) {
					TransitionImpl t = (TransitionImpl) transition;
					t.invokeAction();
				}

				currentMode = transition.getTo();
				break;
			}
		}
	}

	private Set<TransitionImpl> getTransitionsFrom(ModeImpl mode) {
		Set<TransitionImpl> outgoing = new HashSet<>();
		for (TransitionImpl transition : transitions) {
			if (transition.getFrom().equals(mode)) {
				outgoing.add(transition);
			}
		}

		return outgoing;
	}

	private List<TransitionImpl> sortByPriority(Set<TransitionImpl> transitions) {
		List<TransitionImpl> sorted = new ArrayList<>(transitions);
		sorted.sort(Comparator.comparing(t -> -t.getPriority()));

		return sorted;
	}
}
