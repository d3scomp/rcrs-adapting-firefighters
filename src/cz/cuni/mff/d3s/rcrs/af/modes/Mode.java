package cz.cuni.mff.d3s.rcrs.af.modes;

public enum Mode {
	Search((byte) 0),
	MoveToFire((byte) 1),
	MoveToRefill((byte) 2),
	Extinguish((byte) 3),
	Refill((byte) 4);
	
	private byte value;
	
	private Mode(byte value) {
		this.value = value;
	}
	
	public byte getValue() {
		return value;
	}
	
	public static Mode fromValue(byte value) {
		for(Mode m : Mode.values()) {
			if(value == m.value) {
				return m;
			}
		}
		
		throw new IllegalArgumentException(String.format(
				"The value \"%s\" is not valid for %s",
				value, Mode.class));
	}
}
