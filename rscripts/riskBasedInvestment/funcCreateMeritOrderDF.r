createMeritOrderDF<-function(scenarioName,demand1,
                             demand2=0,
                             markRed=c(0,0),markYellow=c(0,0),
                             numberRenewables=60,costsRenewables=1,costsRenewables2=0,
                             numberNuclear=30,costsNuclear=15.43,costsNuclear2=0,
                             numberCoal=90,costsCoal=29.16,costsCoal2=0,
                             numberCCGT=100,costsCCGT=50.27,costsCCGT2=0,
                             numberOCGT=85,costsOCGT=78.04,costsOCGT2=0,
                             costIncrements=0.07){
  
  renewableDF=data.frame(costs=c(rep(costsRenewables,numberRenewables)))
  renewableDF$costs2<-renewableDF$costs+costsRenewables2
  renewableDF$technology<-rep("Renewables",dim(renewableDF)[1])
  nuclearDF=data.frame(costs=seq(costsNuclear,costsNuclear+costIncrements*numberNuclear,by=costIncrements))
  nuclearDF$costs2<-nuclearDF$costs+costsNuclear2
  nuclearDF$technology<-rep("Nuclear",dim(nuclearDF)[1])
  coalDF=data.frame(costs=seq(costsCoal,costsCoal+costIncrements*numberCoal,by=costIncrements))
  coalDF$costs2<-coalDF$costs+costsCoal2
  coalDF$technology<-rep("Coal",dim(coalDF)[1])
  ccgtDF=data.frame(costs=seq(costsCCGT,costsCCGT+costIncrements*numberCCGT,by=costIncrements))
  ccgtDF$costs2<-ccgtDF$costs+costsCCGT2
  ccgtDF$technology<-rep("CCGT",dim(ccgtDF)[1])
  ocgtDF=data.frame(costs=seq(costsOCGT,costsOCGT+costIncrements*numberOCGT,by=costIncrements))
  ocgtDF$costs2<-ocgtDF$costs+costsOCGT2
  ocgtDF$technology<-rep("OCGT",dim(ocgtDF)[1])
  
  result<-rbind(renewableDF,nuclearDF,coalDF,ccgtDF,ocgtDF)
  result$technology<-factor(result$technology,levels=c("Renewables","Nuclear","Coal","CCGT","OCGT"))
  result$capacity<-seq(from=1,to=nrow(result),by=1)
  
  result$inMerit1<-rep("no",dim(result)[1])
  result$inMerit2<-rep("no",dim(result)[1])
  result$margin1<-rep("no",dim(result)[1])
  result$margin2<-rep("no",dim(result)[1])
  result[0:demand1,]$inMerit1<-rep("yes",demand1)
  result[demand1,]$margin1<-"yes"
  result$elPrice1<-rep(result[demand1,]$costs,dim(result)[1])
  result$elPrice2<-rep(0,dim(result)[1])
  if (demand2>0){
    result[0:demand2,]$inMerit2<-rep("yes",demand2)
    result[demand2,]$margin2<-"yes"
    result$elPrice2<-rep(result[demand2,]$costs,dim(result)[1])
  }
  
   if (result[demand1,]$costs2!=result[demand1,]$costs){
     result[0:demand1,]$inMerit2<-rep("yes",demand1)
     result$elPrice2<-rep(result[demand1,]$costs2,dim(result)[1])
  }
  
  result$red<-rep("no",dim(result)[1])
  result$yellow<-rep("no",dim(result)[1])
  if(markRed[2]>0){
    result[markRed[1]:markRed[2],]$red="yes"
  }
  if(markYellow[2]>0){
    result[markYellow[1]:markYellow[2],]$yellow="yes"
  }
  result$scenario<-rep(scenarioName,dim(result)[1])
  return (result)
}