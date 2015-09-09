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
package emlab.gen.domain.agent;

import agentspring.simulation.SimulationParameter;

/**
 * Abstract class of EnergyProducer that consider risk when they decide about
 * their investment. In this class the simulation parameters about the type of
 * considered risks are defined as well as the confidence interval for the
 * variables under consideration.
 *
 * @author Marvin Nebel (mnebel@uni-osnabrueck.de)
 *
 */
public abstract class AbstractEnergyProducerConsideringRiskScenarios extends EnergyProducer {

    /**
     * Simulation parameters to define what risk factors are included and the
     * respective confidence intervals
     */
    @SimulationParameter(label = "Coal price risk")
    private boolean coalPriceRiskIncluded;

    @SimulationParameter(label = "Coal price conifdence level", from = 0, to = 1)
    private double coalPriceConfidenceLevel;

    @SimulationParameter(label = "Gas price risk")
    private boolean gasPriceRiskIncluded;

    @SimulationParameter(label = "Gas price conifdence level", from = 0, to = 1)
    private double gasPriceConfidenceLevel;

    @SimulationParameter(label = "Lignite price risk")
    private boolean lignitePriceRiskIncluded;

    @SimulationParameter(label = "Lignite price conifdence level", from = 0, to = 1)
    private double lignitePriceConfidenceLevel;

    @SimulationParameter(label = "Biomass price risk")
    private boolean biomassPriceRiskIncluded;

    @SimulationParameter(label = "Biomass price conifdence level", from = 0, to = 1)
    private double biomassPriceConfidenceLevel;

    @SimulationParameter(label = "Uranium price risk")
    private boolean uraniumPriceRiskIncluded;

    @SimulationParameter(label = "Uranium price conifdence level", from = 0, to = 1)
    private double uraniumPriceConfidenceLevel;

    @SimulationParameter(label = "CO2 price risk")
    private boolean co2PriceRiskIncluded;

    @SimulationParameter(label = "CO2 price conifdence level", from = 0, to = 1)
    private double co2PriceConfidenceLevel;

    @SimulationParameter(label = "Demand risk")
    private boolean demandRiskIncluded;

    @SimulationParameter(label = "Electricity demand conifdence level", from = 0, to = 1)
    private double demandConfidenceLevel;

    public double getCoalPriceConfidenceLevel() {
        return coalPriceConfidenceLevel;
    }

    public void setCoalPriceConfidenceLevel(double coalPriceConfidenceLevel) {
        this.coalPriceConfidenceLevel = coalPriceConfidenceLevel;
    }

    public double getGasPriceConfidenceLevel() {
        return gasPriceConfidenceLevel;
    }

    public void setGasPriceConfidenceLevel(double gasPriceConfidenceLevel) {
        this.gasPriceConfidenceLevel = gasPriceConfidenceLevel;
    }

    public double getLignitePriceConfidenceLevel() {
        return lignitePriceConfidenceLevel;
    }

    public void setLignitePriceConfidenceLevel(double lignitePriceConfidenceLevel) {
        this.lignitePriceConfidenceLevel = lignitePriceConfidenceLevel;
    }

    public double getBiomassPriceConfidenceLevel() {
        return biomassPriceConfidenceLevel;
    }

    public void setBiomassPriceConfidenceLevel(double biomassPriceConfidenceLevel) {
        this.biomassPriceConfidenceLevel = biomassPriceConfidenceLevel;
    }

    public double getUraniumPriceConfidenceLevel() {
        return uraniumPriceConfidenceLevel;
    }

    public void setUraniumPriceConfidenceLevel(double uraniumPriceConfidenceLevel) {
        this.uraniumPriceConfidenceLevel = uraniumPriceConfidenceLevel;
    }

    public double getCo2PriceConfidenceLevel() {
        return co2PriceConfidenceLevel;
    }

    public void setCo2PriceConfidenceLevel(double co2PriceConfidenceLevel) {
        this.co2PriceConfidenceLevel = co2PriceConfidenceLevel;
    }

    public double getDemandConfidenceLevel() {
        return demandConfidenceLevel;
    }

    public void setDemandConfidenceLevel(double demandConfidenceLevel) {
        this.demandConfidenceLevel = demandConfidenceLevel;
    }

    public boolean isCoalPriceRiskIncluded() {
        return coalPriceRiskIncluded;
    }

    public void setCoalPriceRiskIncluded(boolean coalPriceRiskIncluded) {
        this.coalPriceRiskIncluded = coalPriceRiskIncluded;
    }

    public boolean isGasPriceRiskIncluded() {
        return gasPriceRiskIncluded;
    }

    public void setGasPriceRiskIncluded(boolean gasPriceRiskIncluded) {
        this.gasPriceRiskIncluded = gasPriceRiskIncluded;
    }

    public boolean isLignitePriceRiskIncluded() {
        return lignitePriceRiskIncluded;
    }

    public void setLignitePriceRiskIncluded(boolean lignitePriceRiskIncluded) {
        this.lignitePriceRiskIncluded = lignitePriceRiskIncluded;
    }

    public boolean isBiomassPriceRiskIncluded() {
        return biomassPriceRiskIncluded;
    }

    public void setBiomassPriceRiskIncluded(boolean biomassPriceRiskIncluded) {
        this.biomassPriceRiskIncluded = biomassPriceRiskIncluded;
    }

    public boolean isUraniumPriceRiskIncluded() {
        return uraniumPriceRiskIncluded;
    }

    public void setUraniumPriceRiskIncluded(boolean uraniumPriceRiskIncluded) {
        this.uraniumPriceRiskIncluded = uraniumPriceRiskIncluded;
    }

    public boolean isCo2PriceRiskIncluded() {
        return co2PriceRiskIncluded;
    }

    public void setCo2PriceRiskIncluded(boolean co2PriceRiskIncluded) {
        this.co2PriceRiskIncluded = co2PriceRiskIncluded;
    }

    public boolean isDemandRiskIncluded() {
        return demandRiskIncluded;
    }

    public void setDemandRiskIncluded(boolean demandRiskIncluded) {
        this.demandRiskIncluded = demandRiskIncluded;
    }

}
