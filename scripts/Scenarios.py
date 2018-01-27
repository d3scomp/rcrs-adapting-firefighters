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
LOG_DIR = "LOG_DIR"


USE_EXTENDED_MODES = "USE_EXTENDED_MODES"
WATER_THRESHOLD = "WATER_THRESHOLD"
WATER_NOISE_VARIANCE = "WATER_NOISE_VARIANCE"
FIRE_MAX_DISTANCE_DETECTABILITY = "FIRE_MAX_DISTANCE_DETECTABILITY"
FIRE_MAX_DETECTABLE_DISTANCE = "FIRE_MAX_DETECTABLE_DISTANCE"
FIRE_UNERRING_DETECTABLE_DISTANCE = "FIRE_UNERRING_DETECTABLE_DISTANCE"
FIRE_NOISE_VARIANCE = "FIRE_NOISE_VARIANCE"
FIRE_PROBABILITY_THRESHOLD = "FIRE_PROBABILITY_THRESHOLD"


# Scenarios
scenarios = []

for variance in [0.1, 0.3, 0.5]:
    for maxDistanceDetectability in [0.1, 0.3, 0.5]:
        for fireProbabilityThreshold in [0.2, 0.5, 0.8]:
            for useExtendedmodes in [False, True]:
                scenarios.append({USE_EXTENDED_MODES: useExtendedmodes,
                                  WATER_THRESHOLD: 0,
                                  WATER_NOISE_VARIANCE: variance,
                                  FIRE_MAX_DISTANCE_DETECTABILITY: maxDistanceDetectability,
                                  FIRE_MAX_DETECTABLE_DISTANCE: 40000,
                                  FIRE_UNERRING_DETECTABLE_DISTANCE: 8000,
                                  FIRE_NOISE_VARIANCE: variance,
                                  FIRE_PROBABILITY_THRESHOLD: fireProbabilityThreshold})

# scenarios.append({USE_EXTENDED_MODES:False,
#                   WATER_NOISE_VARIANCE:0.2,
#                   FIRE_MAX_DISTANCE_DETECTABILITY: 0.2,
#                   FIRE_NOISE_VARIANCE: 0.2,
#                   FIRE_PROBABILITY_THRESHOLD: 0.6})
# scenarios.append({USE_EXTENDED_MODES:True,
#                   WATER_NOISE_VARIANCE:0.2,
#                   FIRE_MAX_DISTANCE_DETECTABILITY: 0.2,
#                   FIRE_NOISE_VARIANCE: 0.2,
#                   FIRE_PROBABILITY_THRESHOLD: 0.6})

#################################################

def printDescription(scenario, dir):
    ''' Prints the scenario description. '''
    with open(os.path.join(dir, DESCRIPTION_FILE), "w") as file:
        for key, property in scenario.items():
            file.write("{}: {}\n".format(key, property))
    

def listScenarios():
    print("\nAvailable Scenarios:")
    for i, scenario in enumerate(scenarios):
        print(" {})".format(i))
        for key, property in scenario.items():
            print("  {}: {}".format(key, property))
    print("")


if __name__ == '__main__':
    listScenarios()
    