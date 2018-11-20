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

WATER_THRESHOLD = "WATER_THRESHOLD"
WATER_NOISE_VARIANCE = "WATER_NOISE_VARIANCE"

FIRE_MAX_DISTANCE_DETECTABILITY = "FIRE_MAX_DISTANCE_DETECTABILITY"
FIRE_MAX_DETECTABLE_DISTANCE = "FIRE_MAX_DETECTABLE_DISTANCE"
FIRE_UNERRING_DETECTABLE_DISTANCE = "FIRE_UNERRING_DETECTABLE_DISTANCE"
FIRE_NOISE_VARIANCE = "FIRE_NOISE_VARIANCE"
FIRE_PROBABILITY_THRESHOLD = "FIRE_PROBABILITY_THRESHOLD"
FALSE_POSITIV_FIRE_PROBABILITY = "FALSE_POSITIV_FIRE_PROBABILITY"

PEOPLE_NOISE_VARIANCE = "PEOPLE_NOISE_VARIANCE";
AVG_PEOPLE_PER_FLOOR = "AVG_PEOPLE_PER_FLOOR";
VAR_PEOPLE_PER_FLOOR = "VAR_PEOPLE_PER_FLOOR";

WIND_DEFINED_TARGET_PROBABILITY = "WIND_DEFINED_TARGET_PROBABILITY"
WIND_DEFINED_TARGET_DISTANCE = "WIND_DEFINED_TARGET_DISTANCE"

ARIMA_FORECAST_LENGTH = "ARIMA_FORECAST_LENGTH"    
FIRE_ARIMA_ORDER_P = "FIRE_ARIMA_ORDER_P";
FIRE_ARIMA_ORDER_D = "FIRE_ARIMA_ORDER_D";
FIRE_ARIMA_ORDER_Q = "FIRE_ARIMA_ORDER_Q";
WATER_ARIMA_ORDER_P = "WATER_ARIMA_ORDER_P";
WATER_ARIMA_ORDER_D = "WATER_ARIMA_ORDER_D";
WATER_ARIMA_ORDER_Q = "WATER_ARIMA_ORDER_Q";
WIND_ARIMA_ORDER_P = "WIND_ARIMA_ORDER_P";
WIND_ARIMA_ORDER_D = "WIND_ARIMA_ORDER_D";
WIND_ARIMA_ORDER_Q = "WIND_ARIMA_ORDER_Q";

# Scenarios
scenarios = []

#for item in [{'fireProbabilityThreshold':0.4,
#              'maxDistanceDetectability':0.4,
#              'falsePositiveFireProbability':0.2,
#              'variance':0.1}]:
#              {'fireProbabilityThreshold':0.5,
#              'maxDistanceDetectability':0.4,
#              'falsePositiveFireProbability':0.3,
#              'variance':0.1},
#              {'fireProbabilityThreshold':0.4,
#              'maxDistanceDetectability':0.5,
#              'falsePositiveFireProbability':0.2,
#              'variance':0.1},
#              {'fireProbabilityThreshold':0.5,
#              'maxDistanceDetectability':0.5,
#              'falsePositiveFireProbability':0.3,
#              'variance':0.1},
#              {'fireProbabilityThreshold':0.6,
#              'maxDistanceDetectability':0.6,
#              'falsePositiveFireProbability':0.2,
#              'variance':0.2},
#              {'fireProbabilityThreshold':0.5,
#              'maxDistanceDetectability':0.5,
#              'falsePositiveFireProbability':0.2,
#              'variance':0.2}]:
#     for wind in [0.3, 0.6]:
#         for wdist in [50000, 100000]:
#             scenarios.append({TIME_SERIES_MODE: TS_MODE_NONE,
#                           TS_WINDOW_CNT: 0,
#                           TS_WINDOW_SIZE: 0,
#                           WATER_THRESHOLD: 0,
#                           WATER_NOISE_VARIANCE: item['variance'],
#                           FIRE_MAX_DISTANCE_DETECTABILITY: item['maxDistanceDetectability'],
#                           FIRE_MAX_DETECTABLE_DISTANCE: 40000,
#                           FIRE_UNERRING_DETECTABLE_DISTANCE: 8000,
#                           FIRE_NOISE_VARIANCE: item['variance'],
#                           FIRE_PROBABILITY_THRESHOLD: item['fireProbabilityThreshold'],
#                           FALSE_POSITIV_FIRE_PROBABILITY: item['falsePositiveFireProbability'],
#                           WIND_DEFINED_TARGET_PROBABILITY: wind,
#                           WIND_DEFINED_TARGET_DISTANCE: wdist})
#             for ts in [(2, 3), (2, 4), (3, 3), (3, 4)]:
#                 scenarios.append({TIME_SERIES_MODE: TS_MODE_LR,
#                               TS_WINDOW_CNT: ts[0],
#                               TS_WINDOW_SIZE: ts[1],
#                               WATER_THRESHOLD: 0,
#                               WATER_NOISE_VARIANCE: item['variance'],
#                               FIRE_MAX_DISTANCE_DETECTABILITY: item['maxDistanceDetectability'],
#                               FIRE_MAX_DETECTABLE_DISTANCE: 40000,
#                               FIRE_UNERRING_DETECTABLE_DISTANCE: 8000,
#                               FIRE_NOISE_VARIANCE: item['variance'],
#                               FIRE_PROBABILITY_THRESHOLD: item['fireProbabilityThreshold'],
#                               FALSE_POSITIV_FIRE_PROBABILITY: item['falsePositiveFireProbability'],
#                               WIND_DEFINED_TARGET_PROBABILITY: wind,
#                               WIND_DEFINED_TARGET_DISTANCE: wdist})
#                 # ARIMA
#                 scenarios.append({TIME_SERIES_MODE: TS_MODE_ARIMA,
#                               TS_WINDOW_CNT: ts[0],
#                               TS_WINDOW_SIZE: ts[1],
#                               WATER_THRESHOLD: 0,
#                               WATER_NOISE_VARIANCE: item['variance'],
#                               FIRE_MAX_DISTANCE_DETECTABILITY: item['maxDistanceDetectability'],
#                               FIRE_MAX_DETECTABLE_DISTANCE: 40000,
#                               FIRE_UNERRING_DETECTABLE_DISTANCE: 8000,
#                               FIRE_NOISE_VARIANCE: item['variance'],
#                               FIRE_PROBABILITY_THRESHOLD: item['fireProbabilityThreshold'],
#                               FALSE_POSITIV_FIRE_PROBABILITY: item['falsePositiveFireProbability'],
#                               WIND_DEFINED_TARGET_PROBABILITY: wind,
#                               WIND_DEFINED_TARGET_DISTANCE: wdist})

# for waterVariance in [0.1, 0.05, 0.01]:
#     scenarios.append({TIME_SERIES_MODE: TS_MODE_NONE,
#                            WATER_THRESHOLD: 1000,
#                            WATER_NOISE_VARIANCE: waterVariance,
#                            FIRE_MAX_DISTANCE_DETECTABILITY: 0.95,
#                            FIRE_MAX_DETECTABLE_DISTANCE: 40000,
#                            FIRE_UNERRING_DETECTABLE_DISTANCE: 38000,
#                            FIRE_NOISE_VARIANCE: 0.1,
#                            FIRE_PROBABILITY_THRESHOLD: 0.7,
#                            FALSE_POSITIV_FIRE_PROBABILITY: 0.2,
#                            WIND_DEFINED_TARGET_PROBABILITY: 0.3,
#                            WIND_DEFINED_TARGET_DISTANCE: 50000})
#     scenarios.append({TIME_SERIES_MODE: TS_MODE_LR,
#                           TS_WINDOW_CNT: 2,
#                           TS_WINDOW_SIZE: 4,
#                           WATER_THRESHOLD: 1000,
#                           WATER_NOISE_VARIANCE: waterVariance,
#                           FIRE_MAX_DISTANCE_DETECTABILITY: 0.95,
#                           FIRE_MAX_DETECTABLE_DISTANCE: 40000,
#                           FIRE_UNERRING_DETECTABLE_DISTANCE: 38000,
#                           FIRE_NOISE_VARIANCE: 0.1,
#                           FIRE_PROBABILITY_THRESHOLD: 0.7,
#                           FALSE_POSITIV_FIRE_PROBABILITY: 0.2,
#                           WIND_DEFINED_TARGET_PROBABILITY: 0.3,
#                           WIND_DEFINED_TARGET_DISTANCE: 50000})
#     scenarios.append({TIME_SERIES_MODE: TS_MODE_ARIMA,
#                           TS_WINDOW_CNT: 2,
#                           TS_WINDOW_SIZE: 4,
#                           WATER_THRESHOLD: 1000,
#                           WATER_NOISE_VARIANCE: waterVariance,
#                           FIRE_MAX_DISTANCE_DETECTABILITY: 0.95,
#                           FIRE_MAX_DETECTABLE_DISTANCE: 40000,
#                           FIRE_UNERRING_DETECTABLE_DISTANCE: 38000,
#                           FIRE_NOISE_VARIANCE: 0.1,
#                           FIRE_PROBABILITY_THRESHOLD: 0.7,
#                           FALSE_POSITIV_FIRE_PROBABILITY: 0.2,
#                           WIND_DEFINED_TARGET_PROBABILITY: 0.3,
#                           WIND_DEFINED_TARGET_DISTANCE: 50000})
    
for fireThreshold in [0.4, 0.7, 0.9]:
    scenarios.append({TIME_SERIES_MODE: TS_MODE_NONE,
                           WATER_THRESHOLD: 1000,
                           WATER_NOISE_VARIANCE: 0.01,
                           FIRE_MAX_DISTANCE_DETECTABILITY: 0.95,
                           FIRE_MAX_DETECTABLE_DISTANCE: 40000,
                           FIRE_UNERRING_DETECTABLE_DISTANCE: 38000,
                           FIRE_NOISE_VARIANCE: 0.1,
                           FIRE_PROBABILITY_THRESHOLD: fireThreshold,
                           FALSE_POSITIV_FIRE_PROBABILITY: 0.2,
                           WIND_DEFINED_TARGET_PROBABILITY: 0.3,
                           WIND_DEFINED_TARGET_DISTANCE: 50000})
    scenarios.append({TIME_SERIES_MODE: TS_MODE_LR,
                          TS_WINDOW_CNT: 2,
                          TS_WINDOW_SIZE: 4,
                          WATER_THRESHOLD: 1000,
                          WATER_NOISE_VARIANCE: 0.01,
                          FIRE_MAX_DISTANCE_DETECTABILITY: 0.95,
                          FIRE_MAX_DETECTABLE_DISTANCE: 40000,
                          FIRE_UNERRING_DETECTABLE_DISTANCE: 38000,
                          FIRE_NOISE_VARIANCE: 0.1,
                          FIRE_PROBABILITY_THRESHOLD: fireThreshold,
                          FALSE_POSITIV_FIRE_PROBABILITY: 0.2,
                          WIND_DEFINED_TARGET_PROBABILITY: 0.3,
                          WIND_DEFINED_TARGET_DISTANCE: 50000})
    scenarios.append({TIME_SERIES_MODE: TS_MODE_ARIMA,
                          TS_WINDOW_CNT: 2,
                          TS_WINDOW_SIZE: 4,
                          WATER_THRESHOLD: 1000,
                          WATER_NOISE_VARIANCE: 0.01,
                          FIRE_MAX_DISTANCE_DETECTABILITY: 0.95,
                          FIRE_MAX_DETECTABLE_DISTANCE: 40000,
                          FIRE_UNERRING_DETECTABLE_DISTANCE: 38000,
                          FIRE_NOISE_VARIANCE: 0.1,
                          FIRE_PROBABILITY_THRESHOLD: fireThreshold,
                          FALSE_POSITIV_FIRE_PROBABILITY: 0.2,
                          WIND_DEFINED_TARGET_PROBABILITY: 0.3,
                          WIND_DEFINED_TARGET_DISTANCE: 50000})

# for avgPeople in [20, 50, 100]:
#     for varPeople in [5, 20, 50]:
#         for peopleNoise in [0.1, 0.3, 0.5]:
#                 scenarios.append({TIME_SERIES_MODE: TS_MODE_NONE,
#                            WATER_THRESHOLD: 1000,
#                            WATER_NOISE_VARIANCE: 0.01,
#                            FIRE_MAX_DISTANCE_DETECTABILITY: 0.95,
#                            FIRE_MAX_DETECTABLE_DISTANCE: 40000,
#                            FIRE_UNERRING_DETECTABLE_DISTANCE: 38000,
#                            FIRE_NOISE_VARIANCE: 0.1,
#                            FIRE_PROBABILITY_THRESHOLD: 0.7,
#                            FALSE_POSITIV_FIRE_PROBABILITY: 0.2,
#                            AVG_PEOPLE_PER_FLOOR: avgPeople,
#                            VAR_PEOPLE_PER_FLOOR: varPeople,
#                            PEOPLE_NOISE_VARIANCE: peopleNoise,
#                            WIND_DEFINED_TARGET_PROBABILITY: 0.3,
#                            WIND_DEFINED_TARGET_DISTANCE: 50000})
#                 scenarios.append({TIME_SERIES_MODE: TS_MODE_LR,
#                            TS_WINDOW_CNT: 2,
#                            TS_WINDOW_SIZE: 4,
#                            WATER_THRESHOLD: 1000,
#                            WATER_NOISE_VARIANCE: 0.01,
#                            FIRE_MAX_DISTANCE_DETECTABILITY: 0.95,
#                            FIRE_MAX_DETECTABLE_DISTANCE: 40000,
#                            FIRE_UNERRING_DETECTABLE_DISTANCE: 38000,
#                            FIRE_NOISE_VARIANCE: 0.1,
#                            FIRE_PROBABILITY_THRESHOLD: 0.7,
#                            FALSE_POSITIV_FIRE_PROBABILITY: 0.2,
#                            AVG_PEOPLE_PER_FLOOR: avgPeople,
#                            VAR_PEOPLE_PER_FLOOR: varPeople,
#                            PEOPLE_NOISE_VARIANCE: peopleNoise,
#                            WIND_DEFINED_TARGET_PROBABILITY: 0.3,
#                            WIND_DEFINED_TARGET_DISTANCE: 50000})
                
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
    