'''
Created on Dec 31, 2015

The configuration for the rest of the scripts in the analysis folder.

@author: Dominik Skoda
'''

import os
from enum import Enum


class ArgError(Exception):
    ''' Custom error to indicate invalid argument. '''
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)
    

###############################################################################
# HW RESOURCES
###############################################################################

CORES = 20
''' The number of processor cores to utilize. The number of simulations
    to run in parallel. '''
    
MAX_SERVER_STARTUP_TIME = 90 # 1.5 minute
    
###############################################################################


###############################################################################
# LOCATIONS
###############################################################################

RESULTS_DIR = os.path.realpath(os.path.join('..','results'))
''' The directory where the results from simulation,
    analysis and plots are placed. '''
    
LOGS_DIR = os.path.join(RESULTS_DIR,'logs')
''' The directory where the logs produced by simulations are placed. '''

H3_LOGS = "H3_logs"

FIGURES_DIR = os.path.join(RESULTS_DIR,'figures')
''' The directory where plots are placed. '''

KERNEL_LOG_FILE = "kernel.log"
''' The name of the file containing runtime logs produced by a simulation. '''

KERNEL_STARTED_FILE = "started.log"
''' The name of the file containing runtime logs produced by a simulation. '''

###############################################################################


###############################################################################
# SIMULATION CONFIGURATION
###############################################################################

RCRS_SERVER_BOOT = "../../rcrs-server/boot"

RCRS_PORT_BASE = 7100

AGENTS_PORT_PARAM = "PORT"

SIMULATION_ITERATIONS = 100
''' The number of simulation iterations '''

###############################################################################


###############################################################################
# ANALYSIS CONFIGURATION
###############################################################################

PLOT_LABELS = False
''' Indicates whether the produced plot should contain signature labels. '''

PROJECT_VERSION = "0.0.1"

###############################################################################