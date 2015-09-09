/*******************************************************************************
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICESE-2.0
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
 * Implementation of an EnergyProducerConsideringRisk that creates several
 * scenarios. An investment is only undertaken if the NPV of this project
 * exceeds a certain threshold in all scenarios.
 *
 * @author Marvin Nebel (mnebel@uni-osnabrueck.de)
 *
 */
public class EnergyProducerConsideringRiskScenariosSatisficer extends AbstractEnergyProducerConsideringRiskScenarios {

    /**
     *
     */
    @SimulationParameter(label = "Threshold to evaluate riskiness of projects")
    private double threshold;

    @SimulationParameter(label = "(1): Threshold is defined relatively agains net worth of company."
            + "(2): Threshold is defined relatively against liquidity of company "
            + "(3) Threshold is defined in absolut terms.", from = 1, to = 3)
    private int thresholdDefinition;

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getThresholdDefinition() {
        return thresholdDefinition;
    }

    public void setThresholdDefinition(int thresholdDefinition) {
        this.thresholdDefinition = thresholdDefinition;
    }

    public EnergyProducerConsideringRiskScenariosSatisficer() {
        // Auto-generated constructor stub
    }

}
