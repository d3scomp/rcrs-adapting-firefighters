package cz.cuni.mff.d3s.rcrs.af.comm;

public class FireMsg extends Msg {

	private static final long serialVersionUID = 5931333847296588324L;
	
	public final int fireLocation;
	
	public FireMsg(int fireLocation) {
		this.fireLocation = fireLocation;
	}
	
	@Override
	public String toString() {
		return String.format("Fire at %d", fireLocation);
	}
}
