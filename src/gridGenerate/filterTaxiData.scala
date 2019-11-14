package gridGenerate

import java.io.FileWriter

import gridGenerate.GridGenerate.generateGridID

import scala.collection.mutable.ListBuffer

object filterTaxiData {

  //首先初始化一个SparkSession对象
  val spark = org.apache.spark.sql.SparkSession.builder
    .master("local")
    .appName("Spark CSV Reader")
    .config("spark.sql.warehouse.dir", "file:///")
    .getOrCreate;

  val taxiPath = "portugal_taxi.csv"

  val taxi = spark.read
    .format("com.databricks.spark.csv")
    .option("header", "true") //reading the headers
    .option("mode", "DROPMALFORMED")
    .load(taxiPath); //.csv("csv/file/path") //spark 2.0 api

  val reflectionpath = "reflection_portugal.csv"
  val reflection = spark.read
    .format("com.databricks.spark.csv")
    .option("header", "true") //reading the headers
    .option("mode", "DROPMALFORMED")
    .load(reflectionpath); //.csv("csv/file/path") //spark 2.0 api

  var taxiInSF_100:ListBuffer[(Int, Double, Double)] = new ListBuffer[(Int, Double, Double)]()

  var reflectiongrid : ListBuffer[Int] = new ListBuffer[Int]()

  def main(args: Array[String]): Unit = {
    reflection.foreach(line=>reflectiongrid.append(line.get(0).toString.toInt))

    reflection.show()
    println(reflectiongrid)
    var count = 0
    taxi.foreach(line => {
      var id = line.get(0).toString
      var lat = line.get(1).toString.toDouble
      var lon = line.get(2).toString.toDouble

      var gridid = generateGridID(lat, lon)
      println(gridid)

      if(reflectiongrid.contains(gridid)) count+=1
    })

    println(count)


  }
}
