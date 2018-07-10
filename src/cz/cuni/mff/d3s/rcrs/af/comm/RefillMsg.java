package cz.cuni.mff.d3s.rcrs.af.comm;

import java.nio.ByteBuffer;

import rescuecore2.worldmodel.EntityID;

public class RefillMsg extends Msg {

	static {
		// Instance for decoding
		register(new RefillMsg());
	}

	public final int memberId;
	public final EntityID coordId;
		
	public RefillMsg(int memberId, EntityID coordId) {
		this.memberId = memberId;
		this.coordId = coordId;
	}
	
	private RefillMsg() {
		memberId = Integer.MIN_VALUE;
		coordId = null;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int memberId, int coordId,
		int size = Integer.BYTES * 2
		// space for signature
				 + 1;
		
		ByteBuffer data = ByteBuffer.allocate(size);
		
		data.putInt(memberId);
		data.putInt(coordId.getValue());
		entityBuffer.put(coordId.getValue(), coordId);

		return data;
	}

	@Override
	protected Msg fromMsgBytes(ByteBuffer data) {
		int memberId = data.getInt();
		EntityID coordId = entityBuffer.get(data.getInt());
		
		return new RefillMsg(memberId, coordId);
	}

	@Override
	protected byte getSignature() {
		return 0x6;
	}
	
	@Override
	public String toString() {
		
		return super.toString()
				+ " MemberId=" + memberId
				+ " CoordId=" + coordId.getValue();
	}

}
