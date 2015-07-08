#Placeholders

# Step 1 building the scenarios: insert dataframe and read scenarioA.xml file
xmlFilePath<-"/home/marvin/Schreibtisch/Master/RScript/RiskBasedInvestmentTemplate.xml"
filestump<-''

# Step 2 building the scenarios: make separate data vectors
fuelPriceScenarioLength=1
microScenarioLength=120

fuelPriceFileStumpStandard<-"/data/stochasticFuelPrices/fuelPrices-"
demandFileStumpStandard<-"/data/stochasticDemandCWEandGB/demand-"
fuelPriceFileStumpHighVolatility<-"/data/stochasticFuelPricesHighVolatility/fuelPrices-"
demandFileStumpHighVolatility<-"/data/stochasticDemandCWEandGBHighVolatility/demand-"


#Risk specific parameters
demandIncluded<-TRUE
coalPriceIncluded<-TRUE
gasPriceIncluded<-TRUE
lignitePriceIncluded<-FALSE
biomassPriceIncluded<-FALSE
uraniumPriceIncluded<-FALSE
co2PriceIncluded<-FALSE

demandCofidenceLevel<-0.8
coalPriceConfidenceLevel<-0.95
gasPriceConfidenceLevel<-0.95
lignitePriceConfidenceLevel<-0.95
biomassPriceConfidenceLevel<-0.95
uraniumPriceConfidenceLevel<-0.95
co2PriceConfidenceLevel<-0.95

thresholdDefinition<-3
threshold<- -200000

demandVolatility<-"normal"
coalPriceVolatility<-"normal"
gasPriceVolatility<-"normal"




standardRiskParameterSet=list("#demandIncluded"=demandIncluded,"#coalPriceIncluded"=coalPriceIncluded,"#gasPriceIncluded"=gasPriceIncluded,
                              "#lignitePriceIncluded"=lignitePriceIncluded, "#biomassPriceIncluded"=biomassPriceIncluded,"#uraniumPriceIncluded"=uraniumPriceIncluded,
                              "#co2PriceIncluded"=co2PriceIncluded,"#coalPriceConfidenceLevel"=coalPriceConfidenceLevel,
                              "#gasPriceConfidenceLevel"=gasPriceConfidenceLevel,"#lignitePriceConfidenceLevel"=lignitePriceConfidenceLevel,
                              "#biomassPriceConfidenceLevel"=biomassPriceConfidenceLevel,"#uraniumPriceConfidenceLevel"=uraniumPriceConfidenceLevel,
                              "#co2PriceConfidenceLevel"=co2PriceConfidenceLevel,"#demandConfidenceLevel"=demandCofidenceLevel,
                              "#thresholdDefinition"=thresholdDefinition, "#threshold"=threshold, "#demandVolatility"=demandVolatility, 
                              "#coalPriceVolatility"=coalPriceVolatility, "#gasPriceVolatility"=gasPriceVolatility)
standardRiskParameterSet["#coalPriceIncluded"]
standardRiskParameterSet["#threshold"]

#variations
noRisk=standardRiskParameterSet
noRisk["#demandIncluded"]=FALSE
noRisk["#coalPriceIncluded"]=FALSE
noRisk["#gasPriceIncluded"]=FALSE

noDemand=standardRiskParameterSet
noDemand["#demandIncluded"]=FALSE

onlyDemand=standardRiskParameterSet
onlyDemand["#coalPriceIncluded"]=FALSE
onlyDemand["#gasPriceIncluded"]=FALSE

onlyCoal=standardRiskParameterSet
onlyCoal["#demandIncluded"]=FALSE
onlyCoal["#gasPriceIncluded"]=FALSE

onlyGas=standardRiskParameterSet
onlyGas["#demandIncluded"]=FALSE
onlyGas["#coalPriceIncluded"]=FALSE

demandConfidenceLevelLow=standardRiskParameterSet
demandConfidenceLevelLow["#demandConfidenceLevel"]=0.6

demandConfidenceLevelHigh=standardRiskParameterSet
demandConfidenceLevelHigh["#demandConfidenceLevel"]=0.9

demandConfidenceLevelHigher=standardRiskParameterSet
demandConfidenceLevelHigher["#demandConfidenceLevel"]=0.95

demandConfidenceLevelMax=standardRiskParameterSet
demandConfidenceLevelMax["#demandConfidenceLevel"]=0.99

fuelPriceConfidenceLevelVeryLow=standardRiskParameterSet
fuelPriceConfidenceLevelVeryLow["#coalPriceConfidenceLevel"]=0.6
fuelPriceConfidenceLevelVeryLow["#gasPriceConfidenceLevel"]=0.6

fuelPriceConfidenceLevelLow=standardRiskParameterSet
fuelPriceConfidenceLevelLow["#coalPriceConfidenceLevel"]=0.8
fuelPriceConfidenceLevelLow["#gasPriceConfidenceLevel"]=0.8

fuelPriceConfidenceLevelHigh=standardRiskParameterSet
fuelPriceConfidenceLevelHigh["#coalPriceConfidenceLevel"]=0.99
fuelPriceConfidenceLevelHigh["#gasPriceConfidenceLevel"]=0.99

fuelPriceConfidenceLevelMax=standardRiskParameterSet
fuelPriceConfidenceLevelMax["#coalPriceConfidenceLevel"]=0.999
fuelPriceConfidenceLevelMax["#gasPriceConfidenceLevel"]=0.999

threshold3VeryLow=standardRiskParameterSet
threshold3VeryLow["#threshold"]=-1000000

threshold3Low=standardRiskParameterSet
threshold3Low["#threshold"]=-500000

threshold3High=standardRiskParameterSet
threshold3High["#threshold"]=0

threshold1Low=standardRiskParameterSet
threshold1Low["#thresholdDefinition"]=1
threshold1Low["#threshold"]=-0.0005

threshold1Medium=standardRiskParameterSet
threshold1Medium["#thresholdDefinition"]=1
threshold1Medium["#threshold"]=-0.0001

threshold1High=standardRiskParameterSet
threshold1High["#thresholdDefinition"]=1
threshold1High["#threshold"]=-0.00001

highVolatilityAll=standardRiskParameterSet
highVolatilityAll["#demandVolatility"]="high"
highVolatilityAll["#gasPriceVolatility"]="high"
highVolatilityAll["#coalPriceVolatility"]="high"

highVolatilityDemand=onlyDemand
highVolatilityDemand["#demandVolatility"]="high"

highVolatilityCoal=onlyCoal
highVolatilityCoal["#coalPriceVolatility"]="high"

highVolatilityGas=onlyGas
highVolatilityGas["#gasPriceVolatility"]="high"

variations=list(
                #"0-standardSet"=standardRiskParameterSet,"1.1-noRisk"=noRisk, "1.2-onlyDemand"=onlyDemand,"1.3-onlyCoal"=onlyCoal,
                #"1.4-onlyGas"=onlyGas,"1.5-noDemand"=noDemand,
                #"2.1-demandConfLvLow"=demandConfidenceLevelLow, "2.2-demandConfLvHigh"=demandConfidenceLevelHigh,
                #"2.3-demandConfLvVeryHigh"=demandConfidenceLevelHigher,"2.4-demandConfLvMax"=demandConfidenceLevelMax,
                #"2.5-fuelPriceConfLvlVeryLow"=fuelPriceConfidenceLevelVeryLow,"2.6-fuelPriceConfLvLow"=fuelPriceConfidenceLevelLow,
                #"2.7-fuelPriceConfLvHigh"=fuelPriceConfidenceLevelHigh,"2.8-fuelPriceConfLvMax"=fuelPriceConfidenceLevelMax,
                #"3.1-threshold3VeryLow"=threshold3VeryLow,"3.2-threshold3Low"=threshold3Low,"3.3-threshold3High"=threshold3High,
                #"threshold1Low"=threshold1Low,"threshold1Medium"=threshold1Medium,"threshold1High"=threshold1High
                "4.1-highVolatilityStandard"=highVolatilityAll, "4.2-highVolatilityOnlyDemand"=highVolatilityDemand,
                "4.3-highVolatilityOnlyCoalPrice"=highVolatilityCoal, "4.4-highVolatilityOnlyGasPrice"=highVolatilityGas
              )

#BaseCase Scenario
priceCeiling="120"
coalPriceScenario=c("Coal.Medium","Coal.Low","Coal.High")
gasPriceScenario=c("NaturalGas.Medium","NaturalGas.Low","NaturalGas.High")
fuelPriceScenarios = c("FuelCentral")
demandGrowthScenarios = c("demandCentral")
stabilityReserveFirstYearOfOperation="10"

resPolicyScenarios=list(FRES=c("#cweResPolicy"="/data/policyGoalNREAP_CF_CWE.csv","#gbResPolicy"="/data/policyGoalNREAP_CF_UK.csv"))
producerBankingScenarios=list("BaseBanking-R3"=c("#stabilityReserveBankingFirstYear"="0.8",
                                            "#stabilityReserveBankingSecondYear"="0.5",
                                            "#stabilityReserveBankingThirdYear"="0.2",
                                            "#centralPrivateDiscountingRate"="0.05",
                                            "#centralCO2BackSmoothingFactor"="0",
                                            "#centralCO2TargetReversionSpeedFactor"="3"
                                            ))

co2PolicyScenarios=list(NoETS=c("#emissionCapTimeline"="/data/emissionCapCweUk.csv",
                       "#co2TradingActive"="false"))
                        #PureETS=c("#emissionCapTimeline"="/data/emissionCapCweUk.csv",
                           #"#co2TradingActive"="true"),

microScenarioNo<-seq(1,microScenarioLength)


#No Backloading, smoothed backgloading, backloading
#backLoadingName=c("NBL","SBL","BL")
#backLoadingValue=c("/data/emissionCapCweUk.csv","/data/emissionCapCweUk_unfccc_backloading_smoothed.csv","/data/emissionCapCweUk_unfccc_backloading.csv")
#backLoadingName=c("NBL","SBL","BL")
#backLoadingValue=c("/data/emissionCapCweUk_citl.csv","/data/emissionCapCweUk_citl_backloading_smoothed.csv","/data/emissionCapCweUk_Citl_backloading.csv")

# Step 3 building the scenarios: estimating the last three parameters
#${initial_propensity}
for (parameterCombination in names(variations)){
  co2PolicyNo<-1
  for(co2PolicyScenario in co2PolicyScenarios){
    producerBankingNo<-1
    for(producerBankingScenario in producerBankingScenarios){
      resScenarioNo<-1
      for(resScenario in resPolicyScenarios){
          for (fuelId in seq(1:fuelPriceScenarioLength)){
            for(demandId in seq(1:length(demandGrowthScenarios))){
              if (variations[[parameterCombination]]["#demandVolatility"]=="high") {demandFileStump=demandFileStumpHighVolatility}
              else {demandFileStump=demandFileStumpStandard}
              if (variations[[parameterCombination]]["#coalPriceVolatility"]=="high") {coalPriceFileStump=fuelPriceFileStumpHighVolatility}
              else {coalPriceFileStump=fuelPriceFileStumpStandard}
              if (variations[[parameterCombination]]["#gasPriceVolatility"]=="high") {gasPriceFileStump=fuelPriceFileStumpHighVolatility}
              else {gasPriceFileStump=fuelPriceFileStumpStandard}
              
               for (microId in seq(1:microScenarioLength)){
                  xmlFileContent<-readLines(xmlFilePath, encoding = "UTF-8")
                  xmlFileContent<-gsub("#demandRiskIncluded",variations[[parameterCombination]]["#demandIncluded"], xmlFileContent)
                  xmlFileContent<-gsub("#coalPriceRiskIncluded",variations[[parameterCombination]]["#coalPriceIncluded"], xmlFileContent)
                  xmlFileContent<-gsub("#gasPriceRiskIncluded",variations[[parameterCombination]]["#gasPriceIncluded"], xmlFileContent)
                  xmlFileContent<-gsub("#co2PriceRiskIncluded",variations[[parameterCombination]]["#co2PriceIncluded"], xmlFileContent)
                  xmlFileContent<-gsub("#lignitePriceRiskIncluded",variations[[parameterCombination]]["#lignitePriceIncluded"], xmlFileContent)
                  xmlFileContent<-gsub("#uraniumPriceRiskIncluded",variations[[parameterCombination]]["#uraniumPriceIncluded"], xmlFileContent)
                  xmlFileContent<-gsub("#biomassPriceRiskIncluded",variations[[parameterCombination]]["#biomassPriceIncluded"], xmlFileContent)
                  xmlFileContent<-gsub("#demandConfidenceLevel",variations[[parameterCombination]]["#demandConfidenceLevel"], xmlFileContent)
                  xmlFileContent<-gsub("#coalPriceConfidenceLevel",variations[[parameterCombination]]["#coalPriceConfidenceLevel"], xmlFileContent)
                  xmlFileContent<-gsub("#gasPriceConfidenceLevel",variations[[parameterCombination]]["#gasPriceConfidenceLevel"], xmlFileContent)
                  xmlFileContent<-gsub("#co2PriceConfidenceLevel",variations[[parameterCombination]]["#co2PriceConfidenceLevel"], xmlFileContent)
                  xmlFileContent<-gsub("#lignitePriceConfidenceLevel",variations[[parameterCombination]]["#lignitePriceConfidenceLevel"], xmlFileContent)
                  xmlFileContent<-gsub("#uraniumPriceConfidenceLevel",variations[[parameterCombination]]["#uraniumPriceConfidenceLevel"], xmlFileContent)
                  xmlFileContent<-gsub("#biomassPriceConfidenceLevel",variations[[parameterCombination]]["#biomassPriceConfidenceLevel"], xmlFileContent)
                  xmlFileContent<-gsub("#thresholdDefinition",variations[[parameterCombination]]["#thresholdDefinition"], xmlFileContent)
                  xmlFileContent<-gsub("#threshold",variations[[parameterCombination]]["#threshold"], xmlFileContent)
                  xmlFileContent<-gsub("#demandVolatility ",variations[[parameterCombination]]["#demandVolatility"], xmlFileContent)
                  xmlFileContent<-gsub("#coalPriceVolatility ",variations[[parameterCombination]]["#coalPriceVolatility"], xmlFileContent)
                  xmlFileContent<-gsub("#gasPriceVolatility ",variations[[parameterCombination]]["#gasPriceVolatility"], xmlFileContent)
                  xmlFileContent<-gsub("#coalPricePathAndFileName", paste(coalPriceFileStump,microId,".csv", sep="") , xmlFileContent)
                  xmlFileContent<-gsub("#gasPricePathAndFileName", paste(gasPriceFileStump,microId,".csv", sep="") , xmlFileContent)
                  xmlFileContent<-gsub("#biomassPricePathAndFileName", paste(fuelPriceFileStumpStandard,microId,".csv", sep="") , xmlFileContent)
                  xmlFileContent<-gsub("#uraniumPricePathAndFileName", paste(fuelPriceFileStumpStandard,microId,".csv", sep="") , xmlFileContent)
                  xmlFileContent<-gsub("#lignitePricePathAndFileName", paste(fuelPriceFileStumpStandard,microId,".csv", sep="") , xmlFileContent)
                  xmlFileContent<-gsub("#demandPathandFilename", paste(demandFileStump,microId,".csv", sep="") , xmlFileContent)
                  xmlFileContent<-gsub("#CoalScenario", coalPriceScenario[fuelId], xmlFileContent)
                  xmlFileContent<-gsub("#GasScenario", gasPriceScenario[fuelId], xmlFileContent)
                  for(resPolicyParameterNo in seq(1,length(resScenario))){
                    xmlFileContent<-gsub(names(resScenario)[resPolicyParameterNo], resScenario[resPolicyParameterNo], xmlFileContent)
                  }
                  for(producerBankingParameterNo in seq(1,length(producerBankingScenario))){
                    xmlFileContent<-gsub(names(producerBankingScenario)[producerBankingParameterNo], producerBankingScenario[producerBankingParameterNo], xmlFileContent)
                  }
                  for(co2PolicyParameterNo in seq(1,length(co2PolicyScenario))){
                    #print(paste("Substituting:",co2PolicyScenario[co2PolicyParameterNo]))
                    #flush.console()
                    xmlFileContent<-gsub(names(co2PolicyScenario)[co2PolicyParameterNo], co2PolicyScenario[co2PolicyParameterNo], xmlFileContent)
                  }
                  #print( paste("~/Dropbox/emlabGen/scenario/",filestump,names(co2PolicyScenarios)[co2PolicyNo],"-",names(producerBankingScenarios)[producerBankingNo],"-",names(resPolicyScenarios)[resScenarioNo],"-",names(resRealisations)[resRealisationNumber],"-",fuelPriceScenarios[fuelId],"-",microId,".xml", sep=""))
                  #flush.console()
                  writeLines(xmlFileContent, paste("/home/marvin/Schreibtisch/Master/RScript/Scenario4.1-4.4/",filestump,parameterCombination,"-",microId,".xml", sep=""))
            }
          }
        }
        resScenarioNo<-resScenarioNo+1
      }
    producerBankingNo<-producerBankingNo+1
    }
   co2PolicyNo<-co2PolicyNo+1
  }
}
