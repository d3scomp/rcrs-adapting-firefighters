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
       
    onFire = []
    notOnFire = []
    water = []
    
    if not os.path.exists(file):
        print("Missing file " + file + " Skipping")
        return onFire, notOnFire, water
    
    onFire.append([])
    onFire.append([])
    onFire.append([])
    notOnFire.append([])
    notOnFire.append([])
    notOnFire.append([])
    water.append([])
    water.append([])
    
    with open(file, 'r') as fp:
        for line in fp.readlines():
            onFire_expr = re.compile('.*onFire: t: \d+; d: (\d+); v: ([-+]?\d*\.\d+|\d+); s: ([-+]?\d*\.\d+|\d+);')
            onFire_match = onFire_expr.match(line)
            notOnFire_expr = re.compile('.*notOnFire: t: \d+; d: (\d+); v: ([-+]?\d*\.\d+|\d+); s: ([-+]?\d*\.\d+|\d+);')
            notOnFire_match = notOnFire_expr.match(line)
            water_expr = re.compile('.*water: t: \d+; v: ([-+]?\d*\.\d+|\d+); s: ([-+]?\d*\.\d+|\d+);')
            water_match = water_expr.match(line)
            
            if onFire_match:
                try:
                    onFire[0].append(int(onFire_match.group(1)))
                    onFire[1].append(float(onFire_match.group(2)))
                    onFire[2].append(float(onFire_match.group(3)))
                except ValueError:
                    print line, ' -- error - not a number'
            if notOnFire_match:
                try:
                    notOnFire[0].append(int(notOnFire_match.group(1)))
                    notOnFire[1].append(float(notOnFire_match.group(2)))
                    notOnFire[2].append(float(notOnFire_match.group(3)))
                except ValueError:
                    print line, ' -- error - not a number'
            if water_match:
                try:
                    water[0].append(float(water_match.group(1)))
                    water[1].append(float(water_match.group(2)))
                except ValueError:
                    print line, ' -- error - not a number'
    
    #print("Found result " + str(result))

    return onFire, notOnFire, water


def analyzeScenario(scenarioIndex):
    scenarioDir = os.path.join(LOGS_DIR, "scenario_{}".format(scenarioIndex))
    runDirPattern = re.compile('run_\d+')
    
    if not os.path.isdir(scenarioDir):
        raise Exception("Logs from scenario {} are missing.".format(scenarioIndex))
                
    onFire_scores = []
    notOnFire_scores = []
    water_scores = []
    onFire_scores.append([])
    onFire_scores.append([])
    onFire_scores.append([])
    notOnFire_scores.append([])
    notOnFire_scores.append([])
    notOnFire_scores.append([])
    water_scores.append([])
    water_scores.append([])
            
    runDirs = (os.path.join(scenarioDir, d) for d in os.listdir(scenarioDir) if os.path.isdir(os.path.join(scenarioDir, d)) and runDirPattern.match(d))
    for runDir in runDirs:
        onFire_score, notOnFire_score, water_score = analyzeLog(runDir)
        if onFire_score:
            onFire_scores[0].append(onFire_score[0])
            onFire_scores[1].append(onFire_score[1])
            onFire_scores[2].append(onFire_score[2])
        if notOnFire_score:
            notOnFire_scores[0].append(notOnFire_score[0])
            notOnFire_scores[1].append(notOnFire_score[1])
            notOnFire_scores[2].append(notOnFire_score[2])
        if water_score:
            water_scores[0].append(water_score[0])
            water_scores[1].append(water_score[1])
        if not (onFire_score and notOnFire_score and water_score):
            print("There is no valid score in " + runDir)
            if scenarioIndex not in failedSimulations:
                failedSimulations[scenarioIndex] = 1
            else:
                failedSimulations[scenarioIndex] += 1

    return onFire_scores, notOnFire_scores, water_scores


###############################################################################

# PLOT ########################################################################


def plot(values, scenario):
    if not os.path.exists(FIGURES_DIR):
        os.makedirs(FIGURES_DIR)
    onFire_outputFile = os.path.join(FIGURES_DIR, "onFire_" + str(scenario))
    notOnFire_outputFile = os.path.join(FIGURES_DIR, "notOnFire_" + str(scenario))
    water_outputFile = os.path.join(FIGURES_DIR, "water_" + str(scenario))
    
    onFire = values[0]
    notOnFire = values[1]
    water = values[2]
    
    plt.plot(onFire[0], onFire[2], 'b+', onFire[0], onFire[1], 'ro')
    plt.xlabel("Distance")
    plt.ylabel("Fire probability")
    plt.savefig("{}.png".format(onFire_outputFile))
    plt.clf()
    
    plt.plot(notOnFire[0], notOnFire[2], 'b+', notOnFire[0], notOnFire[1], 'ro')
    plt.xlabel("Distance")
    plt.ylabel("Fire probability")
    plt.savefig("{}.png".format(notOnFire_outputFile))
    plt.clf()
    
    plt.plot(water[0], water[1], 'b+')
    plt.xlabel("Water")
    plt.ylabel("Noised water")
    plt.savefig("{}.png".format(water_outputFile))
    plt.close()


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
        for scenario in scenarioIndices:
            print("Analyzing scenario {}".format(scenario))
            score = analyzeScenario(scenario)
            plot(score, scenario)

        end = time.time()
        
        print("Analysis lasted for {:.2f} mins".format((end-start)/60))
        print("Plot placed to {}.png".format(os.path.join(FIGURES_DIR, plotSignature)))
        for scenario in scenarioIndices:
            if scenario in failedSimulations:
                print("For scenario {} there failed {} simulation(s).".format(scenario, failedSimulations[scenario]))
    except ArgError:
        printHelp()
    except Exception as e:
        print(e.__str__())

