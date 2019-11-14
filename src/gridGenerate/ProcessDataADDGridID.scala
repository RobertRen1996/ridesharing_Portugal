package gridGenerate

import java.io.FileWriter

import scala.collection.mutable.ListBuffer
import gridGenerate.GridGenerate.generateGridID


object ProcessDataADDGridID {
  //首先初始化一个SparkSession对象
  val spark = org.apache.spark.sql.SparkSession.builder
    .master("local")
    .appName("Spark CSV Reader")
    .config("spark.sql.warehouse.dir", "file:///")
    .getOrCreate;

  val dictpath = "F:\\software\\PyCharm\\pycharm file\\ridesharing\\organizeDatafromDBF\\"
  val filename = "passengerdata.csv"

  val df = spark.read
    .format("com.databricks.spark.csv")
    .option("header", "true") //reading the headers
    .option("mode", "DROPMALFORMED")
    .load(dictpath + filename); //.csv("csv/file/path") //spark 2.0 api

  var PassengerData: ListBuffer[(String, String, String, String, String, String, String, String, String, String, String)] = ListBuffer()

  def main(args: Array[String]): Unit = {
    processData()
    outputData()
  }


  def outputData() = {
    var outputfilepath = "C:\\Users\\15877\\Desktop\\passengerdata.csv"
    val writer = new FileWriter(outputfilepath, false)
    writer.append("userid,pickupDate,pickuptime,pickoffDate,pickofftime,startLat,startLon,endLat,endLon,startGrid,endGrid\n")
    PassengerData.foreach { line =>
      writer.append(line._1 + "," + line._2 + "," + line._3 + "," + line._4 + "," + line._5 + "," + line._6 + ","
        +line._7 + "," + line._8 +","+ line._9 +","+line._10 + "," + line._11 + "\n")
    }

    writer.close()
  }

  def processData() = {
    df.foreach(line => {
      var userid = line.get(0).toString
      var pickupDate = line.get(1).toString
      var pickuptime = line.get(2).toString
      var pickoffDate = line.get(3).toString
      var pickofftime = line.get(4).toString
      var startLat = line.get(5).toString
      var startLon = line.get(6).toString
      var endLat = line.get(7).toString
      var endLon = line.get(8).toString
      var startGrid = generateGridID(startLat.toDouble, startLon.toDouble).toString
      var endGrid = generateGridID(endLat.toDouble, endLon.toDouble).toString

      PassengerData.append((userid, pickupDate, pickuptime, pickoffDate, pickofftime, startLat, startLon, endLat, endLon, startGrid, endGrid))
    })
  }

}
