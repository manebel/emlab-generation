calculateProfit<-function(technologieString,segmentDF,technologyDF,interestRate=0.1){
  minSegments<-technologyDF[technologyDF$technology==technologieString,"minSegments"]
  omCostsPerMW<-technologyDF[technologyDF$technology==technologieString,"fixedCostsPerMW"]
  invCostsPerMW<-technologyDF[technologyDF$technology==technologieString,"investmentCostsPerMW"]
  capacity<-technologyDF[technologyDF$technology==technologieString,"capacity"]
  permitTime<-technologyDF[technologyDF$technology==technologieString,"permitTime"]
  constructionTime<-technologyDF[technologyDF$technology==technologieString,"constructionTime"]
  deprecationTime<-technologyDF[technologyDF$technology==technologieString,"deprecationTime"]
  omCosts<-omCostsPerMW*capacity
  invCosts<-invCostsPerMW*capacity
  
  result=data.frame(segment=c(1:20))
  result$segmentProfitPerMW<-(segmentDF$price-technologyDF[technologyDF$technology==technologieString,]$variableCosts)*segmentDF$length
  result$segmentProfit<-result$segmentProfitPerMW*capacity
  result$totalProfit<-rep(0,20)
  result[1,]$totalProfit<-max(result[1,]$segmentProfit-omCosts,-omCosts)
  for (i in 2:20){
    result[i,]$totalProfit<-max(result[i-1,]$totalProfit+result[i,]$segmentProfit,result[i-1,]$totalProfit)
  }
if(minSegments>0){
  result[result$segment<=minSegments,]$totalProfit<-omCosts*-1
}
  result$relativeProfitAgainstInvestment<-result$totalProfit/invCosts
result$relativeProfitPerMW<- result$relativeProfitAgainstInvestment/capacity
absNPV<-c()
for (i in 1:20){
  currentNPV<-calculateNPV(invCosts, result[i,]$totalProfit,interestRate,constructionTime,permitTime,deprecationTime)
  absNPV<-c(absNPV,currentNPV)
}
result$absoluteNPV<-absNPV
result$NPVperMW<-absNPV/capacity
result$technology<-rep(technologieString,20)
  return(result)
}

calculateNPV<-function(investmentCosts,yearlyProfit,interestRate,constructiontime,permittime,deprecationTime){
 buildingTime<-constructiontime+permittime
 invCostPerYear=investmentCosts/buildingTime
 npv=0
  for (i in 1:deprecationTime){
    if (i <= buildingTime ){
      npv=npv-invCostPerYear/(1+interestRate)^(i-1)
    }
    else{
      npv=npv+yearlyProfit/(1+interestRate)^(i-1)
    }
  }
 return (npv)
}