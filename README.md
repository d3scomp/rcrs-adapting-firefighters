# rcrs-adapting-firefighters
Fire fighter agents with adaptation abilities for RoboCup Rescue Simulation employing meta-adaptation mechanisms from [meta-adaptation-manager](https://github.com/d3scomp/meta-adaptation-manager) project.

## Batch invocation and analysis of results
This project contains runnable JAR file, although the [rcrs-server](https://github.com/d3scomp/rcrs-server/blob/adapting-firefighters/README.md) is still needed as a standalone project. Clone this project (branch master) and rcrs-server (branch adapting-firefighters) into the same parent folder. Build the rcrs-server by running `ant` within the root folder of the project. Than everything is set to go. Run the simulation by calling the `rcrs-adapting-firefighters/scripts/Simulate.py`. Threr will be deployed number of simulation iteration configured in the `rcrs-adapting-firefighters/scripts/Configuration.py`.

To ease the process of launching simulations with different settings and analyzing their results we have devised a set of Python scripts (version 3.5). They are placed in the `scripts` folder of the project. The `Configuration.py` script contain the overall config for the simulation run, such as number of processor cores to use etc. The `Scenarios.py` script contains the definition of available scenarios. You can see the list by running the script without parameters. The `Simulate.py` script serves as a starting point. Run the script with the number of the selected scenario to simulate it. Once all simulation runs are finished, the final results are boxplots depicting the "scores" (computed by the function `rescuecore2.standard.score.BuildingDamageScoreFunction`) at each run. Create the plot by running `Plot.py` script with the scenario numbers passed as arguments.

## Running Manually
### Requirements
Frst you need to checkout the following Github projects:

- [rcrs-adapting-firefighters](https://github.com/d3scomp/rcrs-adapting-firefighters) (the project featured here)
- [rcrs-server](https://github.com/d3scomp/rcrs-server/tree/adapting-firefighters), and switch to the "adapting-firefighters" branch
- [meta-adaptation-manager](https://github.com/d3scomp/meta-adaptation-manager)

Import the clonned Eclipse projects to a running Eclipse instance (tested with Oxygen.2):
Run maven install on `meta-adaptation-manager`, maven update on `rcrs-adapting-firefighters` (you need to install the m2e plugin to use maven within Eclipse). Make sure the project `rcrs-adapting-firefighters` is built properly (click `Project->Clean`, it will be built automatically). Eclipse is needed here, because `rcrs-server` is not a maven project. Libraries from `rcrs-server` are coppied and used in `rcrs-adapting-firefighters/lib`. In particular these are the required libraries:

- clear.jar
- collapse.jar
- gis2.jar
- handy.jar
- human.jar
- ignition.jar
- jsi-1.0b2p1.jar
- kernel.jar
- log4j-1.2.15.jar
- maps.jar
- misc.jar
- rescuecore2.jar
- resq-fire.jar
- standard.jar
- traffic3.jar
- trove-0.1.8.jar
- uncommons-maths-1.2.jar

### Running the demo
Locate the files `Configuration.java` and `Run.java` inside the `cz.cuni.mff.d3s.rcrs.af` package of the `rcrs-adapting-firefighters` Eclipse project. `Configuration.java` contains the parameters of the simulation and `Run.java` contains the `main()` of the demo.

0. To enable visualizer edit the `rcrs-server/boot/start-comprun.sh` script. Remove the `--noviewer` option from `startSims` command.

1. First run the server by executing the following command inside `rcrs-server/boot` directory:

    $ ./start-comprun.sh
    
2. Run the agents from eclipse. Right click on `Run.java` and select `Run as -> Java Application`. The agents should connect to the server and the simulation starts.
