package loadData

import scala.collection.mutable.HashMap
import simulate.simulate.travelTime
import simulate.simulate.fromGrid

object LoadtravelTime {
  //首先初始化一个SparkSession对象
  val spark = org.apache.spark.sql.SparkSession.builder
                .master("local")
                .appName("Spark CSV Reader")
                .config("spark.sql.warehouse.dir", "file:///")
                .getOrCreate;
  var filename = "portugal_road.csv"
  
  def getTravelTime(){
    //然后使用SparkSessions对象加载CSV成为DataFrame
    val df = spark.read
             .format("com.databricks.spark.csv")
             .option("header", "true") //reading the headers
             .option("mode", "DROPMALFORMED")
             .load(filename); 
    
    val travelTimeRDD = df.select("from_grid", "to_grid", "avg_time").rdd
    
    travelTimeRDD.foreach { row => addTravelTime(row.get(0).toString(),
          row.get(1).toString(), row.get(2).toString().toInt) }
    
//    println(travelTime.size)
    
  }
  
  // 这块是真实的路网数据，不是映射之后的
  def addTravelTime(grid_from:String, grid_to:String, travel_time:Int){
    
      fromGrid.append(grid_from.toInt)
    
     if (!travelTime.contains(grid_from)){ // 没有这个key了
       var tempMap : HashMap[String,Int] = HashMap()
       tempMap += ((grid_to, travel_time))
       travelTime.+=((grid_from, tempMap))
     }else{ //  有这个key
       travelTime.get(grid_from).get.+=((grid_to,travel_time))
     }
  }
  
}