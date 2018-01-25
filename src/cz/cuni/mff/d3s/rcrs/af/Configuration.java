package cz.cuni.mff.d3s.rcrs.af;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	
	public static String LOG_DIR = null;
	
	public static int PORT = Constants.DEFAULT_KERNEL_PORT_NUMBER;

	
	
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
