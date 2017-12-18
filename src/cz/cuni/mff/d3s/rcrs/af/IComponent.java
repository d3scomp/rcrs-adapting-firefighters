package cz.cuni.mff.d3s.rcrs.af;

import java.util.List;

import cz.cuni.mff.d3s.rcrs.af.modes.TransitionImpl;
import rescuecore2.worldmodel.EntityID;

public interface IComponent {
	
	public int getId();
	public int getWater();
	public int getMaxWater();
	public List<EntityID> getBurningBuildings();
	public EntityID getFireTarget();
	public EntityID getRefillTarget();
	public EntityID getLocation();
	public EntityID findCloseBurningBuilding();
	public void setFireTarget(boolean set);
	public void setRefillTarget(boolean set);
	
	// Callback for ModeChart
	public void addTransition(TransitionImpl transition);
	public void removeTransition(TransitionImpl transition);
}
