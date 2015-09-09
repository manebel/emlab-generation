/*******************************************************************************
 * Copyright 2014 the original author or authors.
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
import java.util.LinkedList;
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
import emlab.gen.domain.agent.DecarbonizationModel;
import emlab.gen.domain.agent.EnergyProducer;
import emlab.gen.domain.agent.EnergyProducerConsideringRiskScenariosSatisficer;
import emlab.gen.domain.agent.Government;
import emlab.gen.domain.agent.PowerPlantManufacturer;
import emlab.gen.domain.agent.StochasticTargetInvestor;
import emlab.gen.domain.agent.StrategicReserveOperator;
import emlab.gen.domain.agent.TargetInvestor;
import emlab.gen.domain.contract.CashFlow;
import emlab.gen.domain.contract.Loan;
import emlab.gen.domain.market.CO2Auction;
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
 * considering investment risks
 *
 * @author <a href="mailto:E.J.L.Chappin@tudelft.nl">Emile Chappin</a> @author
 *         <a href="mailto:A.Chmieliauskas@tudelft.nl">Alfredas
 *         Chmieliauskas</a>
 * @author JCRichstein
 * @author Marvin Nebel
 */
/**
 * @author manebel
 *
 * @param <T>
 */
/**
 * @author manebel
 *
 * @param <T>
 */
/**
 * @author manebel
 *
 * @param <T>
 */
@Configurable
@NodeEntity
public class InvestInPowerGenerationTechnologiesIncludingRisksRole<T extends EnergyProducer> extends
        GenericInvestmentRole<T> implements Role<T>, NodeBacked {

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
    boolean debug = false;
    boolean riskConsideration = true; // TODO - do we need this?

    @Override
    public void act(T agent) {

        long futureTimePoint = getCurrentTick() + agent.getInvestmentFutureTimeHorizon();
        // DEBUG!
        if (debug) {
            logger.warn("Starting act() in InvestInPowerGenerationTechnologiesRole");
            logger.warn(agent + " is looking at timepoint " + futureTimePoint);
        }

        // ==== Expectations ===
        Map<Substance, ? extends SimpleRegressionWithPredictionInterval> fuelPriceRegressions = createSimpleFuelPriceRegressions(agent);
        Map<Substance, Double> expectedFuelPrices = predictFuelPrices(fuelPriceRegressions, futureTimePoint);

        // CO2
        SimpleRegressionWithPredictionInterval co2PriceRegression = createSimpleCO2PriceRegression(agent,
                getCurrentTick());
        Map<ElectricitySpotMarket, Double> expectedCO2PriceFundamental = determineExpectedCO2PriceInclTaxAndFundamentalForecast(
                co2PriceRegression, futureTimePoint);

        // Fundamental Forecast or not
        // Map<ElectricitySpotMarket, Double> expectedCO2Price =
        // expectedCO2PriceNonFundamental;
        Map<ElectricitySpotMarket, Double> expectedCO2Price = expectedCO2PriceFundamental;

        // Demand
        Map<ElectricitySpotMarket, ? extends SimpleRegressionWithPredictionInterval> expectedDemandRegression = createGeometricDemandRegression(
                agent, getCurrentTick());
        Map<ElectricitySpotMarket, Double> expectedDemand = predictElectricityDemand(expectedDemandRegression,
                futureTimePoint);

        // Investment decision
        ElectricitySpotMarket market = agent.getInvestorMarket();
        MarketInformation marketInformation = new MarketInformation(market, expectedDemand, expectedFuelPrices,
                expectedCO2Price.get(market).doubleValue(), futureTimePoint);

        // DEBUG
        if (debug) {
            logger.warn(agent + " is expecting a CO2 price of " + expectedCO2Price.get(market)
                    + " Euro/MWh at timepoint " + futureTimePoint + " in Market " + market);
        }

        if (debug) {
            logger.warn("Agent {}  found the expected prices to be {}", agent,
                    marketInformation.expectedElectricityPricesPerSegment);
        }

        // logger.warn("Agent {}  found that the installed capacity in the market {} in future to be "
        // + marketInformation.capacitySum +
        // "and expectde maximum demand to be "
        // + marketInformation.maxExpectedLoad, agent, market);

        // double highestValue = Double.MIN_VALUE;

        // Map to save all technologies with positive NPV
        Map<PowerGeneratingTechnology, Double> technologiesWithPositiveNPV = new HashMap<PowerGeneratingTechnology, Double>();
        PowerGeneratingTechnology bestTechnology = null;

        // Map to save the nodes and technology combinations that are most
        // profitable
        Map<PowerGeneratingTechnology, PowerGridNode> bestNodes = new HashMap<PowerGeneratingTechnology, PowerGridNode>();

        // Test all technologies
        for (PowerGeneratingTechnology technology : reps.genericRepository.findAll(PowerGeneratingTechnology.class)) {

            DecarbonizationModel model = reps.genericRepository.findAll(DecarbonizationModel.class).iterator().next();

            if (technology.isIntermittent() && model.isNoPrivateIntermittentRESInvestment())
                continue;

            Iterable<PowerGridNode> possibleInstallationNodes;

            /*
             * For dispatchable technologies just choose a random node. For
             * intermittent evaluate all possibilities.
             */
            if (technology.isIntermittent())
                possibleInstallationNodes = reps.powerGridNodeRepository.findAllPowerGridNodesByZone(market.getZone());
            else {
                possibleInstallationNodes = new LinkedList<PowerGridNode>();
                ((LinkedList<PowerGridNode>) possibleInstallationNodes).add(reps.powerGridNodeRepository
                        .findAllPowerGridNodesByZone(market.getZone()).iterator().next());
            }

            // logger.warn("Calculating for " + technology.getName() +
            // ", for Nodes: "
            // + possibleInstallationNodes.toString());

            for (PowerGridNode node : possibleInstallationNodes) {

                // create hypothetical power plant
                PowerPlant plant = new PowerPlant();
                plant.specifyNotPersist(getCurrentTick(), agent, node, technology);

                // Get several variables concerning the current share of this
                // technology
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
                        .calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwner(market, futureTimePoint,
                                agent);
                double expectedOwnedCapacityInMarketOfThisTechnology = reps.powerPlantRepository
                        .calculateCapacityOfExpectedOperationalPowerPlantsInMarketByOwnerAndTechnology(market,
                                technology, futureTimePoint, agent);
                double capacityOfTechnologyInPipeline = reps.powerPlantRepository
                        .calculateCapacityOfPowerPlantsByTechnologyInPipeline(technology, getCurrentTick());
                double operationalCapacityOfTechnology = reps.powerPlantRepository
                        .calculateCapacityOfOperationalPowerPlantsByTechnology(technology, getCurrentTick());
                double capacityInPipelineInMarket = reps.powerPlantRepository
                        .calculateCapacityOfPowerPlantsByMarketInPipeline(market, getCurrentTick());

                // If too much capacity of this technology in the market
                if ((expectedInstalledCapacityOfTechnology + plant.getActualNominalCapacity())
                        / (marketInformation.maxExpectedLoad + plant.getActualNominalCapacity()) > technology
                            .getMaximumInstalledCapacityFractionInCountry()) {
                    // logger.warn(agent +
                    // " will not invest in {} technology because there's too much of this type in the market",
                    // technology);
                }
                // If limit of technology within the node is reached
                else if ((expectedInstalledCapacityOfTechnologyInNode + plant.getActualNominalCapacity()) > pgtNodeLimit) {

                }
                // If too much capacity of this technology is planned by the
                // current agent
                else if (expectedOwnedCapacityInMarketOfThisTechnology > expectedOwnedTotalCapacityInMarket
                        * technology.getMaximumInstalledCapacityFractionPerAgent()) {
                    // logger.warn(agent +
                    // " will not invest in {} technology because there's too much capacity planned by him",
                    // technology);
                    // If capacity in pipeline is higher than 20%
                } else if (capacityInPipelineInMarket > 0.2 * marketInformation.maxExpectedLoad) {
                    // logger.warn("Not investing because more than 20% of demand in pipeline.");

                }
                // If too much capaity of this technology expands out of zero
                else if ((capacityOfTechnologyInPipeline > 2.0 * operationalCapacityOfTechnology)
                        && capacityOfTechnologyInPipeline > 9000) { // TODO:
                    // reflects that you cannot expand a technology out of zero.
                    // logger.warn(agent +
                    // " will not invest in {} technology because there's too much capacity in the pipeline",
                    // technology);
                }
                // If agent has not enough money for downpayment
                else if (plant.getActualInvestedCapital() * (1 - agent.getDebtRatioOfInvestments()) > agent
                        .getDownpaymentFractionOfCash() * agent.getCash()) {
                    // logger.warn(agent +
                    // " will not invest in {} technology as he does not have enough money for downpayment",
                    // technology);
                }
                // Calculate NPV of the investment
                else {

                    // Fuel prices
                    Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
                    for (Substance fuel : technology.getFuels()) {
                        myFuelPrices.put(fuel, expectedFuelPrices.get(fuel));
                    }
                    // Fuel mix of plant
                    Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices,
                            expectedCO2Price.get(market));
                    plant.setFuelMix(fuelMix);

                    // Calculate marginal costs
                    double expectedMarginalCost = determineExpectedMarginalCost(plant, expectedFuelPrices,
                            expectedCO2Price.get(market));
                    double runningHours = 0d;
                    double expectedGrossProfit = 0d;

                    long numberOfSegments = reps.segmentRepository.count();

                    // TODO somehow the prices of long-term contracts could also
                    // be used here to determine the expected profit. Maybe not
                    // though...
                    // Check all segments of the load duration curve as well as
                    // the
                    // corresponding electricity price in order to
                    // find out whether the power plant would operate in the
                    // segment
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

                    // expect to meet minimum running hours?
                    if (runningHours < plant.getTechnology().getMinimumRunningHours()) {
                        // logger.warn(agent+
                        // " will not invest in {} technology as he expect to have {} running, which is lower then required",
                        // technology, runningHours);
                    } else {

                        // Get Fixed Costs
                        double fixedOMCost = calculateFixedOperatingCost(plant, getCurrentTick());// /
                        // plant.getActualNominalCapacity();

                        // Get operating profit
                        double operatingProfit = expectedGrossProfit - fixedOMCost;

                        // TODO Alter discount rate on the basis of the amount
                        // in long-term contracts?
                        // TODO Alter discount rate on the basis of other stuff,
                        // such as amount of money, market share, portfolio
                        // size.

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

                        // Project value
                        double projectValue = discountedOpProfit + discountedCapitalCosts;

                        if (debug) {
                            logger.warn(
                                    "Agent {}  found the project value for technology {} to be "
                                            + Math.round(projectValue / plant.getActualNominalCapacity())
                                            + " EUR/kW (running hours: " + runningHours + ").", agent, technology);
                        }

                        // Store all projects with positive project values in a
                        // map
                        // (technologiesWithPositiveNPV)
                        // Divide project value by capacity
                        if (projectValue > 0) {
                            // look if technology already exists
                            if (technologiesWithPositiveNPV.containsKey(plant.getTechnology())) {
                                // only add it to the list, if project value is
                                // greater than
                                if (projectValue / plant.getActualNominalCapacity() > technologiesWithPositiveNPV
                                        .get(plant.getTechnology())) {
                                    // remove old entry
                                    technologiesWithPositiveNPV.remove(plant.getTechnology());
                                    bestNodes.remove(plant.getTechnology());

                                    // add new entry
                                    technologiesWithPositiveNPV.put(plant.getTechnology(),
                                            projectValue / plant.getActualNominalCapacity());
                                    bestNodes.put(plant.getTechnology(), node);
                                }

                            }
                            // if technology does not yet exist in the map
                            else {
                                technologiesWithPositiveNPV.put(plant.getTechnology(),
                                        projectValue / plant.getActualNominalCapacity());
                                bestNodes.put(plant.getTechnology(), node);
                            }
                        }
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
            bestTechnology = decideOnInvestmentConsideringRisk(sortedTechnologiesWithPositiveNPV, bestNodes,
                    marketInformation, fuelPriceRegressions, co2PriceRegression, expectedDemandRegression, riskAgent,
                    futureTimePoint);
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
            if (debug) {
                logger.warn("Agent {} invested in technology {} at tick " + getCurrentTick(), agent, bestTechnology);
            }
            PowerPlant plant = new PowerPlant();
            plant.specifyAndPersist(getCurrentTick(), agent, bestNodes.get(bestTechnology), bestTechnology);
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

        }
        // no investment? -> Set willingness to invest to false
        else {
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
     * Method to decide on an Investment considering risk
     *
     * @param projects
     *            all possible projects with positive NPV
     * @param fuelPriceRegressions
     *            the regression objects for fuel prices
     * @param co2PriceRegression
     *            the regression objects for co2 prices
     * @param expectedDemandRegression
     *            the regression objects for electricity demand
     *
     * @return the technology to invest in (null, in case no investment is
     *         undertaken)
     */
    public PowerGeneratingTechnology decideOnInvestmentConsideringRisk(
            TreeMap<PowerGeneratingTechnology, Double> projects, Map<PowerGeneratingTechnology, PowerGridNode> nodes,
            MarketInformation baseMarket,
            Map<Substance, ? extends SimpleRegressionWithPredictionInterval> fuelPriceRegressions,
            SimpleRegressionWithPredictionInterval co2PriceRegression,
            Map<ElectricitySpotMarket, ? extends SimpleRegressionWithPredictionInterval> expectedDemandRegression,
            AbstractEnergyProducerConsideringRiskScenarios riskAgent, long futureTimePoint) {
        // If no projects with positive NPV
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
                // Check all scenarios
                for (MarketInformation mar : scenarios) {
                    if (bestTechnology != null) {
                        double npvInScenario;
                        double relativeNPV;
                        // Calculate NPV in scenario
                        npvInScenario = calculateHypotheticalNPVPerCapacity(bestTechnology, nodes.get(bestTechnology),
                                mar, riskAgentScenarios, futureTimePoint);

                        // get threshold
                        switch (riskAgentScenarios.getThresholdDefinition()) {
                        // case 1: relatively against net worth of company
                        case 1:
                            double equity = reps.energyProducerRepository.calculateEquityOfEnergyProducer(
                                    riskAgentScenarios, getCurrentTick());
                            // Makes somehow only sense for positive equite
                            // values...
                            if (equity > 0) {
                                relativeNPV = npvInScenario / equity;
                            } else {
                                if (npvInScenario >= 0) {
                                    relativeNPV = Double.MAX_VALUE;
                                } else {
                                    relativeNPV = Double.MIN_VALUE;
                                }
                            }
                            break;

                        // case 2: relative against liquidity
                        case 2:
                            // only if liquidity > 0
                            if (riskAgentScenarios.getCash() > 0)
                                relativeNPV = npvInScenario / riskAgentScenarios.getCash();
                            else if (npvInScenario >= 0) {
                                relativeNPV = Double.MAX_VALUE;
                            } else {
                                relativeNPV = Double.MIN_VALUE;
                            }
                            break;
                        // case 3: absolute
                        case 3:
                            relativeNPV = npvInScenario;
                        default:
                            // default: Threshold is defined in absolute terms
                            relativeNPV = npvInScenario;
                            break;
                        }
                        // if threshold is met
                        if (relativeNPV >= riskAgentScenarios.getThreshold()) {
                            // npv in this scenario is satisfactory
                            // DEBUG!
                            if (debug) {
                                logger.warn(
                                        "(Relative) NPV of {} in scenario " + mar.name
                                                + " is sufficient for Agent {}. Threshold: "
                                                + riskAgentScenarios.getThreshold() + ".", Math.round(npvInScenario),
                                        riskAgentScenarios);
                                // logger.warn("Fuel prices are: " +
                                // mar.fuelPrices.toString());
                            }
                        }
                        // if threshold is not met
                        else {
                            if (debug) {
                                logger.warn(
                                        "(Relative) NPV of {} in scenario " + mar.name
                                                + " is not satisfactory for Agent {}. Threshold: "
                                                + riskAgentScenarios.getThreshold() + ".", Math.round(npvInScenario),
                                        riskAgentScenarios);
                                // logger.warn("Fuel prices are: " +
                                // mar.fuelPrices.toString());
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
     * Calculates hypthetical NPV for an investment in a certain technology
     *
     * @param technology
     *            the technology that is to be evaluated
     * @param marketInformation
     *            contains the corresponding market information
     * @param agent
     *            the investor
     * @param futureTimePoint
     *            the future time point that seves as the base year
     *
     * @return NPV/capacity
     */
    public double calculateHypotheticalNPVPerCapacity(PowerGeneratingTechnology technology, PowerGridNode node,
            MarketInformation marketInformation, EnergyProducer agent, Long futureTimePoint) {
        // get fuel prices for specific plant
        PowerPlant plant = new PowerPlant();
        plant.specifyNotPersist(getCurrentTick(), agent, node, technology);
        Map<Substance, Double> myFuelPrices = new HashMap<Substance, Double>();
        for (Substance fuel : technology.getFuels()) {
            myFuelPrices.put(fuel, marketInformation.fuelPrices.get(fuel));
        }
        // DEBUG!
        // if (debug) {
        // logger.warn("Hypothetical fuel prices of the plant are " +
        // myFuelPrices.toString());
        // }
        // calculate fuel mix
        Set<SubstanceShareInFuelMix> fuelMix = calculateFuelMix(plant, myFuelPrices, marketInformation.co2price);
        // DEBUG!
        // if (debug) {
        // logger.warn("Hypothetical fuel mix of the plant is " +
        // fuelMix.toString());
        // }
        plant.setFuelMix(fuelMix);

        // calculate marginal costs
        double expectedMarginalCost = determineExpectedMarginalCost(plant, marketInformation.fuelPrices,
                marketInformation.co2price);

        double runningHours = 0d;
        double expectedGrossProfit = 0d;
        long numberOfSegments = reps.segmentRepository.count();

        // Debug!
        double averagePrice = 0d;
        double maxPrice = Double.MIN_VALUE;
        double minPrice = Double.MAX_VALUE;
        int n = 0;

        // go through all segments of the LDC
        for (SegmentLoad segmentLoad : marketInformation.market.getLoadDurationCurve()) {
            double expectedElectricityPrice = marketInformation.expectedElectricityPricesPerSegment.get(segmentLoad
                    .getSegment());

            // DEBUG!
            if (expectedElectricityPrice < minPrice)
                minPrice = expectedElectricityPrice;
            if (expectedElectricityPrice > maxPrice)
                maxPrice = expectedElectricityPrice;
            averagePrice += expectedElectricityPrice;
            n++;

            double hours = segmentLoad.getSegment().getLengthInHours();
            // if power plant has lower variable costs than expected electricity
            // price
            if (expectedMarginalCost <= expectedElectricityPrice) {
                // update running hours
                runningHours += hours;
                // update gross profit
                if (technology.isIntermittent())
                    expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost)
                            * hours
                            * plant.getActualNominalCapacity()
                            * reps.intermittentTechnologyNodeLoadFactorRepository
                                    .findIntermittentTechnologyNodeLoadFactorForNodeAndTechnology(node, technology)
                                    .getLoadFactorForSegment(segmentLoad.getSegment());
                else
                    expectedGrossProfit += (expectedElectricityPrice - expectedMarginalCost) * hours
                            * plant.getAvailableCapacity(futureTimePoint, segmentLoad.getSegment(), numberOfSegments);
            }
        }

        // DEBUG
        // if (debug) {
        // logger.warn("Hypothetical minimum electricity price is: " +
        // Math.round(minPrice) + ", average price is "
        // + Math.round(averagePrice / n) + ", maximum price is " +
        // Math.round(maxPrice));
        // }

        // if running hours do not meet required level, set profit to 0
        if (runningHours < plant.getTechnology().getMinimumRunningHours()) {
            // logger.warn(agent+
            // " will not invest in {} technology as he expect to have {} running, which is lower then required",
            // technology, runningHours);
            // DEBUG!
            if (debug) {
                logger.warn("Hypothetical GrossProfit is 0 because runningHours are " + runningHours);
            }
            expectedGrossProfit = 0;
        }

        // operating profit = gross profit - OM costs
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
        // DEBUG!
        // if (debug) { // DEBUG!
        // logger.warn(
        // "Hypothetical expected marginal costs:" +
        // Math.round(expectedMarginalCost) + "\nFixed OM cost: "
        // + Math.round(fixedOMCost) + "\nGrossProfits: " +
        // Math.round(expectedGrossProfit)
        // + "\nDiscountedCapitalCosts: " + Math.round(discountedCapitalCosts),
        // "\nDiscountedOpProfit: " + Math.round(discountedOpProfit));
        // }
        // add up since discountedCapitalCosts are defined negative
        double projectValue = discountedOpProfit + discountedCapitalCosts;

        // return project value divided by capacity
        return projectValue / plant.getActualNominalCapacity();

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
                    // if (debug) {
                    // logger.warn("added a scenario with different price of " +
                    // e.getKey().getName() + ". Price is "
                    // + m.fuelPrices.get(e.getKey()));
                    // }
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
                    minAndMaxDemand[0] = Math.max(0, minAndMaxDemand[0]);
                    minAndMaxDemand[1] = Math.max(0, minAndMaxDemand[1]);
                    Map<ElectricitySpotMarket, Double> tmp = new HashMap<ElectricitySpotMarket, Double>();
                    tmp.putAll(baseMarket.expectedDemand);
                    // create first scenario (min)
                    tmp.remove(ownMarket);
                    tmp.put(ownMarket, minAndMaxDemand[0]);
                    result.add(new MarketInformation(baseMarket.market, tmp, baseMarket.fuelPrices,
                            baseMarket.co2price, baseMarket.time, "Low Demand"));
                    // create second scenario (max)
                    // tmp.remove(ownMarket);
                    // tmp.put(ownMarket, minAndMaxDemand[1]);
                    // result.add(new MarketInformation(baseMarket.market, tmp,
                    // baseMarket.fuelPrices,
                    // baseMarket.co2price, baseMarket.time, "High Demand"));
                    // DEBUG!
                    if (debug) {
                        logger.warn("Added ONE scenario with different demand (normal forecasted value was: "
                                + baseMarket.expectedDemand.get(ownMarket).toString() + "). MinDemand: "
                                + minAndMaxDemand[0] + ". MaxDemand was nat added, since it is not needed: "
                                + minAndMaxDemand[1]);
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
                    minAndMaxCO2Price[0] = Math.max(0, minAndMaxCO2Price[0]);
                    minAndMaxCO2Price[1] = Math.max(0, minAndMaxCO2Price[1]);
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
     * @return ArrayList of two MarketInformation (for each scenario (min & max)
     *         one object)
     */
    private ArrayList<MarketInformation> alterFuelPriceInMarketInformation(MarketInformation baseMarket,
            Substance substance, SimpleRegressionWithPredictionInterval priceRegression, double confidenceLevel,
            long futureTimePoint) {
        // Two new marketInformation - one min and one max scenario
        ArrayList<MarketInformation> result = new ArrayList<MarketInformation>();
        // copy all other prices
        Map<Substance, Double> lowPrices = new HashMap<Substance, Double>();
        Map<Substance, Double> highPrices = new HashMap<Substance, Double>();
        lowPrices.putAll(baseMarket.fuelPrices);
        highPrices.putAll(baseMarket.fuelPrices);
        // new prices
        double[] minAndMaxPrice = new double[2];
        minAndMaxPrice = priceRegression.getPredictionInterval(futureTimePoint, confidenceLevel);

        // if everything worked out (i.e. n >= 3)
        if (!(Double.isNaN(minAndMaxPrice[0]) || Double.isNaN(minAndMaxPrice[1]))) {
            minAndMaxPrice[0] = Math.max(0, minAndMaxPrice[0]);
            minAndMaxPrice[1] = Math.max(0, minAndMaxPrice[1]);
            // add first scenario (min)
            lowPrices.remove(substance);
            lowPrices.put(substance, minAndMaxPrice[0]);
            // keep everything as in base scenario, only change
            // fuelprice Map
            result.add(new MarketInformation(baseMarket.market, baseMarket.expectedDemand, lowPrices,
                    baseMarket.co2price, baseMarket.time, "Low price for " + substance.getName()));
            // add second scenario (max)
            highPrices.remove(substance);
            highPrices.put(substance, minAndMaxPrice[1]);
            result.add(new MarketInformation(baseMarket.market, baseMarket.expectedDemand, highPrices,
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
     * Method that predicts fuel prices
     *
     * @param regressions
     *            Regression objects
     * @param futureTimePoint
     *            future time point that serves as the base year
     * @return map with the expected fuel prices
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
     * Method to create a geometric trend regression object based on past market
     * results
     *
     * @param agent
     *            agent to create the regression object for
     * @return geometric trend regression object
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
     * Method to create a simple regression object based on past market results
     *
     * @param agent
     *            energy producer to create the regression object for
     * @return simple regression object
     */
    public Map<Substance, SimpleRegressionWithPredictionInterval> createSimpleFuelPriceRegressions(EnergyProducer agent) {
        // DEBUG!
        // if (debug) {
        // logger.warn("InvestInPowerGenerationTechnologiesRole: predictFuelPrices()");
        // }

        Map<Substance, SimpleRegressionWithPredictionInterval> substanceRegressions = new HashMap<Substance, SimpleRegressionWithPredictionInterval>();

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
            SimpleRegressionWithPredictionInterval sr = new SimpleRegressionWithPredictionInterval();
            for (ClearingPoint clearingPoint : cps) {
                // logger.warn("CP {}: {} , in" + clearingPoint.getTime(),
                // substance.getName(), clearingPoint.getPrice());
                sr.addData(clearingPoint.getTime(), clearingPoint.getPrice());
            }
            substanceRegressions.put(substance, sr);
        }
        return substanceRegressions;
    }

    /**
     * Method that determines the expected CO2 price (incl. tax)
     *
     * @param regression
     *            regression object of the co2 price
     * @param futureTimePoint
     *            future time point (base year)
     * @return co2 price
     */
    protected HashMap<ElectricitySpotMarket, Double> determineExpectedCO2PriceInclTax(
            SimpleRegressionWithPredictionInterval regression, long futureTimePoint) {

        HashMap<ElectricitySpotMarket, Double> co2Prices = new HashMap<ElectricitySpotMarket, Double>();
        Government government = reps.template.findAll(Government.class).iterator().next();
        double expectedCO2Price;
        // price should be between 0 and the upper limit
        if (regression.getN() > 1) {
            expectedCO2Price = regression.predict(futureTimePoint);
            expectedCO2Price = Math.max(0, expectedCO2Price);
            expectedCO2Price = Math.min(expectedCO2Price, government.getCo2Penalty(futureTimePoint));
        } else {
            expectedCO2Price = regression.getYSum();
        }
        // Calculate average of regression and past average:
        expectedCO2Price = (expectedCO2Price + regression.getYSum() / regression.getN()) / 2;

        // check in all markets and add possible taxes
        for (ElectricitySpotMarket esm : reps.marketRepository.findAllElectricitySpotMarkets()) {
            double nationalCo2MinPriceinFutureTick = reps.nationalGovernmentRepository
                    .findNationalGovernmentByElectricitySpotMarket(esm).getMinNationalCo2PriceTrend()
                    .getValue(futureTimePoint);
            double co2PriceInCountry = 0d;
            if (expectedCO2Price > nationalCo2MinPriceinFutureTick) {
                co2PriceInCountry = expectedCO2Price;
            } else {
                co2PriceInCountry = nationalCo2MinPriceinFutureTick;
            }
            co2PriceInCountry += reps.genericRepository.findFirst(Government.class).getCO2Tax(futureTimePoint);
            co2Prices.put(esm, Double.valueOf(co2PriceInCountry));
        }
        return co2Prices;
    }

    /**
     * Method that determines the expected CO2 price (incl. tax) - fundamental
     * forecast
     *
     * @param regression
     *            regression object of the co2 price
     * @param futureTimePoint
     *            future time point (base year)
     * @return co2 price
     */
    protected HashMap<ElectricitySpotMarket, Double> determineExpectedCO2PriceInclTaxAndFundamentalForecast(
            SimpleRegressionWithPredictionInterval regression, long futureTimePoint) {

        HashMap<ElectricitySpotMarket, Double> co2Prices = new HashMap<ElectricitySpotMarket, Double>();
        CO2Auction co2Auction = reps.genericRepository.findFirst(CO2Auction.class);
        Government government = reps.template.findAll(Government.class).iterator().next();
        double expectedRegressionCO2Price;
        double expectedCO2Price;
        if (regression.getN() > 1) {
            expectedRegressionCO2Price = regression.predict(futureTimePoint);
            expectedRegressionCO2Price = Math.max(0, expectedRegressionCO2Price);
            expectedRegressionCO2Price = Math
                    .min(expectedRegressionCO2Price, government.getCo2Penalty(futureTimePoint));
        } else {
            expectedRegressionCO2Price = regression.getYSum();
        }
        // Calculate average of regression and past average:
        expectedRegressionCO2Price = (expectedRegressionCO2Price + regression.getYSum() / regression.getN()) / 2;

        // evaluate expected clearing point for this tick
        ClearingPoint expectedCO2ClearingPoint = reps.clearingPointRepository.findClearingPointForMarketAndTime(
                co2Auction, getCurrentTick()
                        + reps.genericRepository.findFirst(DecarbonizationModel.class).getCentralForecastingYear(),
                true);
        expectedCO2Price = (expectedCO2ClearingPoint == null) ? 0 : expectedCO2ClearingPoint.getPrice();
        expectedCO2Price = (expectedCO2Price + expectedRegressionCO2Price) / 2;
        for (ElectricitySpotMarket esm : reps.marketRepository.findAllElectricitySpotMarkets()) {
            double nationalCo2MinPriceinFutureTick = reps.nationalGovernmentRepository
                    .findNationalGovernmentByElectricitySpotMarket(esm).getMinNationalCo2PriceTrend()
                    .getValue(futureTimePoint);
            double co2PriceInCountry = 0d;
            if (expectedCO2Price > nationalCo2MinPriceinFutureTick) {
                co2PriceInCountry = expectedCO2Price;
            } else {
                co2PriceInCountry = nationalCo2MinPriceinFutureTick;
            }
            co2PriceInCountry += reps.genericRepository.findFirst(Government.class).getCO2Tax(futureTimePoint);
            co2Prices.put(esm, Double.valueOf(co2PriceInCountry));
        }
        return co2Prices;
    }

    /**
     * Creates a gemoetric trend regression object for the electricity demand
     *
     * @param agent
     *            energy producer
     * @param currentTick
     *            current Tick
     * @return geometric trend regression object
     */
    public Map<ElectricitySpotMarket, ? extends SimpleRegressionWithPredictionInterval> createGeometricDemandRegression(
            EnergyProducer agent, long currentTick) {
        Map<ElectricitySpotMarket, GeometricTrendRegressionWithPredictionInterval> demandRegressions = new HashMap<ElectricitySpotMarket, GeometricTrendRegressionWithPredictionInterval>();

        // All electricity markets
        for (ElectricitySpotMarket elm : reps.template.findAll(ElectricitySpotMarket.class)) {
            GeometricTrendRegressionWithPredictionInterval gtr = new GeometricTrendRegressionWithPredictionInterval();
            // all timepoints that are within time horizon
            for (long time = getCurrentTick(); time > getCurrentTick()
                    - agent.getNumberOfYearsBacklookingForForecasting()
                    && time >= 0; time = time - 1) {
                gtr.addData(time, elm.getDemandGrowthTrend().getValue(time));
            }
            // add data point
            demandRegressions.put(elm, gtr);
        }
        return demandRegressions;
    }

    /**
     * Predicts future electricity demand for each market in base year
     *
     * @param regressions
     *            regression object on which prediction is based
     * @param futureTimePoint
     *            base year
     * @return prediction for each market
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

    // calculates NPV
    private double npv(TreeMap<Integer, Double> netCashFlow, double wacc) {
        double npv = 0;
        for (Integer iterator : netCashFlow.keySet()) {
            npv += netCashFlow.get(iterator).doubleValue() / Math.pow(1 + wacc, iterator.intValue());
        }
        return npv;
    }

    // determines marginal costs
    public double determineExpectedMarginalCost(PowerPlant plant, Map<Substance, Double> expectedFuelPrices,
            double expectedCO2Price) {
        double mc = determineExpectedMarginalFuelCost(plant, expectedFuelPrices);
        double co2Intensity = plant.calculateEmissionIntensity();
        mc += co2Intensity * expectedCO2Price;
        return mc;
    }

    // determines marginal fuel costs
    public double determineExpectedMarginalFuelCost(PowerPlant powerPlant, Map<Substance, Double> expectedFuelPrices) {
        double fc = 0d;
        for (SubstanceShareInFuelMix mix : powerPlant.getFuelMix()) {
            double amount = mix.getShare();
            double fuelPrice = expectedFuelPrices.get(mix.getSubstance());
            fc += amount * fuelPrice;
        }
        return fc;
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

        // check which substances are included in the risk evaluation
        boolean coalFound = !a.isCoalPriceRiskIncluded();
        boolean gasFound = !a.isGasPriceRiskIncluded();
        boolean ligniteFound = !a.isLignitePriceRiskIncluded();
        boolean uraniumFound = !a.isUraniumPriceRiskIncluded();
        boolean biomassFound = !a.isBiomassPriceRiskIncluded();

        // search substances in repository by string and add confidence level to
        // the map
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

    // Class that covers the relevant market information including the
    // dispatched power plants and
    // the ldc
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
            for (TargetInvestor targetInvestor : reps.targetInvestorRepository.findAllByMarket(market)) {
                if (!(targetInvestor instanceof StochasticTargetInvestor)) {
                    for (PowerGeneratingTechnologyTarget pggt : targetInvestor.getPowerGenerationTechnologyTargets()) {
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
                } else {
                    for (PowerGeneratingTechnologyTarget pggt : targetInvestor.getPowerGenerationTechnologyTargets()) {
                        // Not needed?
                        // double expectedTechnologyCapacity =
                        // reps.powerPlantRepository
                        // .calculateCapacityOfExpectedOperationalPowerPlantsInMarketAndTechnology(market,
                        // pggt.getPowerGeneratingTechnology(), time);
                        double expectedTechnologyAddition = 0;
                        long contructionTime = getCurrentTick()
                                + pggt.getPowerGeneratingTechnology().getExpectedLeadtime()
                                + pggt.getPowerGeneratingTechnology().getExpectedPermittime();
                        for (long investmentTimeStep = contructionTime + 1; investmentTimeStep <= time; investmentTimeStep = investmentTimeStep + 1) {
                            expectedTechnologyAddition += (pggt.getTrend().getValue(investmentTimeStep) - pggt
                                    .getTrend().getValue(investmentTimeStep - 1));
                        }
                        if (expectedTechnologyAddition > 0) {
                            PowerPlant plant = new PowerPlant();
                            plant.specifyNotPersist(getCurrentTick(), new EnergyProducer(),
                                    reps.powerGridNodeRepository.findFirstPowerGridNodeByElectricitySpotMarket(market),
                                    pggt.getPowerGeneratingTechnology());
                            plant.setActualNominalCapacity(expectedTechnologyAddition);
                            double plantMarginalCost = determineExpectedMarginalCost(plant, fuelPrices, co2price);
                            marginalCostMap.put(plant, plantMarginalCost);
                            capacitySum += expectedTechnologyAddition;
                        }
                    }
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
