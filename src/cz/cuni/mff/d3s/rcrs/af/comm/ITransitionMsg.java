package cz.cuni.mff.d3s.rcrs.af.comm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cz.cuni.mff.d3s.rcrs.af.modes.TransitionImpl;

public abstract class ITransitionMsg extends Msg {
	protected static Map<Integer, TransitionImpl> transitionBuffer = new ConcurrentHashMap<>();
	
}
