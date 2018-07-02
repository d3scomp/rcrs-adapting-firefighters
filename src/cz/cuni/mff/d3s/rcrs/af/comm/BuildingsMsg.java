package cz.cuni.mff.d3s.rcrs.af.comm;

import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_BURNING_BUILDINGS;
import static cz.cuni.mff.d3s.rcrs.af.FireFighter.KNOWLEDGE_ID;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import rescuecore2.worldmodel.EntityID;

public class BuildingsMsg extends Msg {

	static {
		// Instance for decoding
		register(new BuildingsMsg());
	}
	
	public final int id;
	public final Set<EntityID> burningBuildings;
	
	
	public BuildingsMsg(int id, Set<EntityID> burningBuildings) {
		this.id = id;
		this.burningBuildings = burningBuildings;
	}
	
	private BuildingsMsg() {
		id = Integer.MIN_VALUE;
		burningBuildings = null;
	}
	
	@Override
	protected ByteBuffer getMsgBytes() {
		// int id,
		int size = Integer.BYTES
		// 1 byte burningBuildings.size,
				 + 1
		// List<Integer> burningBuildings,
				 + Integer.BYTES * burningBuildings.size()
		// space for signature
				 + 1;
		
		ByteBuffer data = ByteBuffer.allocate(size);
		
		data.putInt(id);
		data.put((byte) burningBuildings.size());
		for(EntityID burningBuilding : burningBuildings) {
			data.putInt(burningBuilding.getValue());
			entityBuffer.put(burningBuilding.getValue(), burningBuilding);
		}
		
		return data;
	}

	@Override
	protected Msg fromMsgBytes(ByteBuffer data) {
		int id = data.getInt();
		Set<EntityID> burningBuildings = new HashSet<>();
		for(int i = data.get(); i > 0; i--) {
			burningBuildings.add(entityBuffer.get(data.getInt()));
		}
		
		return new BuildingsMsg(id, burningBuildings);
	}

	@Override
	protected byte getSignature() {
		return 0x3;
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
				+ " " + KNOWLEDGE_BURNING_BUILDINGS + "=[" + bb + "]";
	}

}
