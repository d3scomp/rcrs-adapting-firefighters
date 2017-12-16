package cz.cuni.mff.d3s.rcrs.af.comm;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import rescuecore2.worldmodel.EntityID;


import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_WATER;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_EXTINGUISHING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_MOVE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_DETECT_BUILDINGS;

public class KnowledgeMsg extends Msg {

	static {
		// Instance for decoding
		register(new KnowledgeMsg());
	}
	
	public final int id;
	public final EntityID position;
	public final EntityID target;
	public final int water;
	public final boolean extinguishing;
	public final boolean canMove;
	public final List<EntityID> burningBuildings;
	public final boolean canDetectBuildings;
	
	
	public KnowledgeMsg(int id, EntityID position, EntityID target, int water,
			boolean extinguishing, boolean canMove, List<EntityID> burningBuildings, boolean canDetectBuildings) {
		this.id = id;
		this.position = position;
		this.target = target;
		this.water = water;
		this.extinguishing = extinguishing;
		this.canMove = canMove;
		this.burningBuildings = burningBuildings;
		this.canDetectBuildings = canDetectBuildings;
	}
	
	private KnowledgeMsg() {
		id = Integer.MIN_VALUE;
		position = null;
		target = null;
		water = Integer.MIN_VALUE;
		extinguishing = false;
		canMove = false;
		burningBuildings = null;
		canDetectBuildings = false;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int id, int position, int target, int water,
		int size = Integer.BYTES * 4
		// boolean extinguishing, boolean canMove, boolean canDetectBuildings,
				 + 3
		// 1 byte burningBuildings.size,
				 + 1
		// List<Integer> burningBuildings,
				 + Integer.BYTES * burningBuildings.size()
		// space for signature
				 + 1;
		
		ByteBuffer data = ByteBuffer.allocate(size);
		
		data.putInt(id);
		if(position != null) {
			data.putInt(position.getValue());
			entityBuffer.put(position.getValue(), position);
		} else {
			data.putInt(-1);
		}
		if(target != null) {
			data.putInt(target.getValue());
			entityBuffer.put(target.getValue(), target);
		} else {
			data.putInt(-1);
		}
		data.putInt(water);
		data.put(extinguishing ? (byte) 1 : (byte) 0);
		data.put(canMove ? (byte) 1 : (byte) 0);
		data.put((byte) burningBuildings.size());
		for(EntityID burningBuilding : burningBuildings) {
			data.putInt(burningBuilding.getValue());
			entityBuffer.put(burningBuilding.getValue(), burningBuilding);
		}
		data.put(canDetectBuildings ? (byte) 1 : (byte) 0);
		
		return data;
	}

	@Override
	protected Msg fromMsgBytes(ByteBuffer data) {
		int id = data.getInt();
		int positionInt = data.getInt();
		EntityID position = null;
		if(positionInt != -1) {
			position = entityBuffer.get(positionInt);
		}
		int targetInt = data.getInt();
		EntityID target = null;
		if(targetInt != -1) {
			target = entityBuffer.get(targetInt);
		}
		int water = data.getInt();
		boolean extinguishing = data.get() == 0 ? false : true;
		boolean canMove = data.get() == 0 ? false : true;
		ArrayList<EntityID> burningBuildings = new ArrayList<>();
		for(int i = data.get(); i > 0; i--) {
			burningBuildings.add(entityBuffer.get(data.getInt()));
		}
		boolean canDetectBuildings = data.get() == 0 ? false : true;
		
		return new KnowledgeMsg(id, position, target, water, extinguishing, canMove, burningBuildings, canDetectBuildings);
	}

	@Override
	protected byte getSignature() {
		return 0x1;
	}
	
	@Override
	public String toString() {

		StringBuilder bb = new StringBuilder();
		for(EntityID building : burningBuildings) {
			if(bb.length() != 0) {
				bb.append(", ");
			}
			bb.append(building.getValue());
		}
		
		return super.toString()
				+ " " + KNOWLEDGE_ID + "=" + id
				+ " " + KNOWLEDGE_POSITION + "=" + (position != null ? position.getValue() : -1)
				+ " " + KNOWLEDGE_FIRE_TARGET + "=" + (target != null ? target.getValue() : -1)
				+ " " + KNOWLEDGE_WATER + "=" + water
				+ " " + KNOWLEDGE_EXTINGUISHING + "=" + extinguishing
				+ " " + KNOWLEDGE_CAN_MOVE + "=" + canMove
				+ " " + KNOWLEDGE_BURNING_BUILDINGS + "=[" + bb + "]"
				+ " " + KNOWLEDGE_CAN_DETECT_BUILDINGS + "=" + canDetectBuildings;
	}

}