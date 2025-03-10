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

## Excel Sheet

- The provided Excel sheet serves as a reference tool for comparing and analyzing distance values based on measured RSSI (Received Signal Strength Indication) values. It is designed to work in conjunction with the script mentioned earlier to obtain the coefficients required for the distance comparison.

 - The Excel sheet allows users to input the necessary data, including the measured RSSI values and corresponding distance values. These values are typically obtained from experimental measurements or data collection processes.

 - Once the data is entered into the Excel sheet, the script mentioned earlier comes into play. It uses the curve fitting algorithm to find the best-fit curve that approximates the relationship between RSSI values and distances. By fitting the data to a mathematical equation, the script determines the coefficients that provide the closest match to the observed data.

 - These coefficients are then obtained from the script and can be manually entered into the Excel sheet. By doing so, the Excel sheet can utilize the coefficients to calculate and display the estimated distances corresponding to new or additional RSSI values.

## INSTRUCTION TO BUILD YOUR OWN SHEET

### Calculating Formula Constants

 After taking distance measurements for a specific Android device, the next step is to run a power regression to get the A, B and C constants used in the y=A*x^B+C formula.

### Make a spreadsheet

 Create a new spreadsheet using Excel, Google Docs or similar. You can see an example spreadsheet [here](https://docs.google.com/spreadsheets/d/1ymREowDj40tYuA5CXd4IfC4WYPXxlx5hq1x8tQcWWCI/edit?usp=sharing) that you can follow as a guide.

 - Paste Your Data
 - Paste your measured iPhone 1m RSSI average into table 1.
 - Paste your measured RSSI values at various distances for the Android device into table 2.
 - Step 1: Calculate Ratio
 - Make a new column beside your measured RSSI values at various distances. Divide your measured RSSI at each distance by the iPhone 1m measured RSSI.

### Step 2: Format Data for Regression
 
 Make two new columns and copy your ratio values into the first column and the distance values into the second column. This will align your dependent and independent varaibles for the power regression.

### Step 3: Run the Regression

While there are many tools that can be used to run a power regression, the online website [here](http://www.xuru.org/rt/powr.asp) may be the easiest. Just paste the two columns from the previous step into the form field, and it will output the A and B constants.

### Step 4: Test the Prediction
 
 Make four new columns to form table 5: RSSI, Ratio, Actual Distance, Predicted Distance, and paste for existing values into the first three. For the fourth column, calculate the predicted distance using the formula y=A*x^B.

### Step 4. Calculate C

 The power regression assumes a zero intercept. In order to optimize distance estimates at 1 meter, we add an intercept variable C. To do this, subtract the actual distance from the predicted distance in table 5 above, and put this in table 6.

### Step 5. Test the Prediction again

 Make four new columns to form table 7: RSSI, Ratio, Actual Distance, Predicted Distance, and paste for existing values into the first three. For the fourth column, calculate the predicted distance using the formula y=A*x^B+C. This is the final formula.

### Step 6. Validate Results

 Check the predicted distances against the actual distances to see that the formula provides a reasonable fit for the device. You may wish to compare if this customized formula predicts distance better than the default Nexus formula with the library before submitting the constants for inclusion in the library.

### Step 7. Submit a Pull Request
 Submit a new Pull Request to the Android Beacon Library project, providing a link to your spreadsheet calculations.
