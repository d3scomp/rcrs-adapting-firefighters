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


ENABLE_SEED = True # Seed usage
seed = 0
seed_step = 1

servers = []
simulated = []

totalSpawnedSimulations = 0


def signal_handler(signal, frame):
        print('\n\nTerminating all jobs...')
        for simulation in simulated:
            simulation.terminate()
        for server in servers:
            server.send_signal(signal)
            #server.terminate()
        sleep(2)
        print('done.')
        sys.exit(0)


def finalizeOldestSimulation():
    # Wait for server to finish
    server = servers[0]
    server.wait()
    servers.pop(0)
    
    # If server finished terminate the simulation
    simulation = simulated[0]
    simulation.terminate()
    simulated.pop(0)


def simulate(scenarioIndex):
    global totalSpawnedSimulations
    scenario = scenarios[scenarioIndex]
          
    print('Spawning simulation processes...')
    
    # invoke number of iterations with the same configuration
    for i in range(1,SIMULATION_ITERATIONS+1):
        params = prepareParameters(scenario)
        if scenario[H3_MECHANISM]:
            prepareH3Scenario(scenario, params)
        elif scenario[H4_MECHANISM]:
            prepareH4Scenario(scenario, params)
        else:
            logFile = getLogFile(scenario, totalSpawnedSimulations)
            params.append("{}={}".format(LOG_DIR, logFile))
            spawnSimulation(params, logFile)
        
    # finalize the rest
    while len(simulated) > 0:
        finalizeOldestSimulation()
        
    print("Simulation processes finished.")
   
   
def spawnSimulation(params, logs):
    global totalSpawnedSimulations
    totalSpawnedSimulations += 1
    
    serverLogs = logs + "_s"
    
    # Wait for free core
    if (len(simulated) >= CORES) :
        finalizeOldestSimulation()
    
    port = str(RCRS_PORT_BASE + totalSpawnedSimulations)
    
    # Compose invocation command
    jars = '../lib/*:../target/rcrs-adapting-firefighters-' + PROJECT_VERSION + '-jar-with-dependencies.jar';
    runAgentsCmd = ['java', '-Xmx4096m', '-cp', jars, 'cz.cuni.mff.d3s.rcrs.af.Run']
    runAgentsCmd.extend(params)
    runAgentsCmd.extend([AGENTS_PORT_PARAM + "=" + port])

    # Remove the kernel log file to be able to wait for its existence
    if os.path.exists(kernelStartedLog(serverLogs)):
        os.remove(kernelStartedLog(serverLogs))

    scriptDir = os.getcwd()
    os.chdir(RCRS_SERVER_BOOT)
    runServerCmd = ['./start-comprun.sh', '-p ', port, '-l', serverLogs]
        
    print(runServerCmd)
    if not os.path.exists(serverLogs):
        os.makedirs(serverLogs)
    with open(logs + "_sout", "w") as out:
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
    print("Simulation {}".format(totalSpawnedSimulations))
    with open(logs + "_cout", "w") as out:
        simulation = Popen(runAgentsCmd, preexec_fn=os.setpgrp)#, stdout=out)
    simulated.append(simulation)
    

def clientCanConnect(serverLogs):
    if not os.path.exists(kernelStartedLog(serverLogs)):
        return False
    
    res = call(["grep", "-q", "Started", kernelStartedLog(serverLogs)])
    return (res == 0)


def kernelStartedLog(serverLogs):
    return serverLogs + "/" + KERNEL_STARTED_FILE
    
def prepareParameters(scenario):    
    global totalSpawnedSimulations
    
    # Prepare parameters
    params = []
    
    if(ENABLE_SEED):
        global seed
        params.append("{}={}".format(WITH_SEED, True))
        params.append("{}={}".format(SEED, seed))
        seed += seed_step
        
    for key, value in scenario.items():        
        # ignore parameters that are used by this script but not by the simulation
        if key in {SCENARIO_NAME,
                   H3_DEGREE,
                   H4_DEGREE}:
            continue;
        
        params.append("{}={}".format(key, value))

    return params


def prepareH3Scenario(scenario, params):
    if scenario[H3_DEGREE] == 1:
        transitions = missingTransitions
    else:
        transitions = missingTransitionsReduced
    
    runH3Scenario(scenario, transitions, [], [], params, scenario[H3_DEGREE])
    

def runH3Scenario(scenario, transitions, preparedTransitions, simulatedTransitions, params, degree):
    
    if degree <= 0:
        for item in simulatedTransitions:
            if set(preparedTransitions).issubset(set(item)):
                return # skip if already done
        logFile, H3params = prepareH3Params(scenario, ".".join(preparedTransitions) + ".")
        params = params + H3params
        # remember what was done
        simulatedTransitions.append(preparedTransitions)
        spawnSimulation(params, logFile)
    else:
        for fromMode, toMode in transitions:
            sTransition = "{}-{}".format(fromMode, toMode)
            if sTransition not in preparedTransitions:
                nextDegreeTransitions = list(preparedTransitions) # create a copy of the given list
                nextDegreeTransitions.append(sTransition)
                runH3Scenario(scenario, transitions, nextDegreeTransitions, simulatedTransitions, params, degree-1)
                

def prepareH3Params(scenario, transitions):
    global totalSpawnedSimulations
    
    params = []
    logFile = getLogFile(scenario, totalSpawnedSimulations, transitions)
    params.append("{}={}".format(LOG_DIR, logFile))
    params.append("{}={}".format(H3_TRANSITIONS, transitions))
        
    return logFile, params


def prepareH4Scenario(scenario, params):
    if scenario[H4_DEGREE] == 1:
        properties = adjustedProperties
    else:
        properties = adjustedPropertiesReduced
    
    runH4Scenario(scenario, properties, [], [], params, scenario[H4_DEGREE])


def runH4Scenario(scenario, properties, preparedProperties, simulatedProperties, params, degree):
    if degree <= 0:
        for item in simulatedProperties:
            if set(preparedProperties).issubset(set(item)):
                return # skip if already done
        logFile, H4params = prepareH4Params(scenario, ".".join(preparedProperties) + ".")
        params = params + H4params
        # remember what was done
        simulatedProperties.append(preparedProperties)
        spawnSimulation(params, logFile)
    else:
        for prop, value in properties:
            sProperty = "{}-{}".format(prop, value)
            if sProperty not in preparedProperties:
                nextDegreeeProperties = list(preparedProperties) # create a copy of the given list
                nextDegreeeProperties.append(sProperty)
                runH4Scenario(scenario, properties, nextDegreeeProperties, simulatedProperties, params, degree-1)


def prepareH4Params(scenario, properties):
    global totalSpawnedSimulations
    
    params = []
    logFile = getLogFile(scenario, totalSpawnedSimulations, properties)
    params.append("{}={}".format(LOG_DIR, logFile))
    params.append("{}={}".format(H4_PROPERTIES, properties))
        
    return logFile, params


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
            print("Simulating scenario {} with signature {}"
                  .format(i, getScenarioSignature(i)))
            simulate(i)
            print("Results placed to {}".format(getScenarioSignature(i)))
        end = time.time()
        
        print("All simulations lasted for {:.2f} mins".format((end-start)/60))
    except ArgError:
        printHelp()
