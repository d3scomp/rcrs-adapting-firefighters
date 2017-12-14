package cz.cuni.mff.d3s.rcrs.af.comm;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

public abstract class Msg {

	private static final byte INVALID_SIGNATURE = 0;
	
	private static Map<Byte, Msg> messengers = new HashMap<>();
	
	protected static Map<Integer, EntityID> entityBuffer = new ConcurrentHashMap<>();
	
	final public byte[] getBytes() {
		ByteBuffer buffer = getMsgBytes();
		buffer.put(getSignature());
		return buffer.array();
	}

	final public static Msg fromBytes(byte[] data) {
		byte receivedSignature = data[data.length - 1];
		if(messengers.containsKey(receivedSignature)) {
			ByteBuffer buffer = ByteBuffer.wrap(data, 0, data.length - 1);
			return messengers.get(receivedSignature).fromMsgBytes(buffer);
		} else {
			Logger.error(String.format("The message type with signature \"%d\" is unknown", receivedSignature));
			return null;
		}
	}
	
	final static synchronized protected void register(Msg msg) {
		byte signature = msg.getSignature();
		if(signature == INVALID_SIGNATURE) {
			throw new IllegalArgumentException(String.format("The \"%s\" arguments does not contain allowed value.", "signature"));
		}
		if(messengers.containsKey(signature)) {
			throw new IllegalArgumentException(String.format("A message with \"%s\" = %d already registered.", "signature", signature));
		}
		
		messengers.put(signature, msg);
	}
	
	abstract protected byte getSignature();
	
	abstract protected ByteBuffer getMsgBytes();
	
	abstract protected Msg fromMsgBytes(ByteBuffer data);
	
	@Override
	public String toString() {
		return "Msg [" + getSignature() + "]";
	}

}
