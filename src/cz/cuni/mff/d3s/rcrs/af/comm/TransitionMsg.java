package cz.cuni.mff.d3s.rcrs.af.comm;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cz.cuni.mff.d3s.rcrs.af.modes.TransitionImpl;

public class TransitionMsg extends Msg {

	public enum Action {
		ADD, REMOVE;
	}
	
	protected static Map<Integer, TransitionImpl> transitionBuffer = new ConcurrentHashMap<>();
	
	static {
		// Instance for decoding
		register(new TransitionMsg());
	}
	
	public final int id;
	public final TransitionImpl transition;
	public final Action action;
		
	public TransitionMsg(int id, TransitionImpl transition, Action action) {
		this.id = id;
		this.transition = transition;
		this.action = action;
	}
	
	private TransitionMsg() {
		id = Integer.MIN_VALUE;
		transition = null;
		action = null;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int id, int transition, int action
		int size = Integer.BYTES * 3
		// space for signature
				 + 1;
		
		ByteBuffer data = ByteBuffer.allocate(size);
		
		data.putInt(id);
		data.putInt(transition.hashCode());
		transitionBuffer.put(transition.hashCode(), transition);
		data.putInt(action.ordinal());

		return data;
	}

	@Override
	protected Msg fromMsgBytes(ByteBuffer data) {
		int id = data.getInt();
		TransitionImpl transition = transitionBuffer.get(data.getInt());
		Action action = Action.values()[data.getInt()];
		
		return new TransitionMsg(id, transition, action);
	}

	@Override
	protected byte getSignature() {
		return 0x4;
	}
	
	@Override
	public String toString() {
		
		return super.toString()
				+ " " + KNOWLEDGE_ID + "=" + id
				+ " TRANSITION=" + transition
				+ " ACTION=" + action;
	}

}
