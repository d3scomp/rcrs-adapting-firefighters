package cz.cuni.mff.d3s.rcrs.af.comm;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.cuni.mff.d3s.rcrs.af.modes.TransitionImpl;

public class PropertyMsg extends ITransitionMsg {
	
	protected static List<String> propertyBuffer = Collections.synchronizedList(new ArrayList<String>());
	
	static {
		// Instance for decoding
		register(new PropertyMsg());
	}
	
	public final int id;
	public final TransitionImpl transition;
	public final String property;
	public final double value;
		
	public PropertyMsg(int id, TransitionImpl transition, String property, double value) {
		this.id = id;
		this.transition = transition;
		this.property = property;
		this.value = value;
	}
	
	private PropertyMsg() {
		id = Integer.MIN_VALUE;
		transition = null;
		property = null;
		value = Double.NaN;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int id, int transition, int property,
		int size = Integer.BYTES * 3
		// double value,
				+ Double.BYTES * 1
		// space for signature
				 + 1;
		
		ByteBuffer data = ByteBuffer.allocate(size);
		
		data.putInt(id);
		data.putInt(transition.hashCode());
		transitionBuffer.put(transition.hashCode(), transition);
		if(!propertyBuffer.contains(property)) {
			propertyBuffer.add(property);
		}
		int index = propertyBuffer.indexOf(property);
		data.putInt(index);
		data.putDouble(value);

		return data;
	}

	@Override
	protected Msg fromMsgBytes(ByteBuffer data) {
		int id = data.getInt();
		TransitionImpl transition = transitionBuffer.get(data.getInt());
		String property = propertyBuffer.get(data.getInt());
		double value = data.getDouble();
		
		return new PropertyMsg(id, transition, property, value);
	}

	@Override
	protected byte getSignature() {
		return 0x5;
	}
	
	@Override
	public String toString() {
		
		return super.toString()
				+ " " + KNOWLEDGE_ID + "=" + id
				+ " TRANSITION=" + transition
				+ " PROPERTY=" + property
				+ " VALUE=" + value;
	}

}
