package cz.cuni.mff.d3s.rcrs.af;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rescuecore2.log.Logger;

public class Log {

	private final Set<Enum<?>> enabled;
	private final String sid;
	
	public Log(String sid, Enum<?>... enabled) {
		this.enabled = new HashSet<>();
		for(Enum<?> e : enabled) {
			this.enabled.add(e);
		}
		this.sid = sid;
	}
	
	public void d(int time, Enum<?> classification, String format, Object... args) {
		if(enabled.contains(classification)) {
			Logger.debug(formatLog(time, classification, format, args));
		}
	}
	
	public void i(int time, Enum<?> classification, String format, Object... args) {
		if(enabled.contains(classification)) {
			Logger.info(formatLog(time, classification, format, args));
		}
	}
	
	public void w(int time, Enum<?> classification, String format, Object... args) {
		if(enabled.contains(classification)) {
			Logger.warn(formatLog(time, classification, format, args));
		}
	}
	
	public void e(int time, Enum<?> classification, String format, Object... args) {
		if(enabled.contains(classification)) {
			Logger.error(formatLog(time, classification, format, args));
		}
	}
	
	private String formatLog(int time, Enum<?> classification, String format, Object... args) {
		List<Object> allArgs =  new ArrayList<>();
		allArgs.add(time);
		allArgs.add(classification);
		allArgs.add(sid);
		allArgs.addAll(Arrays.asList(args));
		
		return String.format("T[%d]{%s} %s " + format, allArgs.toArray());
	}
}
