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

import org.apache.commons.math.distribution.TDistribution;

/**
 * Implementation of a geometric trend regression with the additional method
 * double getPredictionIntervall (double x, double alpha) that returns the
 * 1-alpha prediction interval of the predicted value of x. Since the mean of x
 * is required for this calculation, the variable xsum is introduced and
 * updated, whenever any data is added or removed.
 *
 * @author Marvin Nebel
 *
 */
public class GeometricTrendRegressionWithPredictionInterval extends SimpleRegressionWithPredictionInterval {

    /**
     *
     */
    public GeometricTrendRegressionWithPredictionInterval() {
        super();
    }

    /**
     * Constructor
     *
     * @param t
     */
    public GeometricTrendRegressionWithPredictionInterval(TDistribution t) {
        super(t);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void addData(double x, double y) {
        super.addData(x, Math.log(y));
    }

    @Override
    public void removeData(double x, double y) {
        super.removeData(x, Math.log(y));
    }

    @Override
    public void addData(double[][] data) {
        for (double[] d : data) {
            addData(d[0], d[1]);
        }
    }

    @Override
    public void removeData(double[][] data) {
        for (int i = 0; i < data.length && super.getN() > 0; i++) {
            removeData(data[i][0], data[i][1]);
        }
    }

    @Override
    public double predict(double x) {
        return Math.exp(super.predict(x));
    }

    @Override
    public double[] getPredictionInterval(double x, double alpha) {
        double[] result = new double[2];
        // three datapoints required
        if (super.getN() < 3) {
            result[0] = Double.NaN;
            result[1] = Double.NaN;
            return result;
        }
        double volatility = super.getHalfWidthOfPredictionInterval(x, alpha);
        // exp on results as this is a geometric trend regression
        // (data is added as log())
        result[0] = Math.exp(super.predict(x) - volatility);
        result[1] = Math.exp(super.predict(x) + volatility);
        return result;
    }
}
