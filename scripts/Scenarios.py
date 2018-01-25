'''
Created on Jan 8, 2016

Definition of available scenarios to simulate.

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


USE_EXTENDED_MODES = "USE_EXTENDED_MODES"
WATER_THRESHOLD = "WATER_THRESHOLD"
WATER_NOISE = "WATER_NOISE"
FIRE_THRESHOLD = "FIRE_THRESHOLD"
FIRE_NOISE = "FIRE_NOISE"


# Scenarios
scenarios = []

scenarios.append({SCENARIO_NAME:"Baseline",
                  USE_EXTENDED_MODES:False,
                  WATER_THRESHOLD:0,
                  WATER_NOISE:0.2,
                  FIRE_THRESHOLD:0,
                  FIRE_NOISE:0.2})
scenarios.append({SCENARIO_NAME:"Extended modes",
                  USE_EXTENDED_MODES:True,
                  WATER_THRESHOLD:0,
                  WATER_NOISE:0.2,
                  FIRE_THRESHOLD:0,
                  FIRE_NOISE:0.2})

#################################################

def printDescription(scenario, dir):
    ''' Prints the scenario description. '''
    with open(os.path.join(dir, DESCRIPTION_FILE), "w") as file:
        for key, property in scenario.items():
            file.write("{}: {}\n".format(key, property))
    

def listScenarios():
    print("\nAvailable Scenarios:")
    for i, scenario in enumerate(scenarios):
        print(" {}) {}".format(i, scenario[SCENARIO_NAME]))
    print("\n")


if __name__ == '__main__':
    listScenarios()
    