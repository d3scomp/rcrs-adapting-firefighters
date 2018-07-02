package cz.cuni.mff.d3s.rcrs.af;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rescuecore2.log.Logger;

public class Log {

	private final boolean enabled;
	private final String sid;
	
	public Log(String sid, boolean enabled) {
		this.enabled = enabled;
		this.sid = sid;
	}
	
	public void d(int time, String format, Object... args) {
		if(enabled) {
			Logger.debug(formatLog(time, format, args));
		}
	}
	
	public void i(int time, String format, Object... args) {
		if(enabled) {
			Logger.info(formatLog(time, format, args));
		}
	}
	
	public void w(int time, String format, Object... args) {
		if(enabled) {
			Logger.warn(formatLog(time, format, args));
		}
	}
	
	public void e(int time, String format, Object... args) {
		if(enabled) {
			Logger.error(formatLog(time, format, args));
		}
	}
	
	private String formatLog(int time, String format, Object... args) {
		List<Object> allArgs =  new ArrayList<>();
		allArgs.add(time);
		allArgs.add(sid);
		allArgs.addAll(Arrays.asList(args));
		
		return String.format("T[%d] %s " + format, allArgs.toArray());
	}
}
