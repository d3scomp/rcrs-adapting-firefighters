package cz.cuni.mff.d3s.rcrs.af.modeswitchprops;

public class ComponentTypeImpl implements cz.cuni.mff.d3s.metaadaptation.modeswitchprops.ComponentType {

	private static ComponentTypeImpl INSTANCE = new ComponentTypeImpl();
	
	private ComponentTypeImpl() {}
	
	public static ComponentTypeImpl getInstance() {
		return INSTANCE;
	}
	
	@Override
	public double getAverageUtility() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getUtilityThreshold() {
		// TODO Auto-generated method stub
		return 0;
	}

}
