package cz.cuni.mff.d3s.rcrs.af.modeswitch;

import cz.cuni.mff.d3s.metaadaptation.modeswitch.ComponentType;
import cz.cuni.mff.d3s.metaadaptation.modeswitch.ModeChart;
import cz.cuni.mff.d3s.rcrs.af.FFComponent;
import cz.cuni.mff.d3s.rcrs.af.modes.ModeChartImpl;

public class ComponentImpl implements cz.cuni.mff.d3s.metaadaptation.modeswitch.Component {

	private final FFComponent component;
	private final ModeChartImpl modeChart;
	
	public ComponentImpl(FFComponent component) {
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
	
	@Override
	public String toString() {
		return component.toString();
	}

}
