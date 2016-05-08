---
layout: android-beacon-library
---

### Calculating Formula Constants

After [taking distance measurements](distance-calculations.md) for a specific Android device, the next step is to run a power regression to get the A, B and C constants used in the y=A*x^B+C formula.  

#### Make a spreadsheet

Create a new spreadsheet using Excel, Google Docs or similar.  You can see an example spreadsheet [here](https://docs.google.com/spreadsheets/d/1ymREowDj40tYuA5CXd4IfC4WYPXxlx5hq1x8tQcWWCI/edit?usp=sharing) that you can follow as a guide.

#### Paste Your Data

* Paste your measured iPhone 1m RSSI average into table 1.
* Paste your measured RSSI values at various distances for the Android device into table 2.

#### Step 1: Calculate Ratio

Make a new column beside your measured RSSI values at various distances.  Divide your measured RSSI at each distance by the iPhone 1m measured RSSI.


#### Step 2: Format Data for Regression

Make two new columns and copy your ratio values into the first column and the distance values into the second column.  This will align your dependent and independent varaibles for the power regression.

#### Step 3: Run the Regression

While there are many tools that can be used to run a power regression, the online website [here](http://www.xuru.org/rt/powr.asp) may be the easiest.  Just paste the two columns from the previous step into the form field, and it will output the A and B constants.

#### Step 4: Test the Prediction

Make four new columns to form table 5: RSSI, Ratio, Actual Distance, Predicted Distance, and paste for existing values into the first three.  For the fourth column, calculate the predicted distance using the formula y=A*x^B.

#### Step 4. Calculate C

The power regression assumes a zero intercept.  In order to optimize distance estimates at 1 meter, we add an intercept variable C.  To do this, subtract the actual distance from the predicted distance in table 5 above, and put this in table 6.

#### Step 5. Test the Prediction again

Make four new columns to form table 7: RSSI, Ratio, Actual Distance, Predicted Distance, and paste for existing values into the first three.  For the fourth column, calculate the predicted distance using the formula y=A*x^B+C.  This is the final formula.  

#### Step 6. Validate Results

Check the predicted distances against the actual distances to see that the formula provides a reasonable fit for the device.  You may wish to compare if this customized formula predicts distance better than the default Nexus formula with the library before submitting the constants for inclusion in the library.

#### Step 7. Submit a Pull Request

Submit a new Pull Request to the Android Beacon Library project, providing a link to your spreadsheet calculations.