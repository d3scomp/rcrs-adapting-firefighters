package cz.cuni.mff.d3s.rcrs.af;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.SIMULATION_HOUR;
import static cz.cuni.mff.d3s.rcrs.af.buildings.OccupancyData.DAY_LENGTH;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.PropertyConfigurator;

import cz.cuni.mff.d3s.rcrs.af.buildings.BuildingRegistry;
import rescuecore2.Constants;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.connection.ConnectionException;
import rescuecore2.log.Logger;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;

public class Run {
	
	
	public static void main(String[] args) {
		
		// Process arguments
		Configuration.override(args);
		
		if(Configuration.LOG_DIR != null) {
			// Change target log file
		    try(InputStream configStream = Run.class.getResourceAsStream( "/aflog4j.properties")) {
				Properties props = new Properties();
		        props.load(configStream);
		        props.setProperty("log4j.appender.FILE.File", Configuration.LOG_DIR); 
			    PropertyConfigurator.configure(props);
		    } catch (IOException e) { 
		        System.err.println("Error: Cannot load configuration file "); 
		    }
		}
		
		try {

			Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);

			Config config = new Config();
			config.setIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Configuration.PORT);

			// Don't pass arguments defining the connection
			// args = CommandLineOptions.processArgs(args, config);
			int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Constants.DEFAULT_KERNEL_PORT_NUMBER);
			String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY, Constants.DEFAULT_KERNEL_HOST_NAME);

			ComponentLauncher launcher = new TCPComponentLauncher(host, port, config);
			Logger.info("Connecting agents");
			connect(launcher, config);
		} catch (ConnectionException e) {
			Logger.error("Error connecting agents", e);
		} catch (InterruptedException e) {
			Logger.error("Error connecting agents", e);
		}
	}

	private static void connect(ComponentLauncher launcher, Config config)
			throws InterruptedException, ConnectionException {
		int hour = SIMULATION_HOUR;
		if(hour == -1) {
			hour = (new Random()).nextInt(DAY_LENGTH);
		}
		BuildingRegistry buildingRegistry = new BuildingRegistry(hour);

		int unit_number = 0;
		try {
			while (true) {
				unit_number++;
				Logger.info("Connecting fire brigade " + (unit_number) + "...");
				launcher.connect(new FireFighter(unit_number, buildingRegistry));
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
				launcher.connect(new FireStation(unit_number, buildingRegistry));
				Logger.info("success");
			}
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
		}
	}
}
