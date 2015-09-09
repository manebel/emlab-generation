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
filePrefix<-"0-"
socialDiscountRate<-0.03
baseSize<-9
##---- Read in of Data                     ------------------
if(!file.exists(paste(analysisFolder,filePrefix,".rds", sep=""))){
  bigDF <- rbind(getDataFrameForModelRunsInFolderWithFilePattern(resultFolder,"0.*.csv"))
  
  bigDF$modelRun<-lapply(as.character(bigDF$modelRun),function(x,...){strsplit(x,...)}[[1]][2], "-")
  
  
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
timeBreaks=c(0,seq(9,39, by=10))

growthTrend <- function(start,growth,time){
  growingTerm<-(1+growth)^time
  result<-start*growingTerm
  return(result)
}
setwd("/home/marvin/Schreibtisch/Master/Master Thesis Latex/pics/Discussion/")
source("funcCalculateRevenue.r")




bigDFYear20<-bigDF[bigDF$tick==39,]

elPricesCWE=meltPrefixVariables(bigDFYear20,"PriceInEURperMWh_Segment.Country.A.")
elPricesGB=meltPrefixVariables(bigDFYear20,"PriceInEURperMWh_Segment.Country.B.")

meanDFCWE=data.frame(segment=c(1:20))
meanDFCWE$zone=rep("CWE",dim(meanDFCWE)[1])
meanDFCWE$price=rep(0,dim(meanDFCWE)[1])
meanDFGB=data.frame(segment=c(1:20))
meanDFGB$zone=rep("GB",dim(meanDFGB)[1])
meanDFGB$price=rep(0,dim(meanDFGB)[1])
meanDFTotal=data.frame(segment=c(1:20))
meanDFTotal$price=rep(0,dim(meanDFTotal)[1])
for (i in 1:20){
  calculate<-elPricesCWE[elPricesCWE$variable==i,"value"]
  meanDFCWE[i,"price"]<-mean(calculate)
  calculate2<-elPricesGB[elPricesGB$variable==i,"value"]
  meanDFGB[i,"price"]<-mean(calculate2)
  meanDFTotal[i,"price"]<-(meanDFCWE[i,"price"]+meanDFGB[i,"price"])/2
}

meanDFTotal$length<-c(85,207,351,544,666,571,549,630,711,894,933,649,450,346,306,294,293,187,77,17)
technologies<-data.frame(technology=c("Nuclear","coalPSC","IGCC","CCGT","OCGT"),capacity=c(1000,758,758,776,150))
technologies$variableCosts<-c(14.02783,26.51130,24.02613,45.69589,70.94888)
technologies$fixedCostsPerMW<-c(71870,40970,60370.8,29470,14370)
technologies$investmentCostsPerMW<-c(2874800,1365530,2501076,growthTrend(646830,(-0.0075679493),10),359350)
technologies$deprecationTime<-c(25,20,20,15,15)
technologies$minSegments<-c(10,10,0,0,0)
technologies$constructionTime<-c(7,4,4,2,0.5)
technologies$permitTime<-c(2,1,1,1,0.5)

thisInterestRate<-0.03




dfProfitNuclear<-calculateProfit("Nuclear",meanDFTotal,technologies,interestRate=thisInterestRate)
dfProfitCoalPSC<-calculateProfit("coalPSC",meanDFTotal,technologies,interestRate=thisInterestRate)
dfProfitIGCC<-calculateProfit("IGCC",meanDFTotal,technologies,interestRate=thisInterestRate)
dfProfitCCGT<-calculateProfit("CCGT",meanDFTotal,technologies,interestRate=thisInterestRate)
dfProfitOCGT<-calculateProfit("OCGT",meanDFTotal,technologies,interestRate=thisInterestRate)

dfProfitOCGT[1:3,]$NPVperMW
dfProfitCCGT[1:3,]$NPVperMW
dfProfitIGCC[1:3,]$NPVperMW


meanDFTotalHigh<-meanDFTotal
meanDFTotalHigh$price<-meanDFTotal$price
#meanDFTotalHigh[1,]$price<-2000
 #meanDFTotalHigh[2,]$price<-2000
# meanDFTotalHigh[3,]$price<-2000
meanDFTotalHigh[2:6,]$price<-90
  meanDFTotalHigh[10:17,]$price<-24

dfProfitNuclear2<-calculateProfit("Nuclear",meanDFTotalHigh,technologies,interestRate=thisInterestRate)
dfProfitCoalPSC2<-calculateProfit("coalPSC",meanDFTotalHigh,technologies,interestRate=thisInterestRate)
dfProfitIGCC2<-calculateProfit("IGCC",meanDFTotalHigh,technologies,interestRate=thisInterestRate)
dfProfitCCGT2<-calculateProfit("CCGT",meanDFTotalHigh,technologies,interestRate=thisInterestRate)
dfProfitOCGT2<-calculateProfit("OCGT",meanDFTotalHigh,technologies,interestRate=thisInterestRate)
#round(dfProfitOCGT2[1:20,]$NPVperMW/1000)
round(dfProfitCCGT2[20,]$NPVperMW/1000)
#round(dfProfitIGCC2[1:20,]$NPVperMW/1000)
round(dfProfitCoalPSC2[20,]$NPVperMW/1000)
#round(dfProfitNuclear2[1:20,]$NPVperMW/1000)

