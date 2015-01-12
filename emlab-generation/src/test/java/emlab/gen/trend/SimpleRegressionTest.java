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
package emlab.gen.trend;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.apache.commons.math.stat.regression.SimpleRegression;

import emlab.gen.util.GeometricTrendRegressionWithPredictionInterval;
import emlab.gen.util.MapValueComparator;
import emlab.gen.util.SimpleRegressionWithPredictionInterval;

/**
 * @author manebel
 *
 */
public class SimpleRegressionTest {
    public static long futureTimePoint = 2022;

    public static double[][] gasPrices = { { 2010, 15 }, { 2011, 14.2 }, { 2012, 15.6 }, { 2013, 17.1 } };
    public static double[][] coalPrices = { { 2010, 22 }, { 2011, 18.2 }, { 2012, 18.6 }, { 2013, 21.1 } };
    public static double[][] lignitePrices = { { 2010, 9.5 }, { 2011, 9.7 }, { 2012, 10.6 }, { 2013, 10.3 } };

    /**
     * @param args
     * @throws MathException
     */
    public static void main(String[] args) throws MathException {
        SimpleRegression gtr = new SimpleRegression();

        // for (int i = 0; i < 2; i++) {
        // gtr.addData(i, 2);
        // }
        gtr.addData(1, 2);
        gtr.addData(2, 5);
        gtr.addData(3, 4);
        gtr.addData(4, 6);
        double meanSqError = gtr.getMeanSquareError();
        double tValue = (new TDistributionImpl(gtr.getN() - 2)).inverseCumulativeProbability(1d - 0.05 / 2d);
        double sumSquaredDeviationsX = gtr.getXSumSquares();
        double xmean = 2.5;
        double votalitySeven = tValue * Math.sqrt(gtr.getMeanSquareError())
                * Math.sqrt(1 + 1 / gtr.getN() + Math.pow((7 - xmean), 2) / sumSquaredDeviationsX);
        // double xbar = gtr.xbar;

        System.out.println("t-Wert: " + tValue);
        System.out.println("SXY: " + Math.sqrt(meanSqError));
        System.out.println("Sum of squared deviations of X: " + sumSquaredDeviationsX);
        System.out.println("votality: " + votalitySeven);
        System.out.println("Interval: [" + (gtr.predict(7) - votalitySeven) + ", " + (gtr.predict(7) + votalitySeven)
                + "]");
        // System.out.println("xbar: " + xbar);

        System.out.println("Now starting the same measurement with SimpleRegressionWithPredictionInterval...");
        SimpleRegressionWithPredictionInterval s = new SimpleRegressionWithPredictionInterval();
        s.addData(1, 2);
        s.addData(2, 5);
        s.addData(3, 4);
        s.addData(4, 6);

        System.out.println("Intervall bei 5%%: [" + s.getPredictionInterval(7, 0.05)[0] + ", "
                + s.getPredictionInterval(7, 0.05)[1]);
        System.out.println("Intervall bei 50%: [" + s.getPredictionInterval(7, 0.5)[0] + ", "
                + s.getPredictionInterval(7, 0.5)[1]);
        System.out.println("Intervall bei 95%: [" + s.getPredictionInterval(7, 0.95)[0] + ", "
                + s.getPredictionInterval(7, 0.95)[1]);
        // System.out.println("Konfidenzintervall 95: " + volatility95);
        // System.out.println("Konfidenzintervall 99: " + volatility99);
        System.out.println("Now testing GeometricTrendRegressionWithPredictionInterval");
        GeometricTrendRegressionWithPredictionInterval gtr1 = new GeometricTrendRegressionWithPredictionInterval();
        gtr1.addData(1, 2);
        gtr1.addData(2, 5);
        gtr1.addData(3, 4);
        gtr1.addData(4, 6);
        System.out.println("Prediction for x = 7: " + gtr1.predict(7));
        System.out.println("Prediction Interval: " + gtr1.getPredictionInterval(7., 0.8)[0] + ", "
                + gtr1.getPredictionInterval(7., 0.8)[1]);

        System.out.println("Now testing the new algorithm to predict fuel prices");
        System.out.println("Old algorithm...");
        Map<String, Double> expectedFuelPrices = new HashMap<String, Double>();
        SimpleRegression sr1 = new SimpleRegression();
        SimpleRegression sr2 = new SimpleRegression();
        SimpleRegression sr3 = new SimpleRegression();
        for (int i = 0; i < 3; i++) {
            sr1.addData(gasPrices[i][0], gasPrices[i][1]);
            sr2.addData(coalPrices[i][0], coalPrices[i][1]);
            sr3.addData(lignitePrices[i][0], lignitePrices[i][1]);
        }
        expectedFuelPrices.put("gas", sr1.predict(futureTimePoint));
        expectedFuelPrices.put("coal", sr2.predict(futureTimePoint));
        expectedFuelPrices.put("lignite", sr3.predict(futureTimePoint));
        for (Map.Entry<String, Double> e : expectedFuelPrices.entrySet()) {
            System.out.print(e.getKey() + ": " + e.getValue().toString() + " ");
        }
        System.out.println("");
        System.out.println("New algorithm...");
        Map<String, SimpleRegressionWithPredictionInterval> fuelRegressions = predictFuelPrices();
        for (Map.Entry<String, SimpleRegressionWithPredictionInterval> e : fuelRegressions.entrySet()) {
            System.out.print(e.getKey() + ": " + e.getValue().predict(futureTimePoint) + " ");
        }
        System.out.println("\nMAPTEST:");

        Map<String, Double> testMap = new HashMap<String, Double>();
        testMap.put("ZZZ", 1.1);
        testMap.put("MMM", 3.4);
        testMap.put("AAA", 2.);

        MapValueComparator comp = new MapValueComparator(testMap);
        TreeMap<String, Double> sortedTestMap = new TreeMap<String, Double>(comp);
        sortedTestMap.putAll(testMap);

        System.out.println("letzter Eintrag: " + sortedTestMap.lastKey() + ", " + sortedTestMap.lastEntry().getValue()
                + ". Erster Eintrag: " + sortedTestMap.firstEntry().getKey() + ", "
                + sortedTestMap.firstEntry().getValue());
        sortedTestMap.remove(sortedTestMap.lastKey());
        System.out.println(sortedTestMap);
        System.out.println("Removed last key. New last key: " + sortedTestMap.lastKey() + ", "
                + sortedTestMap.lastEntry().getValue());
        System.out.println("Now creating a descending tree set");
        NavigableSet<String> keySet = sortedTestMap.descendingKeySet();
        Iterator<String> i = keySet.iterator();
        while (i.hasNext()) {
            String output = i.next();
            System.out.println(output);
        }

    }

    public static Map<String, SimpleRegressionWithPredictionInterval> predictFuelPrices() {
        // Fuel Prices
        Map<String, SimpleRegressionWithPredictionInterval> expectedFuelPrices = new HashMap<String, SimpleRegressionWithPredictionInterval>();
        SimpleRegressionWithPredictionInterval gtr = new SimpleRegressionWithPredictionInterval();
        for (int i = 0; i < 3; i++) {
            gtr.addData(gasPrices[i][0], gasPrices[i][1]);
        }
        expectedFuelPrices.put("gas", gtr);
        gtr = new SimpleRegressionWithPredictionInterval();

        for (int i = 0; i < 3; i++) {
            gtr.addData(coalPrices[i][0], coalPrices[i][1]);
        }
        expectedFuelPrices.put("coal", gtr);
        gtr = new SimpleRegressionWithPredictionInterval();
        for (int i = 0; i < 3; i++) {
            gtr.addData(lignitePrices[i][0], lignitePrices[i][1]);
        }
        expectedFuelPrices.put("lignite", gtr);
        // logger.warn("Forecast {}: {}, in Step " + futureTimePoint,
        // substance, expectedFuelPrices.get(substance))
        return expectedFuelPrices;
    }
}
