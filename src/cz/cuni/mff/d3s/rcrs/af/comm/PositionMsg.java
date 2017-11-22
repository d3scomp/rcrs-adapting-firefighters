package cz.cuni.mff.d3s.rcrs.af.comm;

import cz.cuni.mff.d3s.rcrs.af.modes.Mode;

public class PositionMsg extends Msg {

	private static final long serialVersionUID = -2232560517561159603L;
	
	public final String agentId;
	public final int position;
	public final Mode mode;
	
	public PositionMsg(String agentId, int position, Mode mode) {
		this.agentId = agentId;
		this.position = position;
		this.mode = mode;
	}
	
	@Override
	public String toString() {
		return String.format("%s is at %d in mode %s", agentId, position, mode);
	}
}
