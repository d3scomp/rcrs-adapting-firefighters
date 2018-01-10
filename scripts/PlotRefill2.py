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


suffix = "_refill"


# ANALYSIS ####################################################################

def analyzeLog(simulationSignature, logDirFile):
    file = os.path.join(LOGS_DIR, simulationSignature, logDirFile)
    print("Analyzing " + file)
    
    inProgress = {}
    result = []
    
    if not os.path.exists(file):
        print("Missing file " + file + " Skipping")
        return result
    
    with open(file, 'r') as fp:
        for line in fp.readlines():
            begin = re.compile('.*T\[(\d+)\].*(FF\d+) moving towards refill target: \d+')
            end1 = re.compile('.*T\[(\d+)\] .* (FF\d+) moving to fire.*')
            end2 = re.compile('.*T\[(\d+)\] .* (FF\d+) searching.*')
            matchBegin = begin.match(line)
            matchEnd1 = end1.match(line)
            matchEnd2 = end2.match(line)
            
            if matchBegin:
                try:
                    bTime = int(matchBegin.group(1))
                    bAgent = matchBegin.group(2)
                    if bAgent not in inProgress:
                        inProgress[bAgent] = bTime
                except ValueError:
                    print matchBegin.group(1), 'error - not a number'
                    
            if matchEnd1 or matchEnd2:
                matchEnd = matchEnd1 if matchEnd1 != None else matchEnd2
                try:
                    eTime = int(matchEnd.group(1))
                    eAgent = matchEnd.group(2)
                    if eAgent in inProgress:
                        result.append(eTime - inProgress[eAgent])
                        del inProgress[eAgent]
                except ValueError:
                    print matchEnd.group(1), 'error - not a number'
    
    print("Found " + str(len(result)) + " records")
    
    return result


def analyzeScenario(scenario):
    signature = getScenarioSignature(scenario)
    validDir = re.compile('.*log_\d+$')
    logsDir = os.path.join(LOGS_DIR, signature)
    
    if not os.path.isdir(logsDir):
        raise Exception("Logs from scenario {} are missing.".format(signature))
                
    scores = []
            
    for _, _, files in os.walk(logsDir):
        for logDirFile in files:
            if(validDir.match(logDirFile) == None):
                continue;
            scores  += analyzeLog(signature, logDirFile)

    return scores


def analyzeH3H4Scenario(scenario):
    signature = getScenarioSignature(scenario)
    validDir = re.compile('.*log_\d+$')
    logsDir = os.path.join(LOGS_DIR, signature)
    
    if not os.path.isdir(logsDir):
        raise Exception("Logs from scenario {} are missing.".format(signature))
                
    scores = {}
            
    for _, _, files in os.walk(logsDir):
        for logDirFile in files:
            if(validDir.match(logDirFile) == None):
                continue;
            score = analyzeLog(signature, logDirFile)
            if scenarios[scenario][H3_MECHANISM]:
                index = getTransitions(logDirFile)
            elif scenarios[scenario][H4_MECHANISM]:
                index = getProperties(logDirFile)
            else:
                raise Exception("Scenario {} is not H3 neither H4 Mechanism.".format(signature))
            if(index not in scores):
                scores[index] = []
            
            scores[index] += score
            
    return scores


def getTransitions(logDirName):
    fromTo = re.compile('((\w+-\w+\.)+)[^\.]+')
    match = fromTo.match(logDirName)
    if(match != None):
        return str(match.group(1))
    else:
        raise Exception("Transition not matched from {}.".format(logDirName))

def getProperties(logDirName):
    fromTo = re.compile('(\w+)-([+-]?\d*\.?\d+)\.[^\.]+')
    match = fromTo.match(logDirName)
    if(match != None):
        return "{}({})".format(match.group(1), match.group(2))
    else:
        raise Exception("Property not matched from {}.".format(logDirName))
    

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


def plot(allValues, scenarioIndices):
    if not os.path.exists(FIGURES_DIR):
        os.makedirs(FIGURES_DIR)
    outputFile = os.path.join(FIGURES_DIR, '-'.join(map(str, scenarioIndices)))
    
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
    
    plt.savefig("{}{}.png".format(outputFile, suffix))
    
    
def plotH3H4(baselineValues, allValues, scenarioIndices):
    if not os.path.exists(FIGURES_DIR):
        os.makedirs(FIGURES_DIR)
    outputFile = os.path.join(FIGURES_DIR, '-'.join(map(str, scenarioIndices)))
    
    legendFile = open("{}.txt".format(outputFile), 'w')
        
    labels = []
    values = []
    signatures = []
    # Prepend baseline
    signatures.append("baseline")
    labels.append(StringLabel(str(1), "black"))
    values.append(baselineValues)
    
    print("LEGEND:")
    print("{}\t{}".format(1, "baseline"))
    legendFile.write("LEGEND:\n")
    legendFile.write("{} - {}\n".format(1, "baseline"))
    
    i = 2
    for k in allValues.keys():
        signatures.append(k)
        labels.append(StringLabel(str(i), "black"))
        print("{}\t{}".format(i, k))
        legendFile.write("{} - {}\n".format(i, k))
        i = i + 1
        values.append(allValues[k])
        
    legendFile.close()
    
    if len(values) > 20:
        plt.figure(figsize=(10,8))
        sp = plt.subplot()
        bp = sp.boxplot(values, widths=0.8)
    else:
        plt.figure()
        sp = plt.subplot()
        bp = sp.boxplot(values)
    # add the value of the medians to the diagram 
    # printMedians(bp)
    
    printYLabel(plt)
    
    # Legend
#    box = sp.get_position()
#    sp.set_position([box.x0, box.y0, box.width*0.5, box.height])
#    fontP = FontProperties()
#    fontP.set_size('small')
#    sp.legend(labels, signatures, handler_map = {StringLabel:StringLabelHandler()}, loc='center left', bbox_to_anchor=(1, 0.5), prop = fontP)
    
    plt.savefig("{}{}.png".format(outputFile, suffix))



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
        scores = []
        for scenario in scenarioIndices:
            print("Analyzing scenario {}".format(scenario))
            
            if scenarios[scenario][H3_MECHANISM] or scenarios[scenario][H4_MECHANISM]:
                baselineScore = analyzeScenario(BASELINE_SCENARIO)
                t = analyzeH3H4Scenario(scenario)
                plotH3H4(baselineScore, t, scenarioIndices)
            else:
                scores.append(analyzeScenario(scenario))
        
        if scores:
            plot(scores, scenarioIndices)

        end = time.time()
        
        print("Analysis lasted for {:.2f} mins".format((end-start)/60))
        print("Plot placed to {}{}.png".format(os.path.join(FIGURES_DIR, plotSignature), suffix))
    except ArgError:
        printHelp()
    except Exception as e:
        print(e.__str__())

