#!/usr/bin/python3

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

TS_WINDOW_CNT = "TS_WINDOW_CNT"
TS_WINDOW_SIZE = "TS_WINDOW_SIZE"

TIME_SERIES_MODE = "TIME_SERIES_MODE"
TS_MODE_NONE = "None"
TS_MODE_LR = "LR"
TS_MODE_ARIMA = "ARIMA"

WIND_DEFINED_TARGET_PROBABILITY = "WIND_DEFINED_TARGET_PROBABILITY"

WATER_THRESHOLD = "WATER_THRESHOLD"
WATER_NOISE_VARIANCE = "WATER_NOISE_VARIANCE"
FIRE_MAX_DISTANCE_DETECTABILITY = "FIRE_MAX_DISTANCE_DETECTABILITY"
FIRE_MAX_DETECTABLE_DISTANCE = "FIRE_MAX_DETECTABLE_DISTANCE"
FIRE_UNERRING_DETECTABLE_DISTANCE = "FIRE_UNERRING_DETECTABLE_DISTANCE"
FIRE_NOISE_VARIANCE = "FIRE_NOISE_VARIANCE"
FIRE_PROBABILITY_THRESHOLD = "FIRE_PROBABILITY_THRESHOLD"
FALSE_POSITIV_FIRE_PROBABILITY = "FALSE_POSITIV_FIRE_PROBABILITY"
WIND_DEFINED_TARGET_PROBABILITY = "WIND_DEFINED_TARGET_PROBABILITY"


# Scenarios
scenarios = []

scenarios.append({TIME_SERIES_MODE: TS_MODE_NONE,
                      TS_WINDOW_CNT: 0,
                      TS_WINDOW_SIZE: 0,
                      WIND_DEFINED_TARGET_PROBABILITY: 0,
                      WATER_THRESHOLD: 0,
                      WATER_NOISE_VARIANCE: 0,
                      FIRE_MAX_DISTANCE_DETECTABILITY: 0.8,
                      FIRE_MAX_DETECTABLE_DISTANCE: 40000,
                      FIRE_UNERRING_DETECTABLE_DISTANCE: 8000,
                      FIRE_NOISE_VARIANCE: 0,
                      FIRE_PROBABILITY_THRESHOLD: 0.6,
                      FALSE_POSITIV_FIRE_PROBABILITY: 0.01})
scenarios.append({TIME_SERIES_MODE: TS_MODE_ARIMA,
                      TS_WINDOW_CNT: 5,
                      TS_WINDOW_SIZE: 5,
                      WIND_DEFINED_TARGET_PROBABILITY: 1,
                      WATER_THRESHOLD: 0,
                      WATER_NOISE_VARIANCE: 0,
                      FIRE_MAX_DISTANCE_DETECTABILITY: 0.5,
                      FIRE_MAX_DETECTABLE_DISTANCE: 40000,
                      FIRE_UNERRING_DETECTABLE_DISTANCE: 8000,
                      FIRE_NOISE_VARIANCE: 0,
                      FIRE_PROBABILITY_THRESHOLD: 0.5,
                      FALSE_POSITIV_FIRE_PROBABILITY: 0.2})

for item in [{'fireProbabilityThreshold':0.4, 
              'maxDistanceDetectability':0.4,
              'falsePositiveFireProbability':0.2,
              'variance':0.1},
              {'fireProbabilityThreshold':0.5, 
              'maxDistanceDetectability':0.4,
              'falsePositiveFireProbability':0.3,
              'variance':0.1},
              {'fireProbabilityThreshold':0.4, 
              'maxDistanceDetectability':0.5,
              'falsePositiveFireProbability':0.2,
              'variance':0.1},
              {'fireProbabilityThreshold':0.5, 
              'maxDistanceDetectability':0.5,
              'falsePositiveFireProbability':0.3,
              'variance':0.1},
              {'fireProbabilityThreshold':0.5, 
              'maxDistanceDetectability':0.4,
              'falsePositiveFireProbability':0.2,
              'variance':0.15},
              {'fireProbabilityThreshold':0.5, 
              'maxDistanceDetectability':0.5,
              'falsePositiveFireProbability':0.2,
              'variance':0.15}]:
    scenarios.append({TIME_SERIES_MODE: TS_MODE_NONE,
                      TS_WINDOW_CNT: 0,
                      TS_WINDOW_SIZE: 0,
                      WIND_DEFINED_TARGET_PROBABILITY: 0,
                      WATER_THRESHOLD: 0,
                      WATER_NOISE_VARIANCE: item['variance'],
                      FIRE_MAX_DISTANCE_DETECTABILITY: item['maxDistanceDetectability'],
                      FIRE_MAX_DETECTABLE_DISTANCE: 40000,
                      FIRE_UNERRING_DETECTABLE_DISTANCE: 8000,
                      FIRE_NOISE_VARIANCE: item['variance'],
                      FIRE_PROBABILITY_THRESHOLD: item['fireProbabilityThreshold'],
                      FALSE_POSITIV_FIRE_PROBABILITY: item['falsePositiveFireProbability']})
    for ts in [(2, 4), (2, 5), (3, 3), (3, 4), (4, 2), (4, 3), (5, 2)]:
        scenarios.append({TIME_SERIES_MODE: TS_MODE_ARIMA,
                          TS_WINDOW_CNT: ts[0],
                          TS_WINDOW_SIZE: ts[1],
                          WIND_DEFINED_TARGET_PROBABILITY: 1,
                          WATER_THRESHOLD: 0,
                          WATER_NOISE_VARIANCE: item['variance'],
                          FIRE_MAX_DISTANCE_DETECTABILITY: item['maxDistanceDetectability'],
                          FIRE_MAX_DETECTABLE_DISTANCE: 40000,
                          FIRE_UNERRING_DETECTABLE_DISTANCE: 8000,
                          FIRE_NOISE_VARIANCE: item['variance'],
                          FIRE_PROBABILITY_THRESHOLD: item['fireProbabilityThreshold'],
                          FALSE_POSITIV_FIRE_PROBABILITY: item['falsePositiveFireProbability']})


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
    