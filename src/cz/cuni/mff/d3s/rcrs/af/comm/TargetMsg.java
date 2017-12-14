package cz.cuni.mff.d3s.rcrs.af.comm;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;

import java.nio.ByteBuffer;

import rescuecore2.worldmodel.EntityID;

public class TargetMsg extends Msg {

	static {
		// Instance for decoding
		register(new TargetMsg());
	}
	
	public final int id;
	public final EntityID target;
		
	public TargetMsg(int id, EntityID target) {
		this.id = id;
		this.target = target;
	}
	
	private TargetMsg() {
		id = Integer.MIN_VALUE;
		target = null;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int id, int target,
		int size = Integer.BYTES * 2
		// space for signature
				 + 1;
		
		ByteBuffer data = ByteBuffer.allocate(size);
		
		data.putInt(id);
		data.putInt(target.getValue());
		entityBuffer.put(target.getValue(), target);

		return data;
	}

	@Override
	protected Msg fromMsgBytes(ByteBuffer data) {
		int id = data.getInt();
		EntityID target = entityBuffer.get(data.getInt());
		
		return new TargetMsg(id, target);
	}

	@Override
	protected byte getSignature() {
		return 0x2;
	}
	
	@Override
	public String toString() {
		
		return super.toString()
				+ " " + KNOWLEDGE_ID + "=" + id
				+ " " + KNOWLEDGE_FIRE_TARGET + "=" + target.getValue();
	}

}
