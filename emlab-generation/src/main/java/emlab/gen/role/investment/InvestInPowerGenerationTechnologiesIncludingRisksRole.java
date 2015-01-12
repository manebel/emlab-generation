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
package emlab.gen.role.investment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

import agentspring.role.Role;
import emlab.gen.domain.agent.AbstractEnergyProducerConsideringRiskScenarios;
import emlab.gen.domain.agent.BigBank;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.EnergyProducerConsideringRiskScenariosSatisficer;
import emlab.gen.domain.agent.PowerPlantManufacturer;
import emlab.gen.domain.agent.StrategicReserveOperator;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.gis.Zone;
import emlab.gen.domain.market.ClearingPoint;
import emlab.gen.domain.market.electricity.ElectricitySpotMarket;
import emlab.gen.domain.market.electricity.Segment;
import emlab.gen.domain.market.electricity.SegmentLoad;
import emlab.gen.domain.policy.PowerGeneratingTechnologyTarget;
import emlab.gen.domain.technology.PowerGeneratingTechnology;
import emlab.gen.domain.technology.PowerGeneratingTechnologyNodeLimit;
import emlab.gen.domain.technology.PowerGridNode;
import emlab.gen.domain.technology.PowerPlant;
import emlab.gen.domain.technology.Substance;
import emlab.gen.domain.technology.SubstanceShareInFuelMix;
import emlab.gen.repository.Reps;
import emlab.gen.repository.StrategicReserveOperatorRepository;
import emlab.gen.util.GeometricTrendRegression;
import emlab.gen.util.GeometricTrendRegressionWithPredictionInterval;
import emlab.gen.util.MapValueComparator;
import emlab.gen.util.SimpleRegressionWithPredictionInterval;

/**
 * {@link EnergyProducer}s decide to invest in new {@link PowerPlant}
 *
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * @author JCRichstein
 */
@Configurable
@NodeEntity
public class InvestInPowerGenerationTechnologiesIncludingRisksRole<T extends EnergyProducer> extends GenericInvestmentRole<T>
implements Role<T>, NodeBacked {

    @Transient
    @Autowired
    Reps reps;

    @Transient
    @Autowired
    Neo4jTemplate template;

    @Transient
    @Autowired
    StrategicReserveOperatorRepository strategicReserveOperatorRepository;

    // market expectations
    @Transient
    Map<ElectricitySpotMarket, MarketInformation> marketInfoMap = new HashMap<ElectricitySpotMarket, MarketInformation>();
    boolean debug = true;

    @Override
    public void act(T agent) {
        boolean riskConsideration = true; // TODO?
        // DEBUG!

        long futureTimePoint = getCurrentTick() + agent.getInvestmentFutureTimeHorizon();
        // DEBUG!
        // if (Debug){
        // logger.warn("Starting act() in InvestInPowerGenerationTechnologiesRole");
        // logger.warn(agent + " is looking at timepoint " + futureTimePoint);
        // }

        // ==== Expectations ===
        Map<Substance, ? extends SimpleRegressionWithPredictionInterval> fuelPriceRegressions = createGeometricFuelPriceRegressions(agent);
        // Map<Substance, Double> expectedFuelPrices_old =
        // predictFuelPrices(agent, futureTimePoint);
        Map<Substance, Double> expectedFuelPrices = predictFuelPrices(fuelPriceRegressions, futureTimePoint);

        // DEBUG!
        // if (debug) {
        // for (Map.Entry<Substance, Double> e : expectedFuelPrices.entrySet())
        // {
        // logger.warn("Substance: " + e.getKey() + "Price old: " +
        // expectedFuelPrices_old.get(e.getKey())
        // + "Price new: " + e.getValue());
        // }
        // }

        // CO2
        // Map<ElectricitySpotMarket, Double> expectedCO2Price_old =
        // determineExpectedCO2PriceInclTax(futureTimePoint,
        // agent.getNumberOfYearsBacklookingForForecasting(), getCurrentTick());

        SimpleRegressionWithPredictionInterval co2PriceRegression = createSimpleCO2PriceRegression(agent,
                getCurrentTick());
        Map<ElectricitySpotMarket, Double> expectedCO2Price = determineExpectedCO2PriceInclTax(co2PriceRegression,
                futureTimePoint);

        // DEBUG!
        // if (debug) {
        // for (Map.Entry<ElectricitySpotMarket, Double> e :
        // expectedCO2Price.entrySet()) {
        // logger.warn("Electricity Market: " + e.getKey() + "CO2 Price old: "
        // + expectedCO2Price_old.get(e.getKey()) + " CO2 Price new: " +
        // e.getValue());
        // }
        // }

        // Demand
        Map<ElectricitySpotMarket, ? extends SimpleRegressionWithPredictionInterval> expectedDemandRegression = createGeometricDemandRegression(
                agent, getCurrentTick());
        Map<ElectricitySpotMarket, Double> expectedDemand = predictElectricityDemand(expectedDemandRegression,
                futureTimePoint);

        // Demand old (not needed)
        // Map<ElectricitySpotMarket, Double> expectedDemand_old = new
        // HashMap<ElectricitySpotMarket, Double>();
        // for (ElectricitySpotMarket elm :
        // reps.template.findAll(ElectricitySpotMarket.class)) {
        // GeometricTrendRegression gtr = new GeometricTrendRegression();
        // for (long time = getCurrentTick(); time > getCurrentTick()
        // - agent.getNumberOfYearsBacklookingForForecasting()
        // && time >= 0; time = time - 1) {
        // gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
        // }
        // expectedDemand_old.put(elm, gtr.predict(futureTimePoint));
        // }

        // DEBUG!
        // if (debug) {
        // for (Map.Entry<ElectricitySpotMarket, Double> e :
        // expectedDemand.entrySet()) {
        // logger.warn("Electricity Market: " + e.getKey() +
        // ". Expected demand old: "
        // + expectedDemand_old.get(e.getKey()) + " Expected demand new: " +
        // e.getValue());
        // }
        // }

        // Investment decision
        ElectricitySpotMarket market = agent.getInvestorMarket();
        MarketInformation marketInformation = new MarketInformation(market, expectedDemand, expectedFuelPrices,
                expectedCO2Price.get(market).doubleValue(), futureTimePoint);

        // if (debug) {
        // logger.warn(agent + " is expecting a CO2 price of " +
        // expectedCO2Price.get(market)
        // + " Euro/MWh at timepoint " + futureTimePoint + " in Market " +
        // market);
        // }

        // logger.warn("Agent {}  found the expected prices to be {}", agent,
        // marketInformation.expectedElectricityPricesPerSegment);
        //
        // logger.warn("Agent {}  found that the installed capacity in the market {} in future to be "
        // + marketInformation.capacitySum +
        // "and expectde maximum demand to be "
        // + marketInformation.maxExpectedLoad, agent, market);

        // double highestValue = Double.MIN_VALUE;

        Map<PowerGeneratingTechnology, Double> technologiesWithPositiveNPV = new HashMap<PowerGeneratingTechnology, Double>();
        PowerGeneratingTechnology bestTechnology = null;

        for (PowerGeneratingTechnology technology : reps.genericRepository.findAll(PowerGeneratingTechnology.class)) {

            PowerPlant plant = new PowerPlant();
            plant.specifyNotPersist(getCurrentTick(), agent, getNodeForZone(market.getZone()), technology);
            // if too much capacity of this technology in the pipeline (not
            // limited to the 5 years)
            double expectedInstalledCapacityOfTechnology = reps.powerPlantRepository
                    .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market, technology,
                            futureTimePoint);
            PowerGeneratingTechnologyTarget technologyTarget = reps.powerGenerationTechnologyTargetRepository
                    .findOneByTechnologyAndMarket(technology, market);
            if (technologyTarget != null) {
                double technologyTargetCapacity = technologyTarget.getTrend().getValue(futureTimePoint);
                expectedInstalledCapacityOfTechnology = (technologyTargetCapacity > expectedInstalledCapacityOfTechnology) ? technologyTargetCapacity
                        : expectedInstalledCapacityOfTechnology;
            }
            double pgtNodeLimit = Double.MAX_VALUE;
            PowerGeneratingTechnologyNodeLimit pgtLimit = reps.powerGeneratingTechnologyNodeLimitRepository
                    .findOneByTechnologyAndNode(technology, plant.getLocation());
            if (pgtLimit != null) {
                pgtNodeLimit = pgtLimit.getUpperCapacityLimit(futureTimePoint);
            }
            double expectedInstalledCapacityOfTechnologyInNode = reps.powerPlantRepository
                    .calculateCapacityOfExpectedOperationalPowerPlantsByNodeAndTechnology(plant.getLocation(),
                            technology, futureTimePoint);
            double expectedOwnedTotalCapacityInMarket = reps.powerPlantRepository
                    .calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwner(market, futureTimePoint, agent);
            double expectedOwnedCapacityInMarketOfThisTechnology = reps.powerPlantRepository
                    .calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwnerAndTechnology(market, technology,
                            futureTimePoint, agent);
            double capacityOfTechnologyInPipeline = reps.powerPlantRepository
                    .calculateCapacityOfPowerPlantsByTechnologyInPipeline(technology, getCurrentTick());
            double operationalCapacityOfTechnology = reps.powerPlantRepository
                    .calculateCapacityOfOperationalPowerPlantsByTechnology(technology, getCurrentTick());
            double capacityInPipelineInMarket = reps.powerPlantRepository
                    .calculateCapacityOfPowerPlantsByMarketInPipeline(market, getCurrentTick());

            if ((expectedInstalledCapacityOfTechnology + plant.getActualNominalCapacity())
                    / (marketInformation.maxExpectedLoad + plant.getActualNominalCapacity()) > technology
                        .getMaximumInstalledCapacityFractionInCountry()) {
                logger.warn(agent
                        + " will not invest in {} technology because there's too much of this type in the market",
                        technology);
            } else if ((expectedInstalledCapacityOfTechnologyInNode + plant.getActualNominalCapacity()) > pgtNodeLimit) {

            } else if (expectedOwnedCapacityInMarketOfThisTechnology > expectedOwnedTotalCapacityInMarket
                    * technology.getMaximumInstalledCapacityFractionPerAgent()) {
                logger.warn(agent
                        + " will not invest in {} technology because there's too much capacity planned by him",
                        technology);
            } else if (capacityInPipelineInMarket > 0.2 * marketInformation.maxExpectedLoad) {
                logger.warn("Not investing because more than 20% of demand in pipeline.");

            } else if ((capacityOfTechnologyInPipeline > 2.0 * operationalCapacityOfTechnology)
                    && capacityOfTechnologyInPipeline > 9000) { // TODO:
                // reflects that you cannot expand a technology out of zero.
                if (debug) {
                    logger.warn(agent
                            + " will not invest in {} technology because there's too much capacity in the pipeline",
                            technology);
                }
            } else if (plant.getActualInvestedCapital() * (1 - agent.getDebtRatioOfInvestments()) > agent
                    .getDownpaymentFractionOfCash() * agent.getCash()) {
                logger.warn(agent
                        + " will not invest in {} technology as he does not have enough money for downpayment",
                        technology);
            } else {

                Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                for (Substance fuel : technology.getFuels()) {
                    myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
                }
                Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices,
                        expectedCO2Price.get(market));
                plant.setFuelMix(fuelMix);

                double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices,
                        expectedCO2Price.get(market));
                double runningHours = 0d;
                double expectedGrossProfit = 0d;

                long numberOfSegments = reps.segmentRepository.count();

                // TODO somehow the prices of long-term contracts could also
                // be used here to determine the expected profit. Maybe not
                // though...
                for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {
                    double expectedElectricityPrice = marketInformation.expectedElectricityPricesPerSegment
                            .get(segmentLoad.getSegment());
                    double hours = segmentLoad.getSegment().getLengthInHours();
                    if (expectedMarginalCost <= expectedElectricityPrice) {
                        runningHours += hours;
                        expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost)
                                * hours
                                * plant.getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(),
                                        numberOfSegments);
                    }
                }

                logger.warn(agent + "expects technology {} to have {} running", technology, runningHours);
                // expect to meet minimum running hours?
                if (runningHours < plant.getTechnology().getMinimumRunningHours()) {
                    logger.warn(
                            agent
                                    + " will not invest in {} technology as he expect to have {} running, which is lower then required",
                            technology, runningHours);
                } else {

                    double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());// /
                    // plant.getActualNominalCapacity();

                    double operatingProfit = expectedGrossProfit - fixedOMCost;

                    // TODO Alter discount rate on the basis of the amount
                    // in long-term contracts?
                    // TODO Alter discount rate on the basis of other stuff,
                    // such as amount of money, market share, portfolio
                    // size.

                    // Calculation of weighted average cost of capital,
                    // based on the companies debt-ratio
                    double wacc = (1 - agent.getDebtRatioOfInvestments()) * agent.getEquityInterestRate()
                            + agent.getDebtRatioOfInvestments() * agent.getLoanInterestRate();

                    // Creation of out cash-flow during power plant building
                    // phase (note that the cash-flow is negative!)
                    TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                            technology.getDepreciationTime(), (int) plant.getActualLeadtime(),
                            plant.getActualInvestedCapital(), 0);
                    // Creation of in cashflow during operation
                    TreeMap<Integer, Double> discountedProjectCashInflow = calculateSimplePowerPlantInvestmentCashFlow(
                            technology.getDepreciationTime(), (int) plant.getActualLeadtime(), 0, operatingProfit);

                    // !! are defined negative!!
                    double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);
                    // !! are defined negative!!

                    // if (debug) {
                    // logger.warn("Agent {}  found that the discounted capital for technology {} to be "
                    // + discountedCapitalCosts, agent, technology);
                    // }

                    double discountedOpProfit = npv(discountedProjectCashInflow, wacc);

                    // logger.warn("Agent {}  found that the projected discounted inflows for technology {} to be "
                    // + discountedOpProfit, agent, technology);

                    double projectValue = discountedOpProfit + discountedCapitalCosts;

                    if (debug) {
                        logger.warn(
                                "Agent {}  found the project value for technology {} to be "
                                        + Math.round(projectValue / plant.getActualNominalCapacity())
                                        + " EUR/kW (running hours: " + runningHours + "", agent, technology);
                    }

                    // Store all projects with positive project values in a map
                    // (technologiesWithPositiveNPV)
                    // Divide project value by capacity, in order not to favor
                    // large power plants (which have the single largest NPV).

                    if (projectValue > 0) {
                        technologiesWithPositiveNPV.put(plant.getTechnology(),
                                projectValue / plant.getActualNominalCapacity());
                    }
                }

            }
        }

        // sort all technologies with positive NPV. Technology with highest
        // predicted NPV per MW will be at the end of
        // sortedTechnologiesWithPositiveNPV
        MapValueComparator comp = new MapValueComparator(technologiesWithPositiveNPV);
        TreeMap<PowerGeneratingTechnology, Double> sortedTechnologiesWithPositiveNPV = new TreeMap<PowerGeneratingTechnology, Double>(
                comp);
        sortedTechnologiesWithPositiveNPV.putAll(technologiesWithPositiveNPV);

        // If risk consideration decide on possible investments including risk
        // consideration
        if (riskConsideration && agent instanceof AbstractEnergyProducerConsideringRiskScenarios) {
            AbstractEnergyProducerConsideringRiskScenarios riskAgent = (AbstractEnergyProducerConsideringRiskScenarios) agent;
            bestTechnology = decideOnInvestmentConsideringRisk(sortedTechnologiesWithPositiveNPV, marketInformation,
                    fuelPriceRegressions, co2PriceRegression, expectedDemandRegression, riskAgent, futureTimePoint);
        } else {
            // No consideration of risk, chose technology with highest predicted
            // NPV
            if (sortedTechnologiesWithPositiveNPV.isEmpty()) {
                bestTechnology = null;
            } else {
                bestTechnology = sortedTechnologiesWithPositiveNPV.lastKey();
            }
        }

        // undertake investment
        if (bestTechnology != null) {
            logger.warn("Agent {} invested in technology {} at tick " + getCurrentTick(), agent, bestTechnology);

            PowerPlant plant = new PowerPlant();
            plant.specifyAndPersist(getCurrentTick(), agent, getNodeForZone(market.getZone()), bestTechnology);
            PowerPlantManufacturer manufacturer = reps.genericRepository.findFirst(PowerPlantManufacturer.class);
            BigBank bigbank = reps.genericRepository.findFirst(BigBank.class);

            double investmentCostPayedByEquity = plant.getActualInvestedCapital()
                    * (1 - agent.getDebtRatioOfInvestments());
            double investmentCostPayedByDebt = plant.getActualInvestedCapital() * agent.getDebtRatioOfInvestments();
            double downPayment = investmentCostPayedByEquity;
            createSpreadOutDownPayments(agent, manufacturer, downPayment, plant);

            double amount = determineLoanAnnuities(investmentCostPayedByDebt, plant.getTechnology()
                    .getDepreciationTime(), agent.getLoanInterestRate());
            // logger.warn("Loan amount is: " + amount);
            Loan loan = reps.loanRepository.createLoan(agent, bigbank, amount, plant.getTechnology()
                    .getDepreciationTime(), getCurrentTick(), plant);
            // Create the loan
            plant.createOrUpdateLoan(loan);

        } else {
            // logger.warn("{} found no suitable technology anymore to invest in at tick "
            // + getCurrentTick(), agent);
            // agent will not participate in the next round of investment if
            // he does not invest now
            setNotWillingToInvest(agent);
        }
    }

    // Creates n downpayments of equal size in each of the n building years of a
    // power plant
    @Transactional
    private void createSpreadOutDownPayments(EnergyProducer agent, PowerPlantManufacturer manufacturer,
            double totalDownPayment, PowerPlant plant) {
        int buildingTime = (int) plant.getActualLeadtime();
        reps.nonTransactionalCreateRepository.createCashFlow(agent, manufacturer, totalDownPayment / buildingTime,
                CashFlow.DOWNPAYMENT, getCurrentTick(), plant);
        Loan downpayment = reps.loanRepository.createLoan(agent, manufacturer, totalDownPayment / buildingTime,
                buildingTime - 1, getCurrentTick(), plant);
        plant.createOrUpdateDownPayment(downpayment);
    }

    @Transactional
    private void setNotWillingToInvest(EnergyProducer agent) {
        agent.setWillingToInvest(false);
    }

    /**
     * TODO
     *
     * @param projects
     * @param fuelPriceRegressions
     * @param co2PriceRegression
     * @param expectedDemandRegression
     * @return
     */
    public PowerGeneratingTechnology decideOnInvestmentConsideringRisk(
            TreeMap<PowerGeneratingTechnology, Double> projects, MarketInformation baseMarket,
            Map<Substance, ? extends SimpleRegressionWithPredictionInterval> fuelPriceRegressions,
            SimpleRegressionWithPredictionInterval co2PriceRegression,
            Map<ElectricitySpotMarket, ? extends SimpleRegressionWithPredictionInterval> expectedDemandRegression,
            AbstractEnergyProducerConsideringRiskScenarios riskAgent, long futureTimePoint) {
        if (projects.isEmpty()) {
            return null;
        }
        PowerGeneratingTechnology bestTechnology = null;
        // If agent is a satisficer over different scenarios
        if (riskAgent instanceof EnergyProducerConsideringRiskScenariosSatisficer) {
            EnergyProducerConsideringRiskScenariosSatisficer riskAgentScenarios = (EnergyProducerConsideringRiskScenariosSatisficer) riskAgent;
            // create all scenarios
            ArrayList<MarketInformation> scenarios = createScenarios(baseMarket, fuelPriceRegressions,
                    co2PriceRegression, expectedDemandRegression, riskAgentScenarios, futureTimePoint);

            NavigableSet<PowerGeneratingTechnology> keySet = projects.descendingKeySet();
            Iterator<PowerGeneratingTechnology> iterator = keySet.iterator();
            while (bestTechnology == null && iterator.hasNext()) {
                // last key is highest key in tree map
                bestTechnology = iterator.next();
                // DEBUG!
                if (debug) {
                    logger.warn("Agent {} now evaluates risk of an investment in {}", riskAgentScenarios,
                            bestTechnology);
                }
                for (MarketInformation mar : scenarios) {
                    if (bestTechnology != null) {
                        double npvInScenario;
                        double relativeNPV;
                        npvInScenario = calculateHypotheticalNPV(bestTechnology, mar, riskAgentScenarios,
                                futureTimePoint);

                        switch (riskAgentScenarios.getThresholdDefinition()) {
                        // case 1: relatively against net worth of company
                        case 1:
                            if (riskAgentScenarios.getCash() != 0) {
                                relativeNPV = npvInScenario / riskAgentScenarios.getCash();
                            } else {
                                if (npvInScenario >= 0) {
                                    relativeNPV = Double.MAX_VALUE;
                                } else {
                                    relativeNPV = Double.MIN_VALUE;
                                }
                            }
                            break;

                            // case 2: absolute
                        case 2:
                            relativeNPV = npvInScenario;
                            break;
                        default:
                            // default: Threshold is defined in absolute terms
                            relativeNPV = npvInScenario;
                            break;
                        }
                        // threshold is met
                        if (relativeNPV >= riskAgentScenarios.getThreshold()) {
                            // npv in this scenario is satisfactory
                            // DEBUG!
                            if (debug) {
                                logger.warn(
                                        "(Relative) NPV of {} in scenario " + mar.name
                                        + " is sufficient for Agent {}. Threshold: "
                                        + riskAgentScenarios.getThreshold() + ".", npvInScenario,
                                        riskAgentScenarios);
                            }
                        }
                        // threshold is not met
                        else {
                            if (debug) {
                                logger.warn(
                                        "(Relative) NPV of {} in scenario " + mar.name
                                        + " is not satisfactory for Agent {}. Threshold: "
                                        + riskAgentScenarios.getThreshold() + ".", npvInScenario,
                                        riskAgentScenarios);
                            }
                            bestTechnology = null;

                        }
                    }
                }
            }
        }

        // Insert other implementations for riskAgents here

        return bestTechnology;
    }

    /**
     * TODO
     *
     * @param technology
     * @param marketInformation
     * @param agent
     * @param futureTimePoint
     * @return
     */
    public double calculateHypotheticalNPV(PowerGeneratingTechnology technology, MarketInformation marketInformation,
            EnergyProducer agent, Long futureTimePoint) {
        // get fuel prices for specific plant
        PowerPlant plant = new PowerPlant();
        plant.specifyNotPersist(getCurrentTick(), agent, getNodeForZone(marketInformation.market.getZone()), technology);
        Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
        for (Substance fuel : technology.getFuels()) {
            myFuelPrices.put(fuel, marketInformation.fuelPrices.get(fuel));
        }
        Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices, marketInformation.co2price);
        double expectedMarginalCost = determineExpectedMarginalCost(plant, marketInformation.fuelPrices,
                marketInformation.co2price);
        double runningHours = 0d;
        double expectedGrossProfit = 0d;
        long numberOfSegments = reps.segmentRepository.count();

        for (SegmentLoad segmentLoad : marketInformation.market.getLoadDurationCurve()) {
            double expectedElectricityPrice = marketInformation.expectedElectricityPricesPerSegment.get(segmentLoad
                    .getSegment());
            double hours = segmentLoad.getSegment().getLengthInHours();
            if (expectedMarginalCost <= expectedElectricityPrice) {
                expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost) * hours
                        * plant.getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(), numberOfSegments);
            }
        }
        if (runningHours < plant.getTechnology().getMinimumRunningHours()) {
            // logger.warn(agent+
            // " will not invest in {} technology as he expect to have {} running, which is lower then required",
            // technology, runningHours);
            // TODO WHAT TO DO???????
            expectedGrossProfit = 0;
        }
        double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());
        double operatingProfit = expectedGrossProfit - fixedOMCost;
        // Calculation of weighted average cost of capital,
        // based on the companies debt-ratio
        double wacc = (1 - agent.getDebtRatioOfInvestments()) * agent.getEquityInterestRate()
                + agent.getDebtRatioOfInvestments() * agent.getLoanInterestRate();
        // Creation of out cash-flow during power plant building
        // phase (note that the cash-flow is negative!)
        TreeMap<Integer, Double> discountedProjectCapitalOutflow = calculateSimplePowerPlantInvestmentCashFlow(
                technology.getDepreciationTime(), (int) plant.getActualLeadtime(), plant.getActualInvestedCapital(), 0);

        // Creation of in cashflow during operation
        TreeMap<Integer, Double> discountedProjectCashInflow = calculateSimplePowerPlantInvestmentCashFlow(
                technology.getDepreciationTime(), (int) plant.getActualLeadtime(), 0, operatingProfit);
        ;

        // !! are defined negative!!
        double discountedCapitalCosts = npv(discountedProjectCapitalOutflow, wacc);

        double discountedOpProfit = npv(discountedProjectCashInflow, wacc);

        // add up since discountedCapitalCosts are defined negative
        double projectValue = discountedOpProfit + discountedCapitalCosts;

        // return absolute project value
        return projectValue;

    }

    /**
     * Method to create all scenarios for the risk assessment of an
     * "EnergyProducerConsideringRiskScenariosSatisficer"
     *
     * @param baseMarket
     *            MarketInformation with all predicted values (base scenario)
     * @param fuelPriceRegressions
     *            Map of regression objects for all fuel prices
     * @param co2PriceRegression
     *            regression object for the co2 price
     * @param expectedDemandRegression
     *            regression objects for the demand
     * @param riskAgent
     *            associated agent (must be of type
     *            EnergyProducerConsideringRiskScenariosSatisficer)
     * @param futureTimePoint
     *            future time point for the calculation of npv.
     * @return List with MarketInformation object, for each scenario one
     *         MarketInformation object
     */
    public ArrayList<MarketInformation> createScenarios(MarketInformation baseMarket,
            Map<Substance, ? extends SimpleRegressionWithPredictionInterval> fuelPriceRegressions,
            SimpleRegressionWithPredictionInterval co2PriceRegression,
            Map<ElectricitySpotMarket, ? extends SimpleRegressionWithPredictionInterval> expectedDemandRegression,
            EnergyProducerConsideringRiskScenariosSatisficer riskAgent, long futureTimePoint) {

        ArrayList<MarketInformation> result = new ArrayList<MarketInformation>();
        // Get data
        Map<Substance, Double> fuelIntervals = getSubstancesPriceRiskConfidenceLevels(riskAgent);
        // Check for all FUEL PRICES, if a new scenario should be created
        for (Map.Entry<Substance, ? extends SimpleRegressionWithPredictionInterval> e : fuelPriceRegressions.entrySet()) {

            // if fuel is to be included in the risk assessment, create two
            // scenarios
            if (fuelIntervals.containsKey(e.getKey())) {
                ArrayList<MarketInformation> newScenarios = alterFuelPriceInMarketInformation(baseMarket, e.getKey(),
                        e.getValue(), fuelIntervals.get(e.getKey()), futureTimePoint);
                for (MarketInformation m : newScenarios) {
                    result.add(m);
                    // Debug!
                    if (debug) {
                        logger.warn("added a scenario with different price of " + e.getKey().getName());
                    }
                }
            }
        }

        // create scenarios with different electricity DEMAND on own market
        // neglect risk consideration of markets in other zones
        if (riskAgent.isDemandRiskIncluded()) {
            ElectricitySpotMarket ownMarket = riskAgent.getInvestorMarket();
            if (expectedDemandRegression.containsKey(ownMarket)) {
                double[] minAndMaxDemand = new double[2];
                minAndMaxDemand = expectedDemandRegression.get(ownMarket).getPredictionInterval(futureTimePoint,
                        riskAgent.getDemandConfidenceLevel());
                // if everything worked out (i.e. n >= 3)
                if (!(Double.isNaN(minAndMaxDemand[0]) || Double.isNaN(minAndMaxDemand[1]))) {
                    Map<ElectricitySpotMarket, Double> tmp = new HashMap<ElectricitySpotMarket, Double>();
                    tmp.putAll(baseMarket.expectedDemand);
                    // create first scenario (min)
                    tmp.remove(ownMarket);
                    tmp.put(ownMarket, minAndMaxDemand[0]);
                    result.add(new MarketInformation(baseMarket.market, tmp, baseMarket.fuelPrices,
                            baseMarket.co2price, baseMarket.time, "Low Demand"));
                    // create second scenario (max)
                    tmp.remove(ownMarket);
                    tmp.put(ownMarket, minAndMaxDemand[1]);
                    result.add(new MarketInformation(baseMarket.market, tmp, baseMarket.fuelPrices,
                            baseMarket.co2price, baseMarket.time, "High Demand"));
                    // DEBUG!
                    if (debug) {
                        logger.warn("added two scenarios with different demand (normal forecasted value was: "
                                + baseMarket.expectedDemand.get(ownMarket).toString() + "). MinDemand: "
                                + minAndMaxDemand[0] + ". MaxDemand: " + minAndMaxDemand[1]);
                    }
                }

            }

            // create scenarios with different CO2 PRICES
            if (riskAgent.isCo2PriceRiskIncluded()) {
                double[] minAndMaxCO2Price = new double[2];
                minAndMaxCO2Price = co2PriceRegression.getPredictionInterval(futureTimePoint,
                        riskAgent.getCo2PriceConfidenceLevel());
                // if everything worked out (i.e. n >= 3)
                if (!(Double.isNaN(minAndMaxCO2Price[0]) || Double.isNaN(minAndMaxCO2Price[1]))) {
                    // add first scenario
                    result.add(new MarketInformation(baseMarket.market, baseMarket.expectedDemand,
                            baseMarket.fuelPrices, minAndMaxCO2Price[0], baseMarket.time, "Low CO2 Price"));
                    // add second scenario
                    result.add(new MarketInformation(baseMarket.market, baseMarket.expectedDemand,
                            baseMarket.fuelPrices, minAndMaxCO2Price[1], baseMarket.time, "High CO2 Price"));
                    // DEBUG!
                    if (debug) {
                        logger.warn("added two scenarios with different CO2 prices (normal forecasted value was: "
                                + baseMarket.co2price + "). Min CO2 price: " + minAndMaxCO2Price[0]
                                + ". Max CO2 price: " + minAndMaxCO2Price[1]);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Method to create scenarios by altering prices of a specific fuel,
     * according to the prediction interval of the associated regression
     *
     * @param baseMarket
     *            MarketInformation with all predicted values (base scenario)
     * @param substance
     *            Substance, that prices have to be changed
     * @param priceRegression
     *            Map of regression objects for all fuel prices
     * @param confidenceLevel
     *            Level of confidence for the prediction interval
     * @param futureTimePoint
     *            Future time point for the calculation of the prediction
     *            interval
     * @return ArrayList of two MarketInformation (for each scenarion (min &
     *         max) one object)
     */
    private ArrayList<MarketInformation> alterFuelPriceInMarketInformation(MarketInformation baseMarket,
            Substance substance, SimpleRegressionWithPredictionInterval priceRegression, double confidenceLevel,
            long futureTimePoint) {
        // TODO Testen!
        // 2 new marketInformation - one min and one max scenario
        ArrayList<MarketInformation> result = new ArrayList<MarketInformation>();
        // copy all other prices
        Map<Substance, Double> tmpPrices = new HashMap<Substance, Double>();
        tmpPrices.putAll(baseMarket.fuelPrices);
        // new prices
        double[] minAndMaxPrice = new double[2];
        minAndMaxPrice = priceRegression.getPredictionInterval(futureTimePoint, confidenceLevel);

        // if everything worked out (i.e. n >= 3)
        if (!(Double.isNaN(minAndMaxPrice[0]) || Double.isNaN(minAndMaxPrice[1]))) {
            // add first scenario (min)
            tmpPrices.put(substance, minAndMaxPrice[0]);
            // keep everything as in base scenario, only change
            // fuelprice Map
            result.add(new MarketInformation(baseMarket.market, baseMarket.expectedDemand, tmpPrices,
                    baseMarket.co2price, baseMarket.time, "Low price for " + substance.getName()));
            // add second scenario (max) (put will overwrite old entry)
            tmpPrices.put(substance, minAndMaxPrice[1]);
            result.add(new MarketInformation(baseMarket.market, baseMarket.expectedDemand, tmpPrices,
                    baseMarket.co2price, baseMarket.time, "High price for " + substance.getName()));
            // DEBUG!
            if (debug) {
                logger.warn("created two scenarios with different prices of " + substance.getName()
                        + ". Forecasted price: " + baseMarket.fuelPrices.get(substance) + ". New min price: "
                        + minAndMaxPrice[0] + ". New max price:" + minAndMaxPrice[1] + ".");
            }
        }
        return result;
    }

    /**
     * Predicts fuel prices for {@link futureTimePoint} using a geometric trend
     * regression forecast. Only predicts fuels that are traded on a commodity
     * market.
     *
     * @param agent
     * @param futureTimePoint
     * @return Map<Substance, Double> of predicted prices.
     */
    public Map<Substance, Double> predictFuelPrices(EnergyProducer agent, long futureTimePoint) {
        // Fuel Prices
        logger.warn("InvestInPowerGenerationTechnologiesRole: predictFuelPrices()");
        Map<Substance, Double> expectedFuelPrices = new HashMap<Substance, Double>();
        for (Substance substance : reps.substanceRepository.findAllSubstancesTradedOnCommodityMarkets()) {
            // Find Clearing Points for the last 5 years (counting current year
            // as one of the last 5 years).
            Iterable<ClearingPoint> cps = reps.clearingPointRepository
                    .findAllClearingPointsForSubstanceTradedOnCommodityMarkesAndTimeRange(substance, getCurrentTick()
                            - (agent.getNumberOfYearsBacklookingForForecasting() - 1), getCurrentTick(), false);
            // logger.warn("{}, {}",
            // getCurrentTick()-(agent.getNumberOfYearsBacklookingForForecasting()-1),
            // getCurrentTick());
            // Create regression object
            GeometricTrendRegression gtr = new GeometricTrendRegression();
            for (ClearingPoint clearingPoint : cps) {
                // logger.warn("CP {}: {} , in" + clearingPoint.getTime(),
                // substance.getName(), clearingPoint.getPrice());
                gtr.addData(clearingPoint.getTime(), clearingPoint.getPrice());
            }
            expectedFuelPrices.put(substance, gtr.predict(futureTimePoint));
            // logger.warn("Forecast {}: {}, in Step " + futureTimePoint,
            // substance, expectedFuelPrices.get(substance));
        }
        return expectedFuelPrices;
    }

    /**
     * TODO!
     */
    public Map<Substance, Double> predictFuelPrices(
            Map<Substance, ? extends SimpleRegressionWithPredictionInterval> regressions, long futureTimePoint) {
        // Fuel Prices
        // logger.warn("InvestInPowerGenerationTechnologiesRole: predictFuelPrices from Regressions");
        Map<Substance, Double> expectedFuelPrices = new HashMap<Substance, Double>();
        for (Map.Entry<Substance, ? extends SimpleRegression> e : regressions.entrySet()) {
            expectedFuelPrices.put(e.getKey(), e.getValue().predict(futureTimePoint));
        }
        return expectedFuelPrices;
    }

    /**
     * TODO!!
     */
    public Map<Substance, GeometricTrendRegressionWithPredictionInterval> createGeometricFuelPriceRegressions(
            EnergyProducer agent) {
        // DEBUG!
        // if (debug) {
        // logger.warn("InvestInPowerGenerationTechnologiesRole: predictFuelPrices()");
        // }

        Map<Substance, GeometricTrendRegressionWithPredictionInterval> substanceRegressions = new HashMap<Substance, GeometricTrendRegressionWithPredictionInterval>();

        for (Substance substance : reps.substanceRepository.findAllSubstancesTradedOnCommodityMarkets()) {
            // Find Clearing Points for the last 5 years (counting current year
            // as one of the last 5 years).
            Iterable<ClearingPoint> cps = reps.clearingPointRepository
                    .findAllClearingPointsForSubstanceTradedOnCommodityMarkesAndTimeRange(substance, getCurrentTick()
                            - (agent.getNumberOfYearsBacklookingForForecasting() - 1), getCurrentTick(), false);
            // logger.warn("{}, {}",
            // getCurrentTick()-(agent.getNumberOfYearsBacklookingForForecasting()-1),
            // getCurrentTick());
            // Create regression object
            GeometricTrendRegressionWithPredictionInterval gtr = new GeometricTrendRegressionWithPredictionInterval();
            for (ClearingPoint clearingPoint : cps) {
                // logger.warn("CP {}: {} , in" + clearingPoint.getTime(),
                // substance.getName(), clearingPoint.getPrice());
                gtr.addData(clearingPoint.getTime(), clearingPoint.getPrice());
            }
            substanceRegressions.put(substance, gtr);
        }
        return substanceRegressions;
    }

    /**
     * TODO
     *
     * @param agent
     * @param currentTick
     * @return
     */
    public Map<ElectricitySpotMarket, ? extends SimpleRegressionWithPredictionInterval> createGeometricDemandRegression(
            EnergyProducer agent, long currentTick) {
        Map<ElectricitySpotMarket, GeometricTrendRegressionWithPredictionInterval> demandRegressions = new HashMap<ElectricitySpotMarket, GeometricTrendRegressionWithPredictionInterval>();

        for (ElectricitySpotMarket elm : reps.template.findAll(ElectricitySpotMarket.class)) {
            GeometricTrendRegressionWithPredictionInterval gtr = new GeometricTrendRegressionWithPredictionInterval();
            for (long time = getCurrentTick(); time > getCurrentTick()
                    - agent.getNumberOfYearsBacklookingForForecasting()
                    && time >= 0; time = time - 1) {
                gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
            }
            demandRegressions.put(elm, gtr);
        }
        return demandRegressions;
    }

    /*
     * TODO
     */
    public Map<ElectricitySpotMarket, Double> predictElectricityDemand(
            Map<ElectricitySpotMarket, ? extends SimpleRegressionWithPredictionInterval> regressions,
            long futureTimePoint) {
        Map<ElectricitySpotMarket, Double> expectedElectricityDemand = new HashMap<ElectricitySpotMarket, Double>();
        for (Map.Entry<ElectricitySpotMarket, ? extends SimpleRegression> e : regressions.entrySet()) {
            expectedElectricityDemand.put(e.getKey(), e.getValue().predict(futureTimePoint));
        }
        return expectedElectricityDemand;
    }

    // Create a powerplant investment and operation cash-flow in the form of a
    // map. If only investment, or operation costs should be considered set
    // totalInvestment or operatingProfit to 0
    private TreeMap<Integer, Double> calculateSimplePowerPlantInvestmentCashFlow(int depriacationTime,
            int buildingTime, double totalInvestment, double operatingProfit) {
        TreeMap<Integer, Double> investmentCashFlow = new TreeMap<Integer, Double>();
        double equalTotalDownPaymentInstallement = totalInvestment / buildingTime;
        for (int i = 0; i < buildingTime; i++) {
            investmentCashFlow.put(new Integer(i), -equalTotalDownPaymentInstallement);
        }
        for (int i = buildingTime; i < depriacationTime + buildingTime; i++) {
            investmentCashFlow.put(new Integer(i), operatingProfit);
        }

        return investmentCashFlow;
    }

    private double npv(TreeMap<Integer, Double> netCashFlow, double wacc) {
        double npv = 0;
        for (Integer iterator : netCashFlow.keySet()) {
            npv += netCashFlow.get(iterator).doubleValue() / Math.pow(1 + wacc, iterator.intValue());
        }
        return npv;
    }

    public double determineExpectedMarginalCost(PowerPlant plant, Map<Substance, Double> expectedFuelPrices,
            double expectedCO2Price) {
        double mc = determineExpectedMarginalFuelCost(plant, expectedFuelPrices);
        double co2Intensity = plant.calculateEmissionIntensity();
        mc += co2Intensity * expectedCO2Price;
        return mc;
    }

    public double determineExpectedMarginalFuelCost(PowerPlant powerPlant, Map<Substance, Double> expectedFuelPrices) {
        double fc = 0d;
        for (SubstanceShareInFuelMix mix : powerPlant.getFuelMix()) {
            double amount = mix.getShare();
            double fuelPrice = expectedFuelPrices.get(mix.getSubstance());
            fc += amount * fuelPrice;
        }
        return fc;
    }

    private PowerGridNode getNodeForZone(Zone zone) {
        for (PowerGridNode node : reps.genericRepository.findAll(PowerGridNode.class)) {
            if (node.getZone().equals(zone)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Creates a Map with all Substances, that are marked to be included in the
     * risk assessment of a EnergyProducerConsideringRisk as keys and the
     * corresponding confidence intervals as values
     *
     * @param a
     *            EnergyProducerConsideringRisk
     * @return Map with Substances and confidence intervals
     */
    private Map<Substance, Double> getSubstancesPriceRiskConfidenceLevels(
            AbstractEnergyProducerConsideringRiskScenarios a) {
        Map<Substance, Double> result = new HashMap<Substance, Double>();

        boolean coalFound = !a.isCoalPriceRiskIncluded();
        boolean gasFound = !a.isGasPriceRiskIncluded();
        boolean ligniteFound = !a.isLignitePriceRiskIncluded();
        boolean uraniumFound = !a.isUraniumPriceRiskIncluded();
        boolean biomassFound = !a.isBiomassPriceRiskIncluded();

        for (Substance substance : reps.substanceRepository.findAllSubstancesTradedOnCommodityMarkets()) {
            if (!coalFound && substance.getName().equalsIgnoreCase("Coal")) {
                result.put(substance, a.getCoalPriceConfidenceLevel());
                coalFound = true;
            }

            if (!gasFound && substance.getName().equalsIgnoreCase("Natural Gas")) {
                result.put(substance, a.getGasPriceConfidenceLevel());
                gasFound = true;
            }

            if (!ligniteFound && substance.getName().equalsIgnoreCase("Lignite")) {
                result.put(substance, a.getLignitePriceConfidenceLevel());
                ligniteFound = true;
            }

            if (!uraniumFound && substance.getName().equalsIgnoreCase("Uranium")) {
                result.put(substance, a.getUraniumPriceConfidenceLevel());
                uraniumFound = true;
            }

            if (!biomassFound && substance.getName().equalsIgnoreCase("Biomass")) {
                result.put(substance, a.getBiomassPriceConfidenceLevel());
                biomassFound = true;
            }
        }
        if (!coalFound)
            logger.warn("Coal should be considered in the risk assessment, but it could not be found to be traded on a comodity market");
        if (!gasFound)
            logger.warn("Natural Gas should be considered in the risk assessment, but it could not be found to be traded on a comodity market");
        if (!ligniteFound)
            logger.warn("Lignite should be considered in the risk assessment, but it could not be found to be traded on a comodity market");
        if (!uraniumFound)
            logger.warn("Uranium should be considered in the risk assessment, but it could not be found to be traded on a comodity market");
        if (!biomassFound)
            logger.warn("Biomass should be considered in the risk assessment, but it could not be found to be traded on a comodity market");
        return result;
    }

    private class MarketInformation {

        Map<Segment, Double> expectedElectricityPricesPerSegment;
        double maxExpectedLoad = 0d;
        Map<PowerPlant, Double> meritOrder;
        double capacitySum;
        Map<ElectricitySpotMarket, Double> expectedDemand;
        Map<Substance, Double> fuelPrices;
        double co2price;
        ElectricitySpotMarket market;
        long time;
        String name;

        MarketInformation(ElectricitySpotMarket market, Map<ElectricitySpotMarket, Double> expectedDemand,
                Map<Substance, Double> fuelPrices, double co2price, long time) {
            this(market, expectedDemand, fuelPrices, co2price, time, "");
        }

        MarketInformation(ElectricitySpotMarket market, Map<ElectricitySpotMarket, Double> expectedDemand,
                Map<Substance, Double> fuelPrices, double co2price, long time, String name) {
            // determine expected power prices
            this.expectedDemand = expectedDemand;
            this.fuelPrices = fuelPrices;
            this.co2price = co2price;
            this.market = market;
            this.time = time;
            this.name = name;

            expectedElectricityPricesPerSegment = new HashMap<Segment, Double>();
            Map<PowerPlant, Double> marginalCostMap = new HashMap<PowerPlant, Double>();
            capacitySum = 0d;

            // get merit order for this market
            for (PowerPlant plant : reps.powerPlantRepository.findExpectedOperationalPowerPlantsInMarket(market, time)) {

                double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                marginalCostMap.put(plant, plantMarginalCost);
                capacitySum += plant.getActualNominalCapacity();
            }

            // get difference between technology target and expected operational
            // capacity
            for (PowerGeneratingTechnologyTarget pggt : reps.powerGenerationTechnologyTargetRepository
                    .findAllByMarket(market)) {
                double expectedTechnologyCapacity = reps.powerPlantRepository
                        .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market,
                                pggt.getPowerGeneratingTechnology(), time);
                double targetDifference = pggt.getTrend().getValue(time) - expectedTechnologyCapacity;
                if (targetDifference > 0) {
                    PowerPlant plant = new PowerPlant();
                    plant.specifyNotPersist(getCurrentTick(), new EnergyProducer(),
                            reps.powerGridNodeRepository.findFirstPowerGridNodeByElectricitySpotMarket(market),
                            pggt.getPowerGeneratingTechnology());
                    plant.setActualNominalCapacity(targetDifference);
                    double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                    marginalCostMap.put(plant, plantMarginalCost);
                    capacitySum += targetDifference;
                }
            }

            MapValueComparator comp = new MapValueComparator(marginalCostMap);
            meritOrder = new TreeMap<PowerPlant, Double>(comp);
            meritOrder.putAll(marginalCostMap);

            long numberOfSegments = reps.segmentRepository.count();

            double demandFactor = expectedDemand.get(market).doubleValue();

            // find expected prices per segment given merit order
            for (SegmentLoad segmentLoad : market.getLoadDurationCurve()) {

                double expectedSegmentLoad = segmentLoad.getBaseLoad() * demandFactor;

                if (expectedSegmentLoad > maxExpectedLoad) {
                    maxExpectedLoad = expectedSegmentLoad;
                }

                double segmentSupply = 0d;
                double segmentPrice = 0d;
                double totalCapacityAvailable = 0d;

                for (Entry<PowerPlant, Double> plantCost : meritOrder.entrySet()) {
                    PowerPlant plant = plantCost.getKey();
                    double plantCapacity = 0d;
                    // Determine available capacity in the future in this
                    // segment
                    plantCapacity = plant
                            .getExpectedAvailableCapacity(time, segmentLoad.getSegment(), numberOfSegments);
                    totalCapacityAvailable += plantCapacity;
                    // logger.warn("Capacity of plant " + plant.toString() +
                    // " is " +
                    // plantCapacity/plant.getActualNominalCapacity());
                    if (segmentSupply < expectedSegmentLoad) {
                        segmentSupply += plantCapacity;
                        segmentPrice = plantCost.getValue();
                    }

                }

                // logger.warn("Segment " +
                // segmentLoad.getSegment().getSegmentID() + " supply equals " +
                // segmentSupply + " and segment demand equals " +
                // expectedSegmentLoad);

                // Find strategic reserve operator for the market.
                double reservePrice = 0;
                double reserveVolume = 0;
                for (StrategicReserveOperator operator : strategicReserveOperatorRepository.findAll()) {
                    ElectricitySpotMarket market1 = reps.marketRepository.findElectricitySpotMarketForZone(operator
                            .getZone());
                    if (market.getNodeId().intValue() == market1.getNodeId().intValue()) {
                        reservePrice = operator.getReservePriceSR();
                        reserveVolume = operator.getReserveVolume();
                    }
                }

                if (segmentSupply >= expectedSegmentLoad
                        && ((totalCapacityAvailable - expectedSegmentLoad) <= (reserveVolume))) {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), reservePrice);
                    // logger.warn("Price: "+
                    // expectedElectricityPricesPerSegment);
                } else if (segmentSupply >= expectedSegmentLoad
                        && ((totalCapacityAvailable - expectedSegmentLoad) > (reserveVolume))) {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), segmentPrice);
                    // logger.warn("Price: "+
                    // expectedElectricityPricesPerSegment);
                } else {
                    expectedElectricityPricesPerSegment.put(segmentLoad.getSegment(), market.getValueOfLostLoad());
                }

            }
        }
    }

}