## INSTRUCTION TO USE THR POWER REGRESSION PYTHON SCRIPT

In the given scenario, we have two variables, namely `xData` and `yData`. The xData variable represents a list of ratios of RSSI (Received Signal Strength Indication) values between your phone and the iPhone at a distance of 1 meter. On the other hand, the yData variable represents the corresponding distances in meters.

To approximate the distance based on the RSSI values, a function called `fun()` is defined. This function follows the form of `Ax^B`, where A and B are coefficients. However, it is possible to modify the equation and find a better approach by using a different form, such as `Ax^B+C`.

By utilizing this equation and the coefficients obtained, it becomes possible to estimate the approximate or much closer distance value compared to the actual distance. This estimation is achieved by applying the modified equation to the RSSI ratios from xData. The resulting values will correspond to the distances in yData.


## About the script

 - The given code snippet performs a curve fitting analysis on a set of economic data using the `scipy.optimize.curve_fit` function. The goal is to find the best-fit curve that approximates the relationship between two variables, xData and yData.

 - The code begins by importing the necessary libraries, such as `numpy`, `scipy`, and `matplotlib`, and defining the required functions and variables. It defines three functions: `func, func2, and func3`, each representing a different mathematical equation form for the curve fitting.

 - Next, the `sumOfSquaredError` function is defined, which calculates the sum of squared errors between the observed `yData` and the values predicted by the curve fitting function. This function will be used by the genetic algorithm implemented in the `differential_evolution `method to minimize the error and find optimal parameter values.

 - The `generate_Initial_Parameters` function sets the search bounds for the parameters (a, b, c, d, and e) used in the curve fitting process. It then calls the differential_evolution function to find the initial parameter values that minimize the sum of squared errors.

 - The code then proceeds to fit the curve using the curve_fit function, passing the `func`, `xData`, `yData`, and the initial parameter values obtained from the genetic algorithm. The resulting `fittedParameters` represent the best-fit parameters for the curve.

 - After fitting the curve, the code calculates various evaluation metrics, such as the root mean squared error (RMSE) and the coefficient of determination (R-squared), to assess the quality of the fit. It also keeps track of the best-fit parameters and the corresponding R-squared value.

 - Finally, the code generates a scatter plot of the raw data points (`xData` and `yData`) and overlays the fitted curve based on the `fittedParameters`. The plot is displayed using `matplotlib.pyplot` and then closed.

 - Overall, this code performs a curve-fitting analysis on economic data, finds the best-fit curve using a genetic algorithm and curve_fit, and visualizes the results through a scatter plot. 
