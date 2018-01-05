package cz.cuni.mff.d3s.rcrs.af.comm;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import rescuecore2.worldmodel.EntityID;


import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILL_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_WATER;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_EXTINGUISHING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILLING;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_CAN_DETECT_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_FIREFIGHTER;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;

public class KnowledgeMsg extends Msg {

	static {
		// Instance for decoding
		register(new KnowledgeMsg());
	}
	
	public final int id;
	public final EntityID position;
	public final EntityID fireTarget;
	public final EntityID helpTarget;
	public final EntityID refillTarget;
	public final int water;
	public final boolean extinguishing;
	public final boolean refilling;
	public final List<EntityID> burningBuildings;
	public final boolean canDetectBuildings;
	public final int helpingFireFighter;
	public final int helpingDistance;
	
	
	public KnowledgeMsg(int id, EntityID position, EntityID fireTarget,
			EntityID helpTarget, EntityID refillTarget, int water,
			boolean extinguishing, boolean refilling, List<EntityID> burningBuildings,
			boolean canDetectBuildings, int helpingFireFighter, int helpingDistance) {
		this.id = id;
		this.position = position;
		this.fireTarget = fireTarget;
		this.helpTarget = helpTarget;
		this.refillTarget = refillTarget;
		this.water = water;
		this.extinguishing = extinguishing;
		this.refilling = refilling;
		this.burningBuildings = burningBuildings;
		this.canDetectBuildings = canDetectBuildings;
		this.helpingFireFighter = helpingFireFighter;
		this.helpingDistance = helpingDistance;
	}
	
	private KnowledgeMsg() {
		id = Integer.MIN_VALUE;
		position = null;
		fireTarget = null;
		helpTarget = null;
		refillTarget = null;
		water = Integer.MIN_VALUE;
		extinguishing = false;
		refilling = false;
		burningBuildings = null;
		canDetectBuildings = false;
		helpingFireFighter = -1;
		helpingDistance = Integer.MAX_VALUE;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int id, int position, int fireTarget, int helpTarget, int refilTarget, int water,
		// int helpingFireFighter, int helpingDistance,
		int size = Integer.BYTES * 8
		// boolean extinguishing, boolean refilling, boolean canDetectBuildings,
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
		if(fireTarget != null) {
			data.putInt(fireTarget.getValue());
			entityBuffer.put(fireTarget.getValue(), fireTarget);
		} else {
			data.putInt(-1);
		}
		if(helpTarget != null) {
			data.putInt(helpTarget.getValue());
			entityBuffer.put(helpTarget.getValue(), helpTarget);
		} else {
			data.putInt(-1);
		}
		if(refillTarget != null) {
			data.putInt(refillTarget.getValue());
			entityBuffer.put(refillTarget.getValue(), refillTarget);
		} else {
			data.putInt(-1);
		}
		data.putInt(water);
		data.put(extinguishing ? (byte) 1 : (byte) 0);
		data.put(refilling ? (byte) 1 : (byte) 0);
		data.put((byte) burningBuildings.size());
		for(EntityID burningBuilding : burningBuildings) {
			data.putInt(burningBuilding.getValue());
			entityBuffer.put(burningBuilding.getValue(), burningBuilding);
		}
		data.put(canDetectBuildings ? (byte) 1 : (byte) 0);
		data.putInt(helpingFireFighter);
		data.putInt(helpingDistance);
				
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
		EntityID fireTarget = null;
		if(targetInt != -1) {
			fireTarget = entityBuffer.get(targetInt);
		}
		targetInt = data.getInt();
		EntityID helpTarget = null;
		if(targetInt != -1) {
			helpTarget = entityBuffer.get(targetInt);
		}
		targetInt = data.getInt();
		EntityID refillTarget = null;
		if(targetInt != -1) {
			refillTarget = entityBuffer.get(targetInt);
		}
		int water = data.getInt();
		boolean extinguishing = data.get() == 0 ? false : true;
		boolean refilling = data.get() == 0 ? false : true;
		ArrayList<EntityID> burningBuildings = new ArrayList<>();
		for(int i = data.get(); i > 0; i--) {
			burningBuildings.add(entityBuffer.get(data.getInt()));
		}
		boolean canDetectBuildings = data.get() == 0 ? false : true;
		int helpingFireFighter = data.getInt();
		int helpingDistance = data.getInt();
		
		return new KnowledgeMsg(id, position, fireTarget, helpTarget, refillTarget,
				water, extinguishing, refilling, burningBuildings, canDetectBuildings,
				helpingFireFighter, helpingDistance);
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
				+ " " + KNOWLEDGE_FIRE_TARGET + "=" + (fireTarget != null ? fireTarget.getValue() : -1)
				+ " " + KNOWLEDGE_FIRE_TARGET + "=" + (helpTarget != null ? helpTarget.getValue() : -1)
				+ " " + KNOWLEDGE_REFILL_TARGET + "=" + (refillTarget != null ? refillTarget.getValue() : -1)
				+ " " + KNOWLEDGE_WATER + "=" + water
				+ " " + KNOWLEDGE_EXTINGUISHING + "=" + extinguishing
				+ " " + KNOWLEDGE_REFILLING + "=" + refilling
				+ " " + KNOWLEDGE_BURNING_BUILDINGS + "=[" + bb + "]"
				+ " " + KNOWLEDGE_CAN_DETECT_BUILDINGS + "=" + canDetectBuildings
				+ " " + KNOWLEDGE_HELPING_FIREFIGHTER + "=" + helpingFireFighter
				+ " " + KNOWLEDGE_HELPING_DISTANCE + "=" + helpingDistance;
	}

}
