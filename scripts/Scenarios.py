'''
Created on Jan 8, 2016

Definition of available scenarios to simulate.
Provides a method to generate a signature of a scenario to distinguish
folders and files produced by simulating and analyzing the scenario.

Explanations of used shortcuts:
    H1_INTRODUCE_FAILURE - Dirt detection failure
    H1_MECHANISM - Collaborative sensing
    H2_INTRODUCE_FAILURE - Dock failure
    H2_MECHANISM - Faulty component isolation
    H3_MECHANISM - Unspecified mode switching

An exclamation mark (!) in front of a shortcut means the feature/failure
is inactive, otherwise it is active.
The probability following the H3_MECHANISM is the starting H3_MECHANISM probability.

@author: Ilias
@author: Dominik Skoda
'''

from Configuration import *

#################################################
# SCENARIOS
#################################################

# Parameters names
SCENARIO_NAME = "SCENARIO_NAME"
LOG_DIR = "LOG_DIR"

WITH_SEED = "WITH_SEED"
SEED = "SEED"

H1_INTRODUCE_FAILURE = "H1_INTRODUCE_FAILURE" # Building detection failure
H1_FAILURE_TIME = "H1_FAILURE_TIME"
H1_FAILURE_IDS = "H1_FAILURE_IDS"
H1_MECHANISM = "H1_MECHANISM" # Collaborative sensing
H1_DEFAULT_TIME = 30
''' The default time of building detection failure in seconds. '''

H2_INTRODUCE_FAILURE = "H2_INTRODUCE_FAILURE" # Movement failure
H2_FAILURE_TIME = "H2_FAILURE_TIME"
H2_FAILURE_IDS = "H2_FAILURE_IDS"
H2_MECHANISM = "H2_MECHANISM" # Faulty component isolation
H2_DEFAULT_TIME = 30
''' The default time of movement failure in seconds. '''

# ENHANCING MODE SWITCHING
H3_MECHANISM = "H3_MECHANISM" # Enhancing mode switching
H3_TRANSITION_PROBABILITY = "H3_TRANSITION_PROBABILITY" 
H3_TRANSITION_PRIORITY = "H3_TRANSITION_PRIORITY"
H3_DEGREE = "H3_DEGREE"
H3_TRANSITIONS = "H3_TRANSITIONS"

# Mode Switch Properties
H4_MECHANISM = "H4_MECHANISM";
H4_DEGREE = "H4_DEGREE"
H4_PROPERTIES = "H4_PROPERTIES";

# Transitions not present in the default mode chart
missingTransitions = [
    ("SearchMode","RefillMode"),
    ("SearchMode","MoveToFireMode"),
    ("SearchMode","MoveToRefillMode"),
    ("ExtinguishMode","RefillMode"),
    ("ExtinguishMode","MoveToFireMode"),
    ("RefillMode","ExtinguishMode"),
    ("RefillMode","MoveToRefillMode"),
    ("MoveToFireMode","SearchMode"),
    ("MoveToFireMode","RefillMode"),
    ("MoveToFireMode","MoveToRefillMode"),
    ("MoveToRefillMode","SearchMode"),
    ("MoveToRefillMode","ExtinguishMode"),
    ("MoveToRefillMode","MoveToFireMode")]
missingTransitionsReduced = [
    ("SearchMode","MoveToRefillMode"),
    ("RefillMode","ExtinguishMode"),
    ("MoveToFireMode","SearchMode")]

# Properties used in scenarios with mode switching properties adjustment
adjustedProperties = [
    ("FILLED_LEVEL_TO_LEAVE", "20000"),
    ("FILLED_LEVEL_TO_LEAVE", "15000"),
    ("FILLED_LEVEL_TO_LEAVE", "10000"),
    ("FILLED_LEVEL_TO_CONTINUE", "10000"),
    ("FILLED_LEVEL_TO_CONTINUE", "15000"),
    ("FILLED_LEVEL_TO_CONTINUE", "20000"),
    ("EMPTY_LEVEL", "2000"),
    ("EMPTY_LEVEL", "5000"),
    ("EMPTY_LEVEL", "10000")]

adjustedPropertiesReduced = [
    ("FILLED_LEVEL_TO_LEAVE", "20000"),
    ("FILLED_LEVEL_TO_CONTINUE", "10000"),
    ("EMPTY_LEVEL", "5000")]

# Scenarios
scenarios = []
# Baseline 3 docks
scenarios.append({SCENARIO_NAME:"Baseline\t",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:False,
                  H4_MECHANISM:False})
# Building detection failure
scenarios.append({SCENARIO_NAME:"H1 failure\t",
                  H1_INTRODUCE_FAILURE:True,
                  H1_FAILURE_TIME:H1_DEFAULT_TIME,
                  H1_MECHANISM:False,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:False,
                  H4_MECHANISM:False})
scenarios.append({SCENARIO_NAME:"H1 remedy\t",
                  H1_INTRODUCE_FAILURE:True,
                  H1_FAILURE_TIME:H1_DEFAULT_TIME,
                  H1_MECHANISM:True,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:False,
                  H4_MECHANISM:False})
# Movement failure
scenarios.append({SCENARIO_NAME:"H2 failure\t",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:True,
                  H2_MECHANISM:False,
                  H3_MECHANISM:False,
                  H4_MECHANISM:False})
scenarios.append({SCENARIO_NAME:"H2 remedy\t",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:True,
                  H2_MECHANISM:True,
                  H3_MECHANISM:False,
                  H4_MECHANISM:False})
# Enhanced mode switching
scenarios.append({SCENARIO_NAME:"H3 p=0.001 deg=1",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:True,
                  H3_DEGREE:1,
                  H3_TRANSITION_PROBABILITY:0.001,
                  H3_TRANSITION_PRIORITY:10,
                  H4_MECHANISM:False})
scenarios.append({SCENARIO_NAME:"H3 p=0.01 deg=1",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:True,
                  H3_DEGREE:1,
                  H3_TRANSITION_PROBABILITY:0.01,
                  H3_TRANSITION_PRIORITY:10,
                  H4_MECHANISM:False})
scenarios.append({SCENARIO_NAME:"H3 p=0.001 deg=2",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:True,
                  H3_DEGREE:2,
                  H3_TRANSITION_PROBABILITY:0.001,
                  H3_TRANSITION_PRIORITY:10,
                  H4_MECHANISM:False})
scenarios.append({SCENARIO_NAME:"H3 p=0.01 deg=2",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:True,
                  H3_DEGREE:2,
                  H3_TRANSITION_PROBABILITY:0.01,
                  H3_TRANSITION_PRIORITY:10,
                  H4_MECHANISM:False})
# Mode Switch Properties
scenarios.append({SCENARIO_NAME:"H4 deg=1\t",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:False,
                  H4_MECHANISM:True,
                  H4_DEGREE:1})
scenarios.append({SCENARIO_NAME:"H4 deg=2\t",
                  H1_INTRODUCE_FAILURE:False,
                  H2_INTRODUCE_FAILURE:False,
                  H3_MECHANISM:False,
                  H4_MECHANISM:True,
                  H4_DEGREE:2})


#################################################


def getSignature(scenario, iterations = 0, detailed = False):
    ''' Compiles the signature of the given scenario. '''
    outputSignature = []
    outputSignature.append("{:02}".format(scenarios.index(scenario)))
    
    if detailed:
        if SCENARIO_NAME in scenario:
            outputSignature.append(" {}\t".format(scenario[SCENARIO_NAME]))
        else:
            raise Exception("Scenario name missing.")
    
    if scenario[H1_INTRODUCE_FAILURE]:
        outputSignature.append("H1F-")
        if detailed:
            if H1_FAILURE_TIME in scenario:
                outputSignature.append(str(scenario[H1_FAILURE_TIME]))
            else:    
                outputSignature.append(str(H1_DEFAULT_TIME))
            outputSignature.append("-")
        outputSignature.append("H1M-" if (scenario[H1_MECHANISM]) else "noH1M-")
    else:
        outputSignature.append("noH1F-")
    if scenario[H2_INTRODUCE_FAILURE]:
        outputSignature.append("H2F-")
        if detailed:
            if H2_FAILURE_TIME in scenario:
                outputSignature.append(str(scenario[H2_FAILURE_TIME]))
            else:    
                outputSignature.append(str(H2_DEFAULT_TIME))
            outputSignature.append("-")
        outputSignature.append("H2M" if (scenario[H2_MECHANISM]) else "noH2M")
    else:
        outputSignature.append("noH2F")
    if scenario[H3_MECHANISM]:
        outputSignature.append("-H3M")
        outputSignature.append(str(scenario[H3_DEGREE]))
        if detailed:
            outputSignature.append("-P" + str(scenario[H3_TRANSITION_PROBABILITY]))
    if scenario[H4_MECHANISM]:
        outputSignature.append("-H4M")
        outputSignature.append(str(scenario[H4_DEGREE]))
    if iterations > 0:
        outputSignature.append("-it-" + str(iterations) + "-")
    return ''.join(outputSignature)


def getScenarioSignature(scenarioIndex, iterations = 0):
    ''' Compiles the signature of the given scenario. '''
    return getSignature(scenarios[scenarioIndex], iterations)


def getLogFile(scenario, iteration, specifier = None):
    if(specifier != None):
        return os.path.join(LOGS_DIR,
                        getSignature(scenario),
                        specifier + "log_" + str(iteration))
        
    return os.path.join(LOGS_DIR,
                        getSignature(scenario),
                        'log_' + str(iteration))
    
    
def getH3LogFile(scenario, iteration, transitions = None):
    if(scenario[H3_MECHANISM]):
        if(transitions != None):
            return os.path.join(LOGS_DIR,
                            getSignature(scenario),
                            H3_LOGS,
                            transitions,
                            'log_' + str(iteration))
    
    return os.path.join(LOGS_DIR,
                        getSignature(scenario),
                        H3_LOGS,
                        'log_' + str(iteration))
    

def listScenarios():
    print("\nExplanations of used shortcuts:")
    print("H1F - Building detection failure")
    print("H1M - Collaborative sensing")
    print("H2F - Movement failure")
    print("H2M - Faulty component isolation")
    print("H3M - Enhancing mode switching")
    print("P - Starting probability for H3")
    print("T - How many transitions tried at once")
    print("\nAn exclamation mark (!) in front of a shortcut "
          "means the mechanism/failure is inactive, "
          "otherwise it is active.\nA number following "
          "the H3M is the scenario index that contains the "
          "detailed H3M configuration.")
    print("\nAvailable Scenarios:")
    for scenario in scenarios:
        print(getSignature(scenario, detailed = True))
    print("\n")


if __name__ == '__main__':
    listScenarios()
    