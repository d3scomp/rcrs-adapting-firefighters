'''
Created on Dec 8, 2015

This script extracts and plots scores produced by simulations.

@author: Ilias
@author: Dominik Skoda
'''

import os
import re
import sys
import time
import shutil
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.text as mpltext
from matplotlib.font_manager import FontProperties
from pylab import *
from Scenarios import *
from Configuration import *


failedSimulations = {}


# ANALYSIS ####################################################################

def analyzeLog(runDir):
    file = os.path.join(runDir, "client.log")
    print("Analyzing " + file)
    
    foundValues = False    
    result = {"MIN":100000, "MAX":0, "SUM":0, "CNT":0}
    
    if not os.path.exists(file):
        print("Missing file " + file + " Skipping")
        return None
    
    with open(file, 'r') as fp:
        for line in fp.readlines():
            expr = re.compile('.*Fire detected distance (\S+)')
            match = expr.match(line)
            
            if match:
                try:
                    value = int(match.group(1))
                    if value > result["MAX"]:
                        result["MAX"] = value
                    if value < result["MIN"]:
                        result["MIN"] = value
                    result["SUM"] += value
                    result["CNT"] += 1
                    foundValues = True
                except ValueError:
                    print match.group(1), 'error - not a number'

    if foundValues:
        return result
    else:
        return None


def analyzeScenario(scenarioIndex):
    scenarioDir = os.path.join(LOGS_DIR, "scenario_{}".format(scenarioIndex))
    runDirPattern = re.compile('run_\d+')
    
    if not os.path.isdir(scenarioDir):
        raise Exception("Logs from scenario {} are missing.".format(scenarioIndex))
                
    scores = []
            
    runDirs = (os.path.join(scenarioDir, d) for d in os.listdir(scenarioDir) if os.path.isdir(os.path.join(scenarioDir, d)) and runDirPattern.match(d))
    for runDir in runDirs:
        score = analyzeLog(runDir)
        if score != None:
            scores.append(score)
        else:
            print("There is no valid score in " + runDir)
            if scenarioIndex not in failedSimulations:
                failedSimulations[scenarioIndex] = 1
            else:
                failedSimulations[scenarioIndex] += 1

    return scores


###############################################################################

def printHelp():
    print("\nUsage:")
    print("\tpython Plot.py scenario1 [scenario2 [...]] ")
    print("\nArguments:")
    print("\tscenario - index of the required scenario")
    print("\nDescription:")
    print("\tThe scenario to plot has to be already simulated and the logs "
          "produced by the simulation has to be available."
          "\n\tThe available scenarios to simulate and analyze can be found by running:"
          "\n\t\tpython Scenarios.py")


def extractArgs(args):
    # Check argument count (1st argument is this script name)
    if len(args) < 2:
        raise ArgError("At least one scenario argument is required")
    
    scenarioIndices = []
    lastIndex = -1;
    for i in range(1, len(args)):
        scenarioIndex = int(args[i])
        if len(scenarios) <= scenarioIndex or 0 > scenarioIndex:
            raise ArgError("Scenario index value {} is out of range.".format(scenarioIndex))
        scenarioIndices.append(scenarioIndex)
    
    return scenarioIndices


if __name__ == '__main__':
        
    try:                
        scenarioIndices = extractArgs(sys.argv)
        
        start = time.time()
        
        plotSignature = '-'.join(map(str, scenarioIndices))
        scores = []
        for scenario in scenarioIndices:
            print("Analyzing scenario {}".format(scenario))
            scores.append(analyzeScenario(scenario))
        for i, scenario in enumerate(scores):
            print("\nScenario {}".format(scenarioIndices[i]))
            for j, run in enumerate(scenario):
                print("Run {}".format(j))
                print("Max distance: {}".format(run["MAX"]))
                print("Min distance: {}".format(run["MIN"]))
                print("Avg distance: {}".format(run["SUM"] / run["CNT"]))
        print("")
        
        end = time.time()
        
        for scenario in scenarioIndices:
            if scenario in failedSimulations:
                print("For scenario {} there failed {} simulation(s).".format(scenario, failedSimulations[scenario]))
    except ArgError:
        printHelp()
    except Exception as e:
        print(e.__str__())

