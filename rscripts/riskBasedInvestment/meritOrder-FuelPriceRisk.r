setwd("/home/marvin/Schreibtisch/Master/Master Thesis Latex/pics/Discussion/")
library(ggplot2)
source("funcCreateMeritOrderDF.r")


numberRenewables<-60
numberNuclear<-30
numberCoal<-90
numberCCGT<-100
numberOCGT<-85
positionRenewables<-numberRenewables/2
positionNuclear<-numberRenewables+numberNuclear/2
positionCoal<-numberRenewables+numberNuclear+numberCoal/2
positionCCGT<-numberRenewables+numberNuclear+numberCoal+numberCCGT/2
positionOCGT<-numberRenewables+numberNuclear+numberCoal+numberCCGT+numberOCGT/2
renewableArea<-c(1,numberRenewables+1)
nuclearArea<-c(numberRenewables+2,numberRenewables+numberNuclear+1)
coalArea<-c(nuclearArea[2]+1,nuclearArea[2]+numberCoal+1)
ccgtArea<-c(coalArea[2]+1,coalArea[2]+numberCCGT+1)
ocgtArea<-c(ccgtArea[2]+1,ccgtArea[2]+numberOCGT+1)


# ocgtHigh1<-createMeritOrderDF("Margin: OCGT (I) base",340)
# ocgtHigh2<-createMeritOrderDF("Margin: OCGT",340,demand2=320,markYellow=c(321,340))
# ocgtHigh2$risk<-rep("Situation I",dim(ocgtHigh2)[1])
# ocgtLow1<-createMeritOrderDF("Margin: OCGT (II) base",290)
# ocgtLow2<-createMeritOrderDF("Margin: OCGT",290,demand2=270,markYellow=c(ocgtArea[1],290),markRed=c(1,ccgtArea[2]))
# ocgtLow2$risk<-rep("Situation II",dim(ocgtLow2)[1])
# ccgtHigh1<-createMeritOrderDF("Margin: CCGT (I) base",250)
# ccgtHigh2<-createMeritOrderDF("Margin: CCGT",250,demand2=230,markYellow=c(231,250))
# ccgtHigh2$risk<-rep("Situation I",dim(ccgtHigh2)[1])
# ccgtLow1<-createMeritOrderDF("Margin: CCGT (II) base",190)
# ccgtLow2<-createMeritOrderDF("Margin: CCGT",190,demand2=170,markYellow=c(ccgtArea[1],190),markRed=c(1,coalArea[2]))
# ccgtLow2$risk<-rep("Situation II",dim(ccgtLow2)[1])
# coalHigh1<-createMeritOrderDF("Margin: Coal (I) base",150)
# coalHigh2<-createMeritOrderDF("Margin: Coal",150,demand2=130,markYellow=c(131,150))
# coalHigh2$risk<-rep("Situation I",dim(coalHigh2)[1])
# coalLow1<-createMeritOrderDF("Margin: Coal (II) base",100)
# coalLow2<-createMeritOrderDF("Margin: Coal",100,demand2=80,markYellow=c(coalArea[1],100),markRed=c(1,nuclearArea[2]))
# coalLow2$risk<-rep("Situation II",dim(coalLow2)[1])

# bigDF<-rbind(ocgtHigh1,ocgtHigh2,ocgtLow1,ocgtLow2,
#              ccgtHigh1,ccgtHigh2,ccgtLow1,ccgtLow2,
#              coalHigh1,coalHigh2,coalLow1,coalLow2)
# bigDF$scenario<-factor(bigDF$scenario,levels=c("Margin: OCGT (I)","Margin: OCGT (I) - Demand decreased",
#                                                "Margin: OCGT (II)","Margin: OCGT (II) - Demand decreased",
#                                                "Margin: CCGT (I)","Margin: CCGT (I) - Demand decreased",
#                                                "Margin: CCGT (II)","Margin: CCGT (II) - Demand decreased",
#                                                "Margin: Coal (I)","Margin: Coal (I) - Demand decreased",
#                                                "Margin: Coal (II)","Margin: Coal (II) - Demand decreased"))

# bigDF<-rbind(ocgtHigh2,ocgtLow2,
#              ccgtHigh2,ccgtLow2,
#              coalHigh2,coalLow2)
# bigDF$scenario<-factor(bigDF$scenario,levels=c("Margin: OCGT",
#                                                "Margin: CCGT",
#                                                "Margin: Coal"))



ocgtMarginCoal<-createMeritOrderDF("Margin: OCGT",310,costsCoal2=12,markRed=coalArea)
ocgtMarginCoal$risk<-rep("Coal price risk",dim(ocgtMarginCoal)[1])
ccgtMarginCoal<-createMeritOrderDF("Margin: CCGT",250,costsCoal2=12,markRed=coalArea)
ccgtMarginCoal$risk<-rep("Coal price risk",dim(ccgtMarginCoal)[1])
coalMarginCoal<-createMeritOrderDF("Margin: Coal",130,costsCoal2=(-9),markRed=c(1,nuclearArea[2]))
coalMarginCoal$risk<-rep("Coal price risk",dim(coalMarginCoal)[1])
ocgtMarginGas<-createMeritOrderDF("Margin: OCGT",310,costsOCGT2=(-16),costsCCGT2=(-14),markRed=c(1,coalArea[2]))
ocgtMarginGas$risk<-rep("Gas price risk",dim(ocgtMarginGas)[1])
ccgtMarginGas<-createMeritOrderDF("Margin: CCGT",250,costsOCGT2=(-16),costsCCGT2=(-14),markRed=c(1,coalArea[2]))
ccgtMarginGas$risk<-rep("Gas price risk",dim(ccgtMarginGas)[1])
coalMarginGas<-createMeritOrderDF("Margin: Coal",130,costsOCGT2=(-16),costsCCGT2=(-14))
coalMarginGas$risk<-rep("Gas price risk",dim(coalMarginGas)[1])



bigDF<-rbind(ocgtMarginCoal,ccgtMarginCoal,coalMarginCoal,
             ocgtMarginGas,ccgtMarginGas,coalMarginGas)
bigDF$scenario<-factor(bigDF$scenario,levels=c("Margin: OCGT",
                                               "Margin: CCGT",
                                               "Margin: Coal"))

meritOrderPlot<-ggplot(bigDF, aes(x=capacity, y=costs))+
#facet_wrap(~scenario,nrow=3)+
facet_grid(scenario~risk)+
  
  geom_bar(aes(fill=technology,alpha=inMerit1),stat="identity", width=1)+ # normal bar
  geom_bar(aes(y=costs2),fill="grey20",alpha=0.4,stat="identity", width=1)+
  
  geom_bar(data=bigDF[bigDF$red=="yes",], aes(x=capacity,y=costs2),
           alpha=0.3, stat="identity",width=1,fill="red")+                #red bar
   geom_bar(data=bigDF[bigDF$yellow=="yes",], aes(x=capacity,y=costs),
           alpha=0.3, stat="identity",width=1,fill="yellow")+                #yellow bar
 
  geom_smooth(data = bigDF[bigDF$inMerit1=="yes",], 
              aes(x=capacity,y=elPrice1,linetype="el1"),
              color="black",stat = "identity")+                         # Electricity Price 1
   geom_smooth(data = bigDF[bigDF$inMerit2=="yes",], 
               aes(x=capacity,y=elPrice2,linetype="el2"),
              color="black",stat = "identity")+                        # Electricity Price 2
  
  geom_vline(data=bigDF[bigDF$margin1=="yes",],
             aes(xintercept = capacity+0.5,linetype="d"),
             show_guide=F)+                                             # Demand 1
#   geom_vline(data=bigDF[bigDF$margin2=="yes",],
#              aes(xintercept = capacity+0.5,linetype="d"),
#              show_guide=F)+                                             # Demand 2
  
  
  scale_x_continuous(lim=c(0,400),
                     breaks=c(positionRenewables,positionNuclear,
                              positionCoal,positionCCGT,positionOCGT),
                     labels=c("Renew.","Nuclear","Coal","CCGT","OCGT"))+
  xlab("\nTechnology")+                                                    # X- axis      
  
  
  scale_y_continuous(lim=c(0,100))+                                                               
  ylab("Bid [EUR/MWh]\n")+                                    # Y-  Axis
  
  
  scale_alpha_discrete(range = c(0.45,1),breaks=c("yes","no"),guide='none')+
  scale_fill_grey(start = 0.2, end = .8,guide='none')+
  scale_linetype_manual(name = "",
                        labels = c("d"=expression("Demand"),
                                   "el1"=expression("Electricity price"[1]),
                                   "el2"=expression("Electricity price"[2])), values = c("d"=1,"el1"=2,"el2"=3))+
  
  guides(colour = guide_legend(override.aes = list(linetype = 0 )), 
         fill = guide_legend(override.aes = list(linetype = 0 )), 
         shape = guide_legend(override.aes = list(linetype = 0 )), 
         linetype = guide_legend())+
  
  theme_bw()+
  theme(legend.text.align=0,
        axis.text.x = element_text(angle = 90,vjust=0.5))

meritOrderPlot

ggsave(filename="meritOrder-fuelPriceRisk1.pdf",plot=meritOrderPlot, width=20, height=20, units="cm")