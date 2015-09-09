library(xtable)
library(scales) 
library("ggplot2")
library(ggplot2)
library(reshape2)
ldc <- data.frame(Segment = 1:20,
                  Demand = c(200942,194131,187841,181793,175934,170046,164570,158788,152512,146567,140841,135096,129296,123423,117229,110924,105485,100506,94334,88014),
                   Hours = c(85,207,351,544,666,571,549,630,711,894,933,649,450,346,306,294,293,187,77,17))

ldc$Demand  <- ldc$Demand /1000
ldc.m <- melt(ldc, id.vars='Segment')
graph<-ggplot(ldc.m, aes(Segment, value)) +   
  geom_bar(stat="identity")
graph<-graph+ scale_x_discrete(limits=c(1:20))
graph<-graph+facet_wrap(~ variable, ncol=1,scales="free_y")
graph

graph2 <- ggplot(ldc,aes(Segment,Demand,fill=Hours))+
geom_bar(stat="identity")+
  scale_fill_gradient(name="Number\nof hours",low="gray80",high="gray11",
                      breaks=c(0,200,400,600,800,1000),limits=c(0,1050))+
  scale_x_discrete(limits=c(1:20))+
  xlab("\nSegment Number")+
  ylab("Demand (GW)\n")+
  theme_bw()
graph2

graph3 <- ggplot(ldc,aes(Segment,Hours,fill=Demand))+
  geom_bar(stat="identity")+
  scale_fill_gradient(low="grey", high="black",)
graph3



