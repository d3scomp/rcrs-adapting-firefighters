'''
Created on Dec 31, 2015

This script is to ease the invocation of multiple simulation iterations,
taking advantage of multi-core processors.

Usage:
    python Simulate.py scenario [iterations]

Arguments:
    scenario - index of the required scenario
    iterations - number of simulations to perform (optional)

Description:
    Default number of simulations to perform is 1.
    The available scenarios to simulate can be found by running:
        python Scenarios.py

@see: Configuration.py
@see: Scenarios.py

@author: Ilias
@author: Dominik Skoda
'''

import os
import sys
import time
from time import sleep
import signal
from signal import SIGINT
from subprocess import *
from Scenarios import *
from Configuration import *


servers = [] # TODO: move to one object with start time
simulated = []


def signal_handler(signal, frame):
        print('\n\nTerminating all jobs...')
        for simulation in simulated:
            simulation.terminate()
        for server in servers:
            server.send_signal(signal)
        sleep(2)
        print('done.')
        sys.exit(0)


def finalizeOldestSimulation():
    # Wait for server to finish
    server = servers[0]
    attempts = MAX_SERVER_RUN_TIME
    while server.poll() == None:
        print("FIN {}".format(attempts))
        attempts -= 1
        if attempts <= 0:
            server.send_signal(SIGINT)
            break
        sleep(1)
    servers.pop(0)
    
    # If server finished terminate the simulation
    simulation = simulated[0]
    simulation.terminate()
    simulated.pop(0)


def simulate(scenarioIndex):
    scenario = scenarios[scenarioIndex]
    scenarioDir = os.path.join(LOGS_DIR, "scenario_{}".format(scenarioIndex))
    if not os.path.exists(scenarioDir):
        os.makedirs(scenarioDir)
        
    printDescription(scenario, scenarioDir)
          
    print('Spawning simulation processes...')
    
    # invoke number of iterations with the same configuration
    for i in range(1,SIMULATION_ITERATIONS+1):
        runDir = os.path.join(scenarioDir, "run_{}".format(i))
        port = str(RCRS_PORT_BASE + 100*scenarioIndex + i)
        params = prepareParameters(scenarioIndex, runDir, port)
        print("Scenario {}, run {}".format(scenarioIndex, i))       
        spawnSimulation(params, runDir, port)
        
    # finalize the rest
    while len(simulated) > 0:
        finalizeOldestSimulation()
        
    print("Simulation processes finished.")
   
   
def spawnSimulation(params, runDir, port):
    
    serverLogs = os.path.join(runDir,  "server")
    if not os.path.exists(serverLogs):
        os.makedirs(serverLogs)
    
    # Wait for free core
    if (len(simulated) >= CORES):
        finalizeOldestSimulation()
        
    # Compose invocation command
    jars = '../lib/*:../target/adapting-firefighters-' + PROJECT_VERSION + '-jar-with-dependencies.jar';
    runAgentsCmd = ['java', '-Xmx4096m', '-cp', jars, 'cz.cuni.mff.d3s.rcrs.af.Run']
    runAgentsCmd.extend(params)

    # Remove the kernel log file to be able to wait for its existence
    if os.path.exists(kernelStartedLog(serverLogs)):
        os.remove(kernelStartedLog(serverLogs))

    scriptDir = os.getcwd()
    os.chdir(RCRS_SERVER_BOOT)
    runServerCmd = ['./start-comprun.sh', '-p ', port, '-l', serverLogs]
        
    print(runServerCmd)
    with open(os.path.join(runDir, "server.out"), "w") as out:
        server = Popen(runServerCmd, preexec_fn=os.setpgrp, stdout=out)
    servers.append(server)
    os.chdir(scriptDir)
    
    # Wait for the server to start listening
    attempts = 0
    while not clientCanConnect(serverLogs):
        attempts += 1
        if attempts > MAX_SERVER_STARTUP_TIME:
            print("Server failed to start.")
            server.send_signal(SIGINT)
            servers.remove(server)
            return
        print("waiting before connecting agents")
        sleep(1)
    
    print(runAgentsCmd)
    with open(os.path.join(runDir, "client.out"), "w") as out:
        simulation = Popen(runAgentsCmd, preexec_fn=os.setpgrp, stdout=out)
    simulated.append(simulation)
    

def clientCanConnect(serverLogs):
    if not os.path.exists(kernelStartedLog(serverLogs)):
        return False
    
    res = call(["grep", "-q", "Started", kernelStartedLog(serverLogs)])
    return (res == 0)


def kernelStartedLog(serverLogs):
    return os.path.join(serverLogs, KERNEL_STARTED_FILE)
    
def prepareParameters(scenarioIndex, runDir, port):
    scenario = scenarios[scenarioIndex]
    # Prepare parameters
    params = []
    
    for key, value in scenario.items():        
        # ignore parameters that are used by this script but not by the simulation
        if key in { SCENARIO_NAME }:
            continue;
        
        params.append("{}={}".format(key, value))

    logFile = os.path.join(runDir, "client.log")
    params.append("{}={}".format(LOG_DIR, logFile))
    params.append("{}={}".format(AGENTS_PORT_PARAM, port))

    return params


def printHelp():
    print("\nUsage:")
    print("\tpython Simulate.py scenario1 [scenario2 [...]]")
    print("\nArguments:")
    print("\tscenario - index of the required scenario")
    print("\nDescription:")
    print("\tThe available scenarios to simulate can be found by running:"
          "\n\t\tpython Scenarios.py")


def extractScenarioArgs(args):
    # Check argument count (1st argument is this script name)
    if len(args) < 2:
        raise ArgError("At least one scenario argument is required")
    
    scenarioIndices = []
    for i in range(1, len(args)):
        scenarioIndex = int(args[i])
        if len(scenarios) <= scenarioIndex or 0 > scenarioIndex:
            raise ArgError("Scenario index value {} is out of range.".format(scenarioIndex))
        scenarioIndices.append(scenarioIndex)
        
    return scenarioIndices
    

if __name__ == '__main__':
    signal.signal(signal.SIGINT, signal_handler)
        
    try:
        si = extractScenarioArgs(sys.argv)        
        
        start = time.time()
        for i in si:
            print("Simulating scenario {}".format(i))
            simulate(i)
        end = time.time()
        
        print("All simulations lasted for {:.2f} mins".format((end-start)/60))
    except ArgError:
        printHelp()
