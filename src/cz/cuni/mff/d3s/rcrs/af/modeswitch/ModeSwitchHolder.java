package cz.cuni.mff.d3s.rcrs.af.modeswitch;


import static cz.cuni.mff.d3s.rcrs.af.Configuration.H3_TRANSITIONS;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H3_TRANSITION_PRIORITY;
import static cz.cuni.mff.d3s.rcrs.af.Configuration.H3_TRANSITION_PROBABILITY;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.cuni.mff.d3s.metaadaptation.MetaAdaptationManager;
import cz.cuni.mff.d3s.metaadaptation.modeswitch.NonDeterministicModeSwitchingManager;
import cz.cuni.mff.d3s.metaadaptation.modeswitch.Transition;
import cz.cuni.mff.d3s.rcrs.af.IComponent;
import cz.cuni.mff.d3s.rcrs.af.modes.ModeChartImpl;
import cz.cuni.mff.d3s.rcrs.af.modes.ModeImpl;
import cz.cuni.mff.d3s.rcrs.af.modes.TransitionImpl;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

public class ModeSwitchHolder {
	private final ComponentManagerImpl componentManager;
	private final NonDeterministicModeSwitchingManager modeSwitchManager;
	private final ModeChartImpl modeChart;
	private boolean registered;
	
	public ModeSwitchHolder() {
		componentManager = new ComponentManagerImpl();
		modeSwitchManager = new NonDeterministicModeSwitchingManager(componentManager, null);
		modeChart = prepareModeChart();
		registered = false;

		modeSwitchManager.setVerbosity(true);
		modeSwitchManager.setTransitionProbability(H3_TRANSITION_PROBABILITY);
		modeSwitchManager.setTransitionPriority(H3_TRANSITION_PRIORITY);
		
		List<Transition> transitions = new ArrayList<>();
		final Pattern transitionPattern = Pattern.compile("(\\w+)-(\\w+)");
		final Matcher transitionMatcher = transitionPattern.matcher(H3_TRANSITIONS);
		while(transitionMatcher.find()){	
			final String fromName = transitionMatcher.group(1);
			final String toName = transitionMatcher.group(2);
			ModeImpl from = getMode(fromName);
			ModeImpl to = getMode(toName);
			TransitionImpl transition = new TransitionImpl(from, to, null, null);
			transition.setPriority(H3_TRANSITION_PRIORITY);
			transitions.add(transition);
			Logger.info("Training transition: " + transition);
		}
		if(transitions.isEmpty()){
			Logger.error(String.format("The training transitions cannot be matched from: \"%s\"", H3_TRANSITIONS));
		}
		modeSwitchManager.setTrainTransitions(transitions);
	}
	
	private ModeChartImpl prepareModeChart() {
		return new ModeChartImpl(new IComponent() {
			@Override
			public int getId() { return 0; }

			@Override
			public int getWater() { return 0; }

			@Override
			public int getMaxWater() { return 0; }

			@Override
			public List<EntityID> getBurningBuildings() { return null; }

			@Override
			public EntityID getFireTarget() { return null; }

			@Override
			public EntityID getRefillTarget() { return null; }

			@Override
			public EntityID getLocation() { return null; }

			@Override
			public EntityID findCloseBurningBuilding() { return null; }

			@Override
			public void setFireTarget(boolean set) {}

			@Override
			public void setRefillTarget(boolean set) {}

			@Override
			public void addTransitionCallback(TransitionImpl transition) {}

			@Override
			public void removeTransitionCallback(TransitionImpl transition) {}

			@Override
			public void setGuardParamCallback(TransitionImpl transition, String name, double value) {}
		});
	}

	private ModeImpl getMode(String modeName){
		for(ModeImpl mode : modeChart.getModes()){
			if(((ModeImpl) mode).getName().equals(modeName)){
				return mode;
			}
		}
		
		throw new IllegalArgumentException(String.format(
				"The \"%s\" mode for training transition was not recognized.",
				modeName));
	}
	
	public void registerAt(MetaAdaptationManager manager) {
		manager.addAdaptation(modeSwitchManager);
		registered = true;
	}
	
	public boolean isRegistered() {
		return registered;
	}
	
	public ComponentManagerImpl getComponentManager() {
		return componentManager;
	}
}
