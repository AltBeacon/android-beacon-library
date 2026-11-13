#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Dec 26 15:23:54 2022

@author: gptshubham595
"""

# fit a line to the economic data
import numpy, scipy, matplotlib
import matplotlib.pyplot as plt
from scipy.optimize import curve_fit
from scipy.optimize import differential_evolution
import warnings
import math
import numpy as np

vector = np.vectorize(np.int_)

yData = numpy.array([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 18, 20, 27, 34, 41])

xData=[1.176470588, 1.215686275, 1.352941176, 1.176470588, 1.254901961, 1.431372549, 1.254901961, 1.31372549, 1.274509804, 1.784313725, 1.490196078, 1.411764706, 1.529411765, 1.588235294, 1.549019608, 1.568627451, 1.450980392, 1.68627451, 1.62745098, 1.62745098]

def func2(x, a, b, c): # from the zunzun.com "function finder"
    return (a * (x**b) + c)

def func(x, a, b): # from the zunzun.com "function finder"
    return (a * (x**b))

def func3(x, a, b, c, d, e): # from the zunzun.com "function finder"
    y=[]
    for i in range(len(x)):
        y.append((a * math.exp(b*x[i]) + c * (x[i]**d) + e))
    return y

# function for genetic algorithm to minimize (sum of squared error)
def sumOfSquaredError(parameterTuple):
    warnings.filterwarnings("ignore") # do not print warnings by genetic algorithm
    val = func(xData, *parameterTuple)
    return numpy.sum((yData - val) ** 2.0)


def generate_Initial_Parameters(i):
    # min and max used for bounds
    maxX = max(xData)
    minX = min(xData)
    maxY = max(yData)
    minY = min(yData)

    minData = min(minX, minY)
    maxData = max(maxX, maxY)

    parameterBounds = []
    parameterBounds.append([0, maxData]) # search bounds for a
    parameterBounds.append([0, maxData]) # search bounds for b
    if(i==1):
        parameterBounds.append([0, maxData]) # search bounds for c
    if(i==2):
        parameterBounds.append([0, maxData]) # search bounds for d
        parameterBounds.append([0, maxData]) # search bounds for e

    # "seed" the numpy random number generator for repeatable results
    result = differential_evolution(sumOfSquaredError, parameterBounds, seed=5)
    return result.x


# by default, differential_evolution completes by calling curve_fit() using parameter bounds
geneticParameters = generate_Initial_Parameters(0)

# now call curve_fit without passing bounds from the genetic algorithm,
# just in case the best fit parameters are aoutside those bounds
#fittedParameters, pcov = curve_fit(func, xData, yData, geneticParameters, maxfev=3000)
bestfit = []
bestRSq = 1000


#for i in range(3):
fittedParameters, pcov = curve_fit(func, xData, yData, geneticParameters, maxfev=8000)

print('Fitted parameters:', fittedParameters)
print()

modelPredictions = func(xData, *fittedParameters)

absError = modelPredictions - yData
SE = numpy.square(absError) # squared errors
MSE = numpy.mean(SE) # mean squared errors
RMSE = numpy.sqrt(MSE) # Root Mean Squared Error, RMSE

Rsquared = 1.0 - (numpy.var(absError) / numpy.var(yData))
if(bestRSq>Rsquared):
    bestRSq = Rsquared
    bestfit = fittedParameters
print()
print('RMSE:', RMSE)
print('R-squared:', Rsquared)

print()

print(bestRSq)
print(bestfit)


##########################################################
# graphics output section
def ModelAndScatterPlot(graphWidth, graphHeight):
    f = plt.figure(figsize=(graphWidth/100.0, graphHeight/100.0), dpi=100)
    axes = f.add_subplot(111)

    # first the raw data as a scatter plot
    axes.plot(xData, yData,  'D')

    # create data for the fitted equation plot
    xModel = numpy.linspace(min(xData), max(xData))
    yModel = func(xModel, *fittedParameters)  #change it to fun or fun2

    # now the model as a line plot
    axes.plot(xModel, yModel)

    axes.set_xlabel('X Data') # X axis data label
    axes.set_ylabel('Y Data') # Y axis data label

    plt.show()
    plt.close('all') # clean up after using pyplot


graphWidth = 800
graphHeight = 600
ModelAndScatterPlot(graphWidth, graphHeight)
