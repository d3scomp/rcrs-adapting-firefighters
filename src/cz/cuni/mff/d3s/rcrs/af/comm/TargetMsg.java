package cz.cuni.mff.d3s.rcrs.af.comm;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;

import java.nio.ByteBuffer;

import rescuecore2.worldmodel.EntityID;

public class TargetMsg extends Msg {

	static {
		// Instance for decoding
		register(new TargetMsg());
	}

	public final int memberId;
	public final int coordId;
	public final EntityID coordTarget;
	public final int helpingDistance;
		
	public TargetMsg(int memberId, int coordId, EntityID coordTarget, int helpingDistance) {
		this.memberId = memberId;
		this.coordId = coordId;
		this.coordTarget = coordTarget;
		this.helpingDistance = helpingDistance;
	}
	
	private TargetMsg() {
		memberId = Integer.MIN_VALUE;
		coordId = Integer.MIN_VALUE;
		coordTarget = null;
		helpingDistance = Integer.MAX_VALUE;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int memberId, int coordId, int memberTarget, int helpingDistance,
		int size = Integer.BYTES * 4
		// space for signature
				 + 1;
		
		ByteBuffer data = ByteBuffer.allocate(size);
		
		data.putInt(memberId);
		data.putInt(coordId);
		data.putInt(coordTarget.getValue());
		entityBuffer.put(coordTarget.getValue(), coordTarget);
		data.putInt(helpingDistance);

		return data;
	}

	@Override
	protected Msg fromMsgBytes(ByteBuffer data) {
		int memberId = data.getInt();
		int coordId = data.getInt();
		EntityID coordTarget = entityBuffer.get(data.getInt());
		int helpingDistance = data.getInt();
		
		return new TargetMsg(memberId, coordId, coordTarget, helpingDistance);
	}

	@Override
	protected byte getSignature() {
		return 0x2;
	}
	
	@Override
	public String toString() {
		
		return super.toString()
				+ " MemberId=" + memberId
				+ " CoordId=" + memberId
				+ " " + KNOWLEDGE_FIRE_TARGET + "=" + coordTarget.getValue()
				+ " " + KNOWLEDGE_HELPING_DISTANCE + "=" + helpingDistance;
	}

}
