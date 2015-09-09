library(ggplot2)
setwd("/home/marvin/Schreibtisch/Master/Master Thesis Latex/pics/Discussion/")
growthTrend <- function(start,growth,time){
 growingTerm<-(1+growth)^time
 result<-start*growingTerm
  return(result)
}

numberOfYears<-40
yearLabeler=seq(from=2011,to=2011+numberOfYears-1,by=1)
yearComputer=seq(from=-10,to=numberOfYears-11,by=1)

coalPSC_start=0.44
coalPSC_growth=0.0032724641

lignite_start=0.45
lignite_growth=0.005

CCGT_start=0.59
CCGT_growth=0.002068676

OCGT_start=0.38
OCGT_growth=0.002068676

nuclear_start=0.33
nuclear_growth=0.00001


coalPSC_series<-sapply(yearComputer,growthTrend,start=coalPSC_start,growth=coalPSC_growth)
lignite_series<-sapply(yearComputer,growthTrend,start=lignite_start,growth=lignite_growth)
CCGT_series<-sapply(yearComputer,growthTrend,start=CCGT_start,growth=CCGT_growth)
OCGT_series<-sapply(yearComputer,growthTrend,start=OCGT_start,growth=OCGT_growth)
nuclear_series<-sapply(yearComputer,growthTrend,start=nuclear_start,growth=nuclear_growth)
IGCC_series<-c(0.383,0.384,0.386,0.388,0.390,0.391,0.393,0.395,0.397,0.399,0.400,0.402,0.404,0.406,0.408,0.410,0.411,0.413,0.415,0.417,0.419,0.421,0.423,0.425,0.427,0.429,0.430,0.432,0.434,0.436,0.438,0.440,0.442,0.444,0.446,0.448,0.450,0.453,0.455,0.457,0.459,0.461,0.463,0.465,0.467,0.469,0.471,0.474,0.476,0.478,0.480,0.482,0.484,0.487,0.489,0.491,0.493,0.495,0.498,0.500,0.501,0.503,0.504,0.505,0.507,0.508,0.509,0.511,0.512,0.513,0.515,0.516,0.517,0.519,0.520,0.522,0.523,0.525,0.527,0.529,0.531,0.532,0.534,0.536,0.538,0.539,0.541,0.543,0.545,0.547,0.549,0.550,0.552,0.554,0.556,0.558,0.560,0.561,0.563,0.565,0.567,0.569,0.571,0.573,0.575,0.577,0.579,0.580,0.582,0.584,0.586,0.588,0.590,0.592,0.594,0.596)
IGCC_series<-IGCC_series[41:(numberOfYears+40)]
biomass_series<-c(0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350,0.350)
biomass_series<-biomass_series[41:(numberOfYears+40)]
biogas_series<-c(0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300,0.300)
biogas_series<-biogas_series[41:(numberOfYears+40)]

dfCoalPrice=read.csv("./DECC_CoalPrice.csv",colClasses=c('numeric','NULL','numeric','NULL'))
dfGasPrice=read.csv("./DECC_GasPrice.csv",colClasses=c('numeric','NULL','numeric','NULL'))
uraniumPrice=1.286
lignitePrice=1.428
biomassPrice<-sapply(0:39,growthTrend,start=4.5,growth=0.01)
coalPSC_costs<-data.frame(Value=(dfCoalPrice[1:numberOfYears,]$Central/coalPSC_series*3.6),Year=yearLabeler)
coalPSC_costs$Technology<-rep("coalPSC",dim(coalPSC_costs)[1])
IGCC_costs<-data.frame(Value=(dfCoalPrice[1:numberOfYears,]$Central/IGCC_series*3.6),Year=yearLabeler)
IGCC_costs$Technology<-rep("IGCC",dim(IGCC_costs)[1])
nuclear_costs<-data.frame(Value=(uraniumPrice/nuclear_series*3.6),Year=yearLabeler)
nuclear_costs$Technology<-rep("Nuclear Power",dim(nuclear_costs)[1])
CCGT_costs<-data.frame(Value=(dfGasPrice[1:numberOfYears,]$Central/CCGT_series*3.6),Year=yearLabeler)
CCGT_costs$Technology<-rep("CCGT",dim(CCGT_costs)[1])
OCGT_costs<-data.frame(Value=(dfGasPrice[1:numberOfYears,]$Central/OCGT_series*3.6),Year=yearLabeler)
OCGT_costs$Technology<-rep("OCGT",dim(OCGT_costs)[1])
lignite_costs<-data.frame(Value=(lignitePrice/lignite_series*3.6),Year=yearLabeler)
lignite_costs$Technology<-rep("Lignite",dim(lignite_costs)[1])
biomass_costs<-data.frame(Value=(biomassPrice/biomass_series*3.6),Year=yearLabeler)
biomass_costs$Technology<-rep("Biomass",dim(biomass_costs)[1])
biogas_costs<-data.frame(Value=(biomassPrice/biogas_series*3.6),Year=yearLabeler)
biogas_costs$Technology<-rep("Biogas",dim(biogas_costs)[1])
renewables_costs<-data.frame(Value=rep(0,numberOfYears),Year=yearLabeler,Technology=rep("Renewables",numberOfYears))

all_costs=rbind(biogas_costs,renewables_costs,coalPSC_costs,IGCC_costs,nuclear_costs,CCGT_costs,OCGT_costs,lignite_costs,biomass_costs)
all_costs$Technology=factor(all_costs$Technology,levels=rev(c("Renewables","Lignite","Nuclear Power","IGCC","coalPSC", "CCGT","Biomass","Biogas","OCGT")))
finalPlot<-ggplot(data=all_costs,aes_string(x="Year",y="Value",color="Technology"))+
  geom_line()+
  ylab("Variable Costs [EUR/MWh]\n")+
  xlab("\nYear")+
  scale_x_continuous(limits=c(2011,2050),breaks=c(2011,2020,2030,2040,2050),labels=c(2011,2020,2030,2040,2050))+
  scale_y_continuous(limits=c(-1,85))+
  geom_point(data=all_costs[all_costs$Year==2011|all_costs$Year%%5==0,],aes(color=Technology),size=2)+
  theme_bw()+
  scale_color_manual("Technology",values=c("OCGT"="blue4","Biogas"="orange",
                                             "Biomass"="orangered3","CCGT"="cadetblue",
                                              "coalPSC"="gray20","IGCC"="gray50",
                                              "Nuclear Power"="darkmagenta",
                                             "Lignite"="tan4","Renewables"="springgreen4"
  ))
finalPlot
ggsave(filename="variableCostsDevelopment.pdf",plot=finalPlot, width=16.66, height=9.44, units="cm")
