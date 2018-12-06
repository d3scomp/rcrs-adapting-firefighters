package cz.cuni.mff.d3s.rcrs.af;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.cuni.mff.d3s.tss.TTable;
import rescuecore2.Constants;
import rescuecore2.log.Logger;

/**
 * <p>
 * This class holds the overall configuration of the simulation.
 * </p>
 * 
 * @author Dominik Skoda <skoda@d3s.mff.cuni.cz>
 *
 */
public class Configuration {

	/////////////////////////////////////////////////////////////////////////
	// SIMULATION CONFIGURATION
	///////////////////////////////////////////////////////////////////////////

	public enum TimeSeriesMode {
		On, Off;
	}
	
	public static String LOG_DIR = null;
	
	public static int PORT = Constants.DEFAULT_KERNEL_PORT_NUMBER;

	
	public static int TS_WINDOW_CNT = 2;
	public static int TS_WINDOW_SIZE = 4;
	public static TTable.ALPHAS TS_ALPHA = TTable.ALPHAS.ALPHA_0_05;
		
	
	public static TimeSeriesMode TIME_SERIES_MODE = TimeSeriesMode.Off;

	public static int MAX_SEPARATION_DISTANCE = 70_000;
	
	public static int WATER_THRESHOLD = 0;
	
	public static double WATER_NOISE_VARIANCE = 0.1; // 10% of the tank capacity
	
	public static double FIRE_MAX_DISTANCE_DETECTABILITY = 0.2; // 20% chance to detect fire at the maximum detectable distance
						 
	public static int FIRE_MAX_DETECTABLE_DISTANCE = 40000;
	
	public static int FIRE_UNERRING_DETECTABLE_DISTANCE = 8000;
		
	public static double FIRE_NOISE_VARIANCE = 0.1; // 10% of fire detectability
	
	public static double FIRE_PROBABILITY_THRESHOLD = 0.6;
	
	public static double FALSE_POSITIV_FIRE_PROBABILITY = 0.1;
	
	public static int FIRE_MAX_HELP_DISTANCE = 200000;
	
	public static double WIND_DEFINED_TARGET_PROBABILITY = 0.2;
	
	public static int WIND_DEFINED_TARGET_DISTANCE = 100000;
	
	public static int AVG_RESIDENTIAL_PEOPLE = 4;
	
	public static int VAR_RESIDENTIAL_PEOPLE = 2;
	
	public static int AVG_COMMERCIAL_PEOPLE_PER_FLOOR = 50;
	
	public static int VAR_COMMERTIAL_PEOPLE_PER_FLOOR = 20;
	
	public static int PEOPLE_ARIMA_ORDER_P = 1;
	
	public static int PEOPLE_ARIMA_ORDER_D = 0;
	
	public static int PEOPLE_ARIMA_ORDER_Q = 1;
	
	public static int SIMULATION_HOUR = -1;
		
	
	///////////////////////////////////////////////////////////////////////////

	public static void override(String[] params) {
		final Pattern nameValuePattern = Pattern.compile("(\\w+)=(.+)");

		for (String param : params) {
			final Matcher nameValueMatcher = nameValuePattern.matcher(param);
			if ((!nameValueMatcher.matches()) || nameValueMatcher.groupCount() != 2) {
				Logger.error(String.format("The \"%s\" parameter is not valid.", param));
				continue;
			}

			final String name = nameValueMatcher.group(1);
			final String value = nameValueMatcher.group(2);
			try {
				Field field = Configuration.class.getField(name);
				setValue(field, value);
			} catch (NoSuchFieldException e) {
				Logger.error(String.format("The configuration field \"%s\" cannot be found", name));
				continue;
			}

		}
	}

	private static void setValue(Field field, String value) {
		Logger.info(String.format("Overriding: %s = %s", field.getName(), value));

		Class<?> type = field.getType();
		if (type == long.class || type == Long.class) {
			try {
				long v = Long.parseLong(value);
				field.set(null, v);
			} catch (Exception e) {
				Logger.error(e.getMessage());
			}
		} else if (type == int.class || type == Integer.class) {
			try {
				int v = Integer.parseInt(value);
				field.set(null, v);
			} catch (Exception e) {
				Logger.error(e.getMessage());
			}
		} else if (type == boolean.class || type == Boolean.class) {
			try {
				boolean v = Boolean.parseBoolean(value);
				field.set(null, v);
			} catch (Exception e) {
				Logger.error(e.getMessage());
			}
		} else if (type == double.class || type == Double.class) {
			try {
				double v = Double.parseDouble(value);
				field.set(null, v);
			} catch (Exception e) {
				Logger.error(e.getMessage());
			}
		} else if (type == TimeSeriesMode.class) {
			try {
				field.set(null, TimeSeriesMode.valueOf(value));
			} catch(Exception e) {
				Logger.error(e.getMessage());
			}
		} else if (type == String.class) {
			try {
				field.set(null, value);
			} catch (Exception e) {
				Logger.error(e.getMessage());
			}
		} else {
			Logger.error(String.format("Unknown type to assign: %s", type.toString()));
		}
	}
}
