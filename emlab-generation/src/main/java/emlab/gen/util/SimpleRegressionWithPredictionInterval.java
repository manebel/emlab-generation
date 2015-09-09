/*******************************************************************************
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package emlab.gen.util;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.apache.commons.math.stat.regression.SimpleRegression;

/**
 * Implementation of a simpleRegression with the additional method double
 * getPredictionIntervall (double x, double alpha) that returns the 1-alpha
 * prediction interval of the predicted value of x. Since the mean of x is
 * required for this calculation, the variable xsum is introduced and updated,
 * whenever any data is added or removed.
 *
 * @author Marvin Nebel
 *
 */
public class SimpleRegressionWithPredictionInterval extends SimpleRegression {

    /**
     *
     */
    private double xsum;
    private double ysum;

    /*
     * Constructor
     */
    public SimpleRegressionWithPredictionInterval() {
        super();
        xsum = 0;
        ysum = 0;
    }

    /**
     * Constructor
     *
     * @param t
     *            t-distribution
     */
    public SimpleRegressionWithPredictionInterval(TDistribution t) {
        super(t);
        xsum = 0;
        ysum = 0;

        // TODO Auto-generated constructor stub
    }

    public double getXSum() {
        return xsum;
    }

    public double getYSum() {
        return ysum;
    }

    @Override
    public void addData(double x, double y) {
        xsum += x;
        ysum += y;
        super.addData(x, y);
    }

    @Override
    public void addData(double[][] data) {
        for (int i = 0; i < data.length; i++) {
            xsum += data[i][0];
            ysum += data[i][1];
        }
        super.addData(data);
    }

    @Override
    public void removeData(double x, double y) {
        xsum -= x;
        ysum -= y;
    }

    @Override
    public void removeData(double[][] data) {
        for (int i = 0; i < data.length; i++) {
            xsum -= data[i][0];
            ysum -= data[i][1];
        }
        super.removeData(data);
    }

    /**
     * Method that returns the half width of the prediction interval on the
     * basis of the underlying data set
     *
     * @param x
     *            the x value, where the prediction interval should be
     *            calculated for
     * @param alpha
     *            confidence level
     * @return half width of the prediction interval
     */
    protected double getHalfWidthOfPredictionInterval(double x, double alpha) {
        long n = super.getN();
        // At least three data points are required
        if (n >= 3) {
            double tvalue = 0;
            // get t-value
            try {
                tvalue = new TDistributionImpl(n - 2).inverseCumulativeProbability((alpha + 1d) / 2d);
            } catch (MathException e) {
                // This should not happen
                e.printStackTrace();
            }

            // for formula see e.g.
            // http://reliawiki.org/index.php/Simple_Linear_Regression_Analysis
            return tvalue * Math.sqrt(super.getMeanSquareError())
                    * Math.sqrt(1 + 1 / n + Math.pow(x - (xsum / n), 2) / super.getXSumSquares());
        }
        // n < 3
        else {
            return Double.NaN;
        }
    }

    /**
     * Method that returns the prediction interval to a given x-value
     *
     * @param x
     *            x-value
     * @param alpha
     *            confidence level
     * @return array of length 2. The first value is the lower bound and second
     *         value the upper bound of the prediction interval
     */
    public double[] getPredictionInterval(double x, double alpha) {
        double[] result = new double[2];
        // at least three datapoints required
        if (super.getN() < 3) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
            return result;
        }
        // take prediction and add and subtract half width of prediction
        // interval
        double volatility = getHalfWidthOfPredictionInterval(x, alpha);
        result[0] = super.predict(x) - volatility;
        result[1] = super.predict(x) + volatility;
        return result;
    }
}
