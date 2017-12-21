package cz.cuni.mff.d3s.rcrs.af.modeswitchprops;

import cz.cuni.mff.d3s.metaadaptation.modeswitchprops.ComponentType;
import cz.cuni.mff.d3s.metaadaptation.modeswitchprops.ModeChart;
import cz.cuni.mff.d3s.rcrs.af.Component;
import cz.cuni.mff.d3s.rcrs.af.modes.ModeChartImpl;

public class ComponentImpl implements cz.cuni.mff.d3s.metaadaptation.modeswitchprops.Component  {

	private final Component component;
	private final ModeChartImpl modeChart;
	
	public ComponentImpl(Component component) {
		this.component = component;
		
		modeChart = new ModeChartImpl(component);
	}

	@Override
	public ComponentType getType() {
		return ComponentTypeImpl.getInstance();
	}

	@Override
	public double getUtility() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ModeChart getModeChart() {
		return modeChart;
	}

}
