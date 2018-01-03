package cz.cuni.mff.d3s.rcrs.af;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rescuecore2.Constants;
import rescuecore2.log.Logger;
import rescuecore2.worldmodel.EntityID;

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

	
	public static String LOG_DIR = null;
	
	public static int PORT = Constants.DEFAULT_KERNEL_PORT_NUMBER;

	public static boolean WITH_SEED = false;
	public static int SEED = 0;

	public static int MAX_SEPARATION_DISTANCE = 70_000;
	public static double HYDRANT_REFILL_PROBABILITY = 0.5;
	
	// COLLABORATIVE SENSING

	public static boolean H1_INTRODUCE_FAILURE = false;
	public static long H1_FAILURE_TIME = 50;
	public static String H1_FAILURE_IDS = "FF1 FF2";
	public static boolean H1_MECHANISM = false;

	// FAULTY COMPONENT ISOLATION

	public static boolean H2_INTRODUCE_FAILURE = false;
	public static long H2_FAILURE_TIME = 50;
	public static double H2_FAILURE_IMPACT = 0.1;
	public static boolean H2_MECHANISM = false;
	
	public static Set<EntityID> failedRefillStations = new HashSet<>();

	// ENHANCING MODE SWITCHING

	public static boolean H3_MECHANISM = false;
	public static String H3_TRANSITIONS = null;
	public static double H3_TRANSITION_PROBABILITY = 0.01;
	public static int H3_TRANSITION_PRIORITY = 10;

	// MODE SWITCHING PROPERTIES

	public static boolean H4_MECHANISM = false;
	public static String H4_PROPERTIES = null;

//	public static String UTILITY_DIRECTORY = "results\\Logs\\06)-!DDF-!DF-UMS-6\\UMS_Loggers";

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
