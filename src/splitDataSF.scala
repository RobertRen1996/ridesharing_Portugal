import java.io.FileWriter

import org.apache.spark.sql.{Dataset, Row}

import scala.collection.mutable.ListBuffer


object splitDataSF {

  //首先初始化一个SparkSession对象
  val spark = org.apache.spark.sql.SparkSession.builder
    .master("local")
    .appName("Spark CSV Reader")
    .config("spark.sql.warehouse.dir", "file:///")
    .getOrCreate


  val filepath = "C:\\Users\\15877\\Desktop\\passengerdata.csv"
  //     val outputPath = "F:/software/Eclipse/Scala_workSpace/HelloWorld/generatePassager/"

  //然后使用SparkSessions对象加载CSV成为DataFrame
  var passengerDataSF = spark.read
    .format("com.databricks.spark.csv")
    .option("header", "true") //reading the headers
    .option("mode", "DROPMALFORMED")
    .load(filepath)

  var timeList = passengerDataSF.select("pickupDate").distinct().collect()


  def main(args: Array[String]): Unit = {
    timeList.foreach(row => {
      var pickupdate = row.get(0).toString
      println("------------" + pickupdate + "------------")
      var filterResult = passengerDataSF.filter(row => row.get(1).toString == pickupdate).sort("pickuptime")

      /**
        * output
        */

      outputdata(pickupdate, filterResult)


    })
  }

  def outputdata(pickupdate: String, outputdata: Dataset[Row]) = {

    /** *
      * F:\software\Intell ij\workspace\ridesharing_experiment\Taxi_Try\Taxi_Try\SFData
      *
      */
    val outputpath = "F:\\software\\Intell ij\\workspace\\ridesharing_SF\\Taxi_Try\\SFData"
    val outputfilepath = outputpath + "/" + pickupdate + "_passengerData.csv"
    val outputdatasetwriter = new FileWriter(outputfilepath, false)
    outputdatasetwriter.append("userid,pickupDate,pickuptime,pickoffDate,pickofftime,startLat,startLon,endLat,endLon,startGrid,endGrid\n")
    outputdata.collect().foreach(line => outputdatasetwriter.append(line.get(0) + "," + line.get(1) + "," + line.get(2) + "," +
      line.get(3) + "," + line.get(4) + "," + line.get(5) + "," + line.get(6) + "," + line.get(7) + "," + line.get(8) +
      "," + line.get(9) + "," + line.get(10) + "\n") )
    outputdatasetwriter.close()
  }


}
