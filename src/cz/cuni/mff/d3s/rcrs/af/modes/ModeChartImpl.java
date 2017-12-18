package cz.cuni.mff.d3s.rcrs.af.modes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import cz.cuni.mff.d3s.metaadaptation.modeswitch.Mode;
import cz.cuni.mff.d3s.metaadaptation.modeswitch.ModeChart;
import cz.cuni.mff.d3s.metaadaptation.modeswitch.Transition;
import cz.cuni.mff.d3s.rcrs.af.IComponent;;

public class ModeChartImpl implements ModeChart {

	private Mode currentMode;
	private Set<Mode> modes;
	private Set<Transition> transitions;
	private IComponent component;

	public ModeChartImpl(IComponent ff) {
		
		component = ff;
		
		// INIT ###############################################################

		modes = new HashSet<>();
		transitions = new HashSet<>();

		// GUARDS #############################################################

		Predicate<Void> refill2searchGuard = new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return ff.getWater() == ff.getMaxWater() && ff.getBurningBuildings().size() == 0;
			}
		};

		Predicate<Void> refill2moveToFireGuard = new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return ff.getWater() == ff.getMaxWater() && ff.getBurningBuildings().size() > 0;
			}
		};

		Predicate<Void> moveToRefill2refillGuard = new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return ff.getRefillTarget() == null || ff.getRefillTarget().equals(ff.getLocation());
			}
		};

		Predicate<Void> moveToFire2extinguishGuard = new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return (ff.getFireTarget() == null && ff.findCloseBurningBuilding() != null)
						|| ff.getFireTarget().equals(ff.getLocation());
			}
		};

		Predicate<Void> extinguish2moveToRefillGuard = new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return ff.getWater() == 0;
			}
		};

		Predicate<Void> extinguish2searchGuard = new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return ff.getWater() > 0 && ff.findCloseBurningBuilding() == null;
			}
		};
		// TODO: check close burning buildings are manageable
		Predicate<Void> search2extinguishGuard = new Predicate<Void>() {
			@Override
			public boolean test(Void v) {
				return ff.findCloseBurningBuilding() != null;
			}
		};

		// ACTIONS ############################################################

		Function<Void, Void> moveToRefillAction = new Function<Void, Void>() {
			@Override
			public Void apply(Void t) {
				ff.setRefillTarget(true);
				return null;
			}
		};

		Function<Void, Void> moveToFireAction = new Function<Void, Void>() {
			@Override
			public Void apply(Void t) {
				ff.setFireTarget(true);
				return null;
			}
		};

		Function<Void, Void> RefillReachedAction = new Function<Void, Void>() {
			@Override
			public Void apply(Void t) {
				ff.setRefillTarget(false);
				return null;
			}
		};

		Function<Void, Void> FireReachedAction = new Function<Void, Void>() {
			@Override
			public Void apply(Void t) {
				ff.setFireTarget(false);
				return null;
			}
		};

		// MODES ##############################################################

		Mode searchMode = new SearchMode();
		modes.add(searchMode);
		Mode moveToFireMode = new MoveToFireMode();
		modes.add(moveToFireMode);
		Mode moveToRefillMode = new MoveToRefillMode();
		modes.add(moveToRefillMode);
		Mode extinguishMode = new ExtinguishMode();
		modes.add(extinguishMode);
		Mode refillMode = new RefillMode();
		modes.add(refillMode);

		currentMode = searchMode;

		// TRANSITIONS ########################################################

		transitions.add(new TransitionImpl(searchMode, extinguishMode, search2extinguishGuard));
		transitions.add(new TransitionImpl(extinguishMode, searchMode, extinguish2searchGuard));
		transitions.add(
				new TransitionImpl(extinguishMode, moveToRefillMode, extinguish2moveToRefillGuard, moveToRefillAction));
		transitions
				.add(new TransitionImpl(moveToFireMode, extinguishMode, moveToFire2extinguishGuard, FireReachedAction));
		transitions
				.add(new TransitionImpl(moveToRefillMode, refillMode, moveToRefill2refillGuard, RefillReachedAction));
		transitions.add(new TransitionImpl(refillMode, moveToFireMode, refill2moveToFireGuard, moveToFireAction));
		transitions.add(new TransitionImpl(refillMode, searchMode, refill2searchGuard));

	}

	public Set<Mode> getModes() {
		return modes;
	}

	public Mode getCurrentMode() {
		return currentMode;
	}
	
	public void setCurrentMode(Class<? extends Mode> mode) {
		for(Mode m : modes) {
			if(m.getClass().equals(mode)) {
				currentMode = m;
				break;
			}
		}
	}

	public Set<Transition> getTransitions() {
		return transitions;
	}

	public Transition addTransition(Mode from, Mode to, Predicate<Void> guard) {
		TransitionImpl t = new TransitionImpl(from, to, guard);
		transitions.add(t);
		
		// Callback
		component.addTransition(t);
		
		return t;
	}

	public void removeTransition(Transition transition) {
		// Callback
		component.removeTransition((TransitionImpl) transition); 
		
		transitions.remove(transition);
	}

	public void decideModeSwitch() {
		// Switch mode only if there is a transition from it
		Set<Transition> out = getTransitionsFrom(currentMode);
		if (out.isEmpty()) {
			return;
		}

		// Sort transitions by priority
		List<Transition> sortedOut = sortByPriority(out);

		// Switch first satisfied transition
		for (Transition transition : sortedOut) {
			if (transition.getGuard().test(null)) {
				// Call the transition listeners before the mode is switched
				if(transition instanceof TransitionImpl) {
					TransitionImpl t = (TransitionImpl) transition;
					t.invokeAction();
				}

				currentMode = transition.getTo();
				break;
			}
		}
	}
	
	private Set<Transition> getTransitionsFrom(Mode mode){
		Set<Transition> outgoing = new HashSet<>();
		for(Transition transition : transitions){
			if(transition.getFrom().equals(mode)){
				outgoing.add(transition);
			}
		}
		
		return outgoing;
	}
	
	private List<Transition> sortByPriority(Set<Transition> transitions){
		List<Transition> sorted = new ArrayList<>(transitions);
		sorted.sort(Comparator.comparing(t -> -t.getPriority()));
		
		return sorted;
	}
}
