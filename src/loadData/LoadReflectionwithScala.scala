package loadData

import loadData.LoadPassengerData.spark  //导入之前定义的SparkSession
import model.Vehicle
import simulate.simulate

object LoadReflectionwithScala {
  
  def getReflectionData(filename : String) ={
    //然后使用SparkSessions对象加载CSV成为DataFrame
    val df = spark.read
             .format("com.databricks.spark.csv")
             .option("header", "true") //reading the headers
             .option("mode", "DROPMALFORMED")
             .load(filename);
    
//    df.show()
    //得到数据
    val vehicleRDD = df.select("grid","reflection").rdd
    
    //对数据进行处理，添加到Tasks列表
    vehicleRDD.foreach { row => InitReflectionMap(row.get(0).toString() ,
             row.get(1).toString()) }

    }
  
  //初始化汽车信息
  def InitReflectionMap(grid:String, reflect:String)={
//    println(grid, reflect)
    simulate.reflection.put(grid, reflect)
    simulate.decodeReflection.put(reflect, grid)
  }
}