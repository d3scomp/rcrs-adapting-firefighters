package cz.cuni.mff.d3s.rcrs.af.modes;

public class ModeImpl implements cz.cuni.mff.d3s.metaadaptation.modeswitch.Mode,
								 cz.cuni.mff.d3s.metaadaptation.modeswitchprops.Mode {
	
	private final String name;
	
	protected ModeImpl(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ModeImpl) {
			ModeImpl other = (ModeImpl) obj;
			return this.name.equals(other.name);
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
