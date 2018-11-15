#!/usr/bin/python3

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
    file = os.path.join(runDir, CLIENT_LOG_FILE)
    print("Analyzing " + file)
        
    survivors = 0
    casualties = 0
    
    if not os.path.exists(file):
        print("Missing file " + file + " Skipping")
        return (survivors, casualties)
    
    with open(file, 'r') as fp:
        for line in fp.readlines():
            expr = re.compile('.*Survivors: (\d+); Casualties: (\d+);')
            match = expr.match(line)
            
            if match:
                try:
                    survivors = int(match.group(1))
                    casualties = int(match.group(2))
                except ValueError:
                    print(match.group(1) + " or " + match.group(2) + " error - not a number")
    
    print("Found result " + str(survivors) + " " + str(casualties))

    return (survivors, casualties)


def analyzeScenario(scenarioIndex):
    scenarioDir = os.path.join(LOGS_DIR, "scenario_{}".format(scenarioIndex))
    runDirPattern = re.compile('run_\d+')
    
    if not os.path.isdir(scenarioDir):
        raise Exception("Logs from scenario {} are missing.".format(scenarioIndex))
                
    survivor_scores = []
    casualties_score = []
            
    runDirs = (os.path.join(scenarioDir, d) for d in os.listdir(scenarioDir) if os.path.isdir(os.path.join(scenarioDir, d)) and runDirPattern.match(d))
    for runDir in runDirs:
        survivors, casualties = analyzeLog(runDir)
        if survivors != 0 and casualties != 0:
            survivor_scores.append(survivors)
            casualties_score.append(casualties)
        else:
            print("There is no valid score in " + runDir)
            if scenarioIndex not in failedSimulations:
                failedSimulations[scenarioIndex] = 1
            else:
                failedSimulations[scenarioIndex] += 1

    return (survivor_scores, casualties_score)


###############################################################################

# PLOT ########################################################################

class StringLabel(object):
    def __init__(self, text, color):
        self.my_text = text
        self.my_color = color


class StringLabelHandler(object):
    def legend_artist(self, legend, orig_handle, fontsize, handlebox):
        x0, y0 = handlebox.xdescent, handlebox.ydescent
        width, height = handlebox.width, handlebox.height
        patch = mpltext.Text(x = 0, y = 0, text = orig_handle.my_text,
                             color = orig_handle.my_color,
                             verticalalignment = u'baseline',
                             horizontalalignment = u'left',
                             multialignment = None, fontproperties = None,
                             rotation = 0, linespacing = None, rotation_mode = None)
        handlebox.add_artist(patch)
        return patch


def plot(allValues, scenarioIndices, label):
    if not os.path.exists(FIGURES_DIR):
        os.makedirs(FIGURES_DIR)
    outputFile = os.path.join(FIGURES_DIR, '-'.join(map(str, scenarioIndices)) + "-" + label)
    
    bp = plt.boxplot(allValues)
        
    # add the value of the medians to the diagram 
    # printMedians(bp)
    
    plt.xlabel("Scenario number")
    printYLabel(plt)
    
    # X ticks
    indices = []
    values = []
    i = 1
    for si in scenarioIndices:
        indices.append(i)
        values.append("{}".format(i))
        i = i+1
    plt.xticks(indices, values)
    
    if PLOT_LABELS:
        signatures = []
        labels = []
        i = 1
        for si in scenarioIndices:
            signatures.append(getScenarioSignature(si))
            labels.append(StringLabel(str(i), "black"))
            i = i+1
        plt.legend(labels, signatures, handler_map = {StringLabel:StringLabelHandler()})
    
    plt.savefig("{}.png".format(outputFile))
    plt.close()
    

def printMedians(bp):
    for line in bp['medians']:
        # get position data for median line
        x,y = line.get_xydata()[1] # top of median line
        # overlay median value
        annotate("{:.0f}".format(y), xy = (x - 0.03, y),
                horizontalalignment = 'right',
                verticalalignment = 'bottom',
                fontsize = 10)
        
def printYLabel(plt):
        plt.ylabel("Score")
        


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
        survivor_scores = []
        casualties_scores = []
        for scenario in scenarioIndices:
            print("Analyzing scenario {}".format(scenario))
            survivors, casualties = analyzeScenario(scenario)
            survivor_scores.append(survivors)
            casualties_scores.append(casualties)
        
        plot(survivor_scores, scenarioIndices, "survivors")
        plot(casualties_scores, scenarioIndices, "casualties")

        end = time.time()
        
        print("Analysis lasted for {:.2f} mins".format((end-start)/60))
        print("Plot placed to {}.png".format(os.path.join(FIGURES_DIR, plotSignature)))
        for scenario in scenarioIndices:
            if scenario in failedSimulations:
                print("For scenario {} there failed {} simulation(s).".format(scenario, failedSimulations[scenario]))
    except ArgError as e:
        print(str(e))
        printHelp()
    except Exception as e:
        print(e.__str__())

