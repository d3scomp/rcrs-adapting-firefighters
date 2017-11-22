package cz.cuni.mff.d3s.rcrs.af.comm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import rescuecore2.log.Logger;

public abstract class Msg implements Serializable {

	private static final long serialVersionUID = 456222571529458162L;

	public byte[] getBytes() {
		byte[] data = null;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			try(ObjectOutputStream writer = new ObjectOutputStream(out)) {
				writer.writeObject(this);
				data = out.toByteArray();
			}
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}

		return data;
	}

	public static Msg fromBytes(byte[] data) {
		Msg msg = null;
		try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
			try (ObjectInputStream reader = new ObjectInputStream(in)) {
				msg = (Msg) reader.readObject();
			} 
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return msg;
	}

}
