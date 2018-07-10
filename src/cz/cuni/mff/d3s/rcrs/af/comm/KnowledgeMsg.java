package cz.cuni.mff.d3s.rcrs.af.comm;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ENTITY_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_FIRE_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_DISTANCE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_HELPING_FIREFIGHTER;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_MODE;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_POSITION;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_REFILL_TARGET;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_WATER;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import cz.cuni.mff.d3s.rcrs.af.modes.Mode;
import rescuecore2.worldmodel.EntityID;

public class KnowledgeMsg extends Msg {

	static {
		// Instance for decoding
		register(new KnowledgeMsg());
	}
	
	public final int id;
	public final EntityID eid;
	public final EntityID position;
	public final EntityID fireTarget;
	public final EntityID helpTarget;
	public final EntityID refillTarget;
	public final int water;
	public final Mode mode;
	public final Set<EntityID> burningBuildings;
	public final int helpingFireFighter;
	public final int helpingDistance;
	
	
	public KnowledgeMsg(int id, EntityID eid, EntityID position, EntityID fireTarget,
			EntityID helpTarget, EntityID refillTarget, int water, Mode mode,
			Set<EntityID> burningBuildings, int helpingFireFighter, int helpingDistance) {
		this.id = id;
		this.eid = eid;
		this.position = position;
		this.fireTarget = fireTarget;
		this.helpTarget = helpTarget;
		this.refillTarget = refillTarget;
		this.water = water;
		this.mode = mode;
		this.burningBuildings = burningBuildings;
		this.helpingFireFighter = helpingFireFighter;
		this.helpingDistance = helpingDistance;
	}
	
	private KnowledgeMsg() {
		id = Integer.MIN_VALUE;
		eid = null;
		position = null;
		fireTarget = null;
		helpTarget = null;
		refillTarget = null;
		water = Integer.MIN_VALUE;
		mode = Mode.Search;
		burningBuildings = null;
		helpingFireFighter = -1;
		helpingDistance = Integer.MAX_VALUE;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int id, int eid, int position, int fireTarget, int helpTarget,
		// int refilTarget, int water, int helpingFireFighter, int helpingDistance,
		int size = Integer.BYTES * 9
		// byte mode, byte burningBuildings.size,
				 + 2
		// List<Integer> burningBuildings,
				 + Integer.BYTES * burningBuildings.size()
		// space for signature
				 + 1;
		
		ByteBuffer data = ByteBuffer.allocate(size);
		
		data.putInt(id);
		if(eid != null) {
			data.putInt(eid.getValue());
			entityBuffer.put(eid.getValue(), eid);
		} else {
			data.putInt(-1);
		}
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
		data.put(mode.getValue());
		data.put((byte) burningBuildings.size());
		for(EntityID burningBuilding : burningBuildings) {
			data.putInt(burningBuilding.getValue());
			entityBuffer.put(burningBuilding.getValue(), burningBuilding);
		}
		data.putInt(helpingFireFighter);
		data.putInt(helpingDistance);
				
		return data;
	}

	@Override
	protected Msg fromMsgBytes(ByteBuffer data) {
		int id = data.getInt();
		int eidInt = data.getInt();
		EntityID eid = null;
		if(eidInt != -1) {
			eid = entityBuffer.get(eidInt);
		}
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
		Mode mode = Mode.fromValue(data.get());
		Set<EntityID> burningBuildings = new HashSet<>();
		for(int i = data.get(); i > 0; i--) {
			burningBuildings.add(entityBuffer.get(data.getInt()));
		}
		int helpingFireFighter = data.getInt();
		int helpingDistance = data.getInt();
		
		return new KnowledgeMsg(id, eid, position, fireTarget, helpTarget, refillTarget,
				water, mode, burningBuildings, helpingFireFighter, helpingDistance);
	}

	@Override
	protected byte getSignature() {
		return 0x1;
	}
	
	public String getSid() {
		return String.format("FF%d", id);
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
				+ " " + KNOWLEDGE_ENTITY_ID + "=" + eid
				+ " " + KNOWLEDGE_POSITION + "=" + (position != null ? position.getValue() : -1)
				+ " " + KNOWLEDGE_FIRE_TARGET + "=" + (fireTarget != null ? fireTarget.getValue() : -1)
				+ " " + KNOWLEDGE_FIRE_TARGET + "=" + (helpTarget != null ? helpTarget.getValue() : -1)
				+ " " + KNOWLEDGE_REFILL_TARGET + "=" + (refillTarget != null ? refillTarget.getValue() : -1)
				+ " " + KNOWLEDGE_WATER + "=" + water
				+ " " + KNOWLEDGE_MODE + "=" + mode
				+ " " + KNOWLEDGE_BURNING_BUILDINGS + "=[" + bb + "]"
				+ " " + KNOWLEDGE_HELPING_FIREFIGHTER + "=" + helpingFireFighter
				+ " " + KNOWLEDGE_HELPING_DISTANCE + "=" + helpingDistance;
	}

}
