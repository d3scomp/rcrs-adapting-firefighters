package cz.cuni.mff.d3s.rcrs.af.comm;

public class GoToMsg extends Msg {

	private static final long serialVersionUID = 3478523097348464565L;

	public final String targetId;
	public final int destination;
	
	public GoToMsg(String targetId, int destination) {
		this.targetId = targetId;
		this.destination = destination;
	}
	
	@Override
	public String toString() {
		return String.format("%s go to %d", targetId, destination);
	}
}
