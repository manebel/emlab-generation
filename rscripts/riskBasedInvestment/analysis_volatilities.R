setwd("~/emlab-generation/rscripts/")
library(xtable)
library(scales)
library(gridExtra)
source("rConfig.R")
source("batchRunAnalysis.R")
setwd(analysisFolder)

safePlots=T
showPlots=F
fileType<-".pdf"
scaleFactor<-1
nrowLength<-1
#textwidth<-16.51
#columnwidth<-7.83
textwidth<-18.4
columnwidth<-8.89
filePrefix<-"5-"
socialDiscountRate<-0.03
baseSize<-9
##---- Read in of Data                     ------------------
if(!file.exists(paste(analysisFolder,filePrefix,".rds", sep=""))){
  bigDF <- rbind(getDataFrameForModelRunsInFolderWithFilePattern(resultFolder,"5.*.csv"))
  
  bigDF$modelRun<-lapply(as.character(bigDF$modelRun),function(x,...){strsplit(x,...)}[[1]][2], "-")
bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="standardSet","ALL_vol default")


  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="highVolatilityStandard","ALL_vol+")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="highVolatilityOnlyCoalPrice","CoalPrice_vol+")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="highVolatilityOnlyGasPrice","GasPrice_vol+")
  bigDF$modelRun<-replace(bigDF$modelRun,bigDF$modelRun=="highVolatilityOnlyDemand","Demand_vol+")
  



bigDF$modelRun<-factor(bigDF$modelRun,levels=c("ALL_vol default","ALL_vol+"))

  bigDF$stochasticId<-bigDF$runId
  bigDF$stochasticId<-lapply(as.character(bigDF$stochasticId),function(x,...){strsplit(x,...)}[[1]][3], "-")
  bigDF$stochasticId<-as.numeric(bigDF$stochasticId)

  saveRDS(bigDF, file=paste(analysisFolder,filePrefix,".rds", sep=""))
} else{
  bigDF<-readRDS(paste(analysisFolder,filePrefix,".rds", sep=""))
}

untilTick<-40
standardSubSet<-c("tick","modelRun","runId")
yearFormatter <- function(x){ 
  x = x+2011
}
timeBreaks=c(0,19,39)

# Because meltTechnologyVariable only looks at prefix...
names(bigDF)[names(bigDF) == 'Total_DemandinMWh_Country.A'] <- 'Total_DemandinMWh_Country.A1'
names(bigDF)[names(bigDF) == 'Total_DemandinMWh_Country.B'] <- 'Total_DemandinMWh_Country.B1'


demandCWE<-meltTechnologyVariable(bigDF,"Total_DemandinMWh_Country.A1")
demandCWE$variable<-rep("Demand (CWE)",dim(demandCWE)[1])
demandGB<-meltTechnologyVariable(bigDF,"Total_DemandinMWh_Country.B1")
demandGB$variable<-rep("Demand (GB)",dim(demandGB)[1])
coalPrice<-meltTechnologyVariable(bigDF,"FuelPricesPerGJ_Coal")
coalPrice$variable<-rep("Coal Price",dim(coalPrice)[1])
gasPrice<-meltTechnologyVariable(bigDF,"FuelPricesPerGJ_Natural.Gas")
gasPrice$variable<-rep("Gas Price",dim(gasPrice)[1])

demandCWE$value<-demandCWE$value/demandCWE[["value"]][1]
demandGB$value<-demandGB$value/demandGB[["value"]][1]
coalPrice$value<-coalPrice$value/coalPrice[["value"]][1]
gasPrice$value<-gasPrice$value/gasPrice[["value"]][1]

variablesAll<-rbind (coalPrice,gasPrice,demandGB,demandCWE)
 
variablesAll$variable<-factor(variablesAll$variable,levels=c("Demand (CWE)","Demand (GB)","Coal Price","Gas Price"))


variableAllPlot1<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(variablesAll, "value", "Relative change\n") +
  facet_grid( modelRun  ~variable)+
  scale_y_continuous(labels = percent)+
  theme(panel.margin = unit(1.5, "lines"))+
  xlab("\nYear")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)
variableAllPlot1
if(safePlots) ggsave(filename=
                       paste(filePrefix, "volatilities",fileType, sep=""),plot=variableAllPlot1, width=18.66, height=10.66, units="cm",scale=scaleFactor)


variableAllPlot<-plotMoltenVariableFacettedByVariable(variablesAll, "Average change")+
  theme(legend.position="bottom")+
  scale_color_discrete("Scenario")+
  scale_fill_discrete(guide = 'none')+
  scale_shape_discrete(guide = 'none')+
  stat_summary(aes_string(colour="modelRun", fill="modelRun", group="modelRun"), fun.y="mean", geom="line", linetype=1)+
  stat_summary(data=variablesAll[variablesAll$tick%%5==0 | variablesAll$tick==39,],aes_string(group="modelRun",shape="modelRun"), fun.y="mean", geom="point", size=1.5)+
  facet_grid(variable~ modelRun)+
  xlab("\nYear")+
  scale_x_continuous(labels=yearFormatter, breaks=timeBreaks)
