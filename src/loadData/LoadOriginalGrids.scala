package loadData

object LoadOriginalGrids {
  //首先初始化一个SparkSession对象
  val spark = org.apache.spark.sql.SparkSession.builder
                .master("local")
                .appName("Spark CSV Reader")
                .config("spark.sql.warehouse.dir", "file:///")
                .getOrCreate;
  
  def getOriginalGridsData(filename : String) ={
    //然后使用SparkSessions对象加载CSV成为DataFrame
    val df = spark.read
             .format("com.databricks.spark.csv")
             .option("header", "true") //reading the headers
             .option("mode", "DROPMALFORMED")
             .load(filename)
    
    
    val gridsRDD = df.select("num", "lat", "lon").rdd
    
    gridsRDD.foreach { row => InitOriginalGridsMap(row.get(0).toString().toInt, 
                          (row.get(1).toString().toDouble, row.get(2).toString().toDouble)) }
    
    
  }
  
  def InitOriginalGridsMap(num:Int, coord:(Double,Double))={
    simulate.simulate.originalGridsWithCentralCoord.+= (num  -> coord)
  }
  
}