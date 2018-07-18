#!/usr/bin/python3

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
from datetime import datetime
import signal
from signal import SIGINT
from subprocess import *
from Scenarios import *
from Configuration import *

class Simulation:
    
    serverProcess = None
    clientProcess = None
    
    def __init__(self, serverProcess, scenario, run):
        self.startTime = datetime.now()
        self.scenario = scenario
        self.run = run
        self.serverProcess = serverProcess
        
    def attachClient(self, clientProcess):
        self.clientProcess = clientProcess
    
    def terminate(self):
        if self.clientProcess != None and self.clientProcess.poll() == None:
            self.clientProcess.terminate()
        if self.serverProcess != None and self.serverProcess.poll() == None:
            self.serverProcess.send_signal(SIGINT)
        print("Scenario {}, run {} terminated".format(self.scenario, self.run))
        
    def finalize(self):
        timeDiff = (datetime.now() - self.startTime).total_seconds()
        while self.serverProcess.poll() == None and timeDiff <= MAX_SERVER_RUN_TIME:
            print("Scenario {}, run {} will timeout in {}".format(self.scenario, self.run, MAX_SERVER_RUN_TIME - timeDiff))
            sleep(10)            
            timeDiff = (datetime.now() - self.startTime).total_seconds()

        self.terminate()
        

simulations = []


def signal_handler(signal, frame):
        print('\n\nTerminating all jobs...')
        for simulation in simulations:
            simulation.terminate()
        
        sleep(2)
        print('done.')
        sys.exit(0)


def finalizeOldestSimulation():
    simulation = simulations[0]
    simulation.finalize()
    simulations.pop(0)


def simulate(scenarioIndices):
    for scenarioIndex in scenarioIndices:
        print("Simulating scenario {}".format(scenarioIndex))
        
        scenario = scenarios[scenarioIndex]
        scenarioDir = os.path.join(LOGS_DIR, "scenario_{}".format(scenarioIndex))
        if not os.path.exists(scenarioDir):
            os.makedirs(scenarioDir)
            
        printDescription(scenario, scenarioDir)
        
        # invoke number of iterations with the same configuration
        for i in range(1,SIMULATION_ITERATIONS+1):
            runDir = os.path.join(scenarioDir, "run_{}".format(i))
            port = str(RCRS_PORT_BASE + 100*scenarioIndex + i)
            params = prepareParameters(scenarioIndex, runDir, port)
            print("Scenario {}, run {}".format(scenarioIndex, i))       
            spawnSimulation(params, runDir, port, scenarioIndex, i)
        
    # finalize the rest
    while len(simulations) > 0:
        finalizeOldestSimulation()
   

def spawnSimulation(params, runDir, port, scenarioIndex, runIndex):
    
    serverLogs = os.path.join(runDir,  "server")
    if not os.path.exists(serverLogs):
        os.makedirs(serverLogs)
    
    # Wait for free core
    if (len(simulations) >= CORES):
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
    simulation = Simulation(server, scenarioIndex, runIndex)
    os.chdir(scriptDir)
    
    # Wait for the server to start listening
    attempts = 0
    while not clientCanConnect(serverLogs):
        attempts += 1
        if attempts > MAX_SERVER_STARTUP_TIME:
            print("Server failed to start.")
            simulation.terminate()
            return
        print("Scenario {}, run {} waiting before connecting agents".format(scenarioIndex, runIndex))
        sleep(5)
    
    print(runAgentsCmd)
    with open(os.path.join(runDir, "client.out"), "w") as out:
        client = Popen(runAgentsCmd, preexec_fn=os.setpgrp, stdout=out)
    simulation.attachClient(client)
    simulations.append(simulation)
    

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
        return []
    
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
        if not si:
            si = range(len(scenarios))
      
        start = time.time()
        simulate(si)
        end = time.time()
        
        print("All simulations lasted for {:.2f} mins".format((end-start)/60))
    except ArgError:
        printHelp()
