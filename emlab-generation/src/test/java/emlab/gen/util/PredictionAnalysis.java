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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author manebel
 *
 */
public class PredictionAnalysis {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // read in data
        String inputFile = "/home/marvin/emlab-generation/emlab-generation"
                + "/src/main/resources/BaseScenario/data/stochasticFuelPrices/fuelPrices-1.csv";

        String outputFile = "/home/marvin/Schreibtisch/Master/PredictionAnalyse/";
        // First year, where Prediction shall be calculated for
        int startYear = 2022;
        // Last year, where Prediction shall be calculated for
        int endYear = 2050;
        double[] confidenceLevels = { 0.6, 0.9, 0.95, 0.99, 0.999 };

        // Create data for different confidence levels
        for (int i = 0; i < confidenceLevels.length; i++) {
            createData(startYear, endYear, confidenceLevels[i], outputFile, inputFile);
        }
    }

    public static void createData(int startingYear, int endYear, double predictionInterval, String outputFile,
            String fuelFile) {
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        String[] years = new String[200];
        String[] coalMedium = new String[200];
        String[] gasMedium = new String[200];

        try {

            br = new BufferedReader(new FileReader(fuelFile));
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (i == 0) {
                    years = line.split(cvsSplitBy);
                }
                if (i == 1) {
                    coalMedium = line.split(cvsSplitBy);
                }
                if (i == 2) {
                    gasMedium = line.split(cvsSplitBy);
                }
                i++;

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        StringBuffer bufCoal = new StringBuffer();
        StringBuffer bufGas = new StringBuffer();
        bufCoal.append("year,prediction,lower,upper,real\n");
        bufGas.append("year,prediction,lower,upper,real\n");

        // FORECASTS FOR COAL PRICES
        // according to different values for numberOfYearsBackLooking and
        // futureTimePoint
        double[][] coal1 = new double[endYear - startingYear + 1][5];
        coal1 = getData(4, 6, predictionInterval, coalMedium, years, startingYear, endYear);
        double[][] coal2 = new double[endYear - startingYear + 1][5];
        coal2 = getData(4, 8, predictionInterval, coalMedium, years, startingYear, endYear);
        double[][] coal3 = new double[endYear - startingYear + 1][5];
        coal3 = getData(6, 6, predictionInterval, coalMedium, years, startingYear, endYear);
        double[][] coal4 = new double[endYear - startingYear + 1][5];
        coal4 = getData(6, 8, predictionInterval, coalMedium, years, startingYear, endYear);
        double[][] coal5 = new double[endYear - startingYear + 1][5];
        coal5 = getData(6, 8, predictionInterval, coalMedium, years, startingYear, endYear);
        // Calculating the average. There are 4 agents, that have
        // numberOfYearsBackLooking: 6 and futureTimePoint: 8
        for (int i = 0; i < endYear - startingYear + 1; i++) {
            for (int j = 0; j < 5; j++) {
                bufCoal.append(((coal1[i][j] + coal2[i][j] + coal3[i][j] + coal4[i][j]) + 4 * coal5[i][j]) / 8);
                if (j == 4)
                    bufCoal.append("\n");
                else
                    bufCoal.append(",");
            }

        }

        // FORECASTS FOR GAS PRICES
        // according to different values for numberOfYearsBackLooking and
        // futureTimePoint
        double[][] gas1 = new double[endYear - startingYear + 1][5];
        gas1 = getData(4, 6, predictionInterval, gasMedium, years, startingYear, endYear);
        double[][] gas2 = new double[endYear - startingYear + 1][5];
        gas2 = getData(4, 8, predictionInterval, gasMedium, years, startingYear, endYear);
        double[][] gas3 = new double[endYear - startingYear + 1][5];
        gas3 = getData(6, 6, predictionInterval, gasMedium, years, startingYear, endYear);
        double[][] gas4 = new double[endYear - startingYear + 1][5];
        gas4 = getData(6, 8, predictionInterval, gasMedium, years, startingYear, endYear);
        double[][] gas5 = new double[endYear - startingYear + 1][5];
        gas5 = getData(6, 8, predictionInterval, gasMedium, years, startingYear, endYear);
        for (int i = 0; i < endYear - startingYear + 1; i++) {
            for (int j = 0; j < 5; j++) {
                // Calculating the average. There are 4 agents, that have
                // numberOfYearsBackLooking: 6 and futureTimePoint: 8
                bufGas.append(((gas1[i][j] + gas2[i][j] + gas3[i][j] + gas4[i][j]) + 4 * gas5[i][j]) / 8);
                if (j == 4)
                    bufGas.append("\n");
                else
                    bufGas.append(",");
            }

        }

        try {
            FileWriter writer = new FileWriter(outputFile + "Coal" + "_" + predictionInterval + ".csv");
            writer.append(bufCoal);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter writer = new FileWriter(outputFile + "Gas" + "_" + predictionInterval + ".csv");
            writer.append(bufGas);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double[][] getData(int yearsBack, int yearsAhead, double alpha, String[] data, String[] years,
            int startYear, int endYear) {
        double[][] result = new double[endYear - startYear + 1][5];
        int currentYear = startYear - yearsAhead;
        int i = 0;
        while (currentYear + yearsAhead <= endYear) {
            int startToCollect = Math.max(1, currentYear - 2011 - yearsBack);
            SimpleRegressionWithPredictionInterval sr = new SimpleRegressionWithPredictionInterval();
            while (startToCollect <= currentYear - 2011) {
                sr.addData(Integer.parseInt(years[startToCollect]), Double.parseDouble(data[startToCollect]));
                startToCollect++;
            }

            result[i][0] = Integer.parseInt(years[currentYear - 2011 + yearsAhead]);
            result[i][1] = sr.predict(currentYear + yearsAhead);
            result[i][2] = Math.max(0, sr.getPredictionInterval(currentYear + yearsAhead, alpha)[0]);
            result[i][3] = sr.getPredictionInterval(currentYear + yearsAhead, alpha)[1];
            result[i][4] = Double.parseDouble(data[currentYear - 2011 + yearsAhead]);

            i++;
            currentYear++;
        }
        return result;
    }
}
