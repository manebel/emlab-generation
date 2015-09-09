setwd("/home/marvin/Schreibtisch/Master/Master Thesis Latex/pics/Discussion/")
library(ggplot2)
source("funcCreateMeritOrderDF.r")
source("funcCreateMeritOrderDF-ccgtBelowCoal.r")


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


meritOrder1<-createMeritOrderDF("Normal prices",340)
meritOrder2<-createMeritOrderDF2("Very high coal prices",340)
meritOrder_all<-rbind(meritOrder1,meritOrder2)


meritOrderPlot<-ggplot(meritOrder_all, aes(x=capacity, y=costs))+
  facet_wrap(~scenario)+
  #facet_grid(scenario~risk)+
  
  geom_bar(aes(fill=technology),stat="identity", width=1)+ # normal bar
  #geom_bar(aes(y=costs2),fill="grey20",alpha=0.4,stat="identity", width=1)+
  
#   geom_bar(data=bigDF[bigDF$red=="yes",], aes(x=capacity,y=costs2),
#            alpha=0.3, stat="identity",width=1,fill="red")+                #red bar
#    geom_bar(data=bigDF[bigDF$yellow=="yes",], aes(x=capacity,y=costs),
#            alpha=0.3, stat="identity",width=1,fill="yellow")+                #yellow bar
 
#   geom_smooth(data = bigDF[bigDF$inMerit1=="yes",], 
#               aes(x=capacity,y=elPrice1,linetype="el1"),
#               color="black",stat = "identity")+                         # Electricity Price 1
#    geom_smooth(data = bigDF[bigDF$inMerit2=="yes",], 
#                aes(x=capacity,y=elPrice2,linetype="el2"),
#               color="black",stat = "identity")+                        # Electricity Price 2
#   
#   geom_vline(data=bigDF[bigDF$margin1=="yes",],
#              aes(xintercept = capacity+0.5,linetype="d"),
#              show_guide=F)+                                             # Demand 1
#   geom_vline(data=bigDF[bigDF$margin2=="yes",],
#              aes(xintercept = capacity+0.5,linetype="d"),
#              show_guide=F)+                                             # Demand 2
  
  
  scale_x_continuous(lim=c(0,400))+
#                       breaks=c(positionRenewables,positionNuclear,
#                                positionCoal,positionCCGT,positionOCGT),
#                       labels=c("Renew.","Nuclear","Coal","CCGT","OCGT"))+
  xlab("Capacity [GW]")+                                                    # X- axis      
  
  
  scale_y_continuous(lim=c(0,100))+                                                               
  ylab("Bid [EUR/MWh]")+                                    # Y-  Axis
  
  
  scale_alpha_discrete(range = c(0.45,1),breaks=c("yes","no"),guide='none')+
  scale_fill_manual(name="Technology",values=c("Renewables"="grey20","Nuclear"="grey35","Coal"="grey50","CCGT"="grey65","OCGT"="grey80"),
                    breaks=c("Renewables","Nuclear","Coal","CCGT","OCGT"))+
#   scale_linetype_manual(name = "",
#                         labels = c("d"=expression("Demand"),
#                                    "el1"=expression("Electricity price"[1]),
#                                    "el2"=expression("Electricity price"[2])), values = c("d"=1,"el1"=4,"el2"=3))+
#   
#   guides(colour = guide_legend(override.aes = list(linetype = 0 )), 
#          fill = guide_legend(override.aes = list(linetype = 0 )), 
#          shape = guide_legend(override.aes = list(linetype = 0 )), 
#          linetype = guide_legend())+
  
  theme_bw()
#   theme(legend.text.align=0,
#         axis.text.x = element_text(angle = 90,vjust=0.5))

meritOrderPlot

ggsave(filename="meritOrder-StructuralChange_all.pdf",plot=meritOrderPlot, width=16, height=10, units="cm")