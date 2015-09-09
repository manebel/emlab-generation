setwd("~/emlab-generation/rscripts/") # folder where all r scripts are
library(xtable)
library(scales)
library(gridExtra)
source("rConfig.R")
source("batchRunAnalysis.R")
setwd(analysisFolder) # should be defined in rConfig.R

safePlots=T #safe plots?
showPlots=F # show plots?
fileType<-".pdf"
scaleFactor<-1
nrowLength<-1
#textwidth<-16.51
#columnwidth<-7.83
textwidth<-18.4
columnwidth<-8.89
filePrefix<-"4-" # all output files will have this prefix
socialDiscountRate<-0.03
baseSize<-9
##---- Read in of Data                     ------------------
# Prefix must be adopted on the filenames in resultFolder
# All files with prefix will be analysed
# The location of resultFolder is defined in "rConfig.r"
if(!file.exists(paste(analysisFolder,filePrefix,".rds", sep=""))){
  bigDF <- rbind(getDataFrameForModelRunsInFolderWithFilePattern(resultFolder,"4.*.csv"))
  
  bigDF$modelRun<-lapply(as.character(bigDF$modelRun),function(x,...){strsplit(x,...)}[[1]][2], "-")
  
  #renaming model runs
  # How should the base rund be named?
# bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="standardSet","risk aversion")
# bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="standardSet","all risks")  
# bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="standardSet","ac=0.95 (default)")  
# bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="standardSet","ad=0.8 (default)")
# bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="standardSet","t=-200.000 (default)")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="standardSet","ALL_vol default")

  # Renaming the other model runs
  # single risks
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="noRisk","risk neutrality")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="onlyCoal","coal price risk")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="onlyGas","gas price risk")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="onlyDemand","demand risk")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="noDemand","coal & gas price risk")

  # demand confidence level
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="demandConfLvLow","ad=0.6")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="demandConfLvHigh","ad=0.9")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="demandConfLvVeryHigh","ad=0.95")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="demandConfLvMax","ad=0.99")
  
  # fuel price confidence level
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="fuelPriceConfLvlVeryLow","ac=0.6")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="fuelPriceConfLvLow","ac=0.8")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="fuelPriceConfLvHigh","ac=0.99")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="fuelPriceConfLvMax","ac=0.999")
  
  # threshold
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="threshold3VeryLow","t=-1.000.000")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="threshold3Low","t=-500.000")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="threshold3High","t=0")
  
  # volatilies
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="highVolatilityStandard","ALL_vol+")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="highVolatilityOnlyCoalPrice","CoalPrice_vol+")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="highVolatilityOnlyGasPrice","GasPrice_vol+")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="highVolatilityOnlyDemand","Demand_vol+")
  

 # factor model runs to change order of occurences
 # risk neutrality vs. risk aversion
 # bigDF$modelRun<-factor(bigDF$modelRun,levels=c("risk neutrality", "risk aversion"))

 # single risk factors: fuel price risks
 # bigDF$modelRun<-factor(bigDF$modelRun,levels=c("risk neutrality", "coal price risk","gas price risk","coal & gas price risk","all risks"))
 
 # single risk factors: demand risk
 # bigDF$modelRun<-factor(bigDF$modelRun,levels=c("risk neutrality", "demand risk","all risks"))
 
 # single risk factors: fuel price risk and demand risk
 # bigDF$modelRun<-factor(bigDF$modelRun,levels=c("risk neutrality", "demand risk","coal & gas price risk","all risks"))

 # demand confidence level
 # bigDF$modelRun<-factor(bigDF$modelRun,levels=c("ac=0.6", "ac=0.8","ac=0.95 (default)","ac=0.99","ac=0.999"))

 # fuel price confidence level
 # bigDF$modelRun<-factor(bigDF$modelRun,levels=c("ad=0.6", "ad=0.8 (default)","ad=0.9","ad=0.99"))

 # threshold
 # bigDF$modelRun<-factor(bigDF$modelRun,levels=c("risk neutrality","t=-1.000.000", "t=-500.000","t=-200.000 (default)","t=0"))
 
 # volatilities
  bigDF$modelRun<-factor(bigDF$modelRun,levels=c("ALL_vol default", "CoalPrice_vol+","GasPrice_vol+","Demand_vol+","ALL_vol+"))
  
  
# add stochastic ID to bigDF
  bigDF$stochasticId<-bigDF$runId
  bigDF$stochasticId<-lapply(as.character(bigDF$stochasticId),function(x,...){strsplit(x,...)}[[1]][3], "-")
  bigDF$stochasticId<-as.numeric(bigDF$stochasticId)

# add supply ratios
  bigDF <- addSupplyRatios(bigDF)

# add aggregated producer cash
  bigDF <- addSumOfVariablesByPrefixToDF(bigDF, "ProducerCash")
  bigDF$ProducerCashInGB<-bigDF$ProducerCash_Energy.Producer.A+bigDF$ProducerCash_Energy.Producer.B+bigDF$ProducerCash_Energy.Producer.C+bigDF$ProducerCash_Energy.Producer.D
  bigDF$ProducerCashInCWE<-bigDF$ProducerCash_Energy.Producer.E+ bigDF$ProducerCash_Energy.Producer.F+bigDF$ProducerCash_Energy.Producer.G+bigDF$ProducerCash_Energy.Producer.H
  
  # Attention: need to adjust the initial differential value to the starting cash balances of producers!
  bigDF<-ddply(bigDF, .(runId), diffExpenditures2, list(c("EUGovernmentCash","Co2PolicyIncome_EU"),c("NationalGovernmentCash_Country.A","Co2PolicyIncome_Country.A"),c("NationalGovernmentCash_Country.B","Co2PolicyIncome_Country.B"),c("SpotMarketCash_GB.electricity.spot.market","Consumer_Cost.GB"),c("SpotMarketCash_CWE.electricity.spot.market","Consumer_Cost.CWE"),c("ProducerCash_Renewable.Target.Investor.CWE","RenewableSubsidy.CWE"),c("ProducerCash_Renewable.Target.Investor.GB","RenewableSubsidy.GB")),0)
  bigDF<-ddply(bigDF, .(runId), diffExpenditures2, list(c("MarketStabilityReserve","MsrInflow")),0)
  
# add aggregated and specific consumer costs
  bigDF$ConsumerCostinclSubs.GB<-(bigDF$Consumer_Cost.GB+bigDF$RenewableSubsidy.GB)*-1
  bigDF$ConsumerCostinclSubs.CWE<-(bigDF$Consumer_Cost.CWE+bigDF$RenewableSubsidy.CWE)*-1

  bigDF$SpecificConsumerCostinclSubs.GB<-bigDF$ConsumerCostinclSubs.GB/bigDF$Total_DemandinMWh_Country.B
  bigDF$SpecificConsumerCostinclSubs.CWE<-bigDF$ConsumerCostinclSubs.CWE/bigDF$Total_DemandinMWh_Country.A
  
  bigDF$SpecificConsumerCost.GB<-bigDF$Consumer_Cost.GB/bigDF$Total_DemandinMWh_Country.B*-1
  bigDF$SpecificConsumerCost.CWE<-bigDF$Consumer_Cost.CWE/bigDF$Total_DemandinMWh_Country.A*-1
  
  bigDF$SpecificRenewableSubsidy.GB<-bigDF$RenewableSubsidy.GB/bigDF$Total_DemandinMWh_Country.B*-1
  bigDF$SpecificRenewableSubsidy.CWE<-bigDF$RenewableSubsidy.CWE/bigDF$Total_DemandinMWh_Country.A*-1
  
  bigDF$OverallWellfare<-bigDF$CountryAProdFinances_Profit+bigDF$CountryBProdFinances_Profit+bigDF$Consumer_Cost.GB+bigDF$Consumer_Cost.CWE+bigDF$Co2PolicyIncome_EU+bigDF$Co2PolicyIncome_Country.A+bigDF$Co2PolicyIncome_Country.B+bigDF$RenewableSubsidy.CWE+bigDF$RenewableSubsidy.GB
  
  # add energy not served
  bigDF$EnergyNotServedinMWh<-bigDF$EnergyNotServedinMWh_Country.B+bigDF$EnergyNotServedinMWh_Country.A
  
  # relative energy not served in GB
  RelativeEnergyNotServed_Country.B<-bigDF$EnergyNotServedinMWh_Country.B/bigDF$Total_DemandinMWh_Country.B
  oldNames<-names(bigDF)
  bigDF<-cbind(bigDF, RelativeEnergyNotServed_Country.B)
  names(bigDF)<-c(oldNames,"RelativeEnergyNotServed_Country.B")
  #p<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF,"EnergyNotServedinMWh","Energy not Served [MWh]")
  
  bigDF$OverallWellfare<-bigDF$OverallWellfare-bigDF$WelfareLossThroughENS_Country.A-bigDF$WelfareLossThroughENS_Country.B
  saveRDS(bigDF, file=paste(analysisFolder,filePrefix,".rds", sep=""))
} else{
  bigDF<-readRDS(paste(analysisFolder,filePrefix,".rds", sep=""))
}

# last tick to be evaluated
untilTick<-40
# standard set for variables to safe
standardSubSet<-c("tick","modelRun","runId")
yearFormatter <- function(x){ 
  x = x+2011
}
# time breaks in plots
timeBreaks=c(0,seq(9,39, by=10))


# If legend labels are different than the column names in scenario
#legendLabels=c(c(expression(paste(alpha[c],"/",alpha[g],"= 0.6 ")),expression(paste(alpha[c],"/",alpha[g],"= 0.8 ")),
#                expression(paste(alpha[c],"/",alpha[g],"= 0.95 (default) ")),expression(paste(alpha[c],"/",alpha[g],"= 0.99 ")),
#                expression(paste(alpha[c],"/",alpha[g],"= 0.999 "))))
#legendLabels=c(c(expression(paste(alpha[d],"= 0.6 ")),expression(paste(alpha[d],"= 0.8 (default)")),
#                 expression(paste(alpha[d],"= 0.9 ")),
#                 expression(paste(alpha[d],"= 0.99 "))))

legendLabels=c(c("risk neutrality",expression(paste(tau,"= -1.000.000 ")),expression(paste(tau,"= -500.000 ")),
                 expression(paste(tau,"= -200.000 (default) ")),
                 expression(paste(tau,"= 0"))))

legendLabels2=c(expression(paste(alpha[d],"= 0.8 (default)")),
                 expression(paste(alpha[d],"= 0.9")),
                 expression(paste(alpha[d],"= 0.99")))

wrap_labeller <- function(variable,value){
  return(legendLabels[value])
}


# Electricity price statistics
elPriceCWE<-meltTechnologyVariable(bigDF,"Avg_El_PricesinEURpMWh_Country.A")
elPriceCWE$variable<-rep("CWE",dim(elPriceCWE)[1])
elPriceGB<-meltTechnologyVariable(bigDF,"Avg_El_PricesinEURpMWh_Country.B")
elPriceGB$variable<-rep("GB",dim(elPriceGB)[1])
elPriceAll<-rbind(elPriceCWE,elPriceGB)

electricityPriceInCWE<-meltTechnologyVariable(bigDF,"Avg_El_PricesinEURpMWh_Country.A")
electricityPriceInCWE$variable<-rep("CWE",dim(electricityPriceInCWE)[1])
electricityPriceInGB<-meltTechnologyVariable(bigDF,"Avg_El_PricesinEURpMWh_Country.B")
electricityPriceInGB$variable<-rep("GB",dim(electricityPriceInCWE)[1])
electricityPriceALL<-rbind(electricityPriceInCWE,electricityPriceInGB)

elPriceAllPlot<-plotMoltenVariableFacettedByVariable(elPriceAll, paste("Average electricity price [EUR/MWh]",sep=""))+
  theme(legend.position="bottom")+
  scale_color_discrete("Scenario")+
  scale_fill_discrete(guide = 'none')+
  scale_shape_discrete(guide = 'none')+
  stat_summary(aes_string(colour="modelRun", fill="modelRun", group="modelRun"), fun.y="mean", geom="line", linetype=1)+
  stat_summary(data=elPriceAll[elPriceAll$tick%%5==0 | elPriceAll$tick==39,],aes_string(group="modelRun",shape="modelRun"), fun.y="mean", geom="point", size=1.5)+
  facet_wrap(~ variable, scales="free_y")+
  xlab("\nYear")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)
if(safePlots) ggsave(filename= paste(filePrefix, "AllElectricityPrice",fileType, sep=""),plot=elPriceAllPlot, width=15.66, height=10.44, units="cm", scale=scaleFactor)


avgPricePlotinCWE<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF, "Avg_El_PricesinEURpMWh_Country.A", "Avg. Electricity Price in CWE [EUR/MW]") +
  facet_grid( .~ modelRun)
if(showPlots) avgPricePlotinCWE
if(safePlots) ggsave(filename= paste(filePrefix, "avgElPriceInCWE",fileType, sep=""),plot=avgPricePlotinCWE, width=15.66, height=10.44, units="cm", scale=scaleFactor)

avgPricePlotinGB<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF, "Avg_El_PricesinEURpMWh_Country.B", "Avg. Electricity Price in GB [EUR/MW]", nrow=nrowLength)
if(showPlots) avgPricePlotinGB
if(safePlots) ggsave(filename= paste(filePrefix, "avgPricePlotinGB",fileType, sep=""),plot=avgPricePlotinGB, width=15.66, height=10.44, units="cm", scale=scaleFactor)


electricityPriceGrid<-ggplot(electricityPriceALL, aes_string(x="tick", y="value"))+
  stat_summary(fun.data="median_hilow", conf.int=0.5, geom="smooth", colour="black") +
  stat_summary(fun.data="median_hilow", conf.int=0.9, geom="smooth", colour="black")+
  facet_grid(variable ~ modelRun, scales="free_y")+#todo
  theme(legend.position="none",panel.margin = unit(0.8, "lines"))+
  xlab("Year")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)+
  ylab("Avg. Electricity Price [EUR/MWh]")
if(safePlots) ggsave(filename=
                       paste(filePrefix, "avgPricePlotGrid",fileType, sep=""),plot=electricityPriceGrid, scale=scaleFactor)

# Electricity not served
electricityNotServedCWE<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF, "EnergyNotServedinMWh_Country.A","Energy not served in CWE [MWh]")
electricityNotServedGB<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF, "EnergyNotServedinMWh_Country.B","Energy not served in GB [MWh]")
electricityNotServedGB<- electricityNotServedGB+
  xlab("Year")+
  #scale_y_continuous(labels = percent)+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)+
  theme(panel.margin = unit(1, "lines"), axis.title.y=element_text(size=9))
if(safePlots) ggsave(filename= paste(filePrefix, "electricityNotServedGB",fileType, sep=""),plot=electricityNotServedGB, width=18.66, height=10.44, units="cm", scale=scaleFactor)

outageYearsInCWE<-ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunId, function(x){sum(x>0.1)}, "EnergyNotServedinMWh_Country.A")
outageYearsInGB<-ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunId, function(x){sum(x>0.1)}, "EnergyNotServedinMWh_Country.B")
outageYearsTotal=ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunId, function(x){sum(x>0.1)}, "EnergyNotServedinMWh")
outageYearsTotal1=outageYearsTotal[outageYearsTotal$modelRun=="ad=0.8 (default)"|outageYearsTotal$modelRun=="ad=0.9"|outageYearsTotal$modelRun=="ad=0.99",]
outageYearsTotal1[outageYearsTotal1$V1>5,]$V1=6

outageYearsTotalPlot<-ggplot(outageYearsTotal1,aes(x=V1))+
  #geom_histogram(binwidth=5,aes(y=..count../sum(..cou[nt..)))+
  geom_bar(binwidth=1,aes(y=..count../120))+
  facet_grid(. ~ modelRun,labeller=wrap_labeller)+
  xlab("Number of outage years")+
  scale_x_continuous(breaks=c(0.5,1.5,2.5,3.5,4.5,5.5,6.5),labels=c(0,1,2,3,4,5,">5"),limits=c(-0.1,7.1))+
  ylab("Distribution (120 model runs)")+
  scale_y_continuous(limits=c(0,1.01),labels = percent)
if(showPlots) outageYearsTotalPlot
if(safePlots) ggsave(filename= paste(filePrefix, "outageYearsTotal",fileType, sep=""),plot=outageYearsTotalPlot, scale=scaleFactor)


# co2 Emission
co2EmissionPlot<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF[bigDF$tick<untilTick,c("CO2Emissions_inTonpA",standardSubSet)], "CO2Emissions_inTonpA", "CO2 Emissions [t/a]", nrow=nrowLength)+
  facet_wrap(~modelRun, nrow=1)+
  theme_publication(base_size=baseSize)
#co2EmissionPlot<-co2EmissionPlot+stat_summary(aes(y=CO2CapinTonpA_CO2_cap),fun.data="median_hilow", conf.int=1, geom="errorbar", colour="black")
if(showPlots) co2EmissionPlot
if(safePlots) ggsave(filename= paste(filePrefix, "co2Emission",fileType, sep=""),plot=co2EmissionPlot,width=textwidth, height=7, units="cm", scale=scaleFactor)


# total capacity statistics
totalCapacityinCWE<-meltTechnologyVariable(bigDF,"TotalOperationalCapacityPerZoneInMW_Country.A")
totalCapacityinCWE$value<-totalCapacityinCWE$value/1000
totalCapacityinCWE$variable<-rep("CWE",dim(totalCapacityinCWE)[1])
totalCapacityinGB<-meltTechnologyVariable(bigDF,"TotalOperationalCapacityPerZoneInMW_Country.B")
totalCapacityinGB$value<-totalCapacityinGB$value/1000
totalCapacityinGB$variable<-rep("GB",dim(totalCapacityinGB)[1])
totalCapacityTogether<- totalCapacityinGB
totalCapacityTogether$value<-totalCapacityinCWE$value+totalCapacityinGB$value
totalCapacityTogether$variable<-rep("Total Capacity",dim(totalCapacityTogether)[1])
allTotalCapacities<-rbind(totalCapacityinCWE,totalCapacityinGB)

overallTotalCapacities<-plotTimeSeriesWithConfidenceIntervalGroupedInOnePlot(totalCapacityTogether, "value", "Total Capacities")+
  ylab("Capacity [GW]\n")+
  xlab("")+
  scale_color_discrete("")+
  scale_fill_discrete("")+
  scale_shape_discrete("")+
  facet_wrap(~ variable)+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)+
  theme(legend.position="none")
if(safePlots) ggsave(filename= paste(filePrefix, "totalCapacitiesPerModelRun",fileType, sep=""),plot=overallTotalCapacities, width=18.66, height=10, units="cm", scale=scaleFactor)

totalCapacitiesFaceted<-plotMoltenVariableFacettedByVariable(allTotalCapacities, "Capacity [GW]")+
  theme(legend.position="bottom")+
  scale_color_discrete("")+
  scale_fill_discrete("")+
  scale_shape_discrete("")+
  stat_summary(aes_string(colour="modelRun", fill="modelRun", group="modelRun"), fun.y="mean", geom="line", linetype=1)+
  stat_summary(data=allTotalCapacities[allTotalCapacities$tick%%5==0 | allTotalCapacities$tick==39,],aes_string(group="modelRun",shape="modelRun"), fun.y="mean", geom="point", size=1.5)+
  facet_wrap(~ variable, scales="free_y")+
  ylab("Capacity [GW]\n")+
  xlab("\nYear")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)
if(safePlots) ggsave(filename= paste(filePrefix, "totalCapacities",fileType, sep=""),plot=totalCapacitiesFaceted, width= 18.66, height= 8, units="cm", scale=scaleFactor)

totalCapacitiesGrid<-arrangeGrob(overallTotalCapacities,totalCapacitiesFaceted)
if(safePlots) ggsave(filename= paste(filePrefix, "totalCapacitiesGrid",fileType, sep=""),plot=totalCapacitiesGrid, width=18.66, height=18.66, units="cm", scale=scaleFactor)


# specific capacities
moltenCapacities<-meltTechnologyVariable(bigDF,"CapacityinMW_")
moltenCapacities$value<-moltenCapacities$value/1000
moltenCapacityinCWE<-meltTechnologyVariable(bigDF,"CapacityinMWinA_")
moltenCapacityinCWE$value<-moltenCapacityinCWE$value/1000
moltenCapacityinGB<-meltTechnologyVariable(bigDF,"CapacityinMWinB_")
moltenCapacityinGB$value<-moltenCapacityinGB$value/1000
moltenCapacityinCWE$zone<-rep("CWE",dim(moltenCapacityinCWE)[1])
moltenCapacityinGB$zone<-rep("GB",dim(moltenCapacityinGB)[1])

allMoltenCapacities<-rbind(moltenCapacityinCWE,moltenCapacityinGB)
selectedMoltenCapacities<-moltenCapacities[(moltenCapacities$variable%in%c("Nuclear","CoalPSC","IGCC","CCGT","OCGT") & moltenCapacities$tick<untilTick),]
selectedMoltenCapacities<-rbind(totalCapacityTogether,selectedMoltenCapacities)
#selectedMoltenCapacities<-moltenCapacities[(moltenCapacities$variable%in%c("Nuclear","Lignite","CoalPSC","IGCC","CCGT","OCGT","HydroPower","Biomass","Biogas",
#"Wind","WindOffshore","PV") & moltenCapacities$tick<untilTick),]
selectedMoltenCapacities$variable<-factor(selectedMoltenCapacities$variable,levels=c("Nuclear", "CoalPSC","IGCC","CCGT","OCGT","Total Capacity"))
selectedMoltenCapacitiesinCWE<-moltenCapacityinCWE[moltenCapacityinCWE$variable%in%c("Nuclear","Lignite","CoalPSC","IGCC","CCGT","OCGT"),]
selectedMoltenCapacitiesinGB<-moltenCapacityinGB[moltenCapacityinGB$variable%in%c("Nuclear","Lignite","CoalPSC","IGCC","CCGT","OCGT"),]


selectedCapacitiesFacetted<-ggplot(selectedMoltenCapacities, aes_string(x="tick", y="value", colour="modelRun", fill="modelRun"))+ #colour=modelRun, fill=modelRun,
  stat_summary(aes_string(colour="modelRun", fill="modelRun", group="modelRun"), fun.y="mean", geom="line", linetype=1)+
  stat_summary(data=selectedMoltenCapacities[selectedMoltenCapacities$tick%%5==0 | selectedMoltenCapacities$tick==39,],aes_string(group="modelRun",shape="modelRun"), fun.y="mean", geom="point", size=1.5)+
  facet_wrap(~ variable)+
  #facet_wrap(~ modelRun)+
  scale_color_discrete("Scenario")+
  scale_fill_discrete(guide='none')+
  scale_shape_discrete("Scenario")+
  xlab("\nYear")+
  ylab("Capacities [GW]\n")+
  theme_publication(base_size=baseSize)+
  theme(legend.position="bottom")+#,axis.title.y = element_text(size = rel(1.5)),axis.title.x = element_text(size = rel(1.5)),
  #       legend.key.width = unit(1.5, "line"),legend.text=element_text(size=14))+                                                              
  facet_wrap(~ variable, scales="free_y")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)
if(showPlots) selectedCapacitiesFacetted
if(safePlots) ggsave(filename= paste(filePrefix, "selectedCapacities",fileType, sep=""),plot=selectedCapacitiesFacetted,width=15, height=10,units="cm", scale=scaleFactor)

selectedcapacitiesinCWEFaceted<-plotMoltenVariableFacettedByVariable(selectedMoltenCapacitiesinCWE, "Capacity [GW]")+
  theme(legend.position="bottom")+
  facet_wrap(~ variable, scales="free_y")+
  ylab("Capacity in CWE [GW]")
#  theme(legend.margin=unit(-1, "cm"), plot.margin=unit(x=c(1,2,1,1),units="mm")) 
if(showPlots) selectedcapacitiesinCWEFaceted
if(safePlots) ggsave(filename= paste(filePrefix, "capacitiesinCWE",fileType, sep=""),plot=selectedcapacitiesinCWEFaceted, width=textwidth, height=15, units="cm", scale=scaleFactor)

selectedcapacitiesinGBFaceted<-plotMoltenVariableFacettedByVariable(selectedMoltenCapacitiesinGB, "Capacity [GW]")+
  theme(legend.position="bottom")+
  facet_wrap(~ variable, scales="free_y")+
  ylab("Capacity in GB [GW]")
#  theme(legend.margin=unit(-1, "cm"), plot.margin=unit(x=c(1,2,1,1),units="mm")) 
if(showPlots) selectedcapacitiesinGBFaceted
if(safePlots) ggsave(filename= paste(filePrefix, "capacitiesinGB",fileType, sep=""),plot=selectedcapacitiesinGBFaceted, width=textwidth, height=15, units="cm", scale=scaleFactor)

selectedAllCapacities<-allMoltenCapacities[allMoltenCapacities$variable%in%c("Nuclear","Lignite","CoalPSC","IGCC","CCGT","OCGT"),]
#selectedAllCapacities<-allMoltenCapacities[allMoltenCapacities$variable%in%c("Nuclear","Lignite","CoalPSC","IGCC","CCGT","OCGT","HydroPower","Biomass","Biogas",
#"Wind","WindOffshore","PV"),]
selectedAllCapacitiesFaceted<-plotBlackAndWhiteMoltenVariableFacettedByVariable(selectedAllCapacities, "Capacity [GW]")+
  theme_tufte(base_size=baseSize)+
  theme(legend.position="bottom",
        panel.background = element_rect(fill = "white", colour = NA), 
        panel.border = element_rect(fill = NA,colour = "grey50"), 
        panel.grid.major = element_line(colour = "grey90", size = 0.2), 
        panel.grid.minor = element_line(colour = "grey98", size = 0.5),
        strip.background = element_rect(fill = "grey80", colour = "grey50"), 
        strip.background = element_rect(fill = "grey80", colour = "grey50"))+
  facet_grid(zone ~ variable, scales="free_y")
#theme(legend.margin=unit(-1, "cm"), plot.margin=unit(x=c(1,2,1,1),units="mm"),axis.line=element_line(),axis.line.x=element_line(),axis.line.y=element_line())+

if(showPlots) selectedAllCapacitiesFaceted
if(safePlots) ggsave(filename= paste(filePrefix, "selectedAllCapacitiesFaceted_BW",fileType, sep=""),plot=selectedAllCapacitiesFaceted, width=textwidth, height=10, units="cm", scale=scaleFactor)


# Supply Ratios
supplyRatioinCWE<-meltTechnologyVariable(bigDF,"SupplyRatio_Country.A")
supplyRatioinCWE$variable<-rep("CWE",dim(supplyRatioinCWE)[1])
supplyRatioinGB<-meltTechnologyVariable(bigDF,"SupplyRatio_Country.B")
supplyRatioinGB$variable<-rep("GB",dim(supplyRatioinGB)[1])
allSupplyRatios<-rbind(supplyRatioinCWE,supplyRatioinGB)
allSupplyRatios$grp <- paste(allSupplyRatios$modelRun,allSupplyRatios$variable)

totalSupplyRatios<-ggplot(data=allSupplyRatios, aes(x=tick, y=value,group=grp))+                       
  theme(legend.position="right")+
  stat_summary(aes_string(colour="modelRun", fill="variable", group="grp", linetype="variable"), fun.y="mean", geom="line")+
  stat_summary(data=allSupplyRatios[allSupplyRatios$tick%%5==0 | allSupplyRatios$tick==39,],aes_string(group="grp",colour="modelRun",shape="modelRun"), fun.y="mean", geom="point", size=1.5)+
  ylab("Supply Ratios\n")+
  xlab("\nYear")+
  scale_fill_discrete(guide = 'none')+
  scale_shape_discrete(guide = 'none')+
  labs(colour="Model run",linetype="Zone")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)
if(safePlots) ggsave(filename= paste(filePrefix, "totalSupplyRatios",fileType, sep=""),plot=totalSupplyRatios, width= 18.66, height= 10, units="cm", scale=scaleFactor)

supplyRatioCWE<-ggplot(bigDF,aes_string(x="tick", y="SupplyRatio_Country.A", colour="modelRun", fill="modelRun")) + 
  stat_summary(aes_string(colour="modelRun", fill="modelRun", group="modelRun"), fun.y="mean",geom="line", linetype=1)+
  ylab("Supply Ratio CWE")
if(safePlots) ggsave(filename= paste(filePrefix, "SupplyRatioCWE",fileType, sep=""),plot=supplyRatioCWE, width= 15.66, height= 10, units="cm", scale=scaleFactor)

supplyRatioConf<-plotTimeSeriesWithConfidenceIntervalGroupedInOnePlot(allSupplyRatios, "value", "Supply ratio")+
  ylab("Supply ratio\n")+
  xlab("")+
  scale_color_discrete("")+
  scale_fill_discrete("")+
  scale_shape_discrete("")+
  facet_wrap(~ variable, scales="free_y")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)+
  theme(legend.position="bottom")
#scale_fill_identity(name = 'the fill', guide = 'legend',labels = c('m1')) +
#scale_colour_manual(name = 'the colour', 
#values =c('black'='black','red'='red'), labels = c('c2','c1'))+
#theme(panel.margin = unit(2, "lines"))
#scale_color_manual("Legend",values=c("black","gray50","gray80"))+
#scale_linetype_manual("Legend",values=c(1,3,4))
if(safePlots) ggsave(filename= paste(filePrefix, "supplyRatioConf",fileType, sep=""),plot=supplyRatioConf, width=18.66, height=10, units="cm", scale=scaleFactor)

supplyRatioGB<-ggplot(bigDF,aes_string(x="tick", y="SupplyRatio_Country.B", colour="modelRun", fill="modelRun")) + 
  stat_summary(aes_string(colour="modelRun", fill="modelRun", group="modelRun"), fun.y="mean",geom="line", linetype=1)+
  ylab("Supply Ratio GB")
if(safePlots) ggsave(filename= paste(filePrefix, "SupplyRatioGB",fileType, sep=""),plot=supplyRatioGB, width= 15.66, height= 10, units="cm", scale=scaleFactor)




# Consumer costs and producer profits
bigDF$DiscSpotMarketExpenditure.GB<-(bigDF$Consumer_Cost.GB)/(1+socialDiscountRate)^bigDF$tick
bigDF$DiscSpotMarketExpenditure.CWE<-(bigDF$Consumer_Cost.CWE)/(1+socialDiscountRate)^bigDF$tick
bigDF$DiscSubsidyExpenditure.GB<-(bigDF$RenewableSubsidy.GB)/(1+socialDiscountRate)^bigDF$tick
bigDF$DiscSubsidyExpenditure.CWE<-(bigDF$RenewableSubsidy.CWE)/(1+socialDiscountRate)^bigDF$tick
bigDF$DiscTotalConsumerExpenditure.GB<-bigDF$ConsumerCostinclSubs.GB/(1+socialDiscountRate)^bigDF$tick
bigDF$DiscTotalConsumerExpenditure.CWE<-bigDF$ConsumerCostinclSubs.CWE/(1+socialDiscountRate)^bigDF$tick

TotalSubsidyExpenditure.CWE<-ddply(ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunId, sum, "DiscSubsidyExpenditure.CWE"), .variables="modelRun", .fun=applyFunToColumnInDF ,fun=quantile, column="V1")

TotalExpenditure.CWE<-ddply(ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunId, sum, "DiscTotalConsumerExpenditure.CWE"), .variables="modelRun", .fun=applyFunToColumnInDF ,fun=quantile, column="V1")
TotalExpenditure.GB<-ddply(ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunId, sum,"DiscTotalConsumerExpenditure.GB"), .variables="modelRun", .fun=applyFunToColumnInDF ,fun=quantile, column="V1")

TotalExpenditure.CWE.raw<-ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunId, sum, "DiscSpotMarketExpenditure.CWE")
TotalExpenditure.GB.raw<-ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunId, sum, "DiscSpotMarketExpenditure.GB")

TotalExpenditure.CWE.raw$Zone<-rep(x="CWE",times=dim(TotalExpenditure.CWE.raw)[1])
TotalExpenditure.GB.raw$Zone<-rep(x="GB",times=dim(TotalExpenditure.GB.raw)[1])

TotalExpenditure.raw<-rbind(TotalExpenditure.CWE.raw,TotalExpenditure.GB.raw)


TotalExpenditureBoxplot<-ggplot(data=TotalExpenditure.raw)+
  geom_boxplot(aes(x=modelRun, y=V1))+
  facet_wrap( ~ Zone, scales="free")+
  xlab("Scenario")+
  ylab("Total Disc. Consumer Cost [EUR]")+
  theme_publication(base_size=baseSize)+
  theme(axis.text.x=element_text(angle=-40,vjust=0.5))
if(showPlots) TotalExpenditureBoxplot
if(safePlots) ggsave(filename= paste(filePrefix, "TotalConsumerExpenditureBoxplot",fileType, sep=""),plot=TotalExpenditureBoxplot, width=columnwidth, height=7.83, units="cm", scale=scaleFactor)

SpecificTotalExpenditure.CWE<-ddply(ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunIdSpecificPerkWhAndCountry, sum, "DiscTotalConsumerExpenditure.CWE","Total_EnergyServedinMWh_Country.A"), .variables="modelRun", .fun=applyFunToColumnInDF ,fun=quantile, column="V1")
SpecificTotalExpenditure.GB<-ddply(ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunIdSpecificPerkWhAndCountry, sum, "DiscTotalConsumerExpenditure.GB","Total_EnergyServedinMWh_Country.B"), .variables="modelRun", .fun=applyFunToColumnInDF ,fun=quantile, column="V1")

SpecificTotalExpenditure.CWE.raw<-ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunIdSpecificPerkWhAndCountry, sum, "DiscTotalConsumerExpenditure.CWE","Total_EnergyServedinMWh_Country.A")
SpecificTotalExpenditure.GB.raw<-ddply(bigDF, .variables=c("runId", "modelRun"), .fun=functionOfVariablePerRunIdSpecificPerkWhAndCountry, sum, "DiscTotalConsumerExpenditure.GB","Total_EnergyServedinMWh_Country.B")

SpecificTotalExpenditure.CWE.raw$Zone<-rep(x="CWE",times=dim(SpecificTotalExpenditure.CWE.raw)[1])
SpecificTotalExpenditure.GB.raw$Zone<-rep(x="GB",times=dim(SpecificTotalExpenditure.GB.raw)[1])

SpecificTotalExpenditure.raw<-rbind(SpecificTotalExpenditure.CWE.raw,SpecificTotalExpenditure.GB.raw)
SpecificTotalExpenditureBoxplot<-ggplot(data=SpecificTotalExpenditure.raw)+
  geom_boxplot(aes(x=modelRun, y=V1))+
  facet_wrap( ~ Zone, scales="free_y")+
  xlab("Model Run")+
  ylab("Total Specific Disc. Consumer Cost [EUR/MWh]")+
  theme_publication(base_size=12)+
  theme(axis.text.x=element_text(angle=-40,vjust=0.5),axis.title.y=element_text(size=10))
if(showPlots) SpecificTotalExpenditureBoxplot
if(safePlots) ggsave(filename= paste(filePrefix, "SpecificTotalConsumerExpenditureBoxplot",fileType, sep=""),plot=SpecificTotalExpenditureBoxplot, width=9.74, height=9.74, units="cm", scale=scaleFactor)

totalProfitCWE<-meltTechnologyVariable(bigDF,"CountryAProdFinances_Profit")
totalProfitCWE$variable<-rep("CWE",dim(totalProfitCWE)[1])
totalProfitCWE$value<-totalProfitCWE$value/1000000000
totalProfitGB<-meltTechnologyVariable(bigDF,"CountryBProdFinances_Profit")
totalProfitGB$variable<-rep("GB",dim(totalProfitGB)[1])
totalProfitGB$value<-totalProfitGB$value/1000000000
totalProfitAll<-rbind(totalProfitCWE,totalProfitGB)


aggregateProfit<-plotMoltenVariableFacettedByVariable(totalProfitAll, paste("Producer Profit [bn EUR]",sep=""))+
  theme(legend.position="bottom")+
  scale_color_discrete("")+
  scale_fill_discrete("")+
  scale_shape_discrete("")+
  stat_summary(aes_string(colour="modelRun", fill="modelRun", group="modelRun"), fun.y="mean", geom="line", linetype=1)+
  stat_summary(data=totalProfitAll[totalProfitAll$tick%%5==0 | totalProfitAll$tick==39,],aes_string(group="modelRun",shape="modelRun"), fun.y="mean", geom="point", size=1.5)+
  facet_wrap(~ variable, scales="free_y")+
  ylab(expression(Producer ~ Profit ~ "["*10^{9} ~ EUR*"]"))+
  xlab("\nYear")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)
if(safePlots) ggsave(filename= paste(filePrefix, "TotalProducerProfit",fileType, sep=""),plot=aggregateProfit, width=15.66, height=10.44, units="cm", scale=scaleFactor)
