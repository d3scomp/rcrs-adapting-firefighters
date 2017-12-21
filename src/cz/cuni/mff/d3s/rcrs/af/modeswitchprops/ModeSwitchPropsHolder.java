package cz.cuni.mff.d3s.rcrs.af.modeswitchprops;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.H4_PROPERTIES;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.cuni.mff.d3s.metaadaptation.MetaAdaptationManager;
import cz.cuni.mff.d3s.metaadaptation.modeswitchprops.ModeSwitchPropsManager;
import cz.cuni.mff.d3s.metaadaptation.modeswitchprops.PropertyValue;
import rescuecore2.log.Logger;

public class ModeSwitchPropsHolder {

	private final ComponentManagerImpl componentManager;
	private final ModeSwitchPropsManager modeSwitchManager;
	private boolean registered;

	public ModeSwitchPropsHolder() {
		componentManager = new ComponentManagerImpl();
		modeSwitchManager = new ModeSwitchPropsManager(componentManager);
		registered = false;

		modeSwitchManager.setVerbosity(true);

		List<PropertyValue> properties = new ArrayList<>();
		final Pattern propertyPattern = Pattern.compile("(\\w+)-([-+]?[0-9]*\\.?[0-9]+)");
		final Matcher propertyMatcher = propertyPattern.matcher(H4_PROPERTIES);
		while (propertyMatcher.find()) {
			final String propertyName = propertyMatcher.group(1);
			final String propertyValue = propertyMatcher.group(2);
			PropertyValue property = new PropertyValue(propertyName, Double.parseDouble(propertyValue));
			properties.add(property);
			Logger.info("Training property: " + property);
		}

		if (properties.isEmpty()) {
			Logger.error(String.format("The training properties cannot be matched from: \"%s\"", H4_PROPERTIES));
		}

		modeSwitchManager.setTrainProperties(properties);
	}

	public void registerAt(MetaAdaptationManager manager) {
		manager.addAdaptation(modeSwitchManager);
		registered = true;
	}

	public boolean isRegistered() {
		return registered;
	}

	public ComponentManagerImpl getComponentManager() {
		return componentManager;
	}
}
