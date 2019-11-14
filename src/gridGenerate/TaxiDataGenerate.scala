package gridGenerate

import GridGenerate.{decodeLat_LonBin, getCentralLat_LonData}

object TaxiDataGenerate {
  
  def main(args:Array[String]){
    //首先初始化一个SparkSession对象
    val spark = org.apache.spark.sql.SparkSession.builder
                .master("local")
                .appName("Spark CSV Reader")
                .config("spark.sql.warehouse.dir", "file:///")
                .getOrCreate;
  
    var filename = "reflection_portugal.csv"
    //然后使用SparkSessions对象加载CSV成为DataFrame
    val df = spark.read
             .format("com.databricks.spark.csv")
             .option("header", "true") //reading the headers
             .option("mode", "DROPMALFORMED")
             .load(filename); 
    
    
    val gridsRDD = df.select("grid", "reflection").rdd
    
    var taxiCoordinate = gridsRDD.map { row =>  
                    getCentralLat_LonData(decodeLat_LonBin(row.get(1).toString().toInt)._1,
                    decodeLat_LonBin(row.get(1).toString().toInt)._2)}
    
    taxiCoordinate.foreach{println}
    
    
  }
 
}