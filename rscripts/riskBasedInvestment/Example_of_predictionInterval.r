library(ggplot2)
x=c(-4,-3,-2,-1,0)
y=c(4,6.3,6.6,7.2,8.5)
xy=data.frame(x, y)
xy$variable=rep("Example I",dim(xy)[1])
mdl=lm(y~x,data=xy)

y2=c(4.3,6.6,5,8,8.6)
xy2=data.frame(x,y)
xy2$y=y2
xy2$variable=rep("Example II",dim(xy2)[1])
mdl2=lm(y2~x,data=xy2)

xy_all=rbind(xy,xy2)

predx0 <- data.frame(x = seq(from = -7, to = 10, by = 0.1))
predx1 <- data.frame(x = seq(from = 1, to = 10, by = 0.1))

pred.int0 <- cbind(predx0, predict(mdl, predx0 ,interval="predict"))
pred.int0$variable<-rep("Example I",dim(pred.int0)[1])
pred.int1 <- cbind(predx1, predict(mdl, predx1,interval="predict"))
pred.int1$variable<-rep("Example I",dim(pred.int1)[1])
pred.int2 <- cbind(predx1, predict(mdl, predx1,interval="predict",level=0.8))
pred.int2$variable<-rep("Example I",dim(pred.int2)[1])
pred.int3 <- cbind(predx1, predict(mdl, predx1,interval="predict",level=0.8))
pred.int3$variable<-rep("Example I",dim(pred.int3)[1])
pred.int4 <- cbind(predx1, predict(mdl, predx1,interval="predict",level=0.99))
pred.int4$variable<-rep("Example I",dim(pred.int4)[1])


pred1.int0 <- cbind(predx0, predict(mdl2, predx0 ,interval="predict"))
pred1.int0$variable<-rep("Example II",dim(pred1.int0)[1])
pred1.int1 <- cbind(predx1, predict(mdl2, predx1,interval="predict"))
pred1.int1$variable<-rep("Example II",dim(pred1.int1)[1])
pred1.int2 <- cbind(predx1, predict(mdl2, predx1,interval="predict",level=0.8))
pred1.int2$variable<-rep("Example II",dim(pred1.int2)[1])
pred1.int3 <- cbind(predx1, predict(mdl2, predx1,interval="predict",level=0.8))
pred1.int3$variable<-rep("Example II",dim(pred1.int3)[1])
pred1.int4 <- cbind(predx1, predict(mdl2, predx1,interval="predict",level=0.99))
pred1.int4$variable<-rep("Example II",dim(pred1.int4)[1])


pred0_all=rbind(pred.int0,pred1.int0)
pred1_all=rbind(pred.int1,pred1.int1)
pred1_all$level=rep(0.95,dim(pred1_all)[1])
pred2_all=rbind(pred.int2,pred1.int2)
pred2_all$level=rep(0.8,dim(pred2_all)[1])
pred3_all=rbind(pred.int3,pred1.int3)
pred4_all=rbind(pred.int4,pred1.int4)
pred4_all$level=rep(0.99,dim(pred4_all)[1])

pred_all=rbind(pred1_all,pred2_all,pred4_all)

g_all.pred <- ggplot(pred2_all, aes(x = x, y = fit,linetype="Regression"))+
  geom_point(data = xy_all, aes(x = x, y = y,shape="DataPoints"),colour = "black") +
  geom_smooth(data = pred0_all, aes(x=x,y=fit), stat = "identity",color="black")+
  geom_smooth(data = pred_all[pred_all$level==0.8,], aes(x=x,ymin = lwr, ymax = upr,fill=factor(level)), color="black",stat = "identity")+
  geom_smooth(data = pred_all[pred_all$level==0.95,], aes(x=x,ymin = lwr, ymax = upr,fill=factor(level)), color="black",stat = "identity")+
  #geom_smooth(data = pred.int3, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
  geom_smooth(data = pred_all[pred_all$level==0.99,], aes(x=x,ymin = lwr, ymax = upr,fill=factor(level)), color="black",stat = "identity")+
  theme_bw()+
  ylab("Y\n")+
  xlab("\nX")+
  scale_x_continuous(breaks=c(-6,-3,0,3,6,9),labels=c(-6,-3,0,3,6,9),limits=c(-7,10))+
  facet_wrap(~variable)+
  theme(legend.position="right",legend.text=element_text(size=9),legend.title=element_text(size=9))+
scale_fill_manual(name="\nConfidence Levels",values=c("0.8"="grey60","0.95"="grey50","0.99"="grey40"),
  labels=c(expression(~alpha~"="~"0.8  "),expression(~alpha~"="~"0.95"),expression(~alpha~"="~"0.99")),
  breaks=c("0.99","0.95","0.8"))+
scale_shape_manual(name="",values = c("DataPoints"=18),labels=c(" Observation of\n past time steps"))+
scale_linetype_manual(name="",values = c("Regression"=1),labels=c(" Regression line"))+
  guides(fill = guide_legend(override.aes = list(linetype = 0),order=3),
         linetype=guide_legend(override.aes=list(fill="white"),order=2),
         shape=guide_legend(order=1))
g_all.pred
setwd("/home/marvin/Schreibtisch/Master/Master Thesis Latex/pics")
ggsave(filename= paste("predictionInterval.pdf"),plot=g_all.pred, width=16.66, height=10.44, units="cm")

# 
# g.pred <- ggplot(pred.int2, aes(x = x, y = fit))+
#   geom_point(data = xy, aes(x = x, y = y)) +
#   geom_smooth(data = pred.int0, aes(x=x,y=fit), stat = "identity")+
#   geom_smooth(data = pred.int1, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
#   geom_smooth(data = pred.int2, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
#   #geom_smooth(data = pred.int3, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
#   geom_smooth(data = pred.int4, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
#   theme_bw()+
#   ylab("Y")+
#   xlab("X")+
#   scale_x_continuous(breaks=c(-6,-4,-2,0,2,4,6,8),labels=c(-6,-4,-2,0,2,4,6,8))
# g.pred
# 
# g2.pred <- ggplot(pred1.int2, aes(x = x, y = fit))+
#   geom_point(data = xy, aes(x = x, y = y)) +
#   geom_smooth(data = pred1.int0, aes(x=x,y=fit), stat = "identity")+
#   geom_smooth(data = pred1.int1, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
#   geom_smooth(data = pred1.int2, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
#   #geom_smooth(data = pred1.int3, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
#   geom_smooth(data = pred1.int4, aes(x=x,y=fit,ymin = lwr, ymax = upr), stat = "identity")+
#   theme_bw()+
#   ylab("Y")+
#   xlab("X")+
#   scale_x_continuous(breaks=c(-6,-4,-2,0,2,4,6,8),labels=c(-6,-4,-2,0,2,4,6,8))
# g2.pred