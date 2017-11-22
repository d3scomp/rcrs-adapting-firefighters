package cz.cuni.mff.d3s.rcrs.af;

import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.connection.ConnectionException;
import rescuecore2.registry.Registry;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;

import java.io.IOException;

import rescuecore2.Constants;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;

public class Run {

	public static void main(String[] args) {
		try {

			Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);

			Config config = new Config();

			args = CommandLineOptions.processArgs(args, config);
			int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Constants.DEFAULT_KERNEL_PORT_NUMBER);
			String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY, Constants.DEFAULT_KERNEL_HOST_NAME);

			ComponentLauncher launcher = new TCPComponentLauncher(host, port, config);
			Logger.info("Connecting agents");
			connect(launcher, config);
		} catch (IOException e) {
			Logger.error("Error connecting agents", e);
		} catch (ConfigException e) {
			Logger.error("Configuration error", e);
		} catch (ConnectionException e) {
			Logger.error("Error connecting agents", e);
		} catch (InterruptedException e) {
			Logger.error("Error connecting agents", e);
		}
	}

	private static void connect(ComponentLauncher launcher, Config config)
			throws InterruptedException, ConnectionException {
		int unit_number = 0;
		try {
			while (true) {
				unit_number++;
				Logger.info("Connecting fire brigade " + (unit_number) + "...");
				launcher.connect(new FireFighter(unit_number));
				Logger.info("success");
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
		
		unit_number = 0;
		try {
			while (true) {
				unit_number++;
				Logger.info("Connecting fire station " + (unit_number) + "...");
				launcher.connect(new FireStation(unit_number));
				Logger.info("success");
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
	}
}
