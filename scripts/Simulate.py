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
from twisted.test.test_tcp import StartStopFactory


ENABLE_SEED = True # Seed usage
seed = 0
seed_step = 1

servers = []
simulated = []


def signal_handler(signal, frame):
        print('\n\nTerminating all jobs...')
        for simulation in simulated:
            simulation.terminate()
        for server in servers:
            server.send_signal(signal)
            #server.terminate()
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
    scenario = scenarios[scenarioIndex]
          
    print('Spawning simulation processes...')
    
    # invoke number of iterations with the same configuration
    for i in range(1,SIMULATION_ITERATIONS+1):
        params = prepareParameters(scenario, i)
        if scenario[H3_MECHANISM]:    
            if H3_TRAINING in scenario and scenario[H3_TRAINING]:
                    prepareH3Scenario(scenario, params, i)
            else:
                print("Unsupported scenario!")
        elif scenario[H4_MECHANISM]:
            if H4_TRAINING in scenario and scenario[H4_TRAINING]:
                    prepareH4Scenario(scenario, params, i)
            else:
                print("Unsupported scenario!")
            
        else:
            logFile = getLogFile(scenario, i)
            params.append("{}={}".format(LOG_DIR, logFile))
            spawnSimulation(params, i, logFile + "_server")
        
    # finalize the rest
    while len(simulated) > 0:
        finalizeOldestSimulation()
        
    print("Simulation processes finished.")
   
   
def spawnSimulation(params, iteration, serverLogs):
    
    # Wait for free core
    if (len(simulated) >= CORES) :
        finalizeOldestSimulation()
    
    port = str(RCRS_PORT_BASE + iteration)
    
    # Compose invocation command
    jars = '../lib/*:../target/rcrs-adapting-firefighters-' + PROJECT_VERSION + '-jar-with-dependencies.jar';
    runAgentsCmd = ['java', '-Xmx4096m', '-cp', jars, 'cz.cuni.mff.d3s.rcrs.af.Run']
    runAgentsCmd.extend(params)
    runAgentsCmd.extend([AGENTS_PORT_PARAM + "=" + port])

    scriptDir = os.getcwd()
    os.chdir(RCRS_SERVER_BOOT)
    runServerCmd = ['./start-comprun.sh', '-p ', port, '-l', serverLogs]
        
    print(runServerCmd)
    server = Popen(runServerCmd, preexec_fn=os.setpgrp)
    servers.append(server)
    sleep(5)
    
    os.chdir(scriptDir)
    print(runAgentsCmd)
    print("Iteration {}".format(iteration))
    
    simulation = Popen(runAgentsCmd, preexec_fn=os.setpgrp)
    simulated.append(simulation)
    
    
def prepareParameters(scenario, iteration):
    # Prepare parameters
    params = []
    
    if(ENABLE_SEED):
        global seed
        params.append("{}={}".format(WITH_SEED, True))
        params.append("{}={}".format(SEED, seed))
        seed += seed_step
        
    if scenario[H3_MECHANISM]:
        params.append("{}={}".format(H3_TRAINING_OUTPUT,
                                     getUMSLogFile(scenario, iteration)))
        
    for key, value in scenario.items():        
        # ignore parameters that are used by this script but not by the simulation
        if key in {SCENARIO_NAME,
                   H3_TRAINING,
                   H3_TRAINING_DEGREE,
                   H4_TRAINING,
                   H4_TRAINING_DEGREE}:
            continue;
        
        params.append("{}={}".format(key, value))

    return params


def prepareH3Scenario(scenario, params, iteration):
    if scenario[H3_TRAINING_DEGREE] == 1:
        transitions = missingTransitions
    else:
        transitions = missingTransitionsReduced
    
    runH3Scenario(scenario, transitions, [], [], params, iteration, scenario[H3_TRAINING_DEGREE])
    

def runH3Scenario(scenario, transitions, preparedTransitions, simulatedTransitions, params, iteration, degree):
    if degree <= 0:
        for item in simulatedTransitions:
            if set(preparedTransitions).issubset(set(item)):
                return # skip if already done
        logFile, params = params + prepareH3Params(scenario, ";".join(preparedTransitions) + ";", iteration)
        # remember what was done
        simulatedTransitions.append(preparedTransitions)
        spawnSimulation(params, iteration, logFile + "_server")
    else:
        for fromMode, toMode in transitions:
            sTransition = "{}-{}".format(fromMode, toMode)
            if sTransition not in preparedTransitions:
                nextDegreeTransitions = list(preparedTransitions) # create a copy of the given list
                nextDegreeTransitions.append(sTransition)
                runH3Scenario(scenario, transitions, nextDegreeTransitions, simulatedTransitions, params, iteration, degree-1)
                

def prepareH3Params(scenario, transitions, iteration):
    params = []
    logFile = getLogFile(scenario, iteration, transitions)
    params.append("{}={}".format(LOG_DIR, logFile))
    params.append("{}={}".format(H3_TRAIN_TRANSITIONS, transitions))
    params.append("{}={}".format(H3_TRAINING_OUTPUT,
                                 getUMSLogFile(scenario, iteration, transitions)))
        
    return logFile, params


def prepareH4Scenario(scenario, params, iteration):
    if scenario[H4_TRAINING_DEGREE] == 1:
        properties = adjustedProperties
    else:
        properties = adjustedPropertiesReduced
    
    runH4Scenario(scenario, properties, [], [], params, iteration, scenario[H4_TRAINING_DEGREE])


def runH4Scenario(scenario, properties, preparedProperties, simulatedProperties, params, iteration, degree):
    if degree <= 0:
        for item in simulatedProperties:
            if set(preparedProperties).issubset(set(item)):
                return # skip if already done
        logFile, params = params + prepareH4Params(scenario, ";".join(preparedProperties) + ";", iteration)
        # remember what was done
        simulatedProperties.append(preparedProperties)
        spawnSimulation(params, iteration, logFile + "_server")
    else:
        for prop, value in properties:
            sProperty = "{}({})".format(prop, value)
            if sProperty not in preparedProperties:
                nextDegreeeProperties = list(preparedProperties) # create a copy of the given list
                nextDegreeeProperties.append(sProperty)
                runH4Scenario(scenario, properties, nextDegreeeProperties, simulatedProperties, params, iteration, degree-1)


def prepareH4Params(scenario, properties, iteration):
    params = []
    logFile = getLogFile(scenario, iteration, properties)
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
    print("Creating jar with dependencies...")
    shellRequired = True if sys.platform == 'win32' else False
    call(['mvn', '-f..', 'package'], shell=shellRequired)
    print("jar prepared.")
    
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
