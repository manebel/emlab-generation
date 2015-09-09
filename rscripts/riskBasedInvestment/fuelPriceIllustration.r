library(ggplot2)
setwd("/home/marvin/Schreibtisch/Master/Master Thesis Latex/pics/Development_Fuel_Prices")
dfCoal=read.csv("./DECC_CoalPrice.csv",colClasses=c('numeric','NULL','numeric','NULL'))
dfCoal$variable<-rep("Coal",dim(dfCoal)[1])
dfGas=read.csv("./DECC_GasPrice.csv",colClasses=c('numeric','NULL','numeric','NULL'))
dfGas$variable<-rep("Gas",dim(dfGas)[1])

dfAll=rbind(dfCoal,dfGas)
fuelPricePlot<- ggplot(dfAll,aes_string(x="Time",y="Central"))+
  geom_line(data=dfAll,aes(color=variable),size=1)+
  scale_x_continuous(limits=c(2011,2050))+
  scale_y_continuous(limits=c(0,10),breaks=c(0,2,4,6,8,10),label=c(0,2,4,6,8,10))+
  geom_point(data=dfAll[dfAll$Time==2011|dfAll$Time%%5==0,],aes(color=variable,shape=variable),size=3)+
  theme_bw()+
  scale_color_manual(name="Fuel",
                     labels = c("Gas","Coal"),
                     values=c("Coal"="black","Gas"="grey50"),breaks=c("Gas","Coal"))+
  scale_shape_manual(name="Fuel",labels = c("Gas","Coal"),values=c("Coal"=17,"Gas"=19),
                     breaks=c("Gas","Coal"))+
  ylab("Price [EUR/GJ]")+
  xlab("\nYear")
ggsave(filename="fuelPriceDevelopment.pdf",plot=fuelPricePlot, width=15.66, height=10.44, units="cm")
